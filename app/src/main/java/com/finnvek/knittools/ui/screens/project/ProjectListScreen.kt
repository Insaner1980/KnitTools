package com.finnvek.knittools.ui.screens.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderMoveDirection
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.components.LabeledCounterImageButton
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectCard
import com.finnvek.knittools.ui.components.ProjectDetailsSheet
import com.finnvek.knittools.ui.components.ProjectDetailsValues
import com.finnvek.knittools.ui.components.RenameProjectDialog
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SelectionAction
import com.finnvek.knittools.ui.components.SelectionActionBar
import com.finnvek.knittools.ui.components.workSessionStatusText
import com.finnvek.knittools.ui.screens.counter.ActiveSessionCompletionDialog
import com.finnvek.knittools.ui.screens.counter.ActiveSessionDeletionDialog
import com.finnvek.knittools.ui.theme.ProjectListDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Suppress("kotlin:S3776") // Reitti omistaa listan sheetit, valinnat ja navigointipyynnöt.
fun ProjectListScreen(
    onOpenCounter: (Long) -> Unit,
    onOpenOverview: (Long) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    viewModelProvider: @Composable () -> ProjectListViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val active by viewModel.activeProjects.collectAsStateWithLifecycle()
    val completed by viewModel.completedProjects.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val pendingCompletionSessionAction by viewModel.pendingCompletionSessionAction.collectAsStateWithLifecycle()
    val pendingDeletionSessionAction by viewModel.pendingDeletionSessionAction.collectAsStateWithLifecycle()
    val continueKnitting by viewModel.continueKnittingProject.collectAsStateWithLifecycle()
    val latestPhotoUris by viewModel.projectLatestPhotoUris.collectAsStateWithLifecycle()
    val patternNames by viewModel.projectPatternNames.collectAsStateWithLifecycle()
    val showCompleted by viewModel.showCompleted.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedProjectIds by viewModel.selectedProjectIds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val folderState by viewModel.folderState.collectAsStateWithLifecycle()
    val selectedFolderFilter by viewModel.selectedFolderFilter.collectAsStateWithLifecycle()
    val hasHiddenCompletedProjects by viewModel.hasHiddenCompletedProjects.collectAsStateWithLifecycle()
    val projectCreationError by viewModel.projectCreationError.collectAsStateWithLifecycle()
    val folders = folderState.snapshot?.folders.orEmpty()
    val memberships = folderState.snapshot?.memberships.orEmpty()
    val selectedActiveCount = active.count { it.id in selectedProjectIds }
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val folderSelectorFocus = remember { FocusRequester() }
    var showFoldersSheet by rememberSaveable { mutableStateOf(false) }
    var showFolderNameDialog by rememberSaveable { mutableStateOf(false) }
    var editingFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingFolderName by rememberSaveable { mutableStateOf("") }
    var deletingFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showMoveSheet by rememberSaveable { mutableStateOf(false) }
    var focusFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusCreateFolder by rememberSaveable { mutableStateOf(false) }
    var restoreSelectorFocus by rememberSaveable { mutableStateOf(false) }
    var creationFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creationFolderName by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateProjectDialog by rememberSaveable { mutableStateOf(false) }
    val projectPromptCount by viewModel.projectCreationPromptCount.collectAsStateWithLifecycle()
    // Kansiovalitsin on yläpalkin otsikko, jota monivalinnassa ei näytetä: fokus palautetaan vasta sen jälkeen.
    LaunchedEffect(restoreSelectorFocus, showMoveSheet, showFoldersSheet, isMultiSelectMode) {
        if (restoreSelectorFocus && !showMoveSheet && !showFoldersSheet && !isMultiSelectMode) {
            withFrameNanos { }
            folderSelectorFocus.requestFocus()
            restoreSelectorFocus = false
        }
    }
    CollectWithLifecycleEffect({ viewModel.folderEvents }) { result ->
        when (result) {
            is ProjectFolderMutationResult.Created, is ProjectFolderMutationResult.Renamed -> {
                showFolderNameDialog = false
            }

            is ProjectFolderMutationResult.Deleted -> {
                deletingFolderId = null
                val remaining = folders.filterNot { it.id == result.folder.id }
                focusFolderId =
                    remaining.firstOrNull { it.sortOrder >= result.folder.sortOrder }?.id
                        ?: remaining.lastOrNull()?.id
                focusCreateFolder = remaining.isEmpty()
                coroutineScope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.folder_deleted)) }
            }

            is ProjectFolderMutationResult.Assigned,
            is ProjectFolderMutationResult.Unassigned,
            is ProjectFolderMutationResult.ProjectsMoved,
            is ProjectFolderMutationResult.AlreadyAssigned,
            -> {
                val count =
                    when (result) {
                        is ProjectFolderMutationResult.ProjectsMoved -> result.projectIds.size
                        is ProjectFolderMutationResult.AlreadyAssigned -> result.projectIds.size
                        else -> 1
                    }
                showMoveSheet = false
                restoreSelectorFocus = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        resources.getQuantityString(R.plurals.folder_moved_projects, count, count),
                    )
                }
            }

            else -> {}
        }
    }
    CollectWithLifecycleEffect({ viewModel.sessionErrors }) { error ->
        coroutineScope.launch { snackbarHostState.showSnackbar(resources.getString(error)) }
    }
    // Luonnin jälkeen navigoi uuteen projektiin
    CollectWithLifecycleEffect({ viewModel.navigateToProject }) { projectId ->
        showCreateProjectDialog = false
        onOpenCounter(projectId)
    }

    CollectWithLifecycleEffect({ viewModel.showCreateProjectDialog }) {
        showCreateProjectDialog = true
    }

    projectPromptCount?.let { count ->
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.Projects,
                    existingProjectCount = count,
                ),
            onDismiss = viewModel::dismissPendingProjectCreation,
            onTrialStarted = viewModel::retryPendingProjectCreation,
            onSeePro = onUpgradeToPro,
        )
    }

    // Multi-select back handler
    BackHandler(enabled = isMultiSelectMode || selectedFolderFilter != ProjectFolderFilter.AllProjects) {
        if (isMultiSelectMode) {
            viewModel.exitMultiSelectMode()
        } else {
            viewModel.selectFolder(ProjectFolderFilter.AllProjects)
        }
    }

    // Dialogi-tilat
    var menuProjectId by rememberSaveable { mutableLongStateOf(0L) }
    var menuProjectName by rememberSaveable { mutableStateOf("") }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showMultiCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var showMultiDeleteDialog by rememberSaveable { mutableStateOf(false) }
    ProjectListDialogs(
        state =
            ProjectListDialogState(
                showRenameDialog = showRenameDialog,
                renameText = renameText,
                showDeleteDialog = showDeleteDialog,
                deleteProjectName = menuProjectName,
                showMultiCompleteDialog = showMultiCompleteDialog,
                selectedCount = selectedProjectIds.size,
                selectedActiveCount = selectedActiveCount,
                showMultiDeleteDialog = showMultiDeleteDialog,
            ),
        actions =
            ProjectListDialogActions(
                onRenameTextChange = { renameText = it },
                onRenameConfirm = {
                    viewModel.renameProject(menuProjectId, renameText.trim())
                    showRenameDialog = false
                },
                onRenameDismiss = { showRenameDialog = false },
                onDeleteConfirm = {
                    viewModel.deleteProject(menuProjectId)
                    showDeleteDialog = false
                },
                onDeleteDismiss = { showDeleteDialog = false },
                onMultiCompleteConfirm = {
                    viewModel.completeSelectedProjects()
                    showMultiCompleteDialog = false
                },
                onMultiCompleteDismiss = { showMultiCompleteDialog = false },
                onMultiDeleteConfirm = {
                    viewModel.deleteSelectedProjects()
                    showMultiDeleteDialog = false
                },
                onMultiDeleteDismiss = { showMultiDeleteDialog = false },
            ),
    )

    if (pendingCompletionSessionAction != null) {
        ActiveSessionCompletionDialog(
            onSave = { viewModel.resolvePendingCompletion(saveSession = true) },
            onDiscard = { viewModel.resolvePendingCompletion(saveSession = false) },
            onCancel = viewModel::cancelPendingCompletion,
        )
    }

    if (pendingDeletionSessionAction != null) {
        ActiveSessionDeletionDialog(
            onDiscardAndDelete = viewModel::resolvePendingDeletion,
            onCancel = viewModel::cancelPendingDeletion,
        )
    }

    if (showCreateProjectDialog) {
        ProjectDetailsSheet(
            title = stringResource(R.string.new_project_details_title),
            confirmText = stringResource(R.string.create_project),
            initialValues =
                ProjectDetailsValues(
                    name = "",
                    craftType = CraftType.KNITTING,
                    mainCounterLabelType = MainCounterLabelType.ROWS,
                    mainCounterCustomLabel = null,
                ),
            onConfirm = { values ->
                viewModel.createProject(
                    values.name,
                    values.craftType,
                    values.mainCounterLabelType,
                    values.mainCounterCustomLabel,
                    targetFolderId = creationFolderId,
                )
            },
            onDismiss = {
                viewModel.projectCreationDismissed()
                showCreateProjectDialog = false
            },
            destinationText =
                stringResource(
                    R.string.folder_project_creation_destination,
                    creationFolderName ?: stringResource(R.string.folder_unfiled),
                ),
            errorMessage =
                if (projectCreationError ==
                    ProjectCreationResult.FolderMissing
                ) {
                    stringResource(R.string.folder_missing)
                } else {
                    null
                },
        )
    }

    if (showFoldersSheet) {
        ProjectFoldersSheet(
            folders = folders,
            selectedFilter = selectedFolderFilter,
            isLoading = folderState.isLoading,
            errorMessage =
                if (folderState.readFailed) {
                    stringResource(R.string.folder_load_error)
                } else {
                    folderState.mutationError?.let { stringResource(it.errorResource(R.string.folder_reorder_error)) }
                },
            isMutating = folderState.isMutating,
            onSelectFilter = {
                viewModel.selectFolder(it)
                showFoldersSheet = false
                restoreSelectorFocus = true
            },
            onCreateFolder = {
                viewModel.clearFolderError()
                editingFolderId = null
                editingFolderName = ""
                showFolderNameDialog = true
            },
            onRenameFolder = { id ->
                viewModel.clearFolderError()
                editingFolderId = id
                editingFolderName = folders.firstOrNull { it.id == id }?.name.orEmpty()
                showFolderNameDialog = true
            },
            onMoveEarlier = { viewModel.moveFolder(it, ProjectFolderMoveDirection.EARLIER) },
            onMoveLater = { viewModel.moveFolder(it, ProjectFolderMoveDirection.LATER) },
            onDeleteFolder = {
                viewModel.clearFolderError()
                focusFolderId = null
                focusCreateFolder = false
                deletingFolderId = it
            },
            onRetry = viewModel::retryFolderLoading,
            onDismiss = {
                showFoldersSheet = false
                restoreSelectorFocus = true
            },
            focusFolderId = focusFolderId,
            focusCreateFolder = focusCreateFolder,
        )
    }
    if (showFolderNameDialog) {
        ProjectFolderNameDialog(
            folderId = editingFolderId,
            initialName = editingFolderName,
            errorMessage =
                folderState.mutationError?.let {
                    stringResource(
                        it.errorResource(
                            if (editingFolderId ==
                                null
                            ) {
                                R.string.folder_create_error
                            } else {
                                R.string.folder_rename_error
                            },
                        ),
                    )
                },
            isSaving = folderState.isMutating,
            onConfirm = { name ->
                val id = editingFolderId
                if (id == null) viewModel.createFolder(name) else viewModel.renameFolder(id, name)
            },
            onDismiss = { showFolderNameDialog = false },
            onClearError = viewModel::clearFolderError,
        )
    }
    deletingFolderId?.let { id ->
        folders.firstOrNull { it.id == id }?.let { folder ->
            DeleteProjectFolderDialog(
                folder = folder,
                assignedProjectCount = memberships.count { it.folderId == id },
                isDeleting = folderState.isMutating,
                errorMessage =
                    folderState.mutationError?.let {
                        stringResource(
                            it.errorResource(R.string.folder_delete_error),
                        )
                    },
                onConfirm = { viewModel.deleteFolder(id) },
                onDismiss = { deletingFolderId = null },
            )
        }
    }
    if (showMoveSheet) {
        val selectedMemberships = memberships.filter { it.projectId in selectedProjectIds }
        val destinations = selectedMemberships.map { it.folderId }.toSet()
        MoveToFolderSheet(
            projectCount = selectedProjectIds.size,
            currentFolderId = destinations.singleOrNull(),
            hasCommonDestination = destinations.size == 1 && selectedMemberships.size == selectedProjectIds.size,
            folders = folders,
            isLoading = folderState.isLoading,
            errorMessage =
                when {
                    folderState.readFailed -> {
                        stringResource(R.string.folder_load_error)
                    }

                    folderState.mutationError == ProjectFolderMutationResult.PersistenceFailure -> {
                        pluralStringResource(
                            R.plurals.folder_move_projects_error,
                            selectedProjectIds.size,
                            selectedProjectIds.size,
                        )
                    }

                    else -> {
                        folderState.mutationError?.let {
                            stringResource(
                                it.errorResource(R.string.folder_move_error),
                            )
                        }
                    }
                },
            isMoving = folderState.isMutating,
            onMoveToFolder = viewModel::moveSelectedProjects,
            onRetry = viewModel::retryFolderLoading,
            onDismiss = {
                showMoveSheet = false
                restoreSelectorFocus = true
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProjectListTopBar(
                state =
                    ProjectListTopBarState(
                        isMultiSelectMode = isMultiSelectMode,
                        selectedCount = selectedProjectIds.size,
                        showCompleted = showCompleted,
                        sortOrder = sortOrder,
                        showOverflowMenu = showOverflowMenu,
                        showSortMenu = showSortMenu,
                    ),
                actions =
                    ProjectListTopBarActions(
                        onExitMultiSelect = { viewModel.exitMultiSelectMode() },
                        onSelectAll = { viewModel.selectAllProjects() },
                        onShowOverflowMenu = { showOverflowMenu = true },
                        onDismissOverflowMenu = { showOverflowMenu = false },
                        onEnterMultiSelect = {
                            showOverflowMenu = false
                            viewModel.enterMultiSelectMode()
                        },
                        onShowSortMenu = {
                            showOverflowMenu = false
                            showSortMenu = true
                        },
                        onDismissSortMenu = { showSortMenu = false },
                        onToggleShowCompleted = { viewModel.toggleShowCompleted() },
                        onSortOrderChange = { order ->
                            viewModel.setSortOrder(order)
                            showSortMenu = false
                        },
                    ),
                // Valittu kansio on näytön otsikko kuten Insightsin aikaväli: alanavigaatio kertoo jo
                // että ollaan Projectsissa, ja erillinen linkki otsikon alla kellui irrallaan.
                folderTitle = {
                    ProjectFolderSelector(
                        selectedFilter = selectedFolderFilter,
                        folders = folders,
                        onClick = {
                            viewModel.clearFolderError()
                            focusFolderId = null
                            focusCreateFolder = false
                            showFoldersSheet = true
                        },
                        enabled = !folderState.isMutating,
                        focusRequester = folderSelectorFocus,
                    )
                },
            )
        },
        bottomBar = {
            MultiSelectBottomBar(
                isMultiSelectMode = isMultiSelectMode,
                hasSelection = selectedProjectIds.isNotEmpty(),
                hasActiveSelection = selectedActiveCount > 0,
                onMove = {
                    viewModel.clearFolderError()
                    showMoveSheet = true
                },
                onComplete = { showMultiCompleteDialog = true },
                onDelete = { showMultiDeleteDialog = true },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            ProjectListContent(
                state =
                    ProjectListContentState(
                        active = active,
                        completed = completed,
                        continueKnitting = continueKnitting,
                        latestPhotoUris = latestPhotoUris,
                        patternNames = patternNames,
                        showCompleted = showCompleted,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedProjectIds = selectedProjectIds,
                        activeSessionProjectId = activeSession?.projectId,
                        activeSessionNeedsReview = activeSession?.needsRecoveryReview == true,
                        selectedFolderFilter = selectedFolderFilter,
                        folders = folders,
                        hasHiddenCompletedProjects = hasHiddenCompletedProjects,
                        isLoading = folderState.isLoading && selectedFolderFilter != ProjectFolderFilter.AllProjects,
                    ),
                actions =
                    ProjectListContentActions(
                        onOpenCounter = onOpenCounter,
                        onOpenOverview = onOpenOverview,
                        onToggleSelection = { viewModel.toggleProjectSelection(it) },
                        onEnterMultiSelect = { viewModel.enterMultiSelectMode(it) },
                        onShowCompleted = viewModel::toggleShowCompleted,
                        onDeleteSwipe = { id, name ->
                            menuProjectId = id
                            menuProjectName = name
                            showDeleteDialog = true
                        },
                    ),
            )

            // Luontipainike ei näy multi-select-tilassa. Teksti kertoo heti, mitä nappi tekee:
            // sama plus-nappi tarkoittaa laskurissa rivin lisäämistä.
            if (!isMultiSelectMode) {
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = stringResource(R.string.new_project),
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = {
                        creationFolderId = (selectedFolderFilter as? ProjectFolderFilter.Folder)?.folderId
                        creationFolderName = folders.firstOrNull { it.id == creationFolderId }?.name
                        viewModel.requestProjectCreation()
                    },
                    enabled = selectedFolderFilter !is ProjectFolderFilter.Folder || !folderState.isLoading,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(ProjectListDimens.CreateButtonMargin),
                )
            }
        }
    }
}

