package com.finnvek.knittools.ui.screens.pattern

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.di.AppDispatchers
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.createCalibrationRowMarkers
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.model.DEFAULT_READING_GUIDE_FRACTION
import com.finnvek.knittools.domain.model.DEFAULT_READING_LINE_Y_FRACTION
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.PROJECT_DOCUMENT_LABEL_MAX_LENGTH
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.PatternDisplayNames
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.READING_LINE_MAX_Y_FRACTION
import com.finnvek.knittools.domain.model.READING_LINE_MIN_Y_FRACTION
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.sanitizeReadingGuideFraction
import com.finnvek.knittools.domain.model.sanitizeReadingLineYFraction
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.components.BackIconButton
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.OverviewEmptyText
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.RowMenuAction
import com.finnvek.knittools.ui.components.RowOverflowMenu
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.WebPatternMetadata
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.components.mainCounterCountTextInline
import com.finnvek.knittools.ui.components.mainCounterLabelText
import com.finnvek.knittools.ui.components.projectDocumentDisplayName
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.layout.AdaptiveDropdownMenu
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.CounterViewerEvent
import com.finnvek.knittools.ui.theme.PatternViewerDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.rememberPatternAnnotationRenderStyle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val READING_LINE_BAND_HEIGHT_FRACTION = 0.045f
private const val READING_LINE_BAND_ALPHA = 0.14f
private const val VERTICAL_GUIDE_BAND_WIDTH_FRACTION = 0.035f
private const val VERTICAL_GUIDE_BAND_ALPHA = 0.10f
private const val CONTENT_ERROR_TIP_BLOCK_MILLIS = 3_500L

