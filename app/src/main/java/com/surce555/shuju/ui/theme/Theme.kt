package com.surce555.shuju.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF2D2D44),
    onPrimaryContainer = Color(0xFFFFD700),
    secondary = Color(0xFFB8860B),
    onSecondary = Color(0xFF1A1A2E),
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFE8E8F0),
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = Color(0xFF252540),
    onSurfaceVariant = Color(0xFFB0B0C8),
    error = Color(0xFFFF4444),
    onError = Color(0xFF1A1A2E),
    outline = Color(0xFF444466)
)

@Composable
fun ShujuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
