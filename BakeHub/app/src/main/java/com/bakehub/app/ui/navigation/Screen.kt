package com.bakehub.app.ui.navigation

/**
 * Central definition of every route in the BakeHub prototype, mirroring the
 * screen navigation map in the design document (Section 3.11).
 */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object RecipeBox : Screen("recipe_box")
    object Grocery : Screen("grocery")
    object Profile : Screen("profile")
    object CreateRecipe : Screen("create_recipe")
    object Settings : Screen("settings")

    object SignUp : Screen("signup")

    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }

    object BakeMode : Screen("bake_mode/{recipeId}") {
        fun createRoute(recipeId: String) = "bake_mode/$recipeId"
    }
}

/** The five root destinations shown behind the persistent bottom navigation bar. */
enum class BottomTab(val screen: Screen, val label: String) {
    HOME(Screen.Home, "Home"),
    SEARCH(Screen.Search, "Search"),
    RECIPE_BOX(Screen.RecipeBox, "Box"),
    GROCERY(Screen.Grocery, "Grocery"),
    PROFILE(Screen.Profile, "Profile"),
}