internal data class PatternRenderState(
    val renderer: PdfPageRenderer?,
    val rendererError: String?,
    val renderedBitmap: Bitmap?,
) {
    /** Merkitse näytetään vasta kun sivu on piirretty; latauksen aikana ei ole mitä merkitä. */
    val canAnnotate: Boolean get() = rendererError == null && renderedBitmap != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Reitti omistaa viewerin lähdekohtaiset tilat ja navigointitoiminnot.
fun PatternViewerScreen(
    onBack: () -> Unit,
    onImportFromRavelry: () -> Unit,
    onSeePro: () -> Unit,
    onSavedPatternDetail: (Long) -> Unit,
    onEditWebPattern: (Long) -> Unit,
    onOpenProjectNotes: () -> Unit,
    counterViewModelProvider: @Composable () -> CounterViewModel,
    patternViewerViewModelProvider: @Composable () -> PatternViewerViewModel,
    annotationViewModelProvider: @Composable () -> PatternAnnotationViewModel,
) {
    val counterViewModel = counterViewModelProvider()
    val patternViewerViewModel = patternViewerViewModelProvider()
    val annotationViewModel = annotationViewModelProvider()
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    val savedPatterns by counterViewModel.savedPatterns.collectAsStateWithLifecycle()
    val bookmarkState by patternViewerViewModel.uiState.collectAsStateWithLifecycle()
    val documentState by patternViewerViewModel.documentUiState.collectAsStateWithLifecycle()
    val annotationState by annotationViewModel.uiState.collectAsStateWithLifecycle()
    val patternImportInProgress by counterViewModel.patternImportInProgress.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val selectedDocument = documentState.selectedDocument
    val contentErrorMessage = stringResource(R.string.generic_error_unknown)
    val contentErrorTimeout =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = CONTENT_ERROR_TIP_BLOCK_MILLIS,
            containsText = true,
        ) ?: CONTENT_ERROR_TIP_BLOCK_MILLIS
    var contentErrorSequence by remember { mutableIntStateOf(0) }
    var contentErrorVisible by remember { mutableStateOf(false) }
    CollectWithLifecycleEffect({ counterViewModel.projectContentErrors }) {
        contentErrorVisible = true
        contentErrorSequence++
        Toast.makeText(context, contentErrorMessage, Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(contentErrorSequence) {
        if (contentErrorVisible) {
            delay(contentErrorTimeout)
            contentErrorVisible = false
        }
    }
    val selectedDocumentAvailable = selectedDocument?.let { documentState.isAvailable(it.id) } == true
    val patternUri = selectedDocument?.localPdfUri?.takeIf { selectedDocumentAvailable }
    val documentError = documentState.error
    val unavailableDocumentError =
        when {
            selectedDocument != null && documentState.availability[selectedDocument.id] == false -> {
                stringResource(R.string.project_documents_error_unavailable)
            }

            selectedDocument == null && documentError != null -> {
                stringResource(documentError.messageRes())
            }

            else -> {
                null
            }
        }
    val exportContext =
        selectedDocument?.takeIf { selectedDocumentAvailable }?.let { document ->
            PatternPdfExportContext(
                sourceUri = document.localPdfUri.toUri(),
                owner = PatternAnnotationOwner.Project(document.projectId, document.documentKey),
                projectDocumentId = document.id,
            )
        }
    val currentPage = selectedDocument?.currentPage ?: 0
    val rowMarkers = remember(selectedDocument?.rowMapping) { parseMapping(selectedDocument?.rowMapping) }
    val hasCurrentRowMarker =
        rowMarkers.any { marker -> marker.row == counterState.counter.count && marker.page == currentPage }
    val hasPageRowMarkers = rowMarkers.any { marker -> marker.page == currentPage }
    val renderState =
        rememberPatternRenderState(
            patternUri = patternUri,
            currentPage = currentPage,
            onPageClamped = { page -> counterViewModel.updatePatternPage(page, selectedDocument?.id) },
        )
    var rowCalibrationState by
        rememberSaveable(selectedDocument?.id, selectedDocument?.rowMapping, stateSaver = RowCalibrationState.Saver) {
            mutableStateOf<RowCalibrationState?>(null)
        }
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
    var topBarBlocking by remember { mutableStateOf(false) }
    var viewportFocusRequest by remember(selectedDocument?.id) { mutableStateOf<PatternViewportFocusRequest?>(null) }
    var accessibilityAnnouncement by remember(selectedDocument?.id) { mutableStateOf<String?>(null) }
    val annotationMode =
        rememberAnnotationModeControls(patternUri, annotationState.activeTool, annotationViewModel::setActiveTool)

    LaunchedEffect(currentPage) {
        annotationViewModel.setCurrentPage(currentPage)
    }

    LaunchedEffect(selectedDocument?.id, selectedDocument?.readingLineYFraction) {
        if (!isReadingLineDragging) {
            readingLinePreviewYFraction = selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION
        }
    }
    LaunchedEffect(selectedDocument?.id, selectedDocument?.verticalReadingGuideXFraction) {
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
                showBookmarkSheet = false
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

    val readerNoticeBlocked =
        listOf(
            topBarBlocking,
            showBookmarkSheet,
            showDocumentSheet,
            showDocumentPicker,
            rowCalibrationState != null,
            annotationMode.active,
            annotationMode.showLayers,
            annotationState.isExporting,
            annotationState.loadError != PatternAnnotationLoadError.NONE,
            annotationState.writeError != PatternAnnotationWriteError.NONE,
            counterState.activeSession?.needsRecoveryReview == true,
            patternImportInProgress,
            contentErrorVisible,
        ).any { it }
    val noticeDocument =
        selectedDocument?.takeIf {
            !documentState.isLoading &&
                selectedDocumentAvailable &&
                renderState.canAnnotate &&
                it.projectId == counterState.projectId
        }
    val counterSlot =
        MainCounterCountSlot(
            counterState.counter.count,
            counterState.mainCounterLabelType,
            counterState.mainCounterCustomLabel,
        )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PatternViewerTopBar(
                onBlockingChanged = { topBarBlocking = it },
                state =
                    TopBarState(
                        patternName = selectedDocument?.label,
                        totalPages = renderState.renderer?.pageCount ?: 0,
                        currentPage = currentPage,
                        currentRow = counterSlot.count.takeIf { patternUri != null },
                        countLabelType = counterSlot.labelType,
                        countCustomLabel = counterSlot.customLabel,
                        canManageDocuments = true,
                        readingLineEnabled = selectedDocument?.readingLineEnabled == true,
                        readingLineFollowCurrentRow = selectedDocument?.readingLineFollowCurrentRow == true,
                        verticalReadingGuideEnabled = selectedDocument?.verticalReadingGuideEnabled == true,
                        canManageBookmarks = true,
                        hasCurrentRowMarker = hasCurrentRowMarker,
                        hasPageRowMarkers = hasPageRowMarkers,
                    ).withAnnotations(
                        renderState.canAnnotate && annotationState.matchesDisplayedPage(exportContext, currentPage),
                        annotationMode,
                        annotationState,
                    ),
                actions =
                    TopBarActions(
                        onBack = onBack,
                        onJumpToPage = { page -> counterViewModel.updatePatternPage(page, selectedDocument?.id) },
                        onReadingLineToggle = { enabled ->
                            counterViewModel.setReadingLineEnabled(enabled, selectedDocument?.id)
                        },
                        onReadingLineFollowToggle = { enabled ->
                            counterViewModel.setReadingLineFollowCurrentRow(enabled, selectedDocument?.id)
                        },
                        onReturnToCurrentRow = {
                            counterViewModel.returnReadingLineToCurrentRow(selectedDocument?.id)
                        },
                        onVerticalReadingGuideToggle = { enabled ->
                            counterViewModel.setVerticalReadingGuideEnabled(enabled, selectedDocument?.id)
                        },
                        onCenterVerticalReadingGuide = {
                            counterViewModel.centerVerticalReadingGuide(selectedDocument?.id)
                        },
                        onOpenDocuments = { showDocumentSheet = true },
                        onOpenProjectNotes = onOpenProjectNotes.takeIf { selectedDocument != null },
                        onSaveReadingLineAsCurrentRow = {
                            counterViewModel.upsertPatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                                yPosition =
                                    selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
                                documentId = selectedDocument?.id,
                                documentRowMapping = selectedDocument?.rowMapping,
                            )
                        },
                        onClearReadingLineRowMarker = {
                            counterViewModel.removePatternRowMarker(
                                row = counterState.counter.count,
                                page = currentPage,
                                documentId = selectedDocument?.id,
                                documentRowMapping = selectedDocument?.rowMapping,
                            )
                        },
                        onClearReadingLinePageMarkers = {
                            counterViewModel.removePatternRowMarkersForPage(
                                page = currentPage,
                                documentId = selectedDocument?.id,
                                documentRowMapping = selectedDocument?.rowMapping,
                            )
                        },
                        onStartRowCalibration = {
                            counterViewModel.setReadingLineEnabled(true, selectedDocument?.id)
                            rowCalibrationState =
                                RowCalibrationState(
                                    rowInput = counterState.counter.count.toString(),
                                    expectedMapping = selectedDocument?.rowMapping,
                                )
                        },
                    ).withAnnotations(annotationMode, annotationViewModel, exportContext),
            )
        },
        bottomBar = {
            Column {
                ProjectReadingLineNotice(
                    document = noticeDocument,
                    blocked = readerNoticeBlocked,
                    counterViewModel = counterViewModel,
                )
                PatternViewerBottomBar(
                    state =
                        PatternViewerBottomBarState(
                            countSlot = counterSlot,
                            currentPage = currentPage,
                            totalPages = renderState.renderer?.pageCount ?: 0,
                        ),
                    actions =
                        PatternViewerBottomBarActions(
                            onPreviousRow = counterViewModel::decrement,
                            onNextRow = counterViewModel::increment,
                            onPreviousPage = {
                                counterViewModel.updatePatternPage(currentPage - 1, selectedDocument?.id)
                            },
                            onNextPage = {
                                counterViewModel.updatePatternPage(currentPage + 1, selectedDocument?.id)
                            },
                            // Kirjanmerkit omana painikkeenaan: 11 rivin valikossa niitä ei löytänyt.
                            onOpenBookmarks = {
                                patternViewerViewModel.selectNearestBookmark(
                                    pageIndex = currentPage,
                                    yFraction =
                                        selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION,
                                )
                                showBookmarkSheet = true
                            },
                        ),
                )
            }
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
                    countSlot = counterSlot,
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
                                calibrationState.copy(
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
                                    showInvalidRowError = false,
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
                            counterViewModel.mergePatternRowMarkers(
                                markersToMerge = markers,
                                documentId = selectedDocument?.id,
                                documentRowMapping = calibrationState.expectedMapping,
                            )
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
                        rendererError = renderState.rendererError ?: unavailableDocumentError,
                        renderedBitmap = renderState.renderedBitmap,
                        patternName = selectedDocument?.label,
                        countSlot = counterSlot,
                        readingLineEnabled = selectedDocument?.readingLineEnabled == true,
                        readingLineYFraction = readingLinePreviewYFraction,
                        readingLineFollowCurrentRow = selectedDocument?.readingLineFollowCurrentRow,
                        verticalReadingGuideEnabled = selectedDocument?.verticalReadingGuideEnabled == true,
                        verticalReadingGuideXFraction = verticalGuidePreviewXFraction,
                        currentPage = currentPage,
                        viewportFocusRequest = viewportFocusRequest,
                        annotationState = annotationState,
                        exportContext = exportContext,
                        annotationMode = annotationMode.active,
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
                            counterViewModel.commitManualReadingLinePosition(
                                sanitizedYFraction,
                                selectedDocument?.id,
                            )
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
                            counterViewModel.updateVerticalReadingGuideXFraction(
                                xFraction = sanitizedXFraction,
                                documentId = selectedDocument?.id,
                            )
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
                        annotationActions = annotationViewModel.patternContentAnnotationActions(),
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        }
    }

    if (annotationMode.showLayers) {
        PatternAnnotationLayersSheet(
            state = annotationState,
            onMasterVisibilityChange = annotationViewModel::setMasterLayerVisible,
            onProjectVisibilityChange = annotationViewModel::setProjectLayerVisible,
            onDismiss = annotationMode.closeLayers,
        )
    }

    if (showBookmarkSheet) {
        val bookmarkDocumentKey = selectedDocument?.documentKey
        val bookmarkPageCount = renderState.renderer?.pageCount ?: 0
        PatternBookmarkSheet(
            state = bookmarkState,
            totalPages = bookmarkPageCount,
            canAddAtCurrentLocation =
                renderState.canAnnotate &&
                    currentPage in 0 until bookmarkPageCount &&
                    bookmarkDocumentKey != null &&
                    bookmarkState.documentKey == bookmarkDocumentKey,
            actions =
                PatternBookmarkSheetActions(
                    onDismiss = { showBookmarkSheet = false },
                    onAdd = { name, onSaved ->
                        patternViewerViewModel.addBookmark(
                            name = name,
                            pageIndex = currentPage,
                            yFraction =
                                if (selectedDocument?.readingLineEnabled == true) {
                                    selectedDocument.readingLineYFraction
                                } else {
                                    DEFAULT_READING_LINE_Y_FRACTION
                                },
                            expectedDocumentKey = bookmarkDocumentKey,
                            onSaved = onSaved,
                        )
                    },
                    onJump = patternViewerViewModel::jumpToBookmark,
                    onPrevious = patternViewerViewModel::jumpToPreviousBookmark,
                    onNext = patternViewerViewModel::jumpToNextBookmark,
                    onRename = patternViewerViewModel::renameBookmark,
                    onDelete = patternViewerViewModel::deleteBookmark,
                    onClearError = patternViewerViewModel::clearError,
                ),
        )
    }

    if (showDocumentSheet) {
        ProjectDocumentsSheet(
            state = documentState,
            showCurrentDocument = true,
            metadataPattern =
                counterState.linkedPattern
                    ?.takeIf { pattern ->
                        documentState.documents.none { it.savedPatternId == pattern.id }
                    },
            onOpenPatternInformation = {
                counterState.linkedPattern?.id?.let(onSavedPatternDetail)
            },
            onOpenPatternWebsite = {
                // CPD-OFF: Ruudun linkinavaus ja virhepalaute pidetaan kayttokohteen yhteydessa.
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
                // CPD-ON
            },
            onEditPatternInformation = {
                counterState.linkedPattern?.id?.let(onEditWebPattern)
            },
            onUnlinkPatternInformation = { expectedPatternId ->
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
            isRestoringProject = counterState.isRestoringProject,
            savedPatterns = savedPatterns.filter { !it.localPdfUri.isNullOrBlank() },
            canUseCameraScan = counterState.canUsePatternCameraScan,
            proState = counterState.proState,
            proAccessReady = counterState.proAccessReady,
            mode = PatternPickerMode.ADD_READABLE_PROJECT_DOCUMENT,
            excludedSavedPatternIds = documentState.documents.mapNotNullTo(mutableSetOf()) { it.savedPatternId },
            onSavedPatternSelected = { patternViewerViewModel.addSavedPattern(it.id) },
            onDocumentSelected = { uri, name, owner ->
                counterViewModel.attachPattern(uri, name, expectedProjectId = owner) { result ->
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
@Suppress("kotlin:S107") // Dokumenttisheet välittää dokumenttikohtaiset toiminnot eksplisiittisesti.
internal fun ProjectDocumentsSheet(
    state: ProjectDocumentUiState,
    metadataPattern: SavedPattern? = null,
    metadataPatternName: String? = null,
    showCurrentDocument: Boolean = false,
    onOpenPatternInformation: () -> Unit = {},
    onOpenPatternWebsite: () -> Unit = {},
    onEditPatternInformation: () -> Unit = {},
    onUnlinkPatternInformation: (Long) -> Unit = {},
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
    var renameDocumentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var removeDocumentId by rememberSaveable { mutableStateOf<Long?>(null) }
    val renameDocument = state.documents.firstOrNull { it.id == renameDocumentId }
    val removeDocument = state.documents.firstOrNull { it.id == removeDocumentId }
    var pendingUnlinkPatternId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingUnlinkPatternName by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberExpandedModalSheetState()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveModalWindow()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Sama otsikko ja oikean reunan tekstitoiminto kuin muissa sheeteissä ja projektinäkymässä.
                SheetTitle(text = stringResource(R.string.project_documents_title), modifier = Modifier.weight(1f))
                OverviewTextAction(
                    label = stringResource(R.string.project_documents_add),
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onAdd()
                        }
                    },
                    enabled = !state.isMutating,
                )
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
                Column {
                    SectionLabel(text = stringResource(R.string.project_documents_pattern_information))
                    Spacer(Modifier.height(PatternViewerDimens.DocumentInfoLineSpacing))
                    val webPattern = metadataPattern?.takeIf(SavedPattern::isWebPatternCompatible)
                    val webUrl = webPattern?.webPatternUrlOrNull
                    Row {
                        Column(
                            modifier = Modifier.weight(1f).alignBy(FirstBaseline),
                            verticalArrangement = Arrangement.spacedBy(PatternViewerDimens.DocumentInfoLineSpacing),
                        ) {
                            Text(it, style = MaterialTheme.typography.titleSmall)
                            if (webPattern != null) {
                                WebPatternMetadata(webPattern.designerName, webUrl?.host, uppercaseBadge = false)
                            } else {
                                Text(
                                    text = stringResource(R.string.project_documents_metadata_only),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (webPattern != null) {
                            RowOverflowMenu(
                                modifier = Modifier.alignBy { it.measuredHeight / 2 },
                                actions =
                                    listOf(
                                        RowMenuAction(stringResource(R.string.web_pattern_edit)) {
                                            coroutineScope.launch {
                                                sheetState.hide()
                                                onEditPatternInformation()
                                                onDismiss()
                                            }
                                        },
                                        RowMenuAction(
                                            stringResource(R.string.web_pattern_unlink_confirm),
                                            isDestructive = true,
                                        ) {
                                            pendingUnlinkPatternId = webPattern.id
                                            pendingUnlinkPatternName = webPattern.name
                                        },
                                    ),
                            )
                        }
                    }
                    if (webPattern != null) {
                        val openDescription =
                            stringResource(
                                R.string.web_pattern_open_website_description,
                                webPattern.name,
                                webUrl?.host.orEmpty(),
                            )
                        OverviewLinkRow(
                            title = stringResource(R.string.web_pattern_open_website),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            subtitle = null,
                            glyph = null,
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onOpenPatternWebsite()
                                    onDismiss()
                                }
                            },
                            modifier =
                                Modifier
                                    .padding(top = PatternViewerDimens.DocumentWebsiteGap)
                                    .semantics(mergeDescendants = true) { contentDescription = openDescription },
                        )
                    } else {
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
            SectionLabel(text = stringResource(R.string.project_documents_pdf_section))
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                state.documents.isEmpty() -> {
                    OverviewEmptyText(R.string.project_documents_empty)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.documents, key = { _, document -> document.id }) { index, document ->
                            val label = projectDocumentDisplayName(document.label)
                            ProjectDocumentRow(
                                document = document,
                                index = index,
                                documentCount = state.documents.size,
                                isSelected = showCurrentDocument && state.selectedDocumentId == document.id,
                                isAvailable = state.isAvailable(document.id),
                                enabled = !state.isMutating,
                                onSelect = { onSelect(document.id) },
                                onRename = {
                                    renameDocumentId = document.id
                                    renameText = label
                                },
                                onMoveEarlier = { onMoveEarlier(document.id) },
                                onMoveLater = { onMoveLater(document.id) },
                                onSetPrimary = { onSetPrimary(document.id) },
                                onRemove = { removeDocumentId = document.id },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingUnlinkPatternId?.let { expectedPatternId ->
        ConfirmationDialog(
            title = stringResource(R.string.web_pattern_unlink_title),
            message = stringResource(R.string.web_pattern_unlink_body, pendingUnlinkPatternName),
            confirmText = stringResource(R.string.web_pattern_unlink_confirm),
            onDismiss = { pendingUnlinkPatternId = null },
            onConfirm = {
                pendingUnlinkPatternId = null
                coroutineScope.launch {
                    sheetState.hide()
                    onUnlinkPatternInformation(expectedPatternId)
                    onDismiss()
                }
            },
        )
    }

    renameDocument?.let { document ->
        val prefilledLabel = projectDocumentDisplayName(document.label)
        ScrollableFormDialog(
            onDismissRequest = { renameDocumentId = null },
            title = { Text(stringResource(R.string.project_documents_rename_title)) },
            text = {
                LabeledTextField(
                    value = renameText,
                    onValueChange = {
                        renameText = it.takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
                    },
                    label = stringResource(R.string.project_documents_label),
                    supportingText =
                        stringResource(
                            R.string.project_documents_label_limit,
                            PROJECT_DOCUMENT_LABEL_MAX_LENGTH,
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Kenttään esitäytetään siistitty nimi; muuttamaton tallennus säilyttää alkuperäisen nimikkeen.
                        if (isDocumentRenameChange(renameText, prefilledLabel)) {
                            onRename(document.id, renameText)
                        }
                        renameDocumentId = null
                    },
                    enabled = renameText.trim().isNotEmpty(),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                CancelButton(onClick = { renameDocumentId = null })
            },
        )
    }

    removeDocument?.let { document ->
        ConfirmationDialog(
            title = stringResource(R.string.project_documents_remove_title),
            message =
                stringResource(
                    when {
                        state.documents.size == 1 -> R.string.project_documents_remove_last_message
                        document.isPrimary -> R.string.project_documents_remove_primary_message
                        else -> R.string.project_documents_remove_message
                    },
                    projectDocumentDisplayName(document.label),
                ),
            confirmText = stringResource(R.string.project_documents_remove_action),
            isDestructive = true,
            onConfirm = {
                onRemove(document.id)
                removeDocumentId = null
            },
            onDismiss = { removeDocumentId = null },
        )
    }
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Dokumenttirivi näyttää kaikki saatavuus-, järjestys- ja valintatilat.
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
    val label = projectDocumentDisplayName(document.label)
    val actionsDescription = stringResource(R.string.project_documents_actions, label)
    val primary = if (document.isPrimary) stringResource(R.string.project_documents_primary) else null
    val page =
        if (isSelected || document.currentPage > 0) {
            stringResource(R.string.project_documents_page, document.currentPage.toLong() + 1L)
        } else {
            null
        }
    val position = stringResource(R.string.project_documents_index, index + 1, documentCount)
    val status =
        when {
            primary != null -> {
                listOfNotNull(primary, page).joinToString(", ")
            }

            page != null -> {
                stringResource(
                    R.string.project_documents_page_standalone,
                    document.currentPage.toLong() + 1L,
                )
            }

            else -> {
                ""
            }
        }
    val openNow = if (isSelected) stringResource(R.string.project_documents_open_now) else null
    val unavailable = if (!isAvailable) stringResource(R.string.project_documents_unavailable) else null
    val documentDescription = listOfNotNull(label, primary, position, page, openNow, unavailable).joinToString(", ")
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled && isAvailable,
                    onClickLabel = openLabel,
                    onClick = onSelect,
                ).semantics {
                    contentDescription = documentDescription
                    customActions = if (enabled) accessibilityActions else emptyList()
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (status.isNotEmpty() || openNow != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PatternViewerDimens.DocumentStatusGap),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (status.isNotEmpty()) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (openNow != null) BadgePill(text = openNow, uppercase = false)
                }
            }
            if (unavailable != null) {
                Text(unavailable, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
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
            AdaptiveDropdownMenu(
                expanded = showActions,
                onDismissRequest = { showActions = false },
            ) {
                if (documentCount > 1) {
                    DropdownMenuItem(
                        text = { Text(moveEarlierLabel) },
                        enabled = index > 0,
                        onClick = {
                            showActions = false
                            onMoveEarlier()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(moveLaterLabel) },
                        enabled = index < documentCount - 1,
                        onClick = {
                            showActions = false
                            onMoveLater()
                        },
                    )
                }
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
                    text = {
                        Text(
                            stringResource(R.string.project_documents_remove_action),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        showActions = false
                        onRemove()
                    },
                )
            }
        }
    }
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
    val expectedMapping: String? = null,
) {
    companion object {
        val Saver =
            listSaver<RowCalibrationState?, Any>(
                save = { state ->
                    if (state == null) {
                        emptyList()
                    } else {
                        listOf(
                            state.rowInput,
                            state.showInvalidRowError,
                            state.firstMarker?.row ?: -1,
                            state.firstMarker?.page ?: 0,
                            state.firstMarker?.yPosition ?: 0f,
                            state.expectedMapping.orEmpty(),
                        )
                    }
                },
                restore = { values ->
                    if (values.isEmpty()) {
                        null
                    } else {
                        RowCalibrationState(
                            rowInput = values[0] as String,
                            showInvalidRowError = values[1] as Boolean,
                            expectedMapping = (values.getOrNull(5) as? String)?.ifEmpty { null },
                            firstMarker =
                                (values[2] as Int).takeIf { it >= 0 }?.let {
                                    RowMarker(
                                        it,
                                        values[3] as Int,
                                        values[4] as Float,
                                    )
                                },
                        )
                    }
                },
            )
    }
}

private val RowCalibrationState.isSecondStep: Boolean
    get() = firstMarker != null

@Composable
private fun RowCalibrationState.rowLabel(slot: MainCounterCountSlot): String =
    if (isSecondStep) {
        mainCounterLabelText(
            labelType = slot.labelType,
            customLabel = slot.customLabel,
            rows = R.string.pattern_calibration_last_row,
            rounds = R.string.pattern_calibration_last_round,
            repeats = R.string.pattern_calibration_last_repeat,
            custom = R.string.pattern_calibration_last_custom,
        )
    } else {
        mainCounterLabelText(
            labelType = slot.labelType,
            customLabel = slot.customLabel,
            rows = R.string.pattern_calibration_first_row,
            rounds = R.string.pattern_calibration_first_round,
            repeats = R.string.pattern_calibration_first_repeat,
            custom = R.string.pattern_calibration_first_custom,
        )
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
    countSlot: MainCounterCountSlot,
    onRowInputChange: (String) -> Unit,
    onSaveFirst: () -> Unit,
    onSaveLast: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.knitToolsColors.cardContainer,
        tonalElevation = PatternViewerDimens.CalibrationPanelElevation,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = PatternViewerDimens.BottomBarHorizontalPadding,
                        vertical = PatternViewerDimens.BottomBarVerticalPadding,
                    ),
        ) {
            Text(
                text = stringResource(R.string.pattern_calibrate_rows),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    stringResource(
                        if (state.isSecondStep) {
                            R.string.pattern_calibration_step_last
                        } else {
                            R.string.pattern_calibration_step_first
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PatternViewerDimens.CalibrationInstructionGap),
            )
            NumberInputField(
                value = state.rowInput,
                onValueChange = onRowInputChange,
                label = state.rowLabel(countSlot),
                options = NumberInputOptions(isLast = true, preserveRawInput = true),
                errorMessage =
                    if (state.showInvalidRowError) {
                        stringResource(
                            R.string.pattern_calibration_invalid_row,
                        )
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth().padding(top = PatternViewerDimens.CalibrationFieldGap),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = PatternViewerDimens.CalibrationActionGap),
                horizontalArrangement = Arrangement.spacedBy(PatternViewerDimens.CalibrationActionGap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CancelButton(onClick = onCancel)
                TextButton(
                    onClick = state.saveAction(onSaveFirst, onSaveLast),
                    enabled = state.rowInput.toIntOrNull() != null,
                ) {
                    Text(
                        text = stringResource(R.string.save),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPatternViewerScreen(
    patternUri: String?,
    patternName: String?,
    onBack: () -> Unit,
    annotationViewModelProvider: @Composable () -> PatternAnnotationViewModel,
) {
    val annotationViewModel = annotationViewModelProvider()
    val annotationState by annotationViewModel.uiState.collectAsStateWithLifecycle()
    val exportContext = patternUri?.let { PatternPdfExportContext(it.toUri(), annotationState.owner) }
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
    val annotationMode =
        rememberAnnotationModeControls(patternUri, annotationState.activeTool, annotationViewModel::setActiveTool)

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
                        canManageDocuments = false,
                        readingLineEnabled = readingLineEnabled,
                        readingLineFollowCurrentRow = false,
                        verticalReadingGuideEnabled = verticalReadingGuideEnabled,
                        canManageBookmarks = false,
                        hasCurrentRowMarker = false,
                        hasPageRowMarkers = false,
                    ).withAnnotations(
                        renderState.canAnnotate && annotationState.matchesDisplayedPage(exportContext, currentPage),
                        annotationMode,
                        annotationState,
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
                        onOpenDocuments = {},
                        onSaveReadingLineAsCurrentRow = {},
                        onClearReadingLineRowMarker = {},
                        onClearReadingLinePageMarkers = {},
                        onStartRowCalibration = {},
                    ).withAnnotations(annotationMode, annotationViewModel, exportContext),
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
                    countSlot = null,
                    readingLineEnabled = readingLineEnabled,
                    readingLineYFraction = readingLineYFraction,
                    readingLineFollowCurrentRow = null,
                    verticalReadingGuideEnabled = verticalReadingGuideEnabled,
                    verticalReadingGuideXFraction = verticalReadingGuideXFraction,
                    currentPage = currentPage,
                    viewportFocusRequest = null,
                    annotationState = annotationState,
                    exportContext = exportContext,
                    annotationMode = annotationMode.active,
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
                    annotationActions = annotationViewModel.patternContentAnnotationActions(),
                ),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
        )
    }

    if (annotationMode.showLayers) {
        PatternAnnotationLayersSheet(
            state = annotationState,
            onMasterVisibilityChange = annotationViewModel::setMasterLayerVisible,
            onProjectVisibilityChange = annotationViewModel::setProjectLayerVisible,
            onDismiss = annotationMode.closeLayers,
        )
    }
}

@Composable
internal fun rememberPatternRenderState(
    patternUri: String?,
    currentPage: Int,
    onPageClamped: (Int) -> Unit,
): PatternRenderState {
    val context = LocalContext.current
    val patternOpenFailed = stringResource(R.string.pattern_open_failed)
    var renderer by remember(patternUri) { mutableStateOf<PdfPageRenderer?>(null) }
    var rendererError by remember(patternUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(patternUri) {
        if (patternUri == null) return@LaunchedEffect
        var ownedRenderer: PdfPageRenderer? = null
        try {
            val createdRenderer =
                withContext(AppDispatchers.IO) {
                    runCatching { PdfPageRenderer(context, patternUri.toUri()) }
                        .getOrNull()
                        .also { ownedRenderer = it }
                }
            if (createdRenderer == null || createdRenderer.pageCount == 0) {
                rendererError = patternOpenFailed
                return@LaunchedEffect
            }
            renderer = createdRenderer
            awaitCancellation()
        } finally {
            withContext(NonCancellable + AppDispatchers.IO) { ownedRenderer?.close() }
        }
    }

    val pageCount = renderer?.pageCount ?: 0
    val validPage = currentPage in 0 until pageCount
    val latestOnPageClamped by rememberUpdatedState(onPageClamped)
    LaunchedEffect(renderer, currentPage) {
        if (pageCount > 0 && !validPage) latestOnPageClamped(clampPatternPage(currentPage, pageCount))
    }

    var pageRenderFailed by remember(renderer, currentPage) { mutableStateOf(false) }
    val renderedBitmap by key(renderer, currentPage) {
        produceState<Bitmap?>(
            initialValue = null,
            key1 = renderer,
            key2 = currentPage,
        ) {
            value = null
            val activeRenderer = renderer ?: return@produceState
            if (!validPage) return@produceState
            var pendingBitmap: Bitmap? = null
            try {
                val bitmap =
                    withContext(AppDispatchers.IO) {
                        runCatching {
                            activeRenderer.renderPage(currentPage, 1600)
                        }.getOrNull()
                            .also { pendingBitmap = it }
                    }
                pendingBitmap = null
                pageRenderFailed = bitmap == null
                value = bitmap
                awaitDispose { bitmap?.recycle() }
            } finally {
                pendingBitmap?.recycle()
            }
        }
    }

    return PatternRenderState(
        renderer = renderer,
        rendererError = rendererError ?: patternOpenFailed.takeIf { pageRenderFailed },
        renderedBitmap = renderedBitmap,
    )
}

// Tilan ja toimintojen ryhmittely PatternViewerTopBarille (S107)
internal data class TopBarState(
    val patternName: String?,
    val totalPages: Int,
    val currentPage: Int,
    val currentRow: Int?,
    val canManageDocuments: Boolean,
    val readingLineEnabled: Boolean,
    val readingLineFollowCurrentRow: Boolean,
    val verticalReadingGuideEnabled: Boolean,
    val canManageBookmarks: Boolean,
    val hasCurrentRowMarker: Boolean,
    val hasPageRowMarkers: Boolean,
    val canAnnotate: Boolean = false,
    val annotationMode: Boolean = false,
    val annotationExportEnabled: Boolean = true,
    val countLabelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    val countCustomLabel: String? = null,
    val canUndoAnnotation: Boolean = false,
    val canRedoAnnotation: Boolean = false,
    val canClearAnnotationPage: Boolean = false,
)

internal data class TopBarActions(
    val onBack: () -> Unit,
    val onJumpToPage: (Int) -> Unit,
    val onReadingLineToggle: (Boolean) -> Unit,
    val onReadingLineFollowToggle: (Boolean) -> Unit,
    val onReturnToCurrentRow: () -> Unit,
    val onVerticalReadingGuideToggle: (Boolean) -> Unit,
    val onCenterVerticalReadingGuide: () -> Unit,
    val onOpenDocuments: () -> Unit,
    val onSaveReadingLineAsCurrentRow: () -> Unit,
    val onClearReadingLineRowMarker: () -> Unit,
    val onClearReadingLinePageMarkers: () -> Unit,
    val onStartRowCalibration: () -> Unit,
    val onOpenProjectNotes: (() -> Unit)? = null,
    val onToggleAnnotationMode: () -> Unit = {},
    val onOpenAnnotationLayers: () -> Unit = {},
    val onExportAnnotations: () -> Unit = {},
    val onUndoAnnotation: () -> Unit = {},
    val onRedoAnnotation: () -> Unit = {},
    val onClearAnnotationPage: () -> Unit = {},
)

private fun TopBarState.withAnnotations(
    canAnnotate: Boolean,
    annotationMode: AnnotationModeControls,
    annotationState: PatternAnnotationUiState,
): TopBarState =
    copy(
        canAnnotate = canAnnotate,
        annotationMode = annotationMode.active,
        annotationExportEnabled = !annotationState.isExporting,
        canUndoAnnotation = canAnnotate && annotationState.canUndo,
        canRedoAnnotation = canAnnotate && annotationState.canRedo,
        canClearAnnotationPage = canAnnotate && annotationState.editableLayerVisible && !annotationState.isSaving,
    )

private fun TopBarActions.withAnnotations(
    annotationMode: AnnotationModeControls,
    annotationViewModel: PatternAnnotationViewModel,
    exportContext: PatternPdfExportContext?,
): TopBarActions =
    copy(
        onToggleAnnotationMode = annotationMode.toggle,
        onOpenAnnotationLayers = annotationMode.openLayers,
        onUndoAnnotation = annotationViewModel::undo,
        onRedoAnnotation = annotationViewModel::redo,
        onClearAnnotationPage = annotationViewModel::clearEditablePage,
        onExportAnnotations = { exportContext?.let(annotationViewModel::requestAnnotatedPdfExport) },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatternViewerTopBar(
    state: TopBarState,
    actions: TopBarActions,
    onBlockingChanged: (Boolean) -> Unit = {},
) {
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showReadingLineMenu by rememberSaveable { mutableStateOf(false) }
    var showPageJumpDialog by rememberSaveable { mutableStateOf(false) }
    var showClearPageDialog by rememberSaveable(state.currentPage, state.patternName) { mutableStateOf(false) }
    val menuOrDialogOpen = showOverflowMenu || showReadingLineMenu || showPageJumpDialog || showClearPageDialog
    SideEffect { onBlockingChanged(menuOrDialogOpen) }
    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.patternName) {
        if (state.patternName != null) {
            withFrameNanos { }
            titleFocusRequester.requestFocus()
        }
    }

    TopAppBar(
        // Matalassa ikkunassa 48 dp:n palkki: 64 dp:n palkit veivät 400 dp:n ikkunasta kolmanneksen.
        expandedHeight =
            if (LocalAdaptiveContentSize.current.isCompactHeight) {
                PatternViewerDimens.CompactTopBarHeight
            } else {
                TopAppBarDefaults.TopAppBarExpandedHeight
            },
        title = {
            Text(
                text =
                    when {
                        state.annotationMode -> stringResource(R.string.pattern_mark_up)
                        state.patternName != null -> projectDocumentDisplayName(state.patternName)
                        else -> stringResource(R.string.pattern_viewer_title)
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.focusRequester(titleFocusRequester).focusable(),
            )
        },
        navigationIcon = {
            BackIconButton(onClick = actions.onBack)
        },
        actions = {
            if (!state.annotationMode && state.totalPages > 0) {
                Box {
                    IconButton(
                        onClick = { showReadingLineMenu = true },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reading_line),
                            contentDescription = stringResource(R.string.pattern_horizontal_reading_line),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PatternReadingOptionsMenu(
                        expanded = showReadingLineMenu,
                        close = { showReadingLineMenu = false },
                        state = state,
                        actions = actions,
                    )
                }
            }
            if (state.annotationMode) {
                IconButton(onClick = actions.onUndoAnnotation, enabled = state.canUndoAnnotation) {
                    Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.pattern_annotation_undo))
                }
                IconButton(onClick = actions.onRedoAnnotation, enabled = state.canRedoAnnotation) {
                    Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.pattern_annotation_redo))
                }
            }
            if (state.canAnnotate) {
                // Merkitse avaa merkintätilan; Valmis palaa lukutilaan. Valikko pysyy oikeassa reunassa.
                OverviewTextAction(
                    label =
                        stringResource(
                            if (state.annotationMode) R.string.pattern_mark_up_done else R.string.pattern_mark_up,
                        ),
                    onClick = actions.onToggleAnnotationMode,
                )
            }
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
                    onClearPageClick = { showClearPageDialog = true },
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

    if (showClearPageDialog && state.annotationMode) {
        ConfirmationDialog(
            title = stringResource(R.string.pattern_annotation_clear_page_title),
            message = stringResource(R.string.pattern_annotation_clear_page_body),
            confirmText = stringResource(R.string.pattern_annotation_clear_page),
            isDestructive = true,
            confirmEnabled = state.canClearAnnotationPage,
            onDismiss = { showClearPageDialog = false },
            onConfirm = {
                showClearPageDialog = false
                actions.onClearAnnotationPage()
            },
        )
    }
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
@Suppress("kotlin:S3776") // Overflow-valikko pitää viewerin ehdolliset komennot yhdessä paikassa.
private fun PatternViewerOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    state: TopBarState,
    actions: TopBarActions,
    onPageJumpClick: () -> Unit,
    onClearPageClick: () -> Unit,
    closeOverflowMenu: () -> Unit,
) {
    AdaptiveDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        if (state.totalPages > 0) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_page_jump)) },
                onClick = {
                    closeOverflowMenu()
                    onPageJumpClick()
                },
            )
        }
        if (state.annotationMode) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_annotation_clear_page)) },
                enabled = state.canClearAnnotationPage,
                onClick = {
                    closeOverflowMenu()
                    onClearPageClick()
                },
            )
        }
        if (state.canAnnotate) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_annotations_layers)) },
                onClick = {
                    closeOverflowMenu()
                    actions.onOpenAnnotationLayers()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_annotation_export_pdf)) },
                enabled = state.annotationExportEnabled,
                onClick = {
                    closeOverflowMenu()
                    actions.onExportAnnotations()
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
        actions.onOpenProjectNotes?.let { openProjectNotes ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_open_project_notes)) },
                onClick = {
                    closeOverflowMenu()
                    openProjectNotes()
                },
            )
        }
    }
}

