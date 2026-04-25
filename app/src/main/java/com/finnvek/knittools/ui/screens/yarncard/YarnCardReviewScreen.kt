package com.finnvek.knittools.ui.screens.yarncard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.care.CareSymbol
import com.finnvek.knittools.ui.components.care.CareSymbolIcon
import com.finnvek.knittools.ui.components.care.CareSymbolPicker
import com.finnvek.knittools.ui.components.care.hasCareSymbol
import com.finnvek.knittools.ui.components.care.toggleCareSymbol
import com.finnvek.knittools.ui.screens.library.YarnStatusSheet
import com.finnvek.knittools.ui.screens.library.yarnStatusUi
import com.finnvek.knittools.ui.theme.knitToolsColors

// Historiallisesta nimestä huolimatta ruutu toimii kahdessa tilassa:
// 1) skannatun yarn cardin review/tallennus
// 2) tallennetun yarn cardin detail/editointi Library-flow’ssa
@Composable
// Compose-modal-state ja ruudun orkestrointi tuottavat Sonarille vääriä osumia.
@Suppress("kotlin:S6615", "kotlin:S3776")
fun YarnCardReviewScreen(
    viewModel: YarnCardViewModel,
    onSaveAndUse: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onDiscard: (weightGrams: String, lengthMeters: String, needleSize: String) -> Unit,
    onBack: () -> Unit,
    initialLinkProjectId: Long? = null,
    onLinkToProject: ((cardId: Long, projectId: Long) -> Unit)? = null,
    onOpenLinkedProject: ((Long) -> Unit)? = null,
) {
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val linkedProjectName by viewModel.linkedProjectName.collectAsStateWithLifecycle()
    val availableProjects by viewModel.availableProjects.collectAsStateWithLifecycle()
    val toastContext = LocalContext.current
    val isDetailMode = form.editingCardId != null
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var savedCardId by rememberSaveable { mutableLongStateOf(0L) }
    var showStatusSheet by rememberSaveable { mutableStateOf(false) }
    var showProjectSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showLinkDialog && initialLinkProjectId != null && onLinkToProject != null) {
        LinkYarnDialog(
            savedCardId = savedCardId,
            projects = availableProjects,
            initialProjectId = initialLinkProjectId,
            onLink = { cardId, projectId ->
                onLinkToProject(cardId, projectId)
                showLinkDialog = false
                val (w, l, n) = viewModel.getCalculatorValues()
                onSaveAndUse(w, l, n)
            },
            onDismiss = {
                showLinkDialog = false
                val (w, l, n) = viewModel.getCalculatorValues()
                onSaveAndUse(w, l, n)
            },
        )
    }

    if (showStatusSheet && isDetailMode) {
        YarnStatusSheet(
            selectedStatus = form.status,
            onSelect = {
                viewModel.updateStatus(it)
                showStatusSheet = false
            },
            onDismiss = { showStatusSheet = false },
        )
    }

    if (showProjectSheet && isDetailMode) {
        LinkedProjectSheet(
            projects = availableProjects,
            linkedProjectId = form.linkedProjectId,
            onSelectProject = { projectId ->
                viewModel.setLinkedProject(projectId)
                showProjectSheet = false
            },
            onRemoveLink = {
                viewModel.setLinkedProject(null)
                showProjectSheet = false
            },
            onDismiss = { showProjectSheet = false },
        )
    }

    if (showDeleteDialog && isDetailMode) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_yarn_card),
            message = stringResource(R.string.delete_yarn_card_message),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                val cardId = form.editingCardId
                if (cardId != null) {
                    viewModel.deleteCard(cardId) {
                        viewModel.deletePhotoFile(form.photoUri)
                        showDeleteDialog = false
                        onBack()
                    }
                } else {
                    showDeleteDialog = false
                }
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    ToolScreenScaffold(
        title =
            if (isDetailMode) {
                form.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) }
            } else {
                stringResource(R.string.scanned_yarn)
            },
        onBack = onBack,
    ) { padding ->
        if (isDetailMode) {
            YarnCardDetailContent(
                form = form,
                linkedProjectName = linkedProjectName,
                onStatusClick = { showStatusSheet = true },
                onQuantityChange = viewModel::updateQuantity,
                onLinkedProjectClick = {
                    form.linkedProjectId?.let { projectId ->
                        if (linkedProjectName != null && onOpenLinkedProject != null) {
                            onOpenLinkedProject(projectId)
                        } else {
                            showProjectSheet = true
                        }
                    } ?: run {
                        showProjectSheet = true
                    }
                },
                onChangeProjectClick = { showProjectSheet = true },
                onDelete = { showDeleteDialog = true },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
            )
        } else {
            YarnCardScanContent(
                form = form,
                viewModel = viewModel,
                onDiscard = onDiscard,
                onSaveAndUse = onSaveAndUse,
                onShowLinkDialog = { id ->
                    savedCardId = id
                    showLinkDialog = true
                },
                initialLinkProjectId = initialLinkProjectId,
                availableProjects = availableProjects,
                onLinkToProject = onLinkToProject,
                toastContext = toastContext,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
@Suppress("kotlin:S107") // Compose-komponentti välittää eksplisiittiset callbackit ilman keinotekoista wrapper-oliota
private fun YarnCardScanContent(
    form: YarnCardFormState,
    viewModel: YarnCardViewModel,
    onDiscard: (String, String, String) -> Unit,
    onSaveAndUse: (String, String, String) -> Unit,
    onShowLinkDialog: (Long) -> Unit,
    initialLinkProjectId: Long?,
    availableProjects: List<CounterProjectEntity>,
    onLinkToProject: ((cardId: Long, projectId: Long) -> Unit)?,
    toastContext: android.content.Context,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabelField(stringResource(R.string.brand), form.brand) {
            viewModel.updateField { copy(brand = it) }
        }
        LabelField(stringResource(R.string.yarn_name), form.yarnName) {
            viewModel.updateField { copy(yarnName = it) }
        }
        LabelField(stringResource(R.string.fiber_content), form.fiberContent) {
            viewModel.updateField { copy(fiberContent = it) }
        }
        LabelField(stringResource(R.string.color_name), form.colorName) {
            viewModel.updateField { copy(colorName = it) }
        }
        LabelField(stringResource(R.string.color_number), form.colorNumber) {
            viewModel.updateField { copy(colorNumber = it) }
        }
        LabelField(stringResource(R.string.dye_lot), form.dyeLot) {
            viewModel.updateField { copy(dyeLot = it) }
        }

        Text(
            text = stringResource(R.string.result),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LabelField(stringResource(R.string.weight_grams), form.weightGrams) {
            viewModel.updateField { copy(weightGrams = it) }
        }
        LabelField(stringResource(R.string.length_meters), form.lengthMeters) {
            viewModel.updateField { copy(lengthMeters = it) }
        }
        LabelField(stringResource(R.string.needle_size_label), form.needleSize) {
            viewModel.updateField { copy(needleSize = it) }
        }
        LabelField(stringResource(R.string.gauge_label), form.gaugeInfo) {
            viewModel.updateField { copy(gaugeInfo = it) }
        }
        LabelField(stringResource(R.string.weight_category), form.weightCategory) {
            viewModel.updateField { copy(weightCategory = it) }
        }

        CareSymbolPicker(
            careSymbols = form.careSymbols,
            onToggle = { symbol ->
                viewModel.updateField { copy(careSymbols = careSymbols.toggleCareSymbol(symbol)) }
            },
        )

        ReviewActionButtons(
            isPro = viewModel.isPro,
            onDiscardClick = {
                val (w, l, n) = viewModel.getCalculatorValues()
                onDiscard(w, l, n)
            },
            onSaveClick = {
                handleSaveClick(
                    viewModel = viewModel,
                    canLink =
                        initialLinkProjectId != null &&
                            onLinkToProject != null &&
                            availableProjects.any { it.id == initialLinkProjectId },
                    onSaveAndUse = onSaveAndUse,
                    onShowLinkDialog = onShowLinkDialog,
                    onSaved = {
                        Toast
                            .makeText(
                                toastContext,
                                toastContext.getString(R.string.saved_to_my_yarn_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                    },
                )
            },
        )
    }
}

@Composable
@Suppress("kotlin:S107") // Detail-näkymä välittää tarkoituksella erilliset UI-callbackit luettavuuden takia
private fun YarnCardDetailContent(
    form: YarnCardFormState,
    linkedProjectName: String?,
    onStatusClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLinkedProjectClick: () -> Unit,
    onChangeProjectClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        YarnIdentitySection(form = form)
        YarnActionsSection(
            form = form,
            linkedProjectName = linkedProjectName,
            onStatusClick = onStatusClick,
            onQuantityChange = onQuantityChange,
            onLinkedProjectClick = onLinkedProjectClick,
            onChangeProjectClick = onChangeProjectClick,
        )
        YarnDetailsSection(form = form)
        YarnCareSection(careSymbols = form.careSymbols)
        TextButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(
                text = stringResource(R.string.delete),
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun YarnIdentitySection(form: YarnCardFormState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (form.photoUri.isNotBlank()) {
            AsyncImage(
                model = form.photoUri,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(88.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium,
                        ),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (form.brand.isNotBlank()) {
                Text(
                    text = form.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                )
            }
            Text(
                text = form.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun YarnActionsSection(
    form: YarnCardFormState,
    linkedProjectName: String?,
    onStatusClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLinkedProjectClick: () -> Unit,
    onChangeProjectClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActionRow(
                label = stringResource(R.string.status_label),
                value = {
                    val status = yarnStatusUi(form.status)
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = status.contentColor,
                        modifier =
                            Modifier
                                .background(status.containerColor, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                },
                onClick = onStatusClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ActionRow(
                label = stringResource(R.string.quantity_label),
                value = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = { onQuantityChange(-1) },
                            enabled = form.quantityInStash > 0,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = stringResource(R.string.counter_decrease),
                            )
                        }
                        Text(
                            text = skeinCountText(form.quantityInStash),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onQuantityChange(1) }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.counter_increase),
                            )
                        }
                    }
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ActionRow(
                label = stringResource(R.string.linked_project_label),
                value = {
                    Text(
                        text = linkedProjectName ?: stringResource(R.string.link_to_project_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (linkedProjectName == null) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = onLinkedProjectClick,
            )
            if (linkedProjectName != null) {
                TextButton(
                    onClick = onChangeProjectClick,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.change_project_link))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    value: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        )
        Spacer(modifier = Modifier.width(16.dp))
        value()
    }
}

@Composable
private fun YarnDetailsSection(form: YarnCardFormState) {
    val detailRows =
        listOf(
            stringResource(R.string.fiber_content) to form.fiberContent,
            stringResource(R.string.weight_category) to form.weightCategory,
            stringResource(R.string.weight_grams) to
                form.weightGrams
                    .takeIf { it.isNotBlank() }
                    ?.let { "$it g" }
                    .orEmpty(),
            stringResource(R.string.length_meters) to
                form.lengthMeters
                    .takeIf { it.isNotBlank() }
                    ?.let { "$it m" }
                    .orEmpty(),
            stringResource(R.string.needle_size_label) to form.needleSize,
            stringResource(R.string.gauge_label) to form.gaugeInfo,
            stringResource(R.string.color_name) to form.colorName,
            stringResource(R.string.color_number) to form.colorNumber,
            stringResource(R.string.dye_lot) to form.dyeLot,
        ).filter { it.second.isNotBlank() }

    if (detailRows.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.yarn_details_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
            Spacer(modifier = Modifier.height(12.dp))
            detailRows.forEachIndexed { index, (label, value) ->
                LabeledDetailRow(label = label, value = value)
                if (index != detailRows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun YarnCareSection(careSymbols: Long) {
    val selectedSymbols = CareSymbol.entries.filter { careSymbols.hasCareSymbol(it) }
    if (selectedSymbols.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.care_symbols),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            selectedSymbols.forEach { symbol ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    CareSymbolIcon(
                        symbol = symbol,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LinkedProjectSheet(
    projects: List<CounterProjectEntity>,
    linkedProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onRemoveLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.select_project),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            projects.forEach { project ->
                val isSelected = project.id == linkedProjectId
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                shape = MaterialTheme.shapes.medium,
                            ).clickable { onSelectProject(project.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (linkedProjectId != null) {
                TextButton(
                    onClick = onRemoveLink,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text(stringResource(R.string.remove_project_link))
                }
            }
        }
    }
}

@Composable
private fun LinkYarnDialog(
    savedCardId: Long,
    projects: List<CounterProjectEntity>,
    initialProjectId: Long,
    onLink: (cardId: Long, projectId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedProjectId by rememberSaveable(savedCardId, initialProjectId) {
        mutableLongStateOf(initialProjectId)
    }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.link_yarn)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                selectedProject?.let { project ->
                    Text(stringResource(R.string.link_to_project, project.name))
                }
                Text(
                    text = stringResource(R.string.select_project),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                projects.forEach { project ->
                    val isSelected = project.id == selectedProjectId
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                ).clickable { selectedProjectId = project.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedProject?.let { onLink(savedCardId, it.id) } },
                enabled = selectedProject != null,
            ) {
                Text(stringResource(R.string.link))
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
private fun ReviewActionButtons(
    isPro: Boolean,
    onDiscardClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onDiscardClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.discard))
        }
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (isPro) {
                    stringResource(R.string.save_and_use)
                } else {
                    stringResource(R.string.use_in_calculator)
                },
            )
        }
    }

    if (!isPro) {
        Text(
            text = stringResource(R.string.pro_required_save),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun handleSaveClick(
    viewModel: YarnCardViewModel,
    canLink: Boolean,
    onSaveAndUse: (String, String, String) -> Unit,
    onShowLinkDialog: (Long) -> Unit,
    onSaved: () -> Unit = {},
) {
    if (!viewModel.isPro) {
        val (w, l, n) = viewModel.getCalculatorValues()
        viewModel.discardScan()
        onSaveAndUse(w, l, n)
        return
    }
    viewModel.saveCard { id ->
        onSaved()
        if (canLink) {
            onShowLinkDialog(id)
        } else {
            val (w, l, n) = viewModel.getCalculatorValues()
            onSaveAndUse(w, l, n)
        }
    }
}

@Composable
private fun LabelField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
    )
}

@Composable
private fun skeinCountText(quantity: Int): String =
    if (quantity == 1) {
        stringResource(R.string.skein_count_one, quantity)
    } else {
        stringResource(R.string.skein_count_many, quantity)
    }