// Data-luokat ProjectListDialogs-parametrien ryhmittelyyn (S107)
data class ProjectListDialogState(
    val showRenameDialog: Boolean,
    val renameText: String,
    val showDeleteDialog: Boolean,
    val deleteProjectName: String,
    val showMultiCompleteDialog: Boolean,
    val selectedCount: Int,
    val showMultiDeleteDialog: Boolean,
    val selectedActiveCount: Int = selectedCount,
)

data class ProjectListDialogActions(
    val onRenameTextChange: (String) -> Unit,
    val onRenameConfirm: () -> Unit,
    val onRenameDismiss: () -> Unit,
    val onDeleteConfirm: () -> Unit,
    val onDeleteDismiss: () -> Unit,
    val onMultiCompleteConfirm: () -> Unit,
    val onMultiCompleteDismiss: () -> Unit,
    val onMultiDeleteConfirm: () -> Unit,
    val onMultiDeleteDismiss: () -> Unit,
)

@Composable
private fun ProjectListDialogs(
    state: ProjectListDialogState,
    actions: ProjectListDialogActions,
) {
    if (state.showRenameDialog) {
        RenameProjectDialog(
            renameText = state.renameText,
            onRenameTextChange = actions.onRenameTextChange,
            onConfirm = actions.onRenameConfirm,
            onDismiss = actions.onRenameDismiss,
        )
    }

    if (state.showDeleteDialog) {
        DeleteProjectDialog(
            projectName = state.deleteProjectName,
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
        )
    }

    if (state.showMultiCompleteDialog) {
        MultiCompleteDialog(
            selectedCount = state.selectedActiveCount,
            onConfirm = actions.onMultiCompleteConfirm,
            onDismiss = actions.onMultiCompleteDismiss,
        )
    }

    if (state.showMultiDeleteDialog) {
        MultiDeleteDialog(
            selectedCount = state.selectedCount,
            onConfirm = actions.onMultiDeleteConfirm,
            onDismiss = actions.onMultiDeleteDismiss,
        )
    }
}

