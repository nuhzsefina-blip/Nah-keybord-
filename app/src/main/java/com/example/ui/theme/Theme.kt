package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = EthiopianGold,
    onPrimary = Obsidian950,
    primaryContainer = Color(0xFF3A2C0B),
    onPrimaryContainer = EthiopianGoldSoft,
    secondary = AxumiteEmerald,
    onSecondary = Color(0xFF031A11),
    secondaryContainer = Color(0xFF0D3326),
    onSecondaryContainer = AxumiteEmeraldSoft,
    tertiary = EthiopianGoldSoft,
    background = Obsidian950,
    onBackground = WarmIvory,
    surface = Obsidian900,
    onSurface = WarmIvory,
    surfaceVariant = Obsidian800,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLow = Obsidian900,
    surfaceContainer = Obsidian850,
    surfaceContainerHigh = Obsidian800,
    outline = SubtleBorder,
    error = TerracottaCrimson,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EthiopianGoldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDC2),
    onPrimaryContainer = Color(0xFF3E2A00),
    secondary = AxumiteEmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    background = DaylightSurface,
    onBackground = DaylightInk,
    surface = DaylightCard,
    onSurface = DaylightInk,
    surfaceVariant = Color(0xFFE7E2D8),
    onSurfaceVariant = DaylightSubtle,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = Color(0xFFF1EDE4),
    surfaceContainerHigh = Color(0xFFE7E2D8),
    outline = Color(0xFFC9C1B4),
    error = TerracottaCrimson,
  )

val NuhShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun NuhKeyboardTheme(
  darkTheme: Boolean = true,
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = NuhShapes,
    content = content,
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  NuhKeyboardTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
