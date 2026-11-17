package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BlockedAppEntity
import com.example.data.model.SessionLogEntity
import com.example.data.model.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncProfileAndBackupRestore_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice_sync_${System.currentTimeMillis()}@example.com")
        val repo = CloudSyncRepository(firestore, auth)

        val settings = CloudPrivateSettings(
            userId = uid,
            isDarkMode = true,
            accentTheme = "emerald",
            designatedReadingApp = "com.amazon.kindle",
            partnerName = "Sam",
            partnerEmail = "sam@example.com",
            notifyPartnerOnQuit = true,
            cloudSyncEnabled = true,
            isPro = true
        )

        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncProfileAndSettings(
                displayName = "Alice Reader",
                focusScore = 420,
                totalFocusMinutes = 180,
                completedSessionsCount = 6,
                currentStreak = 4,
                totalBlockedAttempts = 5,
                treeStage = 4,
                isLeaderboardVisible = true,
                settings = settings
            )
        }
        assertTrue("Expected profile sync success, got ${syncRes.exceptionOrNull()}", syncRes.isSuccess)

        val backupRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.backupUserData(
                sessions = listOf(SessionLogEntity(id = 1, durationSeconds = 1500, blockedAttempts = 2, isCompleted = true, readingAppPackage = "com.amazon.kindle")),
                blockedApps = listOf(BlockedAppEntity("com.zhiliaoapp.musically", "TikTok", true)),
                todos = listOf(TodoEntity(id = 1, title = "Read Chapter 4", isCompleted = false))
            )
        }
        assertTrue("Expected backup success, got ${backupRes.exceptionOrNull()}", backupRes.isSuccess)
        assertEquals(3, backupRes.getOrThrow())

        val restoreRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.fetchCloudRestoreBundle() }
        assertTrue("Expected restore success, got ${restoreRes.exceptionOrNull()}", restoreRes.isSuccess)
        val bundle = restoreRes.getOrThrow()
        assertEquals("emerald", bundle.settings?.accentTheme)
        assertEquals("com.zhiliaoapp.musically", bundle.blockedApps.firstOrNull()?.packageName)
        assertEquals("Read Chapter 4", bundle.todos.firstOrNull()?.title)
    }

    @Test
    fun observeLeaderboard_emitsVisibleProfiles() = runBlocking {
        val uid = signInTestUser("alice_lb_${System.currentTimeMillis()}@example.com")
        val repo = CloudSyncRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncProfileAndSettings(
                displayName = "Leaderboard Star",
                focusScore = 999,
                totalFocusMinutes = 400,
                completedSessionsCount = 12,
                currentStreak = 7,
                totalBlockedAttempts = 1,
                treeStage = 5,
                isLeaderboardVisible = true,
                settings = CloudPrivateSettings(userId = uid, cloudSyncEnabled = true)
            )
        }

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLeaderboard().first { list -> list.any { it.userId == uid } }
        }
        assertTrue(entries.any { it.userId == uid && it.focusScore == 999 })
    }

    @Test
    fun unauthenticatedAccess_failsFailClosed() = runBlocking {
        auth.signOut()
        val repo = CloudSyncRepository(firestore, auth)
        val res = repo.fetchCloudRestoreBundle()
        assertTrue(res.isFailure)
    }
}
