package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Obsidian & Ethiopian Heritage Palette
val Obsidian950 = Color(0xFF0B0D10)
val Obsidian900 = Color(0xFF121417)
val Obsidian850 = Color(0xFF181B20)
val Obsidian800 = Color(0xFF1F242B)
val Obsidian700 = Color(0xFF2B313B)
val Obsidian600 = Color(0xFF39414E)

val EthiopianGold = Color(0xFFF5B731)
val EthiopianGoldSoft = Color(0xFFFFD678)
val EthiopianGoldDeep = Color(0xFFC48B12)

val AxumiteEmerald = Color(0xFF10B981)
val AxumiteEmeraldSoft = Color(0xFF6EE7B7)
val AxumiteEmeraldDeep = Color(0xFF047857)

val FidelMorphBlue = Color(0xFF42A5F5)
val FidelMorphBlueBright = Color(0xFF38BDF8)
val FidelMorphBlueDeep = Color(0xFF0284C7)

val TerracottaCrimson = Color(0xFFEF5350)
val WarmIvory = Color(0xFFF5F3EE)
val MutedSlate = Color(0xFF9CA3AF)
val SubtleBorder = Color(0xFF2E3540)

// Daylight Palette
val DaylightSurface = Color(0xFFF7F5F0)
val DaylightCard = Color(0xFFFFFFFF)
val DaylightKey = Color(0xFFFFFFFF)
val DaylightModifierKey = Color(0xFFE5E1D8)
val DaylightInk = Color(0xFF16191D)
val DaylightSubtle = Color(0xFF5C6470)

data class KeyboardPalette(
  val id: KeyboardThemeStyle,
  val label: String,
  val description: String,
  val boardBackground: Color,
  val hingeBackground: Color,
  val toolbarBackground: Color,
  val keySurface: Color,
  val keySurfacePressed: Color,
  val modifierKeySurface: Color,
  val accentKeySurface: Color,
  val accentKeyText: Color,
  val primaryText: Color,
  val secondaryText: Color,
  val accentColor: Color,
  val secondaryAccent: Color,
  val vowelMorphAccent: Color,
  val keyBorder: Color,
  val chipSurface: Color,
)

enum class KeyboardThemeStyle(val displayName: String) {
  OBSIDIAN_GOLD("Obsidian Gold"),
  AXUMITE_EMERALD("Axumite Emerald"),
  ADDIS_DAYLIGHT("Addis Daylight"),
  HIGH_CONTRAST("High Contrast"),
}

fun paletteFor(style: KeyboardThemeStyle): KeyboardPalette =
  when (style) {
    KeyboardThemeStyle.OBSIDIAN_GOLD ->
      KeyboardPalette(
        id = style,
        label = "Obsidian Gold",
        description = "AMOLED-deep dark slate with warm Ethiopian gold & luminous vowel blue",
        boardBackground = Obsidian950,
        hingeBackground = Color(0xFF07080A),
        toolbarBackground = Obsidian900,
        keySurface = Obsidian800,
        keySurfacePressed = Obsidian700,
        modifierKeySurface = Obsidian850,
        accentKeySurface = EthiopianGold,
        accentKeyText = Obsidian950,
        primaryText = WarmIvory,
        secondaryText = MutedSlate,
        accentColor = EthiopianGold,
        secondaryAccent = AxumiteEmerald,
        vowelMorphAccent = FidelMorphBlueBright,
        keyBorder = Color(0xFF2C333E),
        chipSurface = Obsidian800,
      )
    KeyboardThemeStyle.AXUMITE_EMERALD ->
      KeyboardPalette(
        id = style,
        label = "Axumite Emerald",
        description = "Deep forest graphite paired with luminous emerald & sky cyan",
        boardBackground = Color(0xFF0A1210),
        hingeBackground = Color(0xFF060B09),
        toolbarBackground = Color(0xFF101C18),
        keySurface = Color(0xFF182823),
        keySurfacePressed = Color(0xFF223932),
        modifierKeySurface = Color(0xFF13201C),
        accentKeySurface = AxumiteEmerald,
        accentKeyText = Color(0xFF04140E),
        primaryText = Color(0xFFECFDF5),
        secondaryText = AxumiteEmeraldSoft,
        accentColor = AxumiteEmerald,
        secondaryAccent = EthiopianGold,
        vowelMorphAccent = FidelMorphBlueBright,
        keyBorder = Color(0xFF233B33),
        chipSurface = Color(0xFF172722),
      )
    KeyboardThemeStyle.ADDIS_DAYLIGHT ->
      KeyboardPalette(
        id = style,
        label = "Addis Daylight",
        description = "Warm parchment keys with crisp charcoal ink for bright sun",
        boardBackground = Color(0xFFEBE7DE),
        hingeBackground = Color(0xFFDCD6C9),
        toolbarBackground = DaylightSurface,
        keySurface = DaylightKey,
        keySurfacePressed = Color(0xFFF2ECE0),
        modifierKeySurface = DaylightModifierKey,
        accentKeySurface = EthiopianGoldDeep,
        accentKeyText = Color.White,
        primaryText = DaylightInk,
        secondaryText = DaylightSubtle,
        accentColor = EthiopianGoldDeep,
        secondaryAccent = AxumiteEmeraldDeep,
        vowelMorphAccent = FidelMorphBlueDeep,
        keyBorder = Color(0xFFD1CAC0),
        chipSurface = DaylightCard,
      )
    KeyboardThemeStyle.HIGH_CONTRAST ->
      KeyboardPalette(
        id = style,
        label = "High Contrast",
        description = "Pure OLED black with bold amber & electric blue vowel highlights",
        boardBackground = Color.Black,
        hingeBackground = Color(0xFF050505),
        toolbarBackground = Color(0xFF0D0D0D),
        keySurface = Color(0xFF161616),
        keySurfacePressed = Color(0xFF2B2B2B),
        modifierKeySurface = Color(0xFF1E1E1E),
        accentKeySurface = EthiopianGold,
        accentKeyText = Color.Black,
        primaryText = Color.White,
        secondaryText = Color(0xFFB0BEC5),
        accentColor = EthiopianGold,
        secondaryAccent = AxumiteEmeraldSoft,
        vowelMorphAccent = FidelMorphBlue,
        keyBorder = Color(0xFF474747),
        chipSurface = Color(0xFF1A1A1A),
      )
  }
