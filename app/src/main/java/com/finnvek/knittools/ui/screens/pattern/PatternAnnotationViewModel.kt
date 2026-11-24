package com.finnvek.knittools.ui.screens.pattern

import android.net.Uri
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.local.PatternAnnotationTextConflict
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATIONS
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_TRACKER_CELLS
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
import com.finnvek.knittools.data.storage.PatternPdfExportBudget
import com.finnvek.knittools.data.storage.PatternPdfExportLimitException
import com.finnvek.knittools.data.storage.PatternPdfExportLimitReason
import com.finnvek.knittools.data.storage.PatternPdfExportProgress
import com.finnvek.knittools.data.storage.PatternPdfExporter
import com.finnvek.knittools.data.storage.withEditableLegacyStroke
import com.finnvek.knittools.domain.calculator.ChartTrackerHighlight
import com.finnvek.knittools.domain.calculator.resolveChartTrackerHighlight
import com.finnvek.knittools.domain.calculator.scalePatternAnnotation
import com.finnvek.knittools.domain.calculator.simplifyFreehandPoints
import com.finnvek.knittools.domain.calculator.topmostAnnotationAt
import com.finnvek.knittools.domain.calculator.translatePatternAnnotation
import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCorner
import com.finnvek.knittools.domain.model.ChartCounterType
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.ChartTrackingMode
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationLimits
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.PatternAnnotationPayload
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.retryOnRepositoryReadFailure
import com.finnvek.knittools.ui.theme.PatternAnnotationTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

enum class PatternAnnotationLoadError {
    NONE,
    READ_FAILED,
    PAGE_LIMIT,
}

enum class PatternAnnotationWriteError {
    NONE,
    WRITE_FAILED,
    TEXT_CONFLICT,
}

data class PatternStrokeDraft(
    val tool: PatternAnnotationTool,
    val points: List<NormalizedPatternPoint>,
    val argb: Int,
    val strokeWidth: Float,
    val pressureEnabled: Boolean,
)

data class PatternChartCounterOption(
    val type: ChartCounterType,
    val extraCounterId: Long? = null,
    val name: String,
    val value: Int,
)

data class PatternChartTrackerDraft(
    val rows: Int,
    val columns: Int,
    val rowDirection: ChartRowDirection,
    val columnDirection: ChartColumnDirection,
    val trackingMode: ChartTrackingMode,
    val counter: PatternChartCounterOption,
    val gridStartIndex: Int,
    val wrapAtEnd: Boolean,
    val c2cOrigin: ChartCorner,
)

data class PatternAnnotationTextEdit(
    val owner: PatternAnnotationOwner,
    val annotation: PatternAnnotation,
)

data class PatternPdfExportContext(
    val sourceUri: Uri,
    val owner: PatternAnnotationOwner,
    val projectDocumentId: Long? = null,
)

private data class PatternPdfExportRequest(
    val id: String = UUID.randomUUID().toString(),
    val context: PatternPdfExportContext,
    val layerIds: List<Long>,
    val generation: Long,
)

private enum class PatternPdfExportPhase {
    PREFLIGHT,
    READY,
    DESTINATION,
    EXPORTING,
}

data class PatternAnnotationUiState(
    val owner: PatternAnnotationOwner,
    val currentPage: Int = 0,
    val masterLayerVisible: Boolean = true,
    val projectLayerVisible: Boolean = true,
    val masterAnnotations: List<PatternAnnotation> = emptyList(),
    val projectAnnotations: List<PatternAnnotation> = emptyList(),
    val editableLayerId: Long? = null,
    val loadError: PatternAnnotationLoadError = PatternAnnotationLoadError.NONE,
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
    val activeTool: PatternAnnotationTool = PatternAnnotationTool.BROWSE,
    val penArgb: Int = PatternAnnotationTokens.PEN_DEFAULT_ARGB,
    val penStrokeWidth: Float = PatternAnnotationTokens.PEN_DEFAULT_WIDTH,
    val pressureEnabled: Boolean = true,
    val highlighterArgb: Int = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ARGB,
    val highlighterStrokeWidth: Float = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_WIDTH,
    val highlighterAxisLock: PatternHighlighterAxisLock = PatternHighlighterAxisLock.FREE,
    val draftStroke: PatternStrokeDraft? = null,
    val inProgressAnnotation: PatternAnnotation? = null,
    // CPD-ON
    val isSaving: Boolean = false,
    val writeError: PatternAnnotationWriteError = PatternAnnotationWriteError.NONE,
    val canRetryStroke: Boolean = false,
    val selectedAnnotationId: Long? = null,
    val selectedAnnotationIsEditable: Boolean = false,
    val selectedAnnotationIsText: Boolean = false,
    val textEdit: PatternAnnotationTextEdit? = null,
    val savedDraftId: String? = null,
    val selectedAnnotationSupportsChartTracker: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val chartCounterOptions: List<PatternChartCounterOption> = emptyList(),
    val trackerHighlights: Map<Long, ChartTrackerHighlight> = emptyMap(),
    val masterLayerId: Long? = null,
    val projectLayerId: Long? = null,
    val projectDocumentId: Long? = null,
    val isExporting: Boolean = false,
    val exportCompletedPages: Int = 0,
    val exportTotalPages: Int = 0,
    val exportFailed: Boolean = false,
)

