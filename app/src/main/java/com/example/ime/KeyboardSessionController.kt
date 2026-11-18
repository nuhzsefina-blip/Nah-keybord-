package com.example.ime

import com.example.data.AmharicInputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class KeyboardSubLayer {
  LETTERS,
  SYMBOLS,
  ETHIOPIC_NUMERALS,
}

interface KeyboardOutputTarget {
  fun setComposingText(text: String)

  fun commitText(text: String)

  fun finishComposing()

  fun deleteBackward(count: Int = 1)

  fun replaceLastCharacter(replacement: String)

  fun performEnter()

  fun moveCursor(delta: Int)

  fun getTextBeforeCursor(maxChars: Int): String
}

data class KeyboardSessionUiState(
  val activeFamily: FidelFamily? = null,
  val inPlaceActiveFamily: FidelFamily? = null,
  val isShifted: Boolean = false,
  val isCapsLocked: Boolean = false,
  val subLayer: KeyboardSubLayer = KeyboardSubLayer.LETTERS,
  val currentWordPrefix: String = "",
  val nextFidelHints: List<NextFidelCandidate> = emptyList(),
  val autoCorrection: String? = null,
  val autoCorrectedFrom: String? = null,
  val suggestions: List<String> = listOf("ሰላም", "እንዴት", "እሺ", "አመሰግናለሁ", "መልካም"),
  val lastInsertedFidel: String = "",
)

