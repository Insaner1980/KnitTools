package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.FolderNameValidationError
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderNameValidationResult
import com.finnvek.knittools.domain.model.validateProjectFolderName
import com.finnvek.knittools.ui.theme.ProjectListDimens

private val FolderActionMinHeight = ProjectListDimens.FooterActionTouchSize

@Composable
fun ProjectFolderSelector(
    selectedFilter: ProjectFolderFilter,
    folders: List<ProjectFolder>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val label = selectedFolderLabel(selectedFilter, folders)
    val description = selectedFilterDescription(selectedFilter, folders, selected = true)
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .then(
                    focusRequester?.let {
                        Modifier
                            .focusRequester(it)
                            .focusProperties { canFocus = true }
                    } ?: Modifier,
                ).heightIn(min = FolderActionMinHeight)
                .semantics {
                    contentDescription = description
                    selected = true
                    role = Role.Button
                },
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFoldersSheet(
    folders: List<ProjectFolder>,
    selectedFilter: ProjectFolderFilter,
    isLoading: Boolean,
    errorMessage: String?,
    isMutating: Boolean,
    onSelectFilter: (ProjectFolderFilter) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (Long) -> Unit,
    onMoveEarlier: (Long) -> Unit,
    onMoveLater: (Long) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    focusFolderId: Long? = null,
    focusCreateFolder: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createFocusRequester = remember { FocusRequester() }
    val folderFocusRequesters = remember { mutableStateMapOf<Long, FocusRequester>() }

    LaunchedEffect(focusFolderId, focusCreateFolder) {
        withFrameNanos { }
        when {
            focusCreateFolder -> createFocusRequester.requestFocus()
            focusFolderId != null -> folderFocusRequesters[focusFolderId]?.requestFocus()
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!isMutating) onDismiss() }, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = stringResource(R.string.folder_sheet_title), style = MaterialTheme.typography.titleLarge)
            FolderFilterRow(
                filter = ProjectFolderFilter.AllProjects,
                folders = folders,
                selected = selectedFilter is ProjectFolderFilter.AllProjects,
                enabled = !isMutating,
                onClick = { onSelectFilter(ProjectFolderFilter.AllProjects) },
            )
            FolderFilterRow(
                filter = ProjectFolderFilter.Unfiled,
                folders = folders,
                selected = selectedFilter is ProjectFolderFilter.Unfiled,
                enabled = !isMutating,
                onClick = { onSelectFilter(ProjectFolderFilter.Unfiled) },
            )

            when {
                isLoading -> Text(stringResource(R.string.folder_loading))
                folders.isEmpty() -> Text(stringResource(R.string.folder_no_folders))
                else ->
                    folders.forEachIndexed { index, folder ->
                        key(folder.id) {
                            val focusRequester =
                                remember(folder.id) {
                                    folderFocusRequesters.getOrPut(folder.id) { FocusRequester() }
                                }
                            UserFolderRow(
                                folder = folder,
                                isSelected =
                                    selectedFilter is ProjectFolderFilter.Folder &&
                                        selectedFilter.folderId == folder.id,
                                canMoveEarlier = index > 0,
                                canMoveLater = index < folders.lastIndex,
                                enabled = !isMutating,
                                focusRequester = focusRequester,
                                onSelect = { onSelectFilter(ProjectFolderFilter.Folder(folder.id)) },
                                onRename = { onRenameFolder(folder.id) },
                                onMoveEarlier = { onMoveEarlier(folder.id) },
                                onMoveLater = { onMoveLater(folder.id) },
                                onDelete = { onDeleteFolder(folder.id) },
                            )
                        }
                    }
            }

            errorMessage?.let { message ->
                FolderError(message = message, onRetry = onRetry, enabled = !isMutating)
            }
            HorizontalDivider()
            TextButton(
                onClick = onCreateFolder,
                enabled = !isMutating,
                modifier =
                    Modifier
                        .focusRequester(createFocusRequester)
                        .focusProperties { canFocus = true }
                        .heightIn(min = FolderActionMinHeight),
            ) {
                Text(stringResource(R.string.folder_create))
            }
        }
    }
}

