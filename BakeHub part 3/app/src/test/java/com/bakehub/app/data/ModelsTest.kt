package com.bakehub.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private fun sampleRecipe(prep: Int = 15, cook: Int = 45) = Recipe(
        id = "r1", title = "Banana Bread", authorName = "Alice", category = "Bread",
        difficulty = Difficulty.EASY, prepTimeMinutes = prep, cookTimeMinutes = cook, servings = 8,
        avgRating = 4.5f, ratingCount = 12, nutrition = Nutrition(250, 4f, 40f, 9f),
        ingredients = listOf(Ingredient("Flour", "2", "cups")),
        steps = listOf(RecipeStep(1, "Mix", "Mix everything.", 60)),
        sourceUrl = "https://example.com", imageUrl = null,
    )

    @Test
    fun `total time is prep plus cook`() {
        assertEquals(60, sampleRecipe(15, 45).totalTimeMinutes)
        assertEquals(0, sampleRecipe(0, 0).totalTimeMinutes)
    }

    @Test
    fun `default profile starts at level 1 with empty collections`() {
        val p = defaultProfileFields("  Sam  ")
        assertEquals("Sam", p["displayName"])
        assertEquals(1, p["level"])
        assertEquals(0, p["xp"])
        assertEquals(500, p["xpForNextLevel"])
        assertEquals("Metric", p["unitPreference"])
        assertEquals(emptyList<String>(), p["savedRecipeIds"])
    }

    @Test
    fun `default profile falls back to Baker for blank names`() {
        assertEquals("Baker", defaultProfileFields("   ")["displayName"])
    }

    @Test
    fun `recipe survives a JSON round trip for the offline cache`() {
        val original = sampleRecipe()
        val decoded = RecipeJsonCodec.decode(RecipeJsonCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `offline cache ignores corrupt JSON`() {
        assertEquals(null, RecipeJsonCodec.decode("not json"))
        assertEquals(null, RecipeJsonCodec.decode("{}"))
        assertTrue(RecipeJsonCodec.decode("") == null)
    }
}
