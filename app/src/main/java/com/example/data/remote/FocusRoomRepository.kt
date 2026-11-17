package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FocusRoomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Authentication required to access focus rooms.")
    }

    private fun currentDisplayName(): String {
        return auth.currentUser?.displayName?.take(50)?.ifBlank { null }
            ?: auth.currentUser?.email?.substringBefore("@")?.take(50)
            ?: "Focused Reader"
    }

    fun observePublicRooms(): Flow<List<FocusRoomDto>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return callbackFlow {
            val registration = db.collection("focusRooms")
                .whereEqualTo("isPublic", true)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(25)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val rooms = snap?.documents?.map { d -> parseRoom(d) }.orEmpty()
                    trySend(rooms)
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeRoom(roomId: String): Flow<FocusRoomDto?> {
        if (auth.currentUser == null || roomId.isBlank()) return flowOf(null)
        return callbackFlow {
            val registration = db.collection("focusRooms").document(roomId)
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null || !snap.exists()) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseRoom(snap))
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeParticipants(roomId: String): Flow<List<RoomParticipantDto>> {
        if (auth.currentUser == null || roomId.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val registration = db.collection("focusRooms").document(roomId)
                .collection("participants")
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val participants = snap?.documents?.map { d ->
                        RoomParticipantDto(
                            userId = d.getString("userId") ?: d.id,
                            displayName = d.getString("displayName") ?: "Reader",
                            status = d.getString("status") ?: "ready",
                            distractionAttempts = (d.getLong("distractionAttempts") ?: 0L).toInt(),
                            treeStage = (d.getLong("treeStage") ?: 1L).toInt(),
                            joinedAt = d.getTimestamp("joinedAt"),
                            updatedAt = d.getTimestamp("updatedAt")
                        )
                    }.orEmpty()
                    trySend(participants)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun createRoom(
        name: String,
        durationMinutes: Int,
        isPublic: Boolean,
        treeStage: Int
    ): Result<FocusRoomDto> {
        return try {
            val uid = requireUid()
            val displayName = currentDisplayName()
            val roomId = "rm_${UUID.randomUUID().toString().take(12)}"
            val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val roomCode = (1..6).map { codeChars.random() }.joinToString("")
            val now = Timestamp.now()

            val clampedDuration = durationMinutes.coerceIn(5, 180)
            val safeName = name.trim().take(60).ifBlank { "$displayName's Focus Room" }

            val roomData = hashMapOf<String, Any>(
                "roomId" to roomId,
                "roomCode" to roomCode,
                "name" to safeName,
                "hostId" to uid,
                "hostName" to displayName,
                "durationMinutes" to clampedDuration,
                "status" to "waiting",
                "participantCount" to 1,
                "isPublic" to isPublic,
                "createdAt" to now,
                "updatedAt" to now
            )

            db.collection("focusRooms").document(roomId).set(roomData).awaitResult()

            val participantData = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName,
                "status" to "ready",
                "distractionAttempts" to 0,
                "treeStage" to treeStage.coerceIn(1, 5),
                "joinedAt" to now,
                "updatedAt" to now
            )
            db.collection("focusRooms").document(roomId)
                .collection("participants").document(uid)
                .set(participantData).awaitResult()

            Result.success(
                FocusRoomDto(
                    roomId = roomId,
                    roomCode = roomCode,
                    name = safeName,
                    hostId = uid,
                    hostName = displayName,
                    durationMinutes = clampedDuration,
                    status = "waiting",
                    participantCount = 1,
                    isPublic = isPublic,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "creating focus room")))
        }
    }

    suspend fun joinRoomByCode(roomCode: String, treeStage: Int): Result<FocusRoomDto> {
        return try {
            requireUid()
            val cleanCode = roomCode.trim().uppercase()
            if (cleanCode.length < 4) {
                return Result.failure(IllegalArgumentException("Enter a valid room code."))
            }
            val querySnap = db.collection("focusRooms")
                .whereEqualTo("roomCode", cleanCode)
                .limit(1)
                .get()
                .awaitResult()
            val doc = querySnap.documents.firstOrNull()
                ?: return Result.failure(IllegalStateException("No room found with code $cleanCode."))
            val room = parseRoom(doc)
            joinRoom(room.roomId, treeStage).getOrThrow()
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "joining room by code")))
        }
    }

    suspend fun joinRoom(roomId: String, treeStage: Int): Result<Unit> {
        return try {
            val uid = requireUid()
            val displayName = currentDisplayName()
            val now = Timestamp.now()
            val roomRef = db.collection("focusRooms").document(roomId)
            val partRef = roomRef.collection("participants").document(uid)

            val partData = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName,
                "status" to "ready",
                "distractionAttempts" to 0,
                "treeStage" to treeStage.coerceIn(1, 5),
                "joinedAt" to now,
                "updatedAt" to now
            )
            partRef.set(partData).awaitResult()

            val partsSnap = roomRef.collection("participants").get().awaitResult()
            val count = partsSnap.size().coerceIn(1, 50)
            roomRef.update(
                mapOf(
                    "participantCount" to count,
                    "updatedAt" to now
                )
            ).awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "joining room")))
        }
    }

    suspend fun updateRoomStatus(roomId: String, status: String): Result<Unit> {
        return try {
            requireUid()
            val validStatus = if (status in listOf("waiting", "active", "completed")) status else "waiting"
            db.collection("focusRooms").document(roomId).update(
                mapOf(
                    "status" to validStatus,
                    "updatedAt" to Timestamp.now()
                )
            ).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "updating room status")))
        }
    }

    suspend fun updateMyParticipantStatus(
        roomId: String,
        status: String,
        distractionAttempts: Int,
        treeStage: Int
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val partRef = db.collection("focusRooms").document(roomId)
                .collection("participants").document(uid)
            val validStatus = if (status in listOf("ready", "focusing", "completed", "left")) status else "ready"
            partRef.update(
                mapOf(
                    "status" to validStatus,
                    "distractionAttempts" to distractionAttempts.coerceAtLeast(0),
                    "treeStage" to treeStage.coerceIn(1, 5),
                    "updatedAt" to Timestamp.now()
                )
            ).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "updating participant status")))
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            val uid = requireUid()
            val roomRef = db.collection("focusRooms").document(roomId)
            val roomSnap = roomRef.get().awaitResult()
            val hostId = roomSnap.getString("hostId")
            roomRef.collection("participants").document(uid).delete().awaitResult()
            if (hostId == uid) {
                roomRef.delete().awaitResult()
            } else {
                val remaining = roomRef.collection("participants").get().awaitResult().size().coerceAtLeast(0)
                roomRef.update(
                    mapOf(
                        "participantCount" to remaining,
                        "updatedAt" to Timestamp.now()
                    )
                ).awaitResult()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(FirebaseService.handleFirestoreError(e, "leaving room")))
        }
    }

    private fun parseRoom(d: com.google.firebase.firestore.DocumentSnapshot): FocusRoomDto {
        return FocusRoomDto(
            roomId = d.getString("roomId") ?: d.id,
            roomCode = d.getString("roomCode") ?: "",
            name = d.getString("name") ?: "Focus Room",
            hostId = d.getString("hostId") ?: "",
            hostName = d.getString("hostName") ?: "Host",
            durationMinutes = (d.getLong("durationMinutes") ?: 25L).toInt(),
            status = d.getString("status") ?: "waiting",
            participantCount = (d.getLong("participantCount") ?: 0L).toInt(),
            isPublic = d.getBoolean("isPublic") ?: true,
            createdAt = d.getTimestamp("createdAt"),
            updatedAt = d.getTimestamp("updatedAt")
        )
    }
}
