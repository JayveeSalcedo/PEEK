package com.peek.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PeekViolet,
    onPrimary = Color.White,
    primaryContainer = PeekVioletContainer,
    onPrimaryContainer = Color(0xFF170065),
    secondary = PeekCoral,
    onSecondary = Color.White,
    background = PeekPaper,
    onBackground = PeekInk,
    surface = PeekPaper,
    onSurface = PeekInk,
)

private val DarkColors = darkColorScheme(
    primary = PeekVioletLight,
    onPrimary = Color(0xFF2A1A9E),
    primaryContainer = PeekVioletContainerDark,
    onPrimaryContainer = PeekVioletContainer,
    secondary = PeekCoralLight,
    onSecondary = Color(0xFF690005),
    background = PeekNight,
    onBackground = Color(0xFFE6E1EC),
    surface = PeekNight,
    onSurface = Color(0xFFE6E1EC),
    surfaceContainerHigh = PeekNightSurface,
)

@Composable
fun PeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Stable color for a sender name, so each participant keeps one color within a preview. */
@Composable
@ReadOnlyComposable
fun senderColor(sender: String?): Color {
    val palette = if (isSystemInDarkTheme()) SenderColorsDark else SenderColorsLight
    if (sender == null) return MaterialTheme.colorScheme.onSurfaceVariant
    return palette[Math.floorMod(sender.hashCode(), palette.size)]
}
