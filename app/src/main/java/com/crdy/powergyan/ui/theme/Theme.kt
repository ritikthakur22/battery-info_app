package com.crdy.powergyan.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Every container is derived from the accent so no Material default purple leaks through.
private fun darkScheme(accent: Color): androidx.compose.material3.ColorScheme {
    val container = accent.copy(alpha = .22f).compositeOver(SlateDarkSurface)
    return darkColorScheme(
        primary = accent, onPrimary = contentOn(accent),
        primaryContainer = container, onPrimaryContainer = InkDark,
        secondary = accent, secondaryContainer = container, onSecondaryContainer = InkDark,
        tertiary = accent,
        background = SlateDark, onBackground = InkDark,
        surface = SlateDarkSurface, onSurface = InkDark,
        surfaceVariant = SlateDarkVariant, onSurfaceVariant = InkDark.copy(alpha = .72f),
        outline = InkDark.copy(alpha = .24f),
        error = Color(0xFFFF8A80)
    )
}

private fun lightScheme(accent: Color): androidx.compose.material3.ColorScheme {
    val container = accent.copy(alpha = .12f).compositeOver(SlateLightSurface)
    return lightColorScheme(
        primary = accent, onPrimary = contentOn(accent),
        primaryContainer = container, onPrimaryContainer = InkLight,
        secondary = accent, secondaryContainer = container, onSecondaryContainer = InkLight,
        tertiary = accent,
        background = SlateLight, onBackground = InkLight,
        surface = SlateLightSurface, onSurface = InkLight,
        surfaceVariant = SlateLightVariant, onSurfaceVariant = InkLight.copy(alpha = .68f),
        outline = InkLight.copy(alpha = .2f),
        error = Lithium
    )
}

@Composable
fun PowerGyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = Cobalt,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
