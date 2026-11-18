package com.finnvek.knittools.ui.screens.counterhistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.CounterHistory
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.navigation.toPositiveRouteIdOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class CounterHistoryFormat(
    val locale: Locale,
    val zone: ZoneId,
    val datePattern: String,
    val timePattern: String,
)

data class CounterHistoryRow(
    val event: CounterHistory,
    val date: String,
    val time: String,
    val previousValue: String,
    val newValue: String,
)

data class CounterHistoryDay(
    val key: String,
    val date: String,
    val rows: List<CounterHistoryRow>,
)

data class CounterHistoryUiState(
    val loading: Boolean = true,
    val projectMissing: Boolean = false,
    val projectName: String = "",
    val days: List<CounterHistoryDay> = emptyList(),
)

internal fun presentCounterHistory(
    events: List<CounterHistory>,
    format: CounterHistoryFormat,
): List<CounterHistoryDay> {
    val zone = TimeZone.getTimeZone(format.zone)
    val dateFormat = SimpleDateFormat(format.datePattern, format.locale).apply { timeZone = zone }
    val timeFormat = SimpleDateFormat(format.timePattern, format.locale).apply { timeZone = zone }
    val numbers = NumberFormat.getIntegerInstance(format.locale)
    return events
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(format.zone).toLocalDate() }
        .map { (day, entries) ->
            val rows =
                entries.map { event ->
                    val timestamp = Date(event.timestamp)
                    CounterHistoryRow(
                        event = event,
                        date = dateFormat.format(timestamp),
                        time = timeFormat.format(timestamp),
                        previousValue = numbers.format(event.previousValue),
                        newValue = numbers.format(event.newValue),
                    )
                }
            CounterHistoryDay(day.toString(), rows.first().date, rows)
        }
}

@HiltViewModel
class CounterHistoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: CounterRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val projectId = savedStateHandle.get<Long>("projectId")?.toPositiveRouteIdOrNull()
        private val format = MutableStateFlow<CounterHistoryFormat?>(null)

        val uiState =
            if (projectId == null) {
                flowOf(CounterHistoryUiState(loading = false, projectMissing = true))
            } else {
                combine(repository.observeProject(projectId), repository.observeCounterHistory(projectId), format) {
                    project,
                    events,
                    currentFormat,
                    ->
                    when {
                        project == null -> CounterHistoryUiState(loading = false, projectMissing = true)
                        currentFormat == null -> CounterHistoryUiState()
                        else ->
                            CounterHistoryUiState(
                                loading = false,
                                projectName = project.name,
                                days = presentCounterHistory(events, currentFormat),
                            )
                    }
                }.flowOn(ioDispatcher)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CounterHistoryUiState())

        fun setFormat(value: CounterHistoryFormat) {
            format.value = value
        }
    }