@Composable
fun ProjectFolderNameDialog(
    folderId: Long?,
    initialName: String,
    errorMessage: String?,
    isSaving: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit = {},
) {
    var name by rememberSaveable(folderId) { mutableStateOf(initialName) }
    val validation = validateProjectFolderName(name)
    val validationMessage = validationErrorMessage(validation)
    val displayedError = validationMessage ?: errorMessage
    val validName = (validation as? ProjectFolderNameValidationResult.Valid)?.name
    val canConfirm = validName != null && errorMessage == null && !isSaving
    val isRename = folderId != null

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(if (isRename) R.string.folder_rename else R.string.folder_create)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 320.dp)
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onClearError()
                    },
                    singleLine = true,
                    isError = displayedError != null,
                    label = { Text(stringResource(R.string.folder_name)) },
                    supportingText = displayedError?.let { { Text(it) } },
                    keyboardOptions =
                        androidx.compose.foundation.text
                            .KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        androidx.compose.foundation.text.KeyboardActions(
                            onDone = { validName?.takeIf { canConfirm }?.let(onConfirm) },
                        ),
                    shape = MaterialTheme.shapes.large,
                    colors = folderTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validName?.let(onConfirm) },
                enabled = canConfirm,
                modifier = Modifier.heightIn(min = FolderActionMinHeight),
            ) {
                Text(stringResource(if (isRename) R.string.save else R.string.folder_create))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.heightIn(min = FolderActionMinHeight),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun DeleteProjectFolderDialog(
    folder: ProjectFolder,
    assignedProjectCount: Int,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.folder_delete_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.folder_delete_message, folder.name),
                )
                Text(
                    pluralStringResource(
                        R.plurals.folder_affected_projects,
                        assignedProjectCount,
                        assignedProjectCount,
                    ),
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = FolderActionMinHeight),
            ) {
                Text(stringResource(R.string.folder_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.heightIn(min = FolderActionMinHeight),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    projectCount: Int,
    currentFolderId: Long?,
    hasCommonDestination: Boolean,
    folders: List<ProjectFolder>,
    isLoading: Boolean,
    errorMessage: String?,
    isMoving: Boolean,
    onMoveToFolder: (Long?) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    fallbackFocusRequester: FocusRequester? = null,
    projectName: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(isLoading, folders, sheetState) {
        if (!isLoading) {
            // Kohdista ladatun sisällön uusi ankkuri myös ilman järjestelmäanimaatioita.
            withFrameNanos { }
            sheetState.expand()
        }
    }
    val title =
        when {
            projectCount == 1 && projectName != null ->
                stringResource(R.string.folder_move_project_to, projectName)
            projectCount == 1 -> stringResource(R.string.folder_move_to)
            else ->
                pluralStringResource(
                    R.plurals.folder_move_selected_projects_count,
                    projectCount,
                    projectCount,
                )
        }
    ModalBottomSheet(onDismissRequest = { if (!isMoving) onDismiss() }, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    fallbackFocusRequester?.let { Modifier.focusRequester(it).focusable() } ?: Modifier,
            )
            DestinationRow(
                label = stringResource(R.string.folder_unfiled),
                description = stringResource(R.string.folder_unfiled_description),
                onClickLabel = stringResource(R.string.folder_remove_from),
                selected = hasCommonDestination && currentFolderId == null,
                enabled = !isMoving,
                onClick = { onMoveToFolder(null) },
            )
            when {
                isLoading -> Text(stringResource(R.string.folder_loading))
                folders.isEmpty() -> Text(stringResource(R.string.folder_no_folders))
                else ->
                    folders.forEach { folder ->
                        DestinationRow(
                            label = folder.name,
                            description = stringResource(R.string.folder_user_description, folder.name),
                            selected = hasCommonDestination && currentFolderId == folder.id,
                            enabled = !isMoving,
                            onClick = { onMoveToFolder(folder.id) },
                        )
                    }
            }
            errorMessage?.let { message ->
                FolderError(message = message, onRetry = onRetry, enabled = !isMoving)
            }
        }
    }
}

@Composable
fun ProjectFolderEmptyState(
    filter: ProjectFolderFilter,
    folders: List<ProjectFolder>,
    hasHiddenCompletedProjects: Boolean,
    onShowCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filter is ProjectFolderFilter.AllProjects) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
    ) {
        if (hasHiddenCompletedProjects) {
            Text(stringResource(R.string.folder_hidden_completed))
            TextButton(onClick = onShowCompleted, modifier = Modifier.heightIn(min = FolderActionMinHeight)) {
                Text(stringResource(R.string.show_completed))
            }
        } else {
            val message =
                when (filter) {
                    ProjectFolderFilter.Unfiled -> stringResource(R.string.folder_no_unfiled)
                    is ProjectFolderFilter.Folder -> {
                        val folder = folders.firstOrNull { it.id == filter.folderId }
                        stringResource(R.string.folder_empty, folder?.name.orEmpty())
                    }
                    ProjectFolderFilter.AllProjects -> error("Handled above")
                }
            Text(message)
        }
    }
}

