package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.Recipe
import com.bakehub.app.data.UserRepository
import kotlinx.coroutines.launch
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.Gold
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.WarmGrey
import com.bakehub.app.ui.theme.White

/**
 * 3.5 Recipe Detail — present full recipe information and let the user save it or start baking.
 * Navigates to: Bake Mode (Start Bake Mode), Recipe Box (Save).
 */
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onStartBakeMode: () -> Unit,
) {
    var unitSystem by remember { mutableIntStateOf(0) } // 0 = Metric, 1 = Imperial
    val savedIds by UserRepository.savedRecipeIds.collectAsState()
    val saved = savedIds.contains(recipe.id)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DarkBrown)
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.7f)
                .padding(horizontal = 20.dp)
                .background(WarmGrey, RoundedCornerShape(16.dp)),
        )

        Column(Modifier.padding(20.dp)) {
            Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DarkBrown)

            Text(
                "\u2605 ${recipe.avgRating} (${recipe.ratingCount}) - ${recipe.totalTimeMinutes} min   " +
                    "- ${recipe.servings} servings - Difficulty: ${recipe.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = GreyText,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )

            Text("Nutrition (per serving)",
                fontWeight = FontWeight.Bold,
                color = DarkBrown)

            Text(
                "${recipe.nutrition.calories} kcal - ${recipe.nutrition.proteinG.toInt()}g protein - " +
                    "${recipe.nutrition.carbsG.toInt()}g carbs - ${recipe.nutrition.fatG.toInt()}g fat",
                color = GreyText,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                Text("Ingredients",
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown)


                //button switch

                SingleChoiceSegmentedButtonRow {
                    listOf("Metric", "Imperial").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = unitSystem == index,
                            onClick = { unitSystem = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = Terracotta, activeContentColor = White),
                        ) { Text(label,
                             color = DarkBrown) }
                    }
                }

            }

            Column(Modifier.padding(top = 8.dp, bottom = 18.dp)) {
                recipe.ingredients.forEach { ing ->
                    val qty = if (unitSystem == 0) "${ing.quantity} ${ing.unit}".trim() else convertToImperialLabel(ing.quantity, ing.unit)
                    Text("\u2022 $qty ${ing.name}", color = DarkBrown, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Button(
                onClick = onStartBakeMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = White),
            ) {
                Text("Start Bake Mode \u25b6", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { scope.launch { UserRepository.toggleSaved(recipe.id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (saved) "\u2605 Saved to Recipe Box" else "\u2606 Save to Recipe Box", color = DarkBrown)
            }
        }
    }
}

/** Simple illustrative metric->imperial label for the prototype (not unit-accurate conversion maths). */
private fun convertToImperialLabel(quantity: String, unit: String): String {
    return when (unit) {
        "cups" -> "$quantity cups"
        "g" -> "${quantity} g (\u2248 oz)"
        "ml" -> "${quantity} ml (\u2248 fl oz)"
        else -> "$quantity $unit".trim()
    }
}
