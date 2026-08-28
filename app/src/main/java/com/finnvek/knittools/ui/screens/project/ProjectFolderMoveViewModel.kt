package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.isSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectFolderMoveViewModel
    @Inject
    constructor(
        private val repository: ProjectFolderRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ProjectFoldersState())
        val state = _state.asStateFlow()
        private val movedEventChannel = Channel<Long>(Channel.BUFFERED)
        val movedEvents = movedEventChannel.receiveAsFlow()
        private var projectId: Long? = null
        private var observation: Job? = null

        fun prepareProject(id: Long) {
            if (projectId != id) {
                projectId = id
                _state.value = ProjectFoldersState(isMutating = _state.value.isMutating)
            }
            retryLoading()
        }

        fun retryLoading() {
            observation?.cancel()
            observation =
                viewModelScope.launch {
                    repository
                        .observeOrganization(
                            onReadFailure = { _state.update { it.copy(readFailed = true) } },
                        ).collect { snapshot ->
                            _state.update { it.copy(snapshot = snapshot, isLoading = false, readFailed = false) }
                        }
                }
        }

        fun stopObserving() {
            observation?.cancel()
        }

        fun moveTo(folderId: Long?) {
            val id = projectId ?: return
            if (_state.value.isMutating) return
            _state.update { it.copy(isMutating = true, mutationError = null) }
            viewModelScope.launch {
                try {
                    val result = repository.moveProjects(setOf(id), folderId)
                    if (result.isSuccess) {
                        movedEventChannel.send(id)
                    } else if (projectId == id) {
                        _state.update { it.copy(mutationError = result) }
                    }
                } finally {
                    _state.update { it.copy(isMutating = false) }
                }
            }
        }
    }
