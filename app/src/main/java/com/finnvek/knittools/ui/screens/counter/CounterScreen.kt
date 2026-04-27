package com.finnvek.knittools.ui.screens.counter

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.ProjectCounterEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.RollingCounter
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.theme.YarnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onBack: () -> Unit = {},
    onSessionHistory: (Long) -> Unit = {},
    onPhotoGallery: () -> Unit = {},
    onPatternViewer: (Long) -> Unit = {},
    onNotesEditor: (Long) -> Unit = {},
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val voiceCommandHandler =
        remember(context, coroutineScope) {
            VoiceCommandHandler(context, coroutineScope)
        }
    val voiceResponseManager =
        remember(context, voiceCommandHandler) {
            VoiceResponseManager(context, voiceCommandHandler)
        }
    val isVoiceListening by voiceCommandHandler.isListening.collectAsStateWithLifecycle()
    val isContinuousMode by voiceCommandHandler.isContinuousMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                if (state.voiceLiveEnabled) {
                    viewModel.startLiveVoice()
                } else {
                    voiceCommandHandler.startContinuousListening()
                }
            }
        }
    // TopBar mic: toggle live (v3) tai continuous (v2)
    val toggleVoice = {
        when {
            // Live API aktiivinen → pysäytä
            state.isLiveSessionActive -> {
                viewModel.stopLiveVoice()
            }

            // V2 continuous aktiivinen → pysäytä
            voiceCommandHandler.isContinuousMode.value -> {
                voiceCommandHandler.stopContinuousListening()
            }

            // Live API käytössä → käynnistä live
            state.voiceLiveEnabled && hasAudioPermission(context) -> {
                viewModel.startLiveVoice()
            }

            // V2 fallback → käynnistä continuous
            !state.voiceLiveEnabled && hasAudioPermission(context) -> {
                voiceCommandHandler.startContinuousListening()
            }

            // Ei lupaa → pyydä
            else -> {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showProjectActionsSheet by rememberSaveable { mutableStateOf(false) }
    var showCountersListSheet by rememberSaveable { mutableStateOf(false) }
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var showNotesSheet by rememberSaveable { mutableStateOf(false) }
    var showYarnPicker by rememberSaveable { mutableStateOf(false) }
    var showYarnManagementSheet by rememberSaveable { mutableStateOf(false) }
    var showSummarySheet by rememberSaveable { mutableStateOf(false) }
    var showAddReminder by rememberSaveable { mutableStateOf(false) }
    var showAddCounter by rememberSaveable { mutableStateOf(false) }
    var showStitchDialog by rememberSaveable { mutableStateOf(false) }
    var showPatternInfoSheet by rememberSaveable { mutableStateOf(false) }
    var showPatternPicker by rememberSaveable { mutableStateOf(false) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    val savedYarnCards by viewModel.savedYarnCards.collectAsStateWithLifecycle()
    val savedPatterns by viewModel.savedPatterns.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshNanoAvailability()
    }

    VoiceCommandEffects(
        voiceCommandHandler = voiceCommandHandler,
        voiceResponseManager = voiceResponseManager,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )

    val vibrator = rememberVibrator()

    val performHaptic =
        remember(state.hapticFeedback, vibrator) {
            {
                if (state.hapticFeedback) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(12, 60))
                }
            }
        }

    KeepScreenAwake(state.keepScreenAwake)
    HandleVoiceCommands(
        voiceCommandHandler = voiceCommandHandler,
        viewModel = viewModel,
        performHaptic = performHaptic,
    )
    TriggerAlertHaptic(
        alertId = state.activeAlert?.id,
        hasActiveAlert = state.activeAlert != null,
        performHaptic = performHaptic,
    )
    val sheetActions =
        rememberCounterSheetActions(
            viewModel = viewModel,
            onShowYarnPicker = { showYarnPicker = true },
            onHideYarnPicker = { showYarnPicker = false },
            onHideYarnManagementSheet = { showYarnManagementSheet = false },
            onHideNotesSheet = { showNotesSheet = false },
            onExpandNotes = { state.projectId?.let(onNotesEditor) },
            onHideSummarySheet = { showSummarySheet = false },
            onHidePatternPicker = { showPatternPicker = false },
            onHidePatternInfoSheet = { showPatternInfoSheet = false },
        )
    val projectCountersActions =
        rememberProjectCountersSectionActions(
            viewModel = viewModel,
            performHaptic = performHaptic,
            onShowAddCounter = { showAddCounter = true },
        )
    val dialogActionDependencies =
        CounterDialogActionDependencies(
            viewModel = viewModel,
            projectId = state.projectId,
            renameText = renameText,
            onRenameTextChange = { renameText = it },
            onBack = onBack,
            onHideAddReminder = { showAddReminder = false },
            onHideAddCounter = { showAddCounter = false },
            onHideResetDialog = { showResetDialog = false },
            onHideCompleteDialog = { showCompleteDialog = false },
            onHideDeleteDialog = { showDeleteDialog = false },
            onHideRenameDialog = { showRenameDialog = false },
            onHideStitchDialog = { showStitchDialog = false },
        )
    val topBarActionDependencies =
        CounterTopBarActionDependencies(
            onBack = onBack,
            onPhotoGallery = onPhotoGallery,
            projectId = state.projectId,
            onPatternViewer = onPatternViewer,
            onShowProjectActions = { showProjectActionsSheet = true },
        )
    val startRename = {
        renameText = state.projectName
        showRenameDialog = true
    }
    val projectHeaderActionDependencies =
        ProjectHeaderActionDependencies(
            viewModel = viewModel,
            projectId = state.projectId,
            onPatternViewer = onPatternViewer,
            onShowPatternInfo = { showPatternInfoSheet = true },
            onShowPatternPicker = { showPatternPicker = true },
            onEditingNameChange = { isEditingName = it },
        )
    val dialogActions = rememberCounterDialogActions(dialogActionDependencies)
    val topBarActions = rememberCounterTopBarActions(topBarActionDependencies)
    val projectHeaderActions = rememberProjectHeaderActions(projectHeaderActionDependencies)
    val mainContentActions =
        remember(viewModel, performHaptic, onPatternViewer) {
            CounterMainContentActions(
                onSurfaceIncrement = {
                    performHaptic()
                    viewModel.increment()
                },
                onDecrement = {
                    performHaptic()
                    viewModel.decrement()
                },
                onIncrement = {
                    performHaptic()
                    viewModel.increment()
                },
                onUndo = {
                    performHaptic()
                    viewModel.undo()
                },
                onOpenPatternViewer = onPatternViewer,
                onDecrementSecondary = {
                    performHaptic()
                    viewModel.decrementSecondary()
                },
                onIncrementSecondary = {
                    performHaptic()
                    viewModel.incrementSecondary()
                },
                onDecrementStitch = {
                    performHaptic()
                    viewModel.decrementStitch()
                },
                onIncrementStitch = {
                    performHaptic()
                    viewModel.incrementStitch()
                },
                onShowTargetDialog = { showTargetDialog = true },
            )
        }

    CounterScreenDialogs(
        state =
            CounterDialogState(
                showAddReminder = showAddReminder,
                showAddCounter = showAddCounter,
                showResetDialog = showResetDialog,
                showCompleteDialog = showCompleteDialog,
                showDeleteDialog = showDeleteDialog,
                showRenameDialog = showRenameDialog,
                showStitchDialog = showStitchDialog,
                projectName = state.projectName,
                renameText = renameText,
                currentStitchCount = state.stitchCount,
            ),
        actions = dialogActions,
    )

    if (showTargetDialog) {
        TargetRowsDialog(
            currentTarget = state.targetRows,
            onDismiss = { showTargetDialog = false },
            onConfirm = { target ->
                viewModel.setTargetRows(target)
                showTargetDialog = false
            },
            onRemove = {
                viewModel.clearTarget()
                showTargetDialog = false
            },
        )
    }

    CounterScreenSheets(
        state =
            CounterSheetState(
                showYarnPicker = showYarnPicker,
                showYarnManagementSheet = showYarnManagementSheet,
                savedYarnCards = savedYarnCards,
                linkedYarns = state.linkedYarns,
                showNotesSheet = showNotesSheet,
                notes = state.notes,
                showSummarySheet = showSummarySheet,
                isSummaryLoading = state.isSummaryLoading,
                projectSummary = state.projectSummary,
                summaryError = state.summaryError,
                showPatternPicker = showPatternPicker,
                projectId = state.projectId,
                savedPatterns = savedPatterns,
                isPro = state.isPro,
                showPatternInfoSheet = showPatternInfoSheet,
                linkedPattern = state.linkedPattern,
            ),
        actions = sheetActions,
    )

    if (showProjectActionsSheet) {
        ProjectActionsBottomSheet(
            state = ProjectActionsSheetState(
                linkedYarnCount = state.linkedYarns.size,
                projectCounterCount = state.projectCounters.size,
                stitchTrackingEnabled = state.stitchTrackingEnabled,
                isPro = state.isPro,
                isAiAvailable = state.isAiAvailable,
            ),
            callbacks = ProjectActionsSheetCallbacks(
                onDismiss = { showProjectActionsSheet = false },
                onOpenYarnManagement = {
                    showProjectActionsSheet = false
                    showYarnManagementSheet = true
                },
                onOpenNotes = {
                    showProjectActionsSheet = false
                    state.projectId?.let(onNotesEditor)
                },
                onOpenSummary = {
                    showProjectActionsSheet = false
                    viewModel.generateSummary()
                    showSummarySheet = true
                },
                onOpenPhotos = {
                    showProjectActionsSheet = false
                    onPhotoGallery()
                },
                onOpenCountersList = {
                    showProjectActionsSheet = false
                    showCountersListSheet = true
                },
                onOpenAddCounter = {
                    showProjectActionsSheet = false
                    showAddCounter = true
                },
                onToggleStitchTracking = viewModel::setStitchTrackingEnabled,
                onOpenSessionHistory = {
                    showProjectActionsSheet = false
                    state.projectId?.let(onSessionHistory)
                },
                onStartRename = {
                    showProjectActionsSheet = false
                    startRename()
                },
                onShowResetDialog = {
                    showProjectActionsSheet = false
                    showResetDialog = true
                },
                onShowCompleteDialog = {
                    showProjectActionsSheet = false
                    showCompleteDialog = true
                },
                onShowDeleteDialog = {
                    showProjectActionsSheet = false
                    showDeleteDialog = true
                },
            ),
        )
    }

    if (showCountersListSheet) {
        CountersListSheet(
            projectCounters = state.projectCounters,
            mainRowCount = state.counter.count,
            actions = projectCountersActions,
            onDismiss = { showCountersListSheet = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CounterTopBar(
                isPro = state.isPro,
                showPatternIcon = state.projectId != null &&
                    (state.patternUri != null || state.linkedPattern != null),
                micState =
                    CounterTopBarMicState(
                        isVoiceListening = isVoiceListening,
                        isContinuousMode = isContinuousMode,
                        isLiveSessionActive = state.isLiveSessionActive,
                    ),
                onMicClick = toggleVoice,
                actions = topBarActions,
            )
        },
    ) { scaffoldPadding ->
        CounterScreenContent(
            scaffoldPadding = scaffoldPadding,
            state = state,
            isEditingName = isEditingName,
            projectHeaderActions = projectHeaderActions,
            actions = mainContentActions,
        )
    }
}

@Composable
private fun HandleVoiceCommands(
    voiceCommandHandler: VoiceCommandHandler,
    viewModel: CounterViewModel,
    performHaptic: () -> Unit,
) {
    LaunchedEffect(voiceCommandHandler) {
        voiceCommandHandler.recognizedCommand.collect { command ->
            when (command) {
                is VoiceCommand.Increment -> {
                    repeat(command.count) {
                        performHaptic()
                        viewModel.increment()
                    }
                }

                is VoiceCommand.Decrement -> {
                    repeat(command.count) {
                        performHaptic()
                        viewModel.decrement()
                    }
                }

                VoiceCommand.Undo -> {
                    performHaptic()
                    viewModel.undo()
                }

                VoiceCommand.Reset -> {
                    viewModel.reset()
                }

                VoiceCommand.StitchIncrement -> {
                    performHaptic()
                    viewModel.incrementStitch()
                }

                VoiceCommand.StitchDecrement -> {
                    performHaptic()
                    viewModel.decrementStitch()
                }

                VoiceCommand.StopListening -> {
                    // Handler pysäyttää jatkuvan kuuntelun itse
                }

                VoiceCommand.Help -> {
                    Unit
                }
            }
            // TTS-vahvistus ei-triviaaleille komennoille (paikallinen parseri, offline-yhteensopiva)
            viewModel.emitLocalVoiceFeedback(command)
        }
    }
}

@Composable
private fun VoiceCommandEffects(
    voiceCommandHandler: VoiceCommandHandler,
    voiceResponseManager: VoiceResponseManager,
    viewModel: CounterViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current

    DisposableEffect(voiceCommandHandler, voiceResponseManager) {
        onDispose {
            voiceResponseManager.destroy()
            voiceCommandHandler.destroy()
        }
    }

    // Pysäytä Live API -sessio kun näyttö poistuu tai sovellus menee taustalle
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopLiveVoice()
        }
    }

    LaunchedEffect(voiceCommandHandler) {
        voiceCommandHandler.voiceError.collect { error ->
            val message =
                when (error) {
                    VoiceError.Timeout -> context.getString(R.string.voice_mode_timeout)
                    VoiceError.Fatal -> context.getString(R.string.voice_recognizer_error)
                    VoiceError.NetworkLost -> context.getString(R.string.voice_offline_mode)
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(voiceCommandHandler) {
        voiceCommandHandler.unrecognizedText.collect { text ->
            viewModel.interpretVoiceCommand(text)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.voiceResponse.collect { text ->
            // Älä puhu TTS:llä kun Live API hoitaa äänentoiston
            if (!viewModel.uiState.value.isLiveSessionActive) {
                voiceResponseManager.speak(text)
            }
        }
    }

    // Live API fallback → näytä snackbar + käynnistä v2 continuous mode
    LaunchedEffect(viewModel) {
        viewModel.fallbackToV2.collect { errorMessage ->
            if (errorMessage != null) {
                snackbarHostState.showSnackbar(errorMessage)
            }
            if (hasAudioPermission(context)) {
                voiceCommandHandler.startContinuousListening()
            }
        }
    }
}

@Composable
private fun TriggerAlertHaptic(
    alertId: Long?,
    hasActiveAlert: Boolean,
    performHaptic: () -> Unit,
) {
    LaunchedEffect(alertId) {
        if (hasActiveAlert) performHaptic()
    }
}

// Data-luokat CounterScreenDialogs-parametrien ryhmittelyyn (S107)
data class CounterDialogState(
    val showAddReminder: Boolean,
    val showAddCounter: Boolean,
    val showResetDialog: Boolean,
    val showCompleteDialog: Boolean,
    val showDeleteDialog: Boolean,
    val showRenameDialog: Boolean,
    val showStitchDialog: Boolean,
    val projectName: String,
    val renameText: String,
    val currentStitchCount: Int?,
)

data class CounterDialogActions(
    val onAddReminderSave: (Int, Int?, String) -> Unit,
    val onAddReminderDismiss: () -> Unit,
    val onAddCounterSave: (ProjectCounterDraft) -> Unit,
    val onAddCounterDismiss: () -> Unit,
    val onResetConfirm: () -> Unit,
    val onResetDismiss: () -> Unit,
    val onCompleteConfirm: () -> Unit,
    val onCompleteDismiss: () -> Unit,
    val onDeleteConfirm: () -> Unit,
    val onDeleteDismiss: () -> Unit,
    val onRenameTextChange: (String) -> Unit,
    val onRenameConfirm: () -> Unit,
    val onRenameDismiss: () -> Unit,
    val onStitchConfirm: (Int?) -> Unit,
    val onStitchDismiss: () -> Unit,
)

@Composable
private fun CounterScreenDialogs(
    state: CounterDialogState,
    actions: CounterDialogActions,
) {
    if (state.showAddReminder) {
        AddReminderDialog(
            onSave = actions.onAddReminderSave,
            onDismiss = actions.onAddReminderDismiss,
        )
    }
    if (state.showAddCounter) {
        AddCounterDialog(
            onSave = actions.onAddCounterSave,
            onDismiss = actions.onAddCounterDismiss,
            canUseRepeatSection = true,
        )
    }
    if (state.showResetDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.reset_counter),
            message = stringResource(R.string.reset_counter_message),
            confirmText = stringResource(R.string.reset),
            onConfirm = actions.onResetConfirm,
            onDismiss = actions.onResetDismiss,
        )
    }
    if (state.showCompleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.complete_project),
            message = stringResource(R.string.complete_project_message, state.projectName),
            confirmText = stringResource(R.string.complete_project),
            onConfirm = actions.onCompleteConfirm,
            onDismiss = actions.onCompleteDismiss,
        )
    }
    if (state.showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_project),
            message = stringResource(R.string.delete_project_message, state.projectName),
            confirmText = stringResource(R.string.delete_project),
            isDestructive = true,
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
        )
    }
    if (state.showRenameDialog) {
        CounterRenameDialog(
            renameText = state.renameText,
            onRenameTextChange = actions.onRenameTextChange,
            onConfirm = actions.onRenameConfirm,
            onDismiss = actions.onRenameDismiss,
        )
    }
    if (state.showStitchDialog) {
        StitchCountDialog(
            currentStitchCount = state.currentStitchCount,
            onConfirm = {
                actions.onStitchConfirm(it)
                actions.onStitchDismiss()
            },
            onDismiss = actions.onStitchDismiss,
        )
    }
}

