package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bakehub.app.data.MockData
import com.bakehub.app.data.Recipe
import com.bakehub.app.data.RecipeRepository
import com.bakehub.app.ui.components.RecipeListRow
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White

private val filterOptions = listOf("Category", "Difficulty", "Time", "Diet")

/**
 * 3.4 Search & Filter — active recipe discovery with filtering.
 * Navigates to: Recipe Detail.
 */
@Composable
fun SearchScreen(onOpenRecipe: (Recipe) -> Unit) {
    var query by remember { mutableStateOf("") }
    val selectedFilters = remember { mutableStateOf(setOf<String>()) }

    val allRecipes by RecipeRepository.recipes.collectAsState()
    val results = remember(query, allRecipes) {
        val source = allRecipes.ifEmpty { MockData.recipes }
        if (query.isBlank()) source
        else source.filter { it.title.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search recipes, ingredients\u2026") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = Terracotta,
                unfocusedBorderColor = GreyText,
            ),
        )

        Text("Filters", fontWeight = FontWeight.Bold, color = DarkBrown, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filterOptions.forEach { label ->
                val selected = selectedFilters.value.contains(label)
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedFilters.value = if (selected) selectedFilters.value - label else selectedFilters.value + label
                    },
                    label = { Text(label) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Terracotta,
                        selectedLabelColor = White,
                    ),
                )
            }
        }

        Text(
            "${results.size} results",
            color = GreyText,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
        ) {
            items(results) { recipe ->
                RecipeListRow(recipe = recipe, onClick = { onOpenRecipe(recipe) })
            }
        }
    }
}
