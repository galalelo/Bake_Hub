package com.bakehub.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface RecipeApi {

    // Search recipes by name
    @GET("search.php")
    suspend fun searchRecipes(
        @Query("s") query: String
    ): MealSearchResponse

    // Get recipe details using its ID
    @GET("lookup.php")
    suspend fun getRecipeDetails(
        @Query("i") recipeId: String
    ): MealSearchResponse

    // Get a random recipe
    @GET("random.php")
    suspend fun getRandomRecipe(): MealSearchResponse

    // Filter recipes by category
    @GET("filter.php")
    suspend fun getRecipesByCategory(
        @Query("c") category: String
    ): MealSearchResponse
}