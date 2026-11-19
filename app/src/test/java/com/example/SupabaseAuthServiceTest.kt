package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.SupabaseAuthService
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseAuthServiceTest {

    @Test
    fun `manifest declares INTERNET permission and readlock auth deep link`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertTrue("INTERNET permission must be declared", permissions.contains(Manifest.permission.INTERNET))
        assertTrue(
            "ACCESS_NETWORK_STATE permission must be declared",
            permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE)
        )

        val callbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("readlock://auth/callback?code=sample_code"))
        val resolved = pm.queryIntentActivities(callbackIntent, 0)
        assertTrue(
            "MainActivity must handle readlock://auth/callback redirect",
            resolved.any { it.activityInfo.name.endsWith("MainActivity") }
        )
    }

    @Test
    fun `resolveConfig prefers publishable or jwt keys and rejects placeholders`() {
        val rejected = SupabaseAuthService.resolveConfig(
            urlCandidates = listOf("https://your-project.supabase.co", ""),
            keyCandidates = listOf("YOUR_SUPABASE_ANON_KEY", "YOUR_SUPABASE_PUBLISHABLE_KEY")
        )
        assertNull(rejected)

        val preferred = SupabaseAuthService.resolveConfig(
            urlCandidates = listOf("https://demo.supabase.co/"),
            keyCandidates = listOf("legacy_non_jwt", "sb_publishable_real_key_123")
        )
        assertEquals("https://demo.supabase.co" to "sb_publishable_real_key_123", preferred)

        val quotedAndBareHost = SupabaseAuthService.resolveConfig(
            urlCandidates = listOf("  \"demo.supabase.co/\"  "),
            keyCandidates = listOf("  'YOUR_SUPABASE_ANON_KEY'  ", "  \"sb_publishable_quoted_999\" ")
        )
        assertEquals("https://demo.supabase.co" to "sb_publishable_quoted_999", quotedAndBareHost)
    }

    @Test
    fun `buildBrowserAuthorizeUrl includes google provider s256 challenge and encoded callback`() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge = SupabaseAuthService.generateCodeChallenge(verifier)
        // RFC 7636 Appendix B test vector
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)

        val shaHex = SupabaseAuthService.sha256Hex("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", shaHex)

        val url = SupabaseAuthService.buildBrowserAuthorizeUrl("https://demo.supabase.co", challenge)
        assertTrue(url.startsWith("https://demo.supabase.co/auth/v1/authorize?"))
        assertTrue(url.contains("provider=google"))
        assertTrue(url.contains("redirect_to=readlock%3A%2F%2Fauth%2Fcallback"))
        assertTrue(url.contains("code_challenge=$challenge"))
        assertTrue(url.contains("code_challenge_method=s256"))
    }

    @Test
    fun `parseSessionResponse extracts user metadata and handles missing expires_at`() {
        val json = JSONObject().apply {
            put("access_token", "eyJ_sample_access")
            put("refresh_token", "refresh_123")
            put("expires_in", 3600)
            put("expires_at", 0)
            put(
                "user",
                JSONObject().apply {
                    put("id", "user-uuid-1")
                    put("email", "reader@example.com")
                    put(
                        "user_metadata",
                        JSONObject().apply {
                            put("full_name", "Alex Reader")
                            put("avatar_url", "https://example.com/avatar.png")
                        }
                    )
                }
            )
        }

        val user = SupabaseAuthService.parseSessionResponse(json)
        assertEquals("user-uuid-1", user.uid)
        assertEquals("reader@example.com", user.email)
        assertEquals("Alex Reader", user.displayName)
        assertEquals("https://example.com/avatar.png", user.avatarUrl)
        assertEquals("eyJ_sample_access", user.accessToken)
        assertEquals("refresh_123", user.refreshToken)
        assertTrue(user.expiresAtEpochSeconds > System.currentTimeMillis() / 1000L)
    }

    @Test
    fun `handleOAuthCallback surfaces query and fragment errors cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val wrongScheme = SupabaseAuthService.handleOAuthCallback(
            context,
            Uri.parse("https://example.com/callback?code=123")
        )
        assertTrue(wrongScheme.isFailure)

        val queryError = SupabaseAuthService.handleOAuthCallback(
            context,
            Uri.parse("readlock://auth/callback?error=access_denied&error_description=User+denied+access")
        )
        assertTrue(queryError.isFailure)
        assertEquals("User denied access", queryError.exceptionOrNull()?.message)

        val fragmentError = SupabaseAuthService.handleOAuthCallback(
            context,
            Uri.parse("readlock://auth/callback#error=server_error")
        )
        assertTrue(fragmentError.isFailure)
        assertEquals("server_error", fragmentError.exceptionOrNull()?.message)
    }

    @Test
    fun `main activity consumes auth callback uri on onCreate without crashing`() {
        val launchIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("readlock://auth/callback?error=access_denied")
        )
        val controller = Robolectric.buildActivity(MainActivity::class.java, launchIntent)
            .create()
            .start()
            .resume()
        val activity = controller.get()
        assertNotNull(activity)
        // Consumed auth URI must be cleared so configuration changes never replay it
        assertNull(activity.intent?.data)
        controller.pause().stop().destroy()
    }
}
