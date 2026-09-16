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

private val DarkColorScheme =
  darkColorScheme(
    primary = ChaupalIndigoDarkPrimary,
    onPrimary = ChaupalBackgroundDark,
    primaryContainer = ChaupalSurfaceVariantDark,
    onPrimaryContainer = ChaupalOnSurfaceDark,
    secondary = ChaupalCoralDark,
    onSecondary = ChaupalBackgroundDark,
    tertiary = ChaupalTealDark,
    background = ChaupalBackgroundDark,
    onBackground = ChaupalOnSurfaceDark,
    surface = ChaupalSurfaceDark,
    onSurface = ChaupalOnSurfaceDark,
    surfaceVariant = ChaupalSurfaceVariantDark,
    onSurfaceVariant = ChaupalOnSurfaceVariantDark,
    error = ChaupalRed,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ChaupalIndigoPrimary,
    onPrimary = ChaupalSurfaceLight,
    primaryContainer = ChaupalSurfaceVariantLight,
    onPrimaryContainer = ChaupalIndigoPrimary,
    secondary = ChaupalCoral,
    onSecondary = ChaupalSurfaceLight,
    tertiary = ChaupalTeal,
    background = ChaupalBackgroundLight,
    onBackground = ChaupalOnSurfaceLight,
    surface = ChaupalSurfaceLight,
    onSurface = ChaupalOnSurfaceLight,
    surfaceVariant = ChaupalSurfaceVariantLight,
    onSurfaceVariant = ChaupalOnSurfaceVariantLight,
    error = ChaupalRed,
  )

@Composable
fun ChaupalTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  ChaupalTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
