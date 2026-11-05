package com.bakehub.app.data

import com.bakehub.app.data.firebase.FirebaseClients
import com.bakehub.app.data.repository.MealDbRepository
import com.bakehub.app.data.remote.Meal
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.asStateFlow
import com.bakehub.app.data.toRecipe
import com.bakehub.app.data.toFirestoreMap

/**
 * Firestore-backed recipe repository.
 * TheMealDB is used for external recipe searches.
 */
object RecipeRepository {

    private val db = FirebaseClients.firestore
    private val recipesRef = db.collection("recipes")

    private val _recipes =
        kotlinx.coroutines.flow.MutableStateFlow<List<Recipe>>(emptyList())

    val recipes: kotlinx.coroutines.flow.StateFlow<List<Recipe>> =
        _recipes.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Starts a live listener so recipes stay in sync with Firestore.
     */
    fun startListening() {
        if (listenerRegistration != null) return

        listenerRegistration = recipesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // A failed listener is terminated by Firestore; allow startListening() to re-attach.
                listenerRegistration?.remove()
                listenerRegistration = null
                return@addSnapshotListener
            }
            _recipes.value = snapshot?.documents
                ?.mapNotNull { it.toRecipe() }
                ?: emptyList()
        }
    }

    /** Detaches the listener (on logout / account deletion) so the next sign-in starts a fresh one. */
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _recipes.value = emptyList()
    }

    /**
     * Adds a user-created recipe to Firestore.
     */
    suspend fun addRecipe(recipe: Recipe) {
        recipesRef.document(recipe.id)
            .set(recipe.toFirestoreMap())
            .awaitOrQueue()
    }

    /**
     * Files a report against a user-published recipe (Google Play user-generated-content policy).
     * Stored in the write-only "reports" collection for moderators to review.
     */
    suspend fun reportRecipe(recipeId: String, reason: String) {
        val uid = FirebaseClients.auth.currentUser?.uid ?: return
        db.collection("reports")
            .add(
                mapOf(
                    "recipeId" to recipeId,
                    "reporterUid" to uid,
                    "reason" to reason,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .awaitOrQueue()
    }

    /**
     * Searches TheMealDB instead of Spoonacular.
     */
    suspend fun searchOnline(query: String): List<Recipe> {
        if (query.isBlank()) return emptyList()

        val mealDbRepository = MealDbRepository()

        val meals = mealDbRepository
            .searchRecipes(query.trim())
            .getOrThrow()

        return meals.map { it.toBakeHubRecipe() }
            .also { OnlineRecipeCache.putAll(it) }   // keep results openable offline
    }


    /**
     * Fetches random recipes from TheMealDB
     * and converts them into BakeHub Recipe objects.
     */


    /**
     * Fetches random dessert recipes from TheMealDB.
     */
    suspend fun getRandomOnlineRecipes(count: Int = 3): List<Recipe> {
        val mealDbRepository = MealDbRepository()

        // Get a random selection of desserts
        val desserts = mealDbRepository
            .getRandomDesserts(count)
            .getOrNull()
            .orEmpty()

        val fullRecipes = mutableListOf<Recipe>()

        // Fetch the complete details for each dessert
        for (dessert in desserts) {
            val mealId = dessert.idMeal

            if (!mealId.isNullOrBlank()) {
                val fullMeal = mealDbRepository
                    .getRecipeDetails(mealId)
                    .getOrNull()

                if (fullMeal != null) {
                    fullRecipes.add(fullMeal.toBakeHubRecipe())
                }
            }
        }

        return fullRecipes.distinctBy { it.id }
            .also { OnlineRecipeCache.putAll(it) }
    }


    /**
     * Retrieves a complete saved recipe from TheMealDB.
     */
    suspend fun getOnlineRecipeById(savedRecipeId: String): Recipe? {

        if (!savedRecipeId.startsWith("mealdb-")) {
            return null
        }

        val mealId = savedRecipeId.removePrefix("mealdb-")

        // Network first; fall back to the on-device copy when offline.
        return MealDbRepository()
            .getRecipeDetails(mealId)
            .getOrNull()
            ?.toBakeHubRecipe()
            ?.also { OnlineRecipeCache.put(it) }
            ?: OnlineRecipeCache.get(savedRecipeId)
    }









    /**
     * Looks up a recipe in the live Firestore list.
     */
    fun recipeById(id: String): Recipe? =
        _recipes.value.find { it.id == id }
}
