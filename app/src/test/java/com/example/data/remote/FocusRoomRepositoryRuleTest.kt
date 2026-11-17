package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRoomRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createJoinUpdateLeaveRoom_twoUsers_enforcesRules(): Unit = runBlocking {
        val hostEmail = "host_${System.currentTimeMillis()}@example.com"
        val hostUid = signInTestUser(hostEmail)
        val hostRepo = FocusRoomRepository(firestore, auth)

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.createRoom(
                name = "Deep Reading Lounge",
                durationMinutes = 30,
                isPublic = true,
                treeStage = 3
            )
        }
        assertTrue("Expected room creation success, got ${created.exceptionOrNull()}", created.isSuccess)
        val room = created.getOrThrow()

        // Host starts session
        val statusRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            hostRepo.updateRoomStatus(room.roomId, "active")
        }
        assertTrue("Expected host status update success", statusRes.isSuccess)

        // Second user signs in and joins by code
        val memberUid = signInTestUser("member_${System.currentTimeMillis()}@example.com")
        val memberRepo = FocusRoomRepository(firestore, auth)
        val joined = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.joinRoomByCode(room.roomCode, treeStage = 2)
        }
        assertTrue("Expected join by code success, got ${joined.exceptionOrNull()}", joined.isSuccess)

        val participants = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observeParticipants(room.roomId).first { list ->
                list.any { it.userId == hostUid } && list.any { it.userId == memberUid }
            }
        }
        assertEquals(2, participants.size)

        // Non-host cannot change room status (PERMISSION_DENIED)
        val illegalStatus = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.updateRoomStatus(room.roomId, "completed")
        }
        assertTrue("Expected non-host status update failure", illegalStatus.isFailure)

        // Member leaves, host leaves and deletes room
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { memberRepo.leaveRoom(room.roomId) }.isSuccess)
        signInTestUser(hostEmail)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { hostRepo.leaveRoom(room.roomId) }.isSuccess)
    }
}
