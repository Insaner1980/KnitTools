package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.theme.knitToolsColors

// Data-luokat MyYarnScreen-parametrien ryhmittelyyn (S107)
data class MyYarnState(
    val cards: List<YarnCardEntity>,
    val activeProjectNames: Map<Long, String>,
    val isSelectMode: Boolean,
    val selectedYarnIds: Set<Long>,
)

data class MyYarnActions(
    val onCardClick: (Long) -> Unit,
    val onEnterSelectMode: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onSelectAll: (List<Long>) -> Unit,
    val onDeleteSelected: () -> Unit,
    val onExitSelectMode: () -> Unit,
    val onScanLabel: (() -> Unit)? = null,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyYarnScreen(
    state: MyYarnState,
    actions: MyYarnActions,
) {
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!state.isSelectMode) {
                actions.onScanLabel?.let { onScan ->
                    FloatingActionButton(
                        onClick = onScan,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.scan_yarn_label),
                        )
                    }
                }
            }
        },
        topBar = {
            MyYarnTopBar(
                state = state,
                onExitSelectMode = actions.onExitSelectMode,
                onSelectAll = { actions.onSelectAll(state.cards.map { it.id }) },
                onBack = actions.onBack,
            )
        },
        bottomBar = {
            SelectModeDeleteBar(
                visible = state.isSelectMode && state.selectedYarnIds.isNotEmpty(),
                onDeleteClick = { showDeleteConfirmDialog = true },
            )
        },
    ) { padding ->
        if (state.cards.isEmpty()) {
            MyYarnEmptyState(padding)
        } else {
            MyYarnList(
                state = state,
                actions = actions,
                padding = padding,
            )
        }
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
        message = stringResource(R.string.delete_yarn_cards_confirm, selectedCount),
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
    if (state.isSelectMode) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.n_selected, state.selectedYarnIds.size),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onExitSelectMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            },
            actions = {
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.select_all))
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.my_yarn_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
        )
    }
}

@Composable
private fun MyYarnEmptyState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.camera_icon),
            contentDescription = null,
            modifier = Modifier.size(280.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_my_yarn),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MyYarnList(
    state: MyYarnState,
    actions: MyYarnActions,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(state.cards, key = { it.id }) { card ->
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
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YarnStashCardItem(
    card: YarnCardEntity,
    linkedProjectName: String?,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = backgroundColor,
        ) {
            YarnCardContent(card = card, linkedProjectName = linkedProjectName)
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
    card: YarnCardEntity,
    linkedProjectName: String?,
) {
    val displayName = card.yarnName.ifBlank { stringResource(R.string.yarn_card_fallback_name) }
    val status = yarnStatusUi(card.status)

    Column(modifier = Modifier.padding(14.dp)) {
        if (card.brand.isNotBlank()) {
            Text(
                text = card.brand,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (card.weightCategory.isNotBlank()) {
                WeightCategoryPill(text = card.weightCategory)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(status = status)
            QuantityPill(quantity = card.quantityInStash)
        }
        if (card.status == "IN_USE" && !linkedProjectName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.linked_project_arrow, linkedProjectName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeightCategoryPill(text: String) = BadgePill(text = text)

@Composable
private fun StatusPill(status: YarnStatusUi) {
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelSmall,
        color = status.contentColor,
        modifier =
            Modifier
                .background(
                    color = status.containerColor,
                    shape = MaterialTheme.shapes.small,
                ).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun QuantityPill(quantity: Int) {
    Text(
        text = skeinCountText(quantity),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small,
                ).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun skeinCountText(quantity: Int): String =
    if (quantity == 1) {
        stringResource(R.string.skein_count_one, quantity)
    } else {
        stringResource(R.string.skein_count_many, quantity)
    }
