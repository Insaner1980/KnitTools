package com.finnvek.knittools.ui.screens.pattern

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.storage.PatternAnnotationRenderStyle
import com.finnvek.knittools.data.storage.PatternPdfExporter
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
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.ui.theme.PatternAnnotationTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PatternAnnotationLoadError {
    NONE,
    READ_FAILED,
}

enum class PatternAnnotationWriteError {
    NONE,
    WRITE_FAILED,
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

data class PatternAnnotationUiState(
    val owner: PatternAnnotationOwner,
    val currentPage: Int = 0,
    val masterLayerVisible: Boolean = true,
    val projectLayerVisible: Boolean = true,
    val masterAnnotations: List<PatternAnnotation> = emptyList(),
    val projectAnnotations: List<PatternAnnotation> = emptyList(),
    val editableLayerId: Long? = null,
    val loadError: PatternAnnotationLoadError = PatternAnnotationLoadError.NONE,
    val activeTool: PatternAnnotationTool = PatternAnnotationTool.BROWSE,
    val penArgb: Int = PatternAnnotationTokens.PEN_DEFAULT_ARGB,
    val penStrokeWidth: Float = PatternAnnotationTokens.PEN_DEFAULT_WIDTH,
    val pressureEnabled: Boolean = true,
    val highlighterArgb: Int = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ARGB,
    val highlighterStrokeWidth: Float = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_WIDTH,
    val highlighterAxisLock: PatternHighlighterAxisLock = PatternHighlighterAxisLock.FREE,
    val draftStroke: PatternStrokeDraft? = null,
    val inProgressAnnotation: PatternAnnotation? = null,
    val isSaving: Boolean = false,
    val writeError: PatternAnnotationWriteError = PatternAnnotationWriteError.NONE,
    val selectedAnnotationId: Long? = null,
    val selectedAnnotationIsEditable: Boolean = false,
    val selectedAnnotationSupportsChartTracker: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val chartCounterOptions: List<PatternChartCounterOption> = emptyList(),
    val trackerHighlights: Map<Long, ChartTrackerHighlight> = emptyMap(),
    val masterLayerId: Long? = null,
    val projectLayerId: Long? = null,
    val isExporting: Boolean = false,
    val exportCompletedPages: Int = 0,
    val exportTotalPages: Int = 0,
    val exportFailed: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
// Reittikohtainen ViewModel tarjoaa Compose-kerrokselle jokaisen editorin käyttäjäaikeen erillisenä metodina.
@Suppress("TooManyFunctions")
class PatternAnnotationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val counterRepository: CounterRepository,
        private val layerRepository: PatternAnnotationLayerRepository,
        private val annotationRepository: PatternAnnotationRepository,
        private val projectCounterRepository: ProjectCounterRepository? = null,
        private val pdfExporter: PatternPdfExporter? = null,
    ) : ViewModel() {
        private val routeOwner = savedStateHandle.requirePatternAnnotationOwner()
        private val currentPage = MutableStateFlow(0)
        private val masterLayerVisible = MutableStateFlow(true)
        private val projectLayerVisible = MutableStateFlow(true)
        private val layerReadFailed = MutableStateFlow(false)
        private val annotationReadFailed = MutableStateFlow(false)
        private val interaction = MutableStateFlow(PatternAnnotationInteractionState())
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
                        PageAnnotations(master = master, project = project)
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

        private val annotationFeedback =
            combine(loadError, interaction, counterContext) { readError, interactionState, counters ->
                PatternAnnotationFeedback(readError, interactionState, counters)
            }

        val uiState: StateFlow<PatternAnnotationUiState> =
            combine(layers, pageAnnotations, currentPage, layerVisibility, annotationFeedback) {
                selection,
                annotations,
                page,
                visibility,
                feedback,
                ->
                val topZIndex =
                    (annotations.master + annotations.project)
                        .maxOfOrNull(PatternAnnotation::zIndex)
                        ?.plus(1L)
                        ?: 0L
                val selectedAnnotation =
                    feedback.interaction.selectedAnnotationId?.let { selectedId ->
                        (annotations.master + annotations.project).firstOrNull { it.id == selectedId }
                    }
                PatternAnnotationUiState(
                    owner = selection.owner,
                    currentPage = page,
                    masterLayerVisible = visibility.master,
                    projectLayerVisible = visibility.project,
                    masterAnnotations = annotations.master,
                    projectAnnotations = annotations.project,
                    editableLayerId = selection.editableLayerId,
                    masterLayerId = selection.masterLayer?.id,
                    projectLayerId = selection.projectLayer?.id,
                    loadError = feedback.loadError,
                    activeTool = feedback.interaction.activeTool,
                    penArgb = feedback.interaction.penArgb,
                    penStrokeWidth = feedback.interaction.penStrokeWidth,
                    pressureEnabled = feedback.interaction.pressureEnabled,
                    highlighterArgb = feedback.interaction.highlighterArgb,
                    highlighterStrokeWidth = feedback.interaction.highlighterStrokeWidth,
                    highlighterAxisLock = feedback.interaction.highlighterAxisLock,
                    draftStroke = feedback.interaction.draftStroke,
                    inProgressAnnotation =
                        feedback.interaction.draftStroke?.toAnnotation(
                            layerId = selection.editableLayerId,
                            page = page,
                            zIndex = topZIndex,
                        ),
                    isSaving = feedback.interaction.isSaving,
                    writeError = feedback.interaction.writeError,
                    selectedAnnotationId = feedback.interaction.selectedAnnotationId,
                    selectedAnnotationIsEditable =
                        selectedAnnotation != null &&
                            selectedAnnotation.layerId == selection.editableLayerId,
                    selectedAnnotationSupportsChartTracker =
                        selectedAnnotation?.payload is ChartRegionPayload ||
                            selectedAnnotation?.payload is ChartTrackerPayload,
                    canUndo = feedback.interaction.undoStack.isNotEmpty(),
                    canRedo = feedback.interaction.redoStack.isNotEmpty(),
                    chartCounterOptions = feedback.counterContext.options,
                    trackerHighlights =
                        resolveTrackerHighlights(
                            annotations.master + annotations.project,
                            feedback.counterContext,
                        ),
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
            if (interaction.value.draftStroke != null) {
                commitStroke(DEFAULT_SIMPLIFICATION_TOLERANCE)
            }
            interaction.update { state -> state.copy(selectedAnnotationId = null) }
            currentPage.value = page
        }

        fun setMasterLayerVisible(visible: Boolean) {
            masterLayerVisible.value = visible
        }

        fun setProjectLayerVisible(visible: Boolean) {
            projectLayerVisible.value = visible
        }

        fun setActiveTool(tool: PatternAnnotationTool) {
            if (tool != interaction.value.activeTool && interaction.value.draftStroke != null) cancelStroke()
            interaction.update { it.copy(activeTool = tool, writeError = PatternAnnotationWriteError.NONE) }
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
            interaction.update {
                it.copy(
                    penStrokeWidth =
                        width.coerceIn(
                            PatternAnnotationLimits.MIN_STROKE_WIDTH,
                            PatternAnnotationLimits.MAX_STROKE_WIDTH,
                        ),
                )
            }
        }

        fun setHighlighterStrokeWidth(width: Float) {
            interaction.update {
                it.copy(
                    highlighterStrokeWidth =
                        width.coerceIn(
                            PatternAnnotationLimits.MIN_STROKE_WIDTH,
                            PatternAnnotationLimits.MAX_STROKE_WIDTH,
                        ),
                )
            }
        }

        fun setPressureEnabled(enabled: Boolean) {
            interaction.update { it.copy(pressureEnabled = enabled) }
        }

        fun beginStroke(point: NormalizedPatternPoint) {
            val state = interaction.value
            val style = state.styleForTool() ?: return
            interaction.value =
                state.copy(
                    draftStroke =
                        PatternStrokeDraft(
                            tool = state.activeTool,
                            points = listOf(point),
                            argb = style.argb,
                            strokeWidth = style.strokeWidth,
                            pressureEnabled = state.pressureEnabled,
                        ),
                    writeError = PatternAnnotationWriteError.NONE,
                )
        }

        fun appendStrokePoint(point: NormalizedPatternPoint) {
            interaction.update { state ->
                val draft = state.draftStroke ?: return@update state
                if (draft.points.size >= PatternAnnotationLimits.MAX_FREEHAND_POINTS) return@update state
                state.copy(draftStroke = draft.copy(points = draft.points + point))
            }
        }

        fun commitStroke(simplificationTolerance: Float) {
            val interactionState = interaction.value
            val originalDraft = interactionState.draftStroke ?: return
            val layerId = uiState.value.editableLayerId ?: return
            if (interactionState.isSaving) return
            val lockedPoints =
                if (originalDraft.tool == PatternAnnotationTool.HIGHLIGHTER) {
                    lockHighlighterPoints(originalDraft.points, interactionState.highlighterAxisLock)
                } else {
                    originalDraft.points
                }
            val simplifiedPoints = simplifyFreehandPoints(lockedPoints, simplificationTolerance)
            if (simplifiedPoints.isEmpty()) return
            val annotation =
                originalDraft.copy(points = simplifiedPoints).toAnnotation(
                    layerId = layerId,
                    page = currentPage.value,
                    zIndex = nextEditableZIndex(),
                ) ?: return
            executeCommand(PatternAnnotationCommand.Insert(annotation)) { state ->
                state.copy(draftStroke = state.draftStroke.takeUnless { it == originalDraft })
            }
        }

        fun cancelStroke() {
            interaction.update { state ->
                if (state.isSaving) state else state.copy(draftStroke = null)
            }
        }

        fun eraseStrokeAt(
            point: NormalizedPatternPoint,
            tolerance: Float = PatternAnnotationTokens.ERASER_HIT_TOLERANCE,
        ) {
            val hit = topmostAnnotationAt(editableAnnotations(), point, tolerance) ?: return
            executeCommand(PatternAnnotationCommand.Delete(hit))
        }

        fun selectAnnotationAt(
            point: NormalizedPatternPoint,
            tolerance: Float = PatternAnnotationTokens.SELECTION_HIT_TOLERANCE,
        ) {
            val state = uiState.value
            val projectSelection =
                if (state.projectLayerVisible) {
                    topmostAnnotationAt(state.projectAnnotations, point, tolerance)
                } else {
                    null
                }
            val selected =
                projectSelection
                    ?: if (state.masterLayerVisible) {
                        topmostAnnotationAt(state.masterAnnotations, point, tolerance)
                    } else {
                        null
                    }
            interaction.update { it.copy(selectedAnnotationId = selected?.id) }
        }

        fun addChartTrackerFromSelected(draft: PatternChartTrackerDraft) {
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
            val region =
                sourceRegion.copy(
                    rows = draft.rows.coerceIn(1, MAX_CHART_DIMENSION),
                    columns = draft.columns.coerceIn(1, MAX_CHART_DIMENSION),
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
            )
        }

        fun moveSelected(
            deltaX: Float,
            deltaY: Float,
        ) {
            updateSelected { annotation -> translatePatternAnnotation(annotation, deltaX, deltaY) }
        }

        fun resizeSelected(scale: Float) {
            updateSelected { annotation -> scalePatternAnnotation(annotation, scale) }
        }

        fun duplicateSelected() {
            val selected = selectedAnnotation() ?: return
            val duplicate =
                translatePatternAnnotation(selected, DUPLICATE_OFFSET, DUPLICATE_OFFSET).copy(
                    id = 0L,
                    zIndex = nextEditableZIndex(),
                    createdAt = System.currentTimeMillis(),
                )
            executeCommand(PatternAnnotationCommand.Insert(duplicate))
        }

        fun deleteSelected() {
            val selected = selectedAnnotation() ?: return
            executeCommand(PatternAnnotationCommand.Delete(selected)) { state ->
                state.copy(selectedAnnotationId = null)
            }
        }

        fun bringSelectedForward() {
            val selected = selectedAnnotation() ?: return
            updateSelected { selected.copy(zIndex = nextEditableZIndex(), updatedAt = System.currentTimeMillis()) }
        }

        fun sendSelectedBackward() {
            val selected = selectedAnnotation() ?: return
            val bottomZIndex = editableAnnotations().minOfOrNull(PatternAnnotation::zIndex)?.minus(1L) ?: 0L
            updateSelected { selected.copy(zIndex = bottomZIndex, updatedAt = System.currentTimeMillis()) }
        }

        fun addTextBox(
            text: String,
            bounds: NormalizedPatternBounds = DEFAULT_EDITOR_BOUNDS,
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
            )
        }

        fun addCallout(
            title: String,
            description: String,
            symbol: PatternCalloutSymbol,
            bounds: NormalizedPatternBounds = DEFAULT_EDITOR_BOUNDS,
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
            )
        }

