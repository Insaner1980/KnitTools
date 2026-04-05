package com.finnvek.knittools.ui.screens.project

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ProjectCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
    onProjectClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val active by viewModel.activeProjects.collectAsStateWithLifecycle()
    val completed by viewModel.completedProjects.collectAsStateWithLifecycle()

    // Context menu -tila
    var menuProjectId by remember { mutableLongStateOf(0L) }
    var menuProjectName by remember { mutableStateOf("") }
    var menuIsCompleted by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Dialogi-tilat
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    if (showRenameDialog) {
        RenameProjectDialog(
            renameText = renameText,
            onRenameTextChange = { renameText = it },
            onConfirm = {
                viewModel.renameProject(menuProjectId, renameText.trim())
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteProjectDialog(
            projectName = menuProjectName,
            onConfirm = {
                viewModel.deleteProject(menuProjectId)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    ToolScreenScaffold(
        title = stringResource(R.string.project_list_title),
        onBack = onBack,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Active-osio
                item {
                    SectionLabel(stringResource(R.string.section_active))
                }

                if (active.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_active_projects),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(active, key = { it.id }) { project ->
                        SwipeToDismissItem(
                            onDismiss = { viewModel.archiveProject(project.id) },
                            label = stringResource(R.string.archive_project),
                        ) {
                            Box {
                                ProjectCard(
                                    name = project.name,
                                    rowCount = project.count,
                                    sectionName = project.sectionName,
                                    lastUpdated = project.updatedAt,
                                    onClick = { onProjectClick(project.id) },
                                    onLongClick = {
                                        menuProjectId = project.id
                                        menuProjectName = project.name
                                        menuIsCompleted = false
                                        showMenu = true
                                    },
                                )
                                // Context menu
                                DropdownMenu(
                                    expanded = showMenu && menuProjectId == project.id && !menuIsCompleted,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename_project)) },
                                        onClick = {
                                            showMenu = false
                                            renameText = menuProjectName
                                            showRenameDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.archive_project)) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.archiveProject(menuProjectId)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.delete_project),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Completed-osio
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionLabel(stringResource(R.string.section_completed))
                }

                if (completed.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_completed_projects),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(completed, key = { it.id }) { project ->
                        SwipeToDismissItem(
                            onDismiss = {
                                menuProjectId = project.id
                                menuProjectName = project.name
                                showDeleteDialog = true
                            },
                            label = stringResource(R.string.delete_project),
                        ) {
                            Box {
                                ProjectCard(
                                    name = project.name,
                                    rowCount = project.count,
                                    sectionName = project.sectionName,
                                    lastUpdated = project.completedAt ?: project.updatedAt,
                                    onClick = { onProjectClick(project.id) },
                                    onLongClick = {
                                        menuProjectId = project.id
                                        menuProjectName = project.name
                                        menuIsCompleted = true
                                        showMenu = true
                                    },
                                    isCompleted = true,
                                    totalRows = project.totalRows,
                                )
                                // Context menu completed
                                DropdownMenu(
                                    expanded = showMenu && menuProjectId == project.id && menuIsCompleted,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename_project)) },
                                        onClick = {
                                            showMenu = false
                                            renameText = menuProjectName
                                            showRenameDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.reactivate_project)) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.reactivateProject(menuProjectId)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.delete_project),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FAB
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    onDismiss: () -> Unit,
    label: String,
    content: @Composable () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue =
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                label = "swipe_bg",
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
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
                shape = RoundedCornerShape(12.dp),
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_project)) },
        text = { Text(stringResource(R.string.delete_project_message, projectName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
