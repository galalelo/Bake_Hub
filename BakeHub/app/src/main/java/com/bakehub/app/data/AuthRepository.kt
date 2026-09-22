package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication (email/password) and creates the matching
 * per-user Firestore document ("users/{uid}") on sign-up.
 */
object AuthRepository {
    private val auth = FirebaseClients.auth
    private val db = FirebaseClients.firestore

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signUp(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Sign up failed — no user returned.")

        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        ).await()

        // Seed the Firestore profile doc that ProfileScreen / SettingsScreen read from.
        val initialProfile = mapOf(
            "displayName" to name.trim(),
            "skillLevel" to "Beginner",
            "level" to 1,
            "xp" to 0,
            "xpForNextLevel" to 500,
            "streakCount" to 0,
            "bakesCompleted" to 0,
            "recipesShared" to 0,
            "unitPreference" to "Metric",
            "savedRecipeIds" to emptyList<String>(),
            "offlineRecipeIds" to emptyList<String>(),
        )
        db.collection("users").document(user.uid).set(initialProfile).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    fun logout() {
        auth.signOut()
        UserRepository.reset()
    }
}
