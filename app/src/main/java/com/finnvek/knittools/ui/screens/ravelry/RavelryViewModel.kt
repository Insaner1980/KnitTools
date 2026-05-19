package com.finnvek.knittools.ui.screens.ravelry

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.auth.RavelryAuthManager
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.remote.PatternSearchParams
import com.finnvek.knittools.data.remote.PatternSearchResult
import com.finnvek.knittools.data.remote.RavelryHttpException
import com.finnvek.knittools.data.remote.TransientRavelryException
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.RavelryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class SearchFilters(
    val craft: String = "knitting",
    val availability: String? = null,
    val category: String? = null,
    val weight: String? = null,
    val difficultyFrom: Int? = null,
    val difficultyTo: Int? = null,
)

enum class RavelrySearchError {
    Network,
    RateLimited,
    Authentication,
    ServiceUnavailable,
    Unknown,
}

enum class PatternSaveResult {
    Saved,
    Failed,
}

private data class SubmittedRavelrySearch(
    val query: String,
    val filters: SearchFilters,
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

        private val _submittedQuery = MutableStateFlow("")
        val submittedQuery: StateFlow<String> = _submittedQuery.asStateFlow()

        private val _hasSubmittedSearch = MutableStateFlow(false)
        val hasSubmittedSearch: StateFlow<Boolean> = _hasSubmittedSearch.asStateFlow()

        private val _searchResults = MutableStateFlow<List<PatternSearchResult>>(emptyList())
        val searchResults: StateFlow<List<PatternSearchResult>> = _searchResults.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _searchError = MutableStateFlow<RavelrySearchError?>(null)
        val searchError: StateFlow<RavelrySearchError?> = _searchError.asStateFlow()

        private val _detailError = MutableStateFlow<RavelrySearchError?>(null)
        val detailError: StateFlow<RavelrySearchError?> = _detailError.asStateFlow()

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

        private val _upgradeToPro = MutableSharedFlow<Unit>()
        val upgradeToPro = _upgradeToPro.asSharedFlow()

        private val _patternSaveResults = MutableSharedFlow<PatternSaveResult>()
        val patternSaveResults = _patternSaveResults.asSharedFlow()

        val savedPatterns: StateFlow<List<SavedPattern>> =
            repository.getSavedPatterns().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        private var currentPage = 1
        private var totalPages = 1
        private var isSaveInFlight = false
        private var activeSearch: SubmittedRavelrySearch? = null
        private var searchJob: Job? = null
        private var searchRequestId = 0L

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)

        fun createSignInUri(): Uri = authManager.createOAuthUri()

        fun signOut() {
            authManager.signOut()
        }

        fun updateQuery(query: String) {
            _searchQuery.value = query
            _searchError.value = null
        }

        fun updateFilters(filters: SearchFilters) {
            _filters.value = filters
            _searchError.value = null
        }

        fun search() {
            val query = _searchQuery.value.trim()
            if (query.isBlank()) {
                clearSearchState()
                return
            }
            val submittedSearch = SubmittedRavelrySearch(query = query, filters = _filters.value)
            activeSearch = submittedSearch
            _submittedQuery.value = query
            _hasSubmittedSearch.value = true
            _searchResults.value = emptyList()
            currentPage = 1
            totalPages = 1
            startPageLoad(page = 1, replaceResults = true, submittedSearch = submittedSearch, cancelCurrent = true)
        }

        fun loadMore() {
            val submittedSearch = activeSearch ?: return
            if (submittedSearch.matchesCurrentDraft() && currentPage < totalPages && !_isLoading.value) {
                startPageLoad(
                    page = currentPage + 1,
                    replaceResults = false,
                    submittedSearch = submittedSearch,
                    cancelCurrent = false,
                )
            }
        }

        private fun clearSearchState() {
            searchRequestId += 1
            searchJob?.cancel()
            activeSearch = null
            currentPage = 1
            totalPages = 1
            _submittedQuery.value = ""
            _hasSubmittedSearch.value = false
            _searchResults.value = emptyList()
            _searchError.value = null
            _isLoading.value = false
        }

        private fun startPageLoad(
            page: Int,
            replaceResults: Boolean,
            submittedSearch: SubmittedRavelrySearch,
            cancelCurrent: Boolean,
        ) {
            val requestId = searchRequestId + 1
            searchRequestId = requestId
            if (cancelCurrent) {
                searchJob?.cancel()
            }
            searchJob =
                viewModelScope.launch {
                    _isLoading.value = true
                    _searchError.value = null
                    try {
                        val response =
                            repository.searchPatterns(
                                PatternSearchParams(
                                    query = submittedSearch.query,
                                    craft = submittedSearch.filters.craft,
                                    availability = submittedSearch.filters.availability,
                                    pc = submittedSearch.filters.category,
                                    weight = submittedSearch.filters.weight,
                                    difficultyFrom = submittedSearch.filters.difficultyFrom,
                                    difficultyTo = submittedSearch.filters.difficultyTo,
                                    page = page,
                                ),
                            )
                        if (!shouldApplySearchResult(requestId, submittedSearch)) return@launch
                        totalPages = response.paginator?.pageCount ?: 1
                        currentPage = page
                        if (replaceResults) {
                            _searchResults.value = response.patterns
                        } else {
                            _searchResults.update { current -> current + response.patterns }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (shouldApplySearchResult(requestId, submittedSearch)) {
                            _searchError.value = e.toSearchError()
                        }
                    } finally {
                        if (requestId == searchRequestId) {
                            _isLoading.value = false
                        }
                    }
                }
        }

        private fun Exception.toSearchError(): RavelrySearchError =
            when (this) {
                is RavelryHttpException ->
                    when (statusCode) {
                        401, 403 -> RavelrySearchError.Authentication
                        429 -> RavelrySearchError.RateLimited
                        in 500..599 -> RavelrySearchError.ServiceUnavailable
                        else -> RavelrySearchError.Unknown
                    }

                is TransientRavelryException -> RavelrySearchError.ServiceUnavailable
                is IOException -> RavelrySearchError.Network
                else -> RavelrySearchError.Unknown
            }

        private fun shouldApplySearchResult(
            requestId: Long,
            submittedSearch: SubmittedRavelrySearch,
        ): Boolean =
            requestId == searchRequestId &&
                activeSearch == submittedSearch &&
                submittedSearch.matchesCurrentDraft()

        private fun SubmittedRavelrySearch.matchesCurrentDraft(): Boolean =
            _searchQuery.value.trim() == query &&
                _filters.value == filters

        fun loadDetail(patternId: Int) {
            viewModelScope.launch {
                _isDetailLoading.value = true
                _patternDetail.value = null
                _isPatternSaved.value = false
                _detailError.value = null
                try {
                    val detail = repository.getPatternDetail(patternId)
                    _patternDetail.value = detail
                    _isPatternSaved.value = repository.isPatternSaved(patternId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _detailError.value = e.toSearchError()
                } finally {
                    _isDetailLoading.value = false
                }
            }
        }

        fun savePattern() {
            val detail = _patternDetail.value ?: return
            if (_isPatternSaved.value || isSaveInFlight) return
            isSaveInFlight = true
            viewModelScope.launch {
                try {
                    repository.savePattern(detail)
                    _isPatternSaved.value = true
                    _patternSaveResults.emit(PatternSaveResult.Saved)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _patternSaveResults.emit(PatternSaveResult.Failed)
                } finally {
                    isSaveInFlight = false
                }
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
                if (!isPro && repository.getActiveProjectCount() >= 1) {
                    _upgradeToPro.emit(Unit)
                    return@launch
                }
                repository.createProjectFromPattern(detail)?.let { projectId ->
                    _navigateToProject.emit(projectId)
                }
            }
        }
    }
