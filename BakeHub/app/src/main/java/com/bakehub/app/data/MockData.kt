package com.bakehub.app.data


import kotlin.random.Random
/**
 * Static mock data used to drive the prototype UI end-to-end without a backend.
 * In the full build this is replaced by calls to the BakeHub REST API
 * (see design document Section 4) backed by Firestore.
 */
object MockData {

    val currentUser = UserProfile(
        displayName = "Alex Baker",

        //random mock data just for interest sake
        skillLevel = listOf(
            "Beginner",
            "Intermediate",
            "Advanced",
            "Expert"
        ).random(),

        level = Random.nextInt(1, 11), // Random level from 1 to 10

        xp = Random.nextInt(0, 1501),

        xpForNextLevel = 1500,

        streakCount = Random.nextInt(0, 31), // 0–30 days

        bakesCompleted = Random.nextInt(0, 101), // 0–100 bakes

        recipesShared = Random.nextInt(1, 51), // 0–50 recipes

        unitPreference = "Metric"
    )

    val badges = listOf(
        Badge("b1", "Bread Beginner", "Bake 5 bread recipes", unlocked = true),
        Badge("b2", "7-Day Streak", "Bake on 7 consecutive days", unlocked = true),
        Badge("b3", "Sourdough Starter", "Complete the Sourdough Week challenge", unlocked = true),
        Badge("b4", "Pastry Pro", "Bake 10 pastry recipes", unlocked = false),
    )

    //Recipes mock data
    private val bananaBread = Recipe(
        id = "r1",
        title = "Classic Banana Bread",
        authorName = "BakeHub",
        category = "Bread",
        difficulty = Difficulty.EASY,
        prepTimeMinutes = 15,
        cookTimeMinutes = 50,
        servings = 8,
        avgRating = 4.8f,
        ratingCount = 312,
        nutrition = Nutrition(calories = 210, proteinG = 4f, carbsG = 32f, fatG = 7f),
        ingredients = listOf(
            Ingredient("Flour", "2", "cups"),
            Ingredient("Ripe bananas", "3", ""),
            Ingredient("Eggs", "2", ""),
            Ingredient("Sugar", "1/2", "cup"),
            Ingredient("Butter, melted", "1/3", "cup"),
            Ingredient("Baking soda", "1", "tsp"),
        ),
        steps = listOf(
            RecipeStep(1, "Preheat", "Preheat the oven to 175°C and grease a loaf tin.", timerSeconds = null),
            RecipeStep(2, "Mash bananas", "Mash the bananas in a large bowl until smooth.", timerSeconds = null),
            RecipeStep(3, "Cream butter and sugar", "Beat butter and sugar together until light and fluffy.", timerSeconds = 180),
            RecipeStep(4, "Combine wet ingredients", "Mix in the eggs and mashed banana until combined.", timerSeconds = null),
            RecipeStep(5, "Fold in dry ingredients", "Gently fold in the flour and baking soda until just combined.", timerSeconds = null),
            RecipeStep(6, "Bake", "Pour into the tin and bake until a skewer comes out clean.", timerSeconds = 3000),
            RecipeStep(7, "Cool", "Cool in the tin for 10 minutes, then turn out onto a rack.", timerSeconds = 600),
        )
    )

    private val chocChipCookies = Recipe(
        id = "r2",
        title = "Chewy Chocolate Chip Cookies",
        authorName = "BakeHub",
        category = "Cookies",
        difficulty = Difficulty.EASY,
        prepTimeMinutes = 15,
        cookTimeMinutes = 10,
        servings = 18,
        avgRating = 4.9f,
        ratingCount = 587,
        nutrition = Nutrition(calories = 165, proteinG = 2f, carbsG = 21f, fatG = 8f),
        ingredients = listOf(
            Ingredient("Flour", "2 1/4", "cups"),
            Ingredient("Butter, softened", "1", "cup"),
            Ingredient("Brown sugar", "3/4", "cup"),
            Ingredient("White sugar", "3/4", "cup"),
            Ingredient("Eggs", "2", ""),
            Ingredient("Chocolate chips", "2", "cups"),
        ),
        steps = listOf(
            RecipeStep(1, "Preheat", "Preheat the oven to 190°C and line two baking trays.", timerSeconds = null),
            RecipeStep(2, "Cream butter and sugars", "Beat the butter, brown sugar, and white sugar until fluffy.", timerSeconds = 180),
            RecipeStep(3, "Add eggs", "Beat in the eggs one at a time.", timerSeconds = null),
            RecipeStep(4, "Mix dough", "Stir in the flour, then fold through the chocolate chips.", timerSeconds = null),
            RecipeStep(5, "Portion", "Scoop tablespoon-sized balls of dough onto the trays.", timerSeconds = null),
            RecipeStep(6, "Bake", "Bake until the edges are golden but centres still look soft.", timerSeconds = 600),
        )
    )