internal val PatternAnnotationUiState.editableLayerVisible: Boolean
    get() =
        when (owner) {
            is PatternAnnotationOwner.Project -> projectLayerVisible
            is PatternAnnotationOwner.SavedPattern -> masterLayerVisible
        }

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
// Reittikohtainen ViewModel tarjoaa Compose-kerrokselle jokaisen editorin käyttäjäaikeen erillisenä metodina.
@Suppress("TooManyFunctions", "LargeClass")
class PatternAnnotationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val counterRepository: CounterRepository,
        private val layerRepository: PatternAnnotationLayerRepository,
        private val annotationRepository: PatternAnnotationRepository,
        private val projectDocumentRepository: ProjectDocumentRepository,
        private val projectCounterRepository: ProjectCounterRepository? = null,
        private val pdfExporter: PatternPdfExporter? = null,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        private val routeOwner = savedStateHandle.requirePatternAnnotationOwner()
        private val currentPage = MutableStateFlow(0)
        private var annotationPageSize = IntSize.Zero
        private val masterLayerVisible = MutableStateFlow(true)
        private val projectLayerVisible = MutableStateFlow(true)
        private val layerReadFailed = MutableStateFlow(false)
        private val annotationReadFailed = MutableStateFlow(false)
        private val interaction = MutableStateFlow(PatternAnnotationInteractionState())
        private val exportDestinationChannel = Channel<String>(Channel.BUFFERED)
        val exportDestinationRequests: Flow<String> = exportDestinationChannel.receiveAsFlow()
        private var exportContext: PatternPdfExportContext? = null
        private var exportGeneration = 0L
        private var exportSelection: AnnotationLayerSelection? = null
        private var exportRequest: PatternPdfExportRequest? = null
        private var exportPhase = PatternPdfExportPhase.PREFLIGHT
        private var exportJob: Job? = null
        private var editContext: AnnotationEditContext? = null
        private var editContextGeneration = 0L
        private val pendingStrokes = ArrayDeque<PendingAnnotationStroke>()
        private var savingStroke: PendingAnnotationStroke? = null
        private val counterContext =
            createCounterContextFlow(routeOwner).stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatternCounterContext(),
            )
        private val loadError =
            combine(layerReadFailed, annotationReadFailed) { layerFailed, annotationFailed ->
                if (layerFailed || annotationFailed) {
                    PatternAnnotationLoadError.READ_FAILED
                } else {
                    PatternAnnotationLoadError.NONE
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, PatternAnnotationLoadError.NONE)

        private val layers =
            createLayerFlow(routeOwner)
                .withReadRecovery(layerReadFailed)
                .onEach(::handleLayerSelection)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = AnnotationLayerSelection(owner = routeOwner),
                )

        private val pageAnnotations =
            combine(layers, currentPage) { selection, page -> selection to page }
                .flatMapLatest { (selection, page) ->
                    combine(
                        observePage(selection.masterLayer, page),
                        observePage(selection.projectLayer, page),
                    ) { master, project ->
                        PageAnnotations(page = page, selection = selection, master = master, project = project)
                    }.catch { failure ->
                        if (failure !is PatternAnnotationPageLimitException) throw failure
                        emit(PageAnnotations(page = page, selection = selection, limitExceeded = true))
                    }.onStart { emit(PageAnnotations()) }
                }.onEach { annotations ->
                    interaction.update { state ->
                        val edit = state.textEdit
                        if (edit != null &&
                            (annotations.master + annotations.project).none { it.id == edit.annotation.id }
                        ) {
                            state.copy(textEdit = null)
                        } else {
                            state
                        }
                    }
                }.withReadRecovery(annotationReadFailed)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = PageAnnotations(),
                )

        private val layerVisibility =
            combine(masterLayerVisible, projectLayerVisible) { master, project ->
                LayerVisibility(master = master, project = project)
            }

        private val trackerHighlights =
            combine(pageAnnotations, counterContext, layerVisibility) { annotations, counters, visibility ->
                resolveTrackerHighlights(
                    visiblePatternAnnotations(
                        annotations.master,
                        annotations.project,
                        visibility.master,
                        visibility.project,
                    ),
                    counters,
                )
            }

        private val annotationFeedback =
            combine(
                loadError,
                interaction,
                counterContext,
                trackerHighlights,
            ) { readError, interactionState, counters, highlights ->
                PatternAnnotationFeedback(readError, interactionState, counters, highlights)
            }

        val uiState: StateFlow<PatternAnnotationUiState> =
            combine(layers, pageAnnotations, currentPage, layerVisibility, annotationFeedback) {
                selection,
                observedAnnotations,
                page,
                visibility,
                feedback,
                ->
                val annotations =
                    observedAnnotations.takeIf { it.page == page && it.selection == selection } ?: PageAnnotations()
                val topZIndex = annotations.topZIndex
                val editableVisible =
                    when (selection.owner) {
                        is PatternAnnotationOwner.Project -> visibility.project
                        is PatternAnnotationOwner.SavedPattern -> visibility.master
                    }
                val selectedAnnotation =
                    feedback.interaction.selectedAnnotationId?.let { selectedId ->
                        annotations.master.firstOrNull { it.id == selectedId }
                            ?: annotations.project.firstOrNull { it.id == selectedId }
                    }
                PatternAnnotationUiState(
                    owner = selection.owner,
                    currentPage = page,
                    masterLayerVisible = visibility.master,
                    projectLayerVisible = visibility.project,
                    masterAnnotations = annotations.master,
                    projectAnnotations = annotations.project,
                    editableLayerId = selection.editableLayerId.takeUnless { annotations.limitExceeded },
                    masterLayerId = selection.masterLayer?.id,
                    projectLayerId = selection.projectLayer?.id,
                    projectDocumentId = selection.projectDocumentId,
                    loadError =
                        if (annotations.limitExceeded) PatternAnnotationLoadError.PAGE_LIMIT else feedback.loadError,
                    activeTool = feedback.interaction.activeTool,
                    penArgb = feedback.interaction.penArgb,
                    penStrokeWidth = feedback.interaction.penStrokeWidth,
                    pressureEnabled =
                        feedback.interaction.activeTool == PatternAnnotationTool.PEN &&
                            feedback.interaction.pressureEnabled,
                    highlighterArgb = feedback.interaction.highlighterArgb,
                    highlighterStrokeWidth = feedback.interaction.highlighterStrokeWidth,
                    highlighterAxisLock = feedback.interaction.highlighterAxisLock,
                    draftStroke = feedback.interaction.draftStroke,
                    inProgressAnnotation =
                        feedback.interaction.draftStroke
                            ?.takeUnless {
                                annotations.limitExceeded ||
                                    feedback.interaction.failedStrokes.any { stroke -> stroke.draft === it }
                            }?.withHighlighterAxisLock(feedback.interaction.highlighterAxisLock)
                            ?.toAnnotation(
                                layerId = selection.editableLayerId,
                                page = page,
                                zIndex = topZIndex,
                            ),
                    isSaving = feedback.interaction.isSaving,
                    writeError =
                        if (feedback.interaction.writeError == PatternAnnotationWriteError.NONE &&
                            feedback.interaction.failedStrokes.isNotEmpty()
                        ) {
                            PatternAnnotationWriteError.WRITE_FAILED
                        } else {
                            feedback.interaction.writeError
                        },
                    canRetryStroke = feedback.interaction.failedStrokes.isNotEmpty(),
                    selectedAnnotationId = feedback.interaction.selectedAnnotationId,
                    selectedAnnotationIsText = selectedAnnotation?.payload is TextBoxPayload,
                    textEdit = feedback.interaction.textEdit,
                    savedDraftId = feedback.interaction.savedDraftId,
                    selectedAnnotationIsEditable =
                        selectedAnnotation != null &&
                            selectedAnnotation.layerId == selection.editableLayerId &&
                            editableVisible,
                    selectedAnnotationSupportsChartTracker =
                        selectedAnnotation?.payload is ChartRegionPayload ||
                            selectedAnnotation?.payload is ChartTrackerPayload,
                    canUndo = editableVisible && feedback.interaction.undoStack.isNotEmpty(),
                    canRedo = editableVisible && feedback.interaction.redoStack.isNotEmpty(),
                    chartCounterOptions = feedback.counterContext.options,
                    trackerHighlights = feedback.trackerHighlights,
                    isExporting = feedback.interaction.isExporting,
                    exportCompletedPages = feedback.interaction.exportCompletedPages,
                    exportTotalPages = feedback.interaction.exportTotalPages,
                    exportFailed = feedback.interaction.exportFailed,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatternAnnotationUiState(owner = routeOwner),
            )

        fun setCurrentPage(page: Int) {
            require(page >= 0) { "Pattern annotation page must be non-negative" }
            if (page == currentPage.value) return
            interaction.update { state -> state.copy(draftStroke = null, selectedAnnotationId = null, textEdit = null) }
            currentPage.value = page
        }

        fun setMasterLayerVisible(visible: Boolean) {
            masterLayerVisible.value = visible
            if (!visible && layers.value.owner is PatternAnnotationOwner.SavedPattern) discardHiddenLayerDraft()
        }

        fun setProjectLayerVisible(visible: Boolean) {
            projectLayerVisible.value = visible
            if (!visible && layers.value.owner is PatternAnnotationOwner.Project) discardHiddenLayerDraft()
        }

        private val isEditableLayerVisible: Boolean
            get() =
                when (layers.value.owner) {
                    is PatternAnnotationOwner.Project -> projectLayerVisible.value
                    is PatternAnnotationOwner.SavedPattern -> masterLayerVisible.value
                }

        private fun discardHiddenLayerDraft() {
            interaction.update { it.copy(draftStroke = null, selectedAnnotationId = null, textEdit = null) }
        }

        fun setActiveTool(tool: PatternAnnotationTool) {
            if (interaction.value.activeTool != tool) {
                analytics.track(
                    UsageEvent.ANNOTATION_TOOL_SELECTED,
                    tool.analyticsDetail(),
                )
            }
            if (tool != interaction.value.activeTool && interaction.value.draftStroke != null) cancelStroke()
            interaction.update { it.copy(activeTool = tool) }
        }

        fun dismissWriteError() {
            interaction.update { state ->
                state.copy(
                    writeError = PatternAnnotationWriteError.NONE,
                    draftStroke =
                        state.draftStroke.takeUnless { draft ->
                            state.failedStrokes.any { it.draft === draft }
                        },
                    failedStrokes = emptyList(),
                )
            }
        }

        fun retryFailedStroke() {
            interaction.value.failedStrokes
                .firstOrNull()
                ?.let(::retryStroke)
        }

        private fun retryStroke(stroke: PendingAnnotationStroke) {
            if (!isEditableLayerVisible || interaction.value.isSaving || currentEditContext() != stroke.context) return
            interaction.update { state ->
                state.copy(
                    failedStrokes = state.failedStrokes - stroke,
                    writeError = PatternAnnotationWriteError.NONE,
                )
            }
            pendingStrokes.addFirst(stroke)
            persistNextStroke()
        }

        fun setHighlighterAxisLock(axisLock: PatternHighlighterAxisLock) {
            interaction.update { it.copy(highlighterAxisLock = axisLock) }
        }

        fun setPenArgb(argb: Int) {
            interaction.update { it.copy(penArgb = argb) }
        }

        fun setHighlighterArgb(argb: Int) {
            interaction.update { it.copy(highlighterArgb = argb) }
        }

        fun setPenStrokeWidth(width: Float) {
            interaction.update { it.copy(penStrokeWidth = width.coerceToStrokeWidthLimits()) }
        }

        fun setHighlighterStrokeWidth(width: Float) {
            interaction.update { it.copy(highlighterStrokeWidth = width.coerceToStrokeWidthLimits()) }
        }

        fun setPressureEnabled(enabled: Boolean) {
            interaction.update { it.copy(pressureEnabled = enabled) }
        }

        fun beginStroke(point: NormalizedPatternPoint) {
            if (!isEditableLayerVisible || uiState.value.loadError == PatternAnnotationLoadError.PAGE_LIMIT) return
            val state = interaction.value
            val style = state.styleForTool() ?: return
            val pressureEnabled = state.activeTool == PatternAnnotationTool.PEN && state.pressureEnabled
            interaction.value =
                state.copy(
                    draftStroke =
                        PatternStrokeDraft(
                            tool = state.activeTool,
                            points = listOf(if (pressureEnabled) point else point.copy(pressure = 1f)),
                            argb = style.argb,
                            strokeWidth = style.strokeWidth,
                            pressureEnabled = pressureEnabled,
                        ),
                    writeError = PatternAnnotationWriteError.NONE,
                )
        }

        fun appendStrokePoint(point: NormalizedPatternPoint) {
            interaction.update { state ->
                val draft = state.draftStroke ?: return@update state
                val draftPoint = if (draft.pressureEnabled) point else point.copy(pressure = 1f)
                if (draft.tool != PatternAnnotationTool.PEN && draft.tool != PatternAnnotationTool.HIGHLIGHTER) {
                    return@update state.copy(
                        draftStroke = draft.copy(points = listOf(draft.points.first(), draftPoint)),
                    )
                }
                val points = draft.points + draftPoint
                val boundedPoints =
                    if (points.size <= PatternAnnotationLimits.MAX_FREEHAND_POINTS) {
                        points
                    } else {
                        // Keep samples from the full stroke, including the newly reached endpoint.
                        points.filterIndexed { index, _ -> index % 2 == 0 }
                    }
                state.copy(draftStroke = draft.copy(points = boundedPoints))
            }
        }

        fun commitStroke(simplificationTolerance: Float) {
            val interactionState = interaction.value
            val originalDraft = interactionState.draftStroke ?: return
            val commandContext = currentEditContext()
            val layerId = commandContext.editableLayerId
            val strokeAlreadyQueued =
                savingStroke?.draft === originalDraft ||
                    pendingStrokes.any { it.draft === originalDraft }
            if (
                layerId == null ||
                !isEditableLayerVisible ||
                uiState.value.loadError == PatternAnnotationLoadError.PAGE_LIMIT ||
                strokeAlreadyQueued
            ) {
                return
            }
            interactionState.failedStrokes.firstOrNull { it.draft === originalDraft }?.let { stroke ->
                retryStroke(stroke)
                return
            }
            val lockedPoints = originalDraft.withHighlighterAxisLock(interactionState.highlighterAxisLock).points
            val simplifiedPoints = simplifyFreehandPoints(lockedPoints, simplificationTolerance)
            if (simplifiedPoints.isEmpty()) return
            val annotation =
                originalDraft.copy(points = simplifiedPoints).toAnnotation(
                    layerId = layerId,
                    page = currentPage.value,
                    zIndex = nextEditableZIndex(),
                ) ?: return
            if (PatternAnnotationPayloadCodec.encode(annotation.kind, annotation.payload) == null) {
                interaction.update { state ->
                    if (state.draftStroke === originalDraft) state.copy(draftStroke = null) else state
                }
            } else {
                pendingStrokes.addLast(PendingAnnotationStroke(originalDraft, annotation, commandContext))
                persistNextStroke()
            }
        }

        private fun persistNextStroke() {
            if (interaction.value.isSaving) return
            val stroke = pendingStrokes.removeFirstOrNull() ?: return
            savingStroke = stroke
            interaction.update { it.copy(isSaving = true) }
            persistCommand(PatternAnnotationCommand.Insert(stroke.annotation), stroke.context) { state ->
                state.copy(draftStroke = state.draftStroke.takeUnless { it === stroke.draft })
            }
        }

        fun cancelStroke() {
            interaction.update { state ->
                val draft = state.draftStroke
                if (draft === savingStroke?.draft || pendingStrokes.any { it.draft === draft }) {
                    state
                } else {
                    state.copy(draftStroke = null)
                }
            }
        }

        fun eraseStrokeAt(
            point: NormalizedPatternPoint,
            tolerance: Float = PatternAnnotationTokens.ERASER_HIT_TOLERANCE,
        ) {
            if (uiState.value.loadError == PatternAnnotationLoadError.PAGE_LIMIT || interaction.value.isSaving) return
            val annotations = editableAnnotations()
            val hitId = topmostEditableGeometryAt(annotations, point, tolerance)?.id ?: return
            val hit = annotations.first { it.id == hitId }
            executeCommand(PatternAnnotationCommand.Delete(hit))
        }

        fun selectAnnotationAt(
            point: NormalizedPatternPoint,
            tolerance: Float = PatternAnnotationTokens.SELECTION_HIT_TOLERANCE,
        ) {
            val state = uiState.value
            val projectSelection =
                if (state.projectLayerVisible) {
                    topmostEditableGeometryAt(state.projectAnnotations, point, tolerance)
                } else {
                    null
                }
            val selected =
                projectSelection
                    ?: if (state.masterLayerVisible) {
                        topmostEditableGeometryAt(state.masterAnnotations, point, tolerance)
                    } else {
                        null
                    }
            interaction.update { it.copy(selectedAnnotationId = selected?.id) }
        }

        internal fun setAnnotationPageSize(size: IntSize) {
            annotationPageSize = size
        }

        private fun editableGeometry(annotation: PatternAnnotation): PatternAnnotation =
            annotation.withEditableLegacyStroke(annotationPageSize.width.toFloat(), annotationPageSize.height.toFloat())

        private fun topmostEditableGeometryAt(
            annotations: List<PatternAnnotation>,
            point: NormalizedPatternPoint,
            tolerance: Float,
        ): PatternAnnotation? = topmostAnnotationAt(annotations.map(::editableGeometry), point, tolerance)

        fun addChartTrackerFromSelected(
            draft: PatternChartTrackerDraft,
            draftId: String? = null,
        ) {
            val selectedId = interaction.value.selectedAnnotationId ?: return
            val selected =
                (uiState.value.masterAnnotations + uiState.value.projectAnnotations)
                    .firstOrNull { it.id == selectedId } ?: return
            val sourceRegion =
                when (val selectedPayload = selected.payload) {
                    is ChartRegionPayload -> selectedPayload
                    is ChartTrackerPayload -> selectedPayload.region
                    else -> return
                }
            if (!PatternAnnotationLimits.isValidChartDimensions(draft.rows, draft.columns)) return
            val region =
                sourceRegion.copy(
                    rows = draft.rows,
                    columns = draft.columns,
                    rowDirection = draft.rowDirection,
                    columnDirection = draft.columnDirection,
                )
            insertPayload(
                kind = PatternAnnotationKind.CHART_TRACKER,
                payload =
                    ChartTrackerPayload(
                        region = region,
                        trackingMode = draft.trackingMode,
                        counterType = draft.counter.type,
                        extraCounterId = draft.counter.extraCounterId,
                        counterStartValue = draft.counter.value,
                        gridStartIndex = draft.gridStartIndex,
                        wrapAtEnd = draft.wrapAtEnd,
                        highlightArgb = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ARGB,
                        highlightAlpha = PatternAnnotationTokens.CHART_HIGHLIGHT_ALPHA,
                        c2cOrigin = draft.c2cOrigin,
                    ),
                draftId = draftId,
            )
        }

        fun moveSelected(
            deltaX: Float,
            deltaY: Float,
        ) {
            updateSelected { annotation -> translatePatternAnnotation(editableGeometry(annotation), deltaX, deltaY) }
        }

        fun resizeSelected(scale: Float) {
            updateSelected { annotation -> scalePatternAnnotation(editableGeometry(annotation), scale) }
        }

        fun duplicateSelected() {
            val selected = selectedAnnotation() ?: return
            val duplicate =
                translatePatternAnnotation(editableGeometry(selected), DUPLICATE_OFFSET, DUPLICATE_OFFSET).copy(
                    id = 0L,
                    zIndex = nextEditableZIndex(),
                    createdAt = System.currentTimeMillis(),
                )
            executeCommand(PatternAnnotationCommand.Insert(duplicate))
        }

        fun deleteSelected() {
            val selected = selectedAnnotation() ?: return
            executeCommand(PatternAnnotationCommand.Delete(selected)) { state ->
                if (state.selectedAnnotationId == selected.id) state.copy(selectedAnnotationId = null) else state
            }
        }

        fun bringSelectedForward() {
            reorderSelected(toFront = true)
        }

        fun sendSelectedBackward() {
            reorderSelected(toFront = false)
        }

        private fun reorderSelected(toFront: Boolean) {
            val selected = selectedAnnotation() ?: return
            val before = editableAnnotations()
            val edge = if (toFront) before.maxOf(PatternAnnotation::zIndex) else before.minOf(PatternAnnotation::zIndex)
            val now = System.currentTimeMillis()
            if (edge != if (toFront) Long.MAX_VALUE else Long.MIN_VALUE) {
                updateSelected { selected.copy(zIndex = edge + if (toFront) 1L else -1L, updatedAt = now) }
                return
            }
            val remaining =
                before
                    .filterNot { it.id == selected.id }
                    .sortedWith(compareBy(PatternAnnotation::zIndex, PatternAnnotation::id))
            val ordered = if (toFront) remaining + selected else listOf(selected) + remaining
            val after =
                ordered.mapIndexed { index, annotation ->
                    annotation.copy(
                        zIndex = index.toLong(),
                        updatedAt = if (annotation.id == selected.id) now else annotation.updatedAt,
                    )
                }
            executeCommand(PatternAnnotationCommand.UpdateBatch(before, after))
        }

        fun editSelectedText() {
            val annotation = selectedAnnotation() ?: return
            if (annotation.payload !is TextBoxPayload || interaction.value.isSaving) return
            if (annotation.layerId != layers.value.editableLayerId || annotation.page != currentPage.value) return
            interaction.update {
                it.copy(
                    textEdit = PatternAnnotationTextEdit(layers.value.owner, annotation),
                    writeError = PatternAnnotationWriteError.NONE,
                )
            }
        }

        fun cancelTextEdit(edit: PatternAnnotationTextEdit) {
            interaction.update {
                if (it.textEdit === edit && !it.isSaving) {
                    it.copy(textEdit = null, writeError = PatternAnnotationWriteError.NONE)
                } else {
                    it
                }
            }
        }

        fun saveTextEdit(
            edit: PatternAnnotationTextEdit,
            text: String,
        ) {
            if (interaction.value.textEdit !== edit || interaction.value.isSaving || text.isBlank()) return
            if (interaction.value.writeError == PatternAnnotationWriteError.TEXT_CONFLICT) return
            if (edit.owner != layers.value.owner || edit.annotation.layerId != layers.value.editableLayerId) return
            if (edit.annotation.page != currentPage.value ||
                editableAnnotations().none { it.id == edit.annotation.id }
            ) {
                return
            }
            if (text == (edit.annotation.payload as TextBoxPayload).text) {
                cancelTextEdit(edit)
                return
            }
            executeCommand(PatternAnnotationCommand.EditText(edit.owner, edit.annotation, text)) { state ->
                if (state.textEdit === edit) state.copy(textEdit = null) else state
            }
        }

        fun addTextBox(
            text: String,
            bounds: NormalizedPatternBounds = DEFAULT_EDITOR_BOUNDS,
            draftId: String? = null,
        ) {
            if (text.isBlank()) return
            insertPayload(
                kind = PatternAnnotationKind.TEXT_BOX,
                payload =
                    TextBoxPayload(
                        bounds = bounds,
                        text = text,
                        textSizeSp = PatternAnnotationTokens.TEXT_DEFAULT_SIZE,
                        textArgb = PatternAnnotationTokens.PEN_DEFAULT_ARGB,
                        backgroundArgb = PatternAnnotationTokens.TEXT_BACKGROUND_ARGB,
                        backgroundAlpha = PatternAnnotationTokens.TEXT_BACKGROUND_ALPHA,
                    ),
                draftId = draftId,
            )
        }

        fun addCallout(
            title: String,
            description: String,
            symbol: PatternCalloutSymbol,
            bounds: NormalizedPatternBounds = DEFAULT_EDITOR_BOUNDS,
            draftId: String? = null,
        ) {
            if (title.isBlank() && description.isBlank()) return
            insertPayload(
                kind = PatternAnnotationKind.CALLOUT,
                payload =
                    CalloutPayload(
                        bounds = bounds,
                        symbol = symbol,
                        title = title,
                        description = description,
                        argb = PatternAnnotationTokens.CALLOUT_DEFAULT_ARGB,
                    ),
                draftId = draftId,
            )
        }

        fun clearEditablePage() {
            val layerId = layers.value.editableLayerId ?: return
            if (editableAnnotations().isEmpty()) return
            executeCommand(PatternAnnotationCommand.ClearPage(layerId, currentPage.value)) { state ->
                state.copy(selectedAnnotationId = null)
            }
        }

        fun undo() {
            executeHistoryCommand(isUndo = true)
        }

        fun redo() {
            executeHistoryCommand(isUndo = false)
        }

        fun setExportContext(context: PatternPdfExportContext?) {
            if (exportContext == context) return
            exportContext = context
            invalidateExport()
        }

        fun requestAnnotatedPdfExport(context: PatternPdfExportContext) {
            val exporter = pdfExporter ?: return
            if (exportRequest != null || context != exportContext || !matchesExportLayers(context)) return
            val request =
                PatternPdfExportRequest(
                    context = context,
                    layerIds = visibleExportLayerIds(),
                    generation = exportGeneration,
                )
            exportRequest = request
            exportPhase = PatternPdfExportPhase.PREFLIGHT
            markExportStarted()
            exportJob =
                viewModelScope.launch {
                    runCatching {
                        val annotations = loadExportAnnotations(request.layerIds)
                        coroutineContext.ensureActive()
                        if (!isCurrentExport(request)) return@launch
                        val trackerHighlights = resolveExportTrackerHighlights(annotations, counterContext.value)
                        exporter.preflight(request.context.sourceUri, annotations, trackerHighlights)
                        coroutineContext.ensureActive()
                    }.onSuccess {
                        if (isCurrentExport(request)) {
                            exportPhase = PatternPdfExportPhase.READY
                            interaction.update { it.copy(isExporting = false) }
                            exportDestinationChannel.trySend(request.id)
                        }
                    }.onFailure { handleExportFailure(request, it) }
                }
        }

        fun launchExportDestination(
            requestId: String,
            context: PatternPdfExportContext?,
        ): Boolean {
            val request = exportRequest ?: return false
            if (
                request.id != requestId ||
                context != request.context ||
                !isCurrentExport(request) ||
                exportPhase != PatternPdfExportPhase.READY
            ) {
                return false
            }
            exportPhase = PatternPdfExportPhase.DESTINATION
            return true
        }

        fun exportAnnotatedPdf(
            requestId: String,
            context: PatternPdfExportContext?,
            destinationUri: Uri?,
            style: PatternAnnotationRenderStyle,
        ) {
            val exporter = pdfExporter ?: return
            val request = exportRequest ?: return
            if (request.id != requestId || !isCurrentExport(request)) return
            if (exportPhase != PatternPdfExportPhase.DESTINATION && exportPhase != PatternPdfExportPhase.READY) return
            if (destinationUri == null || context != request.context) {
                if (destinationUri == null) analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_CANCELLED)
                invalidateExport()
                return
            }
            if (exportPhase != PatternPdfExportPhase.DESTINATION) return
            analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_STARTED)
            exportPhase = PatternPdfExportPhase.EXPORTING
            markExportStarted()
            exportJob =
                viewModelScope.launch {
                    runCatching {
                        val annotations = loadExportAnnotations(request.layerIds)
                        coroutineContext.ensureActive()
                        if (!isCurrentExport(request)) return@launch
                        exporter.export(
                            sourceUri = request.context.sourceUri,
                            destinationUri = destinationUri,
                            annotations = annotations,
                            trackerHighlights = resolveExportTrackerHighlights(annotations, counterContext.value),
                            style = style,
                        ) { progress ->
                            updateExportProgress(request, progress)
                        }
                        coroutineContext.ensureActive()
                    }.onSuccess {
                        if (isCurrentExport(request)) {
                            analytics.track(UsageEvent.ANNOTATED_PDF_EXPORT_SUCCEEDED)
                            exportRequest = null
                            interaction.update { it.copy(isExporting = false) }
                        }
                    }.onFailure { handleExportFailure(request, it) }
                }
        }

        private fun updateExportProgress(
            request: PatternPdfExportRequest,
            progress: PatternPdfExportProgress,
        ) {
            viewModelScope.launch {
                if (isCurrentExport(request)) {
                    interaction.update {
                        it.copy(
                            exportCompletedPages = progress.completedPages,
                            exportTotalPages = progress.totalPages,
                        )
                    }
                }
            }
        }

        private fun matchesExportLayers(context: PatternPdfExportContext): Boolean {
            val selection = layers.value
            if (context.owner != selection.owner || selection.editableLayerId == null) return false
            return when (context.owner) {
                is PatternAnnotationOwner.SavedPattern -> {
                    context.owner == routeOwner && context.projectDocumentId == null
                }

                is PatternAnnotationOwner.Project -> {
                    context.projectDocumentId == selection.projectDocumentId &&
                        context.sourceUri.toString() == selection.sourceUri
                }
            }
        }

        private fun isCurrentExport(request: PatternPdfExportRequest): Boolean =
            exportRequest === request &&
                request.generation == exportGeneration &&
                request.context == exportContext &&
                matchesExportLayers(request.context)

        private fun invalidateExport() {
            exportGeneration++
            exportRequest = null
            exportJob?.cancel()
            exportJob = null
            interaction.update {
                it.copy(isExporting = false, exportFailed = false, exportCompletedPages = 0, exportTotalPages = 0)
            }
        }

        private fun visibleExportLayerIds(): List<Long> {
            val selection = layers.value
            return buildList {
                if (masterLayerVisible.value) selection.masterLayer?.id?.let(::add)
                if (projectLayerVisible.value) selection.projectLayer?.id?.let(::add)
            }
        }

        private suspend fun loadExportAnnotations(layerIds: List<Long>): List<PatternAnnotation> {
            val annotations =
                annotationRepository.getForLayersForExport(
                    layerIds = layerIds,
                    maxAnnotations = PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                    maxPayloadBytes = PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
                )
            PatternPdfExportBudget.requireAnnotationCount(annotations.size)
            return annotations
        }

        private fun markExportStarted() {
            interaction.update {
                it.copy(
                    isExporting = true,
                    exportCompletedPages = 0,
                    exportTotalPages = 0,
                    exportFailed = false,
                )
            }
        }

        private fun handleExportFailure(
            request: PatternPdfExportRequest,
            failure: Throwable,
        ) {
            analytics.track(
                if (failure is CancellationException) {
                    UsageEvent.ANNOTATED_PDF_EXPORT_CANCELLED
                } else {
                    UsageEvent.ANNOTATED_PDF_EXPORT_FAILED
                },
            )
            if (isCurrentExport(request)) {
                exportRequest = null
                interaction.update { it.copy(isExporting = false, exportFailed = failure !is CancellationException) }
            }
            if (failure is CancellationException) throw failure
        }

        private fun editableAnnotations(): List<PatternAnnotation> {
            val observed = pageAnnotations.value
            val selection = layers.value
            if (observed.page != currentPage.value || observed.selection != selection) return emptyList()
            return when (selection.owner) {
                is PatternAnnotationOwner.Project -> observed.project
                is PatternAnnotationOwner.SavedPattern -> observed.master
            }
        }

        private fun selectedAnnotation(): PatternAnnotation? {
            if (!isEditableLayerVisible) return null
            val selectedId = interaction.value.selectedAnnotationId ?: return null
            return editableAnnotations().firstOrNull { it.id == selectedId }
        }

        private fun updateSelected(transform: (PatternAnnotation) -> PatternAnnotation) {
            val before = selectedAnnotation() ?: return
            val after = transform(before)
            if (before != after) executeCommand(PatternAnnotationCommand.Update(before, after))
        }

        private fun insertPayload(
            kind: PatternAnnotationKind,
            payload: PatternAnnotationPayload,
            draftId: String? = null,
        ) {
            if (draftId != null && interaction.value.savedDraftId == draftId) return
            val layerId = layers.value.editableLayerId ?: return
            executeCommand(
                PatternAnnotationCommand.Insert(
                    PatternAnnotation(
                        layerId = layerId,
                        page = currentPage.value,
                        kind = kind,
                        payload = payload,
                        zIndex = nextEditableZIndex(),
                    ),
                ),
            ) { state ->
                if (draftId != null) state.copy(savedDraftId = draftId) else state
            }
        }

        private fun executeCommand(
            command: PatternAnnotationCommand,
            onSuccess: (PatternAnnotationInteractionState) -> PatternAnnotationInteractionState = { it },
        ) {
            if (!isEditableLayerVisible ||
                interaction.value.isSaving ||
                uiState.value.loadError == PatternAnnotationLoadError.PAGE_LIMIT
            ) {
                return
            }
            val commandContext = currentEditContext()
            interaction.update { it.copy(isSaving = true, writeError = PatternAnnotationWriteError.NONE) }
            persistCommand(command, commandContext, onSuccess)
        }

        private fun persistCommand(
            command: PatternAnnotationCommand,
            commandContext: AnnotationEditContext,
            onSuccess: (PatternAnnotationInteractionState) -> PatternAnnotationInteractionState,
        ) {
            viewModelScope.launch {
                runCatching { command.apply(annotationRepository) }
                    .onSuccess { inverse ->
                        if (command is PatternAnnotationCommand.Insert) analytics.track(UsageEvent.ANNOTATION_CREATED)
                        interaction.update { state ->
                            if (currentEditContext() != commandContext) return@update state.copy(isSaving = false)
                            onSuccess(state).copy(
                                undoStack = (state.undoStack + inverse).takeLast(HISTORY_LIMIT),
                                redoStack = emptyList(),
                                isSaving = false,
                            )
                        }
                    }.onFailure { failure ->
                        handleCommandFailure(command, commandContext, failure)
                    }
                savingStroke = null
                persistNextStroke()
            }
        }

        private fun handleCommandFailure(
            command: PatternAnnotationCommand,
            commandContext: AnnotationEditContext,
            failure: Throwable,
        ) {
            if (failure is CancellationException) {
                savingStroke = null
                interaction.update { state -> state.copy(isSaving = false) }
                throw failure
            }
            interaction.update { state ->
                if (currentEditContext() == commandContext) {
                    state.copy(
                        isSaving = false,
                        writeError =
                            if (command is PatternAnnotationCommand.EditText &&
                                failure is PatternAnnotationTextConflict
                            ) {
                                PatternAnnotationWriteError.TEXT_CONFLICT
                            } else {
                                PatternAnnotationWriteError.WRITE_FAILED
                            },
                        failedStrokes = savingStroke?.let { state.failedStrokes + it } ?: state.failedStrokes,
                    )
                } else {
                    state.copy(isSaving = false)
                }
            }
        }

        private fun executeHistoryCommand(isUndo: Boolean) {
            val state = interaction.value
            if (!isEditableLayerVisible || state.isSaving) return
            val source = if (isUndo) state.undoStack else state.redoStack
            val command = source.lastOrNull() ?: return
            val commandContext = currentEditContext()
            interaction.value = state.copy(isSaving = true, writeError = PatternAnnotationWriteError.NONE)
            viewModelScope.launch {
                runCatching { command.apply(annotationRepository) }
                    .onSuccess { inverse ->
                        interaction.update { current ->
                            historyCommandSuccessState(current, inverse, isUndo, commandContext)
                        }
                    }.onFailure { failure ->
                        if (failure is CancellationException) {
                            interaction.update { current -> current.copy(isSaving = false) }
                            throw failure
                        }
                        interaction.update { current ->
                            current.copy(
                                isSaving = false,
                                writeError =
                                    if (currentEditContext() == commandContext) {
                                        PatternAnnotationWriteError.WRITE_FAILED
                                    } else {
                                        current.writeError
                                    },
                            )
                        }
                    }
                persistNextStroke()
            }
        }

        private fun historyCommandSuccessState(
            current: PatternAnnotationInteractionState,
            inverse: PatternAnnotationCommand,
            isUndo: Boolean,
            commandContext: AnnotationEditContext,
        ): PatternAnnotationInteractionState {
            if (currentEditContext() != commandContext) {
                return current.copy(isSaving = false)
            }
            return if (isUndo) {
                current.copy(
                    undoStack = current.undoStack.dropLast(1),
                    redoStack = (current.redoStack + inverse).takeLast(HISTORY_LIMIT),
                    isSaving = false,
                )
            } else {
                current.copy(
                    undoStack = (current.undoStack + inverse).takeLast(HISTORY_LIMIT),
                    redoStack = current.redoStack.dropLast(1),
                    isSaving = false,
                )
            }
        }

        private fun currentEditContext(): AnnotationEditContext =
            layers.value.editContext.copy(generation = editContextGeneration)

        private fun handleLayerSelection(selection: AnnotationLayerSelection) {
            if (exportSelection != selection) {
                exportSelection = selection
                invalidateExport()
            }
            val nextContext = selection.editContext
            val previousContext = editContext
            editContext = nextContext
            if (previousContext != null && previousContext != nextContext) {
                editContextGeneration++
                interaction.update { state ->
                    state.copy(
                        draftStroke = null,
                        failedStrokes = emptyList(),
                        writeError = PatternAnnotationWriteError.NONE,
                        selectedAnnotationId = null,
                        textEdit = null,
                        undoStack = emptyList(),
                        redoStack = emptyList(),
                    )
                }
            }
        }

        private fun nextEditableZIndex(): Long =
            editableAnnotations()
                .maxOfOrNull(PatternAnnotation::zIndex)
                ?.coerceAtMost(Long.MAX_VALUE - 1L)
                ?.plus(1L) ?: 0L

        private fun createCounterContextFlow(owner: PatternAnnotationOwner): Flow<PatternCounterContext> =
            when (owner) {
                is PatternAnnotationOwner.SavedPattern -> {
                    flowOf(PatternCounterContext())
                }

                is PatternAnnotationOwner.Project -> {
                    val counters =
                        projectCounterRepository?.getCountersForProject(owner.projectId) ?: flowOf(emptyList())
                    combine(counterRepository.observeProject(owner.projectId), counters) { project, extras ->
                        PatternCounterContext(project, extras)
                    }
                }
            }

        private fun createLayerFlow(owner: PatternAnnotationOwner): Flow<AnnotationLayerSelection> =
            when (owner) {
                is PatternAnnotationOwner.SavedPattern -> {
                    flow {
                        val master =
                            layerRepository.getOrCreateMasterLayer(
                                owner.savedPatternId,
                                owner.documentKey,
                            )
                        emit(AnnotationLayerSelection(owner = owner, masterLayer = master))
                    }
                }

                is PatternAnnotationOwner.Project -> {
                    combine(
                        layerRepository.observeLayers(owner),
                        projectDocumentRepository.observeDocuments(owner.projectId),
                    ) { projectLayers, documents ->
                        projectLayers to documents
                    }.mapLatest { (projectLayers, documents) ->
                        val projectLayer = projectLayers.firstOrNull { it.isActive }
                        val document = documents.firstOrNull { it.documentKey == projectLayer?.owner?.documentKey }
                        if (projectLayer == null || document == null) {
                            AnnotationLayerSelection(owner = owner)
                        } else {
                            val masterLayer =
                                document.savedPatternId?.let { savedPatternId ->
                                    layerRepository.getOrCreateMasterLayer(savedPatternId, document.documentKey)
                                }
                            AnnotationLayerSelection(
                                owner = projectLayer.owner,
                                masterLayer = masterLayer,
                                projectLayer = projectLayer,
                                projectDocumentId = document.id,
                                sourceUri = document.localPdfUri,
                            )
                        }
                    }
                }
            }

        private fun observePage(
            layer: PatternAnnotationLayer?,
            page: Int,
        ): Flow<List<PatternAnnotation>> =
            layer?.let { annotationRepository.observePage(it.id, page) } ?: flowOf(emptyList())

        private fun <T> Flow<T>.withReadRecovery(readFailed: MutableStateFlow<Boolean>): Flow<T> =
            retryOnRepositoryReadFailure { readFailed.value = true }
                .onEach { readFailed.value = false }

        companion object {
            const val PROJECT_ID_KEY = "projectId"
            const val SAVED_PATTERN_ID_KEY = "savedPatternId"
        }
    }

