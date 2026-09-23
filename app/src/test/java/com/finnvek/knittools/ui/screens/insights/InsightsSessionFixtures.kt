package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.data.local.SessionProjectActivity
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.SessionInsightsFacts
import io.mockk.every
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun stubSessions(
    repository: CounterRepository,
    rows: Flow<List<KnitSession>>,
) {
    every {
        repository.observeSessionsForInsights<InsightsSessionAccumulator>(
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    } answers
        {
            val projectId = firstArg<Long?>()
            val start = secondArg<Long?>()
            val zone = thirdArg<java.time.ZoneId>()
            val create = arg<(SessionInsightsFacts) -> InsightsSessionAccumulator>(3)
            val add = arg<suspend (InsightsSessionAccumulator, List<KnitSession>) -> Unit>(4)
            rows.map { sessions ->
                val scoped = sessions.filter { projectId == null || it.projectId == projectId }
                val facts =
                    SessionInsightsFacts(
                        sessions.isNotEmpty(),
                        scoped.groupBy { it.projectId }.map { (id, group) ->
                            SessionProjectActivity(id, group.maxOf { it.startedAt })
                        },
                        if (start == null) InsightsViewModel.firstSessionDate(scoped, zone) else null,
                    )
                val accumulator = create(facts)
                scoped
                    .filter {
                        start == null ||
                            it.endedAt >= start ||
                            it.startedAt +
                            when {
                                it.durationSeconds > 0L -> it.durationSeconds
                                it.durationMinutes > 0 -> it.durationMinutes * 60L
                                else -> 1L
                            } * 1000L >= start
                    }.chunked(2)
                    .forEach { add(accumulator, it) }
                accumulator
            }
        }
}
