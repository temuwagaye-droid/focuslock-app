package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local-persistent & reactive Focus Room store used alongside Supabase Google Sign-In
 * so group focus rooms can be created, joined by code, started, and moderated reliably
 * even when optional remote cloud sync is not configured.
 */
class FocusRoomSyncStore(context: Context?) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _rooms = MutableStateFlow<List<FocusRoomDto>>(emptyList())
    private val _participantsByRoom = MutableStateFlow<Map<String, List<RoomParticipantDto>>>(emptyMap())
    private val _messagesByRoom = MutableStateFlow<Map<String, List<RoomMessageDto>>>(emptyMap())

    init {
        loadFromPrefs()
    }

    fun observePublicRooms(): Flow<List<FocusRoomDto>> = _rooms.map { list ->
        list.filter { it.isPublic && it.status != "completed" }
    }

    fun observeAllRooms(): Flow<List<FocusRoomDto>> = _rooms

    fun observeRoom(roomId: String): Flow<FocusRoomDto?> = _rooms.map { list ->
        list.firstOrNull { it.roomId == roomId }
    }

    fun observeParticipants(roomId: String): Flow<List<RoomParticipantDto>> = _participantsByRoom.map { map ->
        map[roomId].orEmpty()
    }

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessageDto>> = _messagesByRoom.map { map ->
        map[roomId].orEmpty()
    }

    fun sendRoomMessage(roomId: String, user: SupabaseUser, text: String): Result<RoomMessageDto> = synchronized(lock) {
        val clean = text.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val name = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Reader"
        val msg = RoomMessageDto(
            messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            roomId = roomId,
            senderId = user.uid,
            senderName = name,
            content = clean,
            timestampMillis = System.currentTimeMillis()
        )
        val messagesMap = _messagesByRoom.value.toMutableMap()
        val list = messagesMap[roomId].orEmpty().toMutableList()
        list.add(msg)
        messagesMap[roomId] = list
        _messagesByRoom.value = messagesMap
        persistToPrefs()
        Result.success(msg)
    }

    fun createRoom(
        user: SupabaseUser,
        name: String,
        durationMinutes: Int,
        isPublic: Boolean,
        treeStage: Int
    ): FocusRoomDto = synchronized(lock) {
        val currentRooms = _rooms.value.toMutableList()
        val existingCodes = currentRooms.map { it.roomCode }.toSet()
        var code = generateRoomCode()
        while (existingCodes.contains(code)) {
            code = generateRoomCode()
        }

        val cleanName = name.trim().ifBlank { "Quiet Study Lounge" }.take(50)
        val cleanDuration = durationMinutes.coerceIn(5, 120)
        val hostName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Reader"
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val room = FocusRoomDto(
            roomId = roomId,
            roomCode = code,
            name = cleanName,
            hostId = user.uid,
            hostName = hostName,
            durationMinutes = cleanDuration,
            status = "waiting",
            participantCount = 1,
            isPublic = isPublic
        )
        val hostParticipant = RoomParticipantDto(
            userId = user.uid,
            displayName = hostName,
            status = "ready",
            distractionAttempts = 0,
            treeStage = treeStage
        )

        currentRooms.add(0, room)
        val participantsMap = _participantsByRoom.value.toMutableMap()
        participantsMap[roomId] = listOf(hostParticipant)

        _rooms.value = currentRooms
        _participantsByRoom.value = participantsMap
        persistToPrefs()
        room
    }

    fun joinRoom(user: SupabaseUser, roomId: String, treeStage: Int): Result<FocusRoomDto> = synchronized(lock) {
        val currentRooms = _rooms.value.toMutableList()
        val idx = currentRooms.indexOfFirst { it.roomId == roomId }
        if (idx < 0) {
            return Result.failure(IllegalArgumentException("Room no longer exists."))
        }
        val room = currentRooms[idx]
        val participantsMap = _participantsByRoom.value.toMutableMap()
        val list = participantsMap[roomId].orEmpty().toMutableList()
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Reader"

        if (list.none { it.userId == user.uid }) {
            list.add(
                RoomParticipantDto(
                    userId = user.uid,
                    displayName = displayName,
                    status = "ready",
                    distractionAttempts = 0,
                    treeStage = treeStage
                )
            )
        }
        participantsMap[roomId] = list
        val updatedRoom = room.copy(participantCount = list.size)
        currentRooms[idx] = updatedRoom

        _rooms.value = currentRooms
        _participantsByRoom.value = participantsMap
        persistToPrefs()
        Result.success(updatedRoom)
    }

    fun joinRoomByCode(user: SupabaseUser, rawCode: String, treeStage: Int): Result<FocusRoomDto> = synchronized(lock) {
        val normalized = rawCode.trim().uppercase()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a valid room code."))
        }
        val room = _rooms.value.firstOrNull { it.roomCode.equals(normalized, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("No room found with code '$normalized'."))
        joinRoom(user, room.roomId, treeStage)
    }

    fun updateRoomStatus(roomId: String, status: String): Result<Unit> = synchronized(lock) {
        val currentRooms = _rooms.value.toMutableList()
        val idx = currentRooms.indexOfFirst { it.roomId == roomId }
        if (idx < 0) return Result.failure(IllegalArgumentException("Room not found."))
        currentRooms[idx] = currentRooms[idx].copy(status = status)
        _rooms.value = currentRooms
        persistToPrefs()
        Result.success(Unit)
    }

    fun updateParticipantStatus(
        userId: String,
        roomId: String,
        status: String,
        distractionAttempts: Int,
        treeStage: Int
    ) = synchronized(lock) {
        val participantsMap = _participantsByRoom.value.toMutableMap()
        val list = participantsMap[roomId].orEmpty().toMutableList()
        val idx = list.indexOfFirst { it.userId == userId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(
                status = status,
                distractionAttempts = distractionAttempts,
                treeStage = treeStage
            )
            participantsMap[roomId] = list
            _participantsByRoom.value = participantsMap
            persistToPrefs()
        }
    }

    fun leaveRoom(userId: String, roomId: String) = synchronized(lock) {
        val participantsMap = _participantsByRoom.value.toMutableMap()
        val updatedParticipants = participantsMap[roomId].orEmpty().filterNot { it.userId == userId }
        val currentRooms = _rooms.value.toMutableList()
        val idx = currentRooms.indexOfFirst { it.roomId == roomId }

        if (updatedParticipants.isEmpty()) {
            participantsMap.remove(roomId)
            if (idx >= 0) currentRooms.removeAt(idx)
        } else {
            participantsMap[roomId] = updatedParticipants
            if (idx >= 0) {
                currentRooms[idx] = currentRooms[idx].copy(participantCount = updatedParticipants.size)
            }
        }

        _rooms.value = currentRooms
        _participantsByRoom.value = participantsMap
        persistToPrefs()
    }

    fun deleteRoom(roomId: String) = synchronized(lock) {
        _rooms.value = _rooms.value.filterNot { it.roomId == roomId }
        val participantsMap = _participantsByRoom.value.toMutableMap()
        participantsMap.remove(roomId)
        _participantsByRoom.value = participantsMap
        persistToPrefs()
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun loadFromPrefs() {
        val raw = prefs?.getString(KEY_ROOMS_JSON, null) ?: return
        runCatching {
            val root = JSONObject(raw)
            val roomsArr = root.optJSONArray("rooms") ?: JSONArray()
            val loadedRooms = mutableListOf<FocusRoomDto>()
            for (i in 0 until roomsArr.length()) {
                val obj = roomsArr.getJSONObject(i)
                loadedRooms.add(
                    FocusRoomDto(
                        roomId = obj.optString("roomId"),
                        roomCode = obj.optString("roomCode"),
                        name = obj.optString("name"),
                        hostId = obj.optString("hostId"),
                        hostName = obj.optString("hostName"),
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        status = obj.optString("status", "waiting"),
                        participantCount = obj.optInt("participantCount", 1),
                        isPublic = obj.optBoolean("isPublic", true)
                    )
                )
            }
            val participantsObj = root.optJSONObject("participants") ?: JSONObject()
            val loadedParticipants = mutableMapOf<String, List<RoomParticipantDto>>()
            val keys = participantsObj.keys()
            while (keys.hasNext()) {
                val rid = keys.next()
                val arr = participantsObj.optJSONArray(rid) ?: JSONArray()
                val list = mutableListOf<RoomParticipantDto>()
                for (j in 0 until arr.length()) {
                    val p = arr.getJSONObject(j)
                    list.add(
                        RoomParticipantDto(
                            userId = p.optString("userId"),
                            displayName = p.optString("displayName"),
                            status = p.optString("status", "ready"),
                            distractionAttempts = p.optInt("distractionAttempts", 0),
                            treeStage = p.optInt("treeStage", 1)
                        )
                    )
                }
                loadedParticipants[rid] = list
            }
            val messagesObj = root.optJSONObject("messages") ?: JSONObject()
            val loadedMessages = mutableMapOf<String, List<RoomMessageDto>>()
            val mkeys = messagesObj.keys()
            while (mkeys.hasNext()) {
                val rid = mkeys.next()
                val arr = messagesObj.optJSONArray(rid) ?: JSONArray()
                val list = mutableListOf<RoomMessageDto>()
                for (k in 0 until arr.length()) {
                    val m = arr.getJSONObject(k)
                    list.add(
                        RoomMessageDto(
                            messageId = m.optString("messageId"),
                            roomId = m.optString("roomId"),
                            senderId = m.optString("senderId"),
                            senderName = m.optString("senderName"),
                            content = m.optString("content"),
                            timestampMillis = m.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
                loadedMessages[rid] = list
            }
            _rooms.value = loadedRooms
            _participantsByRoom.value = loadedParticipants
            _messagesByRoom.value = loadedMessages
        }
    }

    private fun persistToPrefs() {
        val editor = prefs?.edit() ?: return
        val roomsArr = JSONArray()
        for (room in _rooms.value) {
            roomsArr.put(
                JSONObject().apply {
                    put("roomId", room.roomId)
                    put("roomCode", room.roomCode)
                    put("name", room.name)
                    put("hostId", room.hostId)
                    put("hostName", room.hostName)
                    put("durationMinutes", room.durationMinutes)
                    put("status", room.status)
                    put("participantCount", room.participantCount)
                    put("isPublic", room.isPublic)
                }
            )
        }
        val participantsObj = JSONObject()
        for ((rid, plist) in _participantsByRoom.value) {
            val arr = JSONArray()
            for (p in plist) {
                arr.put(
                    JSONObject().apply {
                        put("userId", p.userId)
                        put("displayName", p.displayName)
                        put("status", p.status)
                        put("distractionAttempts", p.distractionAttempts)
                        put("treeStage", p.treeStage)
                    }
                )
            }
            participantsObj.put(rid, arr)
        }
        val messagesObj = JSONObject()
        for ((rid, mlist) in _messagesByRoom.value) {
            val arr = JSONArray()
            for (m in mlist) {
                arr.put(
                    JSONObject().apply {
                        put("messageId", m.messageId)
                        put("roomId", m.roomId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("content", m.content)
                        put("timestampMillis", m.timestampMillis)
                    }
                )
            }
            messagesObj.put(rid, arr)
        }
        val root = JSONObject().apply {
            put("rooms", roomsArr)
            put("participants", participantsObj)
            put("messages", messagesObj)
        }
        editor.putString(KEY_ROOMS_JSON, root.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "focuslock_room_store"
        private const val KEY_ROOMS_JSON = "rooms_payload_json"
    }
}
