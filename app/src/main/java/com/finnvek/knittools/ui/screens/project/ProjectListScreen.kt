package com.finnvek.knittools.ui.screens.project

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ProjectCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Long) -> Unit,
    onNotesEditor: (Long) -> Unit = {},
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val active by viewModel.activeProjects.collectAsStateWithLifecycle()
    val completed by viewModel.completedProjects.collectAsStateWithLifecycle()
    val continueKnitting by viewModel.continueKnittingProject.collectAsStateWithLifecycle()
    val yarnNames by viewModel.projectYarnNames.collectAsStateWithLifecycle()
    val photoCounts by viewModel.projectPhotoCounts.collectAsStateWithLifecycle()
    val patternNames by viewModel.projectPatternNames.collectAsStateWithLifecycle()
    val hasNotes by viewModel.projectHasNotes.collectAsStateWithLifecycle()
    val showCompleted by viewModel.showCompleted.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedProjectIds by viewModel.selectedProjectIds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    // FAB-luonnin jälkeen navigoi uuteen projektiin
    LaunchedEffect(Unit) {
        viewModel.navigateToProject.collect { projectId ->
            onProjectClick(projectId)
        }
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
                        photoCounts = photoCounts,
                        patternNames = patternNames,
                        hasNotes = hasNotes,
                        showCompleted = showCompleted,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedProjectIds = selectedProjectIds,
                    ),
                actions =
                    ProjectListContentActions(
                        onProjectClick = onProjectClick,
                        onNotesClick = onNotesEditor,
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

            // FAB (ei multi-select-tilassa)
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = { viewModel.createProject() },
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_project))
                }
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
    val sortOrder: String,
    val showOverflowMenu: Boolean,
    val showSortMenu: Boolean,
)

data class ProjectListTopBarActions(
    val onExitMultiSelect: () -> Unit,
    val onSelectAll: () -> Unit,
    val onShowOverflowMenu: () -> Unit,
    val onDismissOverflowMenu: () -> Unit,
    val onEnterMultiSelect: () -> Unit,
    val onShowSortMenu: () -> Unit,
    val onDismissSortMenu: () -> Unit,
    val onToggleShowCompleted: () -> Unit,
    val onSortOrderChange: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
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
    val sortOrder: String,
)

