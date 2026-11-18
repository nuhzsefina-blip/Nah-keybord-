package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.GeminiAudioTranscriber
import com.example.ai.RecordingTelemetry
import com.example.data.KeyboardSettings
import com.example.foldable.FoldableUiState
import com.example.foldable.ZFoldPosture
import com.example.ime.KeyboardSessionController
import com.example.ui.ImeActivationStatus
import com.example.ui.SandboxEditorState
import com.example.ui.SandboxPane
import com.example.ui.VoiceTranscriptionState
import com.example.ui.keyboard.NuhKeyboardPanel
import com.example.ui.theme.AxumiteEmerald
import com.example.ui.theme.EthiopianGold
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.MonoFontFamily

@Composable
fun FoldStudioScreen(
  settings: KeyboardSettings,
  foldableState: FoldableUiState,
  imeStatus: ImeActivationStatus,
  sandboxState: SandboxEditorState,
  sandboxController: KeyboardSessionController,
  voiceState: VoiceTranscriptionState,
  recordingTelemetry: RecordingTelemetry,
  onStartVoiceDictation: () -> Unit,
  onStopVoiceDictation: () -> Unit,
  onOpenVoiceTab: () -> Unit,
  onEnableImeClick: () -> Unit,
  onSelectImeClick: () -> Unit,
  onSelectSandboxPane: (SandboxPane) -> Unit,
  onClearSandboxPane: () -> Unit,
  onSetPreviewPosture: (ZFoldPosture?) -> Unit,
  onSelectInputMode: (com.example.data.AmharicInputMode) -> Unit,
  onToggleInputMode: () -> Unit,
  onCyclePosture: () -> Unit,
  onToggleOneHanded: () -> Unit,
  onNavigateToErgonomics: () -> Unit,
  onCopyTextToClipboard: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) onStartVoiceDictation()
    }
  val effectivePosture = sandboxState.previewPostureOverride ?: foldableState.activePosture

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Banner Card with Z Fold 7 Artwork & Live Posture Badge
    Card(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
      Box(modifier = Modifier.fillMaxWidth().height(172.dp)) {
        Image(
          painter = painterResource(id = R.drawable.img_foldable_hero),
          contentDescription = "Samsung Z Fold 7 ergonomic Amharic keyboard visual",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color.Black.copy(alpha = 0.25f),
                    Color.Black.copy(alpha = 0.85f),
                  )
                )
              )
        )
        Row(
          modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = CircleShape,
            color = EthiopianGold.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, EthiopianGold.copy(alpha = 0.7f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.DevicesFold,
                contentDescription = null,
                tint = EthiopianGold,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Detected: ${foldableState.detectedPosture.badge}",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = MonoFontFamily,
              )
            }
          }
        }
        Column(
          modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(id = R.string.hero_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = EthiopicFontFamily,
            color = Color.White,
          )
          Text(
            text = stringResource(id = R.string.hero_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD1D5DB),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // 2-Step System Keyboard Setup Row
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector =
                if (imeStatus.isDefaultSelected) Icons.Filled.CheckCircle
                else Icons.Filled.Keyboard,
              contentDescription = null,
              tint = if (imeStatus.isDefaultSelected) AxumiteEmerald else EthiopianGold,
              modifier = Modifier.size(20.dp),
            )
            Column {
              Text(
                text =
                  when {
                    imeStatus.isDefaultSelected -> stringResource(R.string.status_active)
                    imeStatus.isEnabled -> stringResource(R.string.status_enabled)
                    else -> stringResource(R.string.status_not_enabled)
                  },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (imeStatus.isDefaultSelected)
                    "Ready across WhatsApp, Messages, Docs & Split-Screen apps"
                  else
                    "Step 1: Enable in Android Settings · Step 2: Switch input method",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onEnableImeClick,
            modifier = Modifier.weight(1f).testTag("btn_enable_ime"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (imeStatus.isEnabled) MaterialTheme.colorScheme.secondaryContainer
                  else MaterialTheme.colorScheme.primary,
                contentColor =
                  if (imeStatus.isEnabled) MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.onPrimary,
              ),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.SettingsInputComponent,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = if (imeStatus.isEnabled) "1. Enabled ✓" else "1. Enable ኑህ",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onSelectImeClick,
            modifier = Modifier.weight(1f).testTag("btn_select_ime"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          ) {
            Icon(
              imageVector = Icons.Filled.Keyboard,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = if (imeStatus.isDefaultSelected) "2. Active ✓" else "2. Select ኑህ",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Z Fold 7 Posture Preview Selector + Multitasking Sandbox
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.sandbox_header),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = effectivePosture.badge,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = sandboxState.previewPostureOverride == null,
          onClick = { onSetPreviewPosture(null) },
          label = { Text("Auto (${foldableState.detectedPosture.badge})") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        ZFoldPosture.entries.forEach { posture ->
          FilterChip(
            selected = sandboxState.previewPostureOverride == posture,
            onClick = { onSetPreviewPosture(posture) },
            label = { Text(posture.title) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          )
        }
      }

      // Quick Voice Dictation into Focused Sandbox Pane
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text =
                when (voiceState) {
                  is VoiceTranscriptionState.Recording ->
                    "Recording audio (${recordingTelemetry.elapsedSeconds}s)… Speak Amharic or EN"
                  is VoiceTranscriptionState.Transcribing ->
                    "Transcribing with ${GeminiAudioTranscriber.MODEL_NAME}…"
                  is VoiceTranscriptionState.Error -> voiceState.message
                  else -> "Voice Dictate into active pane"
                },
              style = MaterialTheme.typography.labelMedium,
              color =
                when (voiceState) {
                  is VoiceTranscriptionState.Recording -> AxumiteEmerald
                  is VoiceTranscriptionState.Error -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.onSurface
                },
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = GeminiAudioTranscriber.MODEL_NAME,
              fontFamily = MonoFontFamily,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (voiceState is VoiceTranscriptionState.Recording) {
              Button(
                onClick = onStopVoiceDictation,
                colors = ButtonDefaults.buttonColors(containerColor = AxumiteEmerald),
                modifier = Modifier.testTag("studio_voice_stop"),
              ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Insert")
              }
            } else if (voiceState is VoiceTranscriptionState.Transcribing) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
              FilledTonalButton(
                onClick = {
                  val granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                      PackageManager.PERMISSION_GRANTED
                  if (granted) onStartVoiceDictation()
                  else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.testTag("studio_voice_mic"),
              ) {
                Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Dictate")
              }
              OutlinedButton(onClick = onOpenVoiceTab) {
                Text("History")
              }
            }
          }
        }
      }

      // Live Side-by-Side Split-Screen Simulator OR Single Focused Editor Pane
      if (effectivePosture == ZFoldPosture.MULTI_WINDOW_PANE ||
        effectivePosture == ZFoldPosture.UNFOLDED_SPLIT
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          SandboxAppPaneCard(
            title = "💬 Chat App",
            icon = Icons.AutoMirrored.Filled.Send,
            text = sandboxState.messageText,
            composing =
              if (sandboxState.activePane == SandboxPane.MESSAGE_PANE) sandboxState.composingText
              else "",
            isFocused = sandboxState.activePane == SandboxPane.MESSAGE_PANE,
            onFocus = { onSelectSandboxPane(SandboxPane.MESSAGE_PANE) },
            onCopy = { onCopyTextToClipboard(sandboxState.messageText) },
            onClear = onClearSandboxPane,
            modifier = Modifier.weight(1f).testTag("sandbox_message_pane"),
          )
          SandboxAppPaneCard(
            title = "📝 Notes Split",
            icon = Icons.AutoMirrored.Filled.Notes,
            text = sandboxState.notesText,
            composing =
              if (sandboxState.activePane == SandboxPane.NOTES_PANE) sandboxState.composingText
              else "",
            isFocused = sandboxState.activePane == SandboxPane.NOTES_PANE,
            onFocus = { onSelectSandboxPane(SandboxPane.NOTES_PANE) },
            onCopy = { onCopyTextToClipboard(sandboxState.notesText) },
            onClear = onClearSandboxPane,
            modifier = Modifier.weight(1f).testTag("sandbox_notes_pane"),
          )
        }
      } else {
        SandboxAppPaneCard(
          title =
            if (effectivePosture == ZFoldPosture.FLEX_TABLETOP) "Flex Upper Display · Scratchpad"
            else "Live Amharic Test Pad",
          icon = Icons.Filled.Splitscreen,
          text = sandboxState.messageText,
          composing = sandboxState.composingText,
          isFocused = true,
          onFocus = { onSelectSandboxPane(SandboxPane.MESSAGE_PANE) },
          onCopy = { onCopyTextToClipboard(sandboxState.messageText) },
          onClear = onClearSandboxPane,
          modifier = Modifier.fillMaxWidth().testTag("sandbox_single_pane"),
        )
      }
    }

    // Embedded Live Interactive Keyboard Surface!
    Surface(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
      tonalElevation = 4.dp,
    ) {
      NuhKeyboardPanel(
        settings = settings,
        posture = effectivePosture,
        controller = sandboxController,
        onSelectInputMode = onSelectInputMode,
        onToggleInputMode = onToggleInputMode,
        onCyclePosture = onCyclePosture,
        onToggleOneHanded = onToggleOneHanded,
        onOpenSettings = onNavigateToErgonomics,
      )
    }
  }
}

