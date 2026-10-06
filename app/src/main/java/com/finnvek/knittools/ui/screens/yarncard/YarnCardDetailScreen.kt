package com.finnvek.knittools.ui.screens.yarncard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.CounterImageButton
import com.finnvek.knittools.ui.components.FabricKind
import com.finnvek.knittools.ui.components.FabricPhotoPlaceholder
import com.finnvek.knittools.ui.components.OverviewEmptyText
import com.finnvek.knittools.ui.components.OverviewHeroPhoto
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewSectionHeader
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.cardContainerColor
import com.finnvek.knittools.ui.components.care.CareSymbol
import com.finnvek.knittools.ui.components.care.CareSymbolIcon
import com.finnvek.knittools.ui.components.care.hasCareSymbol
import com.finnvek.knittools.ui.components.rememberScrollTitleState
import com.finnvek.knittools.ui.components.skeinCountText
import com.finnvek.knittools.ui.screens.library.ManualYarnCardSheet
import com.finnvek.knittools.ui.screens.library.YarnStatusSheet
import com.finnvek.knittools.ui.screens.library.yarnStatusUi
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.launch

data class YarnCardDetailActions(
    val onBack: () -> Unit,
    val onOpenLinkedProject: ((Long) -> Unit)? = null,
    val onDeleteCard: ((Long) -> Unit)? = null,
)

@Composable
// Compose-modal-state ja ruudun orkestrointi tuottavat Sonarille vääriä osumia.
@Suppress("kotlin:S6615", "kotlin:S3776")
fun YarnCardDetailScreen(
    viewModelProvider: @Composable () -> YarnCardViewModel,
    actions: YarnCardDetailActions,
) {
    val viewModel = viewModelProvider()
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val linkedProjectName by viewModel.linkedProjectName.collectAsStateWithLifecycle()
    val availableProjects by viewModel.availableProjects.collectAsStateWithLifecycle()
    var showStatusSheet by rememberSaveable { mutableStateOf(false) }
    var showProjectSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showManualDetailsSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val photoImportError = stringResource(R.string.generic_error_unknown)
    val yarnPhotoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                viewModel.updatePhotoUri(uri) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(photoImportError) }
                }
            }
        }

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

    if (showManualDetailsSheet) {
        ManualYarnCardSheet(
            initialInput = form.toManualYarnCardInput(),
            titleRes = R.string.edit_yarn_details,
            onSave = { input ->
                viewModel.updateManualDetails(input)
                showManualDetailsSheet = false
            },
            onDismiss = { showManualDetailsSheet = false },
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

    // Nimi on jo sisällön otsikkona; yläpalkki näyttää sen vasta kun kuva ja nimi on vieritetty pois.
    val scrollTitle = rememberScrollTitleState()
    val contentActions =
        YarnDetailContentActions(
            onPickPhoto = {
                yarnPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onStatusClick = { showStatusSheet = true },
            onQuantityChange = viewModel::updateQuantity,
            onLinkedProjectClick = {
                val projectId = form.linkedProjectId
                if (projectId != null && linkedProjectName != null && actions.onOpenLinkedProject != null) {
                    actions.onOpenLinkedProject.invoke(projectId)
                } else {
                    showProjectSheet = true
                }
            },
            onChangeProjectClick = { showProjectSheet = true },
            onEditManualDetails = { showManualDetailsSheet = true },
        )
    ToolScreenScaffold(
        title = form.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) },
        onBack = actions.onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        showTitle = scrollTitle.showTitle,
        actions = { YarnDetailMenu(onDelete = { showDeleteDialog = true }) },
    ) { padding ->
        YarnCardDetailContent(
            form = form,
            linkedProjectName = linkedProjectName,
            actions = contentActions,
            headerModifier = scrollTitle.headerModifier,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollTitle.scrollState)
                    .padding(
                        start = ProjectOverviewDimens.ScreenHorizontalPadding,
                        top = ProjectOverviewDimens.TopContentGap,
                        end = ProjectOverviewDimens.ScreenHorizontalPadding,
                        bottom = ProjectOverviewDimens.ContentBottomPadding,
                    ),
        )
    }
}

