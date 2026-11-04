package com.bakehub.app.data

/** Starting values for a brand-new user's Firestore profile document ("users/{uid}"). */
fun defaultProfileFields(displayName: String): Map<String, Any> = mapOf(
    "displayName" to displayName.trim().ifBlank { "Baker" },
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
