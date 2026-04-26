package com.finnvek.knittools.ui.screens.pattern

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.CombinedInstructionResult
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    viewModel: PatternViewerViewModel = hiltViewModel(),
) {
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    val instructionState by viewModel.instructionState.collectAsStateWithLifecycle()
    val explanationState by viewModel.explanationState.collectAsStateWithLifecycle()
    val combineState by viewModel.combineState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExplanationSheet by rememberSaveable { mutableStateOf(false) }
    val patternUri = counterState.patternUri
    val currentPage = counterState.currentPatternPage
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = counterViewModel::updatePatternPage,
        )

    LaunchedEffect(
        patternUri,
        currentPage,
        counterState.counter.count,
        counterState.isPro,
        renderState.renderedBitmap,
    ) {
        viewModel.onViewerContextChanged(
            patternUri = patternUri,
            currentPage = currentPage,
            currentRow = counterState.counter.count,
            renderedBitmap = renderState.renderedBitmap,
            canDisplayInstruction = counterState.isPro,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            PatternViewerTopBar(
                state =
                    TopBarState(
                        patternName = counterState.patternName,
                        canCombineInstructions = counterState.isPro,
                        isCombineEnabled = renderState.renderedBitmap != null,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        canDetachPattern = true,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = counterViewModel::updatePatternPage,
                        onCombineInstructions = {
                            renderState.renderedBitmap?.let { bitmap ->
                                viewModel.onCombineInstructionsTapped(
                                    currentPage = currentPage,
                                    pageBitmap = bitmap,
                                )
                            }
                        },
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
                        instructionState = instructionState,
                        explanationState = explanationState,
                        snackbarHostState = snackbarHostState,
                    ),
                actions =
                    BottomBarActions(
                        onInstructionTap = viewModel::onInstructionTapped,
                        onExplanationTap = { showExplanationSheet = true },
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
            positionPercent = instructionState.positionPercent,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        )
    }

    if (showExplanationSheet) {
        ExplanationSheet(
            instruction = instructionState.instruction.orEmpty(),
            explanation = explanationState.explanation.orEmpty(),
            onDismiss = { showExplanationSheet = false },
        )
    }

    if (combineState.isVisible) {
        CombineInstructionsSheet(
            state = combineState,
            currentRow = counterState.counter.count,
            snackbarHostState = snackbarHostState,
            onDismiss = viewModel::onCombineSheetDismissed,
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
    var currentPage by rememberSaveable(patternUri) { mutableStateOf(0) }
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
                        canCombineInstructions = false,
                        isCombineEnabled = false,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        canDetachPattern = false,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = { currentPage = it },
                        onCombineInstructions = {},
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
    var renderer by remember(patternUri) { mutableStateOf<PdfPageRenderer?>(null) }
    var rendererError by remember(patternUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(patternUri) {
        renderer?.close()
        renderer = null
        rendererError = null
        if (patternUri == null) return@LaunchedEffect
        val createdRenderer =
            withContext(Dispatchers.IO) {
                runCatching { PdfPageRenderer(context, Uri.parse(patternUri)) }
            }
        createdRenderer
            .onSuccess { pdfRenderer ->
                renderer = pdfRenderer
                val maxPage = (pdfRenderer.pageCount - 1).coerceAtLeast(0)
                if (currentPage > maxPage) {
                    onPageClamped(maxPage)
                }
            }.onFailure { error ->
                rendererError = error.message
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
            withContext(Dispatchers.IO) {
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
    val canCombineInstructions: Boolean,
    val isCombineEnabled: Boolean,
    val totalPages: Int,
    val currentPage: Int,
    val canDetachPattern: Boolean,
)

private data class TopBarActions(
    val onBack: () -> Unit,
    val onJumpToPage: (Int) -> Unit,
    val onCombineInstructions: () -> Unit,
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
                    if (state.canCombineInstructions) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pattern_combine_instructions)) },
                            onClick = {
                                showOverflowMenu = false
                                actions.onCombineInstructions()
                            },
                            enabled = state.isCombineEnabled,
                        )
                    }
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
    positionPercent: Int?,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState =
        rememberTransformableState { zoomChange, panChange, _ ->
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
    val instructionState: InstructionDisplayState,
    val explanationState: ExplanationState,
    val snackbarHostState: SnackbarHostState,
)

private data class BottomBarActions(
    val onInstructionTap: (String) -> Unit,
    val onExplanationTap: () -> Unit,
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
    val copiedMessage = stringResource(R.string.pattern_instruction_copied)
    val currentInstruction = state.instructionState.instruction.orEmpty()
    val isExplanationForCurrentInstruction =
        state.explanationState.isVisible &&
            currentInstruction.isNotBlank() &&
            state.explanationState.forInstruction == currentInstruction

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

            BottomBarInstructionSection(
                instructionState = state.instructionState,
                isExplanationForCurrentInstruction = isExplanationForCurrentInstruction,
                snackbarHostState = state.snackbarHostState,
                copiedMessage = copiedMessage,
                onInstructionTap = actions.onInstructionTap,
            )

            BottomBarExplanationSection(
                instructionState = state.instructionState,
                explanationState = state.explanationState,
                isExplanationForCurrentInstruction = isExplanationForCurrentInstruction,
                snackbarHostState = state.snackbarHostState,
                copiedMessage = copiedMessage,
                onExplanationTap = actions.onExplanationTap,
            )
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
private fun BottomBarInstructionSection(
    instructionState: InstructionDisplayState,
    isExplanationForCurrentInstruction: Boolean,
    snackbarHostState: SnackbarHostState,
    copiedMessage: String,
    onInstructionTap: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (instructionState.canDisplayInstruction && instructionState.isLoading) {
        Spacer(modifier = Modifier.height(4.dp))
        PatternInstructionPlaceholder(lineCount = 1)
    }

    AnimatedVisibility(
        visible = instructionState.canDisplayInstruction && !instructionState.instruction.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(),
    ) {
        Text(
            text = instructionState.instruction.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isExplanationForCurrentInstruction) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .padding(top = 4.dp)
                    .combinedClickable(
                        onClick = {
                            val instruction = instructionState.instruction ?: return@combinedClickable
                            onInstructionTap(instruction)
                        },
                        onLongClick = {
                            val instruction = instructionState.instruction ?: return@combinedClickable
                            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                                ClipData.newPlainText("pattern_instruction", instruction),
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = copiedMessage,
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                    ),
        )
    }
}

@Composable
private fun BottomBarExplanationSection(
    instructionState: InstructionDisplayState,
    explanationState: ExplanationState,
    isExplanationForCurrentInstruction: Boolean,
    snackbarHostState: SnackbarHostState,
    copiedMessage: String,
    onExplanationTap: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (instructionState.canDisplayInstruction &&
        isExplanationForCurrentInstruction &&
        explanationState.isLoading
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        PatternInstructionPlaceholder(lineCount = 2)
    }

    AnimatedVisibility(
        visible =
            instructionState.canDisplayInstruction &&
                isExplanationForCurrentInstruction &&
                !explanationState.explanation.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(),
    ) {
        Surface(
            modifier = Modifier.padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ) {
            Text(
                text = explanationState.explanation.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .combinedClickable(
                            onClick = onExplanationTap,
                            onLongClick = {
                                val explanation = explanationState.explanation ?: return@combinedClickable
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                                    ClipData.newPlainText("pattern_explanation", explanation),
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = copiedMessage,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                        ),
            )
        }
    }
}

@Composable
private fun PatternInstructionPlaceholder(lineCount: Int) {
    val transition = rememberInfiniteTransition(label = "patternInstructionPlaceholder")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_500),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "patternInstructionPlaceholderAlpha",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(lineCount.coerceAtLeast(1)) { index ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(if (index == lineCount - 1 && lineCount > 1) 0.8f else 0.6f)
                        .height(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                            shape = RoundedCornerShape(8.dp),
                        ),
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplanationSheet(
    instruction: String,
    explanation: String,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            if (instruction.isNotBlank()) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombineInstructionsSheet(
    state: CombineState,
    currentRow: Int,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copyMessage = stringResource(R.string.pattern_combined_instructions_copied)
    val sheetTitle =
        when {
            state.result?.found == true -> state.result.title ?: stringResource(R.string.pattern_combine_instructions)
            else -> stringResource(R.string.pattern_combine_instructions)
        }
    val combinedText = remember(state.result, sheetTitle) { state.result?.toClipboardText(sheetTitle) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            val textToCopy = combinedText ?: return@combinedClickable
                            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                                ClipData.newPlainText("combined_instructions", textToCopy),
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = copyMessage,
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                    ).padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = sheetTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    PatternInstructionPlaceholder(lineCount = 4)
                }

                state.messageResId != null -> {
                    Text(
                        text = stringResource(state.messageResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.result?.found == true -> {
                    CombinedInstructionList(
                        result = state.result,
                        currentRow = currentRow,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.pattern_combine_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CombinedInstructionList(
    result: CombinedInstructionResult,
    currentRow: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        result.rows.forEach { row ->
            val actualRow = result.startRow?.let { it + row.row - 1 }
            val isCurrentRow = actualRow != null && actualRow == currentRow
            Surface(
                shape = RoundedCornerShape(16.dp),
                color =
                    if (isCurrentRow) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        Color.Transparent
                    },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    val rowLabel =
                        buildString {
                            append("ROW ${row.row}")
                            row.side?.let { append(" ($it)") }
                        }
                    Text(
                        text = rowLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = row.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun CombinedInstructionResult.toClipboardText(heading: String): String? {
    if (!found || rows.isEmpty()) return null
    return buildString {
        appendLine(heading)
        appendLine()
        rows.forEach { row ->
            append("Row ${row.row}")
            row.side?.let { append(" ($it)") }
            append(": ")
            appendLine(row.instruction)
        }
    }.trim()
}
