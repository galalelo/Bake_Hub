package com.bakehub.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealMapperTest {

    @Test
    fun `maps basic TheMealDB fields into a BakeHub recipe`() {
        val recipe = meal(id = "123", name = "Apple Pie", category = "Dessert").toBakeHubRecipe()

        assertEquals("mealdb-123", recipe.id)
        assertEquals("Apple Pie", recipe.title)
        assertEquals("TheMealDB", recipe.source)
        assertEquals("TheMealDB", recipe.authorName)
        assertEquals("Dessert", recipe.category)
        assertEquals("https://example.com/cake.jpg", recipe.imageUrl)
    }

    @Test
    fun `missing category falls back to Other`() {
        assertEquals("Other", meal(category = null).toBakeHubRecipe().category)
    }

    @Test
    fun `keeps ingredients with measures and skips blank or null ones`() {
        val recipe = meal(
            ingredients = listOf(
                "Flour" to "200g",
                "" to "1 tsp",
                "  Sugar  " to "  100g ",
                null to null,
                "Eggs" to null,
            )
        ).toBakeHubRecipe()

        assertEquals(listOf("Flour", "Sugar", "Eggs"), recipe.ingredients.map { it.name })
        assertEquals(listOf("200g", "100g", ""), recipe.ingredients.map { it.quantity })
    }

    @Test
    fun `blank instructions produce a single placeholder step`() {
        val recipe = meal(instructions = "   ").toBakeHubRecipe()
        assertEquals(1, recipe.steps.size)
        assertEquals("No instructions available.", recipe.steps[0].instruction)
    }

    @Test
    fun `splits instructions into sentences and numbers the steps`() {
        val steps = splitInstructions("Preheat the oven. Mix the flour and sugar.\nPour into a tin.")
        assertEquals(3, steps.size)
        assertEquals(listOf(1, 2, 3), steps.map { it.stepNumber })
        assertEquals("Preheat the oven.", steps[0].instruction)
        assertEquals("Pour into a tin.", steps[2].instruction)
    }

    @Test
    fun `removes standalone and inline step numbering`() {
        val steps = splitInstructions("Step 1\nMix the batter.\n2. Bake it.\nSTEP 3: Cool down.")
        assertEquals(listOf("Mix the batter.", "Bake it.", "Cool down."), steps.map { it.instruction })
    }

    @Test
    fun `strips html line breaks and tags from instructions`() {
        val steps = splitInstructions("Whisk eggs.<br>Add <b>milk</b>.")
        assertEquals(listOf("Whisk eggs.", "Add milk."), steps.map { it.instruction })
    }

    @Test
    fun `extracts a step timer in seconds`() {
        assertEquals(20 * 60, extractStepTimerSeconds("Bake for 20 minutes until golden."))
        assertEquals(2 * 3600, extractStepTimerSeconds("Leave to prove for 2 hours."))
        assertEquals(45, extractStepTimerSeconds("Rest for 45 seconds."))
    }

    @Test
    fun `uses the midpoint of a duration range`() {
        assertEquals(12 * 60 + 30, extractStepTimerSeconds("Simmer for 10-15 minutes."))
    }

    @Test
    fun `no timer when the step has no duration`() {
        assertNull(extractStepTimerSeconds("Mix the flour and butter."))
    }

    @Test
    fun `estimates total recipe time from numeric durations`() {
        assertEquals(35, estimateRecipeTime("Chill for 15 minutes then bake for 20 mins."))
        assertEquals(90, estimateRecipeTime("Bake for 1.5 hours."))
        assertEquals(0, estimateRecipeTime("Mix everything together."))
    }

    @Test
    fun `estimates total recipe time from written durations`() {
        assertEquals(60, estimateRecipeTime("Leave to rise for an hour."))
        assertEquals(30, estimateRecipeTime("Cool for half an hour."))
    }

    @Test
    fun `step timers are attached to the mapped recipe`() {
        val recipe = meal(instructions = "Mix the dough. Bake for 30 minutes.").toBakeHubRecipe()
        assertNull(recipe.steps[0].timerSeconds)
        assertEquals(30 * 60, recipe.steps[1].timerSeconds)
        assertTrue(recipe.prepTimeMinutes >= 30)
    }
}
