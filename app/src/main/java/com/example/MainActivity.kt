package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DevicesFold
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signOutFromGoogle
import com.example.foldable.rememberWindowLayoutInfo
import com.example.foldable.resolveZFoldPosture
import com.example.ui.NuhStudioViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FidelGuideScreen
import com.example.ui.screens.FoldStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceTranscribeScreen
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.NuhKeyboardTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class AppDestination(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
) {
  STUDIO(R.string.nav_studio, Icons.Filled.DevicesFold, Icons.Outlined.DevicesFold, "nav_studio"),
  VOICE(R.string.nav_voice, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, "nav_voice"),
  FIDEL(R.string.nav_fidel, Icons.Filled.Translate, Icons.Outlined.Translate, "nav_fidel"),
  SETTINGS(R.string.nav_settings, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_settings"),
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()

    setContent {
      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      val auth = remember { Firebase.auth }
      val credentialManager = remember { CredentialManager.create(context) }
      val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

      var isSigningIn by remember { mutableStateOf(false) }
      var authError by remember { mutableStateOf<String?>(null) }

      LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
          attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { authError = null },
            onUnauthenticated = {},
            scope = scope,
          )
        }
      }

      if (currentUser == null) {
        NuhKeyboardTheme(darkTheme = true) {
          AuthScreen(
            isSigningIn = isSigningIn,
            errorMessage = authError,
            onSignInClick = {
              isSigningIn = true
              authError = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isSigningIn = false
                  authError = null
                },
                onAuthError = { msg ->
                  isSigningIn = false
                  authError = msg
                },
                onAuthCancelled = { isSigningIn = false },
                scope = scope,
              )
            },
          )
        }
      } else {
        val signedInUser = currentUser!!
        key(signedInUser.uid) {
          val displayName =
            signedInUser.displayName?.takeIf { it.isNotBlank() }
              ?: signedInUser.email?.substringBefore("@")
              ?: "ኑህ User"
          val viewModel: NuhStudioViewModel =
            viewModel(
              key = signedInUser.uid,
              factory =
                NuhStudioViewModel.factory(
                  context = context,
                  userId = signedInUser.uid,
                  displayName = displayName,
                ),
            )
          val settings by viewModel.settings.collectAsStateWithLifecycle()
          val isDark = settings.themeStyle != KeyboardThemeStyle.ADDIS_DAYLIGHT

          NuhKeyboardTheme(darkTheme = isDark) {
            NuhKeyboardCompanionApp(
              viewModel = viewModel,
              isInMultiWindow = isInMultiWindowMode,
              onOpenImeSettings = {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
              },
              onShowImePicker = {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showInputMethodPicker()
              },
              onSignOutClick = {
                signOutFromGoogle(
                  credentialManager = credentialManager,
                  scope = scope,
                  onBeforeFirebaseSignOut = { viewModel.cancelVoiceRecording() },
                )
              },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuhKeyboardCompanionApp(
  viewModel: NuhStudioViewModel,
  isInMultiWindow: Boolean,
  onOpenImeSettings: () -> Unit,
  onShowImePicker: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
  val sandboxState by viewModel.sandboxState.collectAsStateWithLifecycle()
  val dictionaryState by viewModel.dictionaryEntriesState.collectAsStateWithLifecycle()
  val transcriptionsState by viewModel.transcriptionsState.collectAsStateWithLifecycle()
  val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
  val recordingTelemetry by viewModel.recordingTelemetry.collectAsStateWithLifecycle()
  val cloudError by viewModel.cloudActionError.collectAsStateWithLifecycle()
  val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
  val windowLayoutInfo = rememberWindowLayoutInfo()

  var currentDestination by rememberSaveable { mutableStateOf(AppDestination.STUDIO) }

  if (currentDestination != AppDestination.STUDIO) {
    BackHandler { currentDestination = AppDestination.STUDIO }
  }

  LaunchedEffect(cloudError) {
    val msg = cloudError
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissCloudError()
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshImeStatus()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun copyAndNotify(text: String) {
    if (text.isBlank()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("ኑህ Amharic", text))
    scope.launch {
      snackbarHostState.showSnackbar("Copied: $text")
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp
    val foldableState =
      resolveZFoldPosture(
        availableWidthDp = maxWidth,
        availableHeightDp = maxHeight,
        windowLayoutInfo = windowLayoutInfo,
        preference = settings.foldLayoutPreference,
        isInMultiWindowMode = isInMultiWindow,
      )

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "ኑህ · Amharic Fold Keyboard",
              fontFamily = EthiopicFontFamily,
              style = MaterialTheme.typography.titleLarge,
            )
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            AppDestination.entries.forEach { dest ->
              val selected = currentDestination == dest
              NavigationBarItem(
                selected = selected,
                onClick = { currentDestination = dest },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            AppDestination.entries.forEach { dest ->
              val selected = currentDestination == dest
              NavigationRailItem(
                selected = selected,
                onClick = { currentDestination = dest },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.tag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f)) {
          when (currentDestination) {
            AppDestination.STUDIO ->
              FoldStudioScreen(
                settings = settings,
                foldableState = foldableState,
                imeStatus = imeStatus,
                sandboxState = sandboxState,
                sandboxController = viewModel.sandboxController,
                voiceState = voiceState,
                recordingTelemetry = recordingTelemetry,
                onStartVoiceDictation = {
                  viewModel.startVoiceRecording(autoStopInsertIntoSandbox = true)
                },
                onStopVoiceDictation = {
                  viewModel.stopAndTranscribeAudio(insertIntoSandbox = true)
                },
                onOpenVoiceTab = { currentDestination = AppDestination.VOICE },
                onEnableImeClick = onOpenImeSettings,
                onSelectImeClick = onShowImePicker,
                onSelectSandboxPane = { viewModel.selectSandboxPane(it) },
                onClearSandboxPane = { viewModel.clearActiveSandboxPane() },
                onSetPreviewPosture = { viewModel.setSandboxPreviewPosture(it) },
                onSelectInputMode = { viewModel.setInputMode(it) },
                onToggleInputMode = { viewModel.cycleInputMode() },
                onCyclePosture = { viewModel.cycleSandboxOrFoldPosture() },
                onToggleOneHanded = { viewModel.cycleOneHandedSide() },
                onNavigateToErgonomics = { currentDestination = AppDestination.SETTINGS },
                onCopyTextToClipboard = { copyAndNotify(it) },
              )
            AppDestination.VOICE ->
              VoiceTranscribeScreen(
                voiceState = voiceState,
                recordingTelemetry = recordingTelemetry,
                transcriptionsState = transcriptionsState,
                onStartRecording = {
                  viewModel.startVoiceRecording(autoStopInsertIntoSandbox = false)
                },
                onStopAndTranscribe = {
                  viewModel.stopAndTranscribeAudio(insertIntoSandbox = false)
                },
                onCancelRecording = { viewModel.cancelVoiceRecording() },
                onInsertIntoSandbox = { transcript ->
                  viewModel.insertTextIntoSandbox(transcript)
                  currentDestination = AppDestination.STUDIO
                },
                onCopyTranscript = { copyAndNotify(it) },
                onDeleteTranscription = { viewModel.deleteTranscription(it) },
              )
            AppDestination.FIDEL ->
              FidelGuideScreen(
                dictionaryState = dictionaryState,
                onAddDictionaryWord = { word, note -> viewModel.addDictionaryWord(word, note) },
                onDeleteDictionaryWord = { entryId -> viewModel.deleteDictionaryWord(entryId) },
                onCopyGlyph = { copyAndNotify(it) },
              )
            AppDestination.SETTINGS ->
              SettingsScreen(
                settings = settings,
                userDisplayName = viewModel.currentUserDisplayName,
                userId = viewModel.currentUserId,
                isSyncingCloud = isSyncingCloud,
                onSyncNowClick = { viewModel.syncSettingsToCloud() },
                onSignOutClick = onSignOutClick,
                onSetInputMode = { viewModel.setInputMode(it) },
                onSetFoldLayoutPreference = { viewModel.setFoldLayoutPreference(it) },
                onSetSplitGapDp = { viewModel.setSplitGapDp(it) },
                onSetKeyHeightDp = { viewModel.setKeyHeightDp(it) },
                onSetLetterSizeSp = { viewModel.setLetterSizeSp(it) },
                onSetThemeStyle = { viewModel.setThemeStyle(it) },
                onSetOneHandedSide = { viewModel.setOneHandedSide(it) },
                onSetShowNumberRow = { viewModel.setShowNumberRow(it) },
                onSetShowEthiopicNumerals = { viewModel.setShowEthiopicNumerals(it) },
                onSetHapticsEnabled = { viewModel.setHapticsEnabled(it) },
                onSetKeyPopupEnabled = { viewModel.setKeyPopupEnabled(it) },
                onSetAutoCorrectEnabled = { viewModel.setAutoCorrectEnabled(it) },
                onSetNextFidelHintsEnabled = { viewModel.setNextFidelHintsEnabled(it) },
                onResetDefaults = { viewModel.resetDefaults() },
              )
          }
        }
      }
    }
  }
}
