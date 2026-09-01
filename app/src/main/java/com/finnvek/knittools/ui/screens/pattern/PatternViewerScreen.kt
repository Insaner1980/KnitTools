package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.createCalibrationRowMarkers
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.model.DEFAULT_READING_GUIDE_FRACTION
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_ROW_STEP_FRACTION
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.CounterViewerEvent
import com.finnvek.knittools.ui.theme.rememberPatternAnnotationRenderStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val READING_LINE_BAND_HEIGHT_FRACTION = 0.045f
private const val READING_LINE_BAND_ALPHA = 0.14f
private const val VERTICAL_GUIDE_BAND_WIDTH_FRACTION = 0.035f
private const val VERTICAL_GUIDE_BAND_ALPHA = 0.10f

private data class PatternRenderState(
    val renderer: PdfPageRenderer?,
    val rendererError: String?,
    val renderedBitmap: Bitmap?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternViewerScreen(
    onBack: () -> Unit,
    onImportFromRavelry: () -> Unit,
    onSeePro: () -> Unit,
    onSavedPatternDetail: (Long) -> Unit,
    onEditWebPattern: (Long) -> Unit,
    counterViewModelProvider: @Composable () -> CounterViewModel,
    patternViewerViewModelProvider: @Composable () -> PatternViewerViewModel,
    annotationViewModel: PatternAnnotationViewModel,
) {
    val counterViewModel = counterViewModelProvider()
    val patternViewerViewModel = patternViewerViewModelProvider()
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    val savedPatterns by counterViewModel.savedPatterns.collectAsStateWithLifecycle()
    val bookmarkState by patternViewerViewModel.uiState.collectAsStateWithLifecycle()
    val documentState by patternViewerViewModel.documentUiState.collectAsStateWithLifecycle()
    val annotationState by annotationViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val selectedDocument = documentState.selectedDocument
    val selectedDocumentAvailable = selectedDocument?.let { documentState.isAvailable(it.id) } == true
    val patternUri = selectedDocument?.localPdfUri?.takeIf { selectedDocumentAvailable }
    val currentPage = selectedDocument?.currentPage ?: 0
    val rowMarkers = remember(selectedDocument?.rowMapping) { parseMapping(selectedDocument?.rowMapping) }
    val hasCurrentRowMarker =
        rowMarkers.any { marker -> marker.row == counterState.counter.count && marker.page == currentPage }
    val hasPageRowMarkers = rowMarkers.any { marker -> marker.page == currentPage }
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = counterViewModel::updatePatternPage,
        )
    var rowCalibrationState by remember(selectedDocument?.id) { mutableStateOf<RowCalibrationState?>(null) }
    var readingLinePreviewYFraction by remember(selectedDocument?.id) {
        mutableFloatStateOf(selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION)
    }
    var isReadingLineDragging by remember(selectedDocument?.id) { mutableStateOf(false) }
    var verticalGuidePreviewXFraction by remember(selectedDocument?.id) {
        mutableFloatStateOf(selectedDocument?.verticalReadingGuideXFraction ?: DEFAULT_READING_GUIDE_FRACTION)
    }
    var isVerticalGuideDragging by remember(selectedDocument?.id) { mutableStateOf(false) }
    var showBookmarkSheet by rememberSaveable(selectedDocument?.id) { mutableStateOf(false) }
    var showDocumentSheet by rememberSaveable { mutableStateOf(false) }
    var showDocumentPicker by rememberSaveable { mutableStateOf(false) }
    var viewportFocusRequest by remember(selectedDocument?.id) { mutableStateOf<PatternViewportFocusRequest?>(null) }
    var accessibilityAnnouncement by remember(selectedDocument?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPage) {
        annotationViewModel.setCurrentPage(currentPage)
    }

    LaunchedEffect(selectedDocument?.id, selectedDocument?.readingLineYFraction, isReadingLineDragging) {
        if (!isReadingLineDragging) {
            readingLinePreviewYFraction = selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION
        }
    }
    LaunchedEffect(selectedDocument?.id, selectedDocument?.verticalReadingGuideXFraction, isVerticalGuideDragging) {
        if (!isVerticalGuideDragging) {
            verticalGuidePreviewXFraction =
                selectedDocument?.verticalReadingGuideXFraction ?: DEFAULT_READING_GUIDE_FRACTION
        }
    }

    LaunchedEffect(documentState.isLoading, documentState.documents.isEmpty()) {
        if (!documentState.isLoading && documentState.documents.isEmpty()) onBack()
    }

    CollectWithLifecycleEffect({ counterViewModel.viewerEvents }) { event ->
        when (event) {
            is CounterViewerEvent.AutomaticReadingLinePageChanged -> {
                accessibilityAnnouncement =
                    resources.getString(
                        R.string.pattern_follow_page_announcement,
                        event.row,
                        event.page + 1,
                        renderState.renderer?.pageCount ?: (event.page + 1),
                    )
            }
            is CounterViewerEvent.ReadingLineFollowingResumed -> {
                accessibilityAnnouncement =
                    resources.getString(
                        if (event.calibrated) {
                            R.string.pattern_follow_resumed
                        } else {
                            R.string.pattern_follow_resumed_uncalibrated
                        },
                    )
            }
        }
    }
    CollectWithLifecycleEffect({ patternViewerViewModel.events }) { event ->
        when (event) {
            is PatternViewerEvent.BookmarkJumped -> {
                viewportFocusRequest =
                    PatternViewportFocusRequest(
                        requestId = event.requestId,
                        pageIndex = event.bookmark.pageIndex,
                        yFraction = event.bookmark.yFraction,
                    )
                accessibilityAnnouncement =
                    resources.getString(
                        R.string.pattern_bookmark_jump_announcement,
                        event.bookmark.name,
                        event.bookmark.pageIndex + 1,
                        renderState.renderer?.pageCount ?: (event.bookmark.pageIndex + 1),
                    )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PatternViewerTopBar(
                state =
                    TopBarState(
                        patternName = selectedDocument?.label,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        currentRow = counterState.counter.count.takeIf { patternUri != null },
                        canDetachPattern = false,
                        canManageDocuments = true,
                        readingLineEnabled = selectedDocument?.readingLineEnabled == true,
                        readingLineFollowCurrentRow = selectedDocument?.readingLineFollowCurrentRow == true,
                        verticalReadingGuideEnabled = selectedDocument?.verticalReadingGuideEnabled == true,
                        canManageBookmarks = true,
                        hasCurrentRowMarker = hasCurrentRowMarker,
                        hasPageRowMarkers = hasPageRowMarkers,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = counterViewModel::updatePatternPage,
                        onReadingLineToggle = counterViewModel::setReadingLineEnabled,
                        onReadingLineFollowToggle = counterViewModel::setReadingLineFollowCurrentRow,
                        onReturnToCurrentRow = counterViewModel::returnReadingLineToCurrentRow,
                        onVerticalReadingGuideToggle = counterViewModel::setVerticalReadingGuideEnabled,
                        onCenterVerticalReadingGuide = counterViewModel::centerVerticalReadingGuide,
                        onOpenBookmarks = {
                            patternViewerViewModel.selectNearestBookmark(
                                pageIndex = currentPage,
                                yFraction = selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
                            )
                            showBookmarkSheet = true
                        },
                        onOpenDocuments = { showDocumentSheet = true },
                        onSaveReadingLineAsCurrentRow = {
                            counterViewModel.upsertPatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                                yPosition = selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
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
                        onDetachPattern = {},
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
            accessibilityAnnouncement?.let { announcement ->
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = announcement
                            },
                )
            }
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
                                            yPosition =
                                                sanitizeReadingLineYFraction(
                                                    selectedDocument?.readingLineYFraction
                                                        ?: DEFAULT_READING_LINE_Y_FRACTION,
                                                ),
                                        ),
                                    rowInput = counterState.counter.count.toString(),
                                )
                            }
                    },
                    onSaveLast = {
                        val markers =
                            calibrationState.toCalibrationMarkers(
                                currentPage = currentPage,
                                currentYFraction =
                                    selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
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
                stateProvider = {
                    PatternViewerContentState(
                        patternUri = patternUri,
                        rendererError = renderState.rendererError,
                        renderedBitmap = renderState.renderedBitmap,
                        patternName = selectedDocument?.label,
                        currentRow = counterState.counter.count,
                        positionPercent = null,
                        readingLineEnabled = selectedDocument?.readingLineEnabled == true,
                        readingLineYFraction = readingLinePreviewYFraction,
                        readingLineFollowCurrentRow = selectedDocument?.readingLineFollowCurrentRow,
                        verticalReadingGuideEnabled = selectedDocument?.verticalReadingGuideEnabled == true,
                        verticalReadingGuideXFraction = verticalGuidePreviewXFraction,
                        currentPage = currentPage,
                        viewportFocusRequest = viewportFocusRequest,
                        annotationState = annotationState,
                    )
                },
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
                            counterViewModel.commitManualReadingLinePosition(sanitizedYFraction)
                        },
                        onReadingLineDragCancel = {
                            isReadingLineDragging = false
                            readingLinePreviewYFraction =
                                selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION
                        },
                        onVerticalGuideDragStart = {
                            isVerticalGuideDragging = true
                        },
                        onVerticalGuideXFractionChange = { xFraction ->
                            isVerticalGuideDragging = true
                            verticalGuidePreviewXFraction = sanitizeReadingGuideFraction(xFraction)
                        },
                        onVerticalGuideXFractionCommit = { xFraction ->
                            val sanitizedXFraction = sanitizeReadingGuideFraction(xFraction)
                            isVerticalGuideDragging = false
                            verticalGuidePreviewXFraction = sanitizedXFraction
                            counterViewModel.updateVerticalReadingGuideXFraction(sanitizedXFraction)
                        },
                        onVerticalGuideDragCancel = {
                            isVerticalGuideDragging = false
                            verticalGuidePreviewXFraction =
                                selectedDocument?.verticalReadingGuideXFraction ?: DEFAULT_READING_GUIDE_FRACTION
                        },
                        onViewportFocusRequestConsumed = { requestId ->
                            if (viewportFocusRequest?.requestId == requestId) {
                                viewportFocusRequest = null
                            }
                        },
                        onMasterLayerVisibilityChange = annotationViewModel::setMasterLayerVisible,
                        onProjectLayerVisibilityChange = annotationViewModel::setProjectLayerVisible,
                        annotationInputActions = annotationViewModel.patternInputActions(),
                        annotationToolbarActions = annotationViewModel.patternToolbarActions(),
                        onExport = annotationViewModel::exportAnnotatedPdf,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        }
    }

    if (showBookmarkSheet) {
        PatternBookmarkSheet(
            state = bookmarkState,
            totalPages = renderState.renderer?.pageCount ?: 0,
            actions =
                PatternBookmarkSheetActions(
                    onDismiss = { showBookmarkSheet = false },
                    onAdd = { name ->
                        patternViewerViewModel.addBookmark(
                            name = name,
                            pageIndex = currentPage,
                            yFraction =
                                if (selectedDocument?.readingLineEnabled == true) {
                                    selectedDocument.readingLineYFraction
                                } else {
                                    DEFAULT_READING_LINE_Y_FRACTION
                                },
                        )
                    },
                    onJump = { bookmarkId ->
                        showBookmarkSheet = false
                        patternViewerViewModel.jumpToBookmark(bookmarkId)
                    },
                    onPrevious = {
                        showBookmarkSheet = false
                        patternViewerViewModel.jumpToPreviousBookmark()
                    },
                    onNext = {
                        showBookmarkSheet = false
                        patternViewerViewModel.jumpToNextBookmark()
                    },
                    onRename = patternViewerViewModel::renameBookmark,
                    onDelete = patternViewerViewModel::deleteBookmark,
                    onClearError = patternViewerViewModel::clearError,
                ),
        )
    }

    if (showDocumentSheet) {
        ProjectDocumentsSheet(
            state = documentState,
            metadataPattern =
                counterState.linkedPattern
                    ?.takeIf { pattern ->
                        documentState.documents.none { it.savedPatternId == pattern.id }
                    },
            onOpenPatternInformation = {
                counterState.linkedPattern?.id?.let(onSavedPatternDetail)
            },
            onOpenPatternWebsite = {
                val result =
                    counterState.linkedPattern
                        ?.webPatternUrlOrNull
                        ?.originalUrl
                        ?.let { openExternalWebLink(context, it) }
                        ?: ExternalWebLinkOpenResult.InvalidUrl
                val messageRes =
                    when (result) {
                        ExternalWebLinkOpenResult.Opened -> null
                        ExternalWebLinkOpenResult.NoBrowser -> R.string.web_pattern_no_browser
                        ExternalWebLinkOpenResult.InvalidUrl,
                        ExternalWebLinkOpenResult.Failed,
                        -> R.string.web_pattern_open_failed
                    }
                messageRes?.let { Toast.makeText(context, resources.getString(it), Toast.LENGTH_SHORT).show() }
            },
            onEditPatternInformation = {
                counterState.linkedPattern?.id?.let(onEditWebPattern)
            },
            onUnlinkPatternInformation = {
                counterState.linkedPattern?.id?.let { expectedPatternId ->
                    counterViewModel.unlinkSavedPatternMetadata(expectedPatternId) { result ->
                        if (
                            result != SavedPatternMetadataMutationResult.Unlinked &&
                            result != SavedPatternMetadataMutationResult.AlreadyUnlinked
                        ) {
                            Toast
                                .makeText(
                                    context,
                                    resources.getString(R.string.web_pattern_save_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                }
            },
            onDismiss = { showDocumentSheet = false },
            onSelect = patternViewerViewModel::selectDocument,
            onRename = patternViewerViewModel::renameDocument,
            onMoveEarlier = patternViewerViewModel::moveDocumentEarlier,
            onMoveLater = patternViewerViewModel::moveDocumentLater,
            onSetPrimary = patternViewerViewModel::setPrimaryDocument,
            onRemove = patternViewerViewModel::removeDocument,
            onAdd = {
                showDocumentSheet = false
                showDocumentPicker = true
            },
            onClearError = patternViewerViewModel::clearDocumentError,
        )
    }

    if (showDocumentPicker) {
        PatternPickerSheet(
            projectId = counterState.projectId,
            savedPatterns = savedPatterns.filter { !it.localPdfUri.isNullOrBlank() },
            canUseCameraScan = counterState.canUsePatternCameraScan,
            proStatus = counterState.proStatus,
            hasExistingPattern = false,
            mode = PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT,
            excludedSavedPatternIds = documentState.documents.mapNotNullTo(mutableSetOf()) { it.savedPatternId },
            onSavedPatternSelected = { patternViewerViewModel.addSavedPattern(it.id) },
            onDocumentSelected = { uri, name ->
                counterViewModel.attachPattern(uri, name) { result ->
                    patternViewerViewModel.handleDocumentAddResult(result)
                    if (result !is ProjectDocumentMutationResult.Added) {
                        showDocumentPicker = false
                        showDocumentSheet = true
                    }
                }
            },
            onImportFromRavelry = onImportFromRavelry,
            onSeePro = onSeePro,
            onDismiss = { showDocumentPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectDocumentsSheet(
    state: ProjectDocumentUiState,
    metadataPattern: SavedPattern? = null,
    metadataPatternName: String? = null,
    onOpenPatternInformation: () -> Unit = {},
    onOpenPatternWebsite: () -> Unit = {},
    onEditPatternInformation: () -> Unit = {},
    onUnlinkPatternInformation: () -> Unit = {},
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onMoveEarlier: (Long) -> Unit,
    onMoveLater: (Long) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: () -> Unit,
    onClearError: () -> Unit,
) {
    var renameDocument by remember { mutableStateOf<ProjectDocument?>(null) }
    var renameText by remember { mutableStateOf("") }
    var removeDocument by remember { mutableStateOf<ProjectDocument?>(null) }
    var showUnlinkConfirmation by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.project_documents_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onAdd()
                        }
                    },
                    enabled = !state.isMutating,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.project_documents_add))
                }
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onClearError),
                )
            }
            val patternName = metadataPattern?.name ?: metadataPatternName
            patternName?.let {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.project_documents_pattern_information),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                    val webPattern = metadataPattern?.takeIf(SavedPattern::isWebPatternCompatible)
                    if (webPattern != null) {
                        val webUrl = webPattern.webPatternUrlOrNull
                        webPattern.designerName.takeIf(String::isNotBlank)?.let { designer ->
                            Text(
                                text = designer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.web_pattern_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        webUrl?.host?.let { host ->
                            Text(
                                text = host,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val openDescription =
                            stringResource(
                                R.string.web_pattern_open_website_description,
                                webPattern.name,
                                webUrl?.host.orEmpty(),
                            )
                        val editDescription = stringResource(R.string.web_pattern_edit_description, webPattern.name)
                        val unlinkDescription = stringResource(R.string.web_pattern_unlink_description, webPattern.name)
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onOpenPatternWebsite()
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = openDescription },
                        ) {
                            Text(stringResource(R.string.web_pattern_open_website))
                        }
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onEditPatternInformation()
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = editDescription },
                        ) {
                            Text(stringResource(R.string.web_pattern_edit))
                        }
                        TextButton(
                            onClick = { showUnlinkConfirmation = true },
                            modifier = Modifier.semantics { contentDescription = unlinkDescription },
                        ) {
                            Text(stringResource(R.string.web_pattern_unlink))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.project_documents_metadata_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onOpenPatternInformation()
                                }
                            },
                        ) {
                            Text(stringResource(R.string.project_documents_pattern_information))
                        }
                    }
                }
            }
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                state.documents.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.project_documents_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.documents, key = { _, document -> document.id }) { index, document ->
                            ProjectDocumentRow(
                                document = document,
                                index = index,
                                documentCount = state.documents.size,
                                isSelected = state.selectedDocumentId == document.id,
                                isAvailable = state.isAvailable(document.id),
                                enabled = !state.isMutating,
                                onSelect = { onSelect(document.id) },
                                onRename = {
                                    renameDocument = document
                                    renameText = document.label
                                },
                                onMoveEarlier = { onMoveEarlier(document.id) },
                                onMoveLater = { onMoveLater(document.id) },
                                onSetPrimary = { onSetPrimary(document.id) },
                                onRemove = { removeDocument = document },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showUnlinkConfirmation) {
        val patternName = metadataPattern?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { showUnlinkConfirmation = false },
            title = { Text(stringResource(R.string.web_pattern_unlink)) },
            text = { Text(stringResource(R.string.web_pattern_unlink_description, patternName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkConfirmation = false
                        coroutineScope.launch {
                            sheetState.hide()
                            onUnlinkPatternInformation()
                        }
                    },
                ) {
                    Text(stringResource(R.string.web_pattern_unlink))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    renameDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { renameDocument = null },
            title = { Text(stringResource(R.string.project_documents_rename_title)) },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(50) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.project_documents_label)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(document.id, renameText)
                        renameDocument = null
                    },
                    enabled = renameText.trim().isNotEmpty(),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDocument = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    removeDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { removeDocument = null },
            title = { Text(stringResource(R.string.project_documents_remove_title)) },
            text = {
                Text(
                    stringResource(
                        when {
                            state.documents.size == 1 -> R.string.project_documents_remove_last_message
                            document.isPrimary -> R.string.project_documents_remove_primary_message
                            else -> R.string.project_documents_remove_message
                        },
                        document.label,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(document.id)
                        removeDocument = null
                    },
                ) {
                    Text(stringResource(R.string.project_documents_remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeDocument = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ProjectDocumentRow(
    document: ProjectDocument,
    index: Int,
    documentCount: Int,
    isSelected: Boolean,
    isAvailable: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
) {
    var showActions by rememberSaveable(document.id) { mutableStateOf(false) }
    val openLabel = stringResource(R.string.project_documents_open_action)
    val renameLabel = stringResource(R.string.project_documents_rename)
    val moveEarlierLabel = stringResource(R.string.project_documents_move_earlier)
    val moveLaterLabel = stringResource(R.string.project_documents_move_later)
    val makePrimaryLabel = stringResource(R.string.project_documents_make_primary)
    val removeLabel = stringResource(R.string.project_documents_remove_from_project)
    val actionsDescription = stringResource(R.string.project_documents_actions, document.label)
    val accessibilityActions =
        buildList {
            if (isAvailable) {
                add(
                    CustomAccessibilityAction(openLabel) {
                        onSelect()
                        true
                    },
                )
            }
            add(
                CustomAccessibilityAction(renameLabel) {
                    onRename()
                    true
                },
            )
            if (index > 0) {
                add(
                    CustomAccessibilityAction(moveEarlierLabel) {
                        onMoveEarlier()
                        true
                    },
                )
            }
            if (index < documentCount - 1) {
                add(
                    CustomAccessibilityAction(moveLaterLabel) {
                        onMoveLater()
                        true
                    },
                )
            }
            if (!document.isPrimary) {
                add(
                    CustomAccessibilityAction(makePrimaryLabel) {
                        onSetPrimary()
                        true
                    },
                )
            }
            add(
                CustomAccessibilityAction(removeLabel) {
                    onRemove()
                    true
                },
            )
        }
    ListItem(
        headlineContent = {
            Text(document.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Text(
                    stringResource(
                        if (document.isPrimary) {
                            R.string.project_documents_primary
                        } else {
                            R.string.project_documents_secondary
                        },
                    ),
                )
                Text(stringResource(R.string.project_documents_position, index + 1, documentCount))
                if (isSelected) Text(stringResource(R.string.project_documents_open))
                if (isAvailable) {
                    Text(stringResource(R.string.project_documents_available))
                } else {
                    Text(
                        text = stringResource(R.string.project_documents_unavailable),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(
                    onClick = { showActions = true },
                    enabled = enabled,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .semantics { contentDescription = actionsDescription },
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = showActions,
                    onDismissRequest = { showActions = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_documents_move_earlier)) },
                        enabled = index > 0,
                        onClick = {
                            showActions = false
                            onMoveEarlier()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_documents_move_later)) },
                        enabled = index < documentCount - 1,
                        onClick = {
                            showActions = false
                            onMoveLater()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_documents_rename)) },
                        onClick = {
                            showActions = false
                            onRename()
                        },
                    )
                    if (!document.isPrimary) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.project_documents_make_primary)) },
                            onClick = {
                                showActions = false
                                onSetPrimary()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_documents_remove_action)) },
                        onClick = {
                            showActions = false
                            onRemove()
                        },
                    )
                }
            }
        },
        modifier =
            Modifier
                .clickable(
                    enabled = enabled && isAvailable,
                    onClickLabel = openLabel,
                    onClick = onSelect,
                ).semantics { customActions = accessibilityActions },
    )
}

