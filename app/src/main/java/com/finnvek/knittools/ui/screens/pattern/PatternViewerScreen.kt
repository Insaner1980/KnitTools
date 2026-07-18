package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.createCalibrationRowMarkers
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.resolveReadingLineYFraction
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import kotlinx.coroutines.withContext

private const val READING_LINE_BAND_HEIGHT_FRACTION = 0.045f
private const val READING_LINE_BAND_ALPHA = 0.14f

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
    annotationViewModel: PatternAnnotationViewModel,
) {
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    val patternUri = counterState.patternUri
    val currentPage = counterState.currentPatternPage
    val rowMarkers = remember(counterState.patternRowMapping) { parseMapping(counterState.patternRowMapping) }
    val hasCurrentRowMarker =
        rowMarkers.any { marker -> marker.row == counterState.counter.count && marker.page == currentPage }
    val hasPageRowMarkers = rowMarkers.any { marker -> marker.page == currentPage }
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = counterViewModel::updatePatternPage,
        )
    var rowCalibrationState by remember(patternUri) { mutableStateOf<RowCalibrationState?>(null) }
    var readingLinePreviewYFraction by remember(patternUri) { mutableFloatStateOf(counterState.readingLineYFraction) }
    var isReadingLineDragging by remember(patternUri) { mutableStateOf(false) }

    LaunchedEffect(currentPage) {
        annotationViewModel.setCurrentPage(currentPage)
    }

    LaunchedEffect(patternUri, counterState.readingLineYFraction, isReadingLineDragging) {
        if (!isReadingLineDragging) {
            readingLinePreviewYFraction = counterState.readingLineYFraction
        }
    }

    TrackReadingLineForCurrentRow(
        currentRow = counterState.counter.count,
        currentPage = currentPage,
        patternRowMapping = counterState.patternRowMapping,
        readingLineEnabled = counterState.readingLineEnabled,
        readingLineYFraction = counterState.readingLineYFraction,
        onReadingLineYFractionChange = counterViewModel::updateReadingLineYFraction,
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
                        currentRow = counterState.counter.count.takeIf { patternUri != null },
                        canDetachPattern = true,
                        readingLineEnabled = counterState.readingLineEnabled,
                        hasCurrentRowMarker = hasCurrentRowMarker,
                        hasPageRowMarkers = hasPageRowMarkers,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = counterViewModel::updatePatternPage,
                        onReadingLineToggle = counterViewModel::setReadingLineEnabled,
                        onSaveReadingLineAsCurrentRow = {
                            counterViewModel.upsertPatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                                yPosition = counterState.readingLineYFraction,
                            )
                        },
                        onClearReadingLineRowMarker = {
                            counterViewModel.removePatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                            )
                        },
                        onClearReadingLinePageMarkers = {
                            counterViewModel.removePatternRowMarkersForPage(currentPage)
                        },
                        onStartRowCalibration = {
                            counterViewModel.setReadingLineEnabled(true)
                            rowCalibrationState =
                                RowCalibrationState(
                                    rowInput = counterState.counter.count.toString(),
                                )
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        ) {
            rowCalibrationState?.let { calibrationState ->
                RowCalibrationPanel(
                    state = calibrationState,
                    onRowInputChange = { input ->
                        rowCalibrationState =
                            calibrationState.copy(
                                rowInput = input.filter(Char::isDigit),
                                showInvalidRowError = false,
                            )
                    },
                    onSaveFirst = {
                        val firstRow = calibrationState.rowInput.toIntOrNull()
                        rowCalibrationState =
                            if (firstRow == null) {
                                calibrationState.copy(showInvalidRowError = true)
                            } else {
                                RowCalibrationState(
                                    firstMarker =
                                        RowMarker(
                                            row = firstRow,
                                            page = currentPage,
                                            yPosition = sanitizeReadingLineYFraction(counterState.readingLineYFraction),
                                        ),
                                    rowInput = counterState.counter.count.toString(),
                                )
                            }
                    },
                    onSaveLast = {
                        val markers =
                            calibrationState.toCalibrationMarkers(
                                currentPage = currentPage,
                                currentYFraction = counterState.readingLineYFraction,
                            )
                        if (markers == null) {
                            rowCalibrationState = calibrationState.copy(showInvalidRowError = true)
                        } else {
                            counterViewModel.mergePatternRowMarkers(markers)
                            rowCalibrationState = null
                        }
                    },
                    onCancel = {
                        rowCalibrationState = null
                    },
                )
            }
            PatternViewerContent(
                state =
                    PatternViewerContentState(
                        patternUri = patternUri,
                        rendererError = renderState.rendererError,
                        renderedBitmap = renderState.renderedBitmap,
                        patternName = counterState.patternName,
                        currentRow = counterState.counter.count,
                        positionPercent = null,
                        readingLineEnabled = counterState.readingLineEnabled,
                        readingLineYFraction = readingLinePreviewYFraction,
                    ),
                actions =
                    PatternViewerContentActions(
                        onReadingLineDragStart = {
                            isReadingLineDragging = true
                        },
                        onReadingLineYFractionChange = { yFraction ->
                            isReadingLineDragging = true
                            readingLinePreviewYFraction = sanitizeReadingLineYFraction(yFraction)
                        },
                        onReadingLineYFractionCommit = { yFraction ->
                            val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
                            isReadingLineDragging = false
                            readingLinePreviewYFraction = sanitizedYFraction
                            counterViewModel.updateReadingLineYFraction(sanitizedYFraction)
                            counterViewModel.upsertPatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                                yPosition = sanitizedYFraction,
                            )
                        },
                        onReadingLineDragCancel = {
                            isReadingLineDragging = false
                            readingLinePreviewYFraction = counterState.readingLineYFraction
                        },
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        }
    }
}

