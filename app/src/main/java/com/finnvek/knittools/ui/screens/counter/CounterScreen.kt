package com.finnvek.knittools.ui.screens.counter

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.lifecycle.repeatOnLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectDetailsDialog
import com.finnvek.knittools.ui.components.ProjectDetailsValues
import com.finnvek.knittools.ui.components.RenameProjectDialog
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.screens.pattern.PatternPickerMode
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentError
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentUiState
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentsSheet
import com.finnvek.knittools.ui.theme.CounterDimens
import kotlinx.coroutines.delay

private enum class PendingCounterProAction {
    OpenCounter,
    RetryCounter,
    OpenReminder,
    RetryReminder,
    SaveToMyYarn,
    IncrementSecondary,
    DecrementSecondary,
}

private fun ProjectDocumentMutationResult.toProjectDocumentError(): ProjectDocumentError? =
    when (this) {
        is ProjectDocumentMutationResult.Added,
        ProjectDocumentMutationResult.PrimaryChanged,
        ProjectDocumentMutationResult.Reordered,
        ProjectDocumentMutationResult.Renamed,
        ProjectDocumentMutationResult.Selected,
        is ProjectDocumentMutationResult.Removed,
        ProjectDocumentMutationResult.ViewerStateUpdated,
        -> null
        ProjectDocumentMutationResult.InvalidLabel -> ProjectDocumentError.INVALID_LABEL
        ProjectDocumentMutationResult.AlreadyAttached,
        ProjectDocumentMutationResult.DuplicateUri,
        ProjectDocumentMutationResult.DuplicateDocumentKey,
        -> ProjectDocumentError.DUPLICATE
        ProjectDocumentMutationResult.PdfUnavailable -> ProjectDocumentError.UNAVAILABLE
        ProjectDocumentMutationResult.MissingProject,
        ProjectDocumentMutationResult.MissingDocument,
        ProjectDocumentMutationResult.MissingSavedPattern,
        ProjectDocumentMutationResult.MetadataOnlyPattern,
        ProjectDocumentMutationResult.StaleAction,
        -> ProjectDocumentError.STALE_ACTION
        ProjectDocumentMutationResult.PersistenceFailure -> ProjectDocumentError.MUTATION_FAILURE
    }

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
fun CounterScreen(
    actions: CounterScreenActions = CounterScreenActions(),
    viewModelProvider: @Composable () -> CounterViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val onBack = actions.onBack
    val onSessionHistory = actions.onSessionHistory
    val onPhotoGallery = actions.onPhotoGallery
    val onPatternViewer = actions.onPatternViewer
    val onSavedPatternDetail = actions.onSavedPatternDetail
    val onImportFromRavelry = actions.onImportFromRavelry
    val onNotesEditor = actions.onNotesEditor
    val onUpgradeToPro = actions.onUpgradeToPro
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.projectClosedEvents.collect { onBack() }
    }

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
    var patternPickerMode by rememberSaveable { mutableStateOf(PatternPickerMode.INITIAL_PROJECT_PATTERN) }
    var showDocumentsSheet by rememberSaveable { mutableStateOf(false) }
    var projectDocumentError by remember { mutableStateOf<ProjectDocumentError?>(null) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var pendingProAction by rememberSaveable { mutableStateOf<PendingCounterProAction?>(null) }
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
        patternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN
        showDocumentsSheet = false
        projectDocumentError = null
        showTargetDialog = false
    }

    LaunchedEffect(state.projectId) {
        val previousProjectId = previousOverlayProjectId
        if (previousProjectId != null && previousProjectId != state.projectId) {
            hideProjectScopedOverlays()
        }
        previousOverlayProjectId = state.projectId
    }

    val requestPhotoGallery = {
        onPhotoGallery()
    }
    val requestAddCounter = {
        requestCounterFeature(
            hasAccess = state.canUseMultipleCounters,
            onOpenFeature = { showAddCounter = true },
            onOpenUpgrade = { pendingProAction = PendingCounterProAction.OpenCounter },
        )
    }
    val requestRowReminders = {
        showRemindersSheet = true
    }
    val requestNotes: () -> Unit = {
        if (state.canUseNotes) {
            showNotesSheet = true
        } else {
            state.projectId?.let(onNotesEditor)
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
    SessionPresentationTicker(
        sessionToken = state.activeSession?.sessionToken,
        onTick = viewModel::refreshSessionPresentationTime,
    )
    TriggerAlertHaptic(
        alertId = state.activeAlert?.id,
        hasActiveAlert = state.activeAlert != null,
        performHaptic = performHaptic,
    )
    val sheetActions =
        rememberCounterSheetActions(
            viewModelProvider = viewModelProvider,
            onShowYarnPicker = { showYarnPicker = true },
            onHideYarnPicker = { showYarnPicker = false },
            onHideYarnManagementSheet = { showYarnManagementSheet = false },
            onHideNotesSheet = { showNotesSheet = false },
            onExpandNotes = { state.projectId?.let(onNotesEditor) },
            onHidePatternPicker = { showPatternPicker = false },
            onImportFromRavelry = onImportFromRavelry,
            onSeePro = onUpgradeToPro,
            onSaveProjectYarnNoteToMyYarn = { noteId ->
                if (!viewModel.saveProjectYarnNoteToMyYarn(noteId)) {
                    pendingProAction = PendingCounterProAction.SaveToMyYarn
                }
            },
        )
    val projectCountersActions =
        rememberProjectCountersSectionActions(
            viewModelProvider = viewModelProvider,
            performHaptic = performHaptic,
            onShowAddCounter = requestAddCounter,
        )
    val dialogActionDependencies =
        CounterDialogActionDependencies(
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
            onReminderProRequired = { pendingProAction = PendingCounterProAction.RetryReminder },
            onCounterProRequired = { pendingProAction = PendingCounterProAction.RetryCounter },
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
            onEditingNameChange = { isEditingName = it },
        )
    val dialogActions = rememberCounterDialogActions(dialogActionDependencies, viewModelProvider)
    val topBarActions = rememberCounterTopBarActions(topBarActionDependencies)
    val projectHeaderActions = rememberProjectHeaderActions(projectHeaderActionDependencies, viewModelProvider)
    val mainContentActions =
        remember(
            viewModel,
            performHaptic,
            onPatternViewer,
            onSavedPatternDetail,
            state.projectId,
            state.linkedPattern?.id,
            state.primaryDocument?.id,
            state.projectDocumentAvailability,
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
                onShowDocuments = { showDocumentsSheet = true },
                onShowPatternPicker = {
                    patternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN
                    showPatternPicker = true
                },
                onOpenSavedPatternDetail = { state.linkedPattern?.id?.let(onSavedPatternDetail) },
                onOpenNotes = requestNotes,
                onOpenYarn = { showYarnManagementSheet = true },
                onOpenPhotos = requestPhotoGallery,
                onOpenReminders = requestRowReminders,
                onShowAddCounter = requestAddCounter,
                onDecrementSecondary = {
                    if (state.canUseSecondaryCounter) {
                        performHaptic()
                        viewModel.decrementSecondary()
                    } else {
                        pendingProAction = PendingCounterProAction.DecrementSecondary
                    }
                },
                onIncrementSecondary = {
                    if (state.canUseSecondaryCounter) {
                        performHaptic()
                        viewModel.incrementSecondary()
                    } else {
                        pendingProAction = PendingCounterProAction.IncrementSecondary
                    }
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
                onStopSession = viewModel::stopWorkSession,
                onResolveSessionRecovery = viewModel::showRecoveryPrompt,
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
                showNotesSheet = showNotesSheet,
                notes = state.notes,
                showPatternPicker = showPatternPicker,
                projectId = state.projectId,
                savedPatterns = savedPatterns,
                canUseCameraScan = state.canUsePatternCameraScan,
                proStatus = state.proStatus,
                hasExistingPattern =
                    patternPickerMode == PatternPickerMode.INITIAL_PROJECT_PATTERN &&
                        state.projectDocuments.isNotEmpty(),
                patternPickerMode = patternPickerMode,
                attachedSavedPatternIds = state.projectDocuments.mapNotNullTo(mutableSetOf()) { it.savedPatternId },
            ),
        actions = sheetActions,
    )

    if (showDocumentsSheet) {
        ProjectDocumentsSheet(
            state =
                ProjectDocumentUiState(
                    documents = state.projectDocuments,
                    selectedDocumentId = state.primaryDocument?.id,
                    availability = state.projectDocumentAvailability,
                    isLoading = false,
                    error = projectDocumentError,
                ),
            metadataPatternName =
                state.linkedPattern
                    ?.takeIf { pattern ->
                        state.projectDocuments.none { it.savedPatternId == pattern.id }
                    }?.name,
            onOpenPatternInformation = {
                showDocumentsSheet = false
                state.linkedPattern?.id?.let(onSavedPatternDetail)
            },
            onDismiss = { showDocumentsSheet = false },
            onSelect = { documentId ->
                viewModel.selectProjectDocument(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                    if (result == ProjectDocumentMutationResult.Selected) {
                        showDocumentsSheet = false
                        state.projectId?.let(onPatternViewer)
                    }
                }
            },
            onRename = { documentId, label ->
                viewModel.renameProjectDocument(documentId, label) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                }
            },
            onMoveEarlier = { documentId ->
                viewModel.moveProjectDocumentEarlier(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                }
            },
            onMoveLater = { documentId ->
                viewModel.moveProjectDocumentLater(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                }
            },
            onSetPrimary = { documentId ->
                viewModel.setPrimaryProjectDocument(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                }
            },
            onRemove = { documentId ->
                viewModel.removeProjectDocument(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                }
            },
            onAdd = {
                showDocumentsSheet = false
                patternPickerMode = PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT
                showPatternPicker = true
            },
            onClearError = { projectDocumentError = null },
        )
    }

    CounterProjectActionsSheetHost(
        showSheet = showProjectActionsSheet,
        state =
            ProjectActionsSheetState(
                reminderCount = state.reminders.count { !it.isCompleted },
                projectCounterCount = state.projectCounters.size,
                stitchTrackingEnabled = state.stitchTrackingEnabled,
                stitchCount = state.stitchCount,
                proStatus = state.proStatus,
                isWorkSessionActiveForProject = state.activeSession?.projectId == state.projectId,
            ),
        callbacks =
            ProjectActionsSheetCallbacks(
                onDismiss = { showProjectActionsSheet = false },
                onOpenDocuments = {
                    showProjectActionsSheet = false
                    showDocumentsSheet = true
                },
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
                onStartWorkSession = {
                    showProjectActionsSheet = false
                    viewModel.startWorkSession()
                },
                onStopWorkSession = {
                    showProjectActionsSheet = false
                    viewModel.stopWorkSession()
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
            proStatus = state.proStatus,
            onAdd = {
                showRemindersSheet = false
                editingReminderId = null
                if (state.canUseRowReminders) {
                    showAddReminder = true
                } else {
                    pendingProAction = PendingCounterProAction.OpenReminder
                }
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

    pendingProAction?.let { action ->
        val request =
            when (action) {
                PendingCounterProAction.OpenReminder,
                PendingCounterProAction.RetryReminder,
                ->
                    ProPromptRequest(
                        source = ProPromptSource.Reminders,
                    )
                PendingCounterProAction.SaveToMyYarn ->
                    ProPromptRequest(
                        source = ProPromptSource.SaveToMyYarn,
                    )
                else ->
                    ProPromptRequest(
                        source = ProPromptSource.Counters,
                    )
            }
        ProPromptSheet(
            request = request,
            onDismiss = { pendingProAction = null },
            onTrialStarted = {
                pendingProAction = null
                when (action) {
                    PendingCounterProAction.OpenCounter -> showAddCounter = true
                    PendingCounterProAction.RetryCounter -> {
                        if (viewModel.retryAddProjectCounter()) showAddCounter = false
                    }
                    PendingCounterProAction.OpenReminder -> showAddReminder = true
                    PendingCounterProAction.RetryReminder -> {
                        if (viewModel.retryAddReminder()) showAddReminder = false
                    }
                    PendingCounterProAction.SaveToMyYarn -> viewModel.retrySaveProjectYarnNoteToMyYarn()
                    PendingCounterProAction.IncrementSecondary -> {
                        performHaptic()
                        viewModel.incrementSecondary()
                    }
                    PendingCounterProAction.DecrementSecondary -> {
                        performHaptic()
                        viewModel.decrementSecondary()
                    }
                }
            },
            onSeePro = onUpgradeToPro,
        )
    }

    state.activeSession?.takeIf { state.showSessionRecoveryPrompt }?.let { activeSession ->
        val activeProjectName =
            state.projects.firstOrNull { it.id == activeSession.projectId }?.name
                ?: state.projectName
        SessionRecoveryDialog(
            projectName = activeProjectName,
            recoveryReason = activeSession.recoveryReason,
            recoveryIntervalToken = activeSession.recoveryIntervalToken,
            trustedDurationSeconds = activeSession.timingAnchors.checkpointedDurationSeconds,
            suggestedDurationSeconds = activeSession.recoverySuggestedDurationSeconds,
            pendingRowsWorked = activeSession.pendingRowsWorked,
            onAdd = viewModel::addRecoveryInterval,
            onDiscard = viewModel::discardRecoveryInterval,
            onEdit = viewModel::editRecoveryDurationAndStop,
            onDismiss = viewModel::dismissRecoveryPrompt,
        )
    }

    state.sessionStartConflict?.let { conflict ->
        val activeProjectName =
            state.projects.firstOrNull { it.id == conflict.activeSession.projectId }?.name
                ?: stringResource(R.string.work_session_another_project)
        SessionStartConflictDialog(
            activeProjectName = activeProjectName,
            onReturnToActive = viewModel::returnToActiveSessionProject,
            onSaveAndStart = { viewModel.resolveSessionStartConflict(saveCurrent = true) },
            onDiscardAndStart = { viewModel.resolveSessionStartConflict(saveCurrent = false) },
            onCancel = viewModel::cancelSessionStartConflict,
        )
    }

    if (state.pendingProjectCompletionSession != null) {
        ActiveSessionCompletionDialog(
            onSave = { viewModel.completeProjectWithActiveSession(saveSession = true) },
            onDiscard = { viewModel.completeProjectWithActiveSession(saveSession = false) },
            onCancel = viewModel::cancelProjectCompletionSessionChoice,
        )
    }

    if (state.pendingProjectDeletionSession != null) {
        ActiveSessionDeletionDialog(
            onDiscardAndDelete = viewModel::deleteProjectDiscardingActiveSession,
            onCancel = viewModel::cancelProjectDeletionSessionChoice,
        )
    }

    state.sessionStopSummary?.let { summary ->
        SessionStopSummaryDialog(
            projectName = summary.projectName,
            durationSeconds = summary.durationSeconds,
            rowsWorked = summary.rowsWorked,
            onSave = viewModel::saveStoppedWorkSession,
            onDiscard = viewModel::discardStoppedWorkSession,
            onCancel = viewModel::cancelSessionStop,
        )
    }

    state.workSessionErrorRes?.let { messageRes ->
        WorkSessionErrorDialog(
            message = stringResource(messageRes),
            canRetry = state.workSessionErrorCanRetry,
            onRetry = viewModel::retryWorkSessionAction,
            onDismiss = viewModel::dismissWorkSessionError,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CounterTopBar(
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
private fun SessionPresentationTicker(
    sessionToken: String?,
    onTick: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, sessionToken) {
        if (sessionToken == null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                onTick()
                delay(1_000L)
            }
        }
    }
}

@Composable
internal fun SessionRecoveryDialog(
    projectName: String,
    recoveryReason: ActiveSessionRecoveryReason?,
    recoveryIntervalToken: String?,
    trustedDurationSeconds: Long,
    suggestedDurationSeconds: Long?,
    pendingRowsWorked: Int,
    onAdd: (Long) -> Unit,
    onDiscard: () -> Unit,
    onEdit: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggestedTotal = safeDurationSum(trustedDurationSeconds, suggestedDurationSeconds ?: 0L)
    var editing by rememberSaveable(recoveryIntervalToken) { mutableStateOf(false) }
    var hoursText by
        rememberSaveable(recoveryIntervalToken) {
            mutableStateOf((suggestedTotal / 3_600L).toString())
        }
    var minutesText by
        rememberSaveable(recoveryIntervalToken) {
            mutableStateOf(((suggestedTotal % 3_600L) / 60L).toString())
        }
    val durationSeconds = parseRecoveryDurationSeconds(hoursText, minutesText)
    val headingFocusRequester = remember { FocusRequester() }
    LaunchedEffect(recoveryIntervalToken) {
        headingFocusRequester.requestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.work_session_recovery_title),
                modifier =
                    Modifier
                        .semantics { heading() }
                        .focusRequester(headingFocusRequester)
                        .focusable(),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(projectName, style = MaterialTheme.typography.titleMedium)
                Text(recoveryReasonText(recoveryReason))
                Text(
                    stringResource(
                        R.string.work_session_confirmed_time_format,
                        formatWorkSessionDuration(trustedDurationSeconds),
                    ),
                )
                Text(
                    if (suggestedDurationSeconds != null) {
                        stringResource(
                            R.string.work_session_time_while_away_format,
                            formatWorkSessionDuration(suggestedDurationSeconds),
                        )
                    } else {
                        stringResource(R.string.work_session_time_unavailable)
                    },
                )
                if (pendingRowsWorked > 0) {
                    Text(stringResource(R.string.work_session_pending_rows_format, pendingRowsWorked))
                }
                if (editing) {
                    Text(stringResource(R.string.work_session_edit_total_duration))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = hoursText,
                            onValueChange = { hoursText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.work_session_hours)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = durationSeconds == null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.work_session_minutes)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = durationSeconds == null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (durationSeconds == null) {
                        Text(
                            text = stringResource(R.string.work_session_invalid_duration),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (editing) {
                    TextButton(
                        enabled = durationSeconds != null,
                        onClick = { durationSeconds?.let(onEdit) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.work_session_save_edited_and_stop))
                    }
                } else {
                    TextButton(
                        enabled = suggestedDurationSeconds != null,
                        onClick = { suggestedDurationSeconds?.let(onAdd) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.work_session_add_and_continue))
                    }
                    TextButton(
                        onClick = { editing = true },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.work_session_edit_duration))
                    }
                }
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.work_session_discard_pending))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun SessionStartConflictDialog(
    activeProjectName: String,
    onReturnToActive: () -> Unit,
    onSaveAndStart: () -> Unit,
    onDiscardAndStart: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_conflict_title)) },
        text = { Text(stringResource(R.string.work_session_conflict_body, activeProjectName)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onReturnToActive, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_return_to_active))
                }
                TextButton(onClick = onSaveAndStart, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_save_then_start))
                }
                TextButton(onClick = onDiscardAndStart, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_discard_then_start))
                }
                TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun ActiveSessionCompletionDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_complete_project_title)) },
        text = { Text(stringResource(R.string.work_session_complete_project_body)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onSave, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_save_and_complete))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_discard_and_complete))
                }
                TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun ActiveSessionDeletionDialog(
    onDiscardAndDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_delete_project_title)) },
        text = { Text(stringResource(R.string.work_session_delete_project_body)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onDiscardAndDelete,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.work_session_discard_and_delete))
                }
                TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun SessionStopSummaryDialog(
    projectName: String,
    durationSeconds: Long,
    rowsWorked: Int,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_stop_summary_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(projectName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.work_session_duration_format, formatWorkSessionDuration(durationSeconds)))
                Text(stringResource(R.string.work_session_rows_format, rowsWorked))
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onSave, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_save))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_discard))
                }
                TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun WorkSessionErrorDialog(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = if (canRetry) onRetry else onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (canRetry) {
                            R.string.work_session_retry
                        } else {
                            R.string.ok
                        },
                    ),
                )
            }
        },
        dismissButton =
            if (canRetry) {
                {
                    TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            } else {
                null
            },
    )
}

