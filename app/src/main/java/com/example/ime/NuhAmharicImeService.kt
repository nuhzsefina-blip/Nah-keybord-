package com.example.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.AmharicInputMode
import com.example.data.FoldLayoutPreference
import com.example.data.KeyboardPreferencesRepository
import com.example.data.KeyboardSettings
import com.example.data.OneHandedSide
import com.example.foldable.resolveZFoldPosture
import com.example.ui.keyboard.NuhKeyboardPanel
import com.example.ui.theme.NuhKeyboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NuhAmharicImeService :
  InputMethodService(),
  LifecycleOwner,
  ViewModelStoreOwner,
  SavedStateRegistryOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val store = ViewModelStore()
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val viewModelStore: ViewModelStore
    get() = store

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  private lateinit var repository: KeyboardPreferencesRepository
  private lateinit var controller: KeyboardSessionController

  private val inputConnectionTarget =
    object : KeyboardOutputTarget {
      override fun setComposingText(text: String) {
        currentInputConnection?.setComposingText(text, 1)
      }

      override fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
      }

      override fun finishComposing() {
        currentInputConnection?.finishComposingText()
      }

      override fun deleteBackward(count: Int) {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
          ic.commitText("", 1)
        } else {
          ic.deleteSurroundingText(count, 0)
        }
      }

      override fun replaceLastCharacter(replacement: String) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(1, 0)
        ic.commitText(replacement, 1)
        ic.endBatchEdit()
      }

      override fun performEnter() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        val actionId =
          editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (actionId != EditorInfo.IME_ACTION_NONE &&
          actionId != EditorInfo.IME_ACTION_UNSPECIFIED &&
          editorInfo != null &&
          (editorInfo.inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == 0
        ) {
          ic.performEditorAction(actionId)
        } else {
          ic.commitText("\n", 1)
        }
      }

      override fun moveCursor(delta: Int) {
        val ic = currentInputConnection ?: return
        val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(delta)) {
          ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
          ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
      }

      override fun getTextBeforeCursor(maxChars: Int): String =
        currentInputConnection?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()
    }

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    repository = KeyboardPreferencesRepository(applicationContext)
    controller = KeyboardSessionController(outputTarget = inputConnectionTarget)
  }

  override fun onEvaluateFullscreenMode(): Boolean = false

  override fun onCreateInputView(): View {
    window?.window?.decorView?.let { decor ->
      decor.setViewTreeLifecycleOwner(this)
      decor.setViewTreeViewModelStoreOwner(this)
      decor.setViewTreeSavedStateRegistryOwner(this)
    }

    return ComposeView(this).apply {
      setViewTreeLifecycleOwner(this@NuhAmharicImeService)
      setViewTreeViewModelStoreOwner(this@NuhAmharicImeService)
      setViewTreeSavedStateRegistryOwner(this@NuhAmharicImeService)
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val settings by
          repository.settingsFlow.collectAsStateWithLifecycle(initialValue = KeyboardSettings())
        val config = LocalConfiguration.current
        val widthDp = config.screenWidthDp.dp
        val heightDp = config.screenHeightDp.dp
        val postureState =
          resolveZFoldPosture(
            availableWidthDp = widthDp,
            availableHeightDp = heightDp,
            windowLayoutInfo = null,
            preference = settings.foldLayoutPreference,
            isInMultiWindowMode = false,
          )

        NuhKeyboardTheme {
          Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            NuhKeyboardPanel(
              settings = settings,
              posture = postureState.activePosture,
              controller = controller,
              onSelectInputMode = { selectInputMode(it) },
              onToggleInputMode = { cycleInputMode() },
              onCyclePosture = { cycleFoldLayoutPreference() },
              onToggleOneHanded = { cycleOneHandedSide() },
              onOpenSettings = { launchCompanionSettings() },
            )
          }
        }
      }
    }
  }

  override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    if (!restarting) {
      serviceScope.launch {
        val mode = repository.settingsFlow.first().inputMode
        controller.resetSession(mode)
      }
    }
  }

  override fun onFinishInputView(finishingInput: Boolean) {
    controller.flushComposing()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    super.onFinishInputView(finishingInput)
  }

  override fun onDestroy() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    store.clear()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun selectInputMode(mode: AmharicInputMode) {
    serviceScope.launch {
      controller.flushComposing()
      repository.setInputMode(mode)
      controller.resetSession(mode)
    }
  }

  private fun cycleInputMode() {
    serviceScope.launch {
      val current = repository.settingsFlow.first().inputMode
      val next =
        if (current == AmharicInputMode.FIDEL_INPLACE) AmharicInputMode.ENGLISH_QWERTY
        else AmharicInputMode.FIDEL_INPLACE
      controller.flushComposing()
      repository.setInputMode(next)
      controller.resetSession(next)
    }
  }

  private fun cycleFoldLayoutPreference() {
    serviceScope.launch {
      val values = FoldLayoutPreference.entries
      val current = repository.settingsFlow.first().foldLayoutPreference
      val next = values[(values.indexOf(current) + 1) % values.size]
      repository.setFoldLayoutPreference(next)
    }
  }

  private fun cycleOneHandedSide() {
    serviceScope.launch {
      val values = OneHandedSide.entries
      val current = repository.settingsFlow.first().oneHandedSide
      val next = values[(values.indexOf(current) + 1) % values.size]
      repository.setOneHandedSide(next)
    }
  }

  private fun launchCompanionSettings() {
    val intent =
      Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      }
    startActivity(intent)
  }
}