private data class RowCalibrationState(
    val firstMarker: RowMarker? = null,
    val rowInput: String,
    val showInvalidRowError: Boolean = false,
)

private val RowCalibrationState.isSecondStep: Boolean
    get() = firstMarker != null

private fun RowCalibrationState.rowLabelRes(): Int =
    if (isSecondStep) {
        R.string.pattern_calibration_last_row
    } else {
        R.string.pattern_calibration_first_row
    }

private fun RowCalibrationState.saveButtonLabelRes(): Int =
    if (isSecondStep) {
        R.string.pattern_calibration_save_last
    } else {
        R.string.pattern_calibration_save_first
    }

private fun RowCalibrationState.saveAction(
    onSaveFirst: () -> Unit,
    onSaveLast: () -> Unit,
): () -> Unit = if (isSecondStep) onSaveLast else onSaveFirst

private fun RowCalibrationState.toCalibrationMarkers(
    currentPage: Int,
    currentYFraction: Float,
): List<RowMarker>? {
    val firstMarker = firstMarker ?: return null
    val lastRow = rowInput.toIntOrNull() ?: return null
    val markers =
        createCalibrationRowMarkers(
            firstRow = firstMarker.row,
            firstPage = firstMarker.page,
            firstYPosition = firstMarker.yPosition,
            lastRow = lastRow,
            lastPage = currentPage,
            lastYPosition = currentYFraction,
        )
    if (markers == null) return null
    return markers
}

