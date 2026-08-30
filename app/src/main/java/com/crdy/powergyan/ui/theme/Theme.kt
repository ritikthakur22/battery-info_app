package com.crdy.powergyan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = Color(0xFF17151F),
    background = Color(0xFF100E16)
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFFF8F6FF),
    background = Color(0xFFF1EFF8)
)

@Composable
fun PowerGyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentColor: Color = Mint,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && accentColor == Mint && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme(accentColor)
        else -> lightScheme(accentColor)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
