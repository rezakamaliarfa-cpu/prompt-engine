package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF6366F1),        // Soft premium indigo
    secondary = Color(0xFF14B8A6),      // Soothing turquoise/teal accent
    tertiary = Color(0xFFF59E0B),       // Warm amber accent
    background = Color(0xFF090D16),     // Extremely soft dark slate-blue background
    surface = Color(0xFF151B26),        // Elegant dark card surface (anti-halation)
    onBackground = Color(0xFFE2E8F0),   // Cozy text color instead of glaring white
    onSurface = Color(0xFFF1F5F9),      // Soft text color (comfort)
    surfaceVariant = Color(0xFF222938), // Smooth card surface variant
    onSurfaceVariant = Color(0xFF94A3B8) // Muted secondary text
  )

private val LightColorScheme = DarkColorScheme // Forced dark theme for eye comfort

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force true to satisfy dark-theme requirement
  // Dynamic color is disabled by default to maintain our custom eye-comfort colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