@Composable
private fun SandboxAppPaneCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  composing: String,
  isFocused: Boolean,
  onFocus: () -> Unit,
  onCopy: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onFocus),
    shape = RoundedCornerShape(14.dp),
    color =
      if (isFocused) MaterialTheme.colorScheme.surfaceContainerHigh
      else MaterialTheme.colorScheme.surfaceContainerLow,
    border =
      BorderStroke(
        width = if (isFocused) 1.5.dp else 0.75.dp,
        color =
          if (isFocused) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
              if (isFocused) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color =
              if (isFocused) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
            Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = "Copy pane text",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
          }
          if (isFocused) {
            IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
              Icon(
                imageVector = Icons.Filled.ClearAll,
                contentDescription = "Clear pane",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }

      val annotated = buildAnnotatedString {
        append(text)
        if (composing.isNotEmpty()) {
          withStyle(
            SpanStyle(
              color = EthiopianGold,
              fontWeight = FontWeight.Bold,
              textDecoration = TextDecoration.Underline,
            )
          ) {
            append(composing)
          }
        }
        if (isFocused) {
          withStyle(SpanStyle(color = AxumiteEmerald, fontWeight = FontWeight.Bold)) {
            append("▎")
          }
        }
      }

      Text(
        text = annotated,
        fontFamily = EthiopicFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface,
        minLines = 3,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
