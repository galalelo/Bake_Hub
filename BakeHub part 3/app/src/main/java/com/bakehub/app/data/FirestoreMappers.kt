package com.bakehub.app.data

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Manual (explicit) mapping between the app's Kotlin models and the plain
 * Map<String, Any?> shape Firestore stores. Written by hand rather than using
 * Firestore's automatic POJO mapping so enum handling and malformed/partial
 * documents are predictable.
 */

fun Recipe.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "authorName" to authorName,
    "source" to source,
    "category" to category,
    "difficulty" to difficulty.name,
    "prepTimeMinutes" to prepTimeMinutes,
    "cookTimeMinutes" to cookTimeMinutes,
    "servings" to servings,
    "avgRating" to avgRating.toDouble(),
    "ratingCount" to ratingCount,
    "nutrition" to mapOf(
        "calories" to nutrition.calories,
        "proteinG" to nutrition.proteinG.toDouble(),
        "carbsG" to nutrition.carbsG.toDouble(),
        "fatG" to nutrition.fatG.toDouble(),
    ),
    "ingredients" to ingredients.map {
        mapOf("name" to it.name, "quantity" to it.quantity, "unit" to it.unit)
    },
    "sourceUrl" to sourceUrl,
    "imageUrl" to imageUrl,
    "steps" to steps.map {
        mapOf(
            "stepNumber" to it.stepNumber,
            "title" to it.title,
            "instruction" to it.instruction,
            "timerSeconds" to it.timerSeconds,
        )
    },
)

fun DocumentSnapshot.toRecipe(): Recipe? {
    return try {
        val nutritionMap = get("nutrition") as? Map<*, *>
        val ingredientsList = get("ingredients") as? List<*>
        val stepsList = get("steps") as? List<*>

        Recipe(
            id = id,
            title = getString("title") ?: return null,
            authorName = getString("authorName") ?: "Unknown",
            source = getString("source") ?: "BakeHub",
            sourceUrl = getString("sourceUrl"),
            imageUrl = getString("imageUrl"),
            category = getString("category") ?: "Other",
            difficulty = runCatching { Difficulty.valueOf(getString("difficulty") ?: "EASY") }
                .getOrDefault(Difficulty.EASY),
            prepTimeMinutes = (getLong("prepTimeMinutes") ?: 0L).toInt(),
            cookTimeMinutes = (getLong("cookTimeMinutes") ?: 0L).toInt(),
            servings = (getLong("servings") ?: 1L).toInt(),
            avgRating = (getDouble("avgRating") ?: 0.0).toFloat(),
            ratingCount = (getLong("ratingCount") ?: 0L).toInt(),
            nutrition = Nutrition(
                calories = ((nutritionMap?.get("calories") as? Long) ?: 0L).toInt(),
                proteinG = ((nutritionMap?.get("proteinG") as? Number)?.toFloat() ?: 0f),
                carbsG = ((nutritionMap?.get("carbsG") as? Number)?.toFloat() ?: 0f),
                fatG = ((nutritionMap?.get("fatG") as? Number)?.toFloat() ?: 0f),
            ),
            ingredients = ingredientsList.orEmpty().mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                Ingredient(
                    name = m["name"] as? String ?: "",
                    quantity = m["quantity"] as? String ?: "",
                    unit = m["unit"] as? String ?: "",
                )
            },
            steps = stepsList.orEmpty().mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                RecipeStep(
                    stepNumber = ((m["stepNumber"] as? Long) ?: 0L).toInt(),
                    title = m["title"] as? String ?: "",
                    instruction = m["instruction"] as? String ?: "",
                    timerSeconds = (m["timerSeconds"] as? Long)?.toInt(),
                )
            },
        )
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
    displayName = getString("displayName") ?: "Baker",
    skillLevel = getString("skillLevel") ?: "Beginner",
    level = (getLong("level") ?: 1L).toInt(),
    xp = (getLong("xp") ?: 0L).toInt(),
    xpForNextLevel = (getLong("xpForNextLevel") ?: 500L).toInt(),
    streakCount = (getLong("streakCount") ?: 0L).toInt(),
    bakesCompleted = (getLong("bakesCompleted") ?: 0L).toInt(),
    recipesShared = (getLong("recipesShared") ?: 0L).toInt(),
    unitPreference = getString("unitPreference") ?: "Metric",
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.stringListOrEmpty(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