@Composable
private fun recoveryReasonText(reason: ActiveSessionRecoveryReason?): String =
    stringResource(
        when (reason) {
            ActiveSessionRecoveryReason.REBOOTED -> R.string.work_session_reason_restarted
            ActiveSessionRecoveryReason.LONG_RUNNING -> R.string.work_session_reason_long_running
            ActiveSessionRecoveryReason.BOOT_IDENTITY_UNAVAILABLE -> R.string.work_session_reason_clock_unavailable
            ActiveSessionRecoveryReason.INVALID_ANCHORS,
            null,
            -> R.string.work_session_reason_clock_changed
        },
    )

internal fun safeDurationSum(
    first: Long,
    second: Long,
): Long =
    when {
        first < 0L || second < 0L -> 0L
        Long.MAX_VALUE - first < second -> Long.MAX_VALUE
        else -> first + second
    }

internal fun parseRecoveryDurationSeconds(
    hoursText: String,
    minutesText: String,
): Long? {
    val hours = hoursText.toLongOrNull() ?: return null
    val minutes = minutesText.toLongOrNull() ?: return null
    if (hours < 0L || minutes !in 0L..59L || hours > Long.MAX_VALUE / 3_600L) return null
    val hourSeconds = hours * 3_600L
    val minuteSeconds = minutes * 60L
    if (Long.MAX_VALUE - hourSeconds < minuteSeconds) return null
    return hourSeconds + minuteSeconds
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
    val proStatus: com.finnvek.knittools.pro.ProStatus,
    val hasExistingPattern: Boolean,
    val patternPickerMode: PatternPickerMode,
    val attachedSavedPatternIds: Set<Long>,
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
    val onSeePro: () -> Unit,
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
            proStatus = state.proStatus,
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
            proStatus = state.proStatus,
            hasExistingPattern = state.hasExistingPattern,
            mode = state.patternPickerMode,
            excludedSavedPatternIds = state.attachedSavedPatternIds,
            onSavedPatternSelected = actions.onSavedPatternSelected,
            onDocumentSelected = actions.onPatternFileSelected,
            onImportFromRavelry = actions.onImportFromRavelry,
            onSeePro = actions.onSeePro,
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
) {
    TopAppBar(
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
                scrolledContainerColor = Color.Transparent,
            ),
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
    val onReminderProRequired: () -> Unit,
    val onCounterProRequired: () -> Unit,
)

