package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nuhDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "nuh_keyboard_prefs")

enum class AmharicInputMode(val label: String, val badge: String) {
  FIDEL_INPLACE("አማ In-Place", "አማ"),
  ENGLISH_QWERTY("English QWERTY", "EN"),
}

enum class FoldLayoutPreference(val label: String, val shortLabel: String) {
  AUTO("Auto Z Fold 7 Sense", "Auto Fold"),
  UNFOLDED_SPLIT("Unfolded Split Ergonomic", "Split"),
  MULTI_WINDOW_DOCK("Multi-Window Compact", "Split-Screen"),
  COVER_COMPACT("Cover Screen Compact", "Cover"),
  FLEX_TABLETOP("Flex / Half-Folded", "Flex"),
}

enum class OneHandedSide(val label: String) {
  NONE("Centered"),
  LEFT("Left Thumb"),
  RIGHT("Right Thumb"),
}

data class KeyboardSettings(
  val inputMode: AmharicInputMode = AmharicInputMode.FIDEL_INPLACE,
  val foldLayoutPreference: FoldLayoutPreference = FoldLayoutPreference.AUTO,
  val splitGapDp: Float = 86f,
  val keyHeightDp: Float = 48f,
  val letterSizeSp: Float = 19f,
  val themeStyle: KeyboardThemeStyle = KeyboardThemeStyle.OBSIDIAN_GOLD,
  val showNumberRow: Boolean = true,
  val showEthiopicNumerals: Boolean = false,
  val hapticsEnabled: Boolean = true,
  val keyPopupEnabled: Boolean = true,
  val autoCorrectEnabled: Boolean = true,
  val nextFidelHintsEnabled: Boolean = true,
  val oneHandedSide: OneHandedSide = OneHandedSide.NONE,
  val customDictionaryWords: Set<String> = emptySet(),
)

class KeyboardPreferencesRepository(private val context: Context) {

  private object Keys {
    val INPUT_MODE = stringPreferencesKey("input_mode")
    val FOLD_LAYOUT = stringPreferencesKey("fold_layout")
    val SPLIT_GAP_DP = floatPreferencesKey("split_gap_dp")
    val KEY_HEIGHT_DP = floatPreferencesKey("key_height_dp")
    val LETTER_SIZE_SP = floatPreferencesKey("letter_size_sp")
    val THEME_STYLE = stringPreferencesKey("theme_style")
    val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
    val SHOW_ETHIOPIC_NUMERALS = booleanPreferencesKey("show_ethiopic_numerals")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val KEY_POPUP_ENABLED = booleanPreferencesKey("key_popup_enabled")
    val AUTO_CORRECT_ENABLED = booleanPreferencesKey("auto_correct_enabled")
    val NEXT_FIDEL_HINTS_ENABLED = booleanPreferencesKey("next_fidel_hints_enabled")
    val ONE_HANDED_SIDE = stringPreferencesKey("one_handed_side")
    val CUSTOM_DICTIONARY_WORDS = stringSetPreferencesKey("custom_dictionary_words")
  }

  val settingsFlow: Flow<KeyboardSettings> =
    context.nuhDataStore.data.map { prefs ->
      KeyboardSettings(
        inputMode =
          when (prefs[Keys.INPUT_MODE]) {
            "ENGLISH_QWERTY" -> AmharicInputMode.ENGLISH_QWERTY
            else -> AmharicInputMode.FIDEL_INPLACE
          },
        foldLayoutPreference =
          prefs[Keys.FOLD_LAYOUT]?.let {
            runCatching { FoldLayoutPreference.valueOf(it) }.getOrNull()
          } ?: FoldLayoutPreference.AUTO,
        splitGapDp = prefs[Keys.SPLIT_GAP_DP] ?: 86f,
        keyHeightDp = prefs[Keys.KEY_HEIGHT_DP] ?: 48f,
        letterSizeSp = prefs[Keys.LETTER_SIZE_SP] ?: 19f,
        themeStyle =
          prefs[Keys.THEME_STYLE]?.let {
            runCatching { KeyboardThemeStyle.valueOf(it) }.getOrNull()
          } ?: KeyboardThemeStyle.OBSIDIAN_GOLD,
        showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: true,
        showEthiopicNumerals = prefs[Keys.SHOW_ETHIOPIC_NUMERALS] ?: false,
        hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        keyPopupEnabled = prefs[Keys.KEY_POPUP_ENABLED] ?: true,
        autoCorrectEnabled = prefs[Keys.AUTO_CORRECT_ENABLED] ?: true,
        nextFidelHintsEnabled = prefs[Keys.NEXT_FIDEL_HINTS_ENABLED] ?: true,
        oneHandedSide =
          prefs[Keys.ONE_HANDED_SIDE]?.let {
            runCatching { OneHandedSide.valueOf(it) }.getOrNull()
          } ?: OneHandedSide.NONE,
        customDictionaryWords = prefs[Keys.CUSTOM_DICTIONARY_WORDS] ?: emptySet(),
      )
    }

