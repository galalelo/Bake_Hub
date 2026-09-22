package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.bakehub.app.data.MockData
import com.bakehub.app.data.Recipe
import com.bakehub.app.data.RecipeRepository
import com.bakehub.app.data.UserRepository
import com.bakehub.app.ui.components.RecipeCard
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.Gold
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White

/**
 * 3.3 Home / Discover — orientation hub, discovery, and a quick gamification snapshot.
 * Navigates to: Search & Filter (search bar), Recipe Detail (recipe cards).
 */
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
) {
    // Live from Firestore ("users/{uid}"); falls back to mock data until the
    // profile doc loads (or for the "Weekly Challenge" gamification content,
    // which is still mock/static — see README).
    val liveProfile by UserRepository.profile.collectAsState()
    val user = liveProfile ?: MockData.currentUser
    val recipes by RecipeRepository.recipes.collectAsState()
    val recommended = recipes.take(6).ifEmpty { MockData.recommended }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("Good morning, ${user.displayName.substringBefore(' ')}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DarkBrown)
            Text(
                " ${user.streakCount} day streak" + " - Lvl ${user.level}",
                color = Terracotta,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
        }

        item {
            OutlinedTextField(
                value = "",
                onValueChange = { onOpenSearch() },
                readOnly = true,
                placeholder = { Text("Search recipes, ingredients\u2026") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = GreyText,
                ),
            )
        }

        item {
            Text("Weekly Challenge", fontWeight = FontWeight.Bold, color = DarkBrown, modifier = Modifier.padding(bottom = 8.dp))
            val challenge = MockData.weeklyChallenge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.6f)
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Gold),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(challenge.title, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Bake it in Bake Mode \u00b7 earn bonus XP", color = White, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item {
            Text("Recommended for you", fontWeight = FontWeight.Bold, color = DarkBrown, modifier = Modifier.padding(bottom = 8.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                items(recommended) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        modifier = Modifier.width(170.dp),
                        onClick = { onOpenRecipe(recipe) },
                    )
                }
            }
        }
    }
}
