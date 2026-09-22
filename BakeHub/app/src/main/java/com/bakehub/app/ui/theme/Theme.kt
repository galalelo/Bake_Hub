package com.bakehub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BakeHubColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = White,
    primaryContainer = Gold,
    onPrimaryContainer = DarkBrown,
    secondary = Gold,
    onSecondary = DarkBrown,
    background = Cream,
    onBackground = DarkBrown,
    surface = White,
    onSurface = DarkBrown,
    surfaceVariant = WarmGrey,
    onSurfaceVariant = GreyText,
    outline = WarmGrey,
    error = TerracottaDark,
)

@Composable
fun BakeHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BakeHubColorScheme,
        typography = BakeHubTypography,
        content = content
    )
}
