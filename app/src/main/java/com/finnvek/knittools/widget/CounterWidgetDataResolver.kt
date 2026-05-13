package com.finnvek.knittools.widget

import android.content.Context
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.CounterRepository

internal suspend fun CounterRepository.resolveWidgetDisplayData(
    context: Context,
    candidates: List<WidgetData>,
): WidgetData {
    for (candidate in candidates) {
        getActiveWidgetProject(candidate)?.let { project ->
            return project.toWidgetData()
        }
    }
    return getLatestActiveProject()?.toWidgetData() ?: CounterWidgetState.defaultData(context)
}

internal suspend fun CounterRepository.getActiveWidgetProject(data: WidgetData): CounterProject? {
    val projectId = data.projectId.takeIf { it > 0L } ?: return null
    val project = getProject(projectId) ?: return null
    return project.takeUnless { it.isCompleted }
}
