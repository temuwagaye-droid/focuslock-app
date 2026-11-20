package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AdminUserManagementStore
import com.example.data.remote.ManagedUserRecord
import com.example.data.remote.SupabaseUser
import kotlinx.coroutines.test.runTest
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
class AdminUserManagementTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AdminUserManagementStore.resetForTests(context)
    }

    @Test
    fun `ensureLoaded seeds primary device user and rejects blank or duplicate registration`() = runTest {
        AdminUserManagementStore.ensureLoaded(context)
        val initial = AdminUserManagementStore.users.value
        assertEquals(1, initial.size)
        assertEquals(ManagedUserRecord.PRIMARY_DEVICE_USER_ID, initial.first().userId)

        val invalidEmail = AdminUserManagementStore.registerUser(
            context = context,
            displayName = "Alex Reader",
            email = "not-an-email",
            initialTier = ManagedUserRecord.PLAN_FREE
        )
        assertTrue(invalidEmail.isFailure)

        val valid = AdminUserManagementStore.registerUser(
            context = context,
            displayName = "Alex Reader",
            email = "alex@focuslock.app",
            initialTier = ManagedUserRecord.PLAN_FREE
        )
        assertTrue(valid.isSuccess)
        assertEquals(ManagedUserRecord.PLAN_FREE, valid.getOrThrow().planTier)

        val duplicate = AdminUserManagementStore.registerUser(
            context = context,
            displayName = "Alex Duplicate",
            email = "ALEX@focuslock.app",
            initialTier = ManagedUserRecord.PLAN_PRO
        )
        assertTrue(duplicate.isFailure)
    }

    @Test
    fun `admin can activate PRO or FREE tier, suspend, and delete registered users`() = runTest {
        val created = AdminUserManagementStore.registerUser(
            context = context,
            displayName = "Mina Scholar",
            email = "mina@example.com",
            initialTier = ManagedUserRecord.PLAN_FREE
        ).getOrThrow()

        val promoted = AdminUserManagementStore.setUserPlan(context, created.userId, ManagedUserRecord.PLAN_PRO)
        assertNotNull(promoted)
        assertTrue(promoted!!.isPro)
        assertEquals(ManagedUserRecord.PLAN_PRO, promoted.planTier)

        val downgraded = AdminUserManagementStore.setUserPlan(context, created.userId, ManagedUserRecord.PLAN_FREE)
        assertNotNull(downgraded)
        assertFalse(downgraded!!.isPro)
        assertEquals(ManagedUserRecord.PLAN_FREE, downgraded.planTier)

        val suspended = AdminUserManagementStore.setUserEnabled(context, created.userId, false)
        assertNotNull(suspended)
        assertFalse(suspended!!.isEnabled)

        assertFalse(AdminUserManagementStore.deleteUser(context, ManagedUserRecord.PRIMARY_DEVICE_USER_ID))
        assertTrue(AdminUserManagementStore.deleteUser(context, created.userId))
        assertFalse(AdminUserManagementStore.users.value.any { it.userId == created.userId })
    }

    @Test
    fun `syncActiveDeviceAccount syncs Google user and Pro status`() {
        val googleUser = SupabaseUser(
            uid = "google_uid_42",
            email = "temu@gmail.com",
            displayName = "Temu Reader",
            avatarUrl = null,
            accessToken = "tok",
            refreshToken = "ref",
            expiresAtEpochSeconds = Long.MAX_VALUE
        )
        AdminUserManagementStore.syncActiveDeviceAccount(context, googleUser, isDevicePro = true)

        val synced = AdminUserManagementStore.users.value.firstOrNull { it.userId == "google_uid_42" }
        assertNotNull(synced)
        assertEquals("Temu Reader", synced!!.displayName)
        assertEquals("temu@gmail.com", synced.email)
        assertTrue(synced.isPro)
        assertEquals(ManagedUserRecord.SOURCE_GOOGLE, synced.source)
    }
}