data class OverflowMenuActions(
    val onShowOverflowMenu: () -> Unit,
    val onDismissOverflowMenu: () -> Unit,
    val onEnterMultiSelect: () -> Unit,
    val onShowSortMenu: () -> Unit,
    val onDismissSortMenu: () -> Unit,
    val onToggleShowCompleted: () -> Unit,
    val onSortOrderChange: (String) -> Unit,
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
    sortOrder: String,
    onDismiss: () -> Unit,
    onSortOrderChange: (String) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        SortMenuItem(
            label = stringResource(R.string.sort_name),
            selected = sortOrder == "name",
            onClick = { onSortOrderChange("name") },
        )
        SortMenuItem(
            label = stringResource(R.string.sort_last_updated),
            selected = sortOrder == "updated",
            onClick = { onSortOrderChange("updated") },
        )
        SortMenuItem(
            label = stringResource(R.string.sort_created_date),
            selected = sortOrder == "created",
            onClick = { onSortOrderChange("created") },
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
data class ProjectListContentState(
    val active: List<CounterProjectEntity>,
    val completed: List<CounterProjectEntity>,
    val continueKnitting: ContinueKnittingProject?,
    val yarnNames: Map<Long, String>,
    val photoCounts: Map<Long, Int>,
    val patternNames: Map<Long, String>,
    val hasNotes: Set<Long>,
    val showCompleted: Boolean,
    val isMultiSelectMode: Boolean,
    val selectedProjectIds: Set<Long>,
)

data class ProjectListContentActions(
    val onProjectClick: (Long) -> Unit,
    val onNotesClick: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
    val onArchive: (Long) -> Unit,
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Continue Knitting -herokortti (ei multi-select-tilassa)
        if (!state.isMultiSelectMode) {
            state.continueKnitting?.let { ck ->
                item {
                    ContinueKnittingCard(
                        projectName = ck.name,
                        rowCount = ck.count,
                        totalMinutes = ck.totalMinutes,
                        onClick = { actions.onProjectClick(ck.projectId) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (visibleActiveProjects.isNotEmpty() || !isHeroVisible) {
            item {
                SectionLabel(stringResource(R.string.section_active))
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
                items(visibleActiveProjects, key = { it.id }) { project ->
                    ActiveProjectItem(
                        project = project,
                        state =
                            ActiveProjectItemState(
                                isMultiSelectMode = state.isMultiSelectMode,
                                isSelected = project.id in state.selectedProjectIds,
                                yarnName = state.yarnNames[project.id],
                                photoCount = state.photoCounts[project.id] ?: 0,
                                patternName = state.patternNames[project.id],
                                hasNotes = project.id in state.hasNotes,
                            ),
                        actions =
                            ActiveProjectItemActions(
                                onProjectClick = actions.onProjectClick,
                                onNotesClick = actions.onNotesClick,
                                onToggleSelection = actions.onToggleSelection,
                                onEnterMultiSelect = actions.onEnterMultiSelect,
                                onArchive = actions.onArchive,
                            ),
                    )
                }
            }
        }

        // Completed-osio (näytetään vain kun toggle päällä)
        if (state.showCompleted) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.section_completed))
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
                items(state.completed, key = { it.id }) { project ->
                    ProjectCard(
                        name = project.name,
                        rowCount = project.count,
                        sectionName = project.sectionName,
                        lastUpdated = project.completedAt ?: project.updatedAt,
                        onClick = { actions.onProjectClick(project.id) },
                        onLongClick = { actions.onDeleteSwipe(project.id, project.name) },
                        totalRows = project.totalRows,
                    )
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
    val photoCount: Int,
    val patternName: String?,
    val hasNotes: Boolean = false,
)

data class ActiveProjectItemActions(
    val onProjectClick: (Long) -> Unit,
    val onNotesClick: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onEnterMultiSelect: (Long) -> Unit,
    val onArchive: (Long) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveProjectItem(
    project: CounterProjectEntity,
    state: ActiveProjectItemState,
    actions: ActiveProjectItemActions,
) {
    if (state.isMultiSelectMode) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                MaterialTheme.shapes.large,
                            )
                        } else {
                            Modifier
                        },
                    ).clickable { actions.onToggleSelection(project.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.isSelected,
                onCheckedChange = { actions.onToggleSelection(project.id) },
            )
            ProjectCard(
                name = project.name,
                rowCount = project.count,
                sectionName = project.sectionName,
                lastUpdated = project.updatedAt,
                onClick = { actions.onToggleSelection(project.id) },
                modifier = Modifier.weight(1f),
                yarnName = state.yarnName,
                yarnColorSeed = project.id,
                photoCount = state.photoCount,
                patternName = state.patternName,
            )
        }
    } else {
        ProjectCard(
            name = project.name,
            rowCount = project.count,
            sectionName = project.sectionName,
            lastUpdated = project.updatedAt,
            onClick = { actions.onProjectClick(project.id) },
            onLongClick = { actions.onEnterMultiSelect(project.id) },
            yarnName = state.yarnName,
            yarnColorSeed = project.id,
            photoCount = state.photoCount,
            patternName = state.patternName,
            hasNotes = state.hasNotes,
            onNotesClick = { actions.onNotesClick(project.id) },
        )
    }
}

@Composable
private fun ContinueKnittingCard(
    projectName: String,
    rowCount: Int,
    totalMinutes: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.continue_knitting),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.rows_format, rowCount) + " · " + formatMinutes(totalMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun formatMinutes(minutes: Int): String =
    when {
        minutes < 60 -> stringResource(R.string.time_spent_minutes_format, minutes)
        else -> stringResource(R.string.session_duration_format, minutes / 60, minutes % 60)
    }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun RenameProjectDialog(
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
            TextButton(onClick = onConfirm, enabled = renameText.isNotBlank()) {
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