@Composable
private fun MultiCompleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.complete_project)) },
        text = { Text(pluralStringResource(R.plurals.complete_n_projects, selectedCount, selectedCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.complete_project))
            }
        },
        dismissButton = {
            CancelButton(onClick = onDismiss)
        },
    )
}

@Composable
private fun MultiDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_project),
        message = pluralStringResource(R.plurals.delete_n_projects, selectedCount, selectedCount),
        confirmText = stringResource(R.string.delete_project),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

// Data-luokat ProjectListTopBar-parametrien ryhmittelyyn (S107)
data class ProjectListTopBarState(
    val isMultiSelectMode: Boolean,
    val selectedCount: Int,
    val showCompleted: Boolean,
    val sortOrder: ProjectSortOrder,
    val showOverflowMenu: Boolean,
    val showSortMenu: Boolean,
)

data class ProjectListTopBarActions(
    val onExitMultiSelect: () -> Unit,
    val onSelectAll: () -> Unit,
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
    val onShowOverflowMenu: () -> Unit,
    val onDismissOverflowMenu: () -> Unit,
    val onEnterMultiSelect: () -> Unit,
    val onShowSortMenu: () -> Unit,
    val onDismissSortMenu: () -> Unit,
    val onToggleShowCompleted: () -> Unit,
    val onSortOrderChange: (ProjectSortOrder) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
// CPD-ON
@Composable
private fun ProjectListTopBar(
    state: ProjectListTopBarState,
    actions: ProjectListTopBarActions,
    folderTitle: @Composable () -> Unit,
) {
    TopAppBar(
        title = {
            if (state.isMultiSelectMode) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.n_selected, state.selectedCount),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    TextButton(onClick = actions.onSelectAll) {
                        Text(stringResource(R.string.select_all))
                    }
                }
            } else {
                folderTitle()
            }
        },
        navigationIcon = {
            if (state.isMultiSelectMode) {
                IconButton(onClick = actions.onExitMultiSelect) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            }
        },
        actions = {
            if (!state.isMultiSelectMode) {
                OverflowMenuWithSort(
                    state =
                        OverflowMenuState(
                            showOverflowMenu = state.showOverflowMenu,
                            showSortMenu = state.showSortMenu,
                            showCompleted = state.showCompleted,
                            sortOrder = state.sortOrder,
                        ),
                    actions =
                        OverflowMenuActions(
                            onShowOverflowMenu = actions.onShowOverflowMenu,
                            onDismissOverflowMenu = actions.onDismissOverflowMenu,
                            onEnterMultiSelect = actions.onEnterMultiSelect,
                            onShowSortMenu = actions.onShowSortMenu,
                            onDismissSortMenu = actions.onDismissSortMenu,
                            onToggleShowCompleted = actions.onToggleShowCompleted,
                            onSortOrderChange = actions.onSortOrderChange,
                        ),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

// Data-luokat OverflowMenuWithSort-parametrien ryhmittelyyn (S107)
data class OverflowMenuState(
    val showOverflowMenu: Boolean,
    val showSortMenu: Boolean,
    val showCompleted: Boolean,
    val sortOrder: ProjectSortOrder,
)

data class OverflowMenuActions(
    val onShowOverflowMenu: () -> Unit,
    val onDismissOverflowMenu: () -> Unit,
    val onEnterMultiSelect: () -> Unit,
    val onShowSortMenu: () -> Unit,
    val onDismissSortMenu: () -> Unit,
    val onToggleShowCompleted: () -> Unit,
    val onSortOrderChange: (ProjectSortOrder) -> Unit,
)

@Composable
private fun OverflowMenuWithSort(
    state: OverflowMenuState,
    actions: OverflowMenuActions,
) {
    Box {
        IconButton(onClick = actions.onShowOverflowMenu) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_options),
            )
        }
        DropdownMenu(
            expanded = state.showOverflowMenu && !state.showSortMenu,
            onDismissRequest = actions.onDismissOverflowMenu,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.select_projects)) },
                onClick = actions.onEnterMultiSelect,
                contentPadding = PaddingValues(horizontal = 12.dp),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_by)) },
                onClick = actions.onShowSortMenu,
                contentPadding = PaddingValues(horizontal = 12.dp),
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.showCompleted) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Text(text = stringResource(R.string.show_completed))
                    }
                },
                onClick = actions.onToggleShowCompleted,
                contentPadding = PaddingValues(horizontal = 12.dp),
            )
        }
        // Lajittelu-alivalikko
        SortSubMenu(
            expanded = state.showSortMenu,
            sortOrder = state.sortOrder,
            onDismiss = actions.onDismissSortMenu,
            onSortOrderChange = actions.onSortOrderChange,
        )
    }
}

