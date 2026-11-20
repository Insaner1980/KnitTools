package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.RowMenuAction
import com.finnvek.knittools.ui.components.RowOverflowMenu
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SheetOptionRow
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S107") // Sheet välittää projektin erilliset lanka-, muistiinpano- ja käyttötilat.
fun YarnManagementSheet(
    linkedYarns: List<LinkedYarnSummary>,
    projectYarnNotes: List<ProjectYarnNote>,
    proState: ProState,
    proAccessReady: Boolean,
    actions: YarnManagementSheetActions,
    snackbarHostState: SnackbarHostState? = null,
    usageItems: List<ProjectYarnUsageItem> = emptyList(),
    onUsage: (YarnUsageOpenRequest) -> Unit = {},
    focusKey: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var pendingCreation by remember { mutableStateOf(actions.pendingProjectYarnNoteCreation()) }
    var createResult by remember { mutableStateOf<ProjectYarnNoteEditResult?>(null) }
    var showProjectYarnForm by rememberSaveable { mutableStateOf(false) }
    var editingNote by rememberSaveable(
        stateSaver = ProjectYarnNoteEditorSaver,
    ) { mutableStateOf<ProjectYarnNote?>(null) }
    val headingFocus = remember { FocusRequester() }

    LaunchedEffect(pendingCreation) {
        val request = pendingCreation ?: return@LaunchedEffect
        createResult = request.await()
        if (createResult == ProjectYarnNoteEditResult.Saved) showProjectYarnForm = false
        actions.consumeProjectYarnNoteCreation(request)
        pendingCreation = null
    }

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        val focusHeading = focusKey != null && usageItems.none { it.key == focusKey }
        LaunchedEffect(sheetState.currentValue, windowFocused, focusHeading) {
            if (sheetState.isVisible && windowFocused && focusHeading) headingFocus.requestFocus()
        }
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Sama sheetin otsikko kuin muissa sheeteissä; linkitetyt langat ovat sen alla omana osionaan.
                SheetTitle(
                    text = stringResource(R.string.project_content_yarn),
                    modifier =
                        Modifier
                            .focusRequester(headingFocus)
                            .focusable()
                            .testTag("yarn_management_heading"),
                )
                SectionLabel(text = stringResource(R.string.linked_yarn_title))

                if (linkedYarns.isEmpty() && projectYarnNotes.isEmpty() && usageItems.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_linked_yarn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                linkedYarns.forEach { yarn ->
                    val usageItem =
                        usageItems.firstOrNull {
                            it.source.yarnCardId == yarn.id &&
                                it.source.projectYarnNoteId == null &&
                                it.status == YarnUsageSourceStatus.AVAILABLE
                        }
                    LinkedYarnRow(
                        yarn = yarn,
                        onUnlinkYarn = actions.onUnlinkYarn,
                    ) {
                        usageItem?.let { item ->
                            YarnUsageRow(item, onUsage, sheetState.isVisible && focusKey == item.key, inCard = true)
                        }
                    }
                }

                ProjectYarnNotesSection(
                    notes = projectYarnNotes,
                    proState = proState,
                    proAccessReady = proAccessReady,
                    onDeleteProjectYarnNote = actions.onDeleteProjectYarnNote,
                    onEditProjectYarnNote = { editingNote = it },
                    onSaveProjectYarnNoteToMyYarn = actions.onSaveProjectYarnNoteToMyYarn,
                    usageItems = usageItems,
                    onUsage = onUsage,
                    focusKey = focusKey.takeIf { sheetState.isVisible },
                )

                usageItems.filter { it.status != YarnUsageSourceStatus.AVAILABLE }.forEach { item ->
                    YarnUsageRow(item, onUsage, sheetState.isVisible && focusKey == item.key)
                }

                SheetOptionRow(
                    title = stringResource(R.string.choose_from_my_yarn),
                    body = stringResource(R.string.choose_from_my_yarn_body),
                    onClick = actions.onAddYarn,
                )
                SheetOptionRow(
                    title = stringResource(R.string.add_yarn_to_project),
                    body = stringResource(R.string.add_yarn_to_project_body),
                    onClick = { showProjectYarnForm = true },
                )
            }
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    // Oma lomakesheet kuten My Yarnin Add Yarn: sheetin sisään avautuva kortti omilla painikkeillaan
    // oli kolmas eri tapa lisätä lanka.
    if (showProjectYarnForm) {
        ProjectYarnForm(
            saving = pendingCreation != null,
            result = createResult,
            onSave = { name, description, quantity, notes ->
                if (pendingCreation == null) {
                    pendingCreation = actions.onSaveProjectYarnNote(name, description, quantity, notes)
                }
            },
            onCancel = { showProjectYarnForm = false },
        )
    }
    editingNote?.let { original ->
        ProjectYarnNoteEditor(
            original = original,
            onSave = actions.onEditProjectYarnNote,
            onDismiss = { editingNote = null },
        )
    }
}

