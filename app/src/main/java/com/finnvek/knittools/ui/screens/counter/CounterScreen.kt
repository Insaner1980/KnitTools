package com.finnvek.knittools.ui.screens.counter

import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.widget.CounterWidgetPinHost
import com.finnvek.knittools.data.widget.requestCounterWidgetPin
import com.finnvek.knittools.domain.calculator.MainCounterLabelSlot
import com.finnvek.knittools.domain.calculator.ProjectSetupItem
import com.finnvek.knittools.domain.calculator.saturatingAdd
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectDetailsSheet
import com.finnvek.knittools.ui.components.ProjectDetailsValues
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.YarnCardPickerSheet
import com.finnvek.knittools.ui.components.cardTextFieldColors
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.components.workSessionDurationText
import com.finnvek.knittools.ui.findActivity
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.screens.pattern.PatternPickerMode
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentError
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentUiState
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentsSheet
import com.finnvek.knittools.ui.screens.project.MoveProjectToFolderSheet
import com.finnvek.knittools.ui.screens.project.ProjectOverviewContentActions
import com.finnvek.knittools.ui.screens.project.ProjectOverviewRouteContent
import com.finnvek.knittools.ui.screens.project.ProjectSetupRoute
import com.finnvek.knittools.ui.theme.CounterDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class PendingCounterProAction {
    OpenCounter,
    RetryCounter,
    OpenReminder,
    RetryReminder,
    SaveToMyYarn,
    OpenSecondary,
    PinWidget,
    IncrementSecondary,
    DecrementSecondary,
}

private const val CONTENT_ERROR_BLOCK_MILLIS = 3_500L

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
    val onProjectClosed: () -> Unit = onBack,
    val onProjectOverview: (Long) -> Unit = {},
    val onOpenCounter: () -> Unit = {},
    val onSessionHistory: (Long) -> Unit = {},
    val onCounterHistory: (Long) -> Unit = {},
    val onPhotoGallery: () -> Unit = {},
    val onPatternViewer: (Long, Long?) -> Unit = { _, _ -> },
    val onSavedPatternDetail: (Long) -> Unit = {},
    val onEditWebPattern: (Long) -> Unit = {},
    val onImportFromRavelry: () -> Unit = {},
    val onAddWebPattern: (Long) -> Unit = {},
    val onNotesEditor: (Long) -> Unit = {},
    val onMeasurements: (Long) -> Unit = {},
    val onUpgradeToPro: () -> Unit = {},
    val onOpenCounterWithWorkSessionActions: (Long) -> Unit = {},
    val onGuide: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776") // Reitti kokoaa counterin tilat ja dialogit yhteen Compose-omistajaan.