        fun clearEditablePage() {
            val annotations = editableAnnotations()
            val layerId = uiState.value.editableLayerId ?: return
            if (annotations.isEmpty()) return
            executeCommand(PatternAnnotationCommand.ClearPage(layerId, currentPage.value, annotations)) { state ->
                state.copy(selectedAnnotationId = null)
            }
        }

        fun undo() {
            executeHistoryCommand(isUndo = true)
        }

        fun redo() {
            executeHistoryCommand(isUndo = false)
        }

        fun clearWriteError() {
            interaction.update { it.copy(writeError = PatternAnnotationWriteError.NONE) }
        }

        fun exportAnnotatedPdf(
            sourceUri: Uri,
            destinationUri: Uri,
            style: PatternAnnotationRenderStyle,
        ) {
            val exporter = pdfExporter ?: return
            if (interaction.value.isExporting) return
            val state = uiState.value
            val layerIds =
                buildList {
                    if (state.masterLayerVisible) state.masterLayerId?.let(::add)
                    if (state.projectLayerVisible) state.projectLayerId?.let(::add)
                }
            interaction.update {
                it.copy(
                    isExporting = true,
                    exportCompletedPages = 0,
                    exportTotalPages = 0,
                    exportFailed = false,
                )
            }
            viewModelScope.launch {
                runCatching {
                    val annotations = annotationRepository.getForLayers(layerIds)
                    exporter.export(
                        sourceUri = sourceUri,
                        destinationUri = destinationUri,
                        annotations = annotations,
                        trackerHighlights = resolveTrackerHighlights(annotations, counterContext.value),
                        style = style,
                    ) { progress ->
                        interaction.update {
                            it.copy(
                                exportCompletedPages = progress.completedPages,
                                exportTotalPages = progress.totalPages,
                            )
                        }
                    }
                }.onSuccess {
                    interaction.update { it.copy(isExporting = false) }
                }.onFailure { failure ->
                    if (failure is kotlinx.coroutines.CancellationException) throw failure
                    interaction.update { it.copy(isExporting = false, exportFailed = true) }
                }
            }
        }

