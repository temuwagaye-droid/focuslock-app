package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
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
        initialized = true
        val restored = loadPersistedUser(context)
        if (restored != null) {
            _currentUser.value = restored
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
            runCatching {
                val payload = JSONObject().put("refresh_token", refreshToken)
                val responseJson = executeAuthPost(
                    url = "${config.first}/auth/v1/token?grant_type=refresh_token",
                    anonKey = config.second,
                    body = payload
                )
                val updated = parseSessionResponse(responseJson)
                persistUser(context, updated)
                _currentUser.value = updated
            }
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<SupabaseGoogleSignInResult> {
        initialize(context)
        val config = resolveSupabaseConfig()
            ?: return Result.failure(
                IllegalStateException(
                    "Supabase is not configured yet. Add SUPABASE_URL and SUPABASE_ANON_KEY in the Secrets panel."
                )
            )
        val (supabaseUrl, anonKey) = config
        val googleWebClientId = resolveGoogleWebClientId(context)

        if (!googleWebClientId.isNullOrBlank()) {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(googleWebClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val user = exchangeGoogleIdTokenWithSupabase(
                        supabaseUrl = supabaseUrl,
                        anonKey = anonKey,
                        idToken = googleCredential.idToken
                    )
                    persistUser(context, user)
                    _currentUser.value = user
                    return Result.success(SupabaseGoogleSignInResult.SignedIn(user))
                }
            } catch (_: GetCredentialCancellationException) {
                return Result.failure(IllegalStateException("Google Sign-In cancelled."))
            } catch (_: NoCredentialException) {
                // Fall back to Supabase Google OAuth PKCE browser flow
            } catch (_: Exception) {
                // Fall back to Supabase Google OAuth PKCE browser flow
            }
        }

        return launchBrowserOAuthFlow(context, supabaseUrl)
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

            val encodedRedirect = URLEncoder.encode(OAUTH_REDIRECT_URI, Charsets.UTF_8.name())
            val authUrl = "$supabaseUrl/auth/v1/authorize" +
                "?provider=google" +
                "&redirect_to=$encodedRedirect" +
                "&code_challenge=$challenge" +
                "&code_challenge_method=s256"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(SupabaseGoogleSignInResult.BrowserFlowLaunched)
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

        val config = resolveSupabaseConfig()
            ?: return Result.failure(IllegalStateException("Supabase configuration missing."))
        val (supabaseUrl, anonKey) = config

        val errorDescription = uri.getQueryParameter("error_description")
            ?: extractFragmentParam(uri, "error_description")
        if (!errorDescription.isNullOrBlank()) {
            return Result.failure(IllegalStateException(errorDescription.replace("+", " ")))
        }

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
        idToken: String
    ): SupabaseUser = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)

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
                throw IllegalStateException(msg)
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
                throw IllegalStateException(msg)
            }
            return json
        }
    }

    private fun parseSessionResponse(json: JSONObject): SupabaseUser {
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) {
            throw IllegalStateException("Supabase did not return an access token.")
        }
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val expiresIn = json.optLong("expires_in", 3600L)
        val expiresAt = json.optLong("expires_at", (System.currentTimeMillis() / 1000L) + expiresIn)
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

    private fun resolveSupabaseConfig(): Pair<String, String>? {
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (url.isBlank() || url.contains("your-project.supabase.co", ignoreCase = true)) return null
        if (key.isBlank() || key == "YOUR_SUPABASE_ANON_KEY") return null
        return url to key
    }

    private fun resolveGoogleWebClientId(context: Context): String? {
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

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
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
}
