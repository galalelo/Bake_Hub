package com.bakehub.app.data.repository

import com.bakehub.app.data.remote.Meal
import com.bakehub.app.data.remote.RetrofitClient

class MealDbRepository {

    private val api = RetrofitClient.recipeApi

    // Search recipes by name
    suspend fun searchRecipes(query: String): Result<List<Meal>> {
        return runCatching {
            api.searchRecipes(query).meals.orEmpty()
        }
    }

    // Get detailed recipe information
    suspend fun getRecipeDetails(recipeId: String): Result<Meal> {
        return runCatching {
            api.getRecipeDetails(recipeId)
                .meals
                ?.firstOrNull()
                ?: throw Exception("Recipe not found")
        }
    }


    // Get random dessert recipes
    suspend fun getRandomDesserts(count: Int = 3): Result<List<Meal>> {
        return runCatching {

            val desserts = api.getRecipesByCategory("Dessert")
                .meals
                .orEmpty()

            desserts.shuffled().take(count)
        }
    }


    // Fetch a random recipe
    suspend fun getRandomRecipe(): Result<Meal> {
        return runCatching {
            api.getRandomRecipe()
                .meals
                ?.firstOrNull()
                ?: throw Exception("No recipe found")
        }
    }

    // Search recipes by category
    suspend fun getRecipesByCategory(
        category: String
    ): Result<List<Meal>> {
        return runCatching {
            api.getRecipesByCategory(category).meals.orEmpty()
        }
    }
}