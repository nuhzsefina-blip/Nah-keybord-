package com.example.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.ime.FidelFamily
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.MonoFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.NuhKeyButton(
  primaryLabel: String,
  secondaryHint: String? = null,
  palette: KeyboardPalette,
  keyHeight: Dp,
  weight: Float = 1f,
  isEthiopic: Boolean = false,
  isModifier: Boolean = false,
  isAccent: Boolean = false,
  isVowelMorphActive: Boolean = false,
  letterSizeSp: Float = 19f,
  hapticsEnabled: Boolean = true,
  popupEnabled: Boolean = true,
  longPressVariants: List<String> = emptyList(),
  testTag: String? = null,
  onClick: () -> Unit,
  onSelectLongPressVariant: ((String) -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  var showVariantsMenu by remember { mutableStateOf(false) }

  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.94f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessHigh),
      label = "key_scale",
    )

  val bgColor =
    when {
      isAccent -> palette.accentKeySurface
      isPressed -> palette.keySurfacePressed
      isModifier -> palette.modifierKeySurface
      else -> palette.keySurface
    }
  val fgColor =
    when {
      isAccent -> palette.accentKeyText
      isVowelMorphActive -> palette.vowelMorphAccent
      else -> palette.primaryText
    }
  val borderColor =
    if (isVowelMorphActive) palette.vowelMorphAccent.copy(alpha = 0.55f) else palette.keyBorder

  Box(
    modifier =
      Modifier.weight(weight)
        .height(keyHeight)
        .scale(scale)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .fillMaxHeight()
          .clip(RoundedCornerShape(10.dp))
          .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
              if (hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onClick()
            },
            onLongClick =
              if (longPressVariants.isNotEmpty() && onSelectLongPressVariant != null) {
                {
                  if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                  showVariantsMenu = true
                }
              } else {
                null
              },
          ),
      shape = RoundedCornerShape(10.dp),
      color = bgColor,
      border = androidx.compose.foundation.BorderStroke(if (isVowelMorphActive) 1.dp else 0.75.dp, borderColor),
      shadowElevation = if (isPressed) 0.dp else 2.dp,
    ) {
      Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 3.dp)) {
        if (!secondaryHint.isNullOrEmpty()) {
          Text(
            text = secondaryHint,
            color =
              if (isVowelMorphActive) palette.vowelMorphAccent.copy(alpha = 0.9f)
              else palette.secondaryText.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontFamily = EthiopicFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp),
          )
        }
        val computedSize =
          when {
            isModifier -> (letterSizeSp - 5f).coerceIn(11f, 16f).sp
            isEthiopic -> letterSizeSp.sp
            else -> (letterSizeSp - 2f).coerceAtLeast(12f).sp
          }
        Text(
          text = primaryLabel,
          color = fgColor,
          fontSize = computedSize,
          fontWeight = if (isVowelMorphActive) FontWeight.Bold else FontWeight.Medium,
          fontFamily = if (isEthiopic) EthiopicFontFamily else DisplayFontFamily,
          maxLines = 1,
          modifier = Modifier.align(Alignment.Center),
        )
      }
    }

    if (popupEnabled && isPressed && !showVariantsMenu) {
      Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -110)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.accentKeySurface,
          shadowElevation = 8.dp,
        ) {
          Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = primaryLabel,
              color = palette.accentKeyText,
              fontSize = 26.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = if (isEthiopic) EthiopicFontFamily else DisplayFontFamily,
            )
          }
        }
      }
    }

    if (longPressVariants.isNotEmpty() && onSelectLongPressVariant != null) {
      DropdownMenu(
        expanded = showVariantsMenu,
        onDismissRequest = { showVariantsMenu = false },
        containerColor = palette.toolbarBackground,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          longPressVariants.forEach { variant ->
            Surface(
              onClick = {
                showVariantsMenu = false
                onSelectLongPressVariant(variant)
              },
              shape = RoundedCornerShape(8.dp),
              color = palette.keySurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.5f)),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = variant,
                  color = palette.primaryText,
                  fontSize = 18.sp,
                  fontFamily = EthiopicFontFamily,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun RowScope.NuhIconKeyButton(
  icon: ImageVector,
  contentDesc: String,
  palette: KeyboardPalette,
  keyHeight: Dp,
  weight: Float = 1.25f,
  isAccent: Boolean = false,
  isHighlighted: Boolean = false,
  hapticsEnabled: Boolean = true,
  testTag: String? = null,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val bg =
    when {
      isAccent -> palette.accentKeySurface
      isHighlighted -> palette.accentColor.copy(alpha = 0.25f)
      isPressed -> palette.keySurfacePressed
      else -> palette.modifierKeySurface
    }
  val tint =
    when {
      isAccent -> palette.accentKeyText
      isHighlighted -> palette.accentColor
      else -> palette.primaryText
    }

  Surface(
    onClick = {
      if (hapticsEnabled) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      onClick()
    },
    interactionSource = interactionSource,
    shape = RoundedCornerShape(10.dp),
    color = bg,
    border =
      androidx.compose.foundation.BorderStroke(
        width = if (isHighlighted) 1.dp else 0.75.dp,
        color = if (isHighlighted) palette.accentColor else palette.keyBorder,
      ),
    shadowElevation = if (isPressed) 0.dp else 2.dp,
    modifier =
      Modifier.weight(weight)
        .height(keyHeight)
        .semantics { contentDescription = contentDesc }
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDesc,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun RowScope.NuhSpaceBar(
  label: String,
  palette: KeyboardPalette,
  keyHeight: Dp,
  weight: Float = 4f,
  hapticsEnabled: Boolean = true,
  testTag: String = "key_space",
  onClick: () -> Unit,
  onSwipeCursor: (Int) -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  var dragAccumulated by remember { mutableFloatStateOf(0f) }

  Surface(
    onClick = {
      if (hapticsEnabled) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      onClick()
    },
    shape = RoundedCornerShape(10.dp),
    color = palette.keySurface,
    border = androidx.compose.foundation.BorderStroke(0.75.dp, palette.keyBorder),
    shadowElevation = 2.dp,
    modifier =
      Modifier.weight(weight)
        .height(keyHeight)
        .testTag(testTag)
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragEnd = { dragAccumulated = 0f },
            onDragCancel = { dragAccumulated = 0f },
            onHorizontalDrag = { _, dragAmount ->
              dragAccumulated += dragAmount
              val threshold = 26f
              if (dragAccumulated >= threshold) {
                onSwipeCursor(1)
                dragAccumulated = 0f
              } else if (dragAccumulated <= -threshold) {
                onSwipeCursor(-1)
                dragAccumulated = 0f
              }
            },
          )
        },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.SpaceBar,
        contentDescription = "Space",
        tint = palette.secondaryText.copy(alpha = 0.7f),
        modifier = Modifier.size(16.dp),
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = label,
        color = palette.secondaryText,
        fontSize = 12.sp,
        fontFamily = EthiopicFontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun CenterHingeSpineButton(
  label: String? = null,
  icon: ImageVector? = null,
  contentDesc: String,
  palette: KeyboardPalette,
  height: Dp,
  isHighlight: Boolean = false,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(9.dp),
    color = if (isHighlight) palette.accentColor.copy(alpha = 0.22f) else palette.toolbarBackground,
    border =
      androidx.compose.foundation.BorderStroke(
        width = 0.75.dp,
        color = if (isHighlight) palette.accentColor else palette.keyBorder,
      ),
    modifier = Modifier.fillMaxWidth().height(height),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = contentDesc,
          tint = if (isHighlight) palette.accentColor else palette.secondaryText,
          modifier = Modifier.size(17.dp),
        )
      } else if (label != null) {
        Text(
          text = label,
          color = if (isHighlight) palette.accentColor else palette.primaryText,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = EthiopicFontFamily,
        )
      }
    }
  }
}

