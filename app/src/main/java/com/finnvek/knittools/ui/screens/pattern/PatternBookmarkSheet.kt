package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PATTERN_BOOKMARK_NAME_MAX_LENGTH
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.layout.AdaptiveDropdownMenu
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens

internal data class PatternBookmarkSheetActions(
    val onDismiss: () -> Unit,
    val onAdd: (String, () -> Unit) -> Unit,
    val onJump: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onRename: (Long, String, () -> Unit) -> Unit,
    val onDelete: (Long) -> Unit,
    val onClearError: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatternBookmarkSheet(
    state: PatternBookmarkUiState,
    totalPages: Int,
    actions: PatternBookmarkSheetActions,
    canAddAtCurrentLocation: Boolean = true,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var renameBookmarkId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteBookmarkId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetState = rememberExpandedModalSheetState()
    val errorDescription = state.error?.let { stringResource(it.messageRes()) }
    val renameBookmark = state.bookmarks.firstOrNull { it.id == renameBookmarkId }
    val deleteBookmark = state.bookmarks.firstOrNull { it.id == deleteBookmarkId }
    val canAdd = canAddAtCurrentLocation && totalPages > 0 && state.documentKey != null && !state.isMutating

    ModalBottomSheet(
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveModalWindow()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Sama otsikko ja oikean reunan lisäystoiminto kuin muistutuksissa ja projektinäkymän osioissa.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SheetTitle(text = stringResource(R.string.pattern_bookmarks), modifier = Modifier.weight(1f))
                OverviewTextAction(
                    label = stringResource(R.string.pattern_bookmark_add_here),
                    onClick = { showAddDialog = true },
                    enabled = canAdd,
                )
            }
            errorDescription?.takeUnless { showAddDialog || renameBookmark != null }?.let { description ->
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.semantics {
                            contentDescription = description
                        },
                )
                TextButton(
                    onClick = actions.onClearError,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
            when {
                state.isLoading -> {
                    Text(stringResource(R.string.pattern_bookmark_loading))
                }

                state.bookmarks.isEmpty() -> {
                    Text(stringResource(R.string.pattern_bookmark_empty))
                }

                else -> {
                    state.bookmarks.forEach { bookmark ->
                        key(bookmark.id) {
                            PatternBookmarkRow(
                                bookmark = bookmark,
                                totalPages = totalPages,
                                selected = bookmark.id == state.selectedBookmarkId,
                                onJump = { actions.onJump(bookmark.id) },
                                onRename = { renameBookmarkId = bookmark.id },
                                onDelete = { deleteBookmarkId = bookmark.id },
                            )
                        }
                    }
                }
            }
            if (state.bookmarks.size >= 2) {
                HorizontalDivider()
                PatternBookmarkNavigation(state, actions)
            }
        }
    }

    if (showAddDialog) {
        PatternBookmarkNameDialog(
            title = stringResource(R.string.pattern_bookmark_add_title),
            initialName = "",
            isSaving = state.isMutating,
            failure = errorDescription,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                actions.onAdd(name) { showAddDialog = false }
            },
        )
    }
    renameBookmark?.let { bookmark ->
        key(bookmark.id) {
            PatternBookmarkNameDialog(
                title = stringResource(R.string.pattern_bookmark_rename_title),
                initialName = bookmark.name,
                isSaving = state.isMutating,
                failure = errorDescription,
                onDismiss = { renameBookmarkId = null },
                onConfirm = { name ->
                    actions.onRename(bookmark.id, name) { renameBookmarkId = null }
                },
            )
        }
    }
    deleteBookmark?.let { bookmark ->
        ConfirmationDialog(
            title = stringResource(R.string.pattern_bookmark_delete_title),
            message = stringResource(R.string.pattern_bookmark_delete_message, bookmark.name),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                actions.onDelete(bookmark.id)
                deleteBookmarkId = null
            },
            onDismiss = { deleteBookmarkId = null },
        )
    }
}

