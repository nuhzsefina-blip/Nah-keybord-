package com.example.firebase

import android.content.Context
import com.example.R
import com.example.data.KeyboardSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NuhCloudRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context,
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireMatchingUserId(expectedUserId: String): String {
    val uid =
      auth.currentUser?.uid
        ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user Firestore operation." }
    return uid
  }

  fun observeUserSettings(userId: String): Flow<UserSettingsProfile?> =
    db.collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            UserSettingsProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }

  fun observeDictionary(userId: String): Flow<List<DictionaryWordEntry>> =
    db.collection("users")
      .document(userId)
      .collection("dictionary")
      .orderBy("frequency", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          DictionaryWordEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

  fun observeTranscriptions(userId: String): Flow<List<VoiceTranscriptionEntry>> =
    db.collection("users")
      .document(userId)
      .collection("transcriptions")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          VoiceTranscriptionEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

  suspend fun syncUserSettings(
    userId: String,
    displayName: String,
    settings: KeyboardSettings,
  ): Result<Unit> {
    val docPath = "users/$userId"
    return try {
      val uid = requireMatchingUserId(userId)
      val safeName = displayName.trim().take(100)
      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef
          .update(
            mapOf(
              "displayName" to safeName,
              "inputMode" to settings.inputMode.name,
              "foldLayoutPreference" to settings.foldLayoutPreference.name,
              "splitGapDp" to settings.splitGapDp.toDouble().coerceIn(40.0, 148.0),
              "keyHeightDp" to settings.keyHeightDp.toDouble().coerceIn(36.0, 68.0),
              "letterSizeSp" to settings.letterSizeSp.toDouble().coerceIn(14.0, 26.0),
              "themeStyle" to settings.themeStyle.name,
              "autoCorrectEnabled" to settings.autoCorrectEnabled,
              "nextFidelHintsEnabled" to settings.nextFidelHintsEnabled,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "displayName" to safeName,
              "inputMode" to settings.inputMode.name,
              "foldLayoutPreference" to settings.foldLayoutPreference.name,
              "splitGapDp" to settings.splitGapDp.toDouble().coerceIn(40.0, 148.0),
              "keyHeightDp" to settings.keyHeightDp.toDouble().coerceIn(36.0, 68.0),
              "letterSizeSp" to settings.letterSizeSp.toDouble().coerceIn(14.0, 26.0),
              "themeStyle" to settings.themeStyle.name,
              "autoCorrectEnabled" to settings.autoCorrectEnabled,
              "nextFidelHintsEnabled" to settings.nextFidelHintsEnabled,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addOrBumpDictionaryWord(
    userId: String,
    rawWord: String,
    rawNote: String? = null,
    existingEntries: List<DictionaryWordEntry> = emptyList(),
  ): Result<Unit> {
    val cleanWord = rawWord.trim()
    if (cleanWord.isEmpty() || cleanWord.length > 64) {
      return Result.failure(IllegalArgumentException("Word must be between 1 and 64 characters."))
    }
    val cleanNote = rawNote?.trim()?.takeIf { it.isNotEmpty() }?.take(120)
    val basePath = "users/$userId/dictionary"
    return try {
      val uid = requireMatchingUserId(userId)
      val existing = existingEntries.firstOrNull { it.word == cleanWord }
      if (existing != null && existing.id.isNotBlank()) {
        val docRef = db.collection("users").document(uid).collection("dictionary").document(existing.id)
        val nextFreq = (existing.frequency + 1L).coerceIn(1L, 1_000_000L)
        val updates =
          buildMap<String, Any> {
            put("frequency", nextFreq)
            put("updatedAt", FieldValue.serverTimestamp())
            if (cleanNote != null) {
              put("note", cleanNote)
            }
          }
        docRef.update(updates).await()
      } else {
        val docRef = db.collection("users").document(uid).collection("dictionary").document()
        val payload =
          buildMap<String, Any> {
            put("userId", uid)
            put("word", cleanWord)
            if (cleanNote != null) {
              put("note", cleanNote)
            }
            put("frequency", 1L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, basePath)
      Result.failure(e)
    }
  }

  suspend fun deleteDictionaryWord(userId: String, entryId: String): Result<Unit> {
    if (entryId.isBlank()) return Result.failure(IllegalArgumentException("Entry ID is blank."))
    val docPath = "users/$userId/dictionary/$entryId"
    return try {
      val uid = requireMatchingUserId(userId)
      db.collection("users").document(uid).collection("dictionary").document(entryId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveTranscription(
    userId: String,
    transcript: String,
    durationSeconds: Int,
    modelName: String = "gemini-3.5-transcribe",
  ): Result<Unit> {
    val cleanTranscript = transcript.trim().take(4000)
    if (cleanTranscript.isEmpty()) {
      return Result.failure(IllegalArgumentException("Transcript must not be empty."))
    }
    val safeDuration = durationSeconds.coerceIn(0, 600).toLong()
    val safeModel = modelName.trim().ifEmpty { "gemini-3.5-transcribe" }.take(80)
    val basePath = "users/$userId/transcriptions"
    return try {
      val uid = requireMatchingUserId(userId)
      val docRef = db.collection("users").document(uid).collection("transcriptions").document()
      docRef
        .set(
          mapOf(
            "userId" to uid,
            "transcript" to cleanTranscript,
            "modelName" to safeModel,
            "durationSeconds" to safeDuration,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, basePath)
      Result.failure(e)
    }
  }

  suspend fun deleteTranscription(userId: String, transcriptionId: String): Result<Unit> {
    if (transcriptionId.isBlank()) {
      return Result.failure(IllegalArgumentException("Transcription ID is blank."))
    }
    val docPath = "users/$userId/transcriptions/$transcriptionId"
    return try {
      val uid = requireMatchingUserId(userId)
      db.collection("users")
        .document(uid)
        .collection("transcriptions")
        .document(transcriptionId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  companion object {
    fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
  }
}
