package com.finnvek.knittools.ui.screens.ravelry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchResult
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.RavelryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchFilters(
    val craft: String = "knitting",
    val availability: String? = null,
    val category: String? = null,
    val weight: String? = null,
    val difficultyFrom: Int? = null,
    val difficultyTo: Int? = null,
)

@HiltViewModel
class RavelryViewModel
    @Inject
    constructor(
        private val repository: RavelryRepository,
        private val proManager: ProManager,
        private val authManager: RavelryAuthManager,
    ) : ViewModel() {
        val isAuthenticated: StateFlow<Boolean> = authManager.isAuthenticated

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val _searchResults = MutableStateFlow<List<PatternSearchResult>>(emptyList())
        val searchResults: StateFlow<List<PatternSearchResult>> = _searchResults.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        private val _filters = MutableStateFlow(SearchFilters())
        val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

        private val _patternDetail = MutableStateFlow<PatternDetail?>(null)
        val patternDetail: StateFlow<PatternDetail?> = _patternDetail.asStateFlow()

        private val _isDetailLoading = MutableStateFlow(false)
        val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

        private val _isPatternSaved = MutableStateFlow(false)
        val isPatternSaved: StateFlow<Boolean> = _isPatternSaved.asStateFlow()

        private val _navigateToProject = MutableSharedFlow<Long>()
        val navigateToProject = _navigateToProject.asSharedFlow()

        val savedPatterns: StateFlow<List<SavedPatternEntity>> =
            repository.getSavedPatterns().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private var currentPage = 1
        private var totalPages = 1

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)

        fun startSignIn(activity: android.app.Activity) {
            authManager.startOAuthFlow(activity)
        }

        fun signOut() {
            authManager.signOut()
        }

        fun updateQuery(query: String) {
            _searchQuery.value = query
        }

        fun updateFilters(filters: SearchFilters) {
            _filters.value = filters
        }

        fun search() {
            currentPage = 1
            _searchResults.value = emptyList()
            loadPage()
        }

        fun loadMore() {
            if (currentPage < totalPages && !_isLoading.value) {
                currentPage++
                loadPage()
            }
        }

        private fun loadPage() {
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    val filters = _filters.value
                    val response =
                        repository.searchPatterns(
                            com.finnvek.knittools.data.remote.PatternSearchParams(
                                query = _searchQuery.value,
                                craft = filters.craft,
                                availability = filters.availability,
                                pc = filters.category,
                                weight = filters.weight,
                                difficultyFrom = filters.difficultyFrom,
                                difficultyTo = filters.difficultyTo,
                                page = currentPage,
                            ),
                        )
                    totalPages = response.paginator?.pageCount ?: 1
                    _searchResults.update { current -> current + response.patterns }
                } catch (e: Exception) {
                    _error.value = e.message
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun loadDetail(patternId: Int) {
            viewModelScope.launch {
                _isDetailLoading.value = true
                _patternDetail.value = null
                _isPatternSaved.value = false
                try {
                    val detail = repository.getPatternDetail(patternId)
                    _patternDetail.value = detail
                    _isPatternSaved.value = repository.isPatternSaved(patternId)
                } catch (e: Exception) {
                    _error.value = e.message
                } finally {
                    _isDetailLoading.value = false
                }
            }
        }

        fun savePattern() {
            val detail = _patternDetail.value ?: return
            viewModelScope.launch {
                repository.savePattern(detail)
                _isPatternSaved.value = true
            }
        }

        fun deleteSavedPattern(id: Long) {
            viewModelScope.launch {
                repository.deleteSavedPattern(id)
            }
        }

        // === Multi-select (SavedTab) ===

        private val _isSavedSelectMode = MutableStateFlow(false)
        val isSavedSelectMode: StateFlow<Boolean> = _isSavedSelectMode.asStateFlow()

        private val _selectedSavedIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedSavedIds: StateFlow<Set<Long>> = _selectedSavedIds.asStateFlow()

        fun enterSavedSelectMode(initialId: Long) {
            _isSavedSelectMode.value = true
            _selectedSavedIds.value = setOf(initialId)
        }

        fun exitSavedSelectMode() {
            _isSavedSelectMode.value = false
            _selectedSavedIds.value = emptySet()
        }

        fun toggleSavedSelection(id: Long) {
            _selectedSavedIds.update { current ->
                val next = if (id in current) current - id else current + id
                if (next.isEmpty()) {
                    _isSavedSelectMode.value = false
                }
                next
            }
        }

        fun selectAllSaved(visibleIds: List<Long>) {
            _selectedSavedIds.value = visibleIds.toSet()
        }

        fun deleteSelectedSaved() {
            viewModelScope.launch {
                val ids = _selectedSavedIds.value.toList()
                ids.forEach { repository.deleteSavedPattern(it) }
                exitSavedSelectMode()
            }
        }

        fun createProjectFromPattern() {
            val detail = _patternDetail.value ?: return
            viewModelScope.launch {
                val projectId = repository.createProjectFromPattern(detail)
                _navigateToProject.emit(projectId)
            }
        }
    }
