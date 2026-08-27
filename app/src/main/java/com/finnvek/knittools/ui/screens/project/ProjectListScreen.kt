package com.finnvek.knittools.ui.screens.project

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.MainCounterTargetStatus
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectDetailsDialog
import com.finnvek.knittools.ui.components.ProjectDetailsValues
import com.finnvek.knittools.ui.components.ProjectListItem
import com.finnvek.knittools.ui.components.RenameProjectDialog
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterTargetFraction
import com.finnvek.knittools.ui.components.mainCounterTargetStatus
import com.finnvek.knittools.ui.components.mainCounterTargetText
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.ProjectListDimens

private enum class PendingProjectProAction {
    OpenCreation,
    RetryCreation,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Long) -> Unit,
    onNotesEditor: (Long) -> Unit = {},
    onPhotoGallery: (Long) -> Unit = {},
    onPatternViewer: (Long) -> Unit = {},
    onYarnCard: (Long) -> Unit = {},
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
    val yarnNames by viewModel.projectYarnNames.collectAsStateWithLifecycle()
    val yarnCardIds by viewModel.projectYarnCardIds.collectAsStateWithLifecycle()
    val photoCounts by viewModel.projectPhotoCounts.collectAsStateWithLifecycle()
    val patternNames by viewModel.projectPatternNames.collectAsStateWithLifecycle()
    val projectIdsWithDocuments by viewModel.projectIdsWithDocuments.collectAsStateWithLifecycle()
    val projectIdsWithAvailablePrimary by viewModel.projectIdsWithAvailablePrimary.collectAsStateWithLifecycle()
    val hasNotes by viewModel.projectHasNotes.collectAsStateWithLifecycle()
    val showCompleted by viewModel.showCompleted.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedProjectIds by viewModel.selectedProjectIds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var showCreateProjectDialog by rememberSaveable { mutableStateOf(false) }
    var pendingProAction by rememberSaveable { mutableStateOf<PendingProjectProAction?>(null) }
    var projectPromptCount by rememberSaveable { mutableIntStateOf(0) }
    // Luonnin jälkeen navigoi uuteen projektiin
    CollectWithLifecycleEffect({ viewModel.navigateToProject }) { projectId ->
        showCreateProjectDialog = false
        pendingProAction = null
        onProjectClick(projectId)
    }

    CollectWithLifecycleEffect({ viewModel.projectCreationPrompts }) { projectCount ->
        projectPromptCount = projectCount
        pendingProAction =
            if (showCreateProjectDialog) {
                PendingProjectProAction.RetryCreation
            } else {
                PendingProjectProAction.OpenCreation
            }
    }

    CollectWithLifecycleEffect({ viewModel.navigateToNotesEditor }) { projectId ->
        onNotesEditor(projectId)
    }

    CollectWithLifecycleEffect({ viewModel.navigateToPhotoGallery }) { projectId ->
        onPhotoGallery(projectId)
    }

    CollectWithLifecycleEffect({ viewModel.showCreateProjectDialog }) {
        showCreateProjectDialog = true
    }

    pendingProAction?.let { action ->
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.Projects,
                    existingProjectCount = projectPromptCount,
                ),
            onDismiss = { pendingProAction = null },
            onTrialStarted = {
                pendingProAction = null
                when (action) {
                    PendingProjectProAction.OpenCreation -> showCreateProjectDialog = true
                    PendingProjectProAction.RetryCreation -> viewModel.retryPendingProjectCreation()
                }
            },
            onSeePro = onUpgradeToPro,
        )
    }

    // Multi-select back handler
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
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
        ProjectListActiveSessionCompletionDialog(
            onSave = { viewModel.resolvePendingCompletion(saveSession = true) },
            onDiscard = { viewModel.resolvePendingCompletion(saveSession = false) },
            onCancel = viewModel::cancelPendingCompletion,
        )
    }

    if (pendingDeletionSessionAction != null) {
        ProjectListActiveSessionDeletionDialog(
            onDiscardAndDelete = viewModel::resolvePendingDeletion,
            onCancel = viewModel::cancelPendingDeletion,
        )
    }

    if (showCreateProjectDialog) {
        ProjectDetailsDialog(
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
                )
            },
            onDismiss = { showCreateProjectDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            )
        },
        bottomBar = {
            MultiSelectBottomBar(
                isMultiSelectMode = isMultiSelectMode,
                hasSelection = selectedProjectIds.isNotEmpty(),
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
                        yarnNames = yarnNames,
                        yarnCardIds = yarnCardIds,
                        photoCounts = photoCounts,
                        patternNames = patternNames,
                        projectIdsWithDocuments = projectIdsWithDocuments,
                        projectIdsWithAvailablePrimary = projectIdsWithAvailablePrimary,
                        hasNotes = hasNotes,
                        showCompleted = showCompleted,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedProjectIds = selectedProjectIds,
                        activeSessionProjectId = activeSession?.projectId,
                        activeSessionNeedsReview = activeSession?.needsRecoveryReview == true,
                    ),
                actions =
                    ProjectListContentActions(
                        onProjectClick = onProjectClick,
                        onNotesClick = viewModel::openNotesEditor,
                        onPhotoGallery = viewModel::openPhotoGallery,
                        onPatternViewer = onPatternViewer,
                        onYarnCard = onYarnCard,
                        onToggleSelection = { viewModel.toggleProjectSelection(it) },
                        onEnterMultiSelect = { viewModel.enterMultiSelectMode(it) },
                        onArchive = { viewModel.archiveProject(it) },
                        onDeleteSwipe = { id, name ->
                            menuProjectId = id
                            menuProjectName = name
                            showDeleteDialog = true
                        },
                    ),
            )

            // Luontipainike ei näy multi-select-tilassa.
            if (!isMultiSelectMode) {
                CounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    contentDescription = stringResource(R.string.new_project),
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = viewModel::requestProjectCreation,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(ProjectListDimens.CreateButtonTouchSize),
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
            selectedCount = state.selectedCount,
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
private fun ProjectListActiveSessionCompletionDialog(
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
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ProjectListActiveSessionDeletionDialog(
    onDiscardAndDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.work_session_delete_project_title)) },
        text = { Text(stringResource(R.string.work_session_delete_project_body)) },
        confirmButton = {
            TextButton(
                onClick = onDiscardAndDelete,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.work_session_discard_and_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun MultiCompleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.complete_project)) },
        text = { Text(stringResource(R.string.complete_n_projects, selectedCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.complete_project))
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
private fun MultiDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_project),
        message = stringResource(R.string.delete_n_projects, selectedCount),
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
) {
    TopAppBar(
        title = {
            Text(
                text =
                    if (state.isMultiSelectMode) {
                        stringResource(R.string.n_selected, state.selectedCount)
                    } else {
                        stringResource(R.string.project_list_title)
                    },
                style = MaterialTheme.typography.headlineMedium,
            )
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
            if (state.isMultiSelectMode) {
                TextButton(onClick = actions.onSelectAll) {
                    Text(stringResource(R.string.select_all))
                }
            } else {
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
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    AnimatedVisibility(
        visible = isMultiSelectMode && hasSelection,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.complete_project))
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(R.string.delete_project))
                }
            }
        }
    }
}

