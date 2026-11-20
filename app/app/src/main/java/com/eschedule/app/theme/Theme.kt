package com.eschedule.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = UepBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = AppBackground,
    onPrimaryContainer = UepBlue,
    secondary = AccentBlue,
    onSecondary = SurfaceWhite,
    tertiary = UepYellow,
    onTertiary = UepBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppColors.title,
    onPrimary = Color(0xFF0B1220),
    primaryContainer = DarkAppColors.surface,
    onPrimaryContainer = DarkAppColors.title,
    secondary = Color(0xFF7FA6EC),
    onSecondary = Color(0xFF0B1220),
    tertiary = UepYellow,
    onTertiary = Color(0xFF101319),
    background = DarkAppColors.background,
    onBackground = DarkAppColors.text,
    surface = DarkAppColors.surface,
    onSurface = DarkAppColors.text,
    surfaceVariant = Color(0xFF262B35),
    onSurfaceVariant = Color(0xFFC3C8D2),
    outline = Color(0xFF3A404C),
)

@Composable
fun EScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