@Composable
fun PhoneticHintForKey(letter: Char, isShifted: Boolean): String {
  val effective = if (isShifted) letter.uppercaseChar() else letter.lowercaseChar()
  return when (effective) {
    'h' -> "ሀ"
    'H' -> "ሐ"
    'l' -> "ለ"
    'm' -> "መ"
    's' -> "ሰ"
    'S' -> "ሠ"
    'r' -> "ረ"
    'q' -> "ቀ"
    'b' -> "በ"
    'v' -> "ቨ"
    't' -> "ተ"
    'T' -> "ጠ"
    'c' -> "ቸ"
    'C' -> "ጨ"
    'n' -> "ነ"
    'N' -> "ኘ"
    'a' -> "አ"
    'A' -> "ዐ"
    'k' -> "ከ"
    'K' -> "ኸ"
    'w' -> "ወ"
    'z' -> "ዘ"
    'Z' -> "ዠ"
    'y' -> "የ"
    'd' -> "ደ"
    'j' -> "ጀ"
    'g' -> "ገ"
    'P' -> "ጰ"
    'f' -> "ፈ"
    'p' -> "ፐ"
    'x' -> "ሸ"
    'u' -> "ኡ"
    'i' -> "ኢ"
    'e' -> "እ"
    'o' -> "ኦ"
    else -> ""
  }
}
