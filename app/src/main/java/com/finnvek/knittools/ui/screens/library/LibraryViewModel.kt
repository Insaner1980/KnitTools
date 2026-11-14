package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternDeleteResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.retryOnRepositoryReadFailure
import com.finnvek.knittools.ui.screens.yarncard.ManualYarnCardInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val savedPatternRepository: SavedPatternRepository,
        private val yarnCardRepository: YarnCardRepository,
        private val progressPhotoRepository: ProgressPhotoRepository,
        private val proManager: ProManager,
        counterRepository: CounterRepository,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        val proState = proManager.proState
        val proAccessReady get() = proManager.initialStateReady
        private val _isSavingYarnCard = MutableStateFlow(false)
        val isSavingYarnCard = _isSavingYarnCard.asStateFlow()
        private val _yarnSaveSuccessId = MutableStateFlow(0L)
        val yarnSaveSuccessId = _yarnSaveSuccessId.asStateFlow()
        private var pendingManualYarnCard: ManualYarnCardInput? = null
        private val _isPhotoSelectMode = MutableStateFlow(false)
        val isPhotoSelectMode: StateFlow<Boolean> = _isPhotoSelectMode.asStateFlow()

        private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()
        private val _photoDeleteErrorId = MutableStateFlow(0L)
        val photoDeleteErrorId: StateFlow<Long> = _photoDeleteErrorId.asStateFlow()

        private val _isPatternSelectMode = MutableStateFlow(false)
        val isPatternSelectMode: StateFlow<Boolean> = _isPatternSelectMode.asStateFlow()

        private val _selectedPatternIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedPatternIds: StateFlow<Set<Long>> = _selectedPatternIds.asStateFlow()
        private val _patternDeleteErrorId = MutableStateFlow(0L)
        val patternDeleteErrorId: StateFlow<Long> = _patternDeleteErrorId.asStateFlow()

        private val _isYarnSelectMode = MutableStateFlow(false)
        val isYarnSelectMode: StateFlow<Boolean> = _isYarnSelectMode.asStateFlow()

        private val _selectedYarnIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedYarnIds: StateFlow<Set<Long>> = _selectedYarnIds.asStateFlow()
        private val _yarnDeleteErrorId = MutableStateFlow(0L)
        val yarnDeleteErrorId: StateFlow<Long> = _yarnDeleteErrorId.asStateFlow()
        private val _yarnSaveErrorId = MutableStateFlow(0L)
        val yarnSaveErrorId: StateFlow<Long> = _yarnSaveErrorId.asStateFlow()

        // Countit Library-hubille
        val savedPatternCount: Flow<Int> = savedPatternRepository.getCount()
        val yarnCardCount: Flow<Int> = yarnCardRepository.getCardCount()
        val photoCount: Flow<Int> = progressPhotoRepository.getAllPhotoCount()
        val canUseYarnCards: StateFlow<Boolean> =
            proManager
                .hasFeatureFlow(ProFeature.UNLIMITED_YARN)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    proManager.hasFeature(ProFeature.UNLIMITED_YARN),
                )

        // Listat alanäytöille
        val savedPatterns: Flow<List<SavedPattern>> =
            savedPatternRepository.getAll().syncSelectionWithItems(
                selectedIds = _selectedPatternIds,
                selectMode = _isPatternSelectMode,
                itemId = SavedPattern::id,
            )
        val yarnCards: Flow<List<YarnCard>> =
            yarnCardRepository.getAllCards().syncSelectionWithItems(
                selectedIds = _selectedYarnIds,
                selectMode = _isYarnSelectMode,
                itemId = YarnCard::id,
            )
        val allPhotos: Flow<List<ProgressPhoto>> =
            progressPhotoRepository.getAllPhotos().syncSelectionWithItems(
                selectedIds = _selectedPhotoIds,
                selectMode = _isPhotoSelectMode,
                itemId = ProgressPhoto::id,
            )
        val allProjects: Flow<List<CounterProject>> = counterRepository.getAllProjects()
        val activeProjectNames: Flow<Map<Long, String>> =
            counterRepository
                .getAllProjects()
                .map { projects ->
                    projects
                        .filterNot { it.isCompleted }
                        .associate { it.id to it.name }
                }

        fun loadSavedPattern(id: Long): Flow<SavedPattern?> =
            flow { emit(savedPatternRepository.getByIdIfAvailable(id)) }
                .retryOnRepositoryReadFailure()

        fun deletePhoto(photo: ProgressPhoto) {
            viewModelScope.launch {
                try {
                    progressPhotoRepository.deletePhoto(photo)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    _photoDeleteErrorId.value += 1
                }
            }
        }

        // === Multi-select (AllPhotosScreen) ===

        fun enterPhotoSelectMode(initialPhotoId: Long) {
            _isPhotoSelectMode.value = true
            _selectedPhotoIds.value = setOf(initialPhotoId)
        }

        fun exitPhotoSelectMode() {
            _isPhotoSelectMode.value = false
            _selectedPhotoIds.value = emptySet()
        }

        fun togglePhotoSelection(id: Long) {
            toggleSelection(selectedIds = _selectedPhotoIds, selectMode = _isPhotoSelectMode, id = id)
        }

        fun selectAllPhotos(visibleIds: List<Long>) {
            _selectedPhotoIds.value = visibleIds.toSet()
        }

        fun deleteSelectedPhotos() {
            deleteSelected(
                selectedIds = _selectedPhotoIds,
                exitSelectMode = ::exitPhotoSelectMode,
                deleteByIds = progressPhotoRepository::deletePhotos,
                onError = { _photoDeleteErrorId.value += 1 },
            )
        }

        // === Multi-select (SavedPatternsScreen) ===

        fun enterPatternSelectMode(initialPatternId: Long) {
            _isPatternSelectMode.value = true
            _selectedPatternIds.value = setOf(initialPatternId)
        }

        fun exitPatternSelectMode() {
            _isPatternSelectMode.value = false
            _selectedPatternIds.value = emptySet()
        }

        fun togglePatternSelection(id: Long) {
            toggleSelection(selectedIds = _selectedPatternIds, selectMode = _isPatternSelectMode, id = id)
        }

        fun selectAllPatterns(visibleIds: List<Long>) {
            _selectedPatternIds.value = visibleIds.toSet()
        }

        fun deleteSelectedPatterns() {
            deleteSelected(
                selectedIds = _selectedPatternIds,
                exitSelectMode = ::exitPatternSelectMode,
                deleteByIds = savedPatternRepository::deleteByIds,
                onError = { _patternDeleteErrorId.value += 1 },
            )
        }

        @Suppress("ThrowsCount", "kotlin:S3776") // Poisto säilyttää lähdekohtaiset invariantit ja peruutuksen.
        fun deleteSavedPattern(
            id: Long,
            onDeleted: () -> Unit,
        ) {
            viewModelScope.launch {
                val pattern =
                    try {
                        savedPatternRepository.getById(id) ?: return@launch
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        _patternDeleteErrorId.value += 1
                        return@launch
                    }

                if (pattern.isWebPatternCompatible) {
                    val result =
                        try {
                            savedPatternRepository.deleteWebPattern(id)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            _patternDeleteErrorId.value += 1
                            return@launch
                        }
                    when (result) {
                        SavedPatternDeleteResult.Deleted -> onDeleted()
                        SavedPatternDeleteResult.PersistenceFailure -> _patternDeleteErrorId.value += 1
                        SavedPatternDeleteResult.PatternMissing,
                        SavedPatternDeleteResult.NotWebPattern,
                        -> Unit
                    }
                } else {
                    try {
                        savedPatternRepository.deleteById(id)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        _patternDeleteErrorId.value += 1
                        return@launch
                    }
                    onDeleted()
                }
            }
        }

        // === Multi-select (MyYarnScreen) ===

        fun enterYarnSelectMode(initialYarnId: Long) {
            _isYarnSelectMode.value = true
            _selectedYarnIds.value = setOf(initialYarnId)
        }

        fun exitYarnSelectMode() {
            _isYarnSelectMode.value = false
            _selectedYarnIds.value = emptySet()
        }

        fun toggleYarnSelection(id: Long) {
            toggleSelection(selectedIds = _selectedYarnIds, selectMode = _isYarnSelectMode, id = id)
        }

        fun selectAllYarn(visibleIds: List<Long>) {
            _selectedYarnIds.value = visibleIds.toSet()
        }

        fun deleteSelectedYarn() {
            deleteSelected(
                selectedIds = _selectedYarnIds,
                exitSelectMode = ::exitYarnSelectMode,
                deleteByIds = { ids ->
                    if (!yarnCardRepository.deleteCards(ids)) _yarnDeleteErrorId.value += 1
                },
                onError = { _yarnDeleteErrorId.value += 1 },
            )
        }

        fun createManualYarnCard(input: ManualYarnCardInput): Boolean {
            if (_isSavingYarnCard.value) return true
            pendingManualYarnCard = input
            if (!canUseYarnCards.value) return false
            return savePendingManualYarnCard()
        }

        fun retryCreateManualYarnCard(): Boolean {
            if (_isSavingYarnCard.value || !canUseYarnCards.value) return false
            return savePendingManualYarnCard()
        }

        private fun savePendingManualYarnCard(): Boolean {
            if (_isSavingYarnCard.value) return true
            val input = pendingManualYarnCard ?: return false
            val yarnName = input.yarnName.trim()
            if (yarnName.isBlank() || input.quantity < 1) {
                pendingManualYarnCard = null
                return false
            }
            _isSavingYarnCard.value = true

            viewModelScope.launch {
                try {
                    val savedId =
                        yarnCardRepository.saveCard(
                            YarnCard(
                                yarnName = yarnName,
                                brand = input.brand.trim(),
                                quantityInStash = input.quantity,
                                weightCategory = input.weightCategory.trim(),
                                colorName = input.colorName.trim(),
                                colorNumber = input.colorNumber.trim(),
                                dyeLot = input.dyeLot.trim(),
                                status = YarnCardStatus.IN_STASH,
                            ),
                        )
                    if (savedId == null) {
                        _yarnSaveErrorId.value += 1
                    } else {
                        pendingManualYarnCard = null
                        analytics.track(UsageEvent.YARN_CARD_CREATED)
                        _yarnSaveSuccessId.value += 1
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    _yarnSaveErrorId.value += 1
                } finally {
                    _isSavingYarnCard.value = false
                }
            }
            return true
        }

        private fun toggleSelection(
            selectedIds: MutableStateFlow<Set<Long>>,
            selectMode: MutableStateFlow<Boolean>,
            id: Long,
        ) {
            selectedIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    selectMode.value = false
                }
                next
            }
        }

        private fun <T> Flow<List<T>>.syncSelectionWithItems(
            selectedIds: MutableStateFlow<Set<Long>>,
            selectMode: MutableStateFlow<Boolean>,
            itemId: (T) -> Long,
        ): Flow<List<T>> =
            onEach { items ->
                val current = selectedIds.value
                if (current.isEmpty()) return@onEach

                val existingIds = items.map(itemId).toSet()
                val next = current.intersect(existingIds)
                if (next != current) {
                    selectedIds.value = next
                }
                if (next.isEmpty()) {
                    selectMode.value = false
                }
            }

        private fun deleteSelected(
            selectedIds: StateFlow<Set<Long>>,
            exitSelectMode: () -> Unit,
            deleteByIds: suspend (List<Long>) -> Unit,
            onError: () -> Unit = {},
        ) {
            viewModelScope.launch {
                try {
                    val ids = selectedIds.value.toList()
                    if (ids.isNotEmpty()) {
                        deleteByIds(ids)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    onError()
                } finally {
                    exitSelectMode()
                }
            }
        }
    }