private data class AnnotationLayerSelection(
    val owner: PatternAnnotationOwner,
    val masterLayer: PatternAnnotationLayer? = null,
    val projectLayer: PatternAnnotationLayer? = null,
    val projectDocumentId: Long? = null,
    val sourceUri: String? = null,
) {
    val editableLayerId: Long?
        get() =
            when (owner) {
                is PatternAnnotationOwner.Project -> projectLayer?.id
                is PatternAnnotationOwner.SavedPattern -> masterLayer?.id
            }

    val editContext: AnnotationEditContext
        get() = AnnotationEditContext(owner = owner, editableLayerId = editableLayerId)
}

private data class AnnotationEditContext(
    val owner: PatternAnnotationOwner,
    val editableLayerId: Long?,
    val generation: Long = 0L,
)

private data class PendingAnnotationStroke(
    val draft: PatternStrokeDraft,
    val annotation: PatternAnnotation,
    val context: AnnotationEditContext,
)

private data class PageAnnotations(
    val page: Int? = null,
    val selection: AnnotationLayerSelection? = null,
    val master: List<PatternAnnotation> = emptyList(),
    val project: List<PatternAnnotation> = emptyList(),
    val limitExceeded: Boolean = false,
) {
    val topZIndex: Long =
        listOfNotNull(master.maxOfOrNull(PatternAnnotation::zIndex), project.maxOfOrNull(PatternAnnotation::zIndex))
            .maxOrNull()
            ?.coerceAtMost(Long.MAX_VALUE - 1L)
            ?.plus(1L) ?: 0L
}

