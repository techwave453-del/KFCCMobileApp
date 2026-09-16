package com.example.helloworld.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KfccSky,
    onPrimary = KfccNavy,
    primaryContainer = Color(0xFF17385F),
    onPrimaryContainer = KfccSky,
    secondary = KfccGold,
    onSecondary = KfccNavy,
    tertiary = KfccBlue,
    background = KfccDarkSurface,
    surface = KfccDarkSurface,
    surfaceVariant = KfccDarkCard
)

private val LightColorScheme = lightColorScheme(
    primary = KfccBlue,
    onPrimary = KfccWhite,
    primaryContainer = KfccSky,
    onPrimaryContainer = KfccNavy,
    secondary = KfccGold,
    onSecondary = KfccNavy,
    tertiary = KfccNavy,
    background = KfccSurface,
    surface = KfccWhite,
    surfaceVariant = Color(0xFFEEF2F7)
)

@Composable
fun KFCCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
