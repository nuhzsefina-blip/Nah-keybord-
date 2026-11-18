package com.example.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AudioWavRecorder
import com.example.ai.GeminiAudioTranscriber
import com.example.ai.RecordingTelemetry
import com.example.data.AmharicInputMode
import com.example.data.FoldLayoutPreference
import com.example.data.KeyboardPreferencesRepository
import com.example.data.KeyboardSettings
import com.example.data.OneHandedSide
import com.example.firebase.DictionaryWordEntry
import com.example.firebase.NuhCloudRepository
import com.example.firebase.OperationType
import com.example.firebase.UiState
import com.example.firebase.UserSettingsProfile
import com.example.firebase.VoiceTranscriptionEntry
import com.example.firebase.handleFirestoreError
import com.example.foldable.ZFoldPosture
import com.example.ime.KeyboardOutputTarget
import com.example.ime.KeyboardSessionController
import com.example.ime.NuhAmharicImeService
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SandboxPane {
  MESSAGE_PANE,
  NOTES_PANE,
}

data class ImeActivationStatus(
  val isEnabled: Boolean = false,
  val isDefaultSelected: Boolean = false,
)

data class SandboxEditorState(
  val activePane: SandboxPane = SandboxPane.MESSAGE_PANE,
  val messageText: String = "ሰላም! ",
  val notesText: String = "Z Fold 7 Split-Screen:\n• ኑህ አማርኛ ኪቦርድ",
  val composingText: String = "",
  val previewPostureOverride: ZFoldPosture? = null,
)

sealed interface VoiceTranscriptionState {
  data object Idle : VoiceTranscriptionState

  data object Recording : VoiceTranscriptionState

  data object Transcribing : VoiceTranscriptionState

  data class Success(val transcript: String, val durationSeconds: Int) : VoiceTranscriptionState

  data class Error(val message: String) : VoiceTranscriptionState
}