private data class LayerVisibility(
    val master: Boolean,
    val project: Boolean,
)

private data class PatternAnnotationInteractionState(
    val textEdit: PatternAnnotationTextEdit? = null,
    val savedDraftId: String? = null,
    val activeTool: PatternAnnotationTool = PatternAnnotationTool.BROWSE,
    val penArgb: Int = PatternAnnotationTokens.PEN_DEFAULT_ARGB,
    val penStrokeWidth: Float = PatternAnnotationTokens.PEN_DEFAULT_WIDTH,
    val pressureEnabled: Boolean = true,
    val highlighterArgb: Int = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ARGB,
    val highlighterStrokeWidth: Float = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_WIDTH,
    val highlighterAxisLock: PatternHighlighterAxisLock = PatternHighlighterAxisLock.FREE,
    val draftStroke: PatternStrokeDraft? = null,
    val failedStrokes: List<PendingAnnotationStroke> = emptyList(),
    val isSaving: Boolean = false,
    val writeError: PatternAnnotationWriteError = PatternAnnotationWriteError.NONE,
    val selectedAnnotationId: Long? = null,
    val undoStack: List<PatternAnnotationCommand> = emptyList(),
    val redoStack: List<PatternAnnotationCommand> = emptyList(),
    val isExporting: Boolean = false,
    val exportCompletedPages: Int = 0,
    val exportTotalPages: Int = 0,
    val exportFailed: Boolean = false,
) {
    fun styleForTool(): PatternStrokeStyle? =
        when (activeTool) {
            PatternAnnotationTool.PEN -> PatternStrokeStyle(penArgb, penStrokeWidth)

            PatternAnnotationTool.HIGHLIGHTER -> PatternStrokeStyle(highlighterArgb, highlighterStrokeWidth)

            PatternAnnotationTool.LINE,
            PatternAnnotationTool.ARROW,
            PatternAnnotationTool.RECTANGLE,
            PatternAnnotationTool.ELLIPSE,
            PatternAnnotationTool.CHART,
            -> PatternStrokeStyle(penArgb, penStrokeWidth)

            else -> null
        }
}

