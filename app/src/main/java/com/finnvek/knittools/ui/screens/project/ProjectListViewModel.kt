package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel
    @Inject
    constructor(
        private val repository: CounterRepository,
        private val proManager: ProManager,
    ) : ViewModel() {
        val activeProjects: StateFlow<List<CounterProjectEntity>> =
            repository.getActiveProjects().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        val completedProjects: StateFlow<List<CounterProjectEntity>> =
            repository.getCompletedProjects().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS)

        fun createProject() {
            viewModelScope.launch {
                if (!isPro && repository.getActiveProjectCount() >= 1) return@launch
                val count = repository.getProjectCount()
                repository.createProject("Project ${count + 1}")
            }
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