@Composable
private fun CounterRenameDialog(
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_project)) },
        text = {
            TextField(
                value = renameText,
                onValueChange = onRenameTextChange,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = renameText.isNotBlank(),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun StitchCountDialog(
    currentStitchCount: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var stitchInput by rememberSaveable {
        mutableStateOf(currentStitchCount?.toString() ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stitches_per_row)) },
        text = {
            TextField(
                value = stitchInput,
                onValueChange = { stitchInput = it.filter { c -> c.isDigit() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(stitchInput.toIntOrNull()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentStitchCount != null && currentStitchCount > 0) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}

// Data-luokka CounterScreenSheets-parametrien ryhmittelyyn (S107)
data class CounterSheetState(
    val showYarnPicker: Boolean,
    val showYarnManagementSheet: Boolean,
    val savedYarnCards: List<YarnCardEntity>,
    val linkedYarns: List<Pair<Long, String>>,
    val showNotesSheet: Boolean,
    val notes: String,
    val showSummarySheet: Boolean,
    val isSummaryLoading: Boolean,
    val projectSummary: String?,
    val summaryError: String?,
    val showPatternPicker: Boolean,
    val projectId: Long?,
    val savedPatterns: List<com.finnvek.knittools.data.local.SavedPatternEntity>,
    val isPro: Boolean,
    val showPatternInfoSheet: Boolean,
    val linkedPattern: com.finnvek.knittools.data.local.SavedPatternEntity?,
)

data class CounterSheetActions(
    val onYarnSelect: (Long) -> Unit,
    val onShowYarnPickerFromManagement: () -> Unit,
    val onUnlinkYarn: (Long) -> Unit,
    val onYarnPickerDismiss: () -> Unit,
    val onYarnManagementDismiss: () -> Unit,
    val onNotesChange: (String) -> Unit,
    val onNotesDismiss: () -> Unit,
    val onNotesExpand: () -> Unit,
    val onSummaryDismiss: () -> Unit,
    val onPatternPickerDismiss: () -> Unit,
    val onPatternInfoDismiss: () -> Unit,
    val onPatternFileSelected: (String, String) -> Unit,
    val onSavedPatternSelected: (com.finnvek.knittools.data.local.SavedPatternEntity) -> Unit,
)

@Composable
private fun CounterScreenSheets(
    state: CounterSheetState,
    actions: CounterSheetActions,
) {
    if (state.showYarnPicker) {
        YarnPickerSheet(
            savedYarnCards = state.savedYarnCards,
            onSelect = actions.onYarnSelect,
            onDismiss = actions.onYarnPickerDismiss,
        )
    }
    if (state.showYarnManagementSheet) {
        YarnManagementSheet(
            linkedYarns = state.linkedYarns,
            onUnlinkYarn = actions.onUnlinkYarn,
            onAddYarn = actions.onShowYarnPickerFromManagement,
            onDismiss = actions.onYarnManagementDismiss,
        )
    }
    if (state.showNotesSheet) {
        NotesSheet(
            notes = state.notes,
            onNotesChange = actions.onNotesChange,
            onDismiss = actions.onNotesDismiss,
            onExpandToFullScreen = actions.onNotesExpand,
        )
    }
    if (state.showSummarySheet) {
        SummarySheet(
            isLoading = state.isSummaryLoading,
            summary = state.projectSummary,
            errorMessage = state.summaryError,
            onDismiss = actions.onSummaryDismiss,
        )
    }
    if (state.showPatternPicker) {
        PatternPickerSheet(
            projectId = state.projectId,
            savedPatterns = state.savedPatterns,
            isPro = state.isPro,
            onSavedPatternSelected = actions.onSavedPatternSelected,
            onDocumentSelected = actions.onPatternFileSelected,
            onDismiss = actions.onPatternPickerDismiss,
        )
    }
    if (state.showPatternInfoSheet && state.linkedPattern != null) {
        PatternInfoSheet(
            pattern = state.linkedPattern,
            onDismiss = actions.onPatternInfoDismiss,
        )
    }
}

// Data-luokka CounterTopBar-callbackien ryhmittelyyn (S107)
data class CounterTopBarActions(
    val onBack: () -> Unit,
    val onPhotoGallery: () -> Unit,
    val onShowPattern: () -> Unit,
    val onShowProjectActions: () -> Unit,
)

data class CounterTopBarMicState(
    val isVoiceListening: Boolean,
    val isContinuousMode: Boolean = false,
    val isLiveSessionActive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterTopBar(
    isPro: Boolean,
    showPatternIcon: Boolean,
    micState: CounterTopBarMicState,
    onMicClick: () -> Unit,
    actions: CounterTopBarActions,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (showPatternIcon) {
                IconButton(onClick = actions.onShowPattern) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = stringResource(R.string.show_pattern),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isPro || BuildConfig.DEBUG) {
                IconButton(onClick = actions.onPhotoGallery) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(R.string.progress_photos),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onMicClick) {
                    CounterTopBarMicAction(
                        isVoiceListening = micState.isVoiceListening,
                        isContinuousMode = micState.isContinuousMode,
                        isLiveSessionActive = micState.isLiveSessionActive,
                    )
                }
            }
            IconButton(onClick = actions.onShowProjectActions) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.project_actions_title),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
    )
}

@Composable
private fun CounterTopBarMicAction(
    isVoiceListening: Boolean,
    isContinuousMode: Boolean,
    isLiveSessionActive: Boolean,
) {
    val isActive = isLiveSessionActive || isContinuousMode
    if (isActive) {
        ActiveCounterMicIcon(isLiveSessionActive = isLiveSessionActive)
        return
    }

    Icon(
        imageVector = Icons.Filled.Mic,
        contentDescription = stringResource(R.string.voice_commands),
        tint =
            if (isVoiceListening) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
    )
}

@Composable
private fun ActiveCounterMicIcon(isLiveSessionActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "mic_bg",
    )
    val activeColor =
        if (isLiveSessionActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(
                        activeColor.copy(alpha = bgAlpha),
                        CircleShape,
                    ),
        )
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.voice_commands),
            tint = activeColor,
        )
    }
}

