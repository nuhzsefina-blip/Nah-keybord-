package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiAudioTranscriber
import com.example.ai.RecordingTelemetry
import com.example.firebase.UiState
import com.example.firebase.VoiceTranscriptionEntry
import com.example.ui.VoiceTranscriptionState
import com.example.ui.theme.AxumiteEmerald
import com.example.ui.theme.EthiopianGold
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.MonoFontFamily

@Composable
fun VoiceTranscribeScreen(
  voiceState: VoiceTranscriptionState,
  recordingTelemetry: RecordingTelemetry,
  transcriptionsState: UiState<List<VoiceTranscriptionEntry>>,
  onStartRecording: () -> Unit,
  onStopAndTranscribe: () -> Unit,
  onCancelRecording: () -> Unit,
  onInsertIntoSandbox: (String) -> Unit,
  onCopyTranscript: (String) -> Unit,
  onDeleteTranscription: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var permissionDeniedOnce by remember { mutableStateOf(false) }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        permissionDeniedOnce = false
        onStartRecording()
      } else {
        permissionDeniedOnce = true
      }
    }

  fun requestMicAndStart() {
    val hasPermission =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (hasPermission) {
      permissionDeniedOnce = false
      onStartRecording()
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  val animatedLevel by
    animateFloatAsState(
      targetValue =
        if (recordingTelemetry.isRecording) recordingTelemetry.normalizedAmplitude.coerceIn(0.08f, 1f)
        else 0.05f,
      label = "mic_level",
    )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Amharic & English Voice AI",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = EthiopianGold.copy(alpha = 0.16f),
            border = BorderStroke(0.75.dp, EthiopianGold.copy(alpha = 0.6f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = EthiopianGold,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = GeminiAudioTranscriber.MODEL_NAME,
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                color = EthiopianGold,
              )
            }
          }
        }
        Text(
          text =
            "Speak naturally in Amharic (አማርኛ) or English. Audio is captured locally and transcribed into Ethiopic script, then synced to your Cloud History.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (permissionDeniedOnce) {
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Microphone permission is needed to record Amharic & English speech.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OutlinedButton(onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
              Text("Allow Mic")
            }
          }
        }
      }
    }

    // Active Recorder Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border =
          BorderStroke(
            width = if (recordingTelemetry.isRecording) 1.5.dp else 0.75.dp,
            color =
              if (recordingTelemetry.isRecording) AxumiteEmerald
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Animated Audio Level Bars
          Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val weights = listOf(0.35f, 0.65f, 0.9f, 1.0f, 0.85f, 0.55f, 0.95f, 0.7f, 0.4f)
            weights.forEach { mult ->
              val barFraction = (animatedLevel * mult).coerceIn(0.14f, 1f)
              Box(
                modifier =
                  Modifier.width(7.dp)
                    .height((44 * barFraction).dp)
                    .clip(CircleShape)
                    .background(
                      if (recordingTelemetry.isRecording) AxumiteEmerald
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
              )
            }
          }

          Text(
            text =
              when (voiceState) {
                is VoiceTranscriptionState.Recording -> {
                  val secs = recordingTelemetry.elapsedSeconds
                  val mm = (secs / 60).toString().padStart(2, '0')
                  val ss = (secs % 60).toString().padStart(2, '0')
                  "Listening… $mm:$ss / 01:30"
                }
                is VoiceTranscriptionState.Transcribing -> "Transcribing with ${GeminiAudioTranscriber.MODEL_NAME}…"
                is VoiceTranscriptionState.Success -> "Latest Transcript Ready"
                is VoiceTranscriptionState.Error -> "Transcription Error"
                VoiceTranscriptionState.Idle -> "Ready to record Amharic or English"
              },
            style = MaterialTheme.typography.titleSmall,
            fontFamily = if (voiceState is VoiceTranscriptionState.Recording) MonoFontFamily else MaterialTheme.typography.titleSmall.fontFamily,
            color =
              when (voiceState) {
                is VoiceTranscriptionState.Recording -> AxumiteEmerald
                is VoiceTranscriptionState.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
              },
          )

          when (voiceState) {
            is VoiceTranscriptionState.Recording -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = onCancelRecording,
                  modifier = Modifier.weight(1f).height(50.dp).testTag("btn_voice_cancel"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Cancel")
                }
                Button(
                  onClick = onStopAndTranscribe,
                  modifier = Modifier.weight(1.4f).height(50.dp).testTag("btn_voice_stop"),
                  shape = RoundedCornerShape(14.dp),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = AxumiteEmerald,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                  Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Stop & Transcribe")
                }
              }
            }
            is VoiceTranscriptionState.Transcribing -> {
              Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Text(
                  text = "Processing WAV audio clip…",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            else -> {
              Button(
                onClick = { requestMicAndStart() },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_voice_record"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Record Amharic / English Audio")
              }
            }
          }

          if (voiceState is VoiceTranscriptionState.Success) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth().testTag("latest_transcript_card"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = voiceState.transcript,
                  fontFamily = EthiopicFontFamily,
                  fontSize = 17.sp,
                  lineHeight = 25.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilledTonalButton(
                    onClick = { onInsertIntoSandbox(voiceState.transcript) },
                    modifier = Modifier.weight(1f).testTag("btn_insert_transcript"),
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.NoteAdd,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Send to Sandbox")
                  }
                  OutlinedButton(
                    onClick = { onCopyTranscript(voiceState.transcript) },
                    modifier = Modifier.weight(1f).testTag("btn_copy_transcript"),
                  ) {
                    Icon(
                      Icons.Filled.ContentCopy,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                  }
                }
              }
            }
          }

          if (voiceState is VoiceTranscriptionState.Error) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().testTag("voice_error_card"),
            ) {
              Text(
                text = voiceState.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      }
    }

    // Cloud History Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Saved Cloud Transcriptions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        if (transcriptionsState is UiState.Success) {
          Text(
            text = "${transcriptionsState.data.size} saved",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    when (transcriptionsState) {
      UiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        }
      }
      is UiState.Error -> {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Unable to load transcription history: ${transcriptionsState.message}",
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        val entries = transcriptionsState.data
        if (entries.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
              Text(
                text = "No saved voice transcripts yet. Tap the microphone above to transcribe Amharic or English speech.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(entries, key = { it.id }) { entry ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = entry.transcript,
                  fontFamily = EthiopicFontFamily,
                  fontSize = 15.sp,
                  lineHeight = 22.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${entry.modelName} · ${entry.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                      onClick = { onInsertIntoSandbox(entry.transcript) },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "Insert transcript into sandbox",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    IconButton(
                      onClick = { onCopyTranscript(entry.transcript) },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy transcript",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    IconButton(
                      onClick = { onDeleteTranscription(entry.id) },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete transcription",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