data class YarnManagementSheetActions(
    val onUnlinkYarn: (Long) -> Unit,
    val onAddYarn: () -> Unit,
    val onSaveProjectYarnNote: (String, String, Int, String) -> kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>,
    val onDeleteProjectYarnNote: (Long) -> Unit,
    val onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    val onDismiss: () -> Unit,
    val pendingProjectYarnNoteCreation: () -> kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>? = { null },
    val consumeProjectYarnNoteCreation: (kotlinx.coroutines.Deferred<ProjectYarnNoteEditResult>) -> Unit = {},
    val onEditProjectYarnNote: suspend (ProjectYarnNote, ProjectYarnNote) -> ProjectYarnNoteEditResult = { _, _ ->
        ProjectYarnNoteEditResult.Failed
    },
)

@Composable
private fun Modifier.yarnEntryModifier(): Modifier =
    this
        .fillMaxWidth()
        .background(
            color = MaterialTheme.knitToolsColors.cardContainer,
            shape = MaterialTheme.shapes.medium,
        ).padding(horizontal = 14.dp, vertical = 12.dp)

@Composable
private fun LinkedYarnRow(
    yarn: LinkedYarnSummary,
    onUnlinkYarn: (Long) -> Unit,
    // Langan käyttö kortin sisällä nimen alla, ei irrallaan korttien välissä.
    usage: @Composable ColumnScope.() -> Unit = {},
) {
    val (id, name, colorLine) = yarn
    Column(
        modifier = Modifier.yarnEntryModifier(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(LinkedYarnDotSize)
                        .background(
                            yarnColorForId(id, MaterialTheme.knitToolsColors.yarnPalette),
                            CircleShape,
                        ),
            )
            Spacer(modifier = Modifier.width(LinkedYarnDotGap))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                colorLine?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onUnlinkYarn(id) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.unlink_yarn),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Linjassa nimen kanssa, pisteen ohi.
        Column(modifier = Modifier.padding(start = LinkedYarnDotSize + LinkedYarnDotGap), content = usage)
    }
}

private val LinkedYarnDotSize = 10.dp
private val LinkedYarnDotGap = 10.dp

@Composable
private fun ProjectYarnNotesSection(
    notes: List<ProjectYarnNote>,
    proState: ProState,
    proAccessReady: Boolean,
    onDeleteProjectYarnNote: (Long) -> Unit,
    onEditProjectYarnNote: (ProjectYarnNote) -> Unit,
    onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    usageItems: List<ProjectYarnUsageItem>,
    onUsage: (YarnUsageOpenRequest) -> Unit,
    focusKey: String?,
) {
    if (notes.isEmpty()) return

    SectionLabel(
        text = stringResource(R.string.project_yarn_notes_title),
    )
    notes.forEach { note ->
        ProjectYarnNoteRow(
            note = note,
            proState = proState,
            proAccessReady = proAccessReady,
            onDeleteProjectYarnNote = onDeleteProjectYarnNote,
            onEditProjectYarnNote = onEditProjectYarnNote,
            onSaveProjectYarnNoteToMyYarn = onSaveProjectYarnNoteToMyYarn,
        ) {
            usageItems.firstOrNull { it.source.projectYarnNoteId == note.id }?.let { item ->
                YarnUsageRow(item, onUsage, focusKey == item.key, inCard = true)
            }
        }
    }
}