class KeyboardSessionController(
  private val outputTarget: KeyboardOutputTarget,
) {
  private val _uiState = MutableStateFlow(KeyboardSessionUiState())
  val uiState: StateFlow<KeyboardSessionUiState> = _uiState.asStateFlow()

  private var lastShiftTapTimeMs = 0L
  private var customWords: Set<String> = emptySet()
  private var autoCorrectEnabled: Boolean = true
  private var nextFidelHintsEnabled: Boolean = true
  private var pendingUndoCorrection: Pair<String, String>? = null

  fun updateDictionaryConfig(
    customDictionaryWords: Set<String>,
    autoCorrect: Boolean,
    nextFidelHints: Boolean,
    inputMode: AmharicInputMode,
  ) {
    val changed =
      customWords != customDictionaryWords ||
        autoCorrectEnabled != autoCorrect ||
        nextFidelHintsEnabled != nextFidelHints
    customWords = customDictionaryWords
    autoCorrectEnabled = autoCorrect
    nextFidelHintsEnabled = nextFidelHints
    if (changed) {
      refreshWordSuggestions(inputMode)
    }
  }

  fun onEnglishKeyPressed(rawChar: Char) {
    pendingUndoCorrection = null
    val state = _uiState.value
    val ch =
      if (state.isShifted || state.isCapsLocked) rawChar.uppercaseChar() else rawChar.lowercaseChar()
    outputTarget.commitText(ch.toString())
    if (state.isShifted && !state.isCapsLocked) {
      _uiState.update { it.copy(isShifted = false, inPlaceActiveFamily = null) }
    }
    refreshWordSuggestions(AmharicInputMode.ENGLISH_QWERTY)
  }

  /**
   * In-Place Fidel QWERTY: tapping a consonant immediately commits its 1st order (e.g. ወ)
   * and activates inPlaceActiveFamily so the keyboard's vowel positions (e, u, i, a, ie, o)
   * transform in-place into blue morphed forms (ው, ዉ, ዊ, ዋ, ዌ, ዎ).
   */
  fun onInPlaceConsonantPressed(family: FidelFamily) {
    pendingUndoCorrection = null
    val glyph = family.baseChar
    outputTarget.commitText(glyph)
    _uiState.update { state ->
      state.copy(
        activeFamily = family,
        inPlaceActiveFamily = family,
        lastInsertedFidel = glyph,
        autoCorrectedFrom = null,
        isShifted = state.isCapsLocked,
      )
    }
    refreshWordSuggestions(AmharicInputMode.FIDEL_INPLACE)
  }

  /**
   * Tapping one of the in-place vowel keys (e->6th, u->2nd, i->3rd, a->4th, ie->5th, o->7th).
   * If a consonant is active, replaces it in-place (e.g., ወ -> ው) and resets vowel keys to idle.
   * Otherwise commits the standalone vowel (አ or ዐ family).
   */
  fun onInPlaceVowelPressed(orderIndex: Int) {
    pendingUndoCorrection = null
    val state = _uiState.value
    val active = state.inPlaceActiveFamily
    if (active != null) {
      val morphed = active.orders.getOrElse(orderIndex) { active.baseChar }
      outputTarget.replaceLastCharacter(morphed)
      _uiState.update {
        it.copy(
          inPlaceActiveFamily = null,
          activeFamily = active,
          lastInsertedFidel = morphed,
        )
      }
    } else {
      val standaloneFamilyId = if (state.isShifted || state.isCapsLocked) "a2" else "a1"
      val standaloneFamily = FidelTable.families.first { it.id == standaloneFamilyId }
      val vowelGlyph = standaloneFamily.orders.getOrElse(orderIndex) { standaloneFamily.baseChar }
      outputTarget.commitText(vowelGlyph)
      _uiState.update {
        it.copy(
          inPlaceActiveFamily = null,
          activeFamily = standaloneFamily,
          lastInsertedFidel = vowelGlyph,
          isShifted = it.isCapsLocked,
        )
      }
    }
    refreshWordSuggestions(AmharicInputMode.FIDEL_INPLACE)
  }

  fun onInPlaceLabializedPressed(specificVariant: String? = null) {
    pendingUndoCorrection = null
    val state = _uiState.value
    val active = state.inPlaceActiveFamily
    val labialized = active?.labialized.orEmpty()
    if (active != null && labialized.isNotEmpty()) {
      val glyph =
        specificVariant
          ?: if (labialized.size >= 3) labialized[2] else labialized.first()
      outputTarget.replaceLastCharacter(glyph)
      _uiState.update {
        it.copy(
          inPlaceActiveFamily = null,
          activeFamily = active,
          lastInsertedFidel = glyph,
        )
      }
    } else {
      val symbol = specificVariant ?: "፧"
      outputTarget.commitText(symbol)
      _uiState.update {
        it.copy(
          inPlaceActiveFamily = null,
          lastInsertedFidel = symbol,
        )
      }
    }
    refreshWordSuggestions(AmharicInputMode.FIDEL_INPLACE)
  }

  /**
   * Inserts or morphs the predicted next fidel suggested by the Amharic dictionary.
   */
  fun onNextFidelClicked(candidate: NextFidelCandidate, inputMode: AmharicInputMode) {
    pendingUndoCorrection = null
    if (candidate.replacesActiveConsonant && _uiState.value.inPlaceActiveFamily != null) {
      outputTarget.replaceLastCharacter(candidate.fidel)
    } else {
      outputTarget.commitText(candidate.fidel)
    }
    val nextFamily = FidelTable.findFamilyForChar(candidate.fidel)
    _uiState.update {
      it.copy(
        activeFamily = nextFamily ?: it.activeFamily,
        inPlaceActiveFamily = null,
        lastInsertedFidel = candidate.fidel,
      )
    }
    refreshWordSuggestions(inputMode)
  }

  fun onInsertSymbolOrText(text: String, inputMode: AmharicInputMode) {
    pendingUndoCorrection = null
    outputTarget.commitText(text)
    _uiState.update { it.copy(lastInsertedFidel = "", inPlaceActiveFamily = null) }
    refreshWordSuggestions(inputMode)
  }

  fun onSpacePressed(inputMode: AmharicInputMode) {
    val state = _uiState.value
    val correction =
      if (inputMode == AmharicInputMode.FIDEL_INPLACE && autoCorrectEnabled) {
        state.autoCorrection
      } else {
        null
      }
    val rawPrefix = state.currentWordPrefix
    val didCorrect = correction != null && rawPrefix.isNotEmpty() && correction != rawPrefix
    if (didCorrect) {
      outputTarget.deleteBackward(rawPrefix.length)
      outputTarget.commitText("$correction ")
      pendingUndoCorrection = rawPrefix to correction
    } else {
      pendingUndoCorrection = null
      outputTarget.commitText(" ")
    }
    _uiState.update {
      it.copy(
        lastInsertedFidel = "",
        currentWordPrefix = "",
        inPlaceActiveFamily = null,
        activeFamily = null,
        autoCorrection = null,
        autoCorrectedFrom = if (didCorrect) rawPrefix else null,
        nextFidelHints = emptyList(),
      )
    }
    refreshWordSuggestions(inputMode)
  }

  fun onUndoAutoCorrectClicked(inputMode: AmharicInputMode) {
    val undo = pendingUndoCorrection ?: return
    val (originalWord, correctedWord) = undo
    pendingUndoCorrection = null
    val before = outputTarget.getTextBeforeCursor(correctedWord.length + 1)
    if (before == "$correctedWord ") {
      outputTarget.deleteBackward(correctedWord.length + 1)
      outputTarget.commitText(originalWord)
    }
    _uiState.update {
      it.copy(
        lastInsertedFidel = "",
        inPlaceActiveFamily = null,
        autoCorrectedFrom = null,
      )
    }
    refreshWordSuggestions(inputMode)
  }

  fun onEnterPressed() {
    pendingUndoCorrection = null
    outputTarget.performEnter()
    _uiState.update {
      it.copy(
        lastInsertedFidel = "",
        currentWordPrefix = "",
        inPlaceActiveFamily = null,
        activeFamily = null,
        autoCorrection = null,
        nextFidelHints = emptyList(),
      )
    }
  }

  fun onBackspacePressed(inputMode: AmharicInputMode) {
    val undo = pendingUndoCorrection
    if (undo != null) {
      val (originalWord, correctedWord) = undo
      pendingUndoCorrection = null
      val before = outputTarget.getTextBeforeCursor(correctedWord.length + 1)
      if (before == "$correctedWord ") {
        outputTarget.deleteBackward(correctedWord.length + 1)
        outputTarget.commitText(originalWord)
        _uiState.update {
          it.copy(lastInsertedFidel = "", inPlaceActiveFamily = null, autoCorrectedFrom = null)
        }
        refreshWordSuggestions(inputMode)
        return
      }
    }
    outputTarget.deleteBackward(1)
    _uiState.update {
      it.copy(
        lastInsertedFidel = "",
        inPlaceActiveFamily = null,
        autoCorrectedFrom = null,
      )
    }
    refreshWordSuggestions(inputMode)
  }

  fun onSuggestionClicked(word: String, inputMode: AmharicInputMode) {
    pendingUndoCorrection = null
    val prefixLen = _uiState.value.currentWordPrefix.length
    if (prefixLen > 0) {
      outputTarget.deleteBackward(prefixLen)
    }
    outputTarget.commitText("$word ")
    _uiState.update {
      it.copy(
        currentWordPrefix = "",
        lastInsertedFidel = "",
        inPlaceActiveFamily = null,
        activeFamily = null,
        autoCorrection = null,
        autoCorrectedFrom = null,
        nextFidelHints = emptyList(),
      )
    }
    refreshWordSuggestions(inputMode)
  }

  fun onShiftPressed() {
    val now = System.currentTimeMillis()
    _uiState.update { state ->
      if (state.isCapsLocked) {
        state.copy(isShifted = false, isCapsLocked = false)
      } else if (state.isShifted && now - lastShiftTapTimeMs < 350L) {
        state.copy(isShifted = true, isCapsLocked = true)
      } else {
        state.copy(isShifted = !state.isShifted, isCapsLocked = false)
      }
    }
    lastShiftTapTimeMs = now
  }

  fun setSubLayer(layer: KeyboardSubLayer) {
    _uiState.update { it.copy(subLayer = layer, inPlaceActiveFamily = null) }
  }

  fun toggleSubLayer(target: KeyboardSubLayer) {
    val current = _uiState.value.subLayer
    setSubLayer(if (current == target) KeyboardSubLayer.LETTERS else target)
  }

  fun moveCursor(delta: Int) {
    pendingUndoCorrection = null
    _uiState.update { it.copy(inPlaceActiveFamily = null) }
    outputTarget.moveCursor(delta)
  }

  fun flushComposing() {
    outputTarget.finishComposing()
  }

  fun resetSession(inputMode: AmharicInputMode = AmharicInputMode.FIDEL_INPLACE) {
    pendingUndoCorrection = null
    _uiState.value = KeyboardSessionUiState()
    refreshWordSuggestions(inputMode)
  }

  private fun refreshWordSuggestions(inputMode: AmharicInputMode) {
    if (inputMode == AmharicInputMode.ENGLISH_QWERTY) {
      _uiState.update {
        it.copy(
          currentWordPrefix = "",
          nextFidelHints = emptyList(),
          autoCorrection = null,
          suggestions = listOf("Hello", "Thanks", "On my way", "Z Fold 7", "Split-screen"),
        )
      }
      return
    }

    val before = outputTarget.getTextBeforeCursor(24)
    val wordBefore = before.takeLastWhile { !it.isWhitespace() && it != '።' && it != '፣' && it != '፡' }
    val result =
      AmharicDictionary.suggest(
        prefix = wordBefore,
        activeConsonantFamily = _uiState.value.inPlaceActiveFamily,
        customWords = customWords,
        autoCorrectEnabled = autoCorrectEnabled,
        nextFidelHintsEnabled = nextFidelHintsEnabled,
      )
    _uiState.update {
      it.copy(
        currentWordPrefix = wordBefore,
        nextFidelHints = result.nextFidelHints,
        autoCorrection = result.autoCorrection,
        suggestions = result.wordCompletions,
      )
    }
  }
}
