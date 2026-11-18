package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import io.mockk.coEvery

internal fun CounterProjectDao.recordCounterHistory(
    projectId: Long,
    history: MutableList<CounterHistoryEntity>,
    onCountChanged: (Int) -> Unit,
) {
    coEvery { getLatestHistory(projectId) } answers { history.lastOrNull() }
    coEvery { deleteHistoryById(any()) } answers { history.removeAll { it.id == firstArg<Long>() } }
    coEvery { updateCount(projectId, any(), any()) } answers { onCountChanged(secondArg()) }
    coEvery { updateCounterStateWithHistory(projectId, any(), any(), any(), any(), any(), any()) } answers {
        onCountChanged(secondArg())
        history +=
            CounterHistoryEntity(
                id = (history.lastOrNull()?.id ?: 0L) + 1L,
                projectId = projectId,
                action = arg(3),
                previousValue = arg(4),
                newValue = secondArg(),
                linkedCounterDeltasJson = arg(6),
            )
    }
}
