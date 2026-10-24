package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CalendyAccentDark,
    onPrimary = CalendyBgDark,
    primaryContainer = CalendySurfaceVariantDark,
    onPrimaryContainer = CalendyTextDark,
    secondary = CalendyMutedDark,
    onSecondary = CalendyBgDark,
    background = CalendyBgDark,
    onBackground = CalendyTextDark,
    surface = CalendySurfaceDark,
    onSurface = CalendyTextDark,
    surfaceVariant = CalendySurfaceVariantDark,
    onSurfaceVariant = CalendyMutedDark,
    outline = CalendyBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CalendyAccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    tertiary = CalendyMutedLight,
    onTertiary = Color.White,
    background = CalendyBgLight,
    onBackground = CalendyTextLight,
    surface = CalendySurfaceLight,
    onSurface = CalendyTextLight,
    surfaceVariant = CalendySurfaceVariantLight,
    onSurfaceVariant = CalendyMutedLight,
    outline = CalendyBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
