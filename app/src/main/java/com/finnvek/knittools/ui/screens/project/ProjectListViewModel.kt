package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.domain.model.parseYarnCardIds
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueKnittingProject(
    val projectId: Long,
    val name: String,
    val count: Int,
    val totalMinutes: Int,
    val sectionName: String?,
    val targetRows: Int?,
    val craftType: CraftType,
    val mainCounterLabelType: MainCounterLabelType,
    val mainCounterCustomLabel: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectListViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
        private val yarnCardRepository: YarnCardRepository,
        private val photoRepository: ProgressPhotoRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val preferencesManager: PreferencesManager,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        // === Preferences ===

        val showCompleted: StateFlow<Boolean> =
            preferencesManager.preferences
                .map { it.showCompletedProjects }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val sortOrder: StateFlow<ProjectSortOrder> =
            preferencesManager.preferences
                .map { it.projectSortOrder }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectSortOrder.DEFAULT)

        // === Lajittelutietoiset projektilistaukset ===

        val activeProjects: StateFlow<List<CounterProject>> =
            sortOrder
                .flatMapLatest { order ->
                    repository.getActiveProjects(order)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val completedProjects: StateFlow<List<CounterProject>> =
            sortOrder
                .flatMapLatest { order ->
                    repository.getCompletedProjects(order)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)

        // === Multi-select ===

        private val _isMultiSelectMode = MutableStateFlow(false)
        val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

        private val _selectedProjectIds = MutableStateFlow<Set<Long>>(emptySet())
        val selectedProjectIds: StateFlow<Set<Long>> = _selectedProjectIds.asStateFlow()

        // === Jatka neulomista ===

        private val _continueKnittingProject = MutableStateFlow<ContinueKnittingProject?>(null)
        val continueKnittingProject: StateFlow<ContinueKnittingProject?> = _continueKnittingProject.asStateFlow()

        private val _projectYarnNames = MutableStateFlow<Map<Long, String>>(emptyMap())
        val projectYarnNames: StateFlow<Map<Long, String>> = _projectYarnNames.asStateFlow()

        private val _projectPhotoCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
        val projectPhotoCounts: StateFlow<Map<Long, Int>> = _projectPhotoCounts.asStateFlow()

        private val _projectPatternNames = MutableStateFlow<Map<Long, String>>(emptyMap())
        val projectPatternNames: StateFlow<Map<Long, String>> = _projectPatternNames.asStateFlow()

        private val _projectHasNotes = MutableStateFlow<Set<Long>>(emptySet())
        val projectHasNotes: StateFlow<Set<Long>> = _projectHasNotes.asStateFlow()

        private val _navigateToProject = MutableSharedFlow<Long>()
        val navigateToProject: SharedFlow<Long> = _navigateToProject.asSharedFlow()

        private val _upgradeToPro = MutableSharedFlow<Unit>()
        val upgradeToPro: SharedFlow<Unit> = _upgradeToPro.asSharedFlow()

        init {
            viewModelScope.launch {
                activeProjects.collect { projects ->
                    updateContinueKnitting(projects)
                    updateYarnNames(projects)
                    updatePhotoCounts(projects)
                    updatePatternNames(projects)
                    updateHasNotes(projects)
                }
            }
        }

        // === Preferences-toiminnot ===

        fun toggleShowCompleted() {
            viewModelScope.launch {
                preferencesManager.setShowCompletedProjects(!showCompleted.value)
            }
        }

        fun setSortOrder(order: ProjectSortOrder) {
            viewModelScope.launch {
                preferencesManager.setProjectSortOrder(order)
            }
        }

        // === Multi-select-toiminnot ===

        fun enterMultiSelectMode(initialProjectId: Long? = null) {
            _isMultiSelectMode.value = true
            _selectedProjectIds.value = if (initialProjectId != null) setOf(initialProjectId) else emptySet()
        }

        fun exitMultiSelectMode() {
            _isMultiSelectMode.value = false
            _selectedProjectIds.value = emptySet()
        }

        fun toggleProjectSelection(id: Long) {
            _selectedProjectIds.update { current ->
                if (id in current) current - id else current + id
            }
        }

        fun selectAllProjects() {
            _selectedProjectIds.value = activeProjects.value.map { it.id }.toSet()
        }

        fun completeSelectedProjects() {
            viewModelScope.launch {
                val ids = _selectedProjectIds.value
                ids.forEach { id ->
                    val project = repository.getProject(id) ?: return@forEach
                    repository.archiveProject(
                        id = id,
                        totalRows = project.count,
                        completedAt = System.currentTimeMillis(),
                    )
                }
                exitMultiSelectMode()
            }
        }

        fun deleteSelectedProjects() {
            viewModelScope.launch {
                val ids = _selectedProjectIds.value
                ids.forEach { id ->
                    repository.deleteProject(id)
                }
                exitMultiSelectMode()
            }
        }

        // === Projektitoiminnot ===

        private suspend fun updateContinueKnitting(projects: List<CounterProject>) {
            val candidate = projects.firstOrNull { it.count > 0 }
            _continueKnittingProject.value =
                if (candidate != null) {
                    val totalMin = repository.getTotalMinutesForProject(candidate.id)
                    ContinueKnittingProject(
                        projectId = candidate.id,
                        name = candidate.name,
                        count = candidate.count,
                        totalMinutes = totalMin,
                        sectionName = candidate.sectionName,
                        targetRows = candidate.targetRows,
                        craftType = candidate.craftType,
                        mainCounterLabelType = candidate.mainCounterLabelType,
                        mainCounterCustomLabel = candidate.mainCounterCustomLabel,
                    )
                } else {
                    null
                }
        }

        private suspend fun updateYarnNames(projects: List<CounterProject>) {
            val yarnMap = mutableMapOf<Long, String>()
            val allYarnIds =
                projects
                    .flatMap { p ->
                        parseYarnCardIds(p.yarnCardIds)
                    }.distinct()
            if (allYarnIds.isNotEmpty()) {
                val cards = yarnCardRepository.getCards(allYarnIds).associateBy { it.id }
                projects.forEach { p ->
                    val ids = parseYarnCardIds(p.yarnCardIds)
                    val firstCard = ids.firstNotNullOfOrNull { cards[it] }
                    if (firstCard != null) {
                        yarnMap[p.id] = firstCard.displayName(::fallbackYarnCardName)
                    }
                }
            }
            _projectYarnNames.value = yarnMap
        }

        private fun fallbackYarnCardName(id: Long): String = context.getString(R.string.yarn_card_number_fallback, id)

        private suspend fun updatePhotoCounts(projects: List<CounterProject>) {
            _projectPhotoCounts.value =
                photoRepository
                    .getPhotoCountsByProjectIds(projects.map { it.id })
                    .filterValues { it > 0 }
        }

        private suspend fun updatePatternNames(projects: List<CounterProject>) {
            val nameMap = mutableMapOf<Long, String>()
            projects.forEach { p ->
                p.patternName?.takeIf { it.isNotBlank() }?.let {
                    nameMap[p.id] = it
                    return@forEach
                }
                val patternId = p.linkedPatternId ?: return@forEach
                savedPatternRepository.getById(patternId)?.let { nameMap[p.id] = it.name }
            }
            _projectPatternNames.value = nameMap
        }

        private fun updateHasNotes(projects: List<CounterProject>) {
            _projectHasNotes.value = projects.filter { it.notes.isNotBlank() }.map { it.id }.toSet()
        }

        fun createProject() {
            viewModelScope.launch {
                val count = repository.getProjectCount()
                createProjectInternal(
                    name = context.getString(R.string.new_project_name_format, count + 1),
                    craftType = CraftType.KNITTING,
                    mainCounterLabelType = CraftType.KNITTING.defaultMainCounterLabelType(),
                    mainCounterCustomLabel = null,
                )
            }
        }

        fun createProject(
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
        ) {
            viewModelScope.launch {
                createProjectInternal(
                    name = name,
                    craftType = craftType,
                    mainCounterLabelType = mainCounterLabelType,
                    mainCounterCustomLabel = mainCounterCustomLabel,
                )
            }
        }

        private suspend fun createProjectInternal(
            name: String,
            craftType: CraftType,
            mainCounterLabelType: MainCounterLabelType,
            mainCounterCustomLabel: String?,
        ) {
            if (!isPro && repository.getActiveProjectCount() >= 1) {
                _upgradeToPro.emit(Unit)
                return
            }
            val id =
                repository.createProject(
                    name = name,
                    craftType = craftType,
                    mainCounterLabelType = mainCounterLabelType,
                    mainCounterCustomLabel = mainCounterCustomLabel,
                ) ?: return
            _navigateToProject.emit(id)
        }

        fun archiveProject(id: Long) {
            viewModelScope.launch {
                val project = repository.getProject(id) ?: return@launch
                repository.archiveProject(
                    id = id,
                    totalRows = project.count,
                    completedAt = System.currentTimeMillis(),
                )
            }
        }

        fun deleteProject(id: Long) {
            viewModelScope.launch {
                repository.deleteProject(id)
            }
        }

        fun renameProject(
            id: Long,
            newName: String,
        ) {
            viewModelScope.launch {
                repository.updateProjectName(id, newName)
            }
        }

        fun reactivateProject(id: Long) {
            viewModelScope.launch {
                repository.reactivateProject(id)
            }
        }
    }