private class YarnDetailContentActions(
    val onPickPhoto: () -> Unit,
    val onStatusClick: () -> Unit,
    val onQuantityChange: (Int) -> Unit,
    val onLinkedProjectClick: () -> Unit,
    val onChangeProjectClick: () -> Unit,
    val onEditManualDetails: () -> Unit,
)

/** Poisto on harvinainen ja peruuttamaton, joten se on ylivuotovalikossa kuten projektinäkymässä. */
@Composable
private fun YarnDetailMenu(onDelete: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_yarn_card)) },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

/**
 * Langan sivu on rakennettu kuten projektinäkymä: kuva tai neulepintapaikkamerkki, nimi ja
 * hiusviivalla erotetut osiot, joiden toiminto on otsikkorivin oikeassa reunassa.
 */
@Composable
private fun YarnCardDetailContent(
    form: YarnCardFormState,
    linkedProjectName: String?,
    actions: YarnDetailContentActions,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(modifier = headerModifier) {
            YarnPhoto(photoUri = form.photoUri, onPickPhoto = actions.onPickPhoto)
            YarnIdentity(form = form)
        }
        YarnStatusSection(status = form.status, onStatusClick = actions.onStatusClick)
        YarnQuantitySection(quantity = form.quantityInStash, onQuantityChange = actions.onQuantityChange)
        YarnLinkedProjectSection(
            linkedProjectName = linkedProjectName,
            onLinkedProjectClick = actions.onLinkedProjectClick,
            onChangeProjectClick = actions.onChangeProjectClick,
        )
        YarnDetailsSection(form = form, onEditManualDetails = actions.onEditManualDetails)
        YarnCareSection(careSymbols = form.careSymbols)
    }
}

@Composable
private fun YarnPhoto(
    photoUri: String,
    onPickPhoto: () -> Unit,
) {
    if (photoUri.isBlank()) {
        // Langan oikeaa väriä ei tiedetä ilman kuvaa, joten tilkku on neutraalia kerrattua lankaa.
        FabricPhotoPlaceholder(
            kind = FabricKind.YARN,
            color = MaterialTheme.knitToolsColors.yarnSwatchNeutral,
            label = stringResource(R.string.add_yarn_photo),
            onClick = onPickPhoto,
        )
    } else {
        OverviewHeroPhoto(
            model = photoUri,
            onClickLabel = stringResource(R.string.change_yarn_photo),
            onClick = onPickPhoto,
        )
    }
}

@Composable
private fun YarnIdentity(form: YarnCardFormState) {
    Column(Modifier.padding(top = ProjectOverviewDimens.HeaderTopGap)) {
        if (form.brand.isNotBlank()) {
            Text(
                text = form.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = form.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun YarnStatusSection(
    status: String,
    onStatusClick: () -> Unit,
) {
    OverviewSectionHeader(R.string.status_label, R.string.project_overview_edit, onStatusClick)
    val statusUi = yarnStatusUi(status)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ProjectOverviewDimens.ActionTouchSize)
                .clickable(role = Role.Button, onClick = onStatusClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = statusUi.label,
            style = MaterialTheme.typography.labelMedium,
            color = statusUi.contentColor,
            modifier =
                Modifier
                    .background(statusUi.containerColor, CircleShape)
                    .padding(
                        horizontal = ProjectOverviewDimens.PillHorizontalPadding,
                        vertical = ProjectOverviewDimens.ContentGap,
                    ),
        )
    }
}

/** Määrä on sivun ainoa usein muutettava arvo, joten se saa ison luvun ja laskurin 3D-napit. */
@Composable
private fun YarnQuantitySection(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    OverviewSectionHeader(R.string.quantity_label)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
    ) {
        Text(
            text = skeinCountText(quantity),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.knitToolsColors.primaryReadable,
            modifier = Modifier.weight(1f),
        )
        CounterImageButton(
            imageRes = R.drawable.counter_minus_button,
            contentDescription = stringResource(R.string.counter_decrease),
            visualSize = ProjectOverviewDimens.StepperVisualSize,
            onClick = { onQuantityChange(-1) },
            modifier = Modifier.size(ProjectOverviewDimens.StepperTouchSize),
            enabled = quantity > 0,
        )
        CounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            contentDescription = stringResource(R.string.counter_increase),
            visualSize = ProjectOverviewDimens.StepperVisualSize,
            onClick = { onQuantityChange(1) },
            modifier = Modifier.size(ProjectOverviewDimens.StepperTouchSize),
        )
    }
}

