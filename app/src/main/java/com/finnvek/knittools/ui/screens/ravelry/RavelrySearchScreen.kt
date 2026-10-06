package com.finnvek.knittools.ui.screens.ravelry

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.auth.RavelryAuthState
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.StatusMessage
import com.finnvek.knittools.ui.components.StatusMessageType
import com.finnvek.knittools.ui.components.highContainerTextFieldColors

data class RavelrySearchActions(
    val onPatternClick: (Int) -> Unit,
    val onBack: () -> Unit,
    val onLaunchRavelryAuth: (Uri) -> Unit = {},
    val onBrowseRavelry: () -> Unit = {},
    val onSavedPatternDetail: (Long) -> Unit = {},
    val onOpenSavedPatterns: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RavelrySearchScreen(
    actions: RavelrySearchActions,
    importUrl: String? = null,
    viewModelProvider: @Composable () -> RavelryViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val submittedQuery by viewModel.submittedQuery.collectAsStateWithLifecycle()
    val hasSubmittedSearch by viewModel.hasSubmittedSearch.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val savedPatterns by viewModel.savedPatterns.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val importConfirmationState by viewModel.importConfirmationState.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(viewModel) {
        viewModel.refreshAuthStatus()
    }

    CollectWithLifecycleEffect({ viewModel.signInLaunchRequests }) { uri ->
        actions.onLaunchRavelryAuth(uri)
    }

    LaunchedEffect(viewModel, importUrl) {
        importUrl?.let(viewModel::showImportConfirmationForUrl)
    }

    importConfirmationState?.let { state ->
        RavelryImportConfirmationSheet(
            state = state,
            onConfirmImport = viewModel::retryImportConfirmation,
            onSave = viewModel::saveImportPattern,
            onSignIn = viewModel::startSignIn,
            onRetry = viewModel::retryImportConfirmation,
            onOpenSavedPattern = actions.onSavedPatternDetail,
            onDismiss = viewModel::dismissImportConfirmation,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_ravelry),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
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
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            // Tallennetut kaavat ovat vain Libraryssa: sama lista kahdessa paikassa eri näköisenä hämmensi.
            val header: @Composable () -> Unit = {
                RavelryAccountHeader(
                    authState = authState,
                    onSignIn = viewModel::startSignIn,
                    onBrowseRavelry = actions.onBrowseRavelry,
                    onDisconnect = viewModel::disconnectRavelry,
                )
                OverviewLinkRow(
                    title = stringResource(R.string.ravelry_saved_patterns),
                    subtitle = null,
                    icon = null,
                    onClick = actions.onOpenSavedPatterns,
                )
            }

            SearchTab(
                header = header,
                state =
                    SearchTabState(
                        searchQuery = searchQuery,
                        submittedQuery = submittedQuery,
                        hasSubmittedSearch = hasSubmittedSearch,
                        results = results,
                        isLoading = isLoading,
                        searchError = searchError,
                        canSearch = authState is RavelryAuthState.Connected,
                        savedRavelryPatternIds = savedPatterns.mapNotNull { it.ravelryPatternId }.toSet(),
                    ),
                onQueryChange = viewModel::updateQuery,
                onSearch = {
                    keyboardController?.hide()
                    viewModel.search()
                },
                onPatternClick = actions.onPatternClick,
                onImportPattern = { patternId ->
                    viewModel.showImportConfirmationForPattern(patternId)
                },
                onLoadMore = viewModel::loadMore,
            )
        }
    }
}

@Immutable
private data class SearchTabState(
    val searchQuery: String,
    val submittedQuery: String,
    val hasSubmittedSearch: Boolean,
    val results: List<com.finnvek.knittools.data.remote.PatternSearchResult>,
    val isLoading: Boolean,
    val searchError: RavelrySearchError?,
    val canSearch: Boolean,
    val savedRavelryPatternIds: Set<Int>,
)

internal enum class RavelrySearchResultCardAction {
    OpenSavedPattern,
    SavePattern,
}

internal fun ravelrySearchResultAction(
    patternId: Int,
    savedRavelryPatternIds: Set<Int>,
): RavelrySearchResultCardAction =
    if (patternId in savedRavelryPatternIds) {
        RavelrySearchResultCardAction.OpenSavedPattern
    } else {
        RavelrySearchResultCardAction.SavePattern
    }

internal fun shouldRequestRavelryLoadMore(
    shouldLoadMore: Boolean,
    canSearch: Boolean,
    resultCount: Int,
    isLoading: Boolean,
    hasError: Boolean,
    isCurrentSubmittedSearch: Boolean,
): Boolean =
    shouldLoadMore &&
        canSearch &&
        resultCount > 0 &&
        !isLoading &&
        !hasError &&
        isCurrentSubmittedSearch

internal fun shouldShowRavelryEmptyState(
    isLoading: Boolean,
    hasError: Boolean,
    resultCount: Int,
    searchQuery: String,
    submittedQuery: String,
    hasSubmittedSearch: Boolean,
): Boolean =
    !isLoading &&
        !hasError &&
        resultCount == 0 &&
        hasSubmittedSearch &&
        searchQuery.trim() == submittedQuery &&
        submittedQuery.isNotEmpty()

private fun SearchTabState.isCurrentSubmittedSearch(): Boolean =
    hasSubmittedSearch &&
        searchQuery.trim() == submittedQuery &&
        submittedQuery.isNotEmpty()

