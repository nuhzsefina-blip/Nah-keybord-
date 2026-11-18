package com.example.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AmharicInputMode
import com.example.data.KeyboardSettings
import com.example.data.OneHandedSide
import com.example.foldable.ZFoldPosture
import com.example.ime.FidelTable
import com.example.ime.KeyboardSessionController
import com.example.ime.KeyboardSessionUiState
import com.example.ime.KeyboardSubLayer
import com.example.ime.NextFidelCandidate
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.paletteFor

private sealed interface InPlaceSlot {
  data class Consonant(val latinHint: String, val familyId: String, val shiftedFamilyId: String? = null) : InPlaceSlot
  data class Vowel(val latinHint: String, val orderIndex: Int) : InPlaceSlot
  data object LabializedOrQuestion : InPlaceSlot
}

@Composable
fun NuhKeyboardPanel(
  settings: KeyboardSettings,
  posture: ZFoldPosture,
  controller: KeyboardSessionController,
  onSelectInputMode: (AmharicInputMode) -> Unit,
  onToggleInputMode: () -> Unit,
  onCyclePosture: () -> Unit,
  onToggleOneHanded: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by controller.uiState.collectAsStateWithLifecycle()
  val palette = paletteFor(settings.themeStyle)

  LaunchedEffect(
    settings.customDictionaryWords,
    settings.autoCorrectEnabled,
    settings.nextFidelHintsEnabled,
    settings.inputMode,
  ) {
    controller.updateDictionaryConfig(
      customDictionaryWords = settings.customDictionaryWords,
      autoCorrect = settings.autoCorrectEnabled,
      nextFidelHints = settings.nextFidelHintsEnabled,
      inputMode = settings.inputMode,
    )
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth().background(palette.boardBackground),
  ) {
    val availableWidth = maxWidth
    val isSplitLayout = posture == ZFoldPosture.UNFOLDED_SPLIT && availableWidth >= 500.dp
    val effectiveSplitGap =
      if (isSplitLayout) settings.splitGapDp.dp.coerceIn(48.dp, (availableWidth * 0.24f)) else 0.dp
    val keyHeight =
      when (posture) {
        ZFoldPosture.MULTI_WINDOW_PANE -> (settings.keyHeightDp - 4f).coerceAtLeast(36f).dp
        else -> settings.keyHeightDp.dp
      }

    val horizontalOneHandFraction =
      if (posture == ZFoldPosture.COVER_SINGLE && settings.oneHandedSide != OneHandedSide.NONE) {
        0.86f
      } else {
        1f
      }

    Column(
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Flex / Half-Folded Tabletop Preview Strip above the fold hinge
      AnimatedVisibility(visible = posture == ZFoldPosture.FLEX_TABLETOP) {
        FlexTabletopHingeBanner(
          uiState = uiState,
          inputMode = settings.inputMode,
          palette = palette,
        )
      }

      // Clean Top Prediction Bar with Next-Fidel & Auto-Correction chips
      if (uiState.subLayer == KeyboardSubLayer.LETTERS) {
        PredictionRibbon(
          uiState = uiState,
          inputMode = settings.inputMode,
          autoCorrectEnabled = settings.autoCorrectEnabled,
          nextFidelHintsEnabled = settings.nextFidelHintsEnabled,
          palette = palette,
          onSelectNextFidel = { controller.onNextFidelClicked(it, settings.inputMode) },
          onSelectSuggestion = { controller.onSuggestionClicked(it, settings.inputMode) },
          onUndoAutoCorrect = { controller.onUndoAutoCorrectClicked(settings.inputMode) },
        )
      }

      // Keyboard Body (with optional Cover Screen One-Handed Shift)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (horizontalOneHandFraction < 1f && settings.oneHandedSide == OneHandedSide.RIGHT) {
          OneHandedSideGutter(
            palette = palette,
            onFlip = onToggleOneHanded,
            modifier = Modifier.weight(1f - horizontalOneHandFraction),
          )
        }

        Box(modifier = Modifier.weight(horizontalOneHandFraction)) {
          when (uiState.subLayer) {
            KeyboardSubLayer.SYMBOLS,
            KeyboardSubLayer.ETHIOPIC_NUMERALS -> {
              SymbolsAndEthiopicLayer(
                isEthiopicNumerals = uiState.subLayer == KeyboardSubLayer.ETHIOPIC_NUMERALS,
                isSplit = isSplitLayout,
                splitGap = effectiveSplitGap,
                keyHeight = keyHeight,
                letterSizeSp = settings.letterSizeSp,
                palette = palette,
                hapticsEnabled = settings.hapticsEnabled,
                popupEnabled = settings.keyPopupEnabled,
                controller = controller,
                inputMode = settings.inputMode,
              )
            }
            KeyboardSubLayer.LETTERS -> {
              if (settings.inputMode == AmharicInputMode.FIDEL_INPLACE) {
                InPlaceFidelQwertyGrid(
                  isSplit = isSplitLayout,
                  splitGap = effectiveSplitGap,
                  keyHeight = keyHeight,
                  letterSizeSp = settings.letterSizeSp,
                  showNumberRow = settings.showNumberRow,
                  useEthiopicNumbers = settings.showEthiopicNumerals,
                  uiState = uiState,
                  palette = palette,
                  hapticsEnabled = settings.hapticsEnabled,
                  popupEnabled = settings.keyPopupEnabled,
                  controller = controller,
                  onToggleInputMode = onToggleInputMode,
                )
              } else {
                EnglishQwertyLetterGrid(
                  isSplit = isSplitLayout,
                  splitGap = effectiveSplitGap,
                  keyHeight = keyHeight,
                  letterSizeSp = settings.letterSizeSp,
                  showNumberRow = settings.showNumberRow,
                  isShifted = uiState.isShifted,
                  isCapsLocked = uiState.isCapsLocked,
                  palette = palette,
                  hapticsEnabled = settings.hapticsEnabled,
                  popupEnabled = settings.keyPopupEnabled,
                  controller = controller,
                  onToggleInputMode = onToggleInputMode,
                )
              }
            }
          }
        }

        if (horizontalOneHandFraction < 1f && settings.oneHandedSide == OneHandedSide.LEFT) {
          OneHandedSideGutter(
            palette = palette,
            onFlip = onToggleOneHanded,
            modifier = Modifier.weight(1f - horizontalOneHandFraction),
          )
        }
      }

      // Bottom Typing Engine & Foldable Controls Bar (anchored below the keyboard in the left corner)
      BottomKeyboardEngineBar(
        inputMode = settings.inputMode,
        posture = posture,
        subLayer = uiState.subLayer,
        palette = palette,
        showOneHandButton = posture == ZFoldPosture.COVER_SINGLE,
        onSelectInputMode = onSelectInputMode,
        onCyclePosture = onCyclePosture,
        onToggleEthiopicNumerals = { controller.toggleSubLayer(KeyboardSubLayer.ETHIOPIC_NUMERALS) },
        onToggleOneHanded = onToggleOneHanded,
        onOpenSettings = onOpenSettings,
      )
    }
  }
}

