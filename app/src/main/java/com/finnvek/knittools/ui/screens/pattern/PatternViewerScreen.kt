package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import kotlinx.coroutines.withContext

private data class PatternRenderState(
    val renderer: PdfPageRenderer?,
    val rendererError: String?,
    val renderedBitmap: Bitmap?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternViewerScreen(
    onBack: () -> Unit,
    counterViewModel: CounterViewModel,
) {
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    val patternUri = counterState.patternUri
    val currentPage = counterState.currentPatternPage
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = counterViewModel::updatePatternPage,
        )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PatternViewerTopBar(
                state =
                    TopBarState(
                        patternName = counterState.patternName,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        canDetachPattern = true,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = counterViewModel::updatePatternPage,
                        onDetachPattern = {
                            counterViewModel.detachPattern()
                            onBack()
                        },
                    ),
            )
        },
        bottomBar = {
            PatternViewerBottomBar(
                state =
                    BottomBarState(
                        currentRow = counterState.counter.count,
                        currentPage = currentPage,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                    ),
                actions =
                    BottomBarActions(
                        onPreviousRow = counterViewModel::decrement,
                        onNextRow = counterViewModel::increment,
                        onPreviousPage = { counterViewModel.updatePatternPage(currentPage - 1) },
                        onNextPage = { counterViewModel.updatePatternPage(currentPage + 1) },
                    ),
            )
        },
    ) { scaffoldPadding ->
        PatternViewerContent(
            patternUri = patternUri,
            rendererError = renderState.rendererError,
            renderedBitmap = renderState.renderedBitmap,
            patternName = counterState.patternName,
            currentRow = counterState.counter.count,
            positionPercent = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPatternViewerScreen(
    patternUri: String?,
    patternName: String?,
    onBack: () -> Unit,
) {
    var currentPage by rememberSaveable(patternUri) { mutableIntStateOf(0) }
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = { currentPage = it },
        )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PatternViewerTopBar(
                state =
                    TopBarState(
                        patternName = patternName,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        canDetachPattern = false,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = { currentPage = it },
                        onDetachPattern = {},
                    ),
            )
        },
        bottomBar = {
            LibraryPatternViewerBottomBar(
                currentPage = currentPage,
                totalPages = renderState.renderer?.pageCount ?: 0,
                onPreviousPage = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                onNextPage = {
                    val maxPage = (renderState.renderer?.pageCount ?: 1) - 1
                    currentPage = (currentPage + 1).coerceAtMost(maxPage.coerceAtLeast(0))
                },
            )
        },
    ) { scaffoldPadding ->
        PatternViewerContent(
            patternUri = patternUri,
            rendererError = renderState.rendererError,
            renderedBitmap = renderState.renderedBitmap,
            patternName = patternName,
            currentRow = null,
            positionPercent = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        )
    }
}

@Composable
private fun rememberPatternRenderState(
    patternUri: String?,
    currentPage: Int,
    onPageClamped: (Int) -> Unit,
): PatternRenderState {
    val context = LocalContext.current
    val patternOpenFailed = stringResource(R.string.pattern_open_failed)
    var renderer by remember(patternUri) { mutableStateOf<PdfPageRenderer?>(null) }
    var rendererError by remember(patternUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(patternUri) {
        renderer?.close()
        renderer = null
        rendererError = null
        if (patternUri == null) return@LaunchedEffect
        val createdRenderer =
            withContext(AppDispatchers.IO) {
                runCatching { PdfPageRenderer(context, patternUri.toUri()) }
            }
        createdRenderer
            .onSuccess { pdfRenderer ->
                renderer = pdfRenderer
                val maxPage = (pdfRenderer.pageCount - 1).coerceAtLeast(0)
                if (currentPage > maxPage) {
                    onPageClamped(maxPage)
                }
            }.onFailure {
                rendererError = patternOpenFailed
            }
    }

    DisposableEffect(patternUri) {
        onDispose {
            renderer?.close()
            renderer = null
        }
    }

    val renderedBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = renderer,
        key2 = currentPage,
    ) {
        val activeRenderer =
            renderer ?: run {
                value = null
                return@produceState
            }
        value =
            withContext(AppDispatchers.IO) {
                runCatching {
                    activeRenderer.renderPage(currentPage, 1600)
                }.getOrNull()
            }
    }

    return PatternRenderState(
        renderer = renderer,
        rendererError = rendererError,
        renderedBitmap = renderedBitmap,
    )
}

// Tilan ja toimintojen ryhmittely PatternViewerTopBarille (S107)
private data class TopBarState(
    val patternName: String?,
    val totalPages: Int,
    val currentPage: Int,
    val canDetachPattern: Boolean,
)

