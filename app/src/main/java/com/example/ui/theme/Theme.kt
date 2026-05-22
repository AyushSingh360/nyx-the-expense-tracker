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
    primary = MintPrimary,
    secondary = CyanSecondary,
    tertiary = NeonPurple,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF0C0F16),
    onSecondary = Color(0xFF0C0F16),
    onTertiary = Color.White,
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextDarkSecondary
  )

private val LightColorScheme = DarkColorScheme // Force dark theme everywhere for dark aesthetic!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful custom styling
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