@Composable
private fun ProjectYarnNoteRow(
    note: ProjectYarnNote,
    proState: ProState,
    proAccessReady: Boolean,
    onDeleteProjectYarnNote: (Long) -> Unit,
    onEditProjectYarnNote: (ProjectYarnNote) -> Unit,
    onSaveProjectYarnNoteToMyYarn: (Long) -> Unit,
    usage: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier.yarnEntryModifier(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            RowOverflowMenu(
                actions =
                    listOf(
                        RowMenuAction(stringResource(R.string.project_overview_edit)) { onEditProjectYarnNote(note) },
                        RowMenuAction(
                            stringResource(R.string.delete),
                            isDestructive = true,
                        ) { onDeleteProjectYarnNote(note.id) },
                    ),
                modifier = Modifier.testTag("project-yarn-actions-${note.id}"),
            )
        }
        Text(
            text = stringResource(R.string.project_yarn_quantity_format, note.quantity),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (note.description.isNotBlank()) {
            Text(
                note.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (note.notes.isNotBlank()) {
            Text(
                note.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (note.savedYarnCardId == null) {
            val yarnAccess = proActionAccess(proState, proAccessReady, ProFeature.UNLIMITED_YARN)
            TextButton(
                onClick = { onSaveProjectYarnNoteToMyYarn(note.id) },
                enabled = yarnAccess != ProActionAccess.Unknown || proState.hasFeature(ProFeature.UNLIMITED_YARN),
            ) {
                Text(modifier = Modifier.weight(1f), text = stringResource(R.string.save_to_my_yarn))
                Spacer(modifier = Modifier.width(6.dp))
                ProMarker(access = yarnAccess)
            }
            Text(
                text = stringResource(R.string.save_to_my_yarn_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Tehty tallennus on tila eikä toiminto: harmaana käytöstä poistettuna painikkeena se näytti rikkinäiseltä.
            BadgePill(text = stringResource(R.string.saved_to_my_yarn))
        }
        usage()
    }
}

@Composable
internal fun ProjectYarnForm(
    onSave: (String, String, Int, String) -> Unit,
    onCancel: () -> Unit,
    initialNote: ProjectYarnNote? = null,
    saving: Boolean = false,
    result: ProjectYarnNoteEditResult? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialNote?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initialNote?.description.orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf(initialNote?.quantity?.toString() ?: "1") }
    var notes by rememberSaveable { mutableStateOf(initialNote?.notes.orEmpty()) }

    FormSheet(
        title =
            stringResource(
                if (initialNote ==
                    null
                ) {
                    R.string.add_yarn_to_project
                } else {
                    R.string.project_yarn_edit_title
                },
            ),
        description = if (initialNote == null) stringResource(R.string.add_yarn_to_project_body) else null,
        onDismiss = { if (!saving) onCancel() },
        cancelEnabled = !saving,
        confirm =
            FormSheetConfirm(
                text = stringResource(R.string.save),
                enabled =
                    name.isNotBlank() &&
                        result != ProjectYarnNoteEditResult.Conflict &&
                        result != ProjectYarnNoteEditResult.Missing,
                busy = saving,
                onClick = {
                    if (!saving) {
                        onSave(name, description, quantity.toIntOrNull() ?: 1, notes)
                    }
                },
            ),
    ) {
        val focusManager = LocalFocusManager.current
        LaunchedEffect(saving) {
            if (saving) focusManager.clearFocus()
        }
        LabeledTextField(
            value = name,
            inputModifier = Modifier.testTag("project-yarn-name"),
            readOnly = saving,
            onValueChange = { name = it },
            label = stringResource(R.string.project_yarn_name),
            singleLine = true,
        )
        LabeledTextField(
            value = description,
            inputModifier = Modifier.testTag("project-yarn-description"),
            readOnly = saving,
            onValueChange = { description = it },
            label = stringResource(R.string.project_yarn_description),
            singleLine = true,
        )
        LabeledTextField(
            value = quantity,
            inputModifier = Modifier.testTag("project-yarn-quantity"),
            readOnly = saving,
            onValueChange = { candidate ->
                val digits = candidate.filter(Char::isDigit).take(Int.MAX_VALUE.toString().length)
                if (digits.isEmpty() || digits.toLongOrNull()?.let { it <= Int.MAX_VALUE } == true) {
                    quantity = digits
                }
            },
            label = stringResource(R.string.quantity_label),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        LabeledTextField(
            value = notes,
            inputModifier = Modifier.testTag("project-yarn-notes"),
            readOnly = saving,
            onValueChange = { notes = it },
            label = stringResource(R.string.notes),
            singleLine = false,
        )
        val error =
            when (result) {
                ProjectYarnNoteEditResult.Conflict -> R.string.project_yarn_edit_conflict
                ProjectYarnNoteEditResult.Missing -> R.string.project_yarn_edit_missing
                ProjectYarnNoteEditResult.InvalidName -> R.string.project_yarn_edit_invalid
                ProjectYarnNoteEditResult.Failed -> R.string.project_yarn_edit_failed
                else -> null
            }
        error?.let {
            Text(
                text = stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
internal fun ProjectYarnNoteEditor(
    original: ProjectYarnNote,
    onSave: suspend (ProjectYarnNote, ProjectYarnNote) -> ProjectYarnNoteEditResult,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ProjectYarnNoteEditResult?>(null) }
    ProjectYarnForm(
        initialNote = original,
        saving = saving,
        result = result,
        onCancel = onDismiss,
        onSave = { name, description, quantity, notes ->
            if (!saving) {
                saving = true
                scope.launch {
                    try {
                        result =
                            onSave(
                                original,
                                original.copy(
                                    name = name,
                                    description = description,
                                    quantity = quantity,
                                    notes = notes,
                                ),
                            )
                        if (result == ProjectYarnNoteEditResult.Saved) onDismiss()
                    } finally {
                        saving = false
                    }
                }
            }
        },
    )
}

private val ProjectYarnNoteEditorSaver =
    listSaver<ProjectYarnNote?, Any>(
        save = {
            if (it ==
                null
            ) {
                emptyList()
            } else {
                listOf(
                    it.id,
                    it.projectId,
                    it.name,
                    it.description,
                    it.quantity,
                    it.notes,
                    it.savedYarnCardId ?: 0L,
                    it.createdAt,
                    it.updatedAt,
                )
            }
        },
        restore = {
            if (it.isEmpty()) {
                null
            } else {
                ProjectYarnNote(
                    id = it[0] as Long,
                    projectId = it[1] as Long,
                    name = it[2] as String,
                    description = it[3] as String,
                    quantity = it[4] as Int,
                    notes = it[5] as String,
                    savedYarnCardId = (it[6] as Long).takeIf { id -> id != 0L },
                    createdAt = it[7] as Long,
                    updatedAt = it[8] as Long,
                )
            }
        },
    )