@Composable
private fun SortSubMenu(
    expanded: Boolean,
    sortOrder: ProjectSortOrder,
    onDismiss: () -> Unit,
    onSortOrderChange: (ProjectSortOrder) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        SortMenuItem(
            label = stringResource(R.string.sort_name),
            selected = sortOrder == ProjectSortOrder.NAME,
            onClick = { onSortOrderChange(ProjectSortOrder.NAME) },
        )
        SortMenuItem(
            label = stringResource(R.string.sort_last_updated),
            selected = sortOrder == ProjectSortOrder.UPDATED,
            onClick = { onSortOrderChange(ProjectSortOrder.UPDATED) },
        )
        SortMenuItem(
            label = stringResource(R.string.sort_created_date),
            selected = sortOrder == ProjectSortOrder.CREATED,
            onClick = { onSortOrderChange(ProjectSortOrder.CREATED) },
        )
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Text(text = label)
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp),
    )
}

@Composable
private fun MultiSelectBottomBar(
    isMultiSelectMode: Boolean,
    hasSelection: Boolean,
    hasActiveSelection: Boolean,
    onMove: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    SelectionActionBar(
        visible = isMultiSelectMode && hasSelection,
        actions =
            listOf(
                SelectionAction(stringResource(R.string.selection_action_move), onMove),
                SelectionAction(
                    stringResource(R.string.selection_action_complete),
                    onComplete,
                    enabled = hasActiveSelection,
                ),
                SelectionAction(stringResource(R.string.delete_project), onDelete, destructive = true),
            ),
    )
}