  suspend fun setInputMode(mode: AmharicInputMode) {
    context.nuhDataStore.edit { it[Keys.INPUT_MODE] = mode.name }
  }

  suspend fun setFoldLayoutPreference(pref: FoldLayoutPreference) {
    context.nuhDataStore.edit { it[Keys.FOLD_LAYOUT] = pref.name }
  }

  suspend fun setSplitGapDp(gapDp: Float) {
    context.nuhDataStore.edit { it[Keys.SPLIT_GAP_DP] = gapDp.coerceIn(40f, 148f) }
  }

  suspend fun setKeyHeightDp(heightDp: Float) {
    context.nuhDataStore.edit { it[Keys.KEY_HEIGHT_DP] = heightDp.coerceIn(36f, 68f) }
  }

  suspend fun setLetterSizeSp(sizeSp: Float) {
    context.nuhDataStore.edit { it[Keys.LETTER_SIZE_SP] = sizeSp.coerceIn(14f, 26f) }
  }

  suspend fun setThemeStyle(style: KeyboardThemeStyle) {
    context.nuhDataStore.edit { it[Keys.THEME_STYLE] = style.name }
  }

  suspend fun setShowNumberRow(show: Boolean) {
    context.nuhDataStore.edit { it[Keys.SHOW_NUMBER_ROW] = show }
  }

  suspend fun setShowEthiopicNumerals(show: Boolean) {
    context.nuhDataStore.edit { it[Keys.SHOW_ETHIOPIC_NUMERALS] = show }
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    context.nuhDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
  }

  suspend fun setKeyPopupEnabled(enabled: Boolean) {
    context.nuhDataStore.edit { it[Keys.KEY_POPUP_ENABLED] = enabled }
  }

  suspend fun setAutoCorrectEnabled(enabled: Boolean) {
    context.nuhDataStore.edit { it[Keys.AUTO_CORRECT_ENABLED] = enabled }
  }

  suspend fun setNextFidelHintsEnabled(enabled: Boolean) {
    context.nuhDataStore.edit { it[Keys.NEXT_FIDEL_HINTS_ENABLED] = enabled }
  }

  suspend fun setOneHandedSide(side: OneHandedSide) {
    context.nuhDataStore.edit { it[Keys.ONE_HANDED_SIDE] = side.name }
  }

  suspend fun setCustomDictionaryWords(words: Set<String>) {
    context.nuhDataStore.edit { it[Keys.CUSTOM_DICTIONARY_WORDS] = words }
  }

  suspend fun applyRemoteCloudSettings(
    inputMode: AmharicInputMode,
    foldLayoutPreference: FoldLayoutPreference,
    splitGapDp: Float,
    keyHeightDp: Float,
    letterSizeSp: Float,
    themeStyle: KeyboardThemeStyle,
    autoCorrectEnabled: Boolean,
    nextFidelHintsEnabled: Boolean,
  ) {
    context.nuhDataStore.edit { prefs ->
      prefs[Keys.INPUT_MODE] = inputMode.name
      prefs[Keys.FOLD_LAYOUT] = foldLayoutPreference.name
      prefs[Keys.SPLIT_GAP_DP] = splitGapDp.coerceIn(40f, 148f)
      prefs[Keys.KEY_HEIGHT_DP] = keyHeightDp.coerceIn(36f, 68f)
      prefs[Keys.LETTER_SIZE_SP] = letterSizeSp.coerceIn(14f, 26f)
      prefs[Keys.THEME_STYLE] = themeStyle.name
      prefs[Keys.AUTO_CORRECT_ENABLED] = autoCorrectEnabled
      prefs[Keys.NEXT_FIDEL_HINTS_ENABLED] = nextFidelHintsEnabled
    }
  }

  suspend fun resetDefaults() {
    context.nuhDataStore.edit { it.clear() }
  }
}
