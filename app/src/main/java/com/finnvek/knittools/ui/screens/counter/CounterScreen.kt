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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ProjectDetailsDialog
import com.finnvek.knittools.ui.components.ProjectDetailsValues
import com.finnvek.knittools.ui.components.RenameProjectDialog
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.scrolledTopBarDivider
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

data class CounterScreenActions(
    val onBack: () -> Unit = {},
    val onSessionHistory: (Long) -> Unit = {},
    val onPhotoGallery: () -> Unit = {},
    val onPatternViewer: (Long) -> Unit = {},
    val onSavedPatternDetail: (Long) -> Unit = {},
    val onImportFromRavelry: () -> Unit = {},
    val onNotesEditor: (Long) -> Unit = {},
    val onUpgradeToPro: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ViewModelForwarding")
fun CounterScreen(
    actions: CounterScreenActions = CounterScreenActions(),
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val onBack = actions.onBack
    val onSessionHistory = actions.onSessionHistory
    val onPhotoGallery = actions.onPhotoGallery
    val onPatternViewer = actions.onPatternViewer
    val onSavedPatternDetail = actions.onSavedPatternDetail
    val onImportFromRavelry = actions.onImportFromRavelry
    val onNotesEditor = actions.onNotesEditor
    val onUpgradeToPro = actions.onUpgradeToPro
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showProjectActionsSheet by rememberSaveable { mutableStateOf(false) }
    var showCountersListSheet by rememberSaveable { mutableStateOf(false) }
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showProjectDetailsDialog by rememberSaveable { mutableStateOf(false) }
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
        showProjectDetailsDialog = false
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
            hasAccess = state.canUseProgressPhotos,
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
        if (state.canUseNotes) {
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

    KeepScreenAwake(enabled = state.keepScreenAwake, projectId = state.projectId)
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
            onImportFromRavelry = onImportFromRavelry,
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
            onSavedPatternDetail,
            state.projectId,
            state.linkedPattern?.id,
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
                onOpenSavedPatternDetail = { state.linkedPattern?.id?.let(onSavedPatternDetail) },
                onOpenNotes = requestNotes,
                onOpenYarn = { showYarnManagementSheet = true },
                onOpenPhotos = requestPhotoGallery,
                onOpenReminders = requestRowReminders,
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

    if (showProjectDetailsDialog) {
        ProjectDetailsDialog(
            title = stringResource(R.string.project_details_title),
            confirmText = stringResource(R.string.save),
            initialValues =
                ProjectDetailsValues(
                    name = state.projectName,
                    craftType = state.craftType,
                    mainCounterLabelType = state.mainCounterLabelType,
                    mainCounterCustomLabel = state.mainCounterCustomLabel,
                ),
            onConfirm = { values ->
                viewModel.setProjectDetails(
                    values.name,
                    values.craftType,
                    values.mainCounterLabelType,
                    values.mainCounterCustomLabel,
                )
                showProjectDetailsDialog = false
            },
            onDismiss = { showProjectDetailsDialog = false },
        )
    }

    CounterScreenSheets(
        state =
            CounterSheetState(
                showYarnPicker = showYarnPicker,
                showYarnManagementSheet = showYarnManagementSheet,
                savedYarnCards = savedYarnCards,
                linkedYarns = state.linkedYarns,
                projectYarnNotes = state.projectYarnNotes,
                canSaveToMyYarn = state.canUseYarnCards,
                showNotesSheet = showNotesSheet,
                notes = state.notes,
                showPatternPicker = showPatternPicker,
                projectId = state.projectId,
                savedPatterns = savedPatterns,
                canUseCameraScan = state.canUsePatternCameraScan,
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
                onOpenProjectDetails = {
                    showProjectActionsSheet = false
                    showProjectDetailsDialog = true
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
        showSheet = showCountersListSheet && state.canUseMultipleCounters,
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
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CounterTopBar(
                scrollBehavior = topBarScrollBehavior,
                state = state,
                isEditingName = isEditingName,
                projectHeaderActions = projectHeaderActions,
                actions = topBarActions,
            )
        },
    ) { scaffoldPadding ->
        CounterWorkspace(
            scaffoldPadding = scaffoldPadding,
            state = state,
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
        containerColor = MaterialTheme.knitToolsColors.modalContainer,
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
    val canSaveToMyYarn: Boolean,
    val showNotesSheet: Boolean,
    val notes: String,
    val showPatternPicker: Boolean,
    val projectId: Long?,
    val savedPatterns: List<SavedPattern>,
    val canUseCameraScan: Boolean,
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
    val onPatternFileSelected: (String, String) -> Unit,
    val onSavedPatternSelected: (SavedPattern) -> Unit,
    val onImportFromRavelry: () -> Unit,
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
            canSaveToMyYarn = state.canSaveToMyYarn,
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
            onImportFromRavelry = actions.onImportFromRavelry,
            onDismiss = actions.onPatternPickerDismiss,
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
private fun CounterTopBar(
    state: CounterUiState,
    isEditingName: Boolean,
    projectHeaderActions: ProjectHeaderActions,
    actions: CounterTopBarActions,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        modifier = Modifier.scrolledTopBarDivider(scrollBehavior),
        title = {
            CounterTopBarTitle(
                state = state,
                isEditingName = isEditingName,
                actions = projectHeaderActions,
            )
        },
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
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CounterTopBarTitle(
    state: CounterUiState,
    isEditingName: Boolean,
    actions: ProjectHeaderActions,
) {
    var draftName by rememberSaveable(state.projectId) { mutableStateOf(state.projectName) }

    LaunchedEffect(isEditingName, state.projectName) {
        if (!isEditingName) {
            draftName = state.projectName
        }
    }

    if (isEditingName) {
        TextField(
            value = draftName,
            onValueChange = { draftName = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = CounterDimens.TopBarTitleEndPadding),
            placeholder = { Text(stringResource(R.string.default_project_name)) },
            singleLine = true,
            shape = RoundedCornerShape(CounterDimens.TopBarTextFieldCornerRadius),
            keyboardOptions = KeyboardOptions.Default,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        actions.onNameSave(draftName.trim())
                        actions.onEditingNameChange(false)
                    },
                    enabled = draftName.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.save),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    } else {
        Text(
            text = state.projectName.ifEmpty { stringResource(R.string.default_project_name) }.localizedUppercase(),
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = CounterDimens.TopBarTitleEndPadding)
                    .clickable(onClick = { actions.onEditingNameChange(true) }),
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
        containerColor = MaterialTheme.knitToolsColors.modalContainer,
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
        containerColor = MaterialTheme.knitToolsColors.modalContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.select_yarn_card).localizedUppercase(),
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
private fun NotesSheet(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onExpandToFullScreen: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.knitToolsColors.modalContainer,
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
                    text = stringResource(R.string.notes).localizedUppercase(),
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
private fun KeepScreenAwake(
    enabled: Boolean,
    projectId: Long?,
) {
    if (!enabled) return
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(view, lifecycleOwner, projectId) {
        val window = (view.context as? android.app.Activity)?.window
        val lifecycle = lifecycleOwner.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START ->
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Lifecycle.Event.ON_STOP ->
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            lifecycle.removeObserver(observer)
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
    onImportFromRavelry: () -> Unit,
): CounterSheetActions =
    remember(
        viewModel,
        onShowYarnPicker,
        onHideYarnPicker,
        onHideYarnManagementSheet,
        onHideNotesSheet,
        onExpandNotes,
        onHidePatternPicker,
        onImportFromRavelry,
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
            onPatternFileSelected = viewModel::attachPattern,
            onSavedPatternSelected = viewModel::attachSavedPattern,
            onImportFromRavelry = onImportFromRavelry,
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
        )
    }
