package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
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
    private val functions = FirebaseFunctions.getInstance("europe-west1")

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


    /** Calls the secured Firebase Function; the third-party API key never ships in the APK. */
    suspend fun searchOnline(query: String): List<Recipe> {
        if (query.isBlank()) return emptyList()
        val result = functions.getHttpsCallable("searchExternalRecipes")
            .call(mapOf("query" to query.trim())).await()
        val rows = (result.data as? Map<*, *>)?.get("recipes") as? List<*> ?: return emptyList()
        return rows.mapNotNull { row ->
            val m = row as? Map<*, *> ?: return@mapNotNull null
            val ingredients = (m["ingredients"] as? List<*>)?.mapNotNull { raw ->
                val i = raw as? Map<*, *> ?: return@mapNotNull null
                Ingredient(i["name"] as? String ?: "", i["quantity"] as? String ?: "", i["unit"] as? String ?: "")
            }.orEmpty()
            Recipe(
                id = m["id"] as? String ?: return@mapNotNull null,
                title = m["title"] as? String ?: return@mapNotNull null,
                authorName = m["authorName"] as? String ?: "External source",
                source = m["source"] as? String ?: "External",
                category = m["category"] as? String ?: "Other",
                difficulty = Difficulty.EASY, prepTimeMinutes = 0, cookTimeMinutes = 0,
                servings = (m["servings"] as? Number)?.toInt() ?: 1, avgRating = 0f, ratingCount = 0,
                nutrition = Nutrition(0, 0f, 0f, 0f), ingredients = ingredients,
                steps = listOf(RecipeStep(1, "Instructions", m["instructions"] as? String ?: "See the original recipe.")),
                sourceUrl = m["sourceUrl"] as? String, imageUrl = m["imageUrl"] as? String
            )
        }
    }

    /** Falls back to MockData only if Firestore hasn't loaded yet (e.g. cold deep link). */
    fun recipeById(id: String): Recipe? =
        _recipes.value.find { it.id == id } ?: MockData.recipeById(id)
}