@Composable
private fun FlexTabletopHingeBanner(
  uiState: KeyboardSessionUiState,
  inputMode: AmharicInputMode,
  palette: KeyboardPalette,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              listOf(palette.toolbarBackground, palette.hingeBackground, palette.toolbarBackground)
            )
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.5f)),
      ) {
        Text(
          text =
            uiState.inPlaceActiveFamily?.baseChar
              ?: uiState.lastInsertedFidel.ifEmpty { "ኑህ" },
          color = palette.vowelMorphAccent,
          fontSize = 22.sp,
          fontFamily = EthiopicFontFamily,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
      }
      Column {
        Text(
          text =
            if (uiState.inPlaceActiveFamily != null) {
              "Active Consonant: ${uiState.inPlaceActiveFamily.baseChar} · Tap blue vowel keys"
            } else {
              "Flex Tabletop Mode · ${inputMode.label}"
            },
          color = palette.primaryText,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = BodyFontFamily,
        )
        Text(
          text = "Upper pane preview · Hinge-safe typing below",
          color = palette.secondaryText,
          fontSize = 10.sp,
          fontFamily = MonoFontFamily,
        )
      }
    }
    HorizontalDivider(thickness = 2.dp, color = palette.accentColor.copy(alpha = 0.45f))
  }
}