private data class TopBarActions(
    val onBack: () -> Unit,
    val onJumpToPage: (Int) -> Unit,
    val onDetachPattern: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternViewerTopBar(
    state: TopBarState,
    actions: TopBarActions,
) {
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showPageJumpDialog by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = state.patternName ?: stringResource(R.string.pattern_viewer_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        actions = {
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pattern_page_jump)) },
                        onClick = {
                            showOverflowMenu = false
                            showPageJumpDialog = true
                        },
                    )
                    if (state.canDetachPattern) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_pattern)) },
                            onClick = {
                                showOverflowMenu = false
                                actions.onDetachPattern()
                            },
                        )
                    }
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
    )

    if (showPageJumpDialog) {
        PatternPageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismiss = { showPageJumpDialog = false },
            onConfirm = { page ->
                actions.onJumpToPage(page)
                showPageJumpDialog = false
            },
        )
    }
}

@Composable
private fun PatternPageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var pageInput by rememberSaveable(totalPages, currentPage) { mutableStateOf((currentPage + 1).toString()) }
    val parsedPage = pageInput.toIntOrNull()
    val isValidPage = parsedPage != null && parsedPage in 1..totalPages.coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_page_jump)) },
        text = {
            TextField(
                value = pageInput,
                onValueChange = { value ->
                    pageInput = value.filter(Char::isDigit)
                },
                singleLine = true,
                label = { Text(stringResource(R.string.pattern_page_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        text =
                            stringResource(
                                R.string.pattern_page_indicator,
                                currentPage + 1,
                                totalPages.coerceAtLeast(1),
                            ),
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm((parsedPage ?: 1) - 1) },
                enabled = isValidPage,
            ) {
                Text(stringResource(R.string.confirm))
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
private fun LibraryPatternViewerBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 0,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pattern_previous_page),
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.pattern_page_indicator,
                        currentPage + 1,
                        totalPages.coerceAtLeast(1),
                    ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages - 1,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.pattern_next_page),
                )
            }
        }
    }
}

@Composable
private fun PatternViewerContent(
    patternUri: String?,
    rendererError: String?,
    renderedBitmap: Bitmap?,
    patternName: String?,
    currentRow: Int?,
    positionPercent: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when {
            patternUri == null -> {
                PatternViewerMessage(message = stringResource(R.string.no_pattern_attached))
            }

            rendererError != null -> {
                PatternViewerMessage(
                    message = rendererError.ifBlank { stringResource(R.string.pattern_open_failed) },
                )
            }

            renderedBitmap == null -> {
                PatternViewerMessage(message = stringResource(R.string.pattern_loading))
            }

            else -> {
                PatternViewerDocument(
                    renderedBitmap = renderedBitmap,
                    patternName = patternName,
                    currentRow = currentRow,
                    positionPercent = positionPercent,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PatternViewerDocument(
    renderedBitmap: Bitmap,
    patternName: String?,
    currentRow: Int?,
    positionPercent: Int?,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState =
        rememberTransformableState { _, zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += panChange
            } else {
                offset = Offset.Zero
            }
        }

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState()),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val aspectRatio = renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat()
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(maxWidth / aspectRatio)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                            )
                        }.transformable(state = transformableState)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
            ) {
                Image(
                    bitmap = renderedBitmap.asImageBitmap(),
                    contentDescription = patternName,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
                RowHighlightOverlay(
                    yPosition = positionPercent?.let { it / 100f },
                    modifier = Modifier.fillMaxSize(),
                    accessibilityDescription =
                        if (currentRow != null && positionPercent != null) {
                            stringResource(R.string.pattern_row_highlight_description, currentRow, positionPercent)
                        } else {
                            null
                        },
                )
            }
        }
    }
}

// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille (S107)
private data class BottomBarState(
    val currentRow: Int,
    val currentPage: Int,
    val totalPages: Int,
)

private data class BottomBarActions(
    val onPreviousRow: () -> Unit,
    val onNextRow: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)

@Composable
private fun PatternViewerBottomBar(
    state: BottomBarState,
    actions: BottomBarActions,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BottomBarNavigationRow(state = state, actions = actions)
        }
    }
}

@Composable
private fun BottomBarNavigationRow(
    state: BottomBarState,
    actions: BottomBarActions,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.current_row_short, state.currentRow),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = actions.onPreviousRow) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.counter_decrease),
                )
            }
            IconButton(onClick = actions.onNextRow) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.counter_increase),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = actions.onPreviousPage,
                enabled = state.currentPage > 0,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pattern_previous_page),
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.pattern_page_indicator,
                        state.currentPage + 1,
                        state.totalPages.coerceAtLeast(1),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(
                onClick = actions.onNextPage,
                enabled = state.currentPage < state.totalPages - 1,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.pattern_next_page),
                )
            }
        }
    }
}

@Composable
private fun PatternViewerMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