@Composable
private fun RowCalibrationPanel(
    state: RowCalibrationState,
    onRowInputChange: (String) -> Unit,
    onSaveFirst: () -> Unit,
    onSaveLast: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.pattern_calibrate_rows),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextField(
                value = state.rowInput,
                onValueChange = onRowInputChange,
                singleLine = true,
                isError = state.showInvalidRowError,
                label = {
                    Text(stringResource(state.rowLabelRes()))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = rowCalibrationSupportingText(state.showInvalidRowError),
                shape = MaterialTheme.shapes.medium,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = state.saveAction(onSaveFirst, onSaveLast),
                    enabled = state.rowInput.toIntOrNull() != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(state.saveButtonLabelRes()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun rowCalibrationSupportingText(showInvalidRowError: Boolean): (@Composable () -> Unit)? {
    if (!showInvalidRowError) return null
    return {
        Text(stringResource(R.string.pattern_calibration_invalid_row))
    }
}

@Composable
private fun TrackReadingLineForCurrentRow(
    currentRow: Int,
    currentPage: Int,
    patternRowMapping: String?,
    readingLineEnabled: Boolean,
    readingLineYFraction: Float,
    onReadingLineYFractionChange: (Float) -> Unit,
) {
    var previousRow by remember(currentPage, patternRowMapping) { mutableIntStateOf(currentRow) }
    val rowMarkers = remember(patternRowMapping) { parseMapping(patternRowMapping) }
    LaunchedEffect(currentRow, currentPage, patternRowMapping, readingLineEnabled) {
        if (!readingLineEnabled) {
            previousRow = currentRow
            return@LaunchedEffect
        }

        val rowDelta = currentRow - previousRow
        val nextYFraction =
            resolveReadingLineYFraction(
                markers = rowMarkers,
                currentRow = currentRow,
                currentPage = currentPage,
                currentYFraction = readingLineYFraction,
                rowDelta = rowDelta,
            )

        previousRow = currentRow
        nextYFraction
            ?.takeIf { it != readingLineYFraction }
            ?.let(onReadingLineYFractionChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPatternViewerScreen(
    patternUri: String?,
    patternName: String?,
    onBack: () -> Unit,
    annotationViewModel: PatternAnnotationViewModel,
) {
    var currentPage by rememberSaveable(patternUri) { mutableIntStateOf(0) }
    var readingLineEnabled by rememberSaveable(patternUri) { mutableStateOf(false) }
    var readingLineYFraction by rememberSaveable(patternUri) { mutableFloatStateOf(DEFAULT_READING_LINE_Y_FRACTION) }
    LaunchedEffect(currentPage) {
        annotationViewModel.setCurrentPage(currentPage)
    }
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
                        currentRow = null,
                        canDetachPattern = false,
                        readingLineEnabled = readingLineEnabled,
                        hasCurrentRowMarker = false,
                        hasPageRowMarkers = false,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = { currentPage = it },
                        onReadingLineToggle = { readingLineEnabled = it },
                        onSaveReadingLineAsCurrentRow = {},
                        onClearReadingLineRowMarker = {},
                        onClearReadingLinePageMarkers = {},
                        onStartRowCalibration = {},
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
            state =
                PatternViewerContentState(
                    patternUri = patternUri,
                    rendererError = renderState.rendererError,
                    renderedBitmap = renderState.renderedBitmap,
                    patternName = patternName,
                    currentRow = null,
                    positionPercent = null,
                    readingLineEnabled = readingLineEnabled,
                    readingLineYFraction = readingLineYFraction,
                ),
            actions =
                PatternViewerContentActions(
                    onReadingLineDragStart = {},
                    onReadingLineYFractionChange = { readingLineYFraction = sanitizeReadingLineYFraction(it) },
                    onReadingLineYFractionCommit = { readingLineYFraction = sanitizeReadingLineYFraction(it) },
                    onReadingLineDragCancel = {},
                ),
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
                val clampedPage = clampPatternPage(currentPage, pdfRenderer.pageCount)
                if (currentPage != clampedPage) {
                    onPageClamped(clampedPage)
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
    val currentRow: Int?,
    val canDetachPattern: Boolean,
    val readingLineEnabled: Boolean,
    val hasCurrentRowMarker: Boolean,
    val hasPageRowMarkers: Boolean,
)

private data class TopBarActions(
    val onBack: () -> Unit,
    val onJumpToPage: (Int) -> Unit,
    val onReadingLineToggle: (Boolean) -> Unit,
    val onSaveReadingLineAsCurrentRow: () -> Unit,
    val onClearReadingLineRowMarker: () -> Unit,
    val onClearReadingLinePageMarkers: () -> Unit,
    val onStartRowCalibration: () -> Unit,
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
                PatternViewerOverflowMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    state = state,
                    actions = actions,
                    onPageJumpClick = { showPageJumpDialog = true },
                    closeOverflowMenu = { showOverflowMenu = false },
                )
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
private fun PatternViewerOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: TopBarState,
    actions: TopBarActions,
    onPageJumpClick: () -> Unit,
    closeOverflowMenu: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pattern_page_jump)) },
            onClick = {
                closeOverflowMenu()
                onPageJumpClick()
            },
        )
        PatternReadingLineMenuItem(
            readingLineEnabled = state.readingLineEnabled,
            onClick = {
                closeOverflowMenu()
                actions.onReadingLineToggle(!state.readingLineEnabled)
            },
        )
        PatternRowMarkerMenuItems(
            state = state,
            actions = actions,
            closeOverflowMenu = closeOverflowMenu,
        )
        if (state.canDetachPattern) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_pattern)) },
                onClick = {
                    closeOverflowMenu()
                    actions.onDetachPattern()
                },
            )
        }
    }
}