fun ProjectContentHost(
    actions: CounterScreenActions,
    viewModelProvider: @Composable () -> CounterViewModel,
    overview: Boolean,
    journeyRequest: CounterLaunchRequest? = null,
    onJourneyRequestHandled: (String) -> Unit = {},
    setupVisitGeneration: Long? = null,
    openWorkSessionActions: Boolean = false,
    onWorkSessionActionsOpened: () -> Unit = {},
) {
    val viewModel = viewModelProvider()
    val onBack = actions.onBack
    val onPhotoGallery = actions.onPhotoGallery
    val onPatternViewer = actions.onPatternViewer
    val onSavedPatternDetail = actions.onSavedPatternDetail
    val onEditWebPattern = actions.onEditWebPattern
    val onImportFromRavelry = actions.onImportFromRavelry
    val onAddWebPattern = actions.onAddWebPattern
    val onNotesEditor = actions.onNotesEditor
    val onUpgradeToPro = actions.onUpgradeToPro
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val patternImportInProgress by viewModel.patternImportInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val yarnSnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    val contentErrorTimeoutMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            CONTENT_ERROR_BLOCK_MILLIS,
            containsText = true,
        ) ?: CONTENT_ERROR_BLOCK_MILLIS
    val widgetPinHost = remember(context) { CounterWidgetPinHost(context) }
    val widgetAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.WIDGET)
    val counterAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.MULTIPLE_COUNTERS)
    val notesAccess =
        proActionAccess(state.proState, state.proAccessReady, ProFeature.NOTES, alreadyUnlocked = state.notesCreated)
    val pinWidget = {
        coroutineScope.launch {
            if (!requestCounterWidgetPin(widgetPinHost)) {
                snackbarHostState.showSnackbar(resources.getString(R.string.widget_add_manual))
            }
        }
        Unit
    }
    var overviewMenuOpen by remember(state.projectId) { mutableStateOf(false) }
    var setupMenuOpen by remember(state.projectId) { mutableStateOf(false) }
    var blockingCounterIds by remember(state.projectId) { mutableStateOf(emptySet<Long>()) }
    var contentErrorAt by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(contentErrorAt, contentErrorTimeoutMillis) {
        if (contentErrorAt != null) {
            delay(contentErrorTimeoutMillis)
            contentErrorAt = null
        }
    }

    fun showWebPatternSaveFailed() {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(resources.getString(R.string.web_pattern_save_failed))
        }
    }

    CollectWithLifecycleEffect({ viewModel.projectClosedEvents }) { selectionVersion ->
        viewModel.consumeProjectClosedEvent(selectionVersion, actions.onProjectClosed)
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showProjectActionsSheet by rememberSaveable { mutableStateOf(false) }
    var focusWorkSession by rememberSaveable { mutableStateOf(false) }
    var showMoveFolderSheet by rememberSaveable { mutableStateOf(false) }
    var showCountersListSheet by rememberSaveable { mutableStateOf(false) }
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showProjectDetailsSheet by rememberSaveable { mutableStateOf(false) }
    var showNotesSheet by rememberSaveable { mutableStateOf(false) }
    var showYarnPicker by rememberSaveable { mutableStateOf(false) }
    var showYarnManagementSheet by rememberSaveable { mutableStateOf(false) }
    var showRemindersSheet by rememberSaveable { mutableStateOf(false) }
    var showAddReminder by rememberSaveable { mutableStateOf(false) }
    var editingReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddCounter by rememberSaveable { mutableStateOf(false) }
    var addCounterInitialType by rememberSaveable { mutableStateOf(ProjectCounterType.COUNT_UP) }
    var showStitchDialog by rememberSaveable { mutableStateOf(false) }
    var enableStitchTrackingOnConfirm by rememberSaveable { mutableStateOf(false) }
    var showPatternPicker by rememberSaveable { mutableStateOf(false) }
    var initialCameraRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var patternPickerMode by rememberSaveable { mutableStateOf(PatternPickerMode.INITIAL_PROJECT_PATTERN) }
    var pendingWebPatternId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingWebPatternName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExistingWebPatternId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Korvausvahvistus koskee projektia, jossa ohjetta liitettiin, ei vahvistushetken projektia.
    var pendingWebPatternProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    val currentProjectId by rememberUpdatedState(state.projectId)
    var showDocumentsSheet by rememberSaveable { mutableStateOf(false) }
    var projectDocumentError by remember { mutableStateOf<ProjectDocumentError?>(null) }
    var showStepSizeSheet by rememberSaveable(state.projectId) { mutableStateOf(false) }
    var stepDelta by remember(state.projectId) { mutableStateOf<Int?>(null) }
    val stepLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, state.projectId, stepLifecycleOwner) {
        stepLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                viewModel.stepFeedback.collectLatest { feedback ->
                    if (feedback.projectId == state.projectId) {
                        stepDelta = feedback.delta
                        try {
                            delay(CounterDimens.STEP_FEEDBACK_DURATION_MILLIS)
                        } finally {
                            stepDelta = null
                        }
                    }
                }
            } finally {
                stepDelta = null
            }
        }
    }
    var targetDialogProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    val targetProjectId = targetDialogProjectId?.takeIf { it == state.projectId }
    var pendingProAction by rememberSaveable { mutableStateOf<PendingCounterProAction?>(null) }
    var pendingSecondaryProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    val requestWidgetPin = {
        when (widgetAccess) {
            ProActionAccess.Allowed, ProActionAccess.Trial -> pinWidget()
            ProActionAccess.Locked -> pendingProAction = PendingCounterProAction.PinWidget
            ProActionAccess.Unknown -> Unit
        }
    }
    var previousOverlayProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var projectActionTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val savedYarnCards by viewModel.savedYarnCards.collectAsStateWithLifecycle()
    val savedPatterns by viewModel.savedPatterns.collectAsStateWithLifecycle()

    fun hideProjectScopedOverlays() {
        showResetDialog = false
        showProjectActionsSheet = false
        showMoveFolderSheet = false
        showCountersListSheet = false
        showCompleteDialog = false
        showDeleteDialog = false
        showProjectDetailsSheet = false
        showNotesSheet = false
        showYarnPicker = false
        showYarnManagementSheet = false
        showRemindersSheet = false
        showAddReminder = false
        editingReminderId = null
        showAddCounter = false
        addCounterInitialType = ProjectCounterType.COUNT_UP
        showStitchDialog = false
        enableStitchTrackingOnConfirm = false
        showPatternPicker = false
        initialCameraRequestId = null
        patternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN
        pendingWebPatternId = null
        pendingWebPatternName = null
        pendingExistingWebPatternId = null
        pendingWebPatternProjectId = null
        showDocumentsSheet = false
        projectDocumentError = null
        targetDialogProjectId = null
        projectActionTargetId = null
        pendingProAction = null
        pendingSecondaryProjectId = null
    }

    LaunchedEffect(state.projectId, state.isRestoringProject) {
        if (state.isRestoringProject) return@LaunchedEffect
        val previousProjectId = previousOverlayProjectId
        if (previousProjectId != null && previousProjectId != state.projectId) {
            hideProjectScopedOverlays()
        }
        previousOverlayProjectId = state.projectId
    }

    LaunchedEffect(openWorkSessionActions, state.projectId, state.startupReady) {
        if (openWorkSessionActions && state.startupReady) {
            if (!state.isCompleted) {
                projectActionTargetId = state.projectId
                focusWorkSession = true
                showProjectActionsSheet = true
            }
            onWorkSessionActionsOpened()
        }
    }

    val requestPhotoGallery = {
        onPhotoGallery()
    }
    val requestAddCounter = {
        addCounterInitialType = ProjectCounterType.COUNT_UP
        projectActionTargetId = state.projectId
        if (counterAccess != ProActionAccess.Unknown || state.canUseMultipleCounters) {
            requestCounterFeature(
                hasAccess = counterAccess != ProActionAccess.Locked,
                onOpenFeature = { showAddCounter = true },
                onOpenUpgrade = { pendingProAction = PendingCounterProAction.OpenCounter },
            )
        }
    }
    val requestRowReminders = {
        showRemindersSheet = true
    }
    val requestNotes: () -> Unit = {
        when (notesAccess) {
            ProActionAccess.Allowed, ProActionAccess.Trial -> showNotesSheet = true
            ProActionAccess.Locked -> state.projectId?.let(onNotesEditor)
            ProActionAccess.Unknown -> if (state.canUseNotes) showNotesSheet = true
        }
    }

    val hapticFeedback = LocalHapticFeedback.current

    val performHaptic =
        remember(hapticFeedback) {
            {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

    if (!overview) {
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
    }
    val sheetActions =
        rememberCounterSheetActions(
            viewModelProvider = viewModelProvider,
            onShowYarnPicker = { showYarnPicker = true },
            onHideYarnPicker = { showYarnPicker = false },
            onHideYarnManagementSheet = { showYarnManagementSheet = false },
            onHideNotesSheet = { showNotesSheet = false },
            onExpandNotes = { state.projectId?.let(onNotesEditor) },
            onHidePatternPicker = {
                showPatternPicker = false
                initialCameraRequestId = null
            },
            onWebPatternReplacementRequired = { pattern, existingPatternId, requestProjectId ->
                // Projekti vaihtui pyynnön aikana: vahvistusta ei avata uuden projektin päälle.
                if (requestProjectId == currentProjectId) {
                    showPatternPicker = false
                    pendingWebPatternId = pattern.id
                    pendingWebPatternName = pattern.name
                    pendingExistingWebPatternId = existingPatternId
                    pendingWebPatternProjectId = requestProjectId
                }
            },
            onPatternMetadataAttachFailed = { showWebPatternSaveFailed() },
            onPatternImportResult = { error ->
                projectDocumentError = error
                if (error != null) {
                    showPatternPicker = false
                    showDocumentsSheet = true
                }
            },
            onImportFromRavelry = onImportFromRavelry,
            onAddWebPattern = { state.projectId?.let(onAddWebPattern) },
            onSeePro = onUpgradeToPro,
            onSaveProjectYarnNoteToMyYarn = { noteId ->
                if (!viewModel.saveProjectYarnNoteToMyYarn(noteId)) {
                    pendingProAction = PendingCounterProAction.SaveToMyYarn
                }
            },
        )

    fun clearPendingWebPatternReplacement() {
        pendingWebPatternId = null
        pendingWebPatternName = null
        pendingExistingWebPatternId = null
        pendingWebPatternProjectId = null
    }

    val replacementPatternId = pendingWebPatternId
    val expectedExistingPatternId = pendingExistingWebPatternId
    val replacementProjectId = pendingWebPatternProjectId
    if (replacementPatternId != null && expectedExistingPatternId != null && replacementProjectId != null) {
        ConfirmationDialog(
            title = stringResource(R.string.web_pattern_replace_confirm_title),
            message =
                stringResource(
                    R.string.web_pattern_replace_confirm_message,
                    pendingWebPatternName.orEmpty(),
                ),
            confirmText = stringResource(R.string.web_pattern_attach),
            isDestructive = true,
            onConfirm = {
                viewModel.attachSavedPatternMetadata(
                    savedPatternId = replacementPatternId,
                    expectedExistingSavedPatternId = expectedExistingPatternId,
                    projectId = replacementProjectId,
                ) { result ->
                    clearPendingWebPatternReplacement()
                    if (
                        result != SavedPatternMetadataMutationResult.Attached(replacementPatternId) &&
                        result != SavedPatternMetadataMutationResult.AlreadyAttached(replacementPatternId)
                    ) {
                        showWebPatternSaveFailed()
                    }
                }
            },
            onDismiss = { clearPendingWebPatternReplacement() },
        )
    }
    val projectCountersActions =
        rememberProjectCountersSectionActions(
            viewModelProvider = viewModelProvider,
            performHaptic = performHaptic,
            onShowAddCounter = requestAddCounter,
        )
    val dialogActionDependencies =
        CounterDialogActionDependencies(
            projectId = projectActionTargetId,
            enableStitchTrackingOnConfirm = enableStitchTrackingOnConfirm,
            editingReminderId = editingReminderId,
            onBack = onBack,
            onHideAddReminder = {
                showAddReminder = false
                editingReminderId = null
            },
            onHideAddCounter = { showAddCounter = false },
            onHideResetDialog = { showResetDialog = false },
            onHideCompleteDialog = { showCompleteDialog = false },
            onHideDeleteDialog = { showDeleteDialog = false },
            onHideStitchDialog = {
                showStitchDialog = false
                enableStitchTrackingOnConfirm = false
            },
            onReminderProRequired = { pendingProAction = PendingCounterProAction.RetryReminder },
            onCounterProRequired = { pendingProAction = PendingCounterProAction.RetryCounter },
        )
    val topBarActionDependencies =
        CounterTopBarActionDependencies(
            onBack = onBack,
            onShowProjectActions = {
                state.projectId?.let { projectId ->
                    projectActionTargetId = projectId
                    focusWorkSession = false
                    showProjectActionsSheet = true
                }
            },
            onOpenOverview = { state.projectId?.let(actions.onProjectOverview) },
        )
    val dialogActions = rememberCounterDialogActions(dialogActionDependencies, viewModelProvider)
    val topBarActions = rememberCounterTopBarActions(topBarActionDependencies)
    val requestProjectCompletion: () -> Unit = {
        val project = viewModel.uiState.value
        if (project.projectId != null && project.projectId == state.projectId && !project.isCompleted) {
            projectActionTargetId = project.projectId
            showCompleteDialog = true
        }
    }
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
            requestProjectCompletion,
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
                onOpenPattern = { state.projectId?.let { onPatternViewer(it, state.primaryDocument?.id) } },
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
                        pendingSecondaryProjectId = state.projectId
                        pendingProAction = PendingCounterProAction.DecrementSecondary
                    }
                },
                onIncrementSecondary = {
                    if (state.canUseSecondaryCounter) {
                        performHaptic()
                        viewModel.incrementSecondary()
                    } else {
                        pendingSecondaryProjectId = state.projectId
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
                onShowTargetDialog = { targetDialogProjectId = state.projectId },
                onMarkComplete = requestProjectCompletion,
                onShowStepSize = { showStepSizeSheet = true },
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
                reminderLabel = MainCounterLabelSlot(state.mainCounterLabelType, state.mainCounterCustomLabel),
                showAddCounter = showAddCounter && projectActionTargetId == state.projectId,
                initialCounterType = addCounterInitialType,
                showResetDialog = showResetDialog && projectActionTargetId == state.projectId,
                showCompleteDialog = showCompleteDialog && projectActionTargetId == state.projectId,
                showDeleteDialog = showDeleteDialog && projectActionTargetId == state.projectId,
                showStitchDialog = showStitchDialog && projectActionTargetId == state.projectId,
                projectName = state.projectName,
                currentStitchCount = state.stitchCount,
                proState = state.proState,
                proAccessReady = state.proAccessReady,
            ),
        actions = dialogActions,
        onCounterTypeProRequired = { pendingProAction = PendingCounterProAction.OpenCounter },
    )

    if (showStepSizeSheet) {
        state.projectId?.let { projectId ->
            key(projectId) {
                MainCounterStepSheet(
                    stepSize = state.counter.stepSize,
                    labelType = state.mainCounterLabelType,
                    customLabel = state.mainCounterCustomLabel,
                    onSave = { size, onResult ->
                        viewModel.setStepSize(size, expectedProjectId = projectId, onResult = onResult)
                    },
                    onDismiss = { showStepSizeSheet = false },
                )
            }
        }
    }
    CounterTargetRowsDialogHost(
        showDialog = targetProjectId != null,
        currentTarget = state.targetRows,
        counterLabel = mainCounterLabelText(state.mainCounterLabelType, state.mainCounterCustomLabel),
        onDismiss = { targetDialogProjectId = null },
        onConfirm = { target ->
            viewModel.setTargetRows(target, expectedProjectId = targetProjectId)
            targetDialogProjectId = null
        },
        onRemove = {
            viewModel.setTargetRows(null, expectedProjectId = targetProjectId)
            targetDialogProjectId = null
        },
    )

    if (showProjectDetailsSheet && projectActionTargetId == state.projectId) {
        ProjectDetailsSheet(
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
                if (projectActionTargetId == viewModel.uiState.value.projectId) {
                    viewModel.setProjectDetails(
                        values.name,
                        values.craftType,
                        values.mainCounterLabelType,
                        values.mainCounterCustomLabel,
                    )
                }
                showProjectDetailsSheet = false
            },
            onDismiss = { showProjectDetailsSheet = false },
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
                initialCameraRequestId = initialCameraRequestId,
                projectId = state.projectId,
                isRestoringProject = state.isRestoringProject,
                savedPatterns = savedPatterns,
                canUseCameraScan = state.canUsePatternCameraScan,
                proState = state.proState,
                proAccessReady = state.proAccessReady,
                patternPickerMode = patternPickerMode,
                attachedSavedPatternIds = state.projectDocuments.mapNotNullTo(mutableSetOf()) { it.savedPatternId },
            ),
        actions = sheetActions,
        yarnSnackbarHostState = yarnSnackbarHostState,
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
            metadataPattern =
                state.linkedPattern
                    ?.takeIf { pattern ->
                        state.projectDocuments.none { it.savedPatternId == pattern.id }
                    },
            onOpenPatternInformation = {
                showDocumentsSheet = false
                state.linkedPattern?.id?.let(onSavedPatternDetail)
            },
            onOpenPatternWebsite = {
                val result =
                    state.linkedPattern
                        ?.webPatternUrlOrNull
                        ?.originalUrl
                        ?.let { openExternalWebLink(context, it) }
                        ?: ExternalWebLinkOpenResult.InvalidUrl
                val messageRes =
                    when (result) {
                        ExternalWebLinkOpenResult.Opened -> null

                        ExternalWebLinkOpenResult.NoBrowser -> R.string.web_pattern_no_browser

                        ExternalWebLinkOpenResult.InvalidUrl,
                        ExternalWebLinkOpenResult.Failed,
                        -> R.string.web_pattern_open_failed
                    }
                messageRes?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            resources.getString(it),
                        )
                    }
                }
            },
            onEditPatternInformation = {
                state.linkedPattern?.id?.let(onEditWebPattern)
            },
            onUnlinkPatternInformation = { expectedPatternId ->
                viewModel.unlinkSavedPatternMetadata(expectedPatternId) { result ->
                    if (
                        result != SavedPatternMetadataMutationResult.Unlinked &&
                        result != SavedPatternMetadataMutationResult.AlreadyUnlinked
                    ) {
                        showWebPatternSaveFailed()
                    }
                }
            },
            onDismiss = { showDocumentsSheet = false },
            onSelect = { documentId ->
                viewModel.selectProjectDocument(documentId) { result ->
                    projectDocumentError = result.toProjectDocumentError()
                    if (result == ProjectDocumentMutationResult.Selected) {
                        showDocumentsSheet = false
                        state.projectId?.let { onPatternViewer(it, documentId) }
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
                projectDocumentError = null
                patternPickerMode = PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT
                showPatternPicker = true
            },
            onClearError = { projectDocumentError = null },
        )
    }

    CounterProjectActionsSheetHost(
        showSheet = showProjectActionsSheet && projectActionTargetId == state.projectId,
        state =
            ProjectActionsSheetState(
                projectId = state.projectId ?: 0L,
                projectName = state.projectName,
                craftType = state.craftType,
                photoUri = state.latestPhotos.firstOrNull()?.photoUri,
                projectCounterCount = state.projectCounters.size,
                stitchTrackingEnabled = state.stitchTrackingEnabled,
                keepScreenAwake = state.keepScreenAwake,
                focusWorkSession = focusWorkSession,
                stitchCount = state.stitchCount,
                proState = state.proState,
                proAccessReady = state.proAccessReady,
                secondaryCounterUsed = state.secondaryCounterUsed,
                isWorkSessionActiveForProject = state.activeSession?.projectId == state.projectId,
                isCompleted = state.isCompleted,
            ),
        callbacks =
            ProjectActionsSheetCallbacks(
                onOpenOverview = {
                    showProjectActionsSheet = false
                    state.projectId?.let(actions.onProjectOverview)
                },
                onDismiss = {
                    showProjectActionsSheet = false
                    projectActionTargetId = null
                },
                onMeasurements = {
                    showProjectActionsSheet = false
                    state.projectId?.let(actions.onMeasurements)
                },
                onOpenCountersList = {
                    showProjectActionsSheet = false
                    showCountersListSheet = true
                },
                onOpenAddCounter = {
                    showProjectActionsSheet = false
                    requestAddCounter()
                },
                onOpenSecondary = {
                    showProjectActionsSheet = false
                    pendingProAction = PendingCounterProAction.OpenSecondary
                },
                onPinWidget = {
                    showProjectActionsSheet = false
                    requestWidgetPin()
                },
                onOpenStitchCount = {
                    showProjectActionsSheet = false
                    enableStitchTrackingOnConfirm = false
                    showStitchDialog = true
                },
                onToggleKeepScreenAwake = viewModel::setKeepScreenAwake,
                onToggleStitchTracking = { enabled ->
                    handleStitchTrackingToggle(
                        enabled = enabled,
                        stitchCount = state.stitchCount,
                        onRequestStitchCount = {
                            showProjectActionsSheet = false
                            enableStitchTrackingOnConfirm = true
                            showStitchDialog = true
                        },
                        onSetStitchTrackingEnabled = viewModel::setStitchTrackingEnabled,
                    )
                },
                onOpenCounterHistory = {
                    showProjectActionsSheet = false
                    state.projectId?.let(actions.onCounterHistory)
                },
                onStartWorkSession = {
                    showProjectActionsSheet = false
                    viewModel.startWorkSession()
                },
                onStopWorkSession = {
                    showProjectActionsSheet = false
                    viewModel.stopWorkSession()
                },
                onShowResetDialog = {
                    showProjectActionsSheet = false
                    showResetDialog = true
                },
            ),
    )

    state.projectId
        ?.takeIf { showMoveFolderSheet && projectActionTargetId == it }
        ?.let { projectId ->
            MoveProjectToFolderSheet(
                projectId = projectId,
                projectName = state.projectName,
                onMoved = {
                    showMoveFolderSheet = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            resources.getString(R.string.folder_project_moved),
                        )
                    }
                },
                onDismiss = { showMoveFolderSheet = false },
            )
        }

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
            label = MainCounterLabelSlot(state.mainCounterLabelType, state.mainCounterCustomLabel),
            proState = state.proState,
            proAccessReady = state.proAccessReady,
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

    state.reactivationPromptCount?.let { count ->
        ProPromptSheet(
            request = ProPromptRequest(ProPromptSource.ProjectReactivation, count),
            onDismiss = viewModel::dismissPendingReactivation,
            onTrialStarted = viewModel::retryPendingReactivation,
            onSeePro = onUpgradeToPro,
        )
    }

    val genericErrorUnknown = stringResource(R.string.generic_error_unknown)
    CollectWithLifecycleEffect({ viewModel.reactivationErrors }) {
        snackbarHostState.showSnackbar(genericErrorUnknown)
    }
    CollectWithLifecycleEffect({ viewModel.projectContentErrors }) {
        contentErrorAt = System.nanoTime()
        Toast.makeText(context, genericErrorUnknown, Toast.LENGTH_LONG).show()
    }
    val yarnSavedMessage = stringResource(R.string.saved_to_my_yarn_explanation)
    CollectWithLifecycleEffect({ viewModel.projectYarnSaved }) { projectId ->
        if (state.projectId == projectId) {
            val host = if (showYarnManagementSheet) yarnSnackbarHostState else snackbarHostState
            host.showSnackbar(yarnSavedMessage)
        }
    }
    LaunchedEffect(showYarnManagementSheet) {
        if (!showYarnManagementSheet) yarnSnackbarHostState.currentSnackbarData?.dismiss()
    }

    pendingProAction?.takeIf { previousOverlayProjectId == state.projectId }?.let { action ->
        val promptProjectId = state.projectId
        val request =
            when (action) {
                PendingCounterProAction.OpenReminder,
                PendingCounterProAction.RetryReminder,
                -> ProPromptRequest(source = ProPromptSource.Reminders)

                PendingCounterProAction.SaveToMyYarn -> ProPromptRequest(source = ProPromptSource.SaveToMyYarn)

                PendingCounterProAction.PinWidget -> ProPromptRequest(source = ProPromptSource.WidgetPin)

                else -> ProPromptRequest(source = ProPromptSource.Counters)
            }
        ProPromptSheet(
            request = request,
            onDismiss = {
                pendingProAction = null
                pendingSecondaryProjectId = null
            },
            onTrialStarted = {
                pendingProAction = null
                val secondaryProjectId = pendingSecondaryProjectId
                pendingSecondaryProjectId = null
                if (viewModel.uiState.value.projectId != promptProjectId) return@ProPromptSheet
                when (action) {
                    PendingCounterProAction.OpenCounter -> {
                        runForCurrentProject(dialogActionDependencies, viewModel) { showAddCounter = true }
                    }

                    PendingCounterProAction.RetryCounter -> {
                        runForCurrentProject(dialogActionDependencies, viewModel) {
                            if (retryAddProjectCounter()) showAddCounter = false
                        }
                    }

                    PendingCounterProAction.OpenReminder -> {
                        showAddReminder = true
                    }

                    PendingCounterProAction.RetryReminder -> {
                        if (viewModel.retryAddReminder()) showAddReminder = false
                    }

                    PendingCounterProAction.SaveToMyYarn -> {
                        viewModel.retrySaveProjectYarnNoteToMyYarn()
                    }

                    PendingCounterProAction.OpenSecondary -> {
                        Unit
                    }

                    PendingCounterProAction.PinWidget -> {
                        pinWidget()
                    }

                    PendingCounterProAction.IncrementSecondary -> {
                        if (secondaryProjectId != null && secondaryProjectId == viewModel.uiState.value.projectId) {
                            performHaptic()
                            viewModel.incrementSecondary()
                        }
                    }

                    PendingCounterProAction.DecrementSecondary -> {
                        if (secondaryProjectId != null && secondaryProjectId == viewModel.uiState.value.projectId) {
                            performHaptic()
                            viewModel.decrementSecondary()
                        }
                    }
                }
            },
            onSeePro = onUpgradeToPro,
        )
    }

    state.activeSession?.takeIf { state.showSessionRecoveryPrompt }?.let { activeSession ->
        val activeProjectName =
            state.projects.firstOrNull { it.id == activeSession.projectId }?.name
                ?: if (state.projectId == activeSession.projectId) {
                    state.projectName
                } else {
                    stringResource(R.string.work_session_another_project)
                }
        key(activeSession.sessionToken, activeSession.recoveryIntervalToken) {
            SessionRecoveryDialog(
                projectName = activeProjectName,
                recoveryReason = activeSession.recoveryReason,
                recoveryIntervalToken = activeSession.recoveryIntervalToken,
                trustedDurationSeconds = activeSession.timingAnchors.checkpointedDurationSeconds,
                suggestedDurationSeconds = activeSession.recoverySuggestedDurationSeconds,
                pendingRowsWorked = activeSession.pendingRowsWorked,
                onAdd = { viewModel.addRecoveryInterval(it, activeSession) },
                onDiscard = { viewModel.discardRecoveryInterval(activeSession) },
                onEdit = { viewModel.editRecoveryDurationAndStop(it, activeSession) },
                onDismiss = { viewModel.dismissRecoveryPrompt(activeSession) },
            )
        }
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

    val journeyActionBlocked =
        listOf(
            showResetDialog,
            showProjectActionsSheet,
            showMoveFolderSheet,
            showCountersListSheet,
            showCompleteDialog,
            showDeleteDialog,
            showProjectDetailsSheet,
            showNotesSheet,
            showYarnPicker,
            showYarnManagementSheet,
            showRemindersSheet,
            showAddReminder,
            editingReminderId != null,
            showAddCounter,
            showStitchDialog,
            showPatternPicker,
            pendingWebPatternId != null,
            showDocumentsSheet,
            projectDocumentError != null,
            patternImportInProgress,
            showStepSizeSheet,
            targetProjectId != null,
            pendingProAction != null,
            overviewMenuOpen,
            setupMenuOpen,
            blockingCounterIds.isNotEmpty(),
            contentErrorAt != null,
            snackbarHostState.currentSnackbarData != null,
            state.blocksJourneyAction(),
        ).any { it }
    CounterJourneyRequestEffect(
        request = journeyRequest,
        state = state,
        blocked = journeyActionBlocked,
        overview = overview,
        onHandled = onJourneyRequestHandled,
        onAction = { action ->
            when (action) {
                TrialJourneyAction.Widget -> {
                    requestWidgetPin()
                }

                TrialJourneyAction.RepeatCounter -> {
                    requestAddCounter()
                    addCounterInitialType = ProjectCounterType.REPEATING
                }

                TrialJourneyAction.PatternCamera -> {
                    initialCameraRequestId = journeyRequest?.requestId
                    patternPickerMode = PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT
                    showPatternPicker = true
                }

                else -> {
                    Unit
                }
            }
        },
    )
    if (overview) {
        fun openProjectContent(open: () -> Unit) {
            if (state.projectId == viewModel.uiState.value.projectId) {
                projectActionTargetId = state.projectId
                open()
            }
        }
        ProjectOverviewRouteContent(
            state = state,
            yarnCards = savedYarnCards,
            actions = actions,
            snackbarHostState = snackbarHostState,
            onBlockingSurfaceChanged = { overviewMenuOpen = it },
            projectSetup = {
                ProjectSetupRoute(
                    state = state,
                    viewModel = viewModel,
                    routeEntryGeneration = setupVisitGeneration,
                    onBlockingSurfaceChanged = { setupMenuOpen = it },
                    onItemClick = { item ->
                        openProjectContent {
                            when (item) {
                                ProjectSetupItem.TARGET -> {
                                    targetDialogProjectId = state.projectId
                                }

                                ProjectSetupItem.PATTERN -> {
                                    patternPickerMode = PatternPickerMode.INITIAL_PROJECT_PATTERN
                                    showPatternPicker = true
                                }

                                ProjectSetupItem.YARN -> {
                                    showYarnManagementSheet = true
                                }

                                ProjectSetupItem.WORK_TIME -> {
                                    state.projectId?.let(actions.onOpenCounterWithWorkSessionActions)
                                }
                            }
                        }
                    },
                )
            },
            contentActions =
                ProjectOverviewContentActions(
                    onEditDetails = { openProjectContent { showProjectDetailsSheet = true } },
                    onMoveToFolder = { openProjectContent { showMoveFolderSheet = true } },
                    onComplete = requestProjectCompletion,
                    onReactivate = {
                        openProjectContent {
                            viewModel.reactivateProject(
                                projectActionTargetId,
                                state.completedAt,
                            )
                        }
                    },
                    onDelete = { openProjectContent { showDeleteDialog = true } },
                    onYarn = { openProjectContent { showYarnManagementSheet = true } },
                    onDocuments = { openProjectContent { showDocumentsSheet = true } },
                    onAddPattern = {
                        openProjectContent {
                            patternPickerMode =
                                if (state.projectDocuments.isEmpty()) {
                                    PatternPickerMode.INITIAL_PROJECT_PATTERN
                                } else {
                                    PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT
                                }
                            showPatternPicker = true
                        }
                    },
                    onReminders = { openProjectContent { showRemindersSheet = true } },
                    onAddReminder = {
                        openProjectContent {
                            editingReminderId = null
                            if (state.canUseRowReminders) {
                                showAddReminder = true
                            } else {
                                pendingProAction = PendingCounterProAction.OpenReminder
                            }
                        }
                    },
                ),
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CounterTopBar(
                    state = state,
                    actions = topBarActions,
                )
            },
        ) { scaffoldPadding ->
            CounterWorkspace(
                scaffoldPadding = scaffoldPadding,
                state = state,
                projectCountersActions = projectCountersActions,
                actions = mainContentActions,
                stepDelta = stepDelta,
                onBlockingCounterChanged = { id, blocked ->
                    blockingCounterIds = if (blocked) blockingCounterIds + id else blockingCounterIds - id
                },
            )
        }
    }
}

