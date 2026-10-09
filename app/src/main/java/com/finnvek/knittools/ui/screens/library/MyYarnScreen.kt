package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.components.LabeledCounterImageButton
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectYarnTextField
import com.finnvek.knittools.ui.components.YarnThumbnail
import com.finnvek.knittools.ui.components.cardContainerColor
import com.finnvek.knittools.ui.components.proBadgeDescription
import com.finnvek.knittools.ui.components.skeinCountText
import com.finnvek.knittools.ui.screens.yarncard.ManualYarnCardInput
import com.finnvek.knittools.ui.screens.yarncard.parseManualYarnQuantity
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

private const val YARN_CARD_SUMMARY_SEPARATOR = ", "
private val yarnCardContentPadding = 14.dp
private val yarnCardLineSpacing = 6.dp

// Data-luokat MyYarnScreen-parametrien ryhmittelyyn (S107)
data class MyYarnState(
    val cards: List<YarnCard>,
    val activeProjectNames: Map<Long, String>,
    val isSelectMode: Boolean,
    val selectedYarnIds: Set<Long>,
    val canCreateYarnCard: Boolean,
    val proStatus: ProStatus,
    val deleteErrorId: Long = 0L,
    val saveErrorId: Long = 0L,
)

data class MyYarnActions(
    val onCardClick: (Long) -> Unit,
    val onCreateYarnCard: (ManualYarnCardInput) -> Boolean,
    val onRetryCreateYarnCard: () -> Boolean,
    val onEnterSelectMode: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onSelectAll: (List<Long>) -> Unit,
    val onDeleteSelected: () -> Unit,
    val onExitSelectMode: () -> Unit,
    val onUpgradeToPro: () -> Unit,
    val onBack: () -> Unit,
)