class NuhStudioViewModel(
  private val appContext: Context,
  private val repository: KeyboardPreferencesRepository,
  private val cloudRepository: NuhCloudRepository,
  private val transcriber: GeminiAudioTranscriber,
  private val recorder: AudioWavRecorder,
  val currentUserId: String,
  val currentUserDisplayName: String,
) : ViewModel() {

  val settings: StateFlow<KeyboardSettings> =
    repository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = KeyboardSettings(),
    )

  val remoteSettingsState: StateFlow<UiState<UserSettingsProfile?>> =
    cloudRepository
      .observeUserSettings(currentUserId)
      .map<UserSettingsProfile?, UiState<UserSettingsProfile?>> { UiState.Success(it) }
      .catch { e ->
        if (e is CancellationException) throw e
        val msg = handleFirestoreError(e, OperationType.GET, "users/$currentUserId")
        emit(UiState.Error(msg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading,
      )

  val dictionaryEntriesState: StateFlow<UiState<List<DictionaryWordEntry>>> =
    cloudRepository
      .observeDictionary(currentUserId)
      .map<List<DictionaryWordEntry>, UiState<List<DictionaryWordEntry>>> { UiState.Success(it) }
      .catch { e ->
        if (e is CancellationException) throw e
        val msg = handleFirestoreError(e, OperationType.LIST, "users/$currentUserId/dictionary")
        emit(UiState.Error(msg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading,
      )

  val transcriptionsState: StateFlow<UiState<List<VoiceTranscriptionEntry>>> =
    cloudRepository
      .observeTranscriptions(currentUserId)
      .map<List<VoiceTranscriptionEntry>, UiState<List<VoiceTranscriptionEntry>>> {
        UiState.Success(it)
      }
      .catch { e ->
        if (e is CancellationException) throw e
        val msg =
          handleFirestoreError(e, OperationType.LIST, "users/$currentUserId/transcriptions")
        emit(UiState.Error(msg))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading,
      )

  val recordingTelemetry: StateFlow<RecordingTelemetry> = recorder.telemetry

  private val _voiceState = MutableStateFlow<VoiceTranscriptionState>(VoiceTranscriptionState.Idle)
  val voiceState: StateFlow<VoiceTranscriptionState> = _voiceState.asStateFlow()

  private val _cloudActionError = MutableStateFlow<String?>(null)
  val cloudActionError: StateFlow<String?> = _cloudActionError.asStateFlow()

  private val _isSyncingCloud = MutableStateFlow(false)
  val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

  private val _imeStatus = MutableStateFlow(ImeActivationStatus())
  val imeStatus: StateFlow<ImeActivationStatus> = _imeStatus.asStateFlow()

  private val _sandboxState = MutableStateFlow(SandboxEditorState())
  val sandboxState: StateFlow<SandboxEditorState> = _sandboxState.asStateFlow()

  private var hasSyncedInitialRemoteSettings = false

  private val sandboxOutputTarget =
    object : KeyboardOutputTarget {
      override fun setComposingText(text: String) {
        _sandboxState.update { it.copy(composingText = text) }
      }

      override fun commitText(text: String) {
        _sandboxState.update { state ->
          when (state.activePane) {
            SandboxPane.MESSAGE_PANE ->
              state.copy(messageText = state.messageText + text, composingText = "")
            SandboxPane.NOTES_PANE ->
              state.copy(notesText = state.notesText + text, composingText = "")
          }
        }
      }

      override fun finishComposing() {
        val currentComposing = _sandboxState.value.composingText
        if (currentComposing.isNotEmpty()) {
          commitText(currentComposing)
        }
      }

      override fun deleteBackward(count: Int) {
        _sandboxState.update { state ->
          if (state.composingText.isNotEmpty()) {
            state.copy(composingText = "")
          } else {
            when (state.activePane) {
              SandboxPane.MESSAGE_PANE ->
                state.copy(messageText = state.messageText.dropLast(count))
              SandboxPane.NOTES_PANE ->
                state.copy(notesText = state.notesText.dropLast(count))
            }
          }
        }
      }

      override fun replaceLastCharacter(replacement: String) {
        _sandboxState.update { state ->
          when (state.activePane) {
            SandboxPane.MESSAGE_PANE ->
              state.copy(
                messageText = state.messageText.dropLast(1) + replacement,
                composingText = "",
              )
            SandboxPane.NOTES_PANE ->
              state.copy(notesText = state.notesText.dropLast(1) + replacement, composingText = "")
          }
        }
      }

      override fun performEnter() {
        commitText("\n")
      }

      override fun moveCursor(delta: Int) {}

      override fun getTextBeforeCursor(maxChars: Int): String {
        val state = _sandboxState.value
        val full =
          when (state.activePane) {
            SandboxPane.MESSAGE_PANE -> state.messageText
            SandboxPane.NOTES_PANE -> state.notesText
          }
        return full.takeLast(maxChars)
      }
    }

  val sandboxController = KeyboardSessionController(outputTarget = sandboxOutputTarget)

  init {
    refreshImeStatus()
    observeAndApplyCloudSync()
  }

  private fun observeAndApplyCloudSync() {
    viewModelScope.launch {
      dictionaryEntriesState.collectLatest { state ->
        if (state is UiState.Success) {
          val words = state.data.map { it.word }.toSet()
          if (words != settings.value.customDictionaryWords) {
            repository.setCustomDictionaryWords(words)
          }
        }
      }
    }

    viewModelScope.launch {
      remoteSettingsState.collectLatest { state ->
        if (state is UiState.Success && !hasSyncedInitialRemoteSettings) {
          hasSyncedInitialRemoteSettings = true
          val profile = state.data
          if (profile != null) {
            val mode =
              if (profile.inputMode == AmharicInputMode.ENGLISH_QWERTY.name) {
                AmharicInputMode.ENGLISH_QWERTY
              } else {
                AmharicInputMode.FIDEL_INPLACE
              }
            val foldPref =
              runCatching { FoldLayoutPreference.valueOf(profile.foldLayoutPreference) }
                .getOrDefault(FoldLayoutPreference.AUTO)
            val theme =
              runCatching { KeyboardThemeStyle.valueOf(profile.themeStyle) }
                .getOrDefault(KeyboardThemeStyle.OBSIDIAN_GOLD)
            repository.applyRemoteCloudSettings(
              inputMode = mode,
              foldLayoutPreference = foldPref,
              splitGapDp = profile.splitGapDp.toFloat(),
              keyHeightDp = profile.keyHeightDp.toFloat(),
              letterSizeSp = profile.letterSizeSp.toFloat(),
              themeStyle = theme,
              autoCorrectEnabled = profile.autoCorrectEnabled,
              nextFidelHintsEnabled = profile.nextFidelHintsEnabled,
            )
          } else {
            syncSettingsToCloud()
          }
        }
      }
    }
  }

  fun dismissCloudError() {
    _cloudActionError.value = null
  }

  fun syncSettingsToCloud() {
    viewModelScope.launch {
      _isSyncingCloud.value = true
      val res =
        cloudRepository.syncUserSettings(
          userId = currentUserId,
          displayName = currentUserDisplayName,
          settings = settings.value,
        )
      res.onFailure { err -> _cloudActionError.value = err.localizedMessage ?: "Cloud sync failed" }
      _isSyncingCloud.value = false
    }
  }

  fun addDictionaryWord(rawWord: String, rawNote: String? = null) {
    val currentEntries =
      (dictionaryEntriesState.value as? UiState.Success)?.data ?: emptyList()
    viewModelScope.launch {
      val res =
        cloudRepository.addOrBumpDictionaryWord(
          userId = currentUserId,
          rawWord = rawWord,
          rawNote = rawNote,
          existingEntries = currentEntries,
        )
      res.onFailure { err ->
        _cloudActionError.value = err.localizedMessage ?: "Failed to save dictionary word"
      }
    }
  }

  fun deleteDictionaryWord(entryId: String) {
    viewModelScope.launch {
      val res = cloudRepository.deleteDictionaryWord(currentUserId, entryId)
      res.onFailure { err ->
        _cloudActionError.value = err.localizedMessage ?: "Failed to delete dictionary word"
      }
    }
  }

  fun startVoiceRecording(autoStopInsertIntoSandbox: Boolean = false) {
    val startRes =
      recorder.startRecording(viewModelScope) {
        stopAndTranscribeAudio(insertIntoSandbox = autoStopInsertIntoSandbox)
      }
    startRes
      .onSuccess { _voiceState.value = VoiceTranscriptionState.Recording }
      .onFailure { err ->
        _voiceState.value =
          VoiceTranscriptionState.Error(
            err.localizedMessage ?: "Could not open device microphone."
          )
      }
  }

  fun stopAndTranscribeAudio(insertIntoSandbox: Boolean = false) {
    if (_voiceState.value !is VoiceTranscriptionState.Recording) return
    val clip = recorder.stopRecording()
    _voiceState.value = VoiceTranscriptionState.Transcribing

    viewModelScope.launch {
      val res = transcriber.transcribeAudio(clip.wavBytes)
      res
        .onSuccess { transcript ->
          _voiceState.value =
            VoiceTranscriptionState.Success(
              transcript = transcript,
              durationSeconds = clip.durationSeconds,
            )
          if (insertIntoSandbox) {
            insertTextIntoSandbox(transcript)
          }
          cloudRepository
            .saveTranscription(
              userId = currentUserId,
              transcript = transcript,
              durationSeconds = clip.durationSeconds,
              modelName = GeminiAudioTranscriber.MODEL_NAME,
            )
            .onFailure { err ->
              _cloudActionError.value =
                err.localizedMessage ?: "Transcribed, but failed to save to Cloud History."
            }
        }
        .onFailure { err ->
          _voiceState.value =
            VoiceTranscriptionState.Error(
              err.localizedMessage ?: "Audio transcription failed."
            )
        }
    }
  }

  fun cancelVoiceRecording() {
    recorder.cancelRecording()
    _voiceState.value = VoiceTranscriptionState.Idle
  }

  fun deleteTranscription(transcriptionId: String) {
    viewModelScope.launch {
      val res = cloudRepository.deleteTranscription(currentUserId, transcriptionId)
      res.onFailure { err ->
        _cloudActionError.value = err.localizedMessage ?: "Failed to delete transcription"
      }
    }
  }

  fun insertTextIntoSandbox(text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    sandboxOutputTarget.finishComposing()
    val current = sandboxOutputTarget.getTextBeforeCursor(1)
    val prefixSpace = if (current.isNotEmpty() && !current.last().isWhitespace()) " " else ""
    sandboxOutputTarget.commitText("$prefixSpace$clean ")
  }

  fun refreshImeStatus() {
    val imm = appContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val targetComponent = ComponentName(appContext, NuhAmharicImeService::class.java)
    val enabled =
      imm?.enabledInputMethodList?.any {
        it.component == targetComponent || it.packageName == appContext.packageName
      } == true
    val defaultIme =
      Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD,
      )
    val isDefault = defaultIme == targetComponent.flattenToShortString()
    _imeStatus.value = ImeActivationStatus(isEnabled = enabled, isDefaultSelected = isDefault)
  }

  fun selectSandboxPane(pane: SandboxPane) {
    sandboxController.flushComposing()
    _sandboxState.update { it.copy(activePane = pane) }
  }

  fun clearActiveSandboxPane() {
    sandboxController.resetSession(settings.value.inputMode)
    _sandboxState.update { state ->
      when (state.activePane) {
        SandboxPane.MESSAGE_PANE -> state.copy(messageText = "", composingText = "")
        SandboxPane.NOTES_PANE -> state.copy(notesText = "", composingText = "")
      }
    }
  }

  fun setSandboxPreviewPosture(posture: ZFoldPosture?) {
    _sandboxState.update { it.copy(previewPostureOverride = posture) }
  }

  fun cycleInputMode() {
    viewModelScope.launch {
      sandboxController.flushComposing()
      val next =
        if (settings.value.inputMode == AmharicInputMode.FIDEL_INPLACE) {
          AmharicInputMode.ENGLISH_QWERTY
        } else {
          AmharicInputMode.FIDEL_INPLACE
        }
      repository.setInputMode(next)
      sandboxController.resetSession(next)
      pushSettingsSnapshot(settings.value.copy(inputMode = next))
    }
  }

  fun setInputMode(mode: AmharicInputMode) {
    viewModelScope.launch {
      sandboxController.flushComposing()
      repository.setInputMode(mode)
      sandboxController.resetSession(mode)
      pushSettingsSnapshot(settings.value.copy(inputMode = mode))
    }
  }

  fun cycleSandboxOrFoldPosture() {
    val current = _sandboxState.value.previewPostureOverride
    val all = ZFoldPosture.entries
    val next =
      if (current == null) all.first()
      else {
        val idx = all.indexOf(current)
        if (idx == all.lastIndex) null else all[idx + 1]
      }
    _sandboxState.update { it.copy(previewPostureOverride = next) }
  }

  fun cycleOneHandedSide() {
    viewModelScope.launch {
      val values = OneHandedSide.entries
      val next = values[(values.indexOf(settings.value.oneHandedSide) + 1) % values.size]
      repository.setOneHandedSide(next)
    }
  }

  fun setFoldLayoutPreference(pref: FoldLayoutPreference) {
    viewModelScope.launch {
      repository.setFoldLayoutPreference(pref)
      pushSettingsSnapshot(settings.value.copy(foldLayoutPreference = pref))
    }
  }

  fun setSplitGapDp(gap: Float) {
    viewModelScope.launch { repository.setSplitGapDp(gap) }
  }

  fun setKeyHeightDp(height: Float) {
    viewModelScope.launch { repository.setKeyHeightDp(height) }
  }

  fun setLetterSizeSp(sizeSp: Float) {
    viewModelScope.launch { repository.setLetterSizeSp(sizeSp) }
  }

  fun setThemeStyle(style: KeyboardThemeStyle) {
    viewModelScope.launch {
      repository.setThemeStyle(style)
      pushSettingsSnapshot(settings.value.copy(themeStyle = style))
    }
  }

  fun setShowNumberRow(show: Boolean) {
    viewModelScope.launch { repository.setShowNumberRow(show) }
  }

  fun setShowEthiopicNumerals(show: Boolean) {
    viewModelScope.launch { repository.setShowEthiopicNumerals(show) }
  }

  fun setHapticsEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setHapticsEnabled(enabled) }
  }

  fun setKeyPopupEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setKeyPopupEnabled(enabled) }
  }

  fun setAutoCorrectEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setAutoCorrectEnabled(enabled)
      pushSettingsSnapshot(settings.value.copy(autoCorrectEnabled = enabled))
    }
  }

  fun setNextFidelHintsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setNextFidelHintsEnabled(enabled)
      pushSettingsSnapshot(settings.value.copy(nextFidelHintsEnabled = enabled))
    }
  }

  fun setOneHandedSide(side: OneHandedSide) {
    viewModelScope.launch { repository.setOneHandedSide(side) }
  }

  fun resetDefaults() {
    viewModelScope.launch {
      val customWords = settings.value.customDictionaryWords
      repository.resetDefaults()
      if (customWords.isNotEmpty()) {
        repository.setCustomDictionaryWords(customWords)
      }
      pushSettingsSnapshot(KeyboardSettings(customDictionaryWords = customWords))
    }
  }

  private suspend fun pushSettingsSnapshot(updated: KeyboardSettings) {
    cloudRepository.syncUserSettings(
      userId = currentUserId,
      displayName = currentUserDisplayName,
      settings = updated,
    )
  }

  override fun onCleared() {
    recorder.cancelRecording()
    super.onCleared()
  }

  companion object {
    fun factory(
      context: Context,
      userId: String,
      displayName: String,
    ): ViewModelProvider.Factory {
      val appCtx = context.applicationContext
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return NuhStudioViewModel(
            appContext = appCtx,
            repository = KeyboardPreferencesRepository(appCtx),
            cloudRepository = NuhCloudRepository(appCtx),
            transcriber = GeminiAudioTranscriber(),
            recorder = AudioWavRecorder(),
            currentUserId = userId,
            currentUserDisplayName = displayName,
          )
            as T
        }
      }
    }
  }
}