// Data-luokat ProjectListContent-parametrien ryhmittelyyn (S107)
@Immutable
data class ProjectListContentState(
    val active: List<CounterProject>,
    val completed: List<CounterProject>,
    val continueKnitting: ContinueKnittingProject?,
    val latestPhotoUris: Map<Long, String>,
    val patternNames: Map<Long, String>,
    val showCompleted: Boolean,
    val isMultiSelectMode: Boolean,
    val selectedProjectIds: Set<Long>,
    val activeSessionProjectId: Long?,
    val activeSessionNeedsReview: Boolean,
    val selectedFolderFilter: ProjectFolderFilter = ProjectFolderFilter.AllProjects,
    val folders: List<ProjectFolder> = emptyList(),
    val hasHiddenCompletedProjects: Boolean = false,
    val isLoading: Boolean = false,
)

// CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
data class ProjectListContentActions(
    val onOpenCounter: (Long) -> Unit,
    val onOpenOverview: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
    // CPD-ON
    val onDeleteSwipe: (Long, String) -> Unit,
    val onShowCompleted: () -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("kotlin:S3776") // Lista kokoaa tarkoituksella hero-, active- ja completed-sektiot samaan composableen
private fun ProjectListContent(
    state: ProjectListContentState,
    actions: ProjectListContentActions,
) {
    val isHeroVisible = !state.isMultiSelectMode && state.continueKnitting != null
    val heroProjectId = state.continueKnitting?.projectId
    val visibleActiveProjects =
        if (isHeroVisible) {
            state.active.filterNot { it.id == heroProjectId }
        } else {
            state.active
        }

    LazyColumn(
        contentPadding =
            PaddingValues(
                start = ProjectListDimens.ScreenHorizontalPadding,
                top = ProjectListDimens.ListTopPadding,
                end = ProjectListDimens.ScreenHorizontalPadding,
                bottom = ProjectListDimens.ListBottomPadding,
            ),
    ) {
        if (state.isLoading) {
            item { Text(stringResource(R.string.folder_loading)) }
            return@LazyColumn
        }
        if (state.selectedFolderFilter != ProjectFolderFilter.AllProjects &&
            state.active.isEmpty() &&
            state.completed.isEmpty()
        ) {
            item {
                ProjectFolderEmptyState(
                    filter = state.selectedFolderFilter,
                    folders = state.folders,
                    hasHiddenCompletedProjects = state.hasHiddenCompletedProjects,
                    onShowCompleted = actions.onShowCompleted,
                )
            }
            return@LazyColumn
        }
        // Continue Knitting -herokortti (ei multi-select-tilassa)
        if (!state.isMultiSelectMode) {
            state.continueKnitting?.let { ck ->
                item {
                    val hasSession = ck.projectId == state.activeSessionProjectId
                    ContinueProjectCard(
                        project =
                            CounterProject(
                                id = ck.projectId,
                                name = ck.name,
                                count = ck.count,
                                sectionName = ck.sectionName,
                                targetRows = ck.targetRows,
                                craftType = ck.craftType,
                                mainCounterLabelType = ck.mainCounterLabelType,
                                mainCounterCustomLabel = ck.mainCounterCustomLabel,
                            ),
                        sessionStatus =
                            workSessionStatusText(hasSession, hasSession && state.activeSessionNeedsReview),
                        // Heron runko avaa projektinäkymän kuten projektikortit; jatka-nappi avaa laskurin.
                        onClick = { actions.onOpenOverview(ck.projectId) },
                        onClickLabel = stringResource(R.string.project_card_open_overview_action),
                        onOpenCounter = { actions.onOpenCounter(ck.projectId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (visibleActiveProjects.isNotEmpty() || !isHeroVisible) {
            item {
                // Lukumäärä sisältää myös heron projektin: se on aktiivinen, vaikka sillä ei ole omaa korttia.
                ProjectSectionLabel(
                    text = stringResource(R.string.section_active),
                    count = state.active.size,
                )
            }

            if (visibleActiveProjects.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_active_projects),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = visibleActiveProjects,
                    key = { _, project -> project.id },
                ) { index, project ->
                    ActiveProjectItem(
                        project = project,
                        state =
                            ActiveProjectItemState(
                                isMultiSelectMode = state.isMultiSelectMode,
                                isSelected = project.id in state.selectedProjectIds,
                                photoUri = state.latestPhotoUris[project.id],
                                patternName = state.patternNames[project.id],
                                hasActiveSession = project.id == state.activeSessionProjectId,
                                sessionNeedsReview =
                                    project.id == state.activeSessionProjectId && state.activeSessionNeedsReview,
                            ),
                        actions =
                            ActiveProjectItemActions(
                                onOpenOverview = actions.onOpenOverview,
                                onToggleSelection = actions.onToggleSelection,
                                onEnterMultiSelect = actions.onEnterMultiSelect,
                            ),
                    )
                    if (index < visibleActiveProjects.lastIndex) {
                        Spacer(modifier = Modifier.height(ProjectListDimens.CardSpacing))
                    }
                }
            }
        }

        // Completed-osio (näytetään vain kun toggle päällä)
        if (state.showCompleted) {
            item {
                ProjectSectionLabel(
                    text = stringResource(R.string.section_completed),
                    count = state.completed.size,
                )
            }

            if (state.completed.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_completed_projects),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(
                    items = state.completed,
                    key = { _, project -> project.id },
                ) { index, project ->
                    val completedDescription = stringResource(R.string.section_completed)
                    ProjectCard(
                        project = project.copy(count = project.totalRows ?: project.count),
                        photoUri = state.latestPhotoUris[project.id],
                        onClick = {
                            if (state.isMultiSelectMode) {
                                actions.onToggleSelection(
                                    project.id,
                                )
                            } else {
                                actions.onOpenOverview(project.id)
                            }
                        },
                        onLongClick =
                            if (state.isMultiSelectMode) {
                                null
                            } else {
                                { actions.onEnterMultiSelect(project.id) }
                            },
                        patternName = project.patternName,
                        selected = (project.id in state.selectedProjectIds).takeIf { state.isMultiSelectMode },
                        onToggleSelection = { actions.onToggleSelection(project.id) },
                        modifier = Modifier.semantics { stateDescription = completedDescription },
                    )
                    if (index < state.completed.lastIndex) {
                        Spacer(modifier = Modifier.height(ProjectListDimens.CardSpacing))
                    }
                }
            }
        }
    }
}

// Data-luokat ActiveProjectItem-parametrien ryhmittelyyn (S107)
data class ActiveProjectItemState(
    val isMultiSelectMode: Boolean,
    val isSelected: Boolean,
    val photoUri: String?,
    val patternName: String?,
    val hasActiveSession: Boolean = false,
    val sessionNeedsReview: Boolean = false,
)

data class ActiveProjectItemActions(
    val onOpenOverview: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveProjectItem(
    project: CounterProject,
    state: ActiveProjectItemState,
    actions: ActiveProjectItemActions,
) {
    ProjectCard(
        project = project,
        photoUri = state.photoUri,
        onClick = {
            if (state.isMultiSelectMode) {
                actions.onToggleSelection(project.id)
            } else {
                actions.onOpenOverview(project.id)
            }
        },
        onLongClick =
            if (state.isMultiSelectMode) {
                null
            } else {
                { actions.onEnterMultiSelect(project.id) }
            },
        patternName = state.patternName,
        selected = state.isSelected.takeIf { state.isMultiSelectMode },
        onToggleSelection = { actions.onToggleSelection(project.id) },
        statusText = workSessionStatusText(state.hasActiveSession, state.sessionNeedsReview),
    )
}

@Composable
private fun ProjectSectionLabel(
    text: String,
    count: Int,
) {
    SectionLabel(
        text = stringResource(R.string.project_section_count_format, text, count),
        modifier =
            Modifier.padding(
                top = ProjectListDimens.SectionTopSpacing,
                bottom = ProjectListDimens.SectionBottomSpacing,
            ),
    )
}

@Composable
private fun DeleteProjectDialog(
    projectName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_project),
        message = stringResource(R.string.delete_project_message, projectName),
        confirmText = stringResource(R.string.delete_project),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
