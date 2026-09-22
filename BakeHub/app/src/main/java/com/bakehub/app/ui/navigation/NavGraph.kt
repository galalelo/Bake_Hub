package com.bakehub.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.bakehub.app.data.AuthRepository
import com.bakehub.app.data.MockData
import com.bakehub.app.data.RecipeRepository
import com.bakehub.app.data.UserRepository
import com.bakehub.app.ui.components.BakeHubBottomBar
import com.bakehub.app.ui.screens.BakeModeScreen
import com.bakehub.app.ui.screens.CreateRecipeScreen
import com.bakehub.app.ui.screens.GroceryListScreen
import com.bakehub.app.ui.screens.HomeScreen
import com.bakehub.app.ui.screens.LoginScreen
import com.bakehub.app.ui.screens.OnboardingScreen
import com.bakehub.app.ui.screens.ProfileScreen
import com.bakehub.app.ui.screens.RecipeBoxScreen
import com.bakehub.app.ui.screens.RecipeDetailScreen
import com.bakehub.app.ui.screens.SearchScreen
import com.bakehub.app.ui.screens.SettingsScreen
import com.bakehub.app.ui.screens.SignUpScreen

/** Routes shown with the persistent bottom tab bar (design document, Section 3.11). */
private val bottomBarRoutes = BottomTab.values().map { it.screen.route }.toSet()

@Composable
fun BakeHubNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // Called right after Firebase confirms a session (login or sign-up):
    // starts the live Firestore listeners and seeds sample recipes on a fresh project.
    fun onAuthenticated() {
        UserRepository.startListening()
        RecipeRepository.startListening()
        scope.launch { RecipeRepository.seedIfEmpty() }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                BakeHubBottomBar(navController)
            }
        }
    ) { innerPadding ->

        // If Firebase already has a signed-in session (persists across app restarts),
        // skip straight to Home instead of Onboarding/Login.
        val startDestination = if (AuthRepository.isLoggedIn) {
            onAuthenticated()
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(
                bottom = if (currentRoute in bottomBarRoutes)
                    innerPadding.calculateBottomPadding()
                else
                    0.dp
            ),
        ) {

            // ONBOARDING
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Login.route)
                    },
                    onLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                )
            }

            // LOGIN
            composable(Screen.Login.route) {
                LoginScreen(

                    // Successful login
                    onLoggedIn = {
                        onAuthenticated()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    },

                    // Go to Sign Up
                    onSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    }
                )
            }

            // SIGN UP
            composable(Screen.SignUp.route) {
                SignUpScreen(

                    // Successful registration — Firebase signs the user in immediately,
                    // so go straight to Home rather than back to Login.
                    onSignup = {
                        onAuthenticated()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    },

                    // Go back to Login
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // HOME
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onOpenRecipe = { recipe ->
                        navController.navigate(
                            Screen.RecipeDetail.createRoute(recipe.id)
                        )
                    },
                )
            }

            // SEARCH
            composable(Screen.Search.route) {
                SearchScreen(
                    onOpenRecipe = { recipe ->
                        navController.navigate(
                            Screen.RecipeDetail.createRoute(recipe.id)
                        )
                    },
                )
            }

            // RECIPE BOX
            composable(Screen.RecipeBox.route) {
                RecipeBoxScreen(
                    onOpenRecipe = { recipe ->
                        navController.navigate(
                            Screen.RecipeDetail.createRoute(recipe.id)
                        )
                    },
                    onCreateRecipe = {
                        navController.navigate(Screen.CreateRecipe.route)
                    },
                )
            }

            // GROCERY
            composable(Screen.Grocery.route) {
                GroceryListScreen()
            }

            // PROFILE
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                )
            }

            // CREATE RECIPE
            composable(Screen.CreateRecipe.route) {
                CreateRecipeScreen(
                    onPublished = {
                        navController.popBackStack()
                    },
                )
            }

            // SETTINGS
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onLogOut = {
                        AuthRepository.logout()
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0)
                        }
                    },
                )
            }

            // RECIPE DETAILS
            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(
                    navArgument("recipeId") {
                        type = NavType.StringType
                    }
                ),
            ) { entry ->

                val recipeId =
                    entry.arguments?.getString("recipeId")

                val recipe =
                    RecipeRepository.recipeById(recipeId ?: "")
                        ?: RecipeRepository.recipes.value.firstOrNull()
                        ?: MockData.recipes.first()

                RecipeDetailScreen(
                    recipe = recipe,
                    onBack = {
                        navController.popBackStack()
                    },
                    onStartBakeMode = {
                        navController.navigate(
                            Screen.BakeMode.createRoute(recipe.id)
                        )
                    },
                )
            }

            // BAKE MODE
            composable(
                route = Screen.BakeMode.route,
                arguments = listOf(
                    navArgument("recipeId") {
                        type = NavType.StringType
                    }
                ),
            ) { entry ->

                val recipeId =
                    entry.arguments?.getString("recipeId")

                val recipe =
                    RecipeRepository.recipeById(recipeId ?: "")
                        ?: RecipeRepository.recipes.value.firstOrNull()
                        ?: MockData.recipes.first()

                BakeModeScreen(
                    recipe = recipe,
                    onExit = {
                        navController.popBackStack()
                    },
                    onCompleted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}