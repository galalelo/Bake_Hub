package com.bakehub.app.data

/**
 * Prototype data models for BakeHub.
 * These mirror the Firestore document shapes described in the design document
 * (Section 6 — Data captured and stored), simplified for a UI-only prototype
 * that runs entirely on mock, in-memory data (no network/database calls yet).
 */

data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String = ""
)

data class RecipeStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val timerSeconds: Int? = null
)

enum class Difficulty { EASY, MEDIUM, HARD }

data class Nutrition(
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float
)

data class Recipe(
    val id: String,
    val title: String,
    val authorName: String,
    val source: String = "BakeHub",
    val category: String,
    val difficulty: Difficulty,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val avgRating: Float,
    val ratingCount: Int,
    val nutrition: Nutrition,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>
) {
    val totalTimeMinutes: Int get() = prepTimeMinutes + cookTimeMinutes
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val unlocked: Boolean
)

data class GroceryItem(
    val id: String,
    val name: String,
    val quantity: String,
    val checked: Boolean = false
)

data class UserProfile(
    val displayName: String,
    val skillLevel: String,
    val level: Int,
    val xp: Int,
    val xpForNextLevel: Int,
    val streakCount: Int,
    val bakesCompleted: Int,
    val recipesShared: Int,
    val unitPreference: String
)
