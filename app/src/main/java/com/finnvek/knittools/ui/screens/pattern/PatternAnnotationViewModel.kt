package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.calculator.isPointNearStroke
import com.finnvek.knittools.domain.calculator.simplifyFreehandPoints
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationLimits
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
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
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnnotationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val counterRepository: CounterRepository,
        private val layerRepository: PatternAnnotationLayerRepository,
        private val annotationRepository: PatternAnnotationRepository,
    ) : ViewModel() {
        private val routeOwner = savedStateHandle.requirePatternAnnotationOwner()
        private val currentPage = MutableStateFlow(0)
        private val masterLayerVisible = MutableStateFlow(true)
        private val projectLayerVisible = MutableStateFlow(true)
        private val layerReadFailed = MutableStateFlow(false)
        private val annotationReadFailed = MutableStateFlow(false)
        private val interaction = MutableStateFlow(PatternAnnotationInteractionState())
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
            combine(loadError, interaction) { readError, interactionState ->
                PatternAnnotationFeedback(readError, interactionState)
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
                PatternAnnotationUiState(
                    owner = selection.owner,
                    currentPage = page,
                    masterLayerVisible = visibility.master,
                    projectLayerVisible = visibility.project,
                    masterAnnotations = annotations.master,
                    projectAnnotations = annotations.project,
                    editableLayerId = selection.editableLayerId,
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
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatternAnnotationUiState(owner = routeOwner),
            )

        fun setCurrentPage(page: Int) {
            require(page >= 0) { "Pattern annotation page must be non-negative" }
            if (page != currentPage.value && interaction.value.draftStroke != null) {
                commitStroke(DEFAULT_SIMPLIFICATION_TOLERANCE)
            }
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
            interaction.update { it.copy(isSaving = true, writeError = PatternAnnotationWriteError.NONE) }
            viewModelScope.launch {
                runCatching { annotationRepository.insertAnnotation(annotation) }
                    .onSuccess {
                        interaction.update { state ->
                            state.copy(
                                draftStroke = state.draftStroke.takeUnless { it == originalDraft },
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

        fun cancelStroke() {
            interaction.update { state ->
                if (state.isSaving) state else state.copy(draftStroke = null)
            }
        }

        fun eraseStrokeAt(
            point: NormalizedPatternPoint,
            tolerance: Float = PatternAnnotationTokens.ERASER_HIT_TOLERANCE,
        ) {
            val hit =
                editableAnnotations()
                    .sortedWith(compareByDescending<PatternAnnotation> { it.zIndex }.thenByDescending { it.id })
                    .firstOrNull { annotation ->
                        val payload = annotation.payload as? FreehandPayload ?: return@firstOrNull false
                        isPointNearStroke(point, payload.points, tolerance)
                    } ?: return
            viewModelScope.launch {
                runCatching { annotationRepository.deleteAnnotation(hit.id) }
                    .onFailure {
                        interaction.update { state ->
                            state.copy(writeError = PatternAnnotationWriteError.WRITE_FAILED)
                        }
                    }
            }
        }

        fun clearWriteError() {
            interaction.update { it.copy(writeError = PatternAnnotationWriteError.NONE) }
        }

        private fun editableAnnotations(): List<PatternAnnotation> =
            when (uiState.value.owner) {
                is PatternAnnotationOwner.Project -> uiState.value.projectAnnotations
                is PatternAnnotationOwner.SavedPattern -> uiState.value.masterAnnotations
            }

        private fun nextEditableZIndex(): Long =
            editableAnnotations().maxOfOrNull(PatternAnnotation::zIndex)?.plus(1L) ?: 0L

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
                            val documentKey =
                                project.linkedPatternId?.let(PatternAnnotationDocumentKey::savedPattern)
                                    ?: PatternAnnotationDocumentKey.legacyProject(project.id)
                            val currentOwner = PatternAnnotationOwner.Project(project.id, documentKey)
                            layerRepository.observeLayers(currentOwner).mapLatest { projectLayers ->
                                val projectLayer = projectLayers.firstOrNull { it.isActive }
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
) {
    fun styleForTool(): PatternStrokeStyle? =
        when (activeTool) {
            PatternAnnotationTool.PEN -> PatternStrokeStyle(penArgb, penStrokeWidth)
            PatternAnnotationTool.HIGHLIGHTER -> PatternStrokeStyle(highlighterArgb, highlighterStrokeWidth)
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
)

private fun PatternStrokeDraft.toAnnotation(
    layerId: Long?,
    page: Int,
    zIndex: Long,
): PatternAnnotation? {
    val targetLayerId = layerId ?: return null
    val kind =
        when (tool) {
            PatternAnnotationTool.PEN -> PatternAnnotationKind.FREEHAND
            PatternAnnotationTool.HIGHLIGHTER -> PatternAnnotationKind.HIGHLIGHTER
            else -> return null
        }
    return PatternAnnotation(
        layerId = targetLayerId,
        page = page,
        kind = kind,
        payload =
            FreehandPayload(
                points = points,
                argb = argb,
                strokeWidth = strokeWidth,
                pressureEnabled = pressureEnabled,
            ),
        zIndex = zIndex,
    )
}

private const val DEFAULT_SIMPLIFICATION_TOLERANCE = 0.001f

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