@Composable
private fun SearchTab(
    header: @Composable () -> Unit,
    state: SearchTabState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPatternClick: (Int) -> Unit,
    onImportPattern: (Int) -> Unit,
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
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val hasError = state.searchError != null
    val isCurrentSubmittedSearch = state.isCurrentSubmittedSearch()
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(
        shouldLoadMore,
        state.canSearch,
        state.results.size,
        state.isLoading,
        hasError,
        isCurrentSubmittedSearch,
    ) {
        if (
            shouldRequestRavelryLoadMore(
                shouldLoadMore = shouldLoadMore,
                canSearch = state.canSearch,
                resultCount = state.results.size,
                isLoading = state.isLoading,
                hasError = hasError,
                isCurrentSubmittedSearch = isCurrentSubmittedSearch,
            )
        ) {
            currentOnLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { header() }
        // Ilman kirjautumista kenttä näkyy käytöstä poistettuna; syyn ja kirjautumisen kertoo
        // tilikortti, joten samaa viestiä ei toisteta kentän alla.
        item {
            RavelrySearchField(
                query = state.searchQuery,
                canSearch = state.canSearch,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
            )
        }
        ravelrySearchResults(
            state = state,
            onPatternClick = onPatternClick,
            onImportPattern = onImportPattern,
        )
        ravelrySearchLoadingItem(isLoading = state.isLoading)
        ravelrySearchErrorItem(
            searchError = state.searchError,
            hasResults = state.results.isNotEmpty(),
            canSearch = state.canSearch,
            retryLabel = retryLabel,
            onSearch = onSearch,
            onLoadMore = onLoadMore,
        )
        ravelrySearchEmptyStateItem(
            shouldShow =
                shouldShowRavelryEmptyState(
                    isLoading = state.isLoading,
                    hasError = hasError,
                    resultCount = state.results.size,
                    searchQuery = state.searchQuery,
                    submittedQuery = state.submittedQuery,
                    hasSubmittedSearch = state.hasSubmittedSearch,
                ),
        )
    }
}

@Composable
private fun RavelrySearchField(
    query: String,
    canSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        enabled = canSearch,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    if (canSearch) {
                        onSearch()
                    }
                },
            ),
        colors =
            highContainerTextFieldColors(),
    )
}

private fun LazyListScope.ravelrySearchResults(
    state: SearchTabState,
    onPatternClick: (Int) -> Unit,
    onImportPattern: (Int) -> Unit,
) {
    items(state.results, key = { it.id }) { pattern ->
        val cardAction =
            ravelrySearchResultAction(
                patternId = pattern.id,
                savedRavelryPatternIds = state.savedRavelryPatternIds,
            )
        PatternCard(
            state =
                PatternCardState(
                    name = pattern.name,
                    designerName = pattern.designer?.name ?: "",
                    thumbnailUrl = pattern.firstPhoto?.small2Url,
                    difficulty = pattern.difficultyAverage,
                    availability = pattern.availability,
                ),
            onClick = { onPatternClick(pattern.id) },
            actionContent = {
                RavelrySearchResultActionContent(
                    action = cardAction,
                    onOpen = { onPatternClick(pattern.id) },
                    onSave = { onImportPattern(pattern.id) },
                )
            },
        )
    }
}

private fun LazyListScope.ravelrySearchLoadingItem(isLoading: Boolean) {
    if (!isLoading) return

    item {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
    }
}

private fun LazyListScope.ravelrySearchErrorItem(
    searchError: RavelrySearchError?,
    hasResults: Boolean,
    canSearch: Boolean,
    retryLabel: String,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
) {
    if (searchError == null) return

    item {
        val retryAction =
            when {
                !canSearch -> null
                hasResults -> onLoadMore
                else -> onSearch
            }
        StatusMessage(
            message = stringResource(searchError.messageRes(isLoadMoreError = hasResults)),
            type = StatusMessageType.Error,
            actionLabel = if (canSearch) retryLabel else null,
            onAction = retryAction,
            modifier = if (hasResults) Modifier else Modifier.padding(vertical = 24.dp),
        )
    }
}

private fun LazyListScope.ravelrySearchEmptyStateItem(shouldShow: Boolean) {
    if (!shouldShow) return

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

@Composable
private fun RavelrySearchResultActionContent(
    action: RavelrySearchResultCardAction,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    when (action) {
        RavelrySearchResultCardAction.OpenSavedPattern -> {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.pattern_saved),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.ravelry_open_saved_pattern))
                }
            }
        }

        RavelrySearchResultCardAction.SavePattern -> {
            Button(
                onClick = onSave,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.save_pattern))
            }
        }
    }
}

private fun RavelrySearchError.messageRes(isLoadMoreError: Boolean): Int =
    when (this) {
        RavelrySearchError.Network, RavelrySearchError.Unknown -> {
            if (isLoadMoreError) {
                R.string.search_more_error
            } else {
                R.string.search_error
            }
        }

        RavelrySearchError.RateLimited -> {
            R.string.ravelry_search_rate_limited
        }

        RavelrySearchError.Authentication -> {
            R.string.ravelry_search_auth_error
        }

        RavelrySearchError.ServiceUnavailable -> {
            R.string.ravelry_search_service_error
        }
    }