internal fun CounterUiState.blocksJourneyAction(): Boolean =
    isRestoringProject ||
        reactivationPromptCount != null ||
        showSessionRecoveryPrompt ||
        sessionStartConflict != null ||
        pendingProjectCompletionSession != null ||
        pendingProjectDeletionSession != null ||
        sessionStopSummary != null ||
        workSessionErrorRes != null ||
        activeAlert != null

@Composable
private fun CounterTargetRowsDialogHost(
    showDialog: Boolean,
    currentTarget: Int?,
    counterLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
    onRemove: () -> Unit,
) {
    if (showDialog) {
        TargetRowsDialog(
            currentTarget = currentTarget,
            counterLabel = counterLabel,
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
@Suppress("kotlin:S107", "kotlin:S3776") // Palautusdialogi näyttää kaikki talletetut palautusarvot eksplisiittisesti.
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
    val suggestedTotal = saturatingAdd(trustedDurationSeconds, suggestedDurationSeconds ?: 0L)
    val initialHours = (suggestedTotal / 3_600L).toString()
    val initialMinutes = ((suggestedTotal % 3_600L) / 60L).toString()
    var editing by rememberSaveable(recoveryIntervalToken) { mutableStateOf(false) }
    var hoursText by rememberSaveable(recoveryIntervalToken) { mutableStateOf(initialHours) }
    var minutesText by rememberSaveable(recoveryIntervalToken) { mutableStateOf(initialMinutes) }
    val durationSeconds =
        parseRecoveryDurationSeconds(hoursText, minutesText)?.let { editedSeconds ->
            if (hoursText == initialHours && minutesText == initialMinutes) suggestedTotal else editedSeconds
        }
    val headingFocusRequester = remember { FocusRequester() }
    LaunchedEffect(recoveryIntervalToken) {
        headingFocusRequester.requestFocus()
    }
    ScrollableFormDialog(
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(projectName, style = MaterialTheme.typography.titleMedium)
                Text(recoveryReasonText(recoveryReason))
                Text(
                    stringResource(
                        R.string.work_session_confirmed_time_format,
                        workSessionDurationText(trustedDurationSeconds),
                    ),
                )
                Text(
                    if (suggestedDurationSeconds != null) {
                        stringResource(
                            R.string.work_session_time_while_away_format,
                            workSessionDurationText(suggestedDurationSeconds),
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
                        NumberInputField(
                            value = hoursText,
                            onValueChange = { hoursText = it },
                            label = stringResource(R.string.work_session_hours),
                            options = NumberInputOptions(preserveRawInput = true),
                            modifier = Modifier.weight(1f),
                        )
                        NumberInputField(
                            value = minutesText,
                            onValueChange = { minutesText = it },
                            label = stringResource(R.string.work_session_minutes),
                            options = NumberInputOptions(isLast = true, preserveRawInput = true),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (durationSeconds == null) {
                        Text(
                            text = stringResource(R.string.work_session_invalid_duration),
                            style = MaterialTheme.typography.bodySmall,
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
                CancelButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
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
    ScrollableFormDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_conflict_title)) },
        text = {
            Text(
                text = stringResource(R.string.work_session_conflict_body, activeProjectName),
            )
        },
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
                CancelButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp))
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
    ScrollableFormDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_complete_project_title)) },
        text = {
            Text(
                text = stringResource(R.string.work_session_complete_project_body),
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onSave, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_save_and_complete))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_discard_and_complete))
                }
                CancelButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp))
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
    ScrollableFormDialog(
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
                CancelButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp))
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
    ScrollableFormDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_stop_summary_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(projectName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.work_session_duration_format, workSessionDurationText(durationSeconds)))
                Text(stringResource(R.string.counter_progress_format, rowsWorked))
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onSave, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text(stringResource(R.string.work_session_save))
                }
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.work_session_discard))
                }
                CancelButton(
                    onClick = onCancel,
                    text = stringResource(R.string.work_session_keep_running),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
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
    ScrollableFormDialog(
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
                    CancelButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp))
                }
            } else {
                {}
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

internal fun parseRecoveryDurationSeconds(
    hoursText: String,
    minutesText: String,
): Long? {
    if (!hoursText.all(Char::isDigit) || !minutesText.all(Char::isDigit)) return null
    val hours = hoursText.toLongOrNull() ?: return null
    val minutes = minutesText.toLongOrNull() ?: return null
    if (minutes > 59L || hours > Long.MAX_VALUE / 3_600L) return null
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
    val reminderLabel: MainCounterLabelSlot = MainCounterLabelSlot(MainCounterLabelType.ROWS),
    val showAddCounter: Boolean,
    val showResetDialog: Boolean,
    val showCompleteDialog: Boolean,
    val showDeleteDialog: Boolean,
    val showStitchDialog: Boolean,
    val projectName: String,
    val currentStitchCount: Int?,
    val proState: ProState,
    val proAccessReady: Boolean,
    val initialCounterType: ProjectCounterType = ProjectCounterType.COUNT_UP,
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
    val onStitchConfirm: (Int?) -> Unit,
    val onStitchDismiss: () -> Unit,
)

@Composable
private fun CounterScreenDialogs(
    state: CounterDialogState,
    actions: CounterDialogActions,
    onCounterTypeProRequired: () -> Unit,
) {
    if (state.showAddReminder) {
        ReminderFormSheet(
            reminder = state.editingReminder,
            label = state.reminderLabel,
            onSave = actions.onAddReminderSave,
            onDismiss = actions.onAddReminderDismiss,
        )
    }
    if (state.showAddCounter) {
        AddCounterDialog(
            onSave = actions.onAddCounterSave,
            onDismiss = actions.onAddCounterDismiss,
            proState = state.proState,
            proAccessReady = state.proAccessReady,
            onProRequired = onCounterTypeProRequired,
            initialCounterType = state.initialCounterType,
        )
    }
    if (state.showResetDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.reset_counter_title),
            message = stringResource(R.string.reset_counter_message),
            confirmText = stringResource(R.string.reset),
            onConfirm = actions.onResetConfirm,
            onDismiss = actions.onResetDismiss,
        )
    }
    if (state.showCompleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.complete_project_title),
            message = stringResource(R.string.complete_project_message, state.projectName),
            confirmText = stringResource(R.string.complete_project),
            onConfirm = actions.onCompleteConfirm,
            onDismiss = actions.onCompleteDismiss,
        )
    }
    if (state.showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_project_title),
            message = stringResource(R.string.delete_project_message, state.projectName),
            confirmText = stringResource(R.string.delete_project),
            isDestructive = true,
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
        )
    }

    if (state.showStitchDialog) {
        StitchCountDialog(
            currentStitchCount = state.currentStitchCount,
            labelType = state.reminderLabel.labelType,
            onConfirm = {
                actions.onStitchConfirm(it)
                actions.onStitchDismiss()
            },
            onDismiss = actions.onStitchDismiss,
        )
    }
}