@Composable
private fun FolderFilterRow(
    filter: ProjectFolderFilter,
    folders: List<ProjectFolder>,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = selectedFolderLabel(filter, folders)
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FolderActionMinHeight)
                .folderSelectionSemantics(selectedFilterDescription(filter, folders, selected), selected),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun UserFolderRow(
    folder: ProjectFolder,
    isSelected: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val description = selectedFilterDescription(ProjectFolderFilter.Folder(folder.id), listOf(folder), isSelected)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onSelect,
            enabled = enabled,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = true }
                    .heightIn(min = FolderActionMinHeight)
                    .folderSelectionSemantics(description, isSelected),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = folder.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSelected) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                    )
                }
            }
        }
        Box {
            androidx.compose.material3.IconButton(
                onClick = { menuExpanded = true },
                enabled = enabled,
                modifier =
                    Modifier
                        .heightIn(min = FolderActionMinHeight)
                        .widthIn(min = FolderActionMinHeight),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.folder_actions_description, folder.name),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                FolderMenuItem(
                    label = R.string.folder_rename,
                    folderName = folder.name,
                    enabled = enabled,
                ) {
                    menuExpanded = false
                    onRename()
                }
                FolderMenuItem(
                    label = R.string.folder_move_earlier,
                    folderName = folder.name,
                    enabled = enabled && canMoveEarlier,
                ) {
                    menuExpanded = false
                    onMoveEarlier()
                }
                FolderMenuItem(
                    label = R.string.folder_move_later,
                    folderName = folder.name,
                    enabled = enabled && canMoveLater,
                ) {
                    menuExpanded = false
                    onMoveLater()
                }
                FolderMenuItem(
                    label = R.string.folder_delete,
                    folderName = folder.name,
                    enabled = enabled,
                ) {
                    menuExpanded = false
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun FolderMenuItem(
    label: Int,
    folderName: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val labelText = stringResource(label)
    val contentDescriptionText =
        stringResource(
            when (label) {
                R.string.folder_rename -> R.string.folder_rename_description
                R.string.folder_move_earlier -> R.string.folder_move_earlier_description
                R.string.folder_move_later -> R.string.folder_move_later_description
                R.string.folder_delete -> R.string.folder_delete_description
                else -> error("Unsupported folder action")
            },
            folderName,
        )
    DropdownMenuItem(
        text = { Text(labelText) },
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = contentDescriptionText },
    )
}

@Composable
private fun DestinationRow(
    label: String,
    description: String,
    onClickLabel: String? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val selectedDescription =
        if (selected) stringResource(R.string.folder_current_destination, label) else description
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FolderActionMinHeight)
                .folderSelectionSemantics(selectedDescription, selected)
                .semantics {
                    onClickLabel?.let { onClick(label = it, action = null) }
                },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun FolderError(
    message: String,
    onRetry: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
    TextButton(onClick = onRetry, enabled = enabled, modifier = Modifier.heightIn(min = FolderActionMinHeight)) {
        Text(stringResource(R.string.retry))
    }
}

private fun Modifier.folderSelectionSemantics(
    description: String,
    selected: Boolean,
): Modifier =
    semantics {
        contentDescription = description
        this.selected = selected
        role = Role.Button
    }

@Composable
private fun selectedFolderLabel(
    filter: ProjectFolderFilter,
    folders: List<ProjectFolder>,
): String =
    when (filter) {
        ProjectFolderFilter.AllProjects -> stringResource(R.string.all_projects)
        ProjectFolderFilter.Unfiled -> stringResource(R.string.folder_unfiled)
        is ProjectFolderFilter.Folder -> folders.firstOrNull { it.id == filter.folderId }?.name.orEmpty()
    }

@Composable
private fun selectedFilterDescription(
    filter: ProjectFolderFilter,
    folders: List<ProjectFolder>,
    selected: Boolean,
): String {
    val base =
        when (filter) {
            ProjectFolderFilter.AllProjects -> stringResource(R.string.folder_all_projects_description)
            ProjectFolderFilter.Unfiled -> stringResource(R.string.folder_unfiled_description)
            is ProjectFolderFilter.Folder ->
                stringResource(
                    R.string.folder_user_description,
                    folders.firstOrNull { it.id == filter.folderId }?.name.orEmpty(),
                )
        }
    return if (selected) stringResource(R.string.folder_selected_description, base) else base
}

@Composable
private fun validationErrorMessage(validation: ProjectFolderNameValidationResult): String? =
    when (validation) {
        is ProjectFolderNameValidationResult.Valid -> null
        is ProjectFolderNameValidationResult.Invalid ->
            when (validation.error) {
                FolderNameValidationError.REQUIRED -> stringResource(R.string.folder_name_required)
                FolderNameValidationError.TOO_LONG -> stringResource(R.string.folder_name_max_length)
                FolderNameValidationError.CONTROL_CHARACTER -> stringResource(R.string.folder_name_invalid)
            }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun folderTextFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