@Composable
private fun rememberVibrator(): Vibrator? {
    val context = LocalView.current.context
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
}

data class ProjectHeaderActions(
    val onNameChange: (String) -> Unit,
    val onEditingNameChange: (Boolean) -> Unit,
    val onShowPatternInfo: () -> Unit,
    val onShowPatternPicker: () -> Unit,
    val onOpenPattern: () -> Unit,
)

data class ProjectCountersSectionActions(
    val onAddCounter: () -> Unit,
    val onIncrementCounter: (ProjectCounterEntity) -> Unit,
    val onDecrementCounter: (ProjectCounterEntity) -> Unit,
    val onRenameCounter: (Long, String) -> Unit,
    val onResetCounter: (Long) -> Unit,
    val onDeleteCounter: (Long) -> Unit,
    val performHaptic: () -> Unit,
)

private data class CounterDialogActionDependencies(
    val viewModel: CounterViewModel,
    val projectId: Long?,
    val renameText: String,
    val onRenameTextChange: (String) -> Unit,
    val onBack: () -> Unit,
    val onHideAddReminder: () -> Unit,
    val onHideAddCounter: () -> Unit,
    val onHideResetDialog: () -> Unit,
    val onHideCompleteDialog: () -> Unit,
    val onHideDeleteDialog: () -> Unit,
    val onHideRenameDialog: () -> Unit,
    val onHideStitchDialog: () -> Unit,
)