private data class PatternStrokeStyle(
    val argb: Int,
    val strokeWidth: Float,
)

private data class PatternAnnotationFeedback(
    val loadError: PatternAnnotationLoadError,
    val interaction: PatternAnnotationInteractionState,
    val counterContext: PatternCounterContext,
    val trackerHighlights: Map<Long, ChartTrackerHighlight>,
)

private data class PatternCounterContext(
    val project: CounterProject? = null,
    val extraCounters: List<ProjectCounter> = emptyList(),
) {
    val options: List<PatternChartCounterOption>
        get() =
            buildList {
                project?.let { currentProject ->
                    add(
                        PatternChartCounterOption(
                            ChartCounterType.MAIN,
                            name = currentProject.name,
                            value = currentProject.count,
                        ),
                    )
                }
                extraCounters.forEach { counter ->
                    add(
                        PatternChartCounterOption(
                            type = ChartCounterType.EXTRA,
                            extraCounterId = counter.id,
                            name = counter.name,
                            value = counter.count,
                        ),
                    )
                }
            }

    fun valueFor(tracker: ChartTrackerPayload): Int? =
        when (tracker.counterType) {
            ChartCounterType.MAIN -> project?.count
            ChartCounterType.EXTRA -> extraCounters.firstOrNull { it.id == tracker.extraCounterId }?.count
        }
}

