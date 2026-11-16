package com.candies.simhuber.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDeep = Color(0xFF0A0F14)
val CardSurface = Color(0xFF131B24)
val CardSurfaceElevated = Color(0xFF1A2532)
val CardBorder = Color(0xFF263545)
val AccentCyan = Color(0xFF00E5FF)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)

private val SimHuberDarkScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF113542),
    onPrimaryContainer = AccentCyan,
    secondary = AccentEmerald,
    onSecondary = Color(0xFF002114),
    tertiary = AccentAmber,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = AccentRose
)

@Composable
fun SIMHuberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SimHuberDarkScheme,
        content = content
    )
}
