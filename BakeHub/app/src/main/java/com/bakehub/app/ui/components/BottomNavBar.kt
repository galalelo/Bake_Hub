package com.bakehub.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bakehub.app.ui.navigation.BottomTab
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import androidx.compose.runtime.getValue

@Composable
fun BakeHubBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar(containerColor = Cream, tonalElevation = 4.dp) {
        BottomTab.values().forEach { tab ->
            val selected = currentRoute?.hierarchy?.any { it.route == tab.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.screen.route) {
                            popUpTo(BottomTab.HOME.screen.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(iconFor(tab), contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    unselectedIconColor = GreyText,
                    unselectedTextColor = GreyText,
                    indicatorColor = Cream,
                )
            )
        }
    }
}

private fun iconFor(tab: BottomTab) = when (tab) {
    BottomTab.HOME -> Icons.Filled.Home
    BottomTab.SEARCH -> Icons.Filled.Search
    BottomTab.RECIPE_BOX -> Icons.Filled.List
    BottomTab.GROCERY -> Icons.Filled.ShoppingCart
    BottomTab.PROFILE -> Icons.Filled.Person
}