private fun resolveTrackerHighlights(
    annotations: List<PatternAnnotation>,
    counterContext: PatternCounterContext,
): Map<Long, ChartTrackerHighlight> {
    val consumedCellsByPage = mutableMapOf<Int, Int>()
    var consumedCells = 0
    return buildMap {
        annotations.forEach { annotation ->
            val tracker = annotation.payload as? ChartTrackerPayload ?: return@forEach
            val cellCount = tracker.region.rows * tracker.region.columns
            val consumed = consumedCellsByPage[annotation.page] ?: 0
            if (cellCount > PatternAnnotationLimits.MAX_CHART_CELLS_PER_PAGE - consumed) return@forEach
            if (cellCount > PatternAnnotationLimits.MAX_CHART_CELLS_PER_DOCUMENT - consumedCells) {
                return@forEach
            }
            consumedCellsByPage[annotation.page] = consumed + cellCount
            consumedCells += cellCount
            put(annotation.id, resolveChartTrackerHighlight(tracker, counterContext.valueFor(tracker)))
        }
    }
}

private fun resolveExportTrackerHighlights(
    annotations: List<PatternAnnotation>,
    counters: PatternCounterContext,
): Map<Long, ChartTrackerHighlight> =
    buildMap {
        var remaining = PATTERN_PDF_EXPORT_MAX_TRACKER_CELLS
        annotations.forEach { annotation ->
            val tracker = annotation.payload as? ChartTrackerPayload ?: return@forEach
            val highlight = resolveChartTrackerHighlight(tracker, counters.valueFor(tracker))
            remaining -= highlight.cells.size.toLong()
            if (remaining < 0L) {
                throw PatternPdfExportLimitException(PatternPdfExportLimitReason.TRACKER_HIGHLIGHT_WORK)
            }
            put(annotation.id, highlight)
        }
    }

