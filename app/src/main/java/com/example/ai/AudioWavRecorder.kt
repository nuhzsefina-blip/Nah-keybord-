package com.example.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordingTelemetry(
  val isRecording: Boolean = false,
  val elapsedSeconds: Int = 0,
  val normalizedAmplitude: Float = 0f,
)

data class RecordedAudioClip(
  val wavBytes: ByteArray,
  val durationSeconds: Int,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RecordedAudioClip) return false
    return durationSeconds == other.durationSeconds && wavBytes.contentEquals(other.wavBytes)
  }

  override fun hashCode(): Int = 31 * wavBytes.contentHashCode() + durationSeconds
}

class AudioWavRecorder {

  private val sampleRateHz = 16_000
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val maxRecordingSeconds = 90 // ~2.88 MB WAV, safely below 20 MB limit

  private val _telemetry = MutableStateFlow(RecordingTelemetry())
  val telemetry: StateFlow<RecordingTelemetry> = _telemetry.asStateFlow()

  private var audioRecord: AudioRecord? = null
  private var captureJob: Job? = null
  private var pcmBuffer = ByteArrayOutputStream()
  private var startTimeMs = 0L

  @SuppressLint("MissingPermission")
  fun startRecording(scope: CoroutineScope, onMaxDurationReached: () -> Unit): Result<Unit> {
    if (_telemetry.value.isRecording) return Result.success(Unit)

    val minBufSize = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)
    val bufferSize = (minBufSize.coerceAtLeast(4096)) * 2
    val recorder =
      try {
        AudioRecord(
          MediaRecorder.AudioSource.MIC,
          sampleRateHz,
          channelConfig,
          audioFormat,
          bufferSize,
        )
      } catch (e: SecurityException) {
        return Result.failure(e)
      } catch (e: Exception) {
        return Result.failure(e)
      }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      return Result.failure(IllegalStateException("Microphone AudioRecord failed to initialize."))
    }

    pcmBuffer = ByteArrayOutputStream()
    startTimeMs = System.currentTimeMillis()
    audioRecord = recorder

    return try {
      recorder.startRecording()
      _telemetry.value = RecordingTelemetry(isRecording = true, elapsedSeconds = 0, normalizedAmplitude = 0f)
      captureJob =
        scope.launch(Dispatchers.IO) {
          val readChunk = ByteArray(2048)
          while (isActive && _telemetry.value.isRecording) {
            val read = recorder.read(readChunk, 0, readChunk.size)
            if (read > 0) {
              synchronized(pcmBuffer) { pcmBuffer.write(readChunk, 0, read) }
              val amp = computeNormalizedRms(readChunk, read)
              val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
              _telemetry.value =
                RecordingTelemetry(
                  isRecording = true,
                  elapsedSeconds = elapsed,
                  normalizedAmplitude = amp,
                )
              if (elapsed >= maxRecordingSeconds) {
                onMaxDurationReached()
                break
              }
            }
          }
        }
      Result.success(Unit)
    } catch (e: Exception) {
      cleanupRecorder()
      Result.failure(e)
    }
  }

  fun stopRecording(): RecordedAudioClip {
    val elapsed =
      ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
    _telemetry.value = RecordingTelemetry(isRecording = false, elapsedSeconds = 0, normalizedAmplitude = 0f)
    captureJob?.cancel()
    captureJob = null
    cleanupRecorder()

    val rawPcm = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
    val wavBytes = wrapPcmAsWav(rawPcm, sampleRateHz, channels = 1, bitsPerSample = 16)
    return RecordedAudioClip(wavBytes = wavBytes, durationSeconds = elapsed)
  }

  fun cancelRecording() {
    _telemetry.value = RecordingTelemetry(isRecording = false, elapsedSeconds = 0, normalizedAmplitude = 0f)
    captureJob?.cancel()
    captureJob = null
    cleanupRecorder()
    synchronized(pcmBuffer) { pcmBuffer.reset() }
  }

  private fun cleanupRecorder() {
    try {
      audioRecord?.stop()
    } catch (_: Exception) {
    }
    try {
      audioRecord?.release()
    } catch (_: Exception) {
    }
    audioRecord = null
  }

  private fun computeNormalizedRms(bytes: ByteArray, length: Int): Float {
    if (length < 2) return 0f
    var sumSquares = 0.0
    var sampleCount = 0
    var i = 0
    while (i + 1 < length) {
      val low = bytes[i].toInt() and 0xFF
      val high = bytes[i + 1].toInt()
      val sample = (high shl 8) or low
      sumSquares += sample.toDouble() * sample.toDouble()
      sampleCount++
      i += 2
    }
    if (sampleCount == 0) return 0f
    val rms = sqrt(sumSquares / sampleCount)
    return (rms / 8000.0).toFloat().coerceIn(0f, 1f)
  }

  private fun wrapPcmAsWav(
    pcmData: ByteArray,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int,
  ): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val dataSize = pcmData.size
    val chunkSize = 36 + dataSize

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(chunkSize)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16) // PCM subchunk size
    header.putShort(1.toShort()) // PCM audio format
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(dataSize)

    return header.array() + pcmData
  }
}
