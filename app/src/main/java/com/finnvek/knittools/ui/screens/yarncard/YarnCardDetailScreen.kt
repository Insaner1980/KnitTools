package com.finnvek.knittools.ui.screens.yarncard

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.care.CareSymbol
import com.finnvek.knittools.ui.components.care.CareSymbolIcon
import com.finnvek.knittools.ui.components.care.hasCareSymbol
import com.finnvek.knittools.ui.components.skeinCountText
import com.finnvek.knittools.ui.screens.library.YarnStatusSheet
import com.finnvek.knittools.ui.screens.library.yarnStatusUi
import com.finnvek.knittools.ui.theme.knitToolsColors

data class YarnCardDetailActions(
    val onBack: () -> Unit,
    val onOpenLinkedProject: ((Long) -> Unit)? = null,
    val onDeleteCard: ((Long) -> Unit)? = null,
)

@Composable
// Compose-modal-state ja ruudun orkestrointi tuottavat Sonarille vääriä osumia.
@Suppress("kotlin:S6615", "kotlin:S3776")
fun YarnCardDetailScreen(
    viewModel: YarnCardViewModel,
    actions: YarnCardDetailActions,
) {
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val linkedProjectName by viewModel.linkedProjectName.collectAsStateWithLifecycle()
    val availableProjects by viewModel.availableProjects.collectAsStateWithLifecycle()
    var showStatusSheet by rememberSaveable { mutableStateOf(false) }
    var showProjectSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showStatusSheet) {
        YarnStatusSheet(
            selectedStatus = form.status,
            onSelect = {
                viewModel.updateStatus(it)
                showStatusSheet = false
            },
            onDismiss = { showStatusSheet = false },
        )
    }

    if (showProjectSheet) {
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

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_yarn_card),
            message = stringResource(R.string.delete_yarn_card_message),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                val cardId = form.editingCardId
                if (cardId != null) {
                    if (actions.onDeleteCard != null) {
                        showDeleteDialog = false
                        actions.onDeleteCard.invoke(cardId)
                    } else {
                        viewModel.deleteCard(cardId) {
                            showDeleteDialog = false
                            actions.onBack()
                        }
                    }
                } else {
                    showDeleteDialog = false
                }
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    ToolScreenScaffold(
        title = form.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) },
        onBack = actions.onBack,
    ) { padding ->
        YarnCardDetailContent(
            form = form,
            linkedProjectName = linkedProjectName,
            onStatusClick = { showStatusSheet = true },
            onQuantityChange = viewModel::updateQuantity,
            onLinkedProjectClick = {
                form.linkedProjectId?.let { projectId ->
                    if (linkedProjectName != null && actions.onOpenLinkedProject != null) {
                        actions.onOpenLinkedProject.invoke(projectId)
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
    projects: List<CounterProject>,
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