        private fun editableAnnotations(): List<PatternAnnotation> =
            when (uiState.value.owner) {
                is PatternAnnotationOwner.Project -> uiState.value.projectAnnotations
                is PatternAnnotationOwner.SavedPattern -> uiState.value.masterAnnotations
            }

        private fun selectedAnnotation(): PatternAnnotation? {
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
            payload: com.finnvek.knittools.domain.model.PatternAnnotationPayload,
        ) {
            val layerId = uiState.value.editableLayerId ?: return
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
            )
        }

        private fun executeCommand(
            command: PatternAnnotationCommand,
            onSuccess: (PatternAnnotationInteractionState) -> PatternAnnotationInteractionState = { it },
        ) {
            if (interaction.value.isSaving) return
            interaction.update { it.copy(isSaving = true, writeError = PatternAnnotationWriteError.NONE) }
            viewModelScope.launch {
                runCatching { command.apply(annotationRepository) }
                    .onSuccess { inverse ->
                        interaction.update { state ->
                            onSuccess(state).copy(
                                undoStack = (state.undoStack + inverse).takeLast(HISTORY_LIMIT),
                                redoStack = emptyList(),
                                isSaving = false,
                            )
                        }
                    }.onFailure {
                        interaction.update { state ->
                            state.copy(isSaving = false, writeError = PatternAnnotationWriteError.WRITE_FAILED)
                        }
                    }
            }
        }

        private fun executeHistoryCommand(isUndo: Boolean) {
            val state = interaction.value
            if (state.isSaving) return
            val source = if (isUndo) state.undoStack else state.redoStack
            val command = source.lastOrNull() ?: return
            interaction.value = state.copy(isSaving = true, writeError = PatternAnnotationWriteError.NONE)
            viewModelScope.launch {
                runCatching { command.apply(annotationRepository) }
                    .onSuccess { inverse ->
                        interaction.update { current ->
                            if (isUndo) {
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
                    }.onFailure {
                        interaction.update { current ->
                            current.copy(isSaving = false, writeError = PatternAnnotationWriteError.WRITE_FAILED)
                        }
                    }
            }
        }

        private fun nextEditableZIndex(): Long =
            editableAnnotations().maxOfOrNull(PatternAnnotation::zIndex)?.plus(1L) ?: 0L

        private fun createCounterContextFlow(owner: PatternAnnotationOwner): Flow<PatternCounterContext> =
            when (owner) {
                is PatternAnnotationOwner.SavedPattern -> flowOf(PatternCounterContext())
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
                is PatternAnnotationOwner.SavedPattern ->
                    flow {
                        val master =
                            layerRepository.getOrCreateMasterLayer(
                                owner.savedPatternId,
                                owner.documentKey,
                            )
                        emit(AnnotationLayerSelection(owner = owner, masterLayer = master))
                    }

                is PatternAnnotationOwner.Project ->
                    counterRepository.observeProject(owner.projectId).flatMapLatest { project ->
                        if (project == null) {
                            flowOf(AnnotationLayerSelection(owner = owner))
                        } else {
                            val linkedDocumentKey =
                                project.linkedPatternId?.let(PatternAnnotationDocumentKey::savedPattern)
                            val defaultDocumentKey =
                                linkedDocumentKey ?: PatternAnnotationDocumentKey.legacyProject(project.id)
                            val defaultOwner = PatternAnnotationOwner.Project(project.id, defaultDocumentKey)
                            layerRepository.observeLayers(defaultOwner).mapLatest { projectLayers ->
                                val projectLayer = projectLayers.firstOrNull { it.isActive }
                                val documentKey =
                                    linkedDocumentKey ?: projectLayer?.owner?.documentKey ?: defaultDocumentKey
                                val currentOwner = PatternAnnotationOwner.Project(project.id, documentKey)
                                val masterLayer =
                                    project.linkedPatternId?.let { savedPatternId ->
                                        layerRepository.getOrCreateMasterLayer(savedPatternId, documentKey)
                                    }
                                AnnotationLayerSelection(
                                    owner = currentOwner,
                                    masterLayer = masterLayer,
                                    projectLayer = projectLayer,
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
            retryWhen { _, _ ->
                readFailed.value = true
                delay(RETRY_DELAY_MS)
                true
            }.onEach {
                readFailed.value = false
            }

        companion object {
            const val PROJECT_ID_KEY = "projectId"
            const val SAVED_PATTERN_ID_KEY = "savedPatternId"
            const val RETRY_DELAY_MS = 250L
        }
    }

private data class AnnotationLayerSelection(
    val owner: PatternAnnotationOwner,
    val masterLayer: PatternAnnotationLayer? = null,
    val projectLayer: PatternAnnotationLayer? = null,
) {
    val editableLayerId: Long?
        get() =
            when (owner) {
                is PatternAnnotationOwner.Project -> projectLayer?.id
                is PatternAnnotationOwner.SavedPattern -> masterLayer?.id
            }
}

private data class PageAnnotations(
    val master: List<PatternAnnotation> = emptyList(),
    val project: List<PatternAnnotation> = emptyList(),
)

private data class LayerVisibility(
    val master: Boolean,
    val project: Boolean,
)

private data class PatternAnnotationInteractionState(
    val activeTool: PatternAnnotationTool = PatternAnnotationTool.BROWSE,
    val penArgb: Int = PatternAnnotationTokens.PEN_DEFAULT_ARGB,
    val penStrokeWidth: Float = PatternAnnotationTokens.PEN_DEFAULT_WIDTH,
    val pressureEnabled: Boolean = true,
    val highlighterArgb: Int = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_ARGB,
    val highlighterStrokeWidth: Float = PatternAnnotationTokens.HIGHLIGHTER_DEFAULT_WIDTH,
    val highlighterAxisLock: PatternHighlighterAxisLock = PatternHighlighterAxisLock.FREE,
    val draftStroke: PatternStrokeDraft? = null,
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
): Map<Long, ChartTrackerHighlight> =
    annotations
        .mapNotNull { annotation ->
            val tracker = annotation.payload as? ChartTrackerPayload ?: return@mapNotNull null
            annotation.id to resolveChartTrackerHighlight(tracker, counterContext.valueFor(tracker))
        }.toMap()

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
            ->
                FreehandPayload(
                    points = points,
                    argb = argb,
                    strokeWidth = strokeWidth,
                    pressureEnabled = pressureEnabled,
                )
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
            else -> return null
        }
    return PatternAnnotation(
        layerId = targetLayerId,
        page = page,
        kind = kind,
        payload = annotationPayload,
        zIndex = zIndex,
    )
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

private const val DEFAULT_SIMPLIFICATION_TOLERANCE = 0.001f
private const val DUPLICATE_OFFSET = 0.02f
private const val HISTORY_LIMIT = 50
private const val MAX_CHART_DIMENSION = 999
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
