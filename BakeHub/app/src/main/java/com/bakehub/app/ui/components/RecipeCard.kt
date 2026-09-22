package com.bakehub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bakehub.app.data.Recipe
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Gold
import com.bakehub.app.ui.theme.WarmGrey
import com.bakehub.app.ui.theme.White

/** A vertical card used in grids (Home recommendations, Search results grid). */
@Composable
fun RecipeCard(recipe: Recipe, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(recipe)
            Column(Modifier.padding(10.dp)) {
                Text(
                    recipe.title,
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                    Text(
                        " ${recipe.avgRating} \u00b7 ${recipe.totalTimeMinutes} min",
                        color = GreyText,
                    )
                }
            }
        }
    }
}

@Composable
private fun Box(recipe: Recipe) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .background(WarmGrey)
    )
}

/** A horizontal row card used in list-style screens (Search list, Recipe Box). */
@Composable
fun RecipeListRow(recipe: Recipe, modifier: Modifier = Modifier, trailingLabel: String? = null, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(WarmGrey, RoundedCornerShape(10.dp))
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(recipe.title, fontWeight = FontWeight.Bold, color = DarkBrown, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "\u2605 ${recipe.avgRating} \u00b7 ${recipe.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }} \u00b7 ${recipe.totalTimeMinutes} min",
                    color = GreyText,
                )
                if (trailingLabel != null) {
                    Text(trailingLabel, color = GreyText)
                }
            }
        }
    }
}
