package com.example.data.remote

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }.addOnFailureListener { ex ->
        if (cont.isActive) cont.resumeWithException(ex)
    }.addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

object FirebaseService {

    fun auth(): FirebaseAuth = Firebase.auth

    fun firestore(context: Context): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val webClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
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
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth().signInWithCredential(firebaseCredential).awaitResult()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Google Sign-In completed without user profile."))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned."))
            }
        } catch (_: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Sign-in cancelled."))
        } catch (_: NoCredentialException) {
            Result.failure(IllegalStateException("No Google account found on this device. Add a Google account in Settings to sign in."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Google Sign-In failed."))
        }
    }

    suspend fun signOut(context: Context) {
        auth().signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
        }
    }

    fun handleFirestoreError(e: Exception, operation: String): String {
        return when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission denied during $operation. Verify you are signed in with the right account."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Cloud service temporarily unavailable during $operation. Check your network connection."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Requested record was not found ($operation)."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Cloud request timed out during $operation. Please retry."
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Cloud index or condition missing for $operation."
            else -> e.localizedMessage ?: "Cloud error during $operation."
        }
    }
}
