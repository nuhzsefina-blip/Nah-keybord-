package com.example.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserSettingsProfile(
  val userId: String = "",
  val displayName: String = "",
  val inputMode: String = "FIDEL_INPLACE",
  val foldLayoutPreference: String = "AUTO",
  val splitGapDp: Double = 86.0,
  val keyHeightDp: Double = 48.0,
  val letterSizeSp: Double = 19.0,
  val themeStyle: String = "OBSIDIAN_GOLD",
  val autoCorrectEnabled: Boolean = true,
  val nextFidelHintsEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DictionaryWordEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val word: String = "",
  val note: String? = null,
  val frequency: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class VoiceTranscriptionEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val transcript: String = "",
  val modelName: String = "gemini-3.5-transcribe",
  val durationSeconds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