@Composable
internal fun StitchCountDialog(
    currentStitchCount: Int?,
    labelType: MainCounterLabelType,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var stitchInput by rememberSaveable {
        mutableStateOf(currentStitchCount?.toString() ?: "")
    }
    val parsedStitchCount = stitchInput.toIntOrNull()?.takeIf { it > 0 }
    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stitches_per_row)) },
        text = {
            Column {
                NumberInputField(
                    value = stitchInput,
                    onValueChange = { stitchInput = it.filter { c -> c.isDigit() } },
                    label =
                        stringResource(
                            if (labelType == MainCounterLabelType.ROUNDS) {
                                R.string.stitches_in_round_label
                            } else {
                                R.string.stitches_in_row_label
                            },
                        ),
                    options = NumberInputOptions(isLast = true, preserveRawInput = true, allowZero = false),
                )
                if (currentStitchCount != null && currentStitchCount > 0) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.stitches_per_row_remove))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedStitchCount?.let(onConfirm) },
                enabled = parsedStitchCount != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}

// Data-luokka CounterScreenSheets-parametrien ryhmittelyyn (S107)
data class CounterSheetState(
    val showYarnPicker: Boolean,
    val showYarnManagementSheet: Boolean,
    val savedYarnCards: List<YarnCard>,
    val linkedYarns: List<LinkedYarnSummary>,
    val projectYarnNotes: List<ProjectYarnNote>,
    val showNotesSheet: Boolean,
    val notes: String,
    val showPatternPicker: Boolean,
    val projectId: Long?,
    val savedPatterns: List<SavedPattern>,
    val canUseCameraScan: Boolean,
    val proState: ProState,
    val proAccessReady: Boolean,
    val patternPickerMode: PatternPickerMode,
    val attachedSavedPatternIds: Set<Long>,
    val isRestoringProject: Boolean = false,
    val initialCameraRequestId: String? = null,
)

