package com.auravoice.tts.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.auravoice.tts.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun getWebClientId(context: Context): String {
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) { "" }

        return if (webClientId.isNotBlank()) {
            webClientId
        } else {
            "745117271430-0042tadrrotqj7h8aqmqesfp4mvg9vvn.apps.googleusercontent.com"
        }
    }

    fun getLegacyGoogleSignInIntent(context: Context): Intent {
        val webClientId = getWebClientId(context)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut()
        return googleSignInClient.signInIntent
    }

    fun handleLegacySignInResult(
        data: Intent?,
        onSuccess: (idToken: String, email: String, displayName: String?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: ""
            val displayName = account?.displayName

            if (!idToken.isNullOrEmpty()) {
                Log.d(TAG, "Legacy Google Sign-In success for email: $email")
                onSuccess(idToken, email, displayName)
            } else {
                Log.e(TAG, "Legacy Google Sign-In failed: No ID Token returned")
                onError("Google Sign-In failed: Could not retrieve ID token from account.")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Legacy Google Sign-In ApiException status code: ${e.statusCode}", e)
            if (e.statusCode == 12501 || e.statusCode == 4) {
                // User cancelled or back pressed
                Log.d(TAG, "Google Sign-In cancelled by user.")
                return
            }
            onError("Google Sign-In error (Code ${e.statusCode}): ${e.localizedMessage ?: "Sign in failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "Legacy Google Sign-In exception", e)
            onError("Google Sign-In error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun launchGoogleSignIn(
        context: Context,
        onSuccess: (idToken: String, email: String, displayName: String?) -> Unit,
        onError: (message: String) -> Unit,
        onFallbackToLegacy: () -> Unit
    ) {
        try {
            val activity = context.findActivity() ?: run {
                onFallbackToLegacy()
                return
            }

            val credentialManager = CredentialManager.create(activity)
            val webClientId = getWebClientId(activity)

            Log.d(TAG, "Attempting CredentialManager Google Sign-In with Web Client ID: $webClientId")

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = webClientId
            ).build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = activity, request = request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.d(TAG, "CredentialManager Google Sign-In success for email: ${googleIdTokenCredential.id}")
                onSuccess(
                    googleIdTokenCredential.idToken,
                    googleIdTokenCredential.id,
                    googleIdTokenCredential.displayName
                )
            } else {
                Log.w(TAG, "Invalid credential type, falling back to GoogleSignInClient")
                onFallbackToLegacy()
            }

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user.")
            // User cancelled — do nothing
        } catch (e: GetCredentialException) {
            Log.w(TAG, "CredentialManager failed (${e.message}), falling back to GoogleSignInClient", e)
            val msg = e.localizedMessage ?: e.message ?: ""
            if (msg.contains("user canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true) || msg.contains("16:", ignoreCase = true)) {
                return
            }
            onFallbackToLegacy()
        } catch (e: Exception) {
            Log.w(TAG, "CredentialManager unexpected error, falling back to GoogleSignInClient", e)
            onFallbackToLegacy()
        }
    }
}