    private val sourdough = Recipe(
        id = "r3",
        title = "Beginner Sourdough Loaf",
        authorName = "BakeHub",
        category = "Bread",
        difficulty = Difficulty.HARD,
        prepTimeMinutes = 30,
        cookTimeMinutes = 45,
        servings = 10,
        avgRating = 4.6f,
        ratingCount = 201,
        nutrition = Nutrition(calories = 180, proteinG = 6f, carbsG = 35f, fatG = 1f),
        ingredients = listOf(
            Ingredient("Active sourdough starter", "100", "g"),
            Ingredient("Bread flour", "500", "g"),
            Ingredient("Water", "350", "ml"),
            Ingredient("Salt", "10", "g"),
        ),
        steps = listOf(
            RecipeStep(1, "Mix", "Combine starter, flour, and water; rest for 30 minutes.", timerSeconds = 1800),
            RecipeStep(2, "Add salt", "Add the salt and work it through the dough.", timerSeconds = null),
            RecipeStep(3, "Bulk ferment", "Cover and let rise until roughly doubled.", timerSeconds = 14400),
            RecipeStep(4, "Shape", "Shape the dough into a tight round loaf.", timerSeconds = null),
            RecipeStep(5, "Bake", "Bake in a preheated Dutch oven until deeply golden.", timerSeconds = 2700),
        )
    )

    private val croissants = Recipe(
        id = "r4",
        title = "Butter Croissants",
        authorName = "Jordan P.",
        source = "User",
        category = "Pastries",
        difficulty = Difficulty.HARD,
        prepTimeMinutes = 90,
        cookTimeMinutes = 20,
        servings = 12,
        avgRating = 4.7f,
        ratingCount = 88,
        nutrition = Nutrition(calories = 270, proteinG = 5f, carbsG = 26f, fatG = 16f),
        ingredients = listOf(
            Ingredient("Bread flour", "500", "g"),
            Ingredient("Butter (for laminating)", "300", "g"),
            Ingredient("Milk", "150", "ml"),
            Ingredient("Yeast", "10", "g"),
        ),
        steps = listOf(
            RecipeStep(1, "Make dough", "Mix flour, milk, and yeast into a smooth dough.", timerSeconds = null),
            RecipeStep(2, "Laminate", "Fold the butter block into the dough in three turns, chilling between each.", timerSeconds = 1800),
            RecipeStep(3, "Shape", "Roll out and cut into triangles, then roll into crescents.", timerSeconds = null),
            RecipeStep(4, "Prove", "Prove until visibly puffy.", timerSeconds = 5400),
            RecipeStep(5, "Bake", "Bake until deep golden brown.", timerSeconds = 1200),
        )
    )

    val recipes = listOf(bananaBread, chocChipCookies, sourdough, croissants)

    val weeklyChallenge = sourdough

    val recommended = listOf(chocChipCookies, bananaBread)

    fun recipeById(id: String): Recipe? = recipes.find { it.id == id }

    val savedRecipeIds = mutableStateSetOf("r1", "r3")
    val offlineRecipeIds = mutableStateSetOf("r1")

    val groceryItems = listOf(
        GroceryItem("g1", "Flour", "4 cups"),
        GroceryItem("g2", "Ripe bananas", "6"),
        GroceryItem("g3", "Eggs", "5"),
        GroceryItem("g4", "Butter", "350 g"),
        GroceryItem("g5", "Sugar", "1.5 cups"),
        GroceryItem("g6", "Baking soda", "2 tsp"),
    )
}

/**
 * Small helper so MockData can expose a mutable set without pulling in
 * Compose runtime imports inside the data module. UI layers wrap this in
 * remember { mutableStateOf(...) } where reactive updates are needed.
 */
fun mutableStateSetOf(vararg items: String): MutableSet<String> = items.toMutableSet()