@Composable
private fun BottomKeyboardEngineBar(
  inputMode: AmharicInputMode,
  posture: ZFoldPosture,
  subLayer: KeyboardSubLayer,
  palette: KeyboardPalette,
  showOneHandButton: Boolean,
  onSelectInputMode: (AmharicInputMode) -> Unit,
  onCyclePosture: () -> Unit,
  onToggleEthiopicNumerals: () -> Unit,
  onToggleOneHanded: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  var engineMenuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(palette.toolbarBackground)
        .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left Corner: Typing Engine Selector Menu + Foldable Posture + Ethiopic Numerals
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box {
        Surface(
          onClick = { engineMenuExpanded = true },
          shape = RoundedCornerShape(8.dp),
          color = palette.accentColor.copy(alpha = 0.16f),
          border = BorderStroke(0.75.dp, palette.accentColor.copy(alpha = 0.6f)),
          modifier = Modifier.testTag("bottom_engine_menu"),
        ) {
          Row(
            modifier = Modifier.padding(start = 9.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = inputMode.badge,
              color = palette.accentColor,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = EthiopicFontFamily,
            )
            Text(
              text = if (inputMode == AmharicInputMode.FIDEL_INPLACE) "In-Place" else "EN",
              color = palette.primaryText,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
            )
            Icon(
              imageVector = Icons.Filled.ArrowDropDown,
              contentDescription = "Choose typing engine",
              tint = palette.accentColor,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        DropdownMenu(
          expanded = engineMenuExpanded,
          onDismissRequest = { engineMenuExpanded = false },
          containerColor = palette.toolbarBackground,
        ) {
          AmharicInputMode.entries.forEach { mode ->
            val selected = mode == inputMode
            DropdownMenuItem(
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = mode.badge,
                    color = palette.accentColor,
                    fontFamily = EthiopicFontFamily,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = mode.label,
                    color = palette.primaryText,
                    fontSize = 13.sp,
                  )
                }
              },
              trailingIcon =
                if (selected) {
                  {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = "Active engine",
                      tint = palette.accentColor,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else null,
              onClick = {
                engineMenuExpanded = false
                onSelectInputMode(mode)
              },
            )
          }
        }
      }

      Surface(
        onClick = onCyclePosture,
        shape = RoundedCornerShape(8.dp),
        color = palette.chipSurface,
        border = BorderStroke(0.75.dp, palette.keyBorder),
        modifier = Modifier.testTag("bottom_posture_cycle"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.DevicesFold,
            contentDescription = "Cycle Z Fold layout posture",
            tint = palette.secondaryAccent,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = posture.badge,
            color = palette.primaryText,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      IconButton(
        onClick = onToggleEthiopicNumerals,
        modifier = Modifier.size(30.dp).testTag("bottom_ethiopic_numerals"),
      ) {
        Text(
          text = "፩",
          color =
            if (subLayer == KeyboardSubLayer.ETHIOPIC_NUMERALS) palette.accentColor
            else palette.secondaryText,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = EthiopicFontFamily,
        )
      }
    }

    // Right Corner: Cover One-Handed Reach & Settings
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      if (showOneHandButton) {
        IconButton(onClick = onToggleOneHanded, modifier = Modifier.size(30.dp)) {
          Icon(
            imageVector = Icons.Filled.SwapHoriz,
            contentDescription = "Toggle one-handed cover shift",
            tint = palette.secondaryText,
            modifier = Modifier.size(17.dp),
          )
        }
      }
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.size(30.dp).testTag("bottom_settings"),
      ) {
        Icon(
          imageVector = Icons.Filled.Tune,
          contentDescription = "Keyboard height, letter size, and fold settings",
          tint = palette.secondaryText,
          modifier = Modifier.size(17.dp),
        )
      }
    }
  }
}

