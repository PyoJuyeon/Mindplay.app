package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    onPrimary = Color.Black,
    secondary = SkyCyan,
    onSecondary = Color.Black,
    tertiary = SunsetAmber,
    onTertiary = Color.Black,
    background = CosmosDarkBackground,
    onBackground = OnCosmosDark,
    surface = CosmosDarkSurface,
    onSurface = OnCosmosDark,
    surfaceVariant = CosmosDarkSurfaceVariant,
    onSurfaceVariant = OnCosmosSecondary,
    error = CoralPink,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E), // Darker teal
    onPrimary = Color.White,
    secondary = Color(0xFF0369A1), // Darker sky cyan
    onSecondary = Color.White,
    tertiary = Color(0xFFB45309), // Darker sunset amber
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding for MindPlay instead of dynamic
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