private enum class PendingYarnProAction {
    OpenCreation,
    RetryCreation,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S3776") // Näkymä kokoaa lankakortin dialogit ja sheetit yhden tilan alle.
fun MyYarnScreen(
    state: MyYarnState,
    actions: MyYarnActions,
) {
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showManualYarnSheet by rememberSaveable { mutableStateOf(false) }
    var pendingProAction by rememberSaveable { mutableStateOf<PendingYarnProAction?>(null) }
    var actionAwaitingEntitlement by rememberSaveable { mutableStateOf<PendingYarnProAction?>(null) }
    var lastHandledDeleteErrorId by rememberSaveable { mutableLongStateOf(state.deleteErrorId) }
    var lastHandledSaveErrorId by rememberSaveable { mutableLongStateOf(state.saveErrorId) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.generic_error_unknown)
    val requestAddYarn = {
        if (state.canCreateYarnCard) {
            showManualYarnSheet = true
        } else {
            pendingProAction = PendingYarnProAction.OpenCreation
        }
    }

    LaunchedEffect(state.deleteErrorId) {
        if (state.deleteErrorId > lastHandledDeleteErrorId) {
            lastHandledDeleteErrorId = state.deleteErrorId
            snackbarHostState.showSnackbar(deleteFailedMessage)
        }
    }

    LaunchedEffect(state.saveErrorId) {
        if (state.saveErrorId > lastHandledSaveErrorId) {
            lastHandledSaveErrorId = state.saveErrorId
            snackbarHostState.showSnackbar(deleteFailedMessage)
        }
    }

    LaunchedEffect(state.canCreateYarnCard, actionAwaitingEntitlement) {
        val action = actionAwaitingEntitlement ?: return@LaunchedEffect
        if (!state.canCreateYarnCard) return@LaunchedEffect
        actionAwaitingEntitlement = null
        when (action) {
            PendingYarnProAction.OpenCreation -> {
                showManualYarnSheet = true
            }

            PendingYarnProAction.RetryCreation -> {
                if (actions.onRetryCreateYarnCard()) showManualYarnSheet = false
            }
        }
    }

    BackHandler(enabled = state.isSelectMode) {
        actions.onExitSelectMode()
    }

    if (showDeleteConfirmDialog) {
        MyYarnDeleteDialog(
            selectedCount = state.selectedYarnIds.size,
            onConfirm = {
                actions.onDeleteSelected()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    if (showManualYarnSheet) {
        ManualYarnCardSheet(
            onSave = { input ->
                if (actions.onCreateYarnCard(input)) {
                    showManualYarnSheet = false
                } else {
                    pendingProAction = PendingYarnProAction.RetryCreation
                }
            },
            onDismiss = { showManualYarnSheet = false },
        )
    }

    pendingProAction?.let { action ->
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.YarnCards,
                ),
            onDismiss = { pendingProAction = null },
            onTrialStarted = {
                pendingProAction = null
                actionAwaitingEntitlement = action
            },
            onSeePro = actions.onUpgradeToPro,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MyYarnTopBar(
                state = state,
                onExitSelectMode = actions.onExitSelectMode,
                onSelectAll = { actions.onSelectAll(state.cards.map { it.id }) },
                onBack = actions.onBack,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            SelectModeDeleteBar(
                visible = state.isSelectMode && state.selectedYarnIds.isNotEmpty(),
                onDeleteClick = { showDeleteConfirmDialog = true },
            )
        },
        floatingActionButton = {
            if (!state.isSelectMode && state.cards.isNotEmpty()) {
                // Sama pilleri kuin Projects-näytön New Project: teksti + kolmiulotteinen plus.
                val addLabel = stringResource(R.string.add_yarn_to_my_yarn)
                val proDescription = proBadgeDescription(state.proStatus)
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = addLabel,
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = requestAddYarn,
                    contentDescription = listOfNotNull(addLabel, proDescription).joinToString(", "),
                    badge = proDescription?.let { { ProBadge(status = state.proStatus) } },
                )
            }
        },
    ) { padding ->
        if (state.cards.isEmpty()) {
            MyYarnEmptyState(
                padding = padding,
                proStatus = state.proStatus,
                onAddYarn = requestAddYarn,
            )
        } else {
            MyYarnList(
                state = state,
                actions = actions,
                padding = padding,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualYarnCardSheet(
    onSave: (ManualYarnCardInput) -> Unit,
    onDismiss: () -> Unit,
    initialInput: ManualYarnCardInput = ManualYarnCardInput(yarnName = ""),
    @StringRes titleRes: Int = R.string.add_yarn_to_my_yarn,
    // Muokkauksessa ohje jätetään pois: "Aloita nimestä" ei päde, kun nimi on jo annettu.
    @StringRes bodyRes: Int? = R.string.manual_yarn_optional_details,
) {
    var yarnName by rememberSaveable { mutableStateOf(initialInput.yarnName) }
    var brand by rememberSaveable { mutableStateOf(initialInput.brand) }
    var quantity by rememberSaveable { mutableStateOf(initialInput.quantity.coerceAtLeast(1).toString()) }
    var weightCategory by rememberSaveable { mutableStateOf(initialInput.weightCategory) }
    var colorName by rememberSaveable { mutableStateOf(initialInput.colorName) }
    var colorNumber by rememberSaveable { mutableStateOf(initialInput.colorNumber) }
    var dyeLot by rememberSaveable { mutableStateOf(initialInput.dyeLot) }

    val validQuantity = parseManualYarnQuantity(quantity)
    FormSheet(
        title = stringResource(titleRes),
        description = bodyRes?.let { stringResource(it) },
        onDismiss = onDismiss,
        confirm =
            FormSheetConfirm(
                text = stringResource(R.string.save),
                enabled = yarnName.isNotBlank() && validQuantity != null,
                onClick = {
                    validQuantity?.let {
                        onSave(
                            ManualYarnCardInput(
                                yarnName = yarnName,
                                brand = brand,
                                quantity = it,
                                weightCategory = weightCategory,
                                colorName = colorName,
                                colorNumber = colorNumber,
                                dyeLot = dyeLot,
                            ),
                        )
                    }
                },
            ),
    ) {
        ProjectYarnTextField(
            value = yarnName,
            onValueChange = { yarnName = it },
            label = stringResource(R.string.project_yarn_name),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = brand,
            onValueChange = { brand = it },
            // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
            label = stringResource(R.string.brand_label),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = stringResource(R.string.quantity_label),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        // CPD-ON
        YarnWeightField(value = weightCategory, onValueChange = { weightCategory = it })
        ProjectYarnTextField(
            value = colorName,
            onValueChange = { colorName = it },
            label = stringResource(R.string.color_name),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = colorNumber,
            onValueChange = { colorNumber = it },
            label = stringResource(R.string.color_number),
            singleLine = true,
        )
        ProjectYarnTextField(
            value = dyeLot,
            onValueChange = { dyeLot = it },
            label = stringResource(R.string.dye_lot),
            singleLine = true,
        )
    }
}

@Composable
private fun MyYarnDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_yarn_card),
        message = pluralStringResource(R.plurals.delete_yarn_cards_confirm, selectedCount, selectedCount),
        confirmText = stringResource(R.string.delete),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyYarnTopBar(
    state: MyYarnState,
    onExitSelectMode: () -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
) {
    LibraryTopBar(
        isSelectMode = state.isSelectMode,
        selectedCount = state.selectedYarnIds.size,
        titleRes = R.string.my_yarn_title,
        onExitSelectMode = onExitSelectMode,
        onSelectAll = onSelectAll,
        onBack = onBack,
    )
}

@Composable
private fun MyYarnEmptyState(
    padding: PaddingValues,
    proStatus: ProStatus,
    onAddYarn: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_my_yarn),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        val addLabel = stringResource(R.string.add_yarn_to_my_yarn)
        val proDescription = proBadgeDescription(proStatus)
        LabeledCounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            label = addLabel,
            visualSize = ProjectListDimens.CreateButtonVisualSize,
            onClick = onAddYarn,
            contentDescription = listOfNotNull(addLabel, proDescription).joinToString(", "),
            badge = proDescription?.let { { ProBadge(status = proStatus) } },
        )
    }
}

@Composable
private fun MyYarnList(
    state: MyYarnState,
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
    actions: MyYarnActions,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // Alareunan tila pitää viimeisen kortin lisäyspainikkeen yläpuolella.
        contentPadding = PaddingValues(bottom = ProjectListDimens.ListBottomPadding),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(state.cards, key = { it.id }) { card ->
            // CPD-ON
            YarnStashCardItem(
                card = card,
                linkedProjectName = card.linkedProjectId?.let(state.activeProjectNames::get),
                isSelectMode = state.isSelectMode,
                isSelected = card.id in state.selectedYarnIds,
                onClick = {
                    if (state.isSelectMode) {
                        actions.onToggleSelection(card.id)
                    } else {
                        actions.onCardClick(card.id)
                    }
                },
                onLongClick = {
                    if (!state.isSelectMode) {
                        actions.onEnterSelectMode(card.id)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YarnStashCardItem(
    card: YarnCard,
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
    linkedProjectName: String?,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor = cardContainerColor(selected = isSelected)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).then(
                    if (isSelectMode) {
                        Modifier.semantics { selected = isSelected }
                    } else {
                        Modifier
                    },
                ),
    ) {
        // CPD-ON
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = if (isSelectMode) 48.dp else 0.dp),
            shape = MaterialTheme.shapes.large,
            color = backgroundColor,
        ) {
            YarnCardContent(
                card = card,
                linkedProjectName = linkedProjectName,
                showOpenAffordance = !isSelectMode,
            )
        }

        if (isSelectMode) {
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun YarnCardContent(
    card: YarnCard,
    linkedProjectName: String?,
    showOpenAffordance: Boolean,
) {
    val fallbackName = stringResource(R.string.yarn_card_number_fallback, card.id)
    val displayName = card.displayName { fallbackName }
    // Projektirivi vain linkitetylle langalle ja nimikkeen kanssa: pelkkä nimi tai "Not linked" ei kertonut,
    // mitä rivi tarkoittaa.
    val projectLine = linkedProjectName?.let { stringResource(R.string.yarn_card_project_format, it) }

    Row(
        modifier = Modifier.padding(yarnCardContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kuva vain kun sellainen on: neutraali laatta ei kertoisi langasta mitään (kuten projektinäkymässä).
        card.photoUri.takeIf(String::isNotBlank)?.let { photoUri ->
            YarnThumbnail(
                photoUri = photoUri,
                size = ProjectListDimens.ThumbnailSizeCompact,
                modifier = Modifier.align(Alignment.Top),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(yarnCardLineSpacing),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            YarnCardMetaLine(card = card)
            YarnManualColorRow(card = card)
            projectLine?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                )
            }
        }
        // "For Linen Market Bag" kertoo jo käytön: lähes jokaisessa kortissa toistuva IN USE peitti poikkeavat
        // tilat. Pilleri näkyy varastossa olevalle, valmiille ja projektiin linkittämättömälle käytössä olevalle.
        if (card.status != YarnCardStatus.IN_USE || projectLine == null) {
            YarnStatusPill(status = card.status, modifier = Modifier.align(Alignment.Top))
        }
        if (showOpenAffordance) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun YarnCardMetaLine(card: YarnCard) {
    // Tila on kortin oikeassa reunassa pillerinä, joten sitä ei toisteta tällä rivillä.
    val summary =
        listOfNotNull(
            card.weightCategory.takeIf { it.isNotBlank() },
            skeinCountText(card.quantityInStash),
        ).joinToString(YARN_CARD_SUMMARY_SEPARATOR)

    Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
    )
}

@Composable
private fun YarnManualColorRow(card: YarnCard) {
    // Lyhyet muodot ("#101", "Lot D24-11"): täydet nimikkeet rivittivät rivin kahdelle riville.
    val colorNumber =
        card.colorNumber.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.yarn_color_number_short, it)
        }
    val dyeLot = card.dyeLot.takeIf { it.isNotBlank() }?.let { stringResource(R.string.yarn_dye_lot_short, it) }
    val colorSummary =
        listOfNotNull(card.colorName.takeIf { it.isNotBlank() }, colorNumber, dyeLot)
            .joinToString(YARN_CARD_SUMMARY_SEPARATOR)
            .takeIf { it.isNotBlank() } ?: return

    // Ei väripistettä: sen sävy ei ollut langan väri, joten se johti harhaan.
    Text(
        text = colorSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
    )
}
