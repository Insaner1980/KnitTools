package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.MeasurementNumberFormatter
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.repository.ProjectYarnUsageRepository
import com.finnvek.knittools.repository.YarnUsageResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class YarnUsageEditorState(
    val draft: YarnUsageDraft? = null,
    val busy: Boolean = false,
    val error: YarnUsageResult? = null,
    val deleting: Boolean = false,
    val completed: Boolean = false,
)

@HiltViewModel
class ProjectYarnUsageViewModel
    @Inject
    constructor(
        private val repository: ProjectYarnUsageRepository,
        private val preferencesManager: PreferencesManager,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val mutableEditor = MutableStateFlow(YarnUsageEditorState(draft = savedStateHandle.restoreYarnUsage()))
        val editor = mutableEditor.asStateFlow()
        private val mutableItems = MutableStateFlow<List<ProjectYarnUsageItem>?>(null)
        val items = mutableItems.asStateFlow()
        private var observation: Job? = null
        private var projectId: Long? = null

        fun observe(project: Long) {
            if (projectId == project && observation?.isActive == true) return
            observation?.cancel()
            projectId = project
            if (editor.value.draft?.projectId != project) closeDraft()
            mutableItems.value = null
            observation =
                viewModelScope.launch { repository.observeForProject(project).collect { mutableItems.value = it } }
        }

        fun stopObserving() {
            observation?.cancel()
        }

        suspend fun open(
            item: ProjectYarnUsageItem,
            name: String,
        ) {
            if (editor.value.busy) return
            val project = projectId ?: return
            val unit =
                if (preferencesManager.preferences.first().useImperial) YarnUsageUnit.YARDS else YarnUsageUnit.METERS
            val current = items.value?.firstOrNull { it.key == item.key } ?: item
            val amounts = current.usage?.amounts

            fun input(value: Double?): YarnUsageInput =
                YarnUsageInput(value?.let(MeasurementNumberFormatter::formatEditing).orEmpty(), value)
            val draft =
                YarnUsageDraft(
                    projectId = project,
                    source = current.source,
                    name = current.name.ifBlank { name },
                    usageId = current.usage?.id,
                    revision = current.usage?.updatedAt ?: 0,
                    planned = input(amounts?.plannedMeters),
                    allocated = input(amounts?.allocatedMeters),
                    used = input(amounts?.usedMeters),
                    conversionEnabled = amounts?.metersPerSkein != null,
                    length = input(amounts?.metersPerSkein),
                    weight = input(amounts?.gramsPerSkein),
                ).displayIn(unit)
            mutableEditor.value = YarnUsageEditorState(draft = draft)
            persistDraft()
        }

        fun edit(
            field: YarnUsageField,
            text: String,
            locale: Locale,
        ) = change { it.edit(field, text, locale) }

        fun unit(unit: YarnUsageUnit) = change { it.switchUnit(unit) }

        fun conversion(enabled: Boolean) =
            change { if (enabled) it.copy(conversionEnabled = true) else it.removeConversion() }

        fun closeDraft() {
            if (editor.value.busy) return
            mutableEditor.value = YarnUsageEditorState()
            savedStateHandle.saveYarnUsage(null)
        }

        fun save() {
            val draft = editor.value.draft ?: return
            if (!draft.canSave || editor.value.busy || editor.value.completed) return
            mutate(deleting = false) {
                if (draft.usageId == null) {
                    repository.create(draft.projectId, draft.source, draft.amounts, draft.name)
                } else {
                    repository.update(draft.projectId, draft.usageId, draft.revision, draft.amounts)
                }
            }
        }

        fun delete() {
            val draft = editor.value.draft ?: return
            val id = draft.usageId ?: return
            if (editor.value.busy || editor.value.completed) return
            mutate(deleting = true) { repository.delete(draft.projectId, id, draft.revision) }
        }

        private fun mutate(
            deleting: Boolean,
            operation: suspend () -> YarnUsageResult,
        ) {
            mutableEditor.update { it.copy(busy = true, error = null, deleting = deleting) }
            viewModelScope.launch {
                try {
                    val result = operation()
                    if (result is YarnUsageResult.Created ||
                        result is YarnUsageResult.Updated ||
                        result == YarnUsageResult.Deleted
                    ) {
                        savedStateHandle.saveYarnUsage(null)
                        mutableEditor.update { it.copy(completed = true, busy = false) }
                    } else {
                        mutableEditor.update { it.copy(error = result) }
                    }
                } finally {
                    mutableEditor.update { it.copy(busy = false) }
                }
            }
        }

        private fun change(transform: (YarnUsageDraft) -> YarnUsageDraft) {
            if (editor.value.busy || editor.value.completed) return
            mutableEditor.update { state -> state.copy(draft = state.draft?.let(transform), error = null) }
            persistDraft()
        }

        private fun persistDraft() {
            savedStateHandle.saveYarnUsage(editor.value.draft)
        }
    }
