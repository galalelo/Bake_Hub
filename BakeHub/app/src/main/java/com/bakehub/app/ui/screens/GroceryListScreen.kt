package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.GroceryItem
import com.bakehub.app.data.MockData
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White

/**
 * 3.8 Grocery List — turn planned recipes into a single practical shopping list.
 * Ingredients from multiple saved recipes are combined (design document, Section 2.1.8).
 */
@Composable
fun GroceryListScreen() {
    val items = remember { mutableStateListOf(*MockData.groceryItems.toTypedArray()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("This Week's List", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DarkBrown)
        Text(
            "Auto-combined from ${MockData.recommended.size + 1} recipes",
            color = GreyText,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                val index = items.indexOf(item)
                GroceryRow(item = item, onToggle = {
                    items[index] = item.copy(checked = !item.checked)
                })
            }
        }

        Button(
            onClick = { /* Share list — device share sheet (prototype no-op) */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = White),
        ) {
            Text("Share List", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GroceryRow(item: GroceryItem, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Terracotta),
            )
            Text(
                "${item.name} \u2014 ${item.quantity}",
                color = if (item.checked) GreyText else DarkBrown,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}