@Composable
private fun PredictionRibbon(
  uiState: KeyboardSessionUiState,
  inputMode: AmharicInputMode,
  autoCorrectEnabled: Boolean,
  nextFidelHintsEnabled: Boolean,
  palette: KeyboardPalette,
  onSelectNextFidel: (NextFidelCandidate) -> Unit,
  onSelectSuggestion: (String) -> Unit,
  onUndoAutoCorrect: () -> Unit,
) {
  LazyRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (inputMode == AmharicInputMode.FIDEL_INPLACE && uiState.autoCorrectedFrom != null) {
      item {
        Surface(
          onClick = onUndoAutoCorrect,
          shape = RoundedCornerShape(8.dp),
          color = palette.chipSurface,
          border = BorderStroke(0.75.dp, palette.secondaryText.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("undo_autocorrect_pill"),
        ) {
          Text(
            text = "↩ ${uiState.autoCorrectedFrom}",
            color = palette.secondaryText,
            fontSize = 12.sp,
            fontFamily = EthiopicFontFamily,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          )
        }
      }
    }

    if (inputMode == AmharicInputMode.FIDEL_INPLACE &&
      (uiState.inPlaceActiveFamily != null || uiState.lastInsertedFidel.isNotEmpty())
    ) {
      val activeGlyph = uiState.inPlaceActiveFamily?.baseChar ?: uiState.lastInsertedFidel
      item {
        Surface(
          shape = CircleShape,
          color = palette.chipSurface,
          border = BorderStroke(1.dp, palette.vowelMorphAccent.copy(alpha = 0.6f)),
          modifier = Modifier.testTag("active_fidel_pill"),
        ) {
          Text(
            text = activeGlyph,
            color = palette.primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = EthiopicFontFamily,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
          )
        }
      }
    }

    if (inputMode == AmharicInputMode.FIDEL_INPLACE &&
      nextFidelHintsEnabled &&
      uiState.nextFidelHints.isNotEmpty()
    ) {
      items(uiState.nextFidelHints.size) { idx ->
        val candidate = uiState.nextFidelHints[idx]
        val isMorph = candidate.replacesActiveConsonant
        val chipTint =
          if (isMorph) palette.vowelMorphAccent else palette.accentColor
        Surface(
          onClick = { onSelectNextFidel(candidate) },
          shape = RoundedCornerShape(8.dp),
          color = chipTint.copy(alpha = 0.14f),
          border = BorderStroke(0.9.dp, chipTint.copy(alpha = 0.65f)),
          modifier = Modifier.testTag("next_fidel_$idx"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = if (isMorph) "↻" else "+",
              color = chipTint,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = candidate.fidel,
              color = chipTint,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = EthiopicFontFamily,
            )
          }
        }
      }
    }

    items(uiState.suggestions.size) { idx ->
      val word = uiState.suggestions[idx]
      val highlightAutoCorrect = autoCorrectEnabled && word == uiState.autoCorrection
      Surface(
        onClick = { onSelectSuggestion(word) },
        shape = RoundedCornerShape(8.dp),
        color =
          if (highlightAutoCorrect) palette.accentColor.copy(alpha = 0.18f)
          else palette.toolbarBackground,
        border =
          BorderStroke(
            width = if (highlightAutoCorrect) 1.dp else 0.5.dp,
            color = if (highlightAutoCorrect) palette.accentColor else palette.keyBorder,
          ),
        modifier =
          Modifier.testTag(if (highlightAutoCorrect) "autocorrect_chip" else "suggestion_chip_$idx"),
      ) {
        Text(
          text = if (highlightAutoCorrect) "✓ $word" else word,
          color = if (highlightAutoCorrect) palette.accentColor else palette.primaryText,
          fontSize = 14.sp,
          fontWeight = if (highlightAutoCorrect) FontWeight.SemiBold else FontWeight.Normal,
          fontFamily = EthiopicFontFamily,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }
  }
}

@Composable
private fun InPlaceFidelQwertyGrid(
  isSplit: Boolean,
  splitGap: Dp,
  keyHeight: Dp,
  letterSizeSp: Float,
  showNumberRow: Boolean,
  useEthiopicNumbers: Boolean,
  uiState: KeyboardSessionUiState,
  palette: KeyboardPalette,
  hapticsEnabled: Boolean,
  popupEnabled: Boolean,
  controller: KeyboardSessionController,
  onToggleInputMode: () -> Unit,
) {
  val familyById = remember { FidelTable.families.associateBy { it.id } }
  val activeMorphFamily = uiState.inPlaceActiveFamily
  val idleVowelFamily =
    familyById[if (uiState.isShifted || uiState.isCapsLocked) "a2" else "a1"]!!
  val vowelSourceFamily = activeMorphFamily ?: idleVowelFamily
  val vowelHighlighted = activeMorphFamily != null

  val numLeft =
    if (useEthiopicNumbers) listOf("፩", "፪", "፫", "፬", "፭") else listOf("1", "2", "3", "4", "5")
  val numRight =
    if (useEthiopicNumbers) listOf("፮", "፯", "፰", "፱", "፲") else listOf("6", "7", "8", "9", "0")

  val row1Left =
    listOf(
      InPlaceSlot.Consonant("q", "q"),
      InPlaceSlot.Consonant("w", "w"),
      InPlaceSlot.Vowel("e", 5),
      InPlaceSlot.Consonant("r", "r"),
      InPlaceSlot.Consonant("t", "t", "T"),
    )
  val row1Right =
    listOf(
      InPlaceSlot.Consonant("y", "y"),
      InPlaceSlot.Vowel("u", 1),
      InPlaceSlot.Vowel("i", 2),
      InPlaceSlot.Vowel("o", 6),
      InPlaceSlot.Consonant("p", "p", "P"),
    )

  val row2Left =
    listOf(
      InPlaceSlot.Vowel("a", 3),
      InPlaceSlot.Consonant("s", "s1", "sh"),
      InPlaceSlot.Consonant("d", "d", "ts"),
      InPlaceSlot.Consonant("f", "f", "s2"),
      InPlaceSlot.Consonant("g", "g"),
    )
  val row2Right =
    listOf(
      InPlaceSlot.Consonant("h", "h1", "h2"),
      InPlaceSlot.Consonant("j", "j", "h3"),
      InPlaceSlot.Consonant("k", "k", "kx"),
      InPlaceSlot.Consonant("l", "l"),
      InPlaceSlot.Vowel("ie", 4),
      InPlaceSlot.LabializedOrQuestion,
    )

  val row3Left =
    listOf(
      InPlaceSlot.Consonant("z", "z", "zh"),
      InPlaceSlot.Consonant("x", "a1", "a2"),
      InPlaceSlot.Consonant("c", "ch", "C"),
      InPlaceSlot.Consonant("v", "v", "tz"),
    )
  val row3Right =
    listOf(
      InPlaceSlot.Consonant("b", "b"),
      InPlaceSlot.Consonant("n", "n", "ny"),
      InPlaceSlot.Consonant("m", "m"),
    )

  @Composable
  fun RowScope.RenderSlot(slot: InPlaceSlot) {
    when (slot) {
      is InPlaceSlot.Consonant -> {
        val targetId =
          if ((uiState.isShifted || uiState.isCapsLocked) && slot.shiftedFamilyId != null) {
            slot.shiftedFamilyId
          } else {
            slot.familyId
          }
        val family = familyById[targetId] ?: familyById[slot.familyId]!!
        val altVariants =
          buildList {
            add(family.baseChar)
            slot.shiftedFamilyId?.let { altId ->
              familyById[altId]?.baseChar?.let { if (!contains(it)) add(it) }
            }
            if (family.id == "h1") {
              familyById["h2"]?.baseChar?.let { if (!contains(it)) add(it) }
              familyById["h3"]?.baseChar?.let { if (!contains(it)) add(it) }
            }
            if (family.id == "s1") {
              familyById["s2"]?.baseChar?.let { if (!contains(it)) add(it) }
            }
          }
        NuhKeyButton(
          primaryLabel = family.baseChar,
          secondaryHint = slot.latinHint,
          palette = palette,
          keyHeight = keyHeight,
          isEthiopic = true,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          longPressVariants = if (altVariants.size > 1) altVariants else family.allVariants,
          testTag = "inplace_key_${slot.latinHint}",
          onClick = { controller.onInPlaceConsonantPressed(family) },
          onSelectLongPressVariant = { chosen ->
            val chosenFamily = FidelTable.findFamilyForChar(chosen)
            if (chosenFamily != null) {
              controller.onInPlaceConsonantPressed(chosenFamily)
            }
          },
        )
      }
      is InPlaceSlot.Vowel -> {
        val glyph = vowelSourceFamily.orders.getOrElse(slot.orderIndex) { vowelSourceFamily.baseChar }
        NuhKeyButton(
          primaryLabel = glyph,
          secondaryHint = slot.latinHint,
          palette = palette,
          keyHeight = keyHeight,
          isEthiopic = true,
          isVowelMorphActive = vowelHighlighted,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "inplace_vowel_${slot.latinHint}",
          onClick = { controller.onInPlaceVowelPressed(slot.orderIndex) },
        )
      }
      InPlaceSlot.LabializedOrQuestion -> {
        val lab = activeMorphFamily?.labialized.orEmpty()
        val label =
          if (activeMorphFamily != null && lab.isNotEmpty()) {
            if (lab.size >= 3) lab[2] else lab.first()
          } else {
            ""
          }
        NuhKeyButton(
          primaryLabel = label,
          secondaryHint = "?",
          palette = palette,
          keyHeight = keyHeight,
          isEthiopic = label.isNotEmpty(),
          isVowelMorphActive = activeMorphFamily != null && lab.isNotEmpty(),
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled && label.isNotEmpty(),
          longPressVariants = if (lab.size > 1) lab else listOf("፧", "?"),
          testTag = "inplace_key_lab_q",
          onClick = { controller.onInPlaceLabializedPressed() },
          onSelectLongPressVariant = { chosen ->
            controller.onInPlaceLabializedPressed(chosen)
          },
        )
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    if (showNumberRow) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        numLeft.forEach { n ->
          NuhKeyButton(
            primaryLabel = n,
            palette = palette,
            keyHeight = (keyHeight - 8.dp).coerceAtLeast(30.dp),
            isEthiopic = useEthiopicNumbers,
            letterSizeSp = (letterSizeSp - 2f).coerceAtLeast(13f),
            hapticsEnabled = hapticsEnabled,
            popupEnabled = popupEnabled,
            onClick = { controller.onInsertSymbolOrText(n, AmharicInputMode.FIDEL_INPLACE) },
          )
        }
        if (isSplit) {
          Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
            CenterHingeSpineButton(
              label = "፡",
              contentDesc = "Ethiopic Wordspace",
              palette = palette,
              height = (keyHeight - 8.dp).coerceAtLeast(30.dp),
              onClick = { controller.onInsertSymbolOrText("፡", AmharicInputMode.FIDEL_INPLACE) },
            )
          }
        }
        numRight.forEach { n ->
          NuhKeyButton(
            primaryLabel = n,
            palette = palette,
            keyHeight = (keyHeight - 8.dp).coerceAtLeast(30.dp),
            isEthiopic = useEthiopicNumbers,
            letterSizeSp = (letterSizeSp - 2f).coerceAtLeast(13f),
            hapticsEnabled = hapticsEnabled,
            popupEnabled = popupEnabled,
            onClick = { controller.onInsertSymbolOrText(n, AmharicInputMode.FIDEL_INPLACE) },
          )
        }
      }
    }

    // Row 1
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      row1Left.forEach { RenderSlot(it) }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = "።",
            contentDesc = "Ethiopic Full Stop",
            palette = palette,
            height = keyHeight,
            isHighlight = true,
            onClick = { controller.onInsertSymbolOrText("።", AmharicInputMode.FIDEL_INPLACE) },
          )
        }
      }
      row1Right.forEach { RenderSlot(it) }
    }

    // Row 2
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      row2Left.forEach { RenderSlot(it) }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = "፣",
            contentDesc = "Ethiopic Comma",
            palette = palette,
            height = keyHeight,
            onClick = { controller.onInsertSymbolOrText("፣", AmharicInputMode.FIDEL_INPLACE) },
          )
        }
      }
      row2Right.forEach { RenderSlot(it) }
    }

    // Row 3
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NuhIconKeyButton(
        icon =
          if (uiState.isCapsLocked) Icons.Filled.KeyboardCapslock
          else Icons.Filled.KeyboardDoubleArrowUp,
        contentDesc = "Shift for secondary Ethiopic consonants",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.35f,
        isHighlighted = uiState.isShifted || uiState.isCapsLocked,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_shift",
        onClick = { controller.onShiftPressed() },
      )
      row3Left.forEach { RenderSlot(it) }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = "፧",
            contentDesc = "Ethiopic Question Mark",
            palette = palette,
            height = keyHeight,
            onClick = { controller.onInsertSymbolOrText("፧", AmharicInputMode.FIDEL_INPLACE) },
          )
        }
      }
      row3Right.forEach { RenderSlot(it) }
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.Backspace,
        contentDesc = "Backspace",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.45f,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_backspace",
        onClick = { controller.onBackspacePressed(AmharicInputMode.FIDEL_INPLACE) },
      )
    }

    // Row 4
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NuhKeyButton(
        primaryLabel = "?12",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.2f,
        isModifier = true,
        isVowelMorphActive = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        testTag = "key_symbols",
        onClick = { controller.setSubLayer(KeyboardSubLayer.SYMBOLS) },
      )
      NuhKeyButton(
        primaryLabel = "EN",
        secondaryHint = "አማ",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.15f,
        isModifier = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        testTag = "key_language",
        onClick = onToggleInputMode,
      )
      NuhKeyButton(
        primaryLabel = "፡",
        secondaryHint = "፣",
        palette = palette,
        keyHeight = keyHeight,
        weight = 0.9f,
        isEthiopic = true,
        isModifier = true,
        isVowelMorphActive = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        longPressVariants = listOf("፡", "፣", "፤", "፥", "፦"),
        onClick = { controller.onInsertSymbolOrText("፡", AmharicInputMode.FIDEL_INPLACE) },
        onSelectLongPressVariant = {
          controller.onInsertSymbolOrText(it, AmharicInputMode.FIDEL_INPLACE)
        },
      )
      if (isSplit) {
        NuhSpaceBar(
          label = "አማርኛ",
          palette = palette,
          keyHeight = keyHeight,
          weight = 2.4f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space_left",
          onClick = { controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(
              onClick = { controller.moveCursor(-1) },
              shape = RoundedCornerShape(8.dp),
              color = palette.toolbarBackground,
              border = BorderStroke(0.75.dp, palette.keyBorder),
              modifier = Modifier.weight(1f).height(keyHeight),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Cursor left",
                  tint = palette.secondaryText,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Surface(
              onClick = { controller.moveCursor(1) },
              shape = RoundedCornerShape(8.dp),
              color = palette.toolbarBackground,
              border = BorderStroke(0.75.dp, palette.keyBorder),
              modifier = Modifier.weight(1f).height(keyHeight),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Cursor right",
                  tint = palette.secondaryText,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
        NuhSpaceBar(
          label = "ኑህ",
          palette = palette,
          keyHeight = keyHeight,
          weight = 2.4f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space_right",
          onClick = { controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
      } else {
        NuhSpaceBar(
          label = "◀  አማርኛ  ▶",
          palette = palette,
          keyHeight = keyHeight,
          weight = 3.6f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space",
          onClick = { controller.onSpacePressed(AmharicInputMode.FIDEL_INPLACE) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
      }
      NuhKeyButton(
        primaryLabel = "።",
        secondaryHint = "፧",
        palette = palette,
        keyHeight = keyHeight,
        weight = 0.95f,
        isEthiopic = true,
        isModifier = true,
        isVowelMorphActive = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        longPressVariants = listOf("።", "፧", "!", ".", "፨"),
        onClick = { controller.onInsertSymbolOrText("።", AmharicInputMode.FIDEL_INPLACE) },
        onSelectLongPressVariant = {
          controller.onInsertSymbolOrText(it, AmharicInputMode.FIDEL_INPLACE)
        },
      )
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.KeyboardReturn,
        contentDesc = "Enter",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.3f,
        isAccent = true,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_enter",
        onClick = { controller.onEnterPressed() },
      )
    }
  }
}

@Composable
private fun EnglishQwertyLetterGrid(
  isSplit: Boolean,
  splitGap: Dp,
  keyHeight: Dp,
  letterSizeSp: Float,
  showNumberRow: Boolean,
  isShifted: Boolean,
  isCapsLocked: Boolean,
  palette: KeyboardPalette,
  hapticsEnabled: Boolean,
  popupEnabled: Boolean,
  controller: KeyboardSessionController,
  onToggleInputMode: () -> Unit,
) {
  val numLeft = listOf("1", "2", "3", "4", "5")
  val numRight = listOf("6", "7", "8", "9", "0")
  val r1Left = listOf('q', 'w', 'e', 'r', 't')
  val r1Right = listOf('y', 'u', 'i', 'o', 'p')
  val r2Left = listOf('a', 's', 'd', 'f', 'g')
  val r2Right = listOf('h', 'j', 'k', 'l')
  val r3Left = listOf('z', 'x', 'c', 'v')
  val r3Right = listOf('b', 'n', 'm')

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    if (showNumberRow) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        numLeft.forEach { n ->
          NuhKeyButton(
            primaryLabel = n,
            palette = palette,
            keyHeight = (keyHeight - 8.dp).coerceAtLeast(30.dp),
            letterSizeSp = (letterSizeSp - 2f).coerceAtLeast(13f),
            hapticsEnabled = hapticsEnabled,
            popupEnabled = popupEnabled,
            onClick = { controller.onInsertSymbolOrText(n, AmharicInputMode.ENGLISH_QWERTY) },
          )
        }
        if (isSplit) {
          Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
            CenterHingeSpineButton(
              label = "-",
              contentDesc = "Hyphen",
              palette = palette,
              height = (keyHeight - 8.dp).coerceAtLeast(30.dp),
              onClick = { controller.onInsertSymbolOrText("-", AmharicInputMode.ENGLISH_QWERTY) },
            )
          }
        }
        numRight.forEach { n ->
          NuhKeyButton(
            primaryLabel = n,
            palette = palette,
            keyHeight = (keyHeight - 8.dp).coerceAtLeast(30.dp),
            letterSizeSp = (letterSizeSp - 2f).coerceAtLeast(13f),
            hapticsEnabled = hapticsEnabled,
            popupEnabled = popupEnabled,
            onClick = { controller.onInsertSymbolOrText(n, AmharicInputMode.ENGLISH_QWERTY) },
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      r1Left.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = ".",
            contentDesc = "Period",
            palette = palette,
            height = keyHeight,
            isHighlight = true,
            onClick = { controller.onInsertSymbolOrText(".", AmharicInputMode.ENGLISH_QWERTY) },
          )
        }
      }
      r1Right.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!isSplit) Spacer(Modifier.weight(0.45f))
      r2Left.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = ",",
            contentDesc = "Comma",
            palette = palette,
            height = keyHeight,
            onClick = { controller.onInsertSymbolOrText(",", AmharicInputMode.ENGLISH_QWERTY) },
          )
        }
      }
      r2Right.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
      if (isSplit) {
        NuhKeyButton(
          primaryLabel = "?",
          palette = palette,
          keyHeight = keyHeight,
          isModifier = true,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          onClick = { controller.onInsertSymbolOrText("?", AmharicInputMode.ENGLISH_QWERTY) },
        )
      } else {
        Spacer(Modifier.weight(0.45f))
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NuhIconKeyButton(
        icon =
          if (isCapsLocked) Icons.Filled.KeyboardCapslock
          else Icons.Filled.KeyboardDoubleArrowUp,
        contentDesc = "Shift",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.35f,
        isHighlighted = isShifted || isCapsLocked,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_shift",
        onClick = { controller.onShiftPressed() },
      )
      r3Left.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
      if (isSplit) {
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          CenterHingeSpineButton(
            label = "!",
            contentDesc = "Exclamation mark",
            palette = palette,
            height = keyHeight,
            onClick = { controller.onInsertSymbolOrText("!", AmharicInputMode.ENGLISH_QWERTY) },
          )
        }
      }
      r3Right.forEach { ch ->
        val label = if (isShifted || isCapsLocked) ch.uppercase() else ch.lowercase()
        NuhKeyButton(
          primaryLabel = label,
          palette = palette,
          keyHeight = keyHeight,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = popupEnabled,
          testTag = "key_$ch",
          onClick = { controller.onEnglishKeyPressed(ch) },
        )
      }
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.Backspace,
        contentDesc = "Backspace",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.45f,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_backspace",
        onClick = { controller.onBackspacePressed(AmharicInputMode.ENGLISH_QWERTY) },
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NuhKeyButton(
        primaryLabel = "?123",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.25f,
        isModifier = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        testTag = "key_symbols",
        onClick = { controller.setSubLayer(KeyboardSubLayer.SYMBOLS) },
      )
      NuhKeyButton(
        primaryLabel = "አማ",
        secondaryHint = "EN",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.15f,
        isEthiopic = true,
        isModifier = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        testTag = "key_language",
        onClick = onToggleInputMode,
      )
      if (isSplit) {
        NuhSpaceBar(
          label = "Space",
          palette = palette,
          keyHeight = keyHeight,
          weight = 2.6f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space_left",
          onClick = { controller.onSpacePressed(AmharicInputMode.ENGLISH_QWERTY) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
        Box(modifier = Modifier.width(splitGap), contentAlignment = Alignment.Center) {
          Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(
              onClick = { controller.moveCursor(-1) },
              shape = RoundedCornerShape(8.dp),
              color = palette.toolbarBackground,
              border = BorderStroke(0.75.dp, palette.keyBorder),
              modifier = Modifier.weight(1f).height(keyHeight),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Cursor left",
                  tint = palette.secondaryText,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Surface(
              onClick = { controller.moveCursor(1) },
              shape = RoundedCornerShape(8.dp),
              color = palette.toolbarBackground,
              border = BorderStroke(0.75.dp, palette.keyBorder),
              modifier = Modifier.weight(1f).height(keyHeight),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Cursor right",
                  tint = palette.secondaryText,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
        NuhSpaceBar(
          label = "EN",
          palette = palette,
          keyHeight = keyHeight,
          weight = 2.6f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space_right",
          onClick = { controller.onSpacePressed(AmharicInputMode.ENGLISH_QWERTY) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
      } else {
        NuhSpaceBar(
          label = "◀  English  ▶",
          palette = palette,
          keyHeight = keyHeight,
          weight = 3.8f,
          hapticsEnabled = hapticsEnabled,
          testTag = "key_space",
          onClick = { controller.onSpacePressed(AmharicInputMode.ENGLISH_QWERTY) },
          onSwipeCursor = { controller.moveCursor(it) },
        )
        NuhKeyButton(
          primaryLabel = ".",
          palette = palette,
          keyHeight = keyHeight,
          weight = 0.95f,
          isModifier = true,
          letterSizeSp = letterSizeSp,
          hapticsEnabled = hapticsEnabled,
          popupEnabled = false,
          onClick = { controller.onInsertSymbolOrText(".", AmharicInputMode.ENGLISH_QWERTY) },
        )
      }
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.KeyboardReturn,
        contentDesc = "Enter",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.35f,
        isAccent = true,
        hapticsEnabled = hapticsEnabled,
        testTag = "key_enter",
        onClick = { controller.onEnterPressed() },
      )
    }
  }
}

@Composable
private fun OneHandedSideGutter(
  palette: KeyboardPalette,
  onFlip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      onClick = onFlip,
      shape = CircleShape,
      color = palette.chipSurface,
      border = BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.5f)),
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Filled.SwapHoriz,
          contentDescription = "Flip one-handed side",
          tint = palette.accentColor,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
