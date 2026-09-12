package com.fiki.ytdownloader.ui.theme

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
fun YTDownloaderTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // VS Code Dark theme
  MaterialTheme(
    colorScheme = VsCodeDarkColorScheme,
    typography = Typography,
    content = content
  )
}

