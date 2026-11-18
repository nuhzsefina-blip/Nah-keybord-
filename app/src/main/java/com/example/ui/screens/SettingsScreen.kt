package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AmharicInputMode
import com.example.data.FoldLayoutPreference
import com.example.data.KeyboardSettings
import com.example.data.OneHandedSide
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.paletteFor
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: KeyboardSettings,
  userDisplayName: String,
  userId: String,
  isSyncingCloud: Boolean,
  onSyncNowClick: () -> Unit,
  onSignOutClick: () -> Unit,
  onSetInputMode: (AmharicInputMode) -> Unit,
  onSetFoldLayoutPreference: (FoldLayoutPreference) -> Unit,
  onSetSplitGapDp: (Float) -> Unit,
  onSetKeyHeightDp: (Float) -> Unit,
  onSetLetterSizeSp: (Float) -> Unit,
  onSetThemeStyle: (KeyboardThemeStyle) -> Unit,
  onSetOneHandedSide: (OneHandedSide) -> Unit,
  onSetShowNumberRow: (Boolean) -> Unit,
  onSetShowEthiopicNumerals: (Boolean) -> Unit,
  onSetHapticsEnabled: (Boolean) -> Unit,
  onSetKeyPopupEnabled: (Boolean) -> Unit,
  onSetAutoCorrectEnabled: (Boolean) -> Unit,
  onSetNextFidelHintsEnabled: (Boolean) -> Unit,
  onResetDefaults: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activePalette = paletteFor(settings.themeStyle)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            "Adjust keyboard height, letter size, Samsung Z Fold 7 split hinge spacing, and cloud sync.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Google Account & Cloud Sync Card
    item {
      SettingSectionCard(title = "Google Account & Cloud Sync") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
              )
              Column {
                Text(
                  text = userDisplayName,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Synced ID: ${userId.take(10)}…",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = MonoFontFamily,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Icon(
              imageVector = Icons.Filled.CloudDone,
              contentDescription = "Cloud connected",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onSyncNowClick,
              enabled = !isSyncingCloud,
              modifier = Modifier.weight(1f).testTag("btn_sync_cloud"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(if (isSyncingCloud) "Syncing…" else "Sync Cloud Now")
            }
            OutlinedButton(
              onClick = onSignOutClick,
              modifier = Modifier.weight(1f).testTag("btn_sign_out"),
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.size(6.dp))
              Text("Sign Out")
            }
          }
        }
      }
    }

    // Keyboard Height & Letter Size Controls with Live Keycap Preview
    item {
      SettingSectionCard(title = "Keyboard Height & Letter Size") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Live Preview of key height and letter size
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = activePalette.boardBackground,
            border = BorderStroke(1.dp, activePalette.keyBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              listOf(
                Triple("ወ", "w", false),
                Triple("ው", "e", true),
                Triple("ሰ", "s", false),
                Triple("A", "", false),
              ).forEach { (glyph, hint, isBlue) ->
                val isEth = glyph != "A"
                Surface(
                  modifier = Modifier.weight(1f).height(settings.keyHeightDp.dp),
                  shape = RoundedCornerShape(10.dp),
                  color = activePalette.keySurface,
                  border =
                    BorderStroke(
                      width = if (isBlue) 1.dp else 0.75.dp,
                      color =
                        if (isBlue) activePalette.vowelMorphAccent.copy(alpha = 0.6f)
                        else activePalette.keyBorder,
                    ),
                ) {
                  Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    if (hint.isNotEmpty()) {
                      Text(
                        text = hint,
                        fontSize = 9.sp,
                        color =
                          if (isBlue) activePalette.vowelMorphAccent
                          else activePalette.secondaryText,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp),
                      )
                    }
                    Text(
                      text = glyph,
                      fontSize =
                        if (isEth) settings.letterSizeSp.sp
                        else (settings.letterSizeSp - 2f).coerceAtLeast(12f).sp,
                      fontWeight = if (isBlue) FontWeight.Bold else FontWeight.Medium,
                      fontFamily = if (isEth) EthiopicFontFamily else DisplayFontFamily,
                      color =
                        if (isBlue) activePalette.vowelMorphAccent else activePalette.primaryText,
                      modifier = Modifier.align(Alignment.Center),
                    )
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Keyboard Height", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${settings.keyHeightDp.roundToInt()} dp",
              fontFamily = MonoFontFamily,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = settings.keyHeightDp,
            onValueChange = onSetKeyHeightDp,
            valueRange = 36f..68f,
            modifier = Modifier.testTag("slider_keyboard_height"),
            colors =
              SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
              ),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Letter Size", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${settings.letterSizeSp.roundToInt()} sp",
              fontFamily = MonoFontFamily,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = settings.letterSizeSp,
            onValueChange = onSetLetterSizeSp,
            valueRange = 14f..26f,
            modifier = Modifier.testTag("slider_letter_size"),
            colors =
              SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
              ),
          )
        }
      }
    }

    // Default Input Mode (Only አማ In-Place and EN)
    item {
      SettingSectionCard(title = "Default Typing Engine") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AmharicInputMode.entries.forEach { mode ->
            FilterChip(
              selected = settings.inputMode == mode,
              onClick = { onSetInputMode(mode) },
              label = { Text(mode.label) },
              modifier = Modifier.weight(1f),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
          }
        }
      }
    }

    // Z Fold 7 Foldable Layout & Hinge Sliders
    item {
      SettingSectionCard(title = "Z Fold 7 Foldable Posture & Hinge") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          FoldLayoutPreference.entries.forEach { pref ->
            val selected = settings.foldLayoutPreference == pref
            Surface(
              onClick = { onSetFoldLayoutPreference(pref) },
              shape = RoundedCornerShape(12.dp),
              color =
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
              border =
                BorderStroke(
                  width = if (selected) 1.25.dp else 0.75.dp,
                  color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = pref.label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Unfolded Split Hinge Gap", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${settings.splitGapDp.roundToInt()} dp",
              fontFamily = MonoFontFamily,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = settings.splitGapDp,
            onValueChange = onSetSplitGapDp,
            valueRange = 48f..144f,
            modifier = Modifier.testTag("slider_split_gap"),
            colors =
              SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
              ),
          )

          Text("Cover Screen One-Handed Reach", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OneHandedSide.entries.forEach { side ->
              FilterChip(
                selected = settings.oneHandedSide == side,
                onClick = { onSetOneHandedSide(side) },
                label = { Text(side.label) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    // Visual Theme Swatches
    item {
      SettingSectionCard(title = "Keyboard Theme Palette") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          KeyboardThemeStyle.entries.forEach { style ->
            val palette = paletteFor(style)
            val selected = settings.themeStyle == style
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSetThemeStyle(style) }
                  .testTag("theme_${style.name}"),
              shape = RoundedCornerShape(14.dp),
              color = palette.boardBackground,
              border =
                BorderStroke(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) palette.accentColor else palette.keyBorder,
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Box(
                    modifier =
                      Modifier.size(28.dp)
                        .clip(CircleShape)
                        .background(palette.accentColor)
                  )
                  Column {
                    Text(
                      text = palette.label,
                      style = MaterialTheme.typography.titleSmall,
                      color = palette.primaryText,
                    )
                    Text(
                      text = palette.description,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.secondaryText,
                    )
                  }
                }
                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected theme",
                    tint = palette.accentColor,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Typing Behavior Toggles
    item {
      SettingSectionCard(title = "Typing & Feedback") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          ToggleSettingRow(
            title = "Dedicated Number Row",
            subtitle = "Display 1–0 (or ፩–፲) above the top letter row",
            checked = settings.showNumberRow,
            onCheckedChange = onSetShowNumberRow,
          )
          ToggleSettingRow(
            title = "Ethiopic Numerals (፩–፲) on Number Row",
            subtitle = "Show Ge'ez numerals instead of Arabic digits when typing Amharic",
            checked = settings.showEthiopicNumerals,
            onCheckedChange = onSetShowEthiopicNumerals,
          )
          ToggleSettingRow(
            title = "Haptic Keystroke Feedback",
            subtitle = "Subtle tactile pulse on key press and long-press",
            checked = settings.hapticsEnabled,
            onCheckedChange = onSetHapticsEnabled,
          )
          ToggleSettingRow(
            title = "Key Preview Popups",
            subtitle = "Show magnified glyph badge while pressing a key",
            checked = settings.keyPopupEnabled,
            onCheckedChange = onSetKeyPopupEnabled,
          )
          ToggleSettingRow(
            title = "Next Correct Fidel Suggestions",
            subtitle = "Suggest the next Fidel character or vowel order from the Amharic dictionary while typing",
            checked = settings.nextFidelHintsEnabled,
            onCheckedChange = onSetNextFidelSuggestionsEnabled,
            switchModifier = Modifier.testTag("toggle_next_fidel"),
          )
          ToggleSettingRow(
            title = "Amharic Auto-Correction",
            subtitle = "Auto-correct Fidel vowel order and spelling mistakes when pressing Space",
            checked = settings.autoCorrectEnabled,
            onCheckedChange = onSetAutoCorrectEnabled,
            switchModifier = Modifier.testTag("toggle_auto_correct"),
          )
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onResetDefaults,
        modifier = Modifier.fillMaxWidth().testTag("btn_reset_defaults"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Reset to Z Fold 7 Recommended Defaults")
      }
    }
  }
}

@Composable
private fun SettingSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      content()
    }
  }
}

@Composable
private fun ToggleSettingRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  switchModifier: Modifier = Modifier,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = switchModifier)
  }
}