private fun ProjectDocumentError.messageRes(): Int =
    when (this) {
        ProjectDocumentError.INVALID_LABEL -> R.string.project_documents_error_invalid_label
        ProjectDocumentError.DUPLICATE -> R.string.project_documents_error_duplicate
        ProjectDocumentError.UNAVAILABLE -> R.string.project_documents_error_unavailable
        ProjectDocumentError.STALE_ACTION -> R.string.project_documents_error_stale
        ProjectDocumentError.MUTATION_FAILURE -> R.string.project_documents_error_generic
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPatternViewerScreen(
    patternUri: String?,
    patternName: String?,
    onBack: () -> Unit,
    annotationViewModel: PatternAnnotationViewModel,
) {
    val annotationState by annotationViewModel.uiState.collectAsStateWithLifecycle()
    var currentPage by rememberSaveable(patternUri) { mutableIntStateOf(0) }
    var readingLineEnabled by rememberSaveable(patternUri) { mutableStateOf(false) }
    var readingLineYFraction by rememberSaveable(patternUri) { mutableFloatStateOf(DEFAULT_READING_LINE_Y_FRACTION) }
    var verticalReadingGuideEnabled by rememberSaveable(patternUri) { mutableStateOf(false) }
    var verticalReadingGuideXFraction by rememberSaveable(patternUri) {
        mutableFloatStateOf(DEFAULT_READING_GUIDE_FRACTION)
    }
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
                        canManageDocuments = false,
                        readingLineEnabled = readingLineEnabled,
                        readingLineFollowCurrentRow = false,
                        verticalReadingGuideEnabled = verticalReadingGuideEnabled,
                        canManageBookmarks = false,
                        hasCurrentRowMarker = false,
                        hasPageRowMarkers = false,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = { currentPage = it },
                        onReadingLineToggle = { readingLineEnabled = it },
                        onReadingLineFollowToggle = {},
                        onReturnToCurrentRow = {},
                        onVerticalReadingGuideToggle = { verticalReadingGuideEnabled = it },
                        onCenterVerticalReadingGuide = {
                            verticalReadingGuideXFraction = DEFAULT_READING_GUIDE_FRACTION
                        },
                        onOpenBookmarks = {},
                        onOpenDocuments = {},
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
            stateProvider = {
                PatternViewerContentState(
                    patternUri = patternUri,
                    rendererError = renderState.rendererError,
                    renderedBitmap = renderState.renderedBitmap,
                    patternName = patternName,
                    currentRow = null,
                    positionPercent = null,
                    readingLineEnabled = readingLineEnabled,
                    readingLineYFraction = readingLineYFraction,
                    readingLineFollowCurrentRow = null,
                    verticalReadingGuideEnabled = verticalReadingGuideEnabled,
                    verticalReadingGuideXFraction = verticalReadingGuideXFraction,
                    currentPage = currentPage,
                    viewportFocusRequest = null,
                    annotationState = annotationState,
                )
            },
            actions =
                PatternViewerContentActions(
                    onReadingLineDragStart = {},
                    onReadingLineYFractionChange = { readingLineYFraction = sanitizeReadingLineYFraction(it) },
                    onReadingLineYFractionCommit = { readingLineYFraction = sanitizeReadingLineYFraction(it) },
                    onReadingLineDragCancel = {},
                    onVerticalGuideDragStart = {},
                    onVerticalGuideXFractionChange = {
                        verticalReadingGuideXFraction = sanitizeReadingGuideFraction(it)
                    },
                    onVerticalGuideXFractionCommit = {
                        verticalReadingGuideXFraction = sanitizeReadingGuideFraction(it)
                    },
                    onVerticalGuideDragCancel = {},
                    onViewportFocusRequestConsumed = {},
                    onMasterLayerVisibilityChange = annotationViewModel::setMasterLayerVisible,
                    onProjectLayerVisibilityChange = annotationViewModel::setProjectLayerVisible,
                    annotationInputActions = annotationViewModel.patternInputActions(),
                    annotationToolbarActions = annotationViewModel.patternToolbarActions(),
                    onExport = annotationViewModel::exportAnnotatedPdf,
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
        value = null
        val activeRenderer =
            renderer ?: run {
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
internal data class TopBarState(
    val patternName: String?,
    val totalPages: Int,
    val currentPage: Int,
    val currentRow: Int?,
    val canDetachPattern: Boolean,
    val canManageDocuments: Boolean,
    val readingLineEnabled: Boolean,
    val readingLineFollowCurrentRow: Boolean,
    val verticalReadingGuideEnabled: Boolean,
    val canManageBookmarks: Boolean,
    val hasCurrentRowMarker: Boolean,
    val hasPageRowMarkers: Boolean,
)

internal data class TopBarActions(
    val onBack: () -> Unit,
    val onJumpToPage: (Int) -> Unit,
    val onReadingLineToggle: (Boolean) -> Unit,
    val onReadingLineFollowToggle: (Boolean) -> Unit,
    val onReturnToCurrentRow: () -> Unit,
    val onVerticalReadingGuideToggle: (Boolean) -> Unit,
    val onCenterVerticalReadingGuide: () -> Unit,
    val onOpenBookmarks: () -> Unit,
    val onOpenDocuments: () -> Unit,
    val onSaveReadingLineAsCurrentRow: () -> Unit,
    val onClearReadingLineRowMarker: () -> Unit,
    val onClearReadingLinePageMarkers: () -> Unit,
    val onStartRowCalibration: () -> Unit,
    val onDetachPattern: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatternViewerTopBar(
    state: TopBarState,
    actions: TopBarActions,
) {
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showPageJumpDialog by rememberSaveable { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.patternName) {
        if (state.patternName != null) titleFocusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            Text(
                text = state.patternName ?: stringResource(R.string.pattern_viewer_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.focusRequester(titleFocusRequester).focusable(),
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
    val enabledState = stringResource(R.string.pattern_guide_enabled_state)
    val disabledState = stringResource(R.string.pattern_guide_disabled_state)
    val followingState = stringResource(R.string.pattern_row_following_active)
    val pausedState = stringResource(R.string.pattern_row_following_paused)
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
        if (state.currentRow != null) {
            DropdownMenuItem(
                modifier =
                    Modifier.semantics {
                        stateDescription = if (state.readingLineFollowCurrentRow) followingState else pausedState
                    },
                text = {
                    Text(
                        stringResource(
                            if (state.readingLineFollowCurrentRow) {
                                R.string.pattern_follow_current_row_off
                            } else {
                                R.string.pattern_follow_current_row_on
                            },
                        ),
                    )
                },
                onClick = {
                    closeOverflowMenu()
                    actions.onReadingLineFollowToggle(!state.readingLineFollowCurrentRow)
                },
            )
            if (!state.readingLineFollowCurrentRow) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pattern_return_to_current_row)) },
                    onClick = {
                        closeOverflowMenu()
                        actions.onReturnToCurrentRow()
                    },
                )
            }
        }
        DropdownMenuItem(
            modifier =
                Modifier.semantics {
                    stateDescription = if (state.verticalReadingGuideEnabled) enabledState else disabledState
                },
            text = {
                Text(
                    stringResource(
                        if (state.verticalReadingGuideEnabled) {
                            R.string.pattern_hide_vertical_guide
                        } else {
                            R.string.pattern_show_vertical_guide
                        },
                    ),
                )
            },
            onClick = {
                closeOverflowMenu()
                actions.onVerticalReadingGuideToggle(!state.verticalReadingGuideEnabled)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pattern_center_vertical_guide)) },
            enabled = state.verticalReadingGuideEnabled,
            onClick = {
                closeOverflowMenu()
                actions.onCenterVerticalReadingGuide()
            },
        )
        if (state.canManageBookmarks) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_bookmarks)) },
                onClick = {
                    closeOverflowMenu()
                    actions.onOpenBookmarks()
                },
            )
        }
        if (state.canManageDocuments) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_documents_title)) },
                onClick = {
                    closeOverflowMenu()
                    actions.onOpenDocuments()
                },
            )
        }
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
    val stateText =
        stringResource(
            if (readingLineEnabled) R.string.pattern_guide_enabled_state else R.string.pattern_guide_disabled_state,
        )
    DropdownMenuItem(
        modifier = Modifier.semantics { stateDescription = stateText },
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
        text = { Text(stringResource(R.string.pattern_set_row_marker_here, currentRow)) },
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
    stateProvider: @Composable () -> PatternViewerContentState,
    actions: PatternViewerContentActions,
    modifier: Modifier = Modifier,
) {
    val state = stateProvider()
    val renderedImage = remember(state.renderedBitmap) { state.renderedBitmap?.asImageBitmap() }
    val exportStyle = rememberPatternAnnotationRenderStyle()
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
            val source = state.patternUri?.toUri()
            if (source != null && destination != null) actions.onExport(source, destination, exportStyle)
        }
    val editableLayerVisible =
        when (state.annotationState.owner) {
            is PatternAnnotationOwner.Project -> state.annotationState.projectLayerVisible
            is PatternAnnotationOwner.SavedPattern -> state.annotationState.masterLayerVisible
        }
    val fallbackPatternName = stringResource(R.string.pattern_annotation_export_default_name)
    val exportBaseName = state.patternName?.substringBeforeLast('.')?.ifBlank { null } ?: fallbackPatternName
    val exportFilename = stringResource(R.string.pattern_annotation_export_filename, exportBaseName)
    val viewportFocusRequester = remember { FocusRequester() }
    Column(modifier = modifier) {
        if (state.patternUri != null) {
            PatternAnnotationLayerPanel(
                state = state.annotationState,
                onMasterVisibilityChange = actions.onMasterLayerVisibilityChange,
                onProjectVisibilityChange = actions.onProjectLayerVisibilityChange,
            )
            PatternAnnotationToolbar(
                state = state.annotationState,
                actions = actions.annotationToolbarActions,
            )
            TextButton(
                enabled = !state.annotationState.isExporting,
                onClick = { exportLauncher.launch(exportFilename) },
            ) {
                val exportText =
                    if (state.annotationState.isExporting) {
                        stringResource(
                            R.string.pattern_annotation_export_progress,
                            state.annotationState.exportCompletedPages,
                            state.annotationState.exportTotalPages,
                        )
                    } else {
                        stringResource(R.string.pattern_annotation_export_pdf)
                    }
                Text(exportText)
            }
            if (state.annotationState.exportFailed) {
                Text(
                    text = stringResource(R.string.pattern_annotation_export_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        when {
            state.patternUri == null -> {
                PatternViewerMessage(message = stringResource(R.string.no_pattern_attached))
            }

            state.rendererError != null -> {
                PatternViewerMessage(
                    message = state.rendererError.ifBlank { stringResource(R.string.pattern_open_failed) },
                )
            }

            renderedImage == null -> {
                PatternViewerMessage(message = stringResource(R.string.pattern_loading))
            }

            else -> {
                PatternDocumentViewport(
                    renderedBitmapProvider = { renderedImage },
                    contentDescription = state.patternName,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(viewportFocusRequester)
                            .focusable(),
                    currentPage = state.currentPage,
                    focusRequest = state.viewportFocusRequest,
                    onFocusRequestConsumed = { requestId ->
                        viewportFocusRequester.requestFocus()
                        actions.onViewportFocusRequestConsumed(requestId)
                    },
                    overlay = { viewport ->
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
                        PatternAnnotationOverlay(
                            masterAnnotations = state.annotationState.masterAnnotations,
                            projectAnnotations = state.annotationState.projectAnnotations,
                            masterVisible = state.annotationState.masterLayerVisible,
                            projectVisible = state.annotationState.projectLayerVisible,
                            inProgressAnnotation = state.annotationState.inProgressAnnotation,
                            inProgressVisible = editableLayerVisible,
                            selectedAnnotationId = state.annotationState.selectedAnnotationId,
                            trackerHighlights = state.annotationState.trackerHighlights,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (state.readingLineEnabled) {
                            ReadingLineOverlay(
                                yFraction = state.readingLineYFraction,
                                currentRow = state.currentRow,
                                followingCurrentRow = state.readingLineFollowCurrentRow,
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
                        if (state.verticalReadingGuideEnabled) {
                            VerticalReadingGuideOverlay(
                                xFraction = state.verticalReadingGuideXFraction,
                                scale = viewport.state.scale,
                                actions =
                                    VerticalGuideOverlayActions(
                                        onDragStart = actions.onVerticalGuideDragStart,
                                        onXFractionChange = actions.onVerticalGuideXFractionChange,
                                        onXFractionCommit = actions.onVerticalGuideXFractionCommit,
                                        onDragCancel = actions.onVerticalGuideDragCancel,
                                    ),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                    interactionOverlay = { viewport ->
                        if (editableLayerVisible && state.annotationState.activeTool != PatternAnnotationTool.BROWSE) {
                            PatternAnnotationInputOverlay(
                                activeTool = state.annotationState.activeTool,
                                coordinateTransform = viewport.coordinateTransform,
                                viewportScale = viewport.state.scale,
                                pressureEnabled = state.annotationState.pressureEnabled,
                                actions = actions.annotationInputActions,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )
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
    val readingLineFollowCurrentRow: Boolean?,
    val verticalReadingGuideEnabled: Boolean,
    val verticalReadingGuideXFraction: Float,
    val currentPage: Int,
    val viewportFocusRequest: PatternViewportFocusRequest?,
    val annotationState: PatternAnnotationUiState,
)

private data class PatternViewerContentActions(
    val onReadingLineDragStart: () -> Unit,
    val onReadingLineYFractionChange: (Float) -> Unit,
    val onReadingLineYFractionCommit: (Float) -> Unit,
    val onReadingLineDragCancel: () -> Unit,
    val onVerticalGuideDragStart: () -> Unit,
    val onVerticalGuideXFractionChange: (Float) -> Unit,
    val onVerticalGuideXFractionCommit: (Float) -> Unit,
    val onVerticalGuideDragCancel: () -> Unit,
    val onViewportFocusRequestConsumed: (Long) -> Unit,
    val onMasterLayerVisibilityChange: (Boolean) -> Unit,
    val onProjectLayerVisibilityChange: (Boolean) -> Unit,
    val annotationInputActions: PatternAnnotationInputActions,
    val annotationToolbarActions: PatternAnnotationToolbarActions,
    val onExport: (Uri, Uri, PatternAnnotationRenderStyle) -> Unit,
)

private fun PatternAnnotationViewModel.patternInputActions() =
    PatternAnnotationInputActions(
        onBeginStroke = ::beginStroke,
        onAppendStrokePoint = ::appendStrokePoint,
        onCommitStroke = ::commitStroke,
        onCancelStroke = ::cancelStroke,
        onEraseStroke = ::eraseStrokeAt,
        onSelectAnnotation = ::selectAnnotationAt,
    )

private fun PatternAnnotationViewModel.patternToolbarActions() =
    PatternAnnotationToolbarActions(
        onToolSelected = ::setActiveTool,
        onPenArgbChange = ::setPenArgb,
        onHighlighterArgbChange = ::setHighlighterArgb,
        onPenStrokeWidthChange = ::setPenStrokeWidth,
        onHighlighterStrokeWidthChange = ::setHighlighterStrokeWidth,
        onPressureEnabledChange = ::setPressureEnabled,
        onHighlighterAxisLockChange = ::setHighlighterAxisLock,
        onMoveSelected = ::moveSelected,
        onResizeSelected = ::resizeSelected,
        onDuplicateSelected = ::duplicateSelected,
        onDeleteSelected = ::deleteSelected,
        onBringSelectedForward = ::bringSelectedForward,
        onSendSelectedBackward = ::sendSelectedBackward,
        onAddTextBox = { text -> addTextBox(text) },
        onAddCallout = { title, description, symbol -> addCallout(title, description, symbol) },
        onUndo = ::undo,
        onRedo = ::redo,
        onClearPage = ::clearEditablePage,
        onAddChartTracker = ::addChartTrackerFromSelected,
    )

internal data class ReadingLineOverlayActions(
    val onDragStart: () -> Unit,
    val onYFractionChange: (Float) -> Unit,
    val onYFractionCommit: (Float) -> Unit,
    val onDragCancel: () -> Unit,
)

@Composable
internal fun ReadingLineOverlay(
    yFraction: Float,
    currentRow: Int?,
    followingCurrentRow: Boolean?,
    scale: Float,
    actions: ReadingLineOverlayActions,
    modifier: Modifier = Modifier,
) {
    val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)
    val currentYFraction by rememberUpdatedState(sanitizedYFraction)
    val lineColor = MaterialTheme.colorScheme.primary
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = READING_LINE_BAND_ALPHA)
    val description = stringResource(R.string.pattern_horizontal_reading_line)
    val stateText =
        stringResource(
            when (followingCurrentRow) {
                true -> R.string.pattern_reading_line_following_state
                false -> R.string.pattern_reading_line_manual_state
                null -> R.string.pattern_reading_line_position_state
            },
            (sanitizedYFraction * 100).toInt(),
        )
    val moveUp = stringResource(R.string.pattern_reading_line_move_up)
    val moveDown = stringResource(R.string.pattern_reading_line_move_down)
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val handleSize = 48.dp
        val maxHandleOffset = (maxHeight - handleSize).coerceAtLeast(0.dp)
        val handleOffset = (maxHeight * sanitizedYFraction - (handleSize / 2f)).coerceIn(0.dp, maxHandleOffset)
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
        Canvas(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = handleOffset)
                    .size(handleSize)
                    .minimumInteractiveComponentSize()
                    .semantics {
                        contentDescription = description
                        stateDescription = stateText
                        customActions =
                            listOf(
                                CustomAccessibilityAction(moveUp) {
                                    val next =
                                        horizontalReadingLineAccessibilityStep(
                                            sanitizedYFraction,
                                            forward = false,
                                        )
                                    actions.onYFractionChange(next)
                                    actions.onYFractionCommit(next)
                                    true
                                },
                                CustomAccessibilityAction(moveDown) {
                                    val next =
                                        horizontalReadingLineAccessibilityStep(
                                            sanitizedYFraction,
                                            forward = true,
                                        )
                                    actions.onYFractionChange(next)
                                    actions.onYFractionCommit(next)
                                    true
                                },
                            )
                    }.pointerInput(scale, containerHeightPx) {
                        var lastYFraction = sanitizedYFraction
                        detectVerticalDragGestures(
                            onDragStart = {
                                actions.onDragStart()
                                lastYFraction = currentYFraction
                            },
                            onDragEnd = { actions.onYFractionCommit(lastYFraction) },
                            onDragCancel = { actions.onDragCancel() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (containerHeightPx <= 0f) return@detectVerticalDragGestures
                                val adjustedDrag = dragAmount / scale.coerceAtLeast(1f)
                                lastYFraction =
                                    (lastYFraction + (adjustedDrag / containerHeightPx)).coerceIn(
                                        READING_LINE_MIN_Y_FRACTION,
                                        READING_LINE_MAX_Y_FRACTION,
                                    )
                                actions.onYFractionChange(lastYFraction)
                            },
                        )
                    },
        ) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = center)
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

internal data class VerticalGuideOverlayActions(
    val onDragStart: () -> Unit,
    val onXFractionChange: (Float) -> Unit,
    val onXFractionCommit: (Float) -> Unit,
    val onDragCancel: () -> Unit,
)

@Composable
internal fun VerticalReadingGuideOverlay(
    xFraction: Float,
    scale: Float,
    actions: VerticalGuideOverlayActions,
    modifier: Modifier = Modifier,
) {
    val sanitizedXFraction = sanitizeReadingGuideFraction(xFraction)
    val currentXFraction by rememberUpdatedState(sanitizedXFraction)
    val lineColor = MaterialTheme.colorScheme.tertiary
    val bandColor = lineColor.copy(alpha = VERTICAL_GUIDE_BAND_ALPHA)
    val description = stringResource(R.string.pattern_vertical_guide_description)
    val stateText = stringResource(R.string.pattern_vertical_guide_position_state, (sanitizedXFraction * 100).toInt())
    val moveLeft = stringResource(R.string.pattern_vertical_guide_move_left)
    val moveRight = stringResource(R.string.pattern_vertical_guide_move_right)
    val centerAction = stringResource(R.string.pattern_center_vertical_guide)
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val handleSize = 48.dp
        val maxHandleOffset = (maxWidth - handleSize).coerceAtLeast(0.dp)
        val handleOffset = (maxWidth * sanitizedXFraction - (handleSize / 2f)).coerceIn(0.dp, maxHandleOffset)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width * sanitizedXFraction
            val bandWidth = (size.width * VERTICAL_GUIDE_BAND_WIDTH_FRACTION).coerceAtLeast(24.dp.toPx())
            drawRect(
                color = bandColor,
                topLeft = Offset(centerX - (bandWidth / 2f), 0f),
                size = Size(bandWidth, size.height),
            )
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }
        Canvas(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = handleOffset)
                    .size(handleSize)
                    .minimumInteractiveComponentSize()
                    .semantics {
                        contentDescription = description
                        stateDescription = stateText
                        customActions =
                            listOf(
                                CustomAccessibilityAction(moveLeft) {
                                    val next =
                                        verticalReadingGuideAccessibilityStep(
                                            sanitizedXFraction,
                                            forward = false,
                                        )
                                    actions.onXFractionChange(next)
                                    actions.onXFractionCommit(next)
                                    true
                                },
                                CustomAccessibilityAction(moveRight) {
                                    val next = verticalReadingGuideAccessibilityStep(sanitizedXFraction, forward = true)
                                    actions.onXFractionChange(next)
                                    actions.onXFractionCommit(next)
                                    true
                                },
                                CustomAccessibilityAction(centerAction) {
                                    actions.onXFractionChange(DEFAULT_READING_GUIDE_FRACTION)
                                    actions.onXFractionCommit(DEFAULT_READING_GUIDE_FRACTION)
                                    true
                                },
                            )
                    }.pointerInput(scale, containerWidthPx) {
                        var lastXFraction = sanitizedXFraction
                        detectHorizontalDragGestures(
                            onDragStart = {
                                actions.onDragStart()
                                lastXFraction = currentXFraction
                            },
                            onDragEnd = { actions.onXFractionCommit(lastXFraction) },
                            onDragCancel = { actions.onDragCancel() },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (containerWidthPx <= 0f) return@detectHorizontalDragGestures
                                val adjustedDrag = dragAmount / scale.coerceAtLeast(1f)
                                lastXFraction =
                                    (lastXFraction + (adjustedDrag / containerWidthPx)).coerceIn(
                                        READING_LINE_MIN_Y_FRACTION,
                                        READING_LINE_MAX_Y_FRACTION,
                                    )
                                actions.onXFractionChange(lastXFraction)
                            },
                        )
                    },
        ) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = center)
        }
    }
}

internal fun horizontalReadingLineAccessibilityStep(
    currentFraction: Float,
    forward: Boolean,
): Float =
    sanitizeReadingLineYFraction(
        currentFraction + if (forward) READING_LINE_ROW_STEP_FRACTION else -READING_LINE_ROW_STEP_FRACTION,
    )

internal fun verticalReadingGuideAccessibilityStep(
    currentFraction: Float,
    forward: Boolean,
): Float =
    sanitizeReadingGuideFraction(
        currentFraction + if (forward) READING_LINE_ROW_STEP_FRACTION else -READING_LINE_ROW_STEP_FRACTION,
    )

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
