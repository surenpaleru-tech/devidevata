package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = SageGreenLightAccent,
        secondary = SandalwoodAltDark,
        tertiary = TerracottaTertiary,
        background = DarkForestBg,
        surface = DeepCharcoalCard,
        onBackground = Color(0xFFECEFF1), // Crisp, high-contrast, easy-reading off-white
        onSurface = Color(0xFFECEFF1),     // Crisp off-white surface texts
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onTertiary = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = SageGreenPrimary,
        secondary = SandalwoodSecondary,
        tertiary = TerracottaTertiary,
        background = WarmIvoryLightBg,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.White,
        onBackground = BurntWoodText,       // Warm comforting charcoal-brown (no-burn text)
        onSurface = BurntWoodText
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