@Composable
private fun PatternBookmarkNavigation(
    state: PatternBookmarkUiState,
    actions: PatternBookmarkSheetActions,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = actions.onPrevious,
            shape = MaterialTheme.shapes.small,
            enabled = state.canGoPrevious && !state.isMutating,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.pattern_bookmark_previous))
        }
        TextButton(
            onClick = actions.onNext,
            shape = MaterialTheme.shapes.small,
            enabled = state.canGoNext && !state.isMutating,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.pattern_bookmark_next))
        }
    }
}

@Composable
private fun PatternBookmarkRow(
    bookmark: PatternBookmark,
    totalPages: Int,
    selected: Boolean,
    onJump: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val description =
        stringResource(
            R.string.pattern_bookmark_accessibility_description,
            bookmark.name,
            bookmark.pageIndex + 1,
        )
    val moreOptionsDescription =
        stringResource(
            R.string.pattern_bookmark_action_accessibility_description,
            stringResource(R.string.more_options),
            bookmark.name,
        )
    val renameDescription =
        stringResource(
            R.string.pattern_bookmark_action_accessibility_description,
            stringResource(R.string.pattern_bookmark_rename),
            bookmark.name,
        )
    val deleteDescription =
        stringResource(
            R.string.pattern_bookmark_action_accessibility_description,
            stringResource(R.string.delete),
            bookmark.name,
        )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = ProjectOverviewDimens.ActionTouchSize)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onJump)
                    .semantics {
                        contentDescription = description
                        this.selected = selected
                    },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = bookmark.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            R.string.pattern_page_indicator,
                            bookmark.pageIndex + 1,
                            totalPages.coerceAtLeast(bookmark.pageIndex + 1),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = moreOptionsDescription,
                )
            }
            AdaptiveDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pattern_bookmark_rename)) },
                    modifier = Modifier.semantics { contentDescription = renameDescription },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.semantics { contentDescription = deleteDescription },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun PatternBookmarkNameDialog(
    title: String,
    initialName: String,
    isSaving: Boolean,
    failure: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var touched by rememberSaveable(initialName) { mutableStateOf(false) }
    val trimmedName = name.trim()
    val empty = trimmedName.isEmpty()
    val tooLong = trimmedName.length > PATTERN_BOOKMARK_NAME_MAX_LENGTH
    val valid = !empty && !tooLong
    val canSave = valid && !isSaving
    val errorMessage =
        when {
            touched && empty -> stringResource(R.string.pattern_bookmark_name_required)
            touched && tooLong -> stringResource(R.string.pattern_bookmark_name_too_long)
            else -> failure
        }
    ScrollableFormDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(title) },
        text = {
            LabeledTextField(
                value = name,
                onValueChange = {
                    name = it
                    touched = true
                },
                onFocusLost = { touched = true },
                errorMessage = errorMessage,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            touched = true
                            if (canSave) onConfirm(trimmedName)
                        },
                    ),
                label = stringResource(R.string.pattern_bookmark_name),
                supportingText = stringResource(R.string.pattern_bookmark_name_limit, PATTERN_BOOKMARK_NAME_MAX_LENGTH),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = canSave,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            CancelButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        },
    )
}

private fun PatternBookmarkError.messageRes(): Int =
    when (this) {
        PatternBookmarkError.EMPTY_NAME -> R.string.pattern_bookmark_name_required
        PatternBookmarkError.NAME_TOO_LONG -> R.string.pattern_bookmark_name_too_long
        PatternBookmarkError.INVALID_LOCATION -> R.string.pattern_bookmark_invalid_location
        PatternBookmarkError.STALE_DOCUMENT -> R.string.pattern_bookmark_stale_document
        PatternBookmarkError.NOT_FOUND -> R.string.pattern_bookmark_not_found
        PatternBookmarkError.JUMP_FAILURE -> R.string.generic_error_unknown
        PatternBookmarkError.SAVE_FAILURE -> R.string.pattern_bookmark_save_failed
        PatternBookmarkError.RENAME_FAILURE -> R.string.pattern_bookmark_rename_failed
        PatternBookmarkError.DELETE_FAILURE -> R.string.pattern_bookmark_delete_failed
    }
