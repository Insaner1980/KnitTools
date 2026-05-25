package com.finnvek.knittools.ui.screens.counter

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.RenameProjectDialog
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ViewModelForwarding")
fun CounterScreen(
    onBack: () -> Unit = {},
    onSessionHistory: (Long) -> Unit = {},
    onPhotoGallery: () -> Unit = {},
    onPatternViewer: (Long) -> Unit = {},
    onNotesEditor: (Long) -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
    var showRemindersSheet by rememberSaveable { mutableStateOf(false) }
    var showAddReminder by rememberSaveable { mutableStateOf(false) }
    var editingReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddCounter by rememberSaveable { mutableStateOf(false) }
    var showStitchDialog by rememberSaveable { mutableStateOf(false) }
    var showPatternInfoSheet by rememberSaveable { mutableStateOf(false) }
    var showPatternPicker by rememberSaveable { mutableStateOf(false) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var previousOverlayProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    val savedYarnCards by viewModel.savedYarnCards.collectAsStateWithLifecycle()
    val savedPatterns by viewModel.savedPatterns.collectAsStateWithLifecycle()

    fun hideProjectScopedOverlays() {
        showResetDialog = false
        showProjectActionsSheet = false
        showCountersListSheet = false
        showCompleteDialog = false
        showDeleteDialog = false
        showRenameDialog = false
        renameText = ""
        isEditingName = false
        showNotesSheet = false
        showYarnPicker = false
        showYarnManagementSheet = false
        showRemindersSheet = false
        showAddReminder = false
        editingReminderId = null
        showAddCounter = false
        showStitchDialog = false
        showPatternInfoSheet = false
        showPatternPicker = false
        showTargetDialog = false
    }

    LaunchedEffect(state.projectId) {
        val previousProjectId = previousOverlayProjectId
        if (previousProjectId != null && previousProjectId != state.projectId) {
            hideProjectScopedOverlays()
        }
        previousOverlayProjectId = state.projectId
    }

    val openProUpgrade = {
        showProjectActionsSheet = false
        showCountersListSheet = false
        onUpgradeToPro()
    }
    val requestPhotoGallery = {
        requestCounterFeature(
            hasAccess = state.canUseProgressPhotos || BuildConfig.DEBUG,
            onOpenFeature = onPhotoGallery,
            onOpenUpgrade = openProUpgrade,
        )
    }
    val requestAddCounter = {
        requestCounterFeature(
            hasAccess = state.canUseMultipleCounters,
            onOpenFeature = { showAddCounter = true },
            onOpenUpgrade = openProUpgrade,
        )
    }
    val requestRowReminders = {
        requestCounterFeature(
            hasAccess = state.canUseRowReminders,
            onOpenFeature = { showRemindersSheet = true },
            onOpenUpgrade = openProUpgrade,
        )
    }
    val requestNotes = {
        if (state.isPro) {
            showNotesSheet = true
        } else {
            openProUpgrade()
        }
    }

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
            onHidePatternPicker = { showPatternPicker = false },
            onHidePatternInfoSheet = { showPatternInfoSheet = false },
        )
    val projectCountersActions =
        rememberProjectCountersSectionActions(
            viewModel = viewModel,
            performHaptic = performHaptic,
            onShowAddCounter = requestAddCounter,
        )
    val dialogActionDependencies =
        CounterDialogActionDependencies(
            viewModel = viewModel,
            projectId = state.projectId,
            editingReminderId = editingReminderId,
            renameText = renameText,
            onRenameTextChange = { renameText = it },
            onBack = onBack,
            onHideAddReminder = {
                showAddReminder = false
                editingReminderId = null
            },
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
        remember(
            viewModel,
            performHaptic,
            onPatternViewer,
            state.projectId,
            requestNotes,
            requestPhotoGallery,
            requestRowReminders,
            requestAddCounter,
        ) {
            CounterWorkspaceActions(
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
                onOpenPattern = { state.projectId?.let(onPatternViewer) },
                onShowPatternPicker = { showPatternPicker = true },
                onShowPatternInfo = { showPatternInfoSheet = true },
                onOpenNotes = requestNotes,
                onOpenYarn = { showYarnManagementSheet = true },
                onOpenPhotos = requestPhotoGallery,
                onOpenReminders = requestRowReminders,
                onOpenProjectActions = { showProjectActionsSheet = true },
                onShowAddCounter = requestAddCounter,
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
                onDismissReminder = viewModel::dismissReminder,
            )
        }
    val reminderBeingEdited =
        remember(editingReminderId, state.reminders) {
            state.reminders.find { it.id == editingReminderId }
        }

    CounterScreenDialogs(
        state =
            CounterDialogState(
                showAddReminder = showAddReminder,
                editingReminder = reminderBeingEdited,
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

    CounterTargetRowsDialogHost(
        showDialog = showTargetDialog,
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

    CounterScreenSheets(
        state =
            CounterSheetState(
                showYarnPicker = showYarnPicker,
                showYarnManagementSheet = showYarnManagementSheet,
                savedYarnCards = savedYarnCards,
                linkedYarns = state.linkedYarns,
                projectYarnNotes = state.projectYarnNotes,
                showNotesSheet = showNotesSheet,
                notes = state.notes,
                showPatternPicker = showPatternPicker,
                projectId = state.projectId,
                savedPatterns = savedPatterns,
                canUseCameraScan = state.canUsePatternCameraScan,
                showPatternInfoSheet = showPatternInfoSheet,
                linkedPattern = state.linkedPattern,
            ),
        actions = sheetActions,
    )

    CounterProjectActionsSheetHost(
        showSheet = showProjectActionsSheet,
        state =
            ProjectActionsSheetState(
                reminderCount = state.reminders.count { !it.isCompleted },
                projectCounterCount = state.projectCounters.size,
                stitchTrackingEnabled = state.stitchTrackingEnabled,
                stitchCount = state.stitchCount,
            ),
        callbacks =
            ProjectActionsSheetCallbacks(
                onDismiss = { showProjectActionsSheet = false },
                onOpenReminders = {
                    showProjectActionsSheet = false
                    requestRowReminders()
                },
                onOpenCountersList = {
                    showProjectActionsSheet = false
                    showCountersListSheet = true
                },
                onOpenAddCounter = {
                    showProjectActionsSheet = false
                    requestAddCounter()
                },
                onOpenStitchCount = {
                    showProjectActionsSheet = false
                    showStitchDialog = true
                },
                onToggleStitchTracking = { enabled ->
                    handleStitchTrackingToggle(
                        enabled = enabled,
                        stitchCount = state.stitchCount,
                        onRequestStitchCount = {
                            showProjectActionsSheet = false
                            showStitchDialog = true
                        },
                        onSetStitchTrackingEnabled = viewModel::setStitchTrackingEnabled,
                    )
                },
                onOpenSessionHistory = {
                    showProjectActionsSheet = false
                    viewModel.openSessionHistory(onSessionHistory)
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

    CounterCountersListSheetHost(
        showSheet = showCountersListSheet,
        projectCounters = state.projectCounters,
        mainRowCount = state.counter.count,
        actions = projectCountersActions,
        onDismiss = { showCountersListSheet = false },
    )

    if (showRemindersSheet) {
        RemindersSheet(
            reminders = state.reminders,
            currentRow = state.counter.count,
            onAdd = {
                showRemindersSheet = false
                editingReminderId = null
                showAddReminder = true
            },
            onEdit = { reminder ->
                showRemindersSheet = false
                editingReminderId = reminder.id
                showAddReminder = true
            },
            onDelete = { reminderId -> viewModel.deleteReminder(reminderId) },
            onDismiss = { showRemindersSheet = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CounterTopBar(
                actions = topBarActions,
            )
        },
    ) { scaffoldPadding ->
        CounterWorkspace(
            scaffoldPadding = scaffoldPadding,
            state = state,
            isEditingName = isEditingName,
            projectHeaderActions = projectHeaderActions,
            projectCountersActions = projectCountersActions,
            actions = mainContentActions,
        )
    }
}

@Composable
private fun CounterTargetRowsDialogHost(
    showDialog: Boolean,
    currentTarget: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
    onRemove: () -> Unit,
) {
    if (showDialog) {
        TargetRowsDialog(
            currentTarget = currentTarget,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun CounterProjectActionsSheetHost(
    showSheet: Boolean,
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    if (showSheet) {
        ProjectActionsBottomSheet(
            state = state,
            callbacks = callbacks,
        )
    }
}

@Composable
private fun CounterCountersListSheetHost(
    showSheet: Boolean,
    projectCounters: List<ProjectCounter>,
    mainRowCount: Int,
    actions: ProjectCountersSectionActions,
    onDismiss: () -> Unit,
) {
    if (showSheet) {
        CountersListSheet(
            projectCounters = projectCounters,
            mainRowCount = mainRowCount,
            actions = actions,
            onDismiss = onDismiss,
        )
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
    val editingReminder: RowReminder?,
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
            reminder = state.editingReminder,
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
        RenameProjectDialog(
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
    val savedYarnCards: List<YarnCard>,
    val linkedYarns: List<Pair<Long, String>>,
    val projectYarnNotes: List<ProjectYarnNote>,
    val showNotesSheet: Boolean,
    val notes: String,
    val showPatternPicker: Boolean,
    val projectId: Long?,
    val savedPatterns: List<SavedPattern>,
    val canUseCameraScan: Boolean,
    val showPatternInfoSheet: Boolean,
    val linkedPattern: SavedPattern?,
)

data class CounterSheetActions(
    val onYarnSelect: (Long) -> Unit,
    val onShowYarnPickerFromManagement: () -> Unit,
    val onUnlinkYarn: (Long) -> Unit,
    val onSaveProjectYarnNote: (String, String, Int, String) -> Unit,
    val onDeleteProjectYarnNote: (Long) -> Unit,
    val onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    val onYarnPickerDismiss: () -> Unit,
    val onYarnManagementDismiss: () -> Unit,
    val onNotesChange: (String) -> Unit,
    val onNotesDismiss: () -> Unit,
    val onNotesExpand: () -> Unit,
    val onPatternPickerDismiss: () -> Unit,
    val onPatternInfoDismiss: () -> Unit,
    val onPatternFileSelected: (String, String) -> Unit,
    val onSavedPatternSelected: (SavedPattern) -> Unit,
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
            projectYarnNotes = state.projectYarnNotes,
            actions =
                YarnManagementSheetActions(
                    onUnlinkYarn = actions.onUnlinkYarn,
                    onAddYarn = actions.onShowYarnPickerFromManagement,
                    onSaveProjectYarnNote = actions.onSaveProjectYarnNote,
                    onDeleteProjectYarnNote = actions.onDeleteProjectYarnNote,
                    onSaveProjectYarnNoteToMyYarn = actions.onSaveProjectYarnNoteToMyYarn,
                    onDismiss = actions.onYarnManagementDismiss,
                ),
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
    if (state.showPatternPicker) {
        PatternPickerSheet(
            projectId = state.projectId,
            savedPatterns = state.savedPatterns,
            canUseCameraScan = state.canUseCameraScan,
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
    val onShowProjectActions: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterTopBar(actions: CounterTopBarActions) {
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

private data class CounterDialogActionDependencies(
    val viewModel: CounterViewModel,
    val projectId: Long?,
    val editingReminderId: Long?,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountersListSheet(
    projectCounters: List<ProjectCounter>,
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
            modifier =
                Modifier
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
                    text = stringResource(R.string.counters_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                projectCounters.forEach { counter ->
                    key(counter.id) {
                        ProjectCounterWorkspaceItem(
                            counter = counter,
                            mainRowCount = mainRowCount,
                            actions = actions,
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
    savedYarnCards: List<YarnCard>,
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
    card: YarnCard,
    onSelect: () -> Unit,
) {
    val fallbackYarnName = stringResource(R.string.yarn_card_number_fallback, card.id)
    val name = card.displayName { fallbackYarnName }
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
    pattern: SavedPattern,
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
                    label = stringResource(R.string.pattern_detail_yardage),
                    value = stringResource(R.string.yardage_format, it),
                )
            }
            pattern.gaugeRows?.let {
                PatternDetailRow(
                    label = stringResource(R.string.gauge_label),
                    value = stringResource(R.string.rows_format, it),
                )
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

@Composable
private fun rememberCounterDialogActions(dependencies: CounterDialogActionDependencies): CounterDialogActions =
    remember(
        dependencies,
    ) {
        CounterDialogActions(
            onAddReminderSave = { targetRow, repeatInterval, message ->
                val editingReminderId = dependencies.editingReminderId
                if (editingReminderId != null) {
                    dependencies.viewModel.updateReminder(editingReminderId, targetRow, repeatInterval, message)
                } else {
                    dependencies.viewModel.addReminder(targetRow, repeatInterval, message)
                }
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
            onSaveProjectYarnNote = viewModel::saveProjectYarnNote,
            onDeleteProjectYarnNote = viewModel::deleteProjectYarnNote,
            onSaveProjectYarnNoteToMyYarn = viewModel::saveProjectYarnNoteToMyYarn,
            onYarnPickerDismiss = onHideYarnPicker,
            onYarnManagementDismiss = onHideYarnManagementSheet,
            onNotesChange = viewModel::setNotes,
            onNotesDismiss = onHideNotesSheet,
            onNotesExpand = onExpandNotes,
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
            onShowProjectActions = dependencies.onShowProjectActions,
        )
    }

@Composable
private fun rememberProjectHeaderActions(dependencies: ProjectHeaderActionDependencies): ProjectHeaderActions =
    remember(dependencies) {
        ProjectHeaderActions(
            onNameSave = dependencies.viewModel::setProjectName,
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