data class CounterSheetActions(
    val onYarnSelect: (Long) -> Unit,
    val onShowYarnPickerFromManagement: () -> Unit,
    val onUnlinkYarn: (Long) -> Unit,
    val onSaveProjectYarnNote: (String, String, Int, String) -> kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>,
    val pendingProjectYarnNoteCreation: () -> kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>?,
    val consumeProjectYarnNoteCreation: (kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>) -> Unit,
    val onEditProjectYarnNote: suspend (ProjectYarnNote, ProjectYarnNote) -> ProjectYarnNoteEditResult,
    val onDeleteProjectYarnNote: (Long) -> Unit,
    val onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    val onYarnPickerDismiss: () -> Unit,
    val onYarnManagementDismiss: () -> Unit,
    val onNotesChange: (String) -> Unit,
    val onNotesDismiss: () -> Unit,
    val onNotesExpand: () -> Unit,
    val onPatternPickerDismiss: () -> Unit,
    val onPatternFileSelected: (String, String, Long) -> Unit,
    val onSavedPatternSelected: (SavedPattern) -> Unit,
    val onImportFromRavelry: () -> Unit,
    val onAddWebPattern: () -> Unit,
    val onSeePro: () -> Unit,
)

@Composable
private fun CounterScreenSheets(
    state: CounterSheetState,
    actions: CounterSheetActions,
    yarnSnackbarHostState: SnackbarHostState,
) {
    if (state.showYarnPicker) {
        YarnCardPickerSheet(
            savedYarnCards = state.savedYarnCards,
            onSelect = { actions.onYarnSelect(it.id) },
            onDismiss = actions.onYarnPickerDismiss,
        )
    }
    if (state.showYarnManagementSheet && state.projectId != null) {
        ProjectYarnUsageFlow(
            projectId = state.projectId,
            linkedYarns = state.linkedYarns,
            projectYarnNotes = state.projectYarnNotes,
            proState = state.proState,
            proAccessReady = state.proAccessReady,
            snackbarHostState = yarnSnackbarHostState,
            actions =
                YarnManagementSheetActions(
                    onUnlinkYarn = actions.onUnlinkYarn,
                    onAddYarn = actions.onShowYarnPickerFromManagement,
                    onSaveProjectYarnNote = actions.onSaveProjectYarnNote,
                    pendingProjectYarnNoteCreation = actions.pendingProjectYarnNoteCreation,
                    consumeProjectYarnNoteCreation = actions.consumeProjectYarnNoteCreation,
                    onEditProjectYarnNote = actions.onEditProjectYarnNote,
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
            initialCameraRequestId = state.initialCameraRequestId,
            isRestoringProject = state.isRestoringProject,
            savedPatterns = state.savedPatterns,
            canUseCameraScan = state.canUseCameraScan,
            proState = state.proState,
            proAccessReady = state.proAccessReady,
            mode = state.patternPickerMode,
            excludedSavedPatternIds = state.attachedSavedPatternIds,
            onSavedPatternSelected = actions.onSavedPatternSelected,
            onDocumentSelected = actions.onPatternFileSelected,
            onImportFromRavelry = actions.onImportFromRavelry,
            onAddWebPattern = actions.onAddWebPattern,
            onSeePro = actions.onSeePro,
            onDismiss = actions.onPatternPickerDismiss,
        )
    }
}

// Data-luokka CounterTopBar-callbackien ryhmittelyyn (S107)
data class CounterTopBarActions(
    val onBack: () -> Unit,
    val onShowProjectActions: () -> Unit,
    val onOpenOverview: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterTopBar(
    state: CounterUiState,
    actions: CounterTopBarActions,
) {
    TopAppBar(
        expandedHeight =
            if (LocalAdaptiveContentSize.current.isCompactHeight) {
                CounterDimens.CompactTopBarHeight
            } else {
                TopAppBarDefaults.TopAppBarExpandedHeight
            },
        title = {
            CounterTopBarTitle(
                state = state,
                onOpenOverview = actions.onOpenOverview,
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

// Otsikon napautus avaa projektinäkymän. Nimen muokkaus kuuluu projektinäkymän Edit details -dialogiin,
// joten erillinen otsikon sisäinen muokkauskenttä poistettiin. Nimi näkyy sellaisenaan kuten projektinäkymässä:
// versaalit katkaisivat pitkiä nimiä ja näyttivät saman projektin eri näköisenä kahdessa näkymässä.
@Composable
private fun CounterTopBarTitle(
    state: CounterUiState,
    onOpenOverview: () -> Unit,
) {
    Text(
        text = state.projectName.ifEmpty { stringResource(R.string.default_project_name) },
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
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.project_card_open_overview_action),
                    onClick = onOpenOverview,
                ),
    )
}

private data class CounterDialogActionDependencies(
    val projectId: Long?,
    val enableStitchTrackingOnConfirm: Boolean,
    val editingReminderId: Long?,
    val onBack: () -> Unit,
    val onHideAddReminder: () -> Unit,
    val onHideAddCounter: () -> Unit,
    val onHideResetDialog: () -> Unit,
    val onHideCompleteDialog: () -> Unit,
    val onHideDeleteDialog: () -> Unit,
    val onHideStitchDialog: () -> Unit,
    val onReminderProRequired: () -> Unit,
    val onCounterProRequired: () -> Unit,
)

private data class CounterTopBarActionDependencies(
    val onBack: () -> Unit,
    val onShowProjectActions: () -> Unit,
    val onOpenOverview: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountersListSheet(
    projectCounters: List<ProjectCounter>,
    mainRowCount: Int,
    actions: ProjectCountersSectionActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedModalSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetTitle(text = stringResource(R.string.counters_list_title))
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
private fun NotesSheet(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onExpandToFullScreen: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberExpandedModalSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetTitle(
                    text = stringResource(R.string.notes),
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
                colors = cardTextFieldColors(),
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
        val window = view.context.findActivity()?.window
        val lifecycle = lifecycleOwner.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    else -> {}
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
                saveReminder(viewModel, dependencies, targetRow, repeatInterval, message)
            },
            onAddReminderDismiss = dependencies.onHideAddReminder,
            onAddCounterSave = { draft -> saveProjectCounter(viewModel, dependencies, draft) },
            onAddCounterDismiss = dependencies.onHideAddCounter,
            onResetConfirm = {
                runForCurrentProject(dependencies, viewModel) { viewModel.reset() }
                dependencies.onHideResetDialog()
            },
            onResetDismiss = dependencies.onHideResetDialog,
            onCompleteConfirm = {
                runForCurrentProject(dependencies, viewModel) { completeProject() }
                dependencies.onHideCompleteDialog()
            },
            onCompleteDismiss = dependencies.onHideCompleteDialog,
            onDeleteConfirm = {
                dependencies.projectId
                    ?.takeIf { it == viewModel.uiState.value.projectId }
                    ?.let { viewModel.deleteProject(it) }
                dependencies.onHideDeleteDialog()
            },
            onDeleteDismiss = dependencies.onHideDeleteDialog,
            onStitchConfirm = { stitchCount ->
                runForCurrentProject(dependencies, viewModel) {
                    setStitchCount(stitchCount, enableTracking = dependencies.enableStitchTrackingOnConfirm)
                }
            },
            onStitchDismiss = dependencies.onHideStitchDialog,
        )
    }
}

private fun saveReminder(
    viewModel: CounterViewModel,
    dependencies: CounterDialogActionDependencies,
    targetRow: Int,
    repeatInterval: Int?,
    message: String,
) {
    val editingReminderId = dependencies.editingReminderId
    if (editingReminderId != null) {
        viewModel.updateReminder(editingReminderId, targetRow, repeatInterval, message)
        dependencies.onHideAddReminder()
        return
    }

    if (viewModel.addReminder(targetRow, repeatInterval, message)) {
        dependencies.onHideAddReminder()
    } else {
        dependencies.onReminderProRequired()
    }
}

private fun saveProjectCounter(
    viewModel: CounterViewModel,
    dependencies: CounterDialogActionDependencies,
    draft: ProjectCounterDraft,
) {
    runForCurrentProject(dependencies, viewModel) {
        if (addProjectCounter(draft)) {
            dependencies.onHideAddCounter()
        } else {
            dependencies.onCounterProRequired()
        }
    }
}

private inline fun runForCurrentProject(
    dependencies: CounterDialogActionDependencies,
    viewModel: CounterViewModel,
    action: CounterViewModel.() -> Unit,
) {
    if (dependencies.projectId != null && dependencies.projectId == viewModel.uiState.value.projectId) {
        viewModel.action()
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
    onWebPatternReplacementRequired: (pattern: SavedPattern, existingPatternId: Long, requestProjectId: Long) -> Unit,
    onPatternMetadataAttachFailed: () -> Unit,
    onPatternImportResult: (ProjectDocumentError?) -> Unit,
    onImportFromRavelry: () -> Unit,
    onAddWebPattern: () -> Unit,
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
        onWebPatternReplacementRequired,
        onPatternMetadataAttachFailed,
        onPatternImportResult,
        onImportFromRavelry,
        onAddWebPattern,
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
            pendingProjectYarnNoteCreation = viewModel::pendingProjectYarnNoteCreation,
            consumeProjectYarnNoteCreation = viewModel::consumeProjectYarnNoteCreation,
            onEditProjectYarnNote = { original, draft -> viewModel.editProjectYarnNote(original, draft).await() },
            onDeleteProjectYarnNote = viewModel::deleteProjectYarnNote,
            onSaveProjectYarnNoteToMyYarn = onSaveProjectYarnNoteToMyYarn,
            onYarnPickerDismiss = onHideYarnPicker,
            onYarnManagementDismiss = onHideYarnManagementSheet,
            onNotesChange = viewModel::setNotes,
            onNotesDismiss = onHideNotesSheet,
            onNotesExpand = onExpandNotes,
            onPatternPickerDismiss = onHidePatternPicker,
            onPatternFileSelected = { uri, name, owner ->
                attachProjectPatternWithFeedback(viewModel, uri, name, owner, onPatternImportResult)
            },
            onSavedPatternSelected = { pattern ->
                // Pyynnön projekti kulkee tuloksen mukana, jottei myöhäinen tulos osu uuteen projektiin.
                val requestProjectId = viewModel.uiState.value.projectId
                if (pattern.isWebPatternCompatible && requestProjectId == null) {
                    onPatternMetadataAttachFailed()
                } else if (pattern.isWebPatternCompatible && requestProjectId != null) {
                    viewModel.attachSavedPatternMetadata(pattern.id, projectId = requestProjectId) { result ->
                        when (result) {
                            is SavedPatternMetadataMutationResult.Attached,
                            is SavedPatternMetadataMutationResult.AlreadyAttached,
                            -> {
                                if (requestProjectId == viewModel.uiState.value.projectId) onHidePatternPicker()
                            }

                            is SavedPatternMetadataMutationResult.ReplacementRequired -> {
                                onWebPatternReplacementRequired(
                                    pattern,
                                    result.existingSavedPatternId,
                                    requestProjectId,
                                )
                            }

                            SavedPatternMetadataMutationResult.ProjectMissing,
                            SavedPatternMetadataMutationResult.PatternMissing,
                            SavedPatternMetadataMutationResult.NotWebPattern,
                            SavedPatternMetadataMutationResult.StaleAction,
                            SavedPatternMetadataMutationResult.PersistenceFailure,
                            SavedPatternMetadataMutationResult.Unlinked,
                            SavedPatternMetadataMutationResult.AlreadyUnlinked,
                            -> {
                                onPatternMetadataAttachFailed()
                            }
                        }
                    }
                } else {
                    viewModel.attachSavedPattern(pattern)
                }
            },
            onImportFromRavelry = onImportFromRavelry,
            onAddWebPattern = onAddWebPattern,
            onSeePro = onSeePro,
        )
    }
}

internal fun attachProjectPatternWithFeedback(
    viewModel: CounterViewModel,
    uri: String,
    name: String,
    owner: Long,
    onResult: (ProjectDocumentError?) -> Unit,
) {
    viewModel.attachPattern(uri, name, expectedProjectId = owner) { result ->
        if (viewModel.uiState.value.projectId == owner) {
            onResult(result.toProjectDocumentError())
        }
    }
}

@Composable
private fun rememberCounterTopBarActions(dependencies: CounterTopBarActionDependencies): CounterTopBarActions =
    remember(dependencies) {
        CounterTopBarActions(
            onBack = dependencies.onBack,
            onShowProjectActions = dependencies.onShowProjectActions,
            onOpenOverview = dependencies.onOpenOverview,
        )
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