// Data-luokat ProjectListContent-parametrien ryhmittelyyn (S107)
@Immutable
data class ProjectListContentState(
    val active: List<CounterProject>,
    val completed: List<CounterProject>,
    val continueKnitting: ContinueKnittingProject?,
    val yarnNames: Map<Long, String>,
    val yarnCardIds: Map<Long, Long>,
    val photoCounts: Map<Long, Int>,
    val patternNames: Map<Long, String>,
    val projectIdsWithDocuments: Set<Long>,
    val projectIdsWithAvailablePrimary: Set<Long>,
    val hasNotes: Set<Long>,
    val showCompleted: Boolean,
    val isMultiSelectMode: Boolean,
    val selectedProjectIds: Set<Long>,
    val activeSessionProjectId: Long?,
    val activeSessionNeedsReview: Boolean,
)

// CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
data class ProjectListContentActions(
    val onProjectClick: (Long) -> Unit,
    val onNotesClick: (Long) -> Unit,
    val onPhotoGallery: (Long) -> Unit,
    val onPatternViewer: (Long) -> Unit,
    val onYarnCard: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
    val onArchive: (Long) -> Unit,
    // CPD-ON
    val onDeleteSwipe: (Long, String) -> Unit,
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
        // Continue Knitting -herokortti (ei multi-select-tilassa)
        if (!state.isMultiSelectMode) {
            state.continueKnitting?.let { ck ->
                item {
                    ContinueKnittingCard(
                        state =
                            ContinueKnittingCardState(
                                projectName = ck.name,
                                rowCount = ck.count,
                                sectionName = ck.sectionName,
                                targetRows = ck.targetRows,
                                craftType = ck.craftType,
                                mainCounterLabelType = ck.mainCounterLabelType,
                                mainCounterCustomLabel = ck.mainCounterCustomLabel,
                                hasActiveSession = ck.projectId == state.activeSessionProjectId,
                                sessionNeedsReview =
                                    ck.projectId == state.activeSessionProjectId && state.activeSessionNeedsReview,
                            ),
                        onClick = { actions.onProjectClick(ck.projectId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (visibleActiveProjects.isNotEmpty() || !isHeroVisible) {
            item {
                SectionLabel(
                    text = stringResource(R.string.section_active),
                    count = visibleActiveProjects.size,
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
                                yarnName = state.yarnNames[project.id],
                                firstYarnCardId = state.yarnCardIds[project.id],
                                photoCount = state.photoCounts[project.id] ?: 0,
                                patternName = state.patternNames[project.id],
                                hasPatternAttachment = project.id in state.projectIdsWithAvailablePrimary,
                                hasNotes = project.id in state.hasNotes,
                                hasActiveSession = project.id == state.activeSessionProjectId,
                                sessionNeedsReview =
                                    project.id == state.activeSessionProjectId && state.activeSessionNeedsReview,
                            ),
                        actions =
                            ActiveProjectItemActions(
                                onProjectClick = actions.onProjectClick,
                                onNotesClick = actions.onNotesClick,
                                onPhotoGallery = actions.onPhotoGallery,
                                onPatternViewer = actions.onPatternViewer,
                                onYarnCard = actions.onYarnCard,
                                onToggleSelection = actions.onToggleSelection,
                                onEnterMultiSelect = actions.onEnterMultiSelect,
                                onArchive = actions.onArchive,
                            ),
                    )
                    if (index < visibleActiveProjects.lastIndex) {
                        ProjectListDivider()
                    }
                }
            }
        }

        // Completed-osio (näytetään vain kun toggle päällä)
        if (state.showCompleted) {
            item {
                SectionLabel(
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
                    ProjectListItem(
                        project = project.copy(count = project.totalRows ?: project.count),
                        lastUpdated = project.completedAt ?: project.updatedAt,
                        onClick = { actions.onProjectClick(project.id) },
                        onLongClick = { actions.onDeleteSwipe(project.id, project.name) },
                        patternName = project.patternName,
                    )
                    if (index < state.completed.lastIndex) {
                        ProjectListDivider()
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
    val yarnName: String?,
    val firstYarnCardId: Long?,
    val photoCount: Int,
    val patternName: String?,
    val hasPatternAttachment: Boolean,
    val hasNotes: Boolean = false,
    val hasActiveSession: Boolean = false,
    val sessionNeedsReview: Boolean = false,
)

data class ActiveProjectItemActions(
    val onProjectClick: (Long) -> Unit,
    val onNotesClick: (Long) -> Unit,
    val onPhotoGallery: (Long) -> Unit,
    val onPatternViewer: (Long) -> Unit,
    val onYarnCard: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
    val onArchive: (Long) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveProjectItem(
    project: CounterProject,
    state: ActiveProjectItemState,
    actions: ActiveProjectItemActions,
) {
    ProjectListItem(
        project = project,
        lastUpdated = project.updatedAt,
        onClick = {
            if (state.isMultiSelectMode) {
                actions.onToggleSelection(project.id)
            } else {
                actions.onProjectClick(project.id)
            }
        },
        onLongClick =
            if (state.isMultiSelectMode) {
                null
            } else {
                { actions.onEnterMultiSelect(project.id) }
            },
        yarnName = state.yarnName,
        photoCount = state.photoCount,
        patternName = state.patternName,
        hasPatternAttachment = state.hasPatternAttachment,
        hasNotes = state.hasNotes,
        onPatternClick = { actions.onPatternViewer(project.id) },
        onNotesClick = { actions.onNotesClick(project.id) },
        onPhotosClick = { actions.onPhotoGallery(project.id) },
        onYarnClick =
            state.firstYarnCardId?.let { yarnCardId ->
                { actions.onYarnCard(yarnCardId) }
            },
        selected = state.isSelected.takeIf { state.isMultiSelectMode },
        onToggleSelection = { actions.onToggleSelection(project.id) },
        statusText =
            if (state.hasActiveSession) {
                stringResource(
                    if (state.sessionNeedsReview) {
                        R.string.work_session_recovery_needed
                    } else {
                        R.string.work_session_active
                    },
                )
            } else {
                null
            },
    )
}

private data class ContinueKnittingCardState(
    val projectName: String,
    val rowCount: Int,
    val sectionName: String?,
    val targetRows: Int?,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
    val hasActiveSession: Boolean,
    val sessionNeedsReview: Boolean,
)

@Composable
private fun ContinueKnittingCard(
    state: ContinueKnittingCardState,
    onClick: () -> Unit,
) {
    val mainCounterDisplay =
        CounterValueFormatter.forMainCounter(
            CounterProject(
                name = state.projectName,
                count = state.rowCount,
                targetRows = state.targetRows,
                craftType = state.craftType,
                mainCounterLabelType = state.mainCounterLabelType,
                mainCounterCustomLabel = state.mainCounterCustomLabel,
            ),
        )
    val targetStatus = mainCounterTargetStatus(mainCounterDisplay.targetLine)
    val progressFraction = mainCounterTargetFraction(mainCounterDisplay.targetLine)
    val countText =
        mainCounterDisplay.targetLine?.let { mainCounterTargetText(it) }
            ?: mainCounterCountText(mainCounterDisplay.projectCardCount)
    val sectionName = normalizedContinueKnittingSectionName(state.sectionName)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(ProjectListDimens.HeroPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.continue_knitting).localizedUppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.projectName,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (state.hasActiveSession) {
                    Text(
                        text =
                            stringResource(
                                if (state.sessionNeedsReview) {
                                    R.string.work_session_recovery_needed
                                } else {
                                    R.string.work_session_active
                                },
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                sectionName?.let {
                    Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(ProjectListDimens.ProgressGroupTopGap))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    targetStatus?.let {
                        Text(
                            text = continueKnittingTargetStatusText { it },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (progressFraction != null) {
                    Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ProjectListDimens.ProgressTrackInset)
                                .height(ProjectListDimens.ProgressTrackHeight)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = ProjectListDimens.ProgressTrackAlpha,
                                    ),
                                ),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(ProjectListDimens.ProgressTrackHeight)
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            CounterImageButton(
                imageRes = R.drawable.counter_continue_button,
                contentDescription =
                    stringResource(R.string.project_continue_content_description, state.projectName),
                visualSize = ProjectListDimens.HeroActionVisualSize,
                onClick = onClick,
                modifier = Modifier.size(ProjectListDimens.HeroActionTouchSize),
            )
        }
    }
}

internal fun normalizedContinueKnittingSectionName(sectionName: String?): String? =
    sectionName?.trim()?.takeIf(String::isNotEmpty)

@Composable
private fun continueKnittingTargetStatusText(statusProvider: @Composable () -> MainCounterTargetStatus): String {
    val status = statusProvider()
    return when (status) {
        is MainCounterTargetStatus.Remaining ->
            stringResource(
                R.string.counter_target_remaining_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
        MainCounterTargetStatus.Reached -> stringResource(R.string.counter_target_reached)
        is MainCounterTargetStatus.Past ->
            stringResource(
                R.string.counter_target_past_format,
                formatIntegerForDisplay(status.countSlot.count.toLong(), rememberCurrentLocale()),
            )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    count: Int,
) {
    Text(
        text =
            stringResource(
                R.string.project_section_count_format,
                text.localizedUppercase(),
                count,
            ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier =
            Modifier.padding(
                top = ProjectListDimens.SectionTopSpacing,
                bottom = ProjectListDimens.SectionBottomSpacing,
            ),
    )
}

@Composable
private fun ProjectListDivider() {
    HorizontalDivider(
        thickness = ProjectListDimens.DividerThickness,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ProjectListDimens.DividerAlpha),
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
