package com.example.data.remote

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long
)

sealed class SupabaseGoogleSignInResult {
    data class SignedIn(val user: SupabaseUser) : SupabaseGoogleSignInResult()
    data object BrowserFlowLaunched : SupabaseGoogleSignInResult()
}

private class SupabaseHttpException(
    val statusCode: Int,
    message: String
) : IllegalStateException(message)

object SupabaseAuthService {

    private const val PREFS_NAME = "supabase_auth_prefs"
    private const val KEY_UID = "sb_uid"
    private const val KEY_EMAIL = "sb_email"
    private const val KEY_DISPLAY_NAME = "sb_display_name"
    private const val KEY_AVATAR_URL = "sb_avatar_url"
    private const val KEY_ACCESS_TOKEN = "sb_access_token"
    private const val KEY_REFRESH_TOKEN = "sb_refresh_token"
    private const val KEY_EXPIRES_AT = "sb_expires_at"
    private const val KEY_PKCE_VERIFIER = "sb_pkce_verifier"

    const val OAUTH_REDIRECT_URI = "readlock://auth/callback"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val restored = loadPersistedUser(context)
            if (restored != null) {
                _currentUser.value = restored
            }
        }
    }

    suspend fun refreshSessionIfNeeded(context: Context) {
        initialize(context)
        val user = _currentUser.value ?: return
        val nowSec = System.currentTimeMillis() / 1000L
        if (user.expiresAtEpochSeconds > nowSec + 120L) return
        val refreshToken = user.refreshToken?.takeIf { it.isNotBlank() } ?: return
        val config = resolveSupabaseConfig() ?: return

        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().put("refresh_token", refreshToken)
                val responseJson = executeAuthPost(
                    url = "${config.first}/auth/v1/token?grant_type=refresh_token",
                    anonKey = config.second,
                    body = payload
                )
                val updated = parseSessionResponse(responseJson)
                persistUser(context, updated)
                _currentUser.value = updated
            } catch (e: SupabaseHttpException) {
                // Only clear persisted session if the refresh token was explicitly rejected/revoked
                if (e.statusCode in setOf(400, 401, 403)) {
                    _currentUser.value = null
                    clearPersistedUser(context)
                }
            } catch (_: IOException) {
                // Keep cached session while offline
            } catch (_: Exception) {
                // Non-fatal background refresh error
            }
        }
    }

    @Volatile
    private var preferBrowserOAuthThisSession = false

    suspend fun signInWithGoogle(context: Context): Result<SupabaseGoogleSignInResult> {
        initialize(context)
        val config = resolveSupabaseConfig()
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase key is not configured yet. Add SUPABASE_ANON_KEY (or NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY) in the AI Studio Secrets panel."
                )
            )
        val (supabaseUrl, anonKey) = config
        val activityContext = context.findActivity() ?: context
        val googleWebClientId = resolveGoogleWebClientId(activityContext)

        if (!googleWebClientId.isNullOrBlank() && !preferBrowserOAuthThisSession) {
            // Phase 1: Try Android Credential Manager (requires matching Android OAuth SHA-1 in GCP)
            val idTokenPair: Pair<String, String>? = try {
                val rawNonce = generateCodeVerifier()
                val hashedNonce = sha256Hex(rawNonce)
                val credentialManager = CredentialManager.create(activityContext)
                val googleIdOption = GetSignInWithGoogleOption.Builder(googleWebClientId)
                    .setNonce(hashedNonce)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleCredential.idToken to rawNonce
                } else {
                    null
                }
            } catch (_: GetCredentialCancellationException) {
                // Play Services throws GetCredentialCancellationException after account selection when
                // the GCP project for GOOGLE_WEB_CLIENT_ID lacks an Android OAuth SHA-1 client.
                // Seamlessly fall back to Supabase Google OAuth PKCE browser flow (no SHA-1 needed).
                preferBrowserOAuthThisSession = true
                null
            } catch (_: NoCredentialException) {
                preferBrowserOAuthThisSession = true
                null
            } catch (_: GetCredentialException) {
                preferBrowserOAuthThisSession = true
                null
            } catch (_: Exception) {
                preferBrowserOAuthThisSession = true
                null
            }

            // Phase 2: Exchange acquired Google ID Token directly with Supabase Auth
            if (idTokenPair != null) {
                return try {
                    val user = exchangeGoogleIdTokenWithSupabase(
                        supabaseUrl = supabaseUrl,
                        anonKey = anonKey,
                        idToken = idTokenPair.first,
                        rawNonce = idTokenPair.second
                    )
                    persistUser(context, user)
                    _currentUser.value = user
                    Result.success(SupabaseGoogleSignInResult.SignedIn(user))
                } catch (e: Exception) {
                    Result.failure(
                        IllegalStateException(
                            e.localizedMessage ?: "Failed to verify Google ID token with Supabase."
                        )
                    )
                }
            }
        }

        return launchBrowserOAuthFlow(activityContext, supabaseUrl)
    }

    private fun launchBrowserOAuthFlow(
        context: Context,
        supabaseUrl: String
    ): Result<SupabaseGoogleSignInResult> {
        return try {
            val verifier = generateCodeVerifier()
            val challenge = generateCodeChallenge(verifier)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PKCE_VERIFIER, verifier)
                .apply()

            val authUrl = buildBrowserAuthorizeUrl(supabaseUrl, challenge)
            val customTabExtras = android.os.Bundle().apply {
                putBinder("android.support.customtabs.extra.SESSION", null)
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                putExtras(customTabExtras)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            Result.success(SupabaseGoogleSignInResult.BrowserFlowLaunched)
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("No web browser found on this device to complete Google Sign-In.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Unable to open browser for Supabase Google Sign-In."
                )
            )
        }
    }

    suspend fun handleOAuthCallback(context: Context, uri: Uri): Result<SupabaseUser> {
        initialize(context)
        if (uri.scheme != "readlock" || uri.host != "auth") {
            return Result.failure(IllegalArgumentException("Unsupported callback URI."))
        }

        val errorDescription = uri.getQueryParameter("error_description")
            ?: extractFragmentParam(uri, "error_description")
            ?: uri.getQueryParameter("error")
            ?: extractFragmentParam(uri, "error")
        if (!errorDescription.isNullOrBlank()) {
            return Result.failure(IllegalStateException(errorDescription.replace("+", " ")))
        }

        val config = resolveSupabaseConfig()
            ?: return Result.failure(IllegalStateException("Supabase configuration missing."))
        val (supabaseUrl, anonKey) = config

        val authCode = uri.getQueryParameter("code")
        if (!authCode.isNullOrBlank()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val verifier = prefs.getString(KEY_PKCE_VERIFIER, null).orEmpty()
            if (verifier.isBlank()) {
                return Result.failure(IllegalStateException("OAuth PKCE verifier missing. Please try signing in again."))
            }

            return withContext(Dispatchers.IO) {
                try {
                    val payload = JSONObject()
                        .put("auth_code", authCode)
                        .put("code_verifier", verifier)
                    val responseJson = executeAuthPost(
                        url = "$supabaseUrl/auth/v1/token?grant_type=pkce",
                        anonKey = anonKey,
                        body = payload
                    )
                    prefs.edit().remove(KEY_PKCE_VERIFIER).apply()
                    val user = parseSessionResponse(responseJson)
                    persistUser(context, user)
                    _currentUser.value = user
                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(IllegalStateException(e.localizedMessage ?: "Supabase OAuth exchange failed."))
                }
            }
        }

        val accessToken = extractFragmentParam(uri, "access_token")
        val refreshToken = extractFragmentParam(uri, "refresh_token")
        val expiresIn = extractFragmentParam(uri, "expires_in")?.toLongOrNull() ?: 3600L
        if (!accessToken.isNullOrBlank()) {
            return withContext(Dispatchers.IO) {
                try {
                    val userJson = executeGetUser(
                        url = "$supabaseUrl/auth/v1/user",
                        anonKey = anonKey,
                        accessToken = accessToken
                    )
                    val user = parseUserObject(
                        userObj = userJson,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + expiresIn
                    )
                    persistUser(context, user)
                    _currentUser.value = user
                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to load Supabase user profile."))
                }
            }
        }

        return Result.failure(IllegalStateException("Google Sign-In did not return an authorization code or session token."))
    }

    suspend fun signOut(context: Context) {
        initialize(context)
        val current = _currentUser.value
        val config = resolveSupabaseConfig()

        _currentUser.value = null
        clearPersistedUser(context)

        if (current != null && config != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder()
                        .url("${config.first}/auth/v1/logout")
                        .addHeader("apikey", config.second)
                        .addHeader("Authorization", "Bearer ${current.accessToken}")
                        .post("{}".toRequestBody(jsonMediaType))
                        .build()
                    httpClient.newCall(request).execute().close()
                }
            }
        }

        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun exchangeGoogleIdTokenWithSupabase(
        supabaseUrl: String,
        anonKey: String,
        idToken: String,
        rawNonce: String? = null
    ): SupabaseUser = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
        if (!rawNonce.isNullOrBlank()) {
            payload.put("nonce", rawNonce)
        }

        val responseJson = executeAuthPost(
            url = "$supabaseUrl/auth/v1/token?grant_type=id_token",
            anonKey = anonKey,
            body = payload
        )
        parseSessionResponse(responseJson)
    }

    private fun executeAuthPost(url: String, anonKey: String, body: JSONObject): JSONObject {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
        if (anonKey.startsWith("eyJ")) {
            requestBuilder.addHeader("Authorization", "Bearer $anonKey")
        }
        val request = requestBuilder
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(rawBody) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                val msg = json.optString("error_description")
                    .ifBlank { json.optString("msg") }
                    .ifBlank { json.optString("message") }
                    .ifBlank { "Supabase Auth error (${response.code})." }
                throw SupabaseHttpException(response.code, msg)
            }
            return json
        }
    }

    private fun executeGetUser(url: String, anonKey: String, accessToken: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(rawBody) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                val msg = json.optString("msg")
                    .ifBlank { json.optString("message") }
                    .ifBlank { "Supabase user lookup error (${response.code})." }
                throw SupabaseHttpException(response.code, msg)
            }
            return json
        }
    }

    @VisibleForTesting
    internal fun parseSessionResponse(json: JSONObject): SupabaseUser {
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) {
            throw IllegalStateException("Supabase did not return an access token.")
        }
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val expiresIn = json.optLong("expires_in", 3600L).let { if (it <= 0L) 3600L else it }
        val rawExpiresAt = json.optLong("expires_at", 0L)
        val expiresAt = if (rawExpiresAt > 0L) rawExpiresAt else (System.currentTimeMillis() / 1000L) + expiresIn
        val userObj = json.optJSONObject("user") ?: JSONObject()
        return parseUserObject(userObj, accessToken, refreshToken, expiresAt)
    }

    private fun parseUserObject(
        userObj: JSONObject,
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochSeconds: Long
    ): SupabaseUser {
        val metadata = userObj.optJSONObject("user_metadata")
        val uid = userObj.optString("id").ifBlank { "sb_user" }
        val email = userObj.optString("email").takeIf { it.isNotBlank() }
        val displayName = metadata?.optString("full_name")?.takeIf { it.isNotBlank() }
            ?: metadata?.optString("name")?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")
        val avatarUrl = metadata?.optString("avatar_url")?.takeIf { it.isNotBlank() }
            ?: metadata?.optString("picture")?.takeIf { it.isNotBlank() }

        return SupabaseUser(
            uid = uid,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAtEpochSeconds
        )
    }

    @VisibleForTesting
    internal fun resolveSupabaseConfig(): Pair<String, String>? {
        return resolveConfig(
            urlCandidates = listOf(BuildConfig.NEXT_PUBLIC_SUPABASE_URL, BuildConfig.SUPABASE_URL),
            keyCandidates = listOf(BuildConfig.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY, BuildConfig.SUPABASE_ANON_KEY)
        )
    }

    @VisibleForTesting
    internal fun resolveConfig(
        urlCandidates: List<String>,
        keyCandidates: List<String>
    ): Pair<String, String>? {
        val url = urlCandidates
            .map { it.trim().trimEnd('/') }
            .firstOrNull {
                (it.startsWith("https://") || it.startsWith("http://")) &&
                    !it.contains("your-project.supabase.co", ignoreCase = true)
            }
            ?: return null
        val validKeys = keyCandidates
            .map { it.trim() }
            .filter {
                it.isNotBlank() &&
                    it != "YOUR_SUPABASE_ANON_KEY" &&
                    it != "YOUR_SUPABASE_PUBLISHABLE_KEY"
            }
        val key = validKeys.firstOrNull {
            it.startsWith("sb_publishable_") || it.startsWith("eyJ")
        } ?: validKeys.firstOrNull() ?: return null
        return url to key
    }

    @VisibleForTesting
    internal fun resolveGoogleWebClientId(context: Context): String? {
        val fromConfig = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (fromConfig.isNotBlank() && fromConfig != "YOUR_GOOGLE_WEB_CLIENT_ID") {
            return fromConfig
        }
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val fromRes = runCatching { context.getString(resId) }.getOrNull()?.trim()
            if (!fromRes.isNullOrBlank() && !fromRes.contains("placeholder", ignoreCase = true)) {
                return fromRes
            }
        }
        return null
    }

    @VisibleForTesting
    internal fun buildBrowserAuthorizeUrl(supabaseUrl: String, codeChallenge: String): String {
        val encodedRedirect = URLEncoder.encode(OAUTH_REDIRECT_URI, Charsets.UTF_8.name())
        return "$supabaseUrl/auth/v1/authorize" +
            "?provider=google" +
            "&redirect_to=$encodedRedirect" +
            "&code_challenge=$codeChallenge" +
            "&code_challenge_method=s256"
    }

    private fun persistUser(context: Context, user: SupabaseUser) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .putString(KEY_ACCESS_TOKEN, user.accessToken)
            .putString(KEY_REFRESH_TOKEN, user.refreshToken)
            .putLong(KEY_EXPIRES_AT, user.expiresAtEpochSeconds)
            .apply()
    }

    private fun loadPersistedUser(context: Context): SupabaseUser? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return SupabaseUser(
            uid = uid,
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            expiresAtEpochSeconds = prefs.getLong(KEY_EXPIRES_AT, 0L)
        )
    }

    private fun clearPersistedUser(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    @VisibleForTesting
    internal fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    @VisibleForTesting
    internal fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun extractFragmentParam(uri: Uri, key: String): String? {
        val fragment = uri.fragment ?: return null
        return fragment.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null
                else part.substring(0, idx) to Uri.decode(part.substring(idx + 1))
            }
            .firstOrNull { it.first == key }
            ?.second
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
