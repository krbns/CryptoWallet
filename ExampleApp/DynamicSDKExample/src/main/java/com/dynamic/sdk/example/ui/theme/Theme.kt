package com.dynamic.sdk.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DynamicBlueDark,
    onPrimary = Color.Black,
    primaryContainer = DynamicBlue,
    onPrimaryContainer = Color.White,
    secondary = DynamicBlueDark,
    onSecondary = Color.Black,
    secondaryContainer = ChainBadgeBlue,
    onSecondaryContainer = DynamicBlueDark,
    tertiary = SuccessGreenLight,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = Color(0xFFFFFFFF),  // Beli tekst za najbolji kontrast
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFFE5E7EB),  // Svetlo sivi tekst
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRedLight,
    onError = Color.Black,
    errorContainer = AccentRedBackground,
    onErrorContainer = ErrorRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = DynamicBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueBackground,
    onPrimaryContainer = DynamicBlueLight,
    secondary = DynamicBlue,
    onSecondary = Color.White,
    secondaryContainer = ChainBadgeBlue,
    onSecondaryContainer = DynamicBlue,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = AccentRedBackground,
    onErrorContainer = ErrorRed
)

@Composable
fun DynamicSDKExampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