@Composable
private fun PatternReadingLineMenuItem(
    readingLineEnabled: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (readingLineEnabled) {
                        R.string.pattern_hide_reading_line
                    } else {
                        R.string.pattern_show_reading_line
                    },
                ),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun PatternRowMarkerMenuItems(
    state: TopBarState,
    actions: TopBarActions,
    closeOverflowMenu: () -> Unit,
) {
    val currentRow = state.currentRow ?: return
    DropdownMenuItem(
        text = { Text(stringResource(R.string.pattern_save_line_as_row, currentRow)) },
        onClick = {
            closeOverflowMenu()
            actions.onSaveReadingLineAsCurrentRow()
        },
    )
    if (state.hasCurrentRowMarker) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pattern_clear_row_mark)) },
            onClick = {
                closeOverflowMenu()
                actions.onClearReadingLineRowMarker()
            },
        )
    }
    if (state.hasPageRowMarkers) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pattern_clear_page_marks)) },
            onClick = {
                closeOverflowMenu()
                actions.onClearReadingLinePageMarkers()
            },
        )
    }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.pattern_calibrate_rows)) },
        onClick = {
            closeOverflowMenu()
            actions.onStartRowCalibration()
        },
    )
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
    state: PatternViewerContentState,
    actions: PatternViewerContentActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when {
            state.patternUri == null -> {
                PatternViewerMessage(message = stringResource(R.string.no_pattern_attached))
            }

            state.rendererError != null -> {
                PatternViewerMessage(
                    message = state.rendererError.ifBlank { stringResource(R.string.pattern_open_failed) },
                )
            }

            state.renderedBitmap == null -> {
                PatternViewerMessage(message = stringResource(R.string.pattern_loading))
            }

            else -> {
                PatternDocumentViewport(
                    renderedBitmap = state.renderedBitmap,
                    contentDescription = state.patternName,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) { viewport ->
                    RowHighlightOverlay(
                        yPosition = state.positionPercent?.let { it / 100f },
                        modifier = Modifier.fillMaxSize(),
                        accessibilityDescription =
                            if (state.currentRow != null && state.positionPercent != null) {
                                stringResource(
                                    R.string.pattern_row_highlight_description,
                                    state.currentRow,
                                    state.positionPercent,
                                )
                            } else {
                                null
                            },
                    )
                    if (state.readingLineEnabled) {
                        ReadingLineOverlay(
                            yFraction = state.readingLineYFraction,
                            currentRow = state.currentRow,
                            scale = viewport.state.scale,
                            actions =
                                ReadingLineOverlayActions(
                                    onDragStart = actions.onReadingLineDragStart,
                                    onYFractionChange = actions.onReadingLineYFractionChange,
                                    onYFractionCommit = actions.onReadingLineYFractionCommit,
                                    onDragCancel = actions.onReadingLineDragCancel,
                                ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

private data class PatternViewerContentState(
    val patternUri: String?,
    val rendererError: String?,
    val renderedBitmap: Bitmap?,
    val patternName: String?,
    val currentRow: Int?,
    val positionPercent: Int?,
    val readingLineEnabled: Boolean,
    val readingLineYFraction: Float,
)

private data class PatternViewerContentActions(
    val onReadingLineDragStart: () -> Unit,
    val onReadingLineYFractionChange: (Float) -> Unit,
    val onReadingLineYFractionCommit: (Float) -> Unit,
    val onReadingLineDragCancel: () -> Unit,
)

private data class ReadingLineOverlayActions(
    val onDragStart: () -> Unit,
    val onYFractionChange: (Float) -> Unit,
    val onYFractionCommit: (Float) -> Unit,
    val onDragCancel: () -> Unit,
)

@Composable
private fun ReadingLineOverlay(
    yFraction: Float,
    currentRow: Int?,
    scale: Float,
    actions: ReadingLineOverlayActions,
    modifier: Modifier = Modifier,
) {
    val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
    val lineColor = MaterialTheme.colorScheme.primary
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = READING_LINE_BAND_ALPHA)
    val description = stringResource(R.string.pattern_reading_line_description)
    BoxWithConstraints(
        modifier =
            modifier
                .semantics { contentDescription = description }
                .pointerInput(sanitizedYFraction, scale) {
                    var lastYFraction = sanitizedYFraction
                    detectVerticalDragGestures(
                        onDragStart = {
                            actions.onDragStart()
                            lastYFraction = sanitizedYFraction
                        },
                        onDragEnd = { actions.onYFractionCommit(lastYFraction) },
                        onDragCancel = { actions.onDragCancel() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val heightPx =
                                size.height.toFloat().takeIf { it > 0f }
                                    ?: return@detectVerticalDragGestures
                            val adjustedDrag = dragAmount / scale.coerceAtLeast(1f)
                            lastYFraction =
                                (lastYFraction + (adjustedDrag / heightPx)).coerceIn(
                                    READING_LINE_MIN_Y_FRACTION,
                                    READING_LINE_MAX_Y_FRACTION,
                                )
                            actions.onYFractionChange(lastYFraction)
                        },
                    )
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height * sanitizedYFraction
            val bandHeight = (size.height * READING_LINE_BAND_HEIGHT_FRACTION).coerceAtLeast(24.dp.toPx())
            drawRect(
                color = bandColor,
                topLeft = Offset(0f, centerY - (bandHeight / 2f)),
                size = Size(size.width, bandHeight),
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 2.dp.toPx(),
            )
        }
        currentRow?.let { currentRow ->
            ReadingLineRowLabel(
                currentRow = currentRow,
                yFraction = sanitizedYFraction,
                containerHeight = maxHeight,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun ReadingLineRowLabel(
    currentRow: Int,
    yFraction: Float,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val verticalMargin = 4.dp
    val labelHeight = 28.dp
    val maxOffset = containerHeight - labelHeight - verticalMargin
    val boundedMaxOffset = if (maxOffset > verticalMargin) maxOffset else verticalMargin
    val labelOffset =
        (containerHeight * yFraction - (labelHeight / 2f))
            .coerceIn(verticalMargin, boundedMaxOffset)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        modifier =
            modifier
                .padding(start = 8.dp)
                .offset(y = labelOffset),
    ) {
        Text(
            text = stringResource(R.string.current_row_short, currentRow),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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