@Composable
private fun YarnLinkedProjectSection(
    linkedProjectName: String?,
    onLinkedProjectClick: () -> Unit,
    onChangeProjectClick: () -> Unit,
) {
    OverviewSectionHeader(
        R.string.linked_project_label,
        if (linkedProjectName == null) R.string.project_overview_add else R.string.project_overview_edit,
        onChangeProjectClick,
    )
    if (linkedProjectName == null) {
        OverviewEmptyText(R.string.yarn_not_linked)
    } else {
        OverviewLinkRow(linkedProjectName, null, null, onLinkedProjectClick)
    }
}

@Composable
private fun YarnDetailsSection(
    form: YarnCardFormState,
    onEditManualDetails: () -> Unit,
) {
    val detailRows =
        listOf(
            stringResource(R.string.fiber_content) to form.fiberContent,
            stringResource(R.string.weight_category) to form.weightCategory,
            stringResource(R.string.weight_grams) to
                form.weightGrams
                    .takeIf { it.isNotBlank() }
                    ?.let { formatYarnMeasurement(it, "g") }
                    .orEmpty(),
            stringResource(R.string.length_meters) to
                form.lengthMeters
                    .takeIf { it.isNotBlank() }
                    ?.let { formatYarnMeasurement(it, "m") }
                    .orEmpty(),
            stringResource(R.string.needle_size_label) to form.needleSize,
            stringResource(R.string.gauge_label) to form.gaugeInfo,
            stringResource(R.string.color_name) to form.colorName,
            stringResource(R.string.color_number) to form.colorNumber,
            stringResource(R.string.dye_lot) to form.dyeLot,
        ).filter { it.second.isNotBlank() }

    OverviewSectionHeader(
        R.string.yarn_details_title,
        if (detailRows.isEmpty()) R.string.project_overview_add else R.string.project_overview_edit,
        onEditManualDetails,
    )
    if (detailRows.isEmpty()) {
        // Valinnaiset tiedot puuttuvat tarkoituksella: kerrotaan että ne voi lisätä, ei virhettä.
        OverviewEmptyText(R.string.yarn_details_empty_body)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.DetailRowGap)) {
        detailRows.forEach { (label, value) -> LabeledDetailRow(label = label, value = value) }
    }
}

internal fun formatYarnMeasurement(
    value: String,
    unit: String,
): String {
    val trimmedValue = value.trim()
    if (trimmedValue.isBlank()) return ""

    val normalizedValue = trimmedValue.lowercase()
    val normalizedUnit = unit.lowercase()
    val hasUnitSuffix =
        normalizedValue.endsWith(" $normalizedUnit") ||
            normalizedValue.matches(Regex("""^.*\d\s*$normalizedUnit$"""))

    return if (hasUnitSuffix) trimmedValue else "$trimmedValue $unit"
}

private fun YarnCardFormState.toManualYarnCardInput(): ManualYarnCardInput =
    ManualYarnCardInput(
        yarnName = yarnName,
        brand = brand,
        quantity = quantityInStash,
        weightCategory = weightCategory,
        colorName = colorName,
        colorNumber = colorNumber,
        dyeLot = dyeLot,
    )

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
        Spacer(modifier = Modifier.width(ProjectOverviewDimens.DetailLabelGap))
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

    OverviewSectionHeader(R.string.care_symbols)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
    ) {
        selectedSymbols.forEach { symbol ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.knitToolsColors.cardContainer,
            ) {
                CareSymbolIcon(
                    symbol = symbol,
                    modifier = Modifier.padding(ProjectOverviewDimens.ContentGap),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
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
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            // CPD-ON
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(text = stringResource(R.string.select_project))

            projects.forEach { project ->
                val isSelected = project.id == linkedProjectId
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = cardContainerColor(selected = isSelected),
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
