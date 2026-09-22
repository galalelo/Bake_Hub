package com.bakehub.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BakeHubTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, color = DarkBrown),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = DarkBrown),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkBrown),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = DarkBrown),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = DarkBrown),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = DarkBrown),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = GreyText),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = White),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = GreyText),
)
