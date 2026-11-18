package com.example.foldable

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.data.FoldLayoutPreference

enum class ZFoldPosture(
  val title: String,
  val badge: String,
  val description: String,
) {
  COVER_SINGLE(
    title = "Cover Screen",
    badge = "Cover 21:9",
    description = "Compact outer-display layout tuned for fast single-hand or dual-thumb reach.",
  ),
  UNFOLDED_SPLIT(
    title = "Unfolded Split",
    badge = "Z Fold Inner Split",
    description = "Ergonomic left/right thumb clusters separated across the center hinge with multitasking spine.",
  ),
  MULTI_WINDOW_PANE(
    title = "Split-Screen Multitask",
    badge = "Multi-Window",
    description = "Space-saving narrow-pane layout with instant Quick-Paste & Clipboard dock for side-by-side apps.",
  ),
  FLEX_TABLETOP(
    title = "Flex Tabletop",
    badge = "Flex Half-Fold",
    description = "Half-folded posture splitting preview/scratchpad above the hinge and keys below.",
  ),
}

data class FoldableUiState(
  val detectedPosture: ZFoldPosture,
  val activePosture: ZFoldPosture,
  val isHalfOpened: Boolean = false,
  val hasVerticalHinge: Boolean = false,
  val hasHorizontalHinge: Boolean = false,
  val isMultiWindowLikely: Boolean = false,
)

@Composable
fun rememberWindowLayoutInfo(): WindowLayoutInfo? {
  val context = LocalContext.current
  val activity = context as? Activity
  val layoutInfo by
    produceState<WindowLayoutInfo?>(initialValue = null, key1 = activity) {
      if (activity != null) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { value = it }
      }
    }
  return layoutInfo
}

fun resolveZFoldPosture(
  availableWidthDp: Dp,
  availableHeightDp: Dp,
  windowLayoutInfo: WindowLayoutInfo?,
  preference: FoldLayoutPreference,
  isInMultiWindowMode: Boolean = false,
): FoldableUiState {
  val foldingFeature =
    windowLayoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()

  val isHalfOpened = foldingFeature?.state == FoldingFeature.State.HALF_OPENED
  val isHorizontalHinge =
    foldingFeature?.orientation == FoldingFeature.Orientation.HORIZONTAL
  val isVerticalHinge = foldingFeature?.orientation == FoldingFeature.Orientation.VERTICAL

  val detected =
    when {
      isHalfOpened && isHorizontalHinge -> ZFoldPosture.FLEX_TABLETOP
      isInMultiWindowMode && availableWidthDp < 540.dp -> ZFoldPosture.MULTI_WINDOW_PANE
      availableWidthDp >= 560.dp || (isVerticalHinge && availableWidthDp >= 500.dp) ->
        ZFoldPosture.UNFOLDED_SPLIT
      availableHeightDp < 440.dp && availableWidthDp < 540.dp -> ZFoldPosture.MULTI_WINDOW_PANE
      else -> ZFoldPosture.COVER_SINGLE
    }

  val active =
    when (preference) {
      FoldLayoutPreference.AUTO -> detected
      FoldLayoutPreference.UNFOLDED_SPLIT -> ZFoldPosture.UNFOLDED_SPLIT
      FoldLayoutPreference.MULTI_WINDOW_DOCK -> ZFoldPosture.MULTI_WINDOW_PANE
      FoldLayoutPreference.COVER_COMPACT -> ZFoldPosture.COVER_SINGLE
      FoldLayoutPreference.FLEX_TABLETOP -> ZFoldPosture.FLEX_TABLETOP
    }

  return FoldableUiState(
    detectedPosture = detected,
    activePosture = active,
    isHalfOpened = isHalfOpened,
    hasVerticalHinge = isVerticalHinge,
    hasHorizontalHinge = isHorizontalHinge,
    isMultiWindowLikely = isInMultiWindowMode,
  )
}
