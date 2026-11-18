package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.remote.FocusRoomSyncStore
import com.example.data.remote.SupabaseUser
import com.example.data.repository.FocusRepository
import com.example.service.FocusSessionManager
import com.example.ui.FocusViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusCoreAndAdminTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: FocusRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FocusRepository(db.focusDao())
    }

    @After
    fun tearDown() {
        if (FocusSessionManager.isSessionActive.value) {
            FocusSessionManager.stopSession(context, completed = false)
        }
        if (FocusSessionManager.isBreakActive.value) {
            FocusSessionManager.stopBreak(context)
        }
        db.close()
    }

    @Test
    fun `startSession succeeds with reading app and zero blocked apps`() {
        FocusSessionManager.startSession(
            context = context,
            durationMinutes = 25,
            readingAppPackage = "com.amazon.kindle",
            isWhitelist = false,
            blockedPkgs = emptyList()
        )
        assertTrue(
            "Session should start immediately with reading app selected and no blocked apps",
            FocusSessionManager.isSessionActive.value
        )
        assertEquals("com.amazon.kindle", FocusSessionManager.designatedReadingApp)
        assertEquals(25 * 60L, FocusSessionManager.timeLeftSeconds.value)
        FocusSessionManager.stopSession(context, completed = false)
        assertFalse(FocusSessionManager.isSessionActive.value)
    }

    @Test
    fun `focusRoomSyncStore supports create join status update leave and admin delete`() = runTest {
        val store = FocusRoomSyncStore(context)
        val host = SupabaseUser(
            uid = "sb-host-1",
            email = "host@example.com",
            displayName = "Host Reader",
            avatarUrl = null,
            accessToken = "tok_1",
            refreshToken = null,
            expiresAtEpochSeconds = Long.MAX_VALUE
        )
        val guest = SupabaseUser(
            uid = "sb-guest-2",
            email = "guest@example.com",
            displayName = "Guest Reader",
            avatarUrl = null,
            accessToken = "tok_2",
            refreshToken = null,
            expiresAtEpochSeconds = Long.MAX_VALUE
        )

        val created = store.createRoom(
            user = host,
            name = "Deep Reading Club",
            durationMinutes = 30,
            isPublic = true,
            treeStage = 2
        )
        assertEquals("Deep Reading Club", created.name)
        assertEquals("waiting", created.status)
        assertEquals(6, created.roomCode.length)

        val publicRooms = store.observePublicRooms().first()
        assertTrue(publicRooms.any { it.roomId == created.roomId })

        val joined = store.joinRoomByCode(guest, created.roomCode, treeStage = 1)
        assertTrue(joined.isSuccess)
        assertEquals(2, joined.getOrThrow().participantCount)

        val participants = store.observeParticipants(created.roomId).first()
        assertEquals(2, participants.size)

        store.updateRoomStatus(created.roomId, "active")
        assertEquals("active", store.observeRoom(created.roomId).first()?.status)

        store.deleteRoom(created.roomId)
        assertFalse(store.observeAllRooms().first().any { it.roomId == created.roomId })
    }

    @Test
    fun `admin viewModel controls clear blocked apps reset pin and manage rooms`() = runTest {
        repository.insertBlockedApp("com.instagram.android", "Instagram")
        repository.setParentalLockEnabled(true)
        repository.setParentalPinHash("hashed_pin_1234")

        val vm = FocusViewModel(repository, context)
        assertNotNull(vm)

        vm.adminClearAllBlockedApps()
        kotlinx.coroutines.delay(150)
        assertTrue(repository.getBlockedAppsSync().isEmpty())

        vm.adminResetParentalLock()
        kotlinx.coroutines.delay(150)
        assertFalse(repository.isParentalLockEnabled().first())

        vm.adminQuickCreateRoom("Admin Lounge", 20, true)
        kotlinx.coroutines.delay(150)
        val rooms = vm.allRooms.first()
        assertTrue(rooms.any { it.name == "Admin Lounge" })
    }
}
