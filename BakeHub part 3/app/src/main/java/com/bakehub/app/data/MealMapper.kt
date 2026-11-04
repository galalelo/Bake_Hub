package com.bakehub.app.data

import com.bakehub.app.data.remote.Meal

/**
 * Pure (Android-free) mapping from TheMealDB's response model to BakeHub's
 * [Recipe]. Kept out of RecipeRepository so it can be unit-tested on the JVM
 * without initialising Firebase.
 */

/**
 * Converts a TheMealDB meal into BakeHub's Recipe model.
 */

fun Meal.toBakeHubRecipe(): Recipe {

    // Convert TheMealDB ingredients and measurements.
    val ingredients = listOf(
        strIngredient1 to strMeasure1,
        strIngredient2 to strMeasure2,
        strIngredient3 to strMeasure3,
        strIngredient4 to strMeasure4,
        strIngredient5 to strMeasure5,
        strIngredient6 to strMeasure6,
        strIngredient7 to strMeasure7,
        strIngredient8 to strMeasure8,
        strIngredient9 to strMeasure9,
        strIngredient10 to strMeasure10,
        strIngredient11 to strMeasure11,
        strIngredient12 to strMeasure12,
        strIngredient13 to strMeasure13,
        strIngredient14 to strMeasure14,
        strIngredient15 to strMeasure15,
        strIngredient16 to strMeasure16,
        strIngredient17 to strMeasure17,
        strIngredient18 to strMeasure18,
        strIngredient19 to strMeasure19,
        strIngredient20 to strMeasure20
    ).mapNotNull { (name, measure) ->

        val ingredientName = name?.trim()

        if (ingredientName.isNullOrBlank()) {
            null
        } else {
            Ingredient(
                name = ingredientName,
                quantity = measure?.trim().orEmpty(),
                unit = ""
            )
        }
    }

    // Get the original instructions.
    val instructions = strInstructions
        ?.takeIf { it.isNotBlank() }
        ?: "No instructions available."

    // Separate instructions into individual steps.
    val steps = splitInstructions(instructions)

    // Estimate recipe time from explicit time references.
    val estimatedTime = estimateRecipeTime(instructions)

    return Recipe(
        id = "mealdb-$idMeal",
        title = strMeal,
        authorName = "TheMealDB",
        source = "TheMealDB",
        category = strCategory ?: "Other",
        difficulty = Difficulty.EASY,

        // Estimated time, not an official TheMealDB value.
        prepTimeMinutes = estimatedTime,
        cookTimeMinutes = 0,

        servings = 1,
        avgRating = 0f,
        ratingCount = 0,
        nutrition = Nutrition(0, 0f, 0f, 0f),
        ingredients = ingredients,
        steps = steps,
        sourceUrl = null,
        imageUrl = strMealThumb
    )
}

/**
 * Splits a long instruction paragraph into individual sentences.
 */

internal fun splitInstructions(instructions: String): List<RecipeStep> {

    val cleaned = instructions
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace(Regex("(?i)&nbsp;"), " ")
        .trim()

    if (cleaned.isBlank()) {
        return listOf(
            RecipeStep(1, "", "No instructions available.")
        )
    }

    // Remove standalone headings such as "Step 5" or "5."
    val withoutStepHeadings = cleaned
        .replace(
            Regex("(?im)^\\s*(?:step\\s*)?\\d+\\s*[:.)]?\\s*$"),
            ""
        )

    val paragraphs = withoutStepHeadings
        .split(Regex("\\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val individualSteps = mutableListOf<String>()

    for (paragraph in paragraphs) {

        // Remove inline numbering such as "Step 5: Add the flour."
        val withoutNumber = paragraph.replaceFirst(
            Regex("(?i)^\\s*(?:step\\s*)?\\d+\\s*[:.)-]\\s*"),
            ""
        ).trim()

        if (withoutNumber.isBlank()) continue

        // Split long paragraphs into individual sentences.
        val sentences = withoutNumber
            .split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9])"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        individualSteps.addAll(sentences)
    }

    return individualSteps.mapIndexed { index, instruction ->
        RecipeStep(
            stepNumber = index + 1,
            title = "",
            instruction = instruction,
            timerSeconds = extractStepTimerSeconds(instruction)
        )
    }
}

internal fun extractStepTimerSeconds(instruction: String): Int? {

    val pattern = Regex(
        "(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to\\s+)?\\s*(\\d+(?:\\.\\d+)?)?\\s*(hours?|hrs?|minutes?|mins?|seconds?|secs?)\\b"
    )

    val match = pattern.find(instruction) ?: return null

    val first = match.groupValues[1].toDoubleOrNull() ?: return null
    val second = match.groupValues[2].toDoubleOrNull()

    // Use the midpoint for durations such as 10-15 minutes.
    val amount = if (second != null) {
        (first + second) / 2.0
    } else {
        first
    }

    val unit = match.groupValues[3].lowercase()

    val seconds = when {
        unit.startsWith("hour") || unit.startsWith("hr") ->
            amount * 3600

        unit.startsWith("minute") || unit.startsWith("min") ->
            amount * 60

        else -> amount
    }

    return seconds.toInt().takeIf { it > 0 }
}



/**
 * Estimates total time from durations explicitly mentioned
 * in the recipe instructions.
 */

internal fun estimateRecipeTime(instructions: String): Int {

    var totalMinutes = 0

    // Recognise numeric durations and ranges.
    val numericPattern = Regex(
        "(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to\\s+)?\\s*(\\d+(?:\\.\\d+)?)?\\s*(hours?|hrs?|minutes?|mins?)\\b"
    )

    numericPattern.findAll(instructions).forEach { match ->

        val firstAmount = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val secondAmount = match.groupValues[2].toDoubleOrNull()
        val unit = match.groupValues[3].lowercase()

        // Use the midpoint when a duration is given as a range.
        val amount = if (secondAmount != null) {
            (firstAmount + secondAmount) / 2.0
        } else {
            firstAmount
        }

        val minutes = if (
            unit.startsWith("hour") || unit.startsWith("hr")
        ) {
            amount * 60
        } else {
            amount
        }

        totalMinutes += minutes.toInt()
    }

    // Recognise common written durations.
    val wordPattern = Regex(
        "(?i)\\b(a couple of|couple of|a few|few|half an?|an?|a)\\s+(hours?|hrs?|minutes?|mins?)\\b"
    )

    wordPattern.findAll(instructions).forEach { match ->

        val amountText = match.groupValues[1].lowercase()
        val unit = match.groupValues[2].lowercase()

        val amount = when (amountText) {
            "a couple of", "couple of" -> 2.0
            "a few", "few" -> 3.0
            "half an", "half a" -> 0.5
            "an", "a" -> 1.0
            else -> 0.0
        }

        val minutes = if (
            unit.startsWith("hour") || unit.startsWith("hr")
        ) {
            amount * 60
        } else {
            amount
        }

        totalMinutes += minutes.toInt()
    }

    return totalMinutes
}
