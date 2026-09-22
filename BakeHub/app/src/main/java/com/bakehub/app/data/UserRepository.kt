package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Live view of the signed-in user's Firestore profile document
 * ("users/{uid}"): display name/XP/level plus saved & offline recipe ids
 * (replaces MockData.currentUser / savedRecipeIds / offlineRecipeIds).
 */
object UserRepository {
    private val db = FirebaseClients.firestore
    private val auth = FirebaseClients.auth

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _savedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val savedRecipeIds: StateFlow<Set<String>> = _savedRecipeIds.asStateFlow()

    private val _offlineRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val offlineRecipeIds: StateFlow<Set<String>> = _offlineRecipeIds.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    /** Starts (or restarts, if the signed-in user changed) the live profile listener. */
    fun startListening() {
        val uid = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()
        listenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                _profile.value = snap.toUserProfile()
                _savedRecipeIds.value = snap.stringListOrEmpty("savedRecipeIds").toSet()
                _offlineRecipeIds.value = snap.stringListOrEmpty("offlineRecipeIds").toSet()
            }
    }

    fun reset() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _profile.value = null
        _savedRecipeIds.value = emptySet()
        _offlineRecipeIds.value = emptySet()
    }

    suspend fun toggleSaved(recipeId: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        if (_savedRecipeIds.value.contains(recipeId)) {
            ref.update("savedRecipeIds", FieldValue.arrayRemove(recipeId)).await()
        } else {
            ref.update("savedRecipeIds", FieldValue.arrayUnion(recipeId)).await()
        }
    }

    suspend fun setUnitPreference(preference: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("unitPreference", preference).await()
    }
}