private data class CounterTopBarActionDependencies(
    val onBack: () -> Unit,
    val onPhotoGallery: () -> Unit,
    val projectId: Long?,
    val onPatternViewer: (Long) -> Unit,
    val onShowProjectActions: () -> Unit,
)

private data class ProjectHeaderActionDependencies(
    val viewModel: CounterViewModel,
    val projectId: Long?,
    val onPatternViewer: (Long) -> Unit,
    val onShowPatternInfo: () -> Unit,
    val onShowPatternPicker: () -> Unit,
    val onEditingNameChange: (Boolean) -> Unit,
)

private data class CounterMainContentActions(
    val onSurfaceIncrement: () -> Unit,
    val onDecrement: () -> Unit,
    val onIncrement: () -> Unit,
    val onUndo: () -> Unit,
    val onOpenPatternViewer: (Long) -> Unit,
    val onDecrementSecondary: () -> Unit,
    val onIncrementSecondary: () -> Unit,
    val onDecrementStitch: () -> Unit,
    val onIncrementStitch: () -> Unit,
    val onShowTargetDialog: () -> Unit,
)

@Composable
private fun CounterScreenContent(
    scaffoldPadding: PaddingValues,
    state: CounterUiState,
    isEditingName: Boolean,
    projectHeaderActions: ProjectHeaderActions,
    actions: CounterMainContentActions,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .clipToBounds(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProjectHeader(
                state = state,
                isEditingName = isEditingName,
                actions = projectHeaderActions,
            )
            // Compact pattern-repeat -rivi on otsikon alapuolella; alla oleva
            // weight-Box pitää laskurin keskellä riippumatta tämän näkyvyydestä.
            if (state.isPro) {
                Spacer(modifier = Modifier.height(8.dp))
                CompactPatternRepeatRow(
                    count = state.secondaryCount,
                    onDecrement = actions.onDecrementSecondary,
                    onIncrement = actions.onIncrementSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = actions.onSurfaceIncrement),
                ) {
                    CounterRowLabel(state = state, onShowTargetDialog = actions.onShowTargetDialog)
                    CounterMainNumber(state = state)
                    CounterTargetProgressBar(state = state, onShowTargetDialog = actions.onShowTargetDialog)
                }
            }
            CounterButtons(
                onDecrement = actions.onDecrement,
                onIncrement = actions.onIncrement,
                onUndo = actions.onUndo,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CounterRowLabel(
    state: CounterUiState,
    onShowTargetDialog: () -> Unit,
) {
    val labelText =
        if (state.targetRows != null) {
            stringResource(R.string.row_label_with_target, state.counter.count, state.targetRows)
        } else {
            stringResource(R.string.current_row)
        }
    Text(
        text = labelText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onShowTargetDialog),
    )
}

@Composable
private fun CounterMainNumber(state: CounterUiState) {
    val counterFontSize = (115f / LocalDensity.current.fontScale).sp
    val counterDescription =
        if (state.targetRows != null && state.targetRows > 0) {
            val percent =
                ((state.counter.count.toFloat() / state.targetRows.toFloat()) * 100)
                    .toInt()
                    .coerceIn(0, 100)
            stringResource(R.string.voice_row_of_target, state.counter.count, state.targetRows, percent)
        } else {
            stringResource(R.string.current_row_short, state.counter.count)
        }
    RollingCounter(
        count = state.counter.count,
        textStyle =
            MaterialTheme.typography.displayMedium.copy(
                fontSize = counterFontSize,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        contentDescription = counterDescription,
    )
}

@Composable
private fun CounterTargetProgressBar(
    state: CounterUiState,
    onShowTargetDialog: () -> Unit,
) {
    val target = state.targetRows ?: return
    if (target <= 0) return
    Spacer(modifier = Modifier.height(12.dp))
    val fraction = (state.counter.count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val completed = state.counter.count >= target
    val fillColor =
        if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable(onClick = onShowTargetDialog),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(2.dp)
                    .background(fillColor),
        )
    }
}

@Composable
private fun ProjectHeader(
    state: CounterUiState,
    isEditingName: Boolean,
    actions: ProjectHeaderActions,
) {
    if (isEditingName) {
        TextField(
            value = state.projectName,
            onValueChange = actions.onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.default_project_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            trailingIcon = {
                IconButton(onClick = { actions.onEditingNameChange(false) }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.save),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    } else {
        Text(
            text = state.projectName.ifEmpty { stringResource(R.string.default_project_name) }.uppercase(),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 0.5.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { actions.onEditingNameChange(true) }),
        )
        Spacer(modifier = Modifier.height(6.dp))
        PatternHeaderRow(
            attachedPatternName = state.patternName,
            linkedPatternName = state.linkedPattern?.name,
            onShowPatternPicker = actions.onShowPatternPicker,
            onOpenPattern = actions.onOpenPattern,
            onShowPatternInfo = actions.onShowPatternInfo,
        )
    }
}

@Composable
private fun PatternHeaderRow(
    attachedPatternName: String?,
    linkedPatternName: String?,
    onShowPatternPicker: () -> Unit,
    onOpenPattern: () -> Unit,
    onShowPatternInfo: () -> Unit,
) {
    when {
        !attachedPatternName.isNullOrBlank() -> {
            Text(
                text = attachedPatternName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPattern)
                        .padding(vertical = 2.dp),
            )
        }

        !linkedPatternName.isNullOrBlank() -> {
            Text(
                text = "$linkedPatternName · Ravelry",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowPatternInfo)
                        .padding(vertical = 2.dp),
            )
        }

        else -> {
            Text(
                text = stringResource(R.string.attach_pattern),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier =
                    Modifier
                        .clickable(onClick = onShowPatternPicker)
                        .padding(vertical = 2.dp),
            )
        }
    }
}