private fun PatternStrokeDraft.withHighlighterAxisLock(axisLock: PatternHighlighterAxisLock): PatternStrokeDraft =
    if (tool == PatternAnnotationTool.HIGHLIGHTER) copy(points = lockHighlighterPoints(points, axisLock)) else this

private fun PatternStrokeDraft.toAnnotation(
    layerId: Long?,
    page: Int,
    zIndex: Long,
): PatternAnnotation? {
    val targetLayerId = layerId ?: return null
    val kind = tool.toAnnotationKind() ?: return null
    val annotationPayload =
        when (kind) {
            PatternAnnotationKind.FREEHAND,
            PatternAnnotationKind.HIGHLIGHTER,
            -> {
                FreehandPayload(
                    points = points,
                    argb = argb,
                    strokeWidth = strokeWidth,
                    pressureEnabled = pressureEnabled,
                )
            }

            PatternAnnotationKind.LINE,
            PatternAnnotationKind.ARROW,
            PatternAnnotationKind.RECTANGLE,
            PatternAnnotationKind.ELLIPSE,
            -> {
                val start = points.firstOrNull() ?: return null
                val end = points.lastOrNull() ?: return null
                ShapePayload(
                    start = start,
                    end = end,
                    strokeArgb = argb,
                    strokeWidth = strokeWidth,
                )
            }

            PatternAnnotationKind.CHART_REGION -> {
                val start = points.firstOrNull() ?: return null
                val end = points.lastOrNull() ?: return null
                ChartRegionPayload(
                    bounds =
                        NormalizedPatternBounds(
                            left = minOf(start.x, end.x),
                            top = minOf(start.y, end.y),
                            right = maxOf(start.x, end.x),
                            bottom = maxOf(start.y, end.y),
                        ),
                    name = DEFAULT_CHART_NAME,
                    rows = DEFAULT_CHART_ROWS,
                    columns = DEFAULT_CHART_COLUMNS,
                    rowDirection = ChartRowDirection.BOTTOM_TO_TOP,
                    columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                )
            }

            else -> {
                return null
            }
        }
    return PatternAnnotation(
        layerId = targetLayerId,
        page = page,
        kind = kind,
        payload = annotationPayload,
        zIndex = zIndex,
    )
}

