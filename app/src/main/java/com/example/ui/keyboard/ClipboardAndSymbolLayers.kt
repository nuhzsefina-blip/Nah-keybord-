package com.example.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AmharicInputMode
import com.example.ime.KeyboardSessionController
import com.example.ime.KeyboardSubLayer
import com.example.ui.theme.KeyboardPalette

@Composable
fun SymbolsAndEthiopicLayer(
  isEthiopicNumerals: Boolean,
  isSplit: Boolean,
  splitGap: Dp,
  keyHeight: Dp,
  letterSizeSp: Float,
  palette: KeyboardPalette,
  hapticsEnabled: Boolean,
  popupEnabled: Boolean,
  controller: KeyboardSessionController,
  inputMode: AmharicInputMode,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    if (isEthiopicNumerals) {
      val row1 = listOf("፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱", "፲")
      val row2 = listOf("፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺", "፻", "፼")
      val row3 = listOf("፡", "።", "፣", "፤", "፥", "፦", "፧", "፨", "«", "»")

      SplitCapableSymbolRow(
        keys = row1,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = true,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
      SplitCapableSymbolRow(
        keys = row2,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = true,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
      SplitCapableSymbolRow(
        keys = row3,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = true,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
    } else {
      val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
      val row2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
      val row3 = listOf("*", "\"", "'", ":", ";", "!", "?", "።", "፣", "፧")

      SplitCapableSymbolRow(
        keys = row1,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = false,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
      SplitCapableSymbolRow(
        keys = row2,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = false,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
      SplitCapableSymbolRow(
        keys = row3,
        isSplit = isSplit,
        splitGap = splitGap,
        keyHeight = keyHeight,
        letterSizeSp = letterSizeSp,
        palette = palette,
        isEthiopic = false,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onKey = { controller.onInsertSymbolOrText(it, inputMode) },
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NuhKeyButton(
        primaryLabel = "ABC",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.4f,
        isModifier = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        onClick = { controller.setSubLayer(KeyboardSubLayer.LETTERS) },
      )
      NuhKeyButton(
        primaryLabel = if (isEthiopicNumerals) "123" else "፩፪፫",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.3f,
        isEthiopic = !isEthiopicNumerals,
        isModifier = true,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = false,
        onClick = {
          controller.setSubLayer(
            if (isEthiopicNumerals) KeyboardSubLayer.SYMBOLS else KeyboardSubLayer.ETHIOPIC_NUMERALS
          )
        },
      )
      NuhSpaceBar(
        label = "Space · ${inputMode.badge}",
        palette = palette,
        keyHeight = keyHeight,
        weight = 3.2f,
        hapticsEnabled = hapticsEnabled,
        onClick = { controller.onSpacePressed(inputMode) },
        onSwipeCursor = { controller.moveCursor(it) },
      )
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.Backspace,
        contentDesc = "Backspace",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.2f,
        hapticsEnabled = hapticsEnabled,
        onClick = { controller.onBackspacePressed(inputMode) },
      )
      NuhIconKeyButton(
        icon = Icons.AutoMirrored.Filled.KeyboardReturn,
        contentDesc = "Enter",
        palette = palette,
        keyHeight = keyHeight,
        weight = 1.3f,
        isAccent = true,
        hapticsEnabled = hapticsEnabled,
        onClick = { controller.onEnterPressed() },
      )
    }
  }
}

@Composable
private fun SplitCapableSymbolRow(
  keys: List<String>,
  isSplit: Boolean,
  splitGap: Dp,
  keyHeight: Dp,
  letterSizeSp: Float,
  palette: KeyboardPalette,
  isEthiopic: Boolean,
  hapticsEnabled: Boolean,
  popupEnabled: Boolean,
  onKey: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val mid = keys.size / 2
    keys.forEachIndexed { idx, symbol ->
      if (isSplit && idx == mid) {
        Spacer(Modifier.width(splitGap))
      }
      NuhKeyButton(
        primaryLabel = symbol,
        palette = palette,
        keyHeight = keyHeight,
        isEthiopic = isEthiopic,
        letterSizeSp = letterSizeSp,
        hapticsEnabled = hapticsEnabled,
        popupEnabled = popupEnabled,
        onClick = { onKey(symbol) },
      )
    }
  }
}
