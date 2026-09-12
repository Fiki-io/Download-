package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VsCodeDarkColorScheme = darkColorScheme(
  primary = VsCodeBlue,
  onPrimary = Color.White,
  primaryContainer = VsCodeBlueDark,
  onPrimaryContainer = Color.White,
  secondary = VsCodeGreen,
  onSecondary = Color.Black,
  secondaryContainer = VsCodePanel,
  onSecondaryContainer = VsCodeText,
  tertiary = VsCodeOrange,
  onTertiary = Color.Black,
  background = VsCodeBackground,
  onBackground = VsCodeText,
  surface = VsCodeSidebar,
  onSurface = VsCodeText,
  surfaceVariant = VsCodePanel,
  onSurfaceVariant = VsCodeTextMuted,
  outline = VsCodeBorder,
  error = VsCodeRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the iconic VS Code Dark theme requested by user
  MaterialTheme(
    colorScheme = VsCodeDarkColorScheme,
    typography = Typography,
    content = content
  )
}

