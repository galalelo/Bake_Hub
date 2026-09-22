package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed replacement for the recipe portion of MockData.
 * Collection shape: "recipes/{recipeId}" — see FirestoreMappers.kt for the field layout.
 */
object RecipeRepository {
    private val db = FirebaseClients.firestore
    private val recipesRef = db.collection("recipes")

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    /** Starts a live listener so `recipes` stays in sync with Firestore. Safe to call repeatedly. */
    fun startListening() {
        if (listenerRegistration != null) return
        listenerRegistration = recipesRef.addSnapshotListener { snapshot, _ ->
            _recipes.value = snapshot?.documents?.mapNotNull { it.toRecipe() } ?: emptyList()
        }
    }

    /**
     * One-time bootstrap: if the "recipes" collection is empty (fresh Firebase project),
     * upload the built-in sample recipes so the app isn't blank on first run.
     */
    suspend fun seedIfEmpty() {
        val snapshot = recipesRef.limit(1).get().await()
        if (snapshot.isEmpty) {
            for (recipe in MockData.recipes) {
                recipesRef.document(recipe.id).set(recipe.toFirestoreMap()).await()
            }
        }
    }

    suspend fun addRecipe(recipe: Recipe) {
        recipesRef.document(recipe.id).set(recipe.toFirestoreMap()).await()
    }

    /** Falls back to MockData only if Firestore hasn't loaded yet (e.g. cold deep link). */
    fun recipeById(id: String): Recipe? =
        _recipes.value.find { it.id == id } ?: MockData.recipeById(id)
}
