package com.finnvek.knittools.ui.screens.ravelry

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.screens.library.SelectionIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun RavelrySearchScreen(
    onPatternClick: (Int) -> Unit,
    onLocalPatternClick: (Long) -> Unit,
    onSavedPatterns: () -> Unit,
    onBack: () -> Unit,
    viewModel: RavelryViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedPatterns by viewModel.savedPatterns.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isSavedSelectMode by viewModel.isSavedSelectMode.collectAsStateWithLifecycle()
    val selectedSavedIds by viewModel.selectedSavedIds.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    BackHandler(enabled = isSavedSelectMode) {
        viewModel.exitSavedSelectMode()
    }

    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_pattern),
            message = stringResource(R.string.delete_patterns_confirm, selectedSavedIds.size),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSelectedSaved()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSavedSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.n_selected, selectedSavedIds.size),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSavedSelectMode() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAllSaved(savedPatterns.map { it.id }) }) {
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
                            text = stringResource(R.string.tool_ravelry),
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
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSavedSelectMode && selectedSavedIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Kirjautumisbanneri
            if (!isAuthenticated && !isSavedSelectMode) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            (context as? Activity)?.let { viewModel.startSignIn(it) }
                        },
                    ) {
                        Text(stringResource(R.string.ravelry_sign_in))
                    }
                }
            }

            // Välilehdet (piilotetaan select-modessa)
            if (!isSavedSelectMode) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.ravelry_search)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.ravelry_saved_patterns)) },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    SearchTab(
                        state =
                            SearchTabState(
                                searchQuery = searchQuery,
                                results = results,
                                isLoading = isLoading,
                                error = error,
                            ),
                        onQueryChange = viewModel::updateQuery,
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        },
                        onPatternClick = onPatternClick,
                        onLoadMore = viewModel::loadMore,
                    )
                }

                1 -> {
                    SavedTab(
                        patterns = savedPatterns,
                        isSelectMode = isSavedSelectMode,
                        selectedIds = selectedSavedIds,
                        onPatternClick = onPatternClick,
                        onLocalPatternClick = onLocalPatternClick,
                        onEnterSelectMode = viewModel::enterSavedSelectMode,
                        onToggleSelection = viewModel::toggleSavedSelection,
                    )
                }
            }
        }
    }
}

private data class SearchTabState(
    val searchQuery: String,
    val results: List<com.finnvek.knittools.data.remote.PatternSearchResult>,
    val isLoading: Boolean,
    val error: String?,
)

@Composable
private fun SearchTab(
    state: SearchTabState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPatternClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.results.isNotEmpty()) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            TextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        }

        items(state.results, key = { it.id }) { pattern ->
            PatternCard(
                name = pattern.name,
                designerName = pattern.designer?.name ?: "",
                thumbnailUrl = pattern.firstPhoto?.small2Url,
                difficulty = pattern.difficultyAverage,
                isFree = pattern.free,
                onClick = { onPatternClick(pattern.id) },
            )
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }

        if (state.error != null && state.results.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }

        if (!state.isLoading && state.error == null && state.results.isEmpty() && state.searchQuery.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedTab(
    patterns: List<com.finnvek.knittools.data.local.SavedPatternEntity>,
    isSelectMode: Boolean,
    selectedIds: Set<Long>,
    onPatternClick: (Int) -> Unit,
    onLocalPatternClick: (Long) -> Unit,
    onEnterSelectMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
) {
    if (patterns.isEmpty()) {
        SavedTabEmptyState()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(patterns, key = { it.id }) { pattern ->
                SavedPatternItem(
                    pattern = pattern,
                    isSelectMode = isSelectMode,
                    isSelected = pattern.id in selectedIds,
                    onPatternClick = onPatternClick,
                    onLocalPatternClick = onLocalPatternClick,
                    onEnterSelectMode = onEnterSelectMode,
                    onToggleSelection = onToggleSelection,
                )
            }
        }
    }
}

@Composable
private fun SavedTabEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_saved_patterns),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPatternItem(
    pattern: com.finnvek.knittools.data.local.SavedPatternEntity,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onPatternClick: (Int) -> Unit,
    onLocalPatternClick: (Long) -> Unit,
    onEnterSelectMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
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
                    onClick = {
                        if (isSelectMode) {
                            onToggleSelection(pattern.id)
                        } else if (pattern.ravelryId > 0) {
                            onPatternClick(pattern.ravelryId)
                        } else {
                            onLocalPatternClick(pattern.id)
                        }
                    },
                    onLongClick = {
                        if (!isSelectMode) {
                            onEnterSelectMode(pattern.id)
                        }
                    },
                ),
    ) {
        PatternCard(
            name = pattern.name,
            designerName = pattern.designerName,
            thumbnailUrl = pattern.thumbnailUrl,
            difficulty = pattern.difficulty,
            isFree = pattern.isFree,
            onClick = {
                if (isSelectMode) {
                    onToggleSelection(pattern.id)
                } else if (pattern.ravelryId > 0) {
                    onPatternClick(pattern.ravelryId)
                } else {
                    onLocalPatternClick(pattern.id)
                }
            },
            modifier = Modifier.background(backgroundColor, MaterialTheme.shapes.large),
        )
        if (isSelectMode) {
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
            )
        }
    }
}