@Composable
private fun CounterButtons(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Image(
            painter = painterResource(R.drawable.plus_button),
            contentDescription = stringResource(R.string.counter_increase),
            modifier =
                Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onIncrement),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onUndo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.counter_undo),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactPatternRepeatRow(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.counter_repeat_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.counter_decrease),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.counter_increase),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountersListSheet(
    projectCounters: List<ProjectCounterEntity>,
    mainRowCount: Int,
    actions: ProjectCountersSectionActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.counters_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (projectCounters.isEmpty()) {
                Text(
                    text = stringResource(R.string.voice_counters_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                projectCounters.forEach { counter ->
                    if (counter.counterType == "REPEAT_SECTION") {
                        RepeatSectionItem(
                            counter = counter,
                            mainRowCount = mainRowCount,
                            onDelete = { actions.onDeleteCounter(counter.id) },
                        )
                    } else {
                        CounterListItem(
                            counter = counter,
                            actions = CounterItemActions(
                                onIncrement = { actions.onIncrementCounter(counter) },
                                onDecrement = { actions.onDecrementCounter(counter) },
                                onRename = { actions.onRenameCounter(counter.id, it) },
                                onReset = { actions.onResetCounter(counter.id) },
                                onDelete = { actions.onDeleteCounter(counter.id) },
                                performHaptic = actions.performHaptic,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YarnPickerSheet(
    savedYarnCards: List<YarnCardEntity>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.select_yarn_card).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (savedYarnCards.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_saved_yarns),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedYarnCards, key = { it.id }) { card ->
                        YarnPickerItem(card = card, onSelect = { onSelect(card.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun YarnPickerItem(
    card: YarnCardEntity,
    onSelect: () -> Unit,
) {
    val name =
        listOfNotNull(
            card.brand.takeIf { it.isNotBlank() },
            card.yarnName.takeIf { it.isNotBlank() },
        ).joinToString(" ").ifEmpty { "Yarn #${card.id}" }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onSelect)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            if (card.colorName.isNotBlank()) {
                Text(
                    text = card.colorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (card.weightCategory.isNotBlank()) {
            Text(
                text = card.weightCategory,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternInfoSheet(
    pattern: com.finnvek.knittools.data.local.SavedPatternEntity,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (pattern.designerName.isNotBlank()) {
                Text(
                    text = pattern.designerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            pattern.needleSize?.let {
                PatternDetailRow(label = stringResource(R.string.needle_size_label), value = it)
            }
            pattern.yarnWeight?.let {
                PatternDetailRow(label = stringResource(R.string.filter_weight), value = it)
            }
            pattern.yardage?.let {
                PatternDetailRow(
                    label = "Yardage",
                    value = stringResource(R.string.yardage_format, it),
                )
            }
            pattern.gaugeRows?.let {
                PatternDetailRow(label = stringResource(R.string.gauge_label), value = "$it rows")
            }
            pattern.difficulty?.let {
                PatternDetailRow(
                    label = stringResource(R.string.filter_difficulty),
                    value = stringResource(R.string.difficulty_format, it),
                )
            }
        }
    }
}

@Composable
private fun PatternDetailRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onExpandToFullScreen: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.notes).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        onDismiss()
                        onExpandToFullScreen()
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInFull,
                        contentDescription = stringResource(R.string.open_notes_editor),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.add_note)) },
                minLines = 6,
                shape = MaterialTheme.shapes.large,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummarySheet(
    isLoading: Boolean,
    summary: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FilterVintage,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.project_summary),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.generating_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SummarySheetResult(summary = summary, errorMessage = errorMessage)
            }
        }
    }
}

@Composable
private fun SummarySheetResult(
    summary: String?,
    errorMessage: String?,
) {
    if (!summary.isNullOrBlank()) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        )
    }
    if (!errorMessage.isNullOrBlank()) {
        if (!summary.isNullOrBlank()) Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeepScreenAwake(enabled: Boolean) {
    if (!enabled) return
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun hasAudioPermission(context: android.content.Context): Boolean =
    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun yarnColor(yarnId: Long): androidx.compose.ui.graphics.Color = YarnColors[(yarnId % YarnColors.size).toInt()]

@Composable
private fun rememberCounterDialogActions(dependencies: CounterDialogActionDependencies): CounterDialogActions =
    remember(
        dependencies,
    ) {
        CounterDialogActions(
            onAddReminderSave = { targetRow, repeatInterval, message ->
                dependencies.viewModel.addReminder(targetRow, repeatInterval, message)
                dependencies.onHideAddReminder()
            },
            onAddReminderDismiss = dependencies.onHideAddReminder,
            onAddCounterSave = { draft ->
                dependencies.viewModel.addProjectCounter(draft)
                dependencies.onHideAddCounter()
            },
            onAddCounterDismiss = dependencies.onHideAddCounter,
            onResetConfirm = {
                dependencies.viewModel.reset()
                dependencies.onHideResetDialog()
            },
            onResetDismiss = dependencies.onHideResetDialog,
            onCompleteConfirm = {
                dependencies.viewModel.completeProject()
                dependencies.onHideCompleteDialog()
                dependencies.onBack()
            },
            onCompleteDismiss = dependencies.onHideCompleteDialog,
            onDeleteConfirm = {
                dependencies.projectId?.let { dependencies.viewModel.deleteProject(it) }
                dependencies.onHideDeleteDialog()
                dependencies.onBack()
            },
            onDeleteDismiss = dependencies.onHideDeleteDialog,
            onRenameTextChange = dependencies.onRenameTextChange,
            onRenameConfirm = {
                dependencies.viewModel.setProjectName(dependencies.renameText.trim())
                dependencies.onHideRenameDialog()
            },
            onRenameDismiss = dependencies.onHideRenameDialog,
            onStitchConfirm = dependencies.viewModel::setStitchCount,
            onStitchDismiss = dependencies.onHideStitchDialog,
        )
    }

@Composable
@Suppress("kotlin:S107") // Sheet-toiminnot pidetään eksplisiittisinä, jotta kutsupuolen tila pysyy näkyvänä
private fun rememberCounterSheetActions(
    viewModel: CounterViewModel,
    onShowYarnPicker: () -> Unit,
    onHideYarnPicker: () -> Unit,
    onHideYarnManagementSheet: () -> Unit,
    onHideNotesSheet: () -> Unit,
    onExpandNotes: () -> Unit,
    onHideSummarySheet: () -> Unit,
    onHidePatternPicker: () -> Unit,
    onHidePatternInfoSheet: () -> Unit,
): CounterSheetActions =
    remember(
        viewModel,
        onShowYarnPicker,
        onHideYarnPicker,
        onHideYarnManagementSheet,
        onHideNotesSheet,
        onExpandNotes,
        onHideSummarySheet,
        onHidePatternPicker,
        onHidePatternInfoSheet,
    ) {
        CounterSheetActions(
            onYarnSelect = {
                viewModel.linkYarnCard(it)
                onHideYarnPicker()
            },
            onShowYarnPickerFromManagement = {
                onHideYarnManagementSheet()
                onShowYarnPicker()
            },
            onUnlinkYarn = viewModel::unlinkYarnCard,
            onYarnPickerDismiss = onHideYarnPicker,
            onYarnManagementDismiss = onHideYarnManagementSheet,
            onNotesChange = viewModel::setNotes,
            onNotesDismiss = onHideNotesSheet,
            onNotesExpand = onExpandNotes,
            onSummaryDismiss = {
                onHideSummarySheet()
                viewModel.clearSummary()
            },
            onPatternPickerDismiss = onHidePatternPicker,
            onPatternInfoDismiss = onHidePatternInfoSheet,
            onPatternFileSelected = viewModel::attachPattern,
            onSavedPatternSelected = { pattern ->
                viewModel.attachPattern(pattern.patternUrl, pattern.name)
            },
        )
    }

@Composable
private fun rememberCounterTopBarActions(dependencies: CounterTopBarActionDependencies): CounterTopBarActions =
    remember(dependencies) {
        CounterTopBarActions(
            onBack = dependencies.onBack,
            onPhotoGallery = dependencies.onPhotoGallery,
            onShowPattern = { dependencies.projectId?.let(dependencies.onPatternViewer) },
            onShowProjectActions = dependencies.onShowProjectActions,
        )
    }

@Composable
private fun rememberProjectHeaderActions(dependencies: ProjectHeaderActionDependencies): ProjectHeaderActions =
    remember(dependencies) {
        ProjectHeaderActions(
            onNameChange = dependencies.viewModel::setProjectName,
            onEditingNameChange = dependencies.onEditingNameChange,
            onShowPatternInfo = dependencies.onShowPatternInfo,
            onShowPatternPicker = dependencies.onShowPatternPicker,
            onOpenPattern = { dependencies.projectId?.let(dependencies.onPatternViewer) },
        )
    }

@Composable
private fun rememberProjectCountersSectionActions(
    viewModel: CounterViewModel,
    performHaptic: () -> Unit,
    onShowAddCounter: () -> Unit,
): ProjectCountersSectionActions =
    remember(viewModel, performHaptic, onShowAddCounter) {
        ProjectCountersSectionActions(
            onAddCounter = onShowAddCounter,
            onIncrementCounter = { counter ->
                performHaptic()
                viewModel.incrementProjectCounter(counter)
            },
            onDecrementCounter = { counter ->
                performHaptic()
                viewModel.decrementProjectCounter(counter)
            },
            onRenameCounter = viewModel::renameProjectCounter,
            onResetCounter = viewModel::resetProjectCounter,
            onDeleteCounter = viewModel::deleteProjectCounter,
            performHaptic = performHaptic,
        )
    }