private fun PatternAnnotationTool.analyticsDetail(): UsageDetail =
    when (this) {
        PatternAnnotationTool.BROWSE -> UsageDetail.ANNOTATION_BROWSE
        PatternAnnotationTool.PEN -> UsageDetail.ANNOTATION_PEN
        PatternAnnotationTool.HIGHLIGHTER -> UsageDetail.ANNOTATION_HIGHLIGHTER
        PatternAnnotationTool.ERASER -> UsageDetail.ANNOTATION_ERASER
        PatternAnnotationTool.SELECT -> UsageDetail.ANNOTATION_SELECT
        PatternAnnotationTool.LINE -> UsageDetail.ANNOTATION_LINE
        PatternAnnotationTool.ARROW -> UsageDetail.ANNOTATION_ARROW
        PatternAnnotationTool.RECTANGLE -> UsageDetail.ANNOTATION_RECTANGLE
        PatternAnnotationTool.ELLIPSE -> UsageDetail.ANNOTATION_ELLIPSE
        PatternAnnotationTool.TEXT -> UsageDetail.ANNOTATION_TEXT
        PatternAnnotationTool.CALLOUT -> UsageDetail.ANNOTATION_CALLOUT
        PatternAnnotationTool.CHART -> UsageDetail.ANNOTATION_CHART
    }

private fun PatternAnnotationTool.toAnnotationKind(): PatternAnnotationKind? =
    when (this) {
        PatternAnnotationTool.PEN -> PatternAnnotationKind.FREEHAND
        PatternAnnotationTool.HIGHLIGHTER -> PatternAnnotationKind.HIGHLIGHTER
        PatternAnnotationTool.LINE -> PatternAnnotationKind.LINE
        PatternAnnotationTool.ARROW -> PatternAnnotationKind.ARROW
        PatternAnnotationTool.RECTANGLE -> PatternAnnotationKind.RECTANGLE
        PatternAnnotationTool.ELLIPSE -> PatternAnnotationKind.ELLIPSE
        PatternAnnotationTool.CHART -> PatternAnnotationKind.CHART_REGION
        else -> null
    }

private fun Float.coerceToStrokeWidthLimits(): Float =
    coerceIn(PatternAnnotationLimits.MIN_STROKE_WIDTH, PatternAnnotationLimits.MAX_STROKE_WIDTH)

private const val DUPLICATE_OFFSET = 0.02f
private const val HISTORY_LIMIT = 50
private const val DEFAULT_CHART_ROWS = 10
private const val DEFAULT_CHART_COLUMNS = 10
private const val DEFAULT_CHART_NAME = "Chart"
private val DEFAULT_EDITOR_BOUNDS = NormalizedPatternBounds(0.2f, 0.2f, 0.8f, 0.4f)

private fun SavedStateHandle.requirePatternAnnotationOwner(): PatternAnnotationOwner {
    val projectId = get<Long>(PatternAnnotationViewModel.PROJECT_ID_KEY)?.takeIf { it > 0L }
    val savedPatternId = get<Long>(PatternAnnotationViewModel.SAVED_PATTERN_ID_KEY)?.takeIf { it > 0L }
    require((projectId == null) != (savedPatternId == null)) {
        "Pattern annotation route requires exactly one positive owner id"
    }
    return if (projectId != null) {
        PatternAnnotationOwner.Project(projectId, PatternAnnotationDocumentKey.legacyProject(projectId))
    } else {
        val id = checkNotNull(savedPatternId)
        PatternAnnotationOwner.SavedPattern(id, PatternAnnotationDocumentKey.savedPattern(id))
    }
}
