package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
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
import javax.inject.Inject

enum class PatternAnnotationLoadError {
    NONE,
    READ_FAILED,
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

        val uiState: StateFlow<PatternAnnotationUiState> =
            combine(layers, pageAnnotations, currentPage, layerVisibility, loadError) {
                selection,
                annotations,
                page,
                visibility,
                error,
                ->
                PatternAnnotationUiState(
                    owner = selection.owner,
                    currentPage = page,
                    masterLayerVisible = visibility.master,
                    projectLayerVisible = visibility.project,
                    masterAnnotations = annotations.master,
                    projectAnnotations = annotations.project,
                    editableLayerId = selection.editableLayerId,
                    loadError = error,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatternAnnotationUiState(owner = routeOwner),
            )

        fun setCurrentPage(page: Int) {
            require(page >= 0) { "Pattern annotation page must be non-negative" }
            currentPage.value = page
        }

        fun setMasterLayerVisible(visible: Boolean) {
            masterLayerVisible.value = visible
        }

        fun setProjectLayerVisible(visible: Boolean) {
            projectLayerVisible.value = visible
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
