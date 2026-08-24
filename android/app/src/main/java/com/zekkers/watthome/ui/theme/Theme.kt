package com.zekkers.watthome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestMid,
    onPrimary = Color.White,
    secondary = Solar,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Leaf,
    onSurfaceVariant = Forest,
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = Forest,
    secondary = Solar,
    onSecondary = Ink,
    background = Color(0xFF10150F),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF1B3A24),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF24462E),
    onSurfaceVariant = Leaf,
    error = Color(0xFFF2B8B5)
)

@Composable
fun WattHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
