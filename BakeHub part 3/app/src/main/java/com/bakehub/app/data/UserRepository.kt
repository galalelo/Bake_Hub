
package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserRepository {

    private val db = FirebaseClients.firestore
    private val auth = FirebaseClients.auth

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _savedRecipeIds =
        MutableStateFlow<Set<String>>(emptySet())
    val savedRecipeIds: StateFlow<Set<String>> =
        _savedRecipeIds.asStateFlow()

    private val _offlineRecipeIds =
        MutableStateFlow<Set<String>>(emptySet())
    val offlineRecipeIds: StateFlow<Set<String>> =
        _offlineRecipeIds.asStateFlow()

    // Grocery list
    private val _groceryItems =
        MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceryItems: StateFlow<List<GroceryItem>> =
        _groceryItems.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var groceryListenerRegistration: ListenerRegistration? = null

    // Firestore references
    private fun groceryCollection() =
        db.collection("users")
            .document(auth.currentUser?.uid ?: "")
            .collection("groceryItems")

    // Start listening to the user's profile and grocery list
    fun startListening() {

        val uid = auth.currentUser?.uid ?: return

        listenerRegistration?.remove()
        groceryListenerRegistration?.remove()

        val userDocument = db.collection("users").document(uid)

        // Profile listener
        listenerRegistration = userDocument
            .addSnapshotListener { snap, _ ->

                if (snap == null || !snap.exists()) return@addSnapshotListener

                _profile.value = snap.toUserProfile()

                _savedRecipeIds.value =
                    snap.stringListOrEmpty("savedRecipeIds").toSet()

                _offlineRecipeIds.value =
                    snap.stringListOrEmpty("offlineRecipeIds").toSet()
            }

        // Grocery list listener
        groceryListenerRegistration = userDocument
            .collection("groceryItems")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null) return@addSnapshotListener

                _groceryItems.value = snapshot.documents.mapNotNull { document ->

                    val name = document.getString("name") ?: return@mapNotNull null
                    val quantity = document.getString("quantity") ?: ""

                    GroceryItem(
                        id = document.id,
                        name = name,
                        quantity = quantity,
                        checked = document.getBoolean("checked") ?: false
                    )
                }
            }
    }

    // Reset listeners and local data
    fun reset() {

        listenerRegistration?.remove()
        listenerRegistration = null

        groceryListenerRegistration?.remove()
        groceryListenerRegistration = null

        _profile.value = null
        _savedRecipeIds.value = emptySet()
        _offlineRecipeIds.value = emptySet()
        _groceryItems.value = emptyList()
    }

    // Save a recipe
    suspend fun toggleSaved(recipeId: String) {

        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)

        // Update local state first so the UI is correct immediately, even while offline.
        if (_savedRecipeIds.value.contains(recipeId)) {
            _savedRecipeIds.value = _savedRecipeIds.value - recipeId
            ref.update(
                "savedRecipeIds",
                FieldValue.arrayRemove(recipeId)
            ).awaitOrQueue()
        } else {
            _savedRecipeIds.value = _savedRecipeIds.value + recipeId
            ref.update(
                "savedRecipeIds",
                FieldValue.arrayUnion(recipeId)
            ).awaitOrQueue()
        }
    }

    // Add a grocery item
    suspend fun addGroceryItem(name: String, quantity: String) {

        val uid = auth.currentUser?.uid ?: return

        val data = hashMapOf(
            "name" to name,
            "quantity" to quantity,
            "checked" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(uid)
            .collection("groceryItems")
            .add(data)
            .awaitOrQueue()
    }

    // Update a grocery item
    suspend fun updateGroceryItem(item: GroceryItem) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("groceryItems")
            .document(item.id)
            .update(
                mapOf(
                    "name" to item.name,
                    "quantity" to item.quantity,
                    "checked" to item.checked
                )
            )
            .awaitOrQueue()
    }

    // Update checkbox status
    suspend fun setGroceryItemChecked(
        itemId: String,
        checked: Boolean
    ) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("groceryItems")
            .document(itemId)
            .update("checked", checked)
            .awaitOrQueue()
    }

    // Delete a grocery item
    suspend fun deleteGroceryItem(itemId: String) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("groceryItems")
            .document(itemId)
            .delete()
            .awaitOrQueue()
    }

    // Update measurement preference
    suspend fun setUnitPreference(preference: String) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .update("unitPreference", preference)
            .awaitOrQueue()
    }
}
