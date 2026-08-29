package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProjectYarnTextField
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.theme.YarnColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarnManagementSheet(
    linkedYarns: List<Pair<Long, String>>,
    projectYarnNotes: List<ProjectYarnNote>,
    proStatus: ProStatus,
    actions: YarnManagementSheetActions,
    usageItems: List<ProjectYarnUsageItem> = emptyList(),
    onUsage: (YarnUsageOpenRequest) -> Unit = {},
    focusKey: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var showProjectYarnForm by rememberSaveable { mutableStateOf(false) }
    val headingFocus = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
        val focusHeading = focusKey != null && usageItems.none { it.key == focusKey }
        LaunchedEffect(sheetState.currentValue, windowFocused, focusHeading) {
            if (sheetState.isVisible && windowFocused && focusHeading) headingFocus.requestFocus()
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.linked_yarn_title).localizedUppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier =
                    Modifier
                        .focusRequester(
                            headingFocus,
                        ).focusable()
                        .semantics { heading() }
                        .testTag("yarn_management_heading"),
            )

            if (linkedYarns.isEmpty() && projectYarnNotes.isEmpty() && usageItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_linked_yarn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            linkedYarns.forEach { (id, label) ->
                LinkedYarnRow(
                    id = id,
                    label = label,
                    onUnlinkYarn = actions.onUnlinkYarn,
                )
                usageItems
                    .firstOrNull {
                        it.source.yarnCardId == id &&
                            it.source.projectYarnNoteId == null &&
                            it.status == YarnUsageSourceStatus.AVAILABLE
                    }?.let { item ->
                        YarnUsageRow(item, onUsage, sheetState.isVisible && focusKey == item.key)
                    }
            }

            ProjectYarnNotesSection(
                notes = projectYarnNotes,
                proStatus = proStatus,
                onDeleteProjectYarnNote = actions.onDeleteProjectYarnNote,
                onSaveProjectYarnNoteToMyYarn = actions.onSaveProjectYarnNoteToMyYarn,
                usageItems = usageItems,
                onUsage = onUsage,
                focusKey = focusKey.takeIf { sheetState.isVisible },
            )

            usageItems.filter { it.status != YarnUsageSourceStatus.AVAILABLE }.forEach { item ->
                YarnUsageRow(item, onUsage, sheetState.isVisible && focusKey == item.key)
            }

            YarnOptionCard(
                titleRes = R.string.choose_from_my_yarn,
                bodyRes = R.string.choose_from_my_yarn_body,
                onClick = actions.onAddYarn,
            )
            YarnOptionCard(
                titleRes = R.string.add_yarn_to_project,
                bodyRes = R.string.add_yarn_to_project_body,
                onClick = { showProjectYarnForm = true },
            )

            if (showProjectYarnForm) {
                ProjectYarnForm(
                    onSave = { name, description, quantity, notes ->
                        actions.onSaveProjectYarnNote(name, description, quantity, notes)
                        showProjectYarnForm = false
                    },
                    onCancel = { showProjectYarnForm = false },
                )
            }
        }
    }
}

data class YarnManagementSheetActions(
    val onUnlinkYarn: (Long) -> Unit,
    val onAddYarn: () -> Unit,
    val onSaveProjectYarnNote: (String, String, Int, String) -> Unit,
    val onDeleteProjectYarnNote: (Long) -> Unit,
    val onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
private fun LinkedYarnRow(
    id: Long,
    label: String,
    onUnlinkYarn: (Long) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(YarnColors[(id % YarnColors.size).toInt()], CircleShape),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onUnlinkYarn(id) }) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.unlink_yarn),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectYarnNotesSection(
    notes: List<ProjectYarnNote>,
    proStatus: ProStatus,
    onDeleteProjectYarnNote: (Long) -> Unit,
    onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    usageItems: List<ProjectYarnUsageItem>,
    onUsage: (YarnUsageOpenRequest) -> Unit,
    focusKey: String?,
) {
    if (notes.isEmpty()) return

    Text(
        text = stringResource(R.string.project_yarn_notes_title).localizedUppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
    notes.forEach { note ->
        ProjectYarnNoteRow(
            note = note,
            proStatus = proStatus,
            onDeleteProjectYarnNote = onDeleteProjectYarnNote,
            onSaveProjectYarnNoteToMyYarn = onSaveProjectYarnNoteToMyYarn,
        )
        usageItems.firstOrNull { it.source.projectYarnNoteId == note.id }?.let { item ->
            YarnUsageRow(item, onUsage, focusKey == item.key)
        }
    }
}

@Composable
private fun ProjectYarnNoteRow(
    note: ProjectYarnNote,
    proStatus: ProStatus,
    onDeleteProjectYarnNote: (Long) -> Unit,
    onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onDeleteProjectYarnNote(note.id) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = note.summaryText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            onClick = { onSaveProjectYarnNoteToMyYarn(note.id) },
            enabled = note.savedYarnCardId == null,
        ) {
            Text(
                text =
                    stringResource(
                        if (note.savedYarnCardId == null) {
                            R.string.save_to_my_yarn
                        } else {
                            R.string.saved_to_my_yarn
                        },
                    ),
            )
            if (note.savedYarnCardId == null) {
                Spacer(modifier = Modifier.width(6.dp))
                ProBadge(status = proStatus)
            }
        }
    }
}

@Composable
private fun ProjectYarnNote.summaryText(): String {
    val quantity = stringResource(R.string.project_yarn_quantity_format, quantity)
    val details =
        listOf(
            quantity,
            description.takeIf(String::isNotBlank),
            notes.takeIf(String::isNotBlank),
        )
    return details.filterNotNull().joinToString(", ")
}

@Composable
private fun YarnOptionCard(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectYarnForm(
    onSave: (String, String, Int, String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var notes by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                ).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProjectYarnTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.project_yarn_name),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(R.string.project_yarn_description),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = stringResource(R.string.quantity_label),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        ProjectYarnTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.notes),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = {
                    onSave(
                        name,
                        description,
                        quantity.toIntOrNull() ?: 1,
                        notes,
                    )
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
