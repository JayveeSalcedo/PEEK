package com.peek.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalPeekColors = staticCompositionLocalOf { LightPeekColors }

@Composable
fun PeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val peek = if (darkTheme) DarkPeekColors else LightPeekColors
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = peek.tint,
        onPrimary = Color.White,
        primaryContainer = peek.outgoingBubble,
        onPrimaryContainer = Color.White,
        secondary = peek.tint,
        tertiary = peek.green,
        error = peek.red,
        background = peek.groupedBackground,
        onBackground = peek.label,
        surface = peek.cell,
        onSurface = peek.label,
        onSurfaceVariant = peek.secondaryLabel,
        surfaceContainer = peek.cell,
        surfaceContainerHigh = peek.cell,
        surfaceContainerHighest = peek.incomingBubble,
        outline = peek.separator,
        outlineVariant = peek.separator,
    )
    CompositionLocalProvider(LocalPeekColors provides peek) {
        MaterialTheme(colorScheme = scheme, typography = PeekTypography, content = content)
    }
}
