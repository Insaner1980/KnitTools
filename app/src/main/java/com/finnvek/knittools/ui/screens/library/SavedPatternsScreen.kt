package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.LabeledCounterImageButton
import com.finnvek.knittools.ui.components.SelectionAction
import com.finnvek.knittools.ui.components.SelectionActionBar
import com.finnvek.knittools.ui.components.cardContainerColor
import com.finnvek.knittools.ui.components.withExtraBottom
import com.finnvek.knittools.ui.screens.ravelry.PatternCard
import com.finnvek.knittools.ui.screens.ravelry.PatternCardState
import com.finnvek.knittools.ui.theme.ProjectListDimens

// Data-luokat SavedPatternsScreen-parametrien ryhmittelyyn (S107)
data class SavedPatternsState(
    val patterns: List<SavedPattern>,
    val isSelectMode: Boolean,
    val selectedPatternIds: Set<Long>,
    val deleteErrorId: Long,
)

data class SavedPatternsActions(
    val onPatternClick: (Long) -> Unit,
    val onAddWebPattern: () -> Unit,
    val onEnterSelectMode: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onSelectAll: (List<Long>) -> Unit,
    val onDeleteSelected: () -> Unit,
    val onExitSelectMode: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPatternsScreen(
    state: SavedPatternsState,
    actions: SavedPatternsActions,
) {
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var lastHandledDeleteErrorId by rememberSaveable { mutableLongStateOf(state.deleteErrorId) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.generic_error_unknown)

    BackHandler(enabled = state.isSelectMode) {
        actions.onExitSelectMode()
    }

    LaunchedEffect(state.deleteErrorId) {
        if (state.deleteErrorId > lastHandledDeleteErrorId) {
            lastHandledDeleteErrorId = state.deleteErrorId
            snackbarHostState.showSnackbar(deleteFailedMessage)
        }
    }

    if (showDeleteConfirmDialog) {
        SavedPatternsDeleteDialog(
            selectedCount = state.selectedPatternIds.size,
            onConfirm = {
                actions.onDeleteSelected()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    val density = LocalDensity.current
    var actionHeight by remember { mutableStateOf(0.dp) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SavedPatternsTopBar(
                state = state,
                onExitSelectMode = actions.onExitSelectMode,
                onSelectAll = { actions.onSelectAll(state.patterns.map { it.id }) },
                onBack = actions.onBack,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!state.isSelectMode && state.patterns.isNotEmpty()) {
                // Sama pilleri kuin New Project ja Add Yarn: teksti + kolmiulotteinen plus.
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = stringResource(R.string.web_pattern_add),
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = actions.onAddWebPattern,
                    modifier = Modifier.onSizeChanged { actionHeight = with(density) { it.height.toDp() } },
                )
            }
        },
        bottomBar = {
            SelectModeDeleteBar(
                visible = state.isSelectMode && state.selectedPatternIds.isNotEmpty(),
                onDeleteClick = { showDeleteConfirmDialog = true },
            )
        },
    ) { padding ->
        if (state.patterns.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_saved_patterns),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledCounterImageButton(
                    imageRes = R.drawable.counter_plus_button,
                    label = stringResource(R.string.web_pattern_add),
                    visualSize = ProjectListDimens.CreateButtonVisualSize,
                    onClick = actions.onAddWebPattern,
                )
            }
        } else {
            SavedPatternsList(
                state = state,
                actions = actions,
                padding =
                    padding.withExtraBottom(actionHeight + 32.dp, LocalLayoutDirection.current),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPatternsList(
    state: SavedPatternsState,
    actions: SavedPatternsActions,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(state.patterns, key = { it.id }) { pattern ->
            SavedPatternItem(
                pattern = pattern,
                isSelectMode = state.isSelectMode,
                isSelected = pattern.id in state.selectedPatternIds,
                onClick = {
                    if (state.isSelectMode) {
                        actions.onToggleSelection(pattern.id)
                    } else {
                        actions.onPatternClick(pattern.id)
                    }
                },
                onLongClick = {
                    if (!state.isSelectMode) {
                        actions.onEnterSelectMode(pattern.id)
                    }
                },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun SavedPatternsDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_pattern),
        message = pluralStringResource(R.plurals.delete_patterns_confirm, selectedCount, selectedCount),
        scrollableMessage = true,
        confirmText = stringResource(R.string.delete),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPatternsTopBar(
    state: SavedPatternsState,
    onExitSelectMode: () -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
) {
    LibraryTopBar(
        isSelectMode = state.isSelectMode,
        selectedCount = state.selectedPatternIds.size,
        titleRes = R.string.saved_patterns_title,
        onExitSelectMode = onExitSelectMode,
        onSelectAll = onSelectAll,
        onBack = onBack,
    )
}

// Jaettu poistopalkki valintamoodille (käytetään useasta näytöstä)
@Composable
internal fun SelectModeDeleteBar(
    visible: Boolean,
    onDeleteClick: () -> Unit,
) {
    // Sama matala tekstitoimintopalkki kuin projektien joukkovalinnassa.
    SelectionActionBar(
        visible = visible,
        actions = listOf(SelectionAction(stringResource(R.string.delete), onDeleteClick, destructive = true)),
    )
}

// Jaettu valintaindikaattori multi-select-moodeille
@Composable
internal fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconTint =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }
    Box(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                ).padding(2.dp),
    ) {
        Icon(
            imageVector =
                if (isSelected) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.Circle
                },
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPatternItem(
    pattern: SavedPattern,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor = cardContainerColor(selected = isSelected)

    Box(modifier = Modifier.fillMaxWidth()) {
        if (pattern.isWebPatternCompatible) {
            WebPatternCard(
                pattern = pattern,
                backgroundColor = backgroundColor,
                onClick = onClick,
                onLongClick = onLongClick,
                selection = isSelected.takeIf { isSelectMode },
            )
        } else {
            PatternCard(
                state =
                    PatternCardState(
                        name = pattern.name,
                        designerName = pattern.designerName,
                        thumbnailUrl = pattern.thumbnailUrl,
                        difficulty = pattern.difficulty,
                        availability = pattern.availability,
                        sourceLabel = savedPatternSourceLabel(pattern.source),
                    ),
                onClick = onClick,
                onLongClick = onLongClick,
                modifier =
                    Modifier
                        .padding(start = if (isSelectMode) 48.dp else 0.dp)
                        .background(backgroundColor, MaterialTheme.shapes.large)
                        .then(
                            if (isSelectMode) {
                                Modifier.semantics { selected = isSelected }
                            } else {
                                Modifier
                            },
                        ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WebPatternCard(
    pattern: SavedPattern,
    backgroundColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selection: Boolean?,
) {
    val host = pattern.webPatternUrlOrNull?.host.orEmpty()
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = if (selection != null) 48.dp else 0.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .then(
                    if (selection != null) {
                        Modifier.semantics { selected = selection }
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleMedium,
            )
            pattern.designerName.takeIf { it.isNotBlank() }?.let { designer ->
                Text(
                    text = designer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Lähde samalla merkillä kuin Ravelry- ja PDF-kaavoilla.
            BadgePill(text = stringResource(R.string.web_pattern_label))
            if (host.isNotBlank()) {
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Kaavan lähde merkkinä: tallennetut kaavat ovat vain Libraryssa, joten lähde kerrotaan listassa. */
@Composable
private fun savedPatternSourceLabel(source: SavedPatternSource): String? =
    when (source) {
        SavedPatternSource.Ravelry -> stringResource(R.string.tool_ravelry)
        SavedPatternSource.LocalFile -> stringResource(R.string.pattern_source_pdf)
        SavedPatternSource.WebLink,
        SavedPatternSource.Other,
        SavedPatternSource.Unknown,
        -> null
    }
