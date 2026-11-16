package com.candies.simhuber.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD8EB),
    onPrimary = Color(0xFF00363D),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0E1416),
    surface = Color(0xFF161D20)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFF6FAFB),
    surface = Color.White
)

@Composable
fun SIMHuberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
