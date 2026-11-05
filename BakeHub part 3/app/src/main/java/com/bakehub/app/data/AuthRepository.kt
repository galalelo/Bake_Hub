package com.bakehub.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.bakehub.app.data.firebase.FirebaseClients
import com.bakehub.app.notifications.AppNotifications
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication - email/password and Google single sign-on (SSO) - and creates the
 * matching per-user Firestore document ("users/{uid}") the first time someone signs in.
 */
object AuthRepository {
    private val auth = FirebaseClients.auth
    private val db = FirebaseClients.firestore

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signUp(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Sign up failed - no user returned.")

        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        ).await()

        db.collection("users").document(user.uid).set(defaultProfileFields(name)).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    // ---------------------------------------------------------------- Google SSO

    /**
     * Single sign-on with a Google account through Android's Credential Manager, exchanged for a
     * Firebase credential. [serverClientId] is the *web* OAuth client ID of the Firebase project
     * (generated as `default_web_client_id` once Google sign-in is enabled - see README).
     */
    suspend fun signInWithGoogle(activityContext: Context, serverClientId: String) {
        val credential = googleFirebaseCredential(activityContext, serverClientId)
        val user = auth.signInWithCredential(credential).await().user
            ?: error("Google sign-in failed - no user returned.")
        ensureProfile(user)
    }

    private suspend fun googleFirebaseCredential(
        activityContext: Context,
        serverClientId: String
    ): AuthCredential {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val manager = CredentialManager.create(activityContext)
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            manager.getCredential(activityContext, request)
        } catch (e: NoCredentialException) {
            // No account was offered by the bottom sheet. Fall back to the full "Sign in with
            // Google" flow, which can also pick or add an account instead of failing outright.
            val fallback = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
                .build()
            manager.getCredential(activityContext, fallback)
        }

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleId = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAuthProvider.getCredential(googleId.idToken, null)
        }
        error("Unexpected credential type returned by Google sign-in.")
    }

    /** Creates the Firestore profile for first-time SSO users; leaves existing profiles untouched. */
    private suspend fun ensureProfile(user: FirebaseUser) {
        val ref = db.collection("users").document(user.uid)
        if (!ref.get().await().exists()) {
            val name = user.displayName ?: user.email?.substringBefore('@') ?: "Baker"
            ref.set(defaultProfileFields(name)).await()
        }
    }

    // ---------------------------------------------------------------- sign out / delete

    suspend fun logout(context: Context? = null) {
        AppNotifications.onLogout()
        auth.signOut()
        UserRepository.reset()
        RecipeRepository.stopListening()
        if (context != null) {
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    /**
     * Permanently deletes the signed-in user's account and data (required by Google Play for apps
     * that let people create accounts). Re-authenticates first because Firebase only allows
     * account deletion shortly after a sign-in. Removes: the grocery list, the profile document,
     * the user's published recipes (best effort) and finally the Firebase Auth user.
     *
     * @param password required for email/password accounts; ignored for Google accounts.
     */
    suspend fun deleteAccount(context: Context, password: String?, serverClientId: String?) {
        val user = auth.currentUser ?: error("Not signed in.")
        val uid = user.uid

        val credential: AuthCredential =
            if (user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
                require(!password.isNullOrEmpty()) { "Password required." }
                EmailAuthProvider.getCredential(user.email.orEmpty(), password)
            } else {
                googleFirebaseCredential(
                    context,
                    requireNotNull(serverClientId) { "Google sign-in is not configured." }
                )
            }
        user.reauthenticate(credential).await()

        AppNotifications.onLogout()
        UserRepository.reset()
        RecipeRepository.stopListening()

        // User-published recipes (best effort - never block account deletion on this).
        runCatching {
            val name = user.displayName
            if (!name.isNullOrBlank()) {
                val mine = db.collection("recipes")
                    .whereEqualTo("authorName", name)
                    .whereEqualTo("source", "User")
                    .get().await()
                val batch = db.batch()
                mine.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }

        val userDoc = db.collection("users").document(uid)
        val groceries = userDoc.collection("groceryItems").get().await()
        val batch = db.batch()
        groceries.documents.forEach { batch.delete(it.reference) }
        batch.delete(userDoc)
        batch.commit().await()

        user.delete().await()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
