package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PATTERN_BOOKMARK_NAME_MAX_LENGTH
import com.finnvek.knittools.domain.model.PatternBookmark

internal data class PatternBookmarkSheetActions(
    val onDismiss: () -> Unit,
    val onAdd: (String) -> Unit,
    val onJump: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onRename: (Long, String) -> Unit,
    val onDelete: (Long) -> Unit,
    val onClearError: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatternBookmarkSheet(
    state: PatternBookmarkUiState,
    totalPages: Int,
    actions: PatternBookmarkSheetActions,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var renameBookmarkId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteBookmarkId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val errorDescription = state.error?.let { stringResource(it.messageRes()) }
    val renameBookmark = state.bookmarks.firstOrNull { it.id == renameBookmarkId }
    val deleteBookmark = state.bookmarks.firstOrNull { it.id == deleteBookmarkId }

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.pattern_bookmarks),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
            TextButton(
                onClick = { showAddDialog = true },
                enabled = state.documentKey != null && !state.isMutating,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.pattern_bookmark_add_here))
            }
            state.error?.let { error ->
                Text(
                    text = errorDescription.orEmpty(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = errorDescription.orEmpty() },
                )
                TextButton(
                    onClick = actions.onClearError,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
            when {
                state.isLoading -> Text(stringResource(R.string.pattern_bookmark_loading))
                state.bookmarks.isEmpty() -> Text(stringResource(R.string.pattern_bookmark_empty))
                else ->
                    state.bookmarks.forEach { bookmark ->
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
            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = actions.onPrevious,
                    enabled = state.canGoPrevious && !state.isMutating,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.pattern_bookmark_previous),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = actions.onNext,
                    enabled = state.canGoNext && !state.isMutating,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.pattern_bookmark_next),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        PatternBookmarkNameDialog(
            title = stringResource(R.string.pattern_bookmark_add_title),
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                actions.onAdd(name)
                showAddDialog = false
            },
        )
    }
    renameBookmark?.let { bookmark ->
        PatternBookmarkNameDialog(
            title = stringResource(R.string.pattern_bookmark_rename_title),
            initialName = bookmark.name,
            onDismiss = { renameBookmarkId = null },
            onConfirm = { name ->
                actions.onRename(bookmark.id, name)
                renameBookmarkId = null
            },
        )
    }
    deleteBookmark?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { deleteBookmarkId = null },
            title = { Text(stringResource(R.string.pattern_bookmark_delete_title)) },
            text = { Text(stringResource(R.string.pattern_bookmark_delete_message, bookmark.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onDelete(bookmark.id)
                        deleteBookmarkId = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteBookmarkId = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onJump,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = description },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = bookmark.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            R.string.pattern_page_indicator,
                            bookmark.pageIndex + 1,
                            totalPages.coerceAtLeast(bookmark.pageIndex + 1),
                        ),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                if (selected) {
                    Text(
                        text = stringResource(R.string.pattern_bookmark_selected),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = moreOptionsDescription,
                )
            }
            DropdownMenu(
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
                    text = { Text(stringResource(R.string.delete)) },
                    modifier = Modifier.semantics { contentDescription = deleteDescription },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun PatternBookmarkNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()
    val empty = trimmedName.isEmpty()
    val tooLong = trimmedName.length > PATTERN_BOOKMARK_NAME_MAX_LENGTH
    val valid = !empty && !tooLong
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                isError = !valid,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(name) }),
                label = { Text(stringResource(R.string.pattern_bookmark_name)) },
                supportingText = {
                    Text(
                        when {
                            empty -> stringResource(R.string.pattern_bookmark_name_required)
                            tooLong -> stringResource(R.string.pattern_bookmark_name_too_long)
                            else ->
                                stringResource(
                                    R.string.pattern_bookmark_name_limit,
                                    PATTERN_BOOKMARK_NAME_MAX_LENGTH,
                                )
                        },
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = valid,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.cancel))
            }
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
        PatternBookmarkError.SAVE_FAILURE -> R.string.pattern_bookmark_save_failed
        PatternBookmarkError.RENAME_FAILURE -> R.string.pattern_bookmark_rename_failed
        PatternBookmarkError.DELETE_FAILURE -> R.string.pattern_bookmark_delete_failed
    }