@Composable
private fun PatternReadingOptionsMenu(
    expanded: Boolean,
    close: () -> Unit,
    state: TopBarState,
    actions: TopBarActions,
) {
    val enabledState = stringResource(R.string.pattern_guide_enabled_state)
    val disabledState = stringResource(R.string.pattern_guide_disabled_state)
    val followingState = stringResource(R.string.pattern_row_following_active)
    val pausedState = stringResource(R.string.pattern_row_following_paused)
    AdaptiveDropdownMenu(expanded = expanded, onDismissRequest = close) {
        PatternReadingLineMenuItem(
            readingLineEnabled = state.readingLineEnabled,
            onClick = {
                close()
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
                    close()
                    actions.onReadingLineFollowToggle(!state.readingLineFollowCurrentRow)
                },
            )
            if (!state.readingLineFollowCurrentRow) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pattern_return_to_current_row)) },
                    onClick = {
                        close()
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
                close()
                actions.onVerticalReadingGuideToggle(!state.verticalReadingGuideEnabled)
            },
        )
        if (state.verticalReadingGuideEnabled) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pattern_center_vertical_guide)) },
                onClick = {
                    close()
                    actions.onCenterVerticalReadingGuide()
                },
            )
        }
        PatternRowMarkerMenuItems(state = state, actions = actions, closeOverflowMenu = close)
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
    val countText =
        mainCounterCountTextInline(MainCounterCountSlot(currentRow, state.countLabelType, state.countCustomLabel))
    DropdownMenuItem(
        text = { Text(stringResource(R.string.pattern_set_row_marker_here, countText)) },
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
    // Syöte alustetaan dialogin avautuessa; lukulinjan sivunvaihto ei saa nollata kirjoitettua numeroa.
    var pageInput by rememberSaveable(totalPages) { mutableStateOf((currentPage + 1).toString()) }
    val parsedPage = pageInput.toIntOrNull()
    val isValidPage = parsedPage != null && parsedPage in 1..totalPages.coerceAtLeast(1)

    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_page_jump)) },
        text = {
            NumberInputField(
                value = pageInput,
                onValueChange = { value -> pageInput = value.filter(Char::isDigit) },
                label = stringResource(R.string.pattern_page_number),
                options = NumberInputOptions(isLast = true, preserveRawInput = true),
                supportingText =
                    stringResource(
                        R.string.pattern_page_indicator,
                        currentPage + 1,
                        totalPages.coerceAtLeast(1),
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm((parsedPage ?: 1) - 1) },
                enabled = isValidPage,
            ) {
                Text(stringResource(R.string.pattern_page_jump_go))
            }
        },
        dismissButton = {
            CancelButton(onClick = onDismiss)
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
    Surface(color = MaterialTheme.knitToolsColors.cardContainer) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = PatternViewerDimens.LibraryBottomBarHorizontalPadding,
                        vertical =
                            PatternViewerDimens.bottomBarVerticalPadding(
                                compactHeight = LocalAdaptiveContentSize.current.isCompactHeight,
                                library = true,
                            ),
                    ),
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
@Suppress("kotlin:S3776") // Viewer-sisältö kokoaa renderöinti-, opas- ja annotaatiotilat yhteen pintaan.
private fun PatternViewerContent(
    stateProvider: @Composable () -> PatternViewerContentState,
    actions: PatternViewerContentActions,
    modifier: Modifier = Modifier,
) {
    val state = stateProvider()
    val annotationActions = actions.annotationActions
    val renderedImage = remember(state.renderedBitmap) { state.renderedBitmap?.asImageBitmap() }
    val exportStyle = rememberPatternAnnotationRenderStyle()
    val exportRequests = annotationActions.exportDestinationRequestsProvider()
    val activity = LocalActivity.current
    SideEffect { annotationActions.onExportContextChanged(state.exportContext) }
    DisposableEffect(exportRequests, activity) {
        onDispose {
            if (activity?.isChangingConfigurations != true) annotationActions.onExportContextChanged(null)
        }
    }
    var pendingExportRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    val annotationPageMatches = state.annotationState.matchesDisplayedPage(state.exportContext, state.currentPage)
    val editableLayerVisible = annotationPageMatches && state.annotationState.editableLayerVisible
    val fallbackPatternName = stringResource(R.string.pattern_annotation_export_default_name)
    val exportBaseName =
        annotatedPdfExportBaseName(
            patternName = state.patternName,
            photoFallback = stringResource(R.string.pattern_images_document_fallback),
            defaultName = fallbackPatternName,
        )
    val exportFilename = stringResource(R.string.pattern_annotation_export_filename, exportBaseName)
    val requestId = pendingExportRequestId
    key(requestId) {
        var destinationLaunched by rememberSaveable { mutableStateOf(false) }
        val exportLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/pdf"),
            ) { destination ->
                if (pendingExportRequestId == requestId) pendingExportRequestId = null
                if (requestId != null) {
                    annotationActions.onExport(requestId, state.exportContext, destination, exportStyle)
                }
            }
        LaunchedEffect(requestId) {
            if (requestId != null && !destinationLaunched) {
                if (annotationActions.onLaunchExportDestination(requestId, state.exportContext)) {
                    destinationLaunched = true
                    try {
                        exportLauncher.launch(exportFilename)
                    } catch (_: ActivityNotFoundException) {
                        if (pendingExportRequestId == requestId) pendingExportRequestId = null
                        annotationActions.onExport(requestId, state.exportContext, null, exportStyle)
                    }
                } else {
                    if (pendingExportRequestId == requestId) pendingExportRequestId = null
                    annotationActions.onExport(requestId, state.exportContext, null, exportStyle)
                }
            }
        }
    }
    CollectWithLifecycleEffect({ exportRequests }) { requestId ->
        if (pendingExportRequestId == null) {
            pendingExportRequestId = requestId
        } else if (pendingExportRequestId != requestId) {
            annotationActions.onExport(requestId, state.exportContext, null, exportStyle)
        }
    }
    val viewportFocusRequester = remember { FocusRequester() }
    val viewportStates = rememberSaveableStateHolder()
    BoxWithConstraints(modifier = modifier) {
        val toolsMaxHeight =
            if (state.annotationState.activeTool == PatternAnnotationTool.HIGHLIGHTER) {
                val windowHeight =
                    with(LocalDensity.current) {
                        LocalWindowInfo.current.containerSize.height
                            .toDp()
                    }
                windowHeight * PatternViewerDimens.HIGHLIGHTER_PALETTE_WINDOW_FRACTION
            } else {
                maxHeight / 2
            }
        Column(modifier = Modifier.fillMaxSize()) {
            // Lukutilassa ohje täyttää näytön; työkalut näkyvät vain merkintätilassa.
            if (state.patternUri != null && state.annotationMode && annotationPageMatches) {
                Column(
                    modifier = Modifier.heightIn(max = toolsMaxHeight).verticalScroll(rememberScrollState()),
                ) {
                    PatternAnnotationToolbar(
                        state = state.annotationState,
                        actions = annotationActions.annotationToolbarActions,
                    )
                    if (state.annotationState.loadError == PatternAnnotationLoadError.PAGE_LIMIT) {
                        Text(
                            text = stringResource(R.string.pattern_annotation_page_limit),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
            if (state.annotationState.writeError == PatternAnnotationWriteError.WRITE_FAILED &&
                (!state.annotationMode || state.annotationState.textEdit == null)
            ) {
                Snackbar(
                    action = {
                        Row {
                            if (state.annotationState.canRetryStroke) {
                                TextButton(
                                    onClick = annotationActions.onRetryStroke,
                                    enabled = editableLayerVisible && !state.annotationState.isSaving,
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                            TextButton(onClick = annotationActions.onDismissWriteError) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    },
                    actionOnNewLine = true,
                ) {
                    Text(stringResource(R.string.pattern_annotation_command_failed))
                }
            }
            // Vienti käynnistyy valikosta, joten edistyminen ja virhe näkyvät tilasta riippumatta.
            if (state.annotationState.isExporting) {
                Text(
                    text =
                        stringResource(
                            R.string.pattern_annotation_export_progress,
                            state.annotationState.exportCompletedPages,
                            state.annotationState.exportTotalPages,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            if (state.annotationState.exportFailed) {
                Text(
                    text = stringResource(R.string.pattern_annotation_export_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            when {
                state.rendererError != null -> {
                    PatternViewerMessage(
                        message = state.rendererError.ifBlank { stringResource(R.string.pattern_open_failed) },
                    )
                }

                state.patternUri == null -> {
                    PatternViewerMessage(message = stringResource(R.string.no_pattern_attached))
                }

                renderedImage == null -> {
                    PatternViewerMessage(message = stringResource(R.string.pattern_loading))
                }

                else -> {
                    viewportStates.SaveableStateProvider("${state.patternUri}:${state.currentPage}") {
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
                                PatternAnnotationOverlay(
                                    masterAnnotations = state.annotationState.masterAnnotations,
                                    projectAnnotations = state.annotationState.projectAnnotations,
                                    masterVisible = annotationPageMatches && state.annotationState.masterLayerVisible,
                                    projectVisible = annotationPageMatches && state.annotationState.projectLayerVisible,
                                    inProgressAnnotation = state.annotationState.inProgressAnnotation,
                                    inProgressVisible = editableLayerVisible,
                                    selectedAnnotationId = state.annotationState.selectedAnnotationId,
                                    trackerHighlights = state.annotationState.trackerHighlights,
                                    modifier =
                                        Modifier.fillMaxSize().onSizeChanged(
                                            annotationActions.onPageSizeChanged,
                                        ),
                                )
                                if (state.readingLineEnabled) {
                                    ReadingLineOverlay(
                                        yFraction = state.readingLineYFraction,
                                        currentRow = state.countSlot?.count,
                                        countLabelType = state.countSlot?.labelType ?: MainCounterLabelType.ROWS,
                                        countCustomLabel = state.countSlot?.customLabel,
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
                                // Piirto vain merkintätilassa, vaikka edellinen työkalu olisi vielä valittuna.
                                if (state.annotationMode &&
                                    editableLayerVisible &&
                                    state.annotationState.editableLayerId != null &&
                                    state.annotationState.activeTool != PatternAnnotationTool.BROWSE
                                ) {
                                    PatternAnnotationInputOverlay(
                                        activeTool = state.annotationState.activeTool,
                                        coordinateTransform = viewport.coordinateTransform,
                                        viewportScale = viewport.state.scale,
                                        pressureEnabled = state.annotationState.pressureEnabled,
                                        actions = annotationActions.annotationInputActions,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun PatternAnnotationUiState.matchesDisplayedPage(
    context: PatternPdfExportContext?,
    page: Int,
): Boolean =
    context != null && owner == context.owner && projectDocumentId == context.projectDocumentId && currentPage == page

private data class PatternViewerContentState(
    val patternUri: String?,
    val rendererError: String?,
    val renderedBitmap: Bitmap?,
    val patternName: String?,
    val countSlot: MainCounterCountSlot?,
    val readingLineEnabled: Boolean,
    val readingLineYFraction: Float,
    val readingLineFollowCurrentRow: Boolean?,
    val verticalReadingGuideEnabled: Boolean,
    val verticalReadingGuideXFraction: Float,
    val currentPage: Int,
    val viewportFocusRequest: PatternViewportFocusRequest?,
    val annotationState: PatternAnnotationUiState,
    val exportContext: PatternPdfExportContext?,
    val annotationMode: Boolean = false,
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
    val annotationActions: PatternViewerAnnotationActions,
)

private data class PatternViewerAnnotationActions(
    val onDismissWriteError: () -> Unit,
    val onRetryStroke: () -> Unit,
    val onMasterLayerVisibilityChange: (Boolean) -> Unit,
    val onProjectLayerVisibilityChange: (Boolean) -> Unit,
    val onPageSizeChanged: (IntSize) -> Unit,
    val annotationInputActions: PatternAnnotationInputActions,
    val annotationToolbarActions: PatternAnnotationToolbarActions,
    val exportDestinationRequestsProvider: () -> Flow<String>,
    val onExportContextChanged: (PatternPdfExportContext?) -> Unit,
    val onLaunchExportDestination: (String, PatternPdfExportContext?) -> Boolean,
    val onExport: (String, PatternPdfExportContext?, Uri?, PatternAnnotationRenderStyle) -> Unit,
)

private fun PatternAnnotationViewModel.patternContentAnnotationActions() =
    PatternViewerAnnotationActions(
        onDismissWriteError = ::dismissWriteError,
        onRetryStroke = ::retryFailedStroke,
        onMasterLayerVisibilityChange = ::setMasterLayerVisible,
        onProjectLayerVisibilityChange = ::setProjectLayerVisible,
        onPageSizeChanged = ::setAnnotationPageSize,
        annotationInputActions = patternInputActions(),
        annotationToolbarActions = patternToolbarActions(),
        exportDestinationRequestsProvider = { exportDestinationRequests },
        onExportContextChanged = ::setExportContext,
        onLaunchExportDestination = ::launchExportDestination,
        onExport = ::exportAnnotatedPdf,
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

internal fun PatternAnnotationViewModel.patternToolbarActions() =
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
        onAddTextBox = { text, draftId -> addTextBox(text, draftId = draftId) },
        onAddCallout = {
            title,
            description,
            symbol,
            draftId,
            ->
            addCallout(title, description, symbol, draftId = draftId)
        },
        onAddChartTracker = ::addChartTrackerFromSelected,
        onEditText = ::editSelectedText,
        onCancelTextEdit = ::cancelTextEdit,
        onSaveTextEdit = ::saveTextEdit,
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
    countLabelType: MainCounterLabelType = MainCounterLabelType.ROWS,
    countCustomLabel: String? = null,
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
        val handleSize = PatternViewerDimens.ReadingLineHandleSize
        val bandHeight =
            (maxHeight * READING_LINE_BAND_HEIGHT_FRACTION)
                .coerceAtLeast(PatternViewerDimens.ReadingLineMinimumBandHeight)
        val maxHandleOffset = (maxHeight - handleSize).coerceAtLeast(0.dp)
        val handleOffset = (maxHeight * sanitizedYFraction - (handleSize / 2f)).coerceIn(0.dp, maxHandleOffset)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height * sanitizedYFraction
            val bandHeightPx = bandHeight.toPx()
            drawRect(
                color = bandColor,
                topLeft = Offset(0f, centerY - (bandHeightPx / 2f)),
                size = Size(size.width, bandHeightPx),
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
            drawCircle(color = lineColor, radius = PatternViewerDimens.ReadingLineHandleRadius.toPx(), center = center)
        }
        currentRow?.let { currentRow ->
            ReadingLineRowLabel(
                countSlot = MainCounterCountSlot(currentRow, countLabelType, countCustomLabel),
                bandCenter = maxHeight * sanitizedYFraction,
                bandHeight = bandHeight,
                containerHeight = maxHeight,
                modifier = Modifier.align(Alignment.TopEnd),
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
                    .align(AbsoluteAlignment.BottomLeft)
                    .absoluteOffset(x = handleOffset)
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

@Composable
private fun ReadingLineRowLabel(
    countSlot: MainCounterCountSlot,
    bandCenter: Dp,
    bandHeight: Dp,
    containerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val gap = PatternViewerDimens.ReadingLineLabelGap
    val density = LocalDensity.current
    var measuredLabelHeight by remember { mutableIntStateOf(0) }
    val labelHeight = with(density) { measuredLabelHeight.toDp() }
    val above = bandCenter - bandHeight / 2f - gap - labelHeight
    val below = bandCenter + bandHeight / 2f + gap
    val labelOffset =
        when {
            above >= 0.dp -> above
            below + labelHeight <= containerHeight -> below
            else -> null
        }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        modifier =
            modifier
                .padding(
                    end =
                        PatternViewerDimens.ReadingLineHandleSize / 2f +
                            PatternViewerDimens.ReadingLineHandleRadius + gap,
                ).offset(y = labelOffset ?: 0.dp)
                .alpha(if (measuredLabelHeight > 0 && labelOffset != null) 1f else 0f)
                .onSizeChanged { measuredLabelHeight = it.height },
    ) {
        Text(
            text = mainCounterCountText(countSlot),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Vientinimen pohja: vain .pdf-pääte poistetaan, joten "Huivi v1.2" ei lyhene muotoon "Huivi v1". */
internal fun annotatedPdfExportBaseName(
    patternName: String?,
    photoFallback: String,
    defaultName: String,
): String = patternName?.let { PatternDisplayNames.forDisplay(it, photoFallback) }?.ifBlank { null } ?: defaultName

/** Kenttään esitäytetään siistitty nimi; muuttamaton tallennus ei korvaa alkuperäistä nimikettä. */
internal fun isDocumentRenameChange(
    input: String,
    prefilledLabel: String,
): Boolean = input.trim() != prefilledLabel.trim()

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
