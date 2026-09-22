package com.bakehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakehub.app.ui.theme.Cream
import com.bakehub.app.ui.theme.DarkBrown
import com.bakehub.app.ui.theme.GreyText
import com.bakehub.app.ui.theme.Terracotta
import com.bakehub.app.ui.theme.White

/**
 * 3.1 Splash / Onboarding — brand introduction and entry point into authentication.
 * Navigates to: Login/Register.
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(110.dp)
                .background(Terracotta, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("\uD83E\uDD56", fontSize = 46.sp) // baking-adjacent emoji as a placeholder mark
        }

        Text(
            "BakeHub",
            modifier = Modifier.padding(top = 20.dp),
            color = DarkBrown,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
        )
        Text(
            "Bake smarter, together.",
            modifier = Modifier.padding(top = 4.dp, bottom = 48.dp),
            color = GreyText,
            fontSize = 16.sp,
        )

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = White),
        ) {
            Text("Get Started", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("I already have an account", color = DarkBrown, textAlign = TextAlign.Center)
        }
    }
}