private data class CounterTopBarActionDependencies(
    val onBack: () -> Unit,
    val onShowProjectActions: () -> Unit,
)

private data class ProjectHeaderActionDependencies(
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
// CPD-OFF: Ruudun paikallinen valintaikkuna pidetaan kayttokohteen yhteydessa.
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
            // CPD-ON
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
private fun rememberCounterDialogActions(
    dependencies: CounterDialogActionDependencies,
    viewModelProvider: @Composable () -> CounterViewModel,
): CounterDialogActions {
    val viewModel = viewModelProvider()
    return remember(dependencies, viewModel) {
        CounterDialogActions(
            onAddReminderSave = { targetRow, repeatInterval, message ->
                val editingReminderId = dependencies.editingReminderId
                if (editingReminderId != null) {
                    viewModel.updateReminder(editingReminderId, targetRow, repeatInterval, message)
                    dependencies.onHideAddReminder()
                } else {
                    val saved = viewModel.addReminder(targetRow, repeatInterval, message)
                    if (saved) {
                        dependencies.onHideAddReminder()
                    } else {
                        dependencies.onReminderProRequired()
                    }
                }
            },
            onAddReminderDismiss = dependencies.onHideAddReminder,
            onAddCounterSave = { draft ->
                if (viewModel.addProjectCounter(draft)) {
                    dependencies.onHideAddCounter()
                } else {
                    dependencies.onCounterProRequired()
                }
            },
            onAddCounterDismiss = dependencies.onHideAddCounter,
            onResetConfirm = {
                viewModel.reset()
                dependencies.onHideResetDialog()
            },
            onResetDismiss = dependencies.onHideResetDialog,
            onCompleteConfirm = {
                viewModel.completeProject()
                dependencies.onHideCompleteDialog()
            },
            onCompleteDismiss = dependencies.onHideCompleteDialog,
            onDeleteConfirm = {
                dependencies.projectId?.let { viewModel.deleteProject(it) }
                dependencies.onHideDeleteDialog()
            },
            onDeleteDismiss = dependencies.onHideDeleteDialog,
            onRenameTextChange = dependencies.onRenameTextChange,
            onRenameConfirm = {
                viewModel.setProjectName(dependencies.renameText.trim())
                dependencies.onHideRenameDialog()
            },
            onRenameDismiss = dependencies.onHideRenameDialog,
            onStitchConfirm = viewModel::setStitchCount,
            onStitchDismiss = dependencies.onHideStitchDialog,
        )
    }
}

@Composable
@Suppress("kotlin:S107") // Sheet-toiminnot pidetään eksplisiittisinä, jotta kutsupuolen tila pysyy näkyvänä
private fun rememberCounterSheetActions(
    viewModelProvider: @Composable () -> CounterViewModel,
    onShowYarnPicker: () -> Unit,
    onHideYarnPicker: () -> Unit,
    onHideYarnManagementSheet: () -> Unit,
    onHideNotesSheet: () -> Unit,
    onExpandNotes: () -> Unit,
    onHidePatternPicker: () -> Unit,
    onImportFromRavelry: () -> Unit,
    onSeePro: () -> Unit,
    onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
): CounterSheetActions {
    val viewModel = viewModelProvider()
    return remember(
        viewModel,
        onShowYarnPicker,
        onHideYarnPicker,
        onHideYarnManagementSheet,
        onHideNotesSheet,
        onExpandNotes,
        onHidePatternPicker,
        onImportFromRavelry,
        onSeePro,
        onSaveProjectYarnNoteToMyYarn,
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
            onSaveProjectYarnNoteToMyYarn = onSaveProjectYarnNoteToMyYarn,
            onYarnPickerDismiss = onHideYarnPicker,
            onYarnManagementDismiss = onHideYarnManagementSheet,
            onNotesChange = viewModel::setNotes,
            onNotesDismiss = onHideNotesSheet,
            onNotesExpand = onExpandNotes,
            onPatternPickerDismiss = onHidePatternPicker,
            onPatternFileSelected = { uri, name -> viewModel.attachPattern(uri, name) },
            onSavedPatternSelected = viewModel::attachSavedPattern,
            onImportFromRavelry = onImportFromRavelry,
            onSeePro = onSeePro,
        )
    }
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
private fun rememberProjectHeaderActions(
    dependencies: ProjectHeaderActionDependencies,
    viewModelProvider: @Composable () -> CounterViewModel,
): ProjectHeaderActions {
    val viewModel = viewModelProvider()
    return remember(dependencies, viewModel) {
        ProjectHeaderActions(
            onNameSave = viewModel::setProjectName,
            onEditingNameChange = dependencies.onEditingNameChange,
        )
    }
}

@Composable
private fun rememberProjectCountersSectionActions(
    viewModelProvider: @Composable () -> CounterViewModel,
    performHaptic: () -> Unit,
    onShowAddCounter: () -> Unit,
): ProjectCountersSectionActions {
    val viewModel = viewModelProvider()
    return remember(viewModel, performHaptic, onShowAddCounter) {
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
}
