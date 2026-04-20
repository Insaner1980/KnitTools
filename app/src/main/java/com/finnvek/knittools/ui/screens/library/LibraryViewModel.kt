package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val savedPatternRepository: SavedPatternRepository,
        private val yarnCardRepository: YarnCardRepository,
        private val progressPhotoRepository: ProgressPhotoRepository,
        counterRepository: CounterRepository,
    ) : ViewModel() {
        // Countit Library-hubille
        val savedPatternCount: Flow<Int> = savedPatternRepository.getCount()
        val yarnCardCount: Flow<Int> = yarnCardRepository.getCardCount()
        val photoCount: Flow<Int> = progressPhotoRepository.getAllPhotoCount()

        // Listat alanäytöille
        val savedPatterns: Flow<List<SavedPatternEntity>> = savedPatternRepository.getAll()
        val yarnCards: Flow<List<YarnCardEntity>> = yarnCardRepository.getAllCards()
        val allPhotos: Flow<List<ProgressPhotoEntity>> = progressPhotoRepository.getAllPhotos()
        val allProjects: Flow<List<CounterProjectEntity>> = counterRepository.getAllProjects()
        val activeProjectNames: Flow<Map<Long, String>> =
            counterRepository
                .getAllProjects()
                .map { projects ->
                    projects
                        .filterNot { it.isCompleted }
                        .associate { it.id to it.name }
                }

        fun deletePhoto(photo: ProgressPhotoEntity) {
            viewModelScope.launch {
                progressPhotoRepository.deletePhoto(photo)
            }
        }

        // === Multi-select (AllPhotosScreen) ===

        private val _isPhotoSelectMode = MutableStateFlow(false)
        val isPhotoSelectMode: StateFlow<Boolean> = _isPhotoSelectMode.asStateFlow()

        private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

        fun enterPhotoSelectMode(initialPhotoId: Long) {
            _isPhotoSelectMode.value = true
            _selectedPhotoIds.value = setOf(initialPhotoId)
        }

        fun exitPhotoSelectMode() {
            _isPhotoSelectMode.value = false
            _selectedPhotoIds.value = emptySet()
        }

        fun togglePhotoSelection(id: Long) {
            _selectedPhotoIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    _isPhotoSelectMode.value = false
                }
                next
            }
        }

        fun selectAllPhotos(visibleIds: List<Long>) {
            _selectedPhotoIds.value = visibleIds.toSet()
        }

        fun deleteSelectedPhotos() {
            viewModelScope.launch {
                val ids = _selectedPhotoIds.value.toList()
                progressPhotoRepository.deletePhotos(ids)
                exitPhotoSelectMode()
            }
        }

        // === Multi-select (SavedPatternsScreen) ===

        private val _isPatternSelectMode = MutableStateFlow(false)
        val isPatternSelectMode: StateFlow<Boolean> = _isPatternSelectMode.asStateFlow()

        private val _selectedPatternIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedPatternIds: StateFlow<Set<Long>> = _selectedPatternIds.asStateFlow()

        fun enterPatternSelectMode(initialPatternId: Long) {
            _isPatternSelectMode.value = true
            _selectedPatternIds.value = setOf(initialPatternId)
        }

        fun exitPatternSelectMode() {
            _isPatternSelectMode.value = false
            _selectedPatternIds.value = emptySet()
        }

        fun togglePatternSelection(id: Long) {
            _selectedPatternIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    _isPatternSelectMode.value = false
                }
                next
            }
        }

        fun selectAllPatterns(visibleIds: List<Long>) {
            _selectedPatternIds.value = visibleIds.toSet()
        }

        fun deleteSelectedPatterns() {
            viewModelScope.launch {
                val ids = _selectedPatternIds.value.toList()
                savedPatternRepository.deleteByIds(ids)
                exitPatternSelectMode()
            }
        }

        // === Multi-select (MyYarnScreen) ===

        private val _isYarnSelectMode = MutableStateFlow(false)
        val isYarnSelectMode: StateFlow<Boolean> = _isYarnSelectMode.asStateFlow()

        private val _selectedYarnIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedYarnIds: StateFlow<Set<Long>> = _selectedYarnIds.asStateFlow()

        fun enterYarnSelectMode(initialYarnId: Long) {
            _isYarnSelectMode.value = true
            _selectedYarnIds.value = setOf(initialYarnId)
        }

        fun exitYarnSelectMode() {
            _isYarnSelectMode.value = false
            _selectedYarnIds.value = emptySet()
        }

        fun toggleYarnSelection(id: Long) {
            _selectedYarnIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    _isYarnSelectMode.value = false
                }
                next
            }
        }

        fun selectAllYarn(visibleIds: List<Long>) {
            _selectedYarnIds.value = visibleIds.toSet()
        }

        fun deleteSelectedYarn() {
            viewModelScope.launch {
                val ids = _selectedYarnIds.value.toList()
                yarnCardRepository.deleteCards(ids)
                exitYarnSelectMode()
            }
        }
    }
