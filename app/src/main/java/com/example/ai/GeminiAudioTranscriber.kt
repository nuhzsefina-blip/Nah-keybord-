package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class GeminiAudioTranscriber {

  private val model by lazy {
    Firebase.ai.generativeModel(modelName = MODEL_NAME)
  }

  suspend fun transcribeAudio(wavBytes: ByteArray): Result<String> {
    if (wavBytes.size <= 44) {
      return Result.failure(IllegalArgumentException("No audio was captured. Hold and speak closer to the mic."))
    }
    return try {
      val response =
        withTimeout(TRANSCRIPTION_TIMEOUT_MS) {
          model.generateContent(
            content {
              inlineData(wavBytes, "audio/wav")
              text(
                "Transcribe the spoken audio verbatim. " +
                  "If the speaker speaks Amharic, output accurate Ethiopic (Ge'ez) script. " +
                  "If English or mixed Amharic and English, transcribe faithfully with punctuation and no extra commentary."
              )
            }
          )
        }
      val text = response.text?.trim()
      if (!text.isNullOrEmpty()) {
        Result.success(text)
      } else {
        Result.failure(IllegalStateException("gemini-3.5-transcribe returned an empty transcript."))
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Audio transcription timed out", e)
      Result.failure(
        IllegalStateException("Transcription timed out after 60 seconds. Please try a shorter clip.", e)
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI audio transcription failed", e)
      Result.failure(e)
    }
  }

  companion object {
    const val MODEL_NAME = "gemini-3.5-transcribe"
    private const val TAG = "GeminiAudioTranscriber"
    private const val TRANSCRIPTION_TIMEOUT_MS = 60_000L
  }
}
