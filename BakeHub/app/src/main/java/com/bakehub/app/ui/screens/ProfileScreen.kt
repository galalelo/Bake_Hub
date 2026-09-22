package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.data.AuthRepository
import com.bakehub.app.data.Badge
import com.bakehub.app.data.MockData
import com.bakehub.app.data.UserRepository
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.Gold
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.WarmGrey
import com.bakehub.app.ui.theme.White

/**
 * 3.10 Profile & Achievements — gamification dashboard and access point for Settings.
 * Navigates to: Settings.
 */
@Composable
fun ProfileScreen(onOpenSettings: () -> Unit) {

    // Live from Firestore ("users/{uid}"); XP/level/streak/badges are still
    // mock/static gamification content for this milestone (see README) —
    // only identity + saved/offline recipes are real so far.
    val liveProfile by UserRepository.profile.collectAsState()
    val user = liveProfile ?: MockData.currentUser
    val displayName = AuthRepository.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: user.displayName

    val progress = user.xp.toFloat() / user.xpForNextLevel.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        item {

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(WarmGrey, CircleShape),
            )

            Text(
                displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DarkBrown,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                "Level ${user.level} · ${user.xp} XP",
                color = Terracotta,
                fontWeight = FontWeight.Medium
            )

            LinearProgressIndicator(
                progress = {
                    progress.coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp),
                color = Gold,
                trackColor = WarmGrey,
            )

            Text(
                "Current streak: ${user.streakCount} days",
                color = DarkBrown,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Start,
            )

            Text(
                "Badges",
                fontWeight = FontWeight.Bold,
                color = DarkBrown,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp),
                textAlign = TextAlign.Start,
            )
        }

        item {

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MockData.badges) { badge ->
                    BadgeCircle(badge)
                }
            }
        }

        item {

            Text(
                "Baking Stats",
                fontWeight = FontWeight.Bold,
                color = DarkBrown,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 6.dp),
                textAlign = TextAlign.Start,
            )

            Text(
                "${user.bakesCompleted} bakes completed - ${user.recipesShared} recipes shared",
                color = GreyText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Settings",
                    color = DarkBrown
                )
            }
        }
    }
}

@Composable
private fun BadgeCircle(badge: Badge) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (badge.unlocked) Gold else WarmGrey,
                    CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (badge.unlocked) "\u2605" else "\u2606",
                fontSize = 20.sp
            )
        }

        Text(
            badge.name,
            fontSize = 10.sp,
            color = if (badge.unlocked) DarkBrown else GreyText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(70.dp)
                .padding(top = 4.dp),
        )
    }
}
