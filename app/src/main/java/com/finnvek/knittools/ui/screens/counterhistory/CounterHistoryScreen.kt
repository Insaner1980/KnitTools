package com.finnvek.knittools.ui.screens.counterhistory

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.time.ZoneId

@Composable
fun CounterHistoryScreen(
    onBack: () -> Unit,
    onProjectMissing: () -> Unit,
) {
    val viewModel: CounterHistoryViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = rememberCurrentLocale()
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val zone = ZoneId.systemDefault()
    val format =
        remember(locale, use24Hour, zone) {
            CounterHistoryFormat(
                locale,
                zone,
                localizedDateTimePattern(locale, "yMMMd"),
                localizedDateTimePattern(locale, if (use24Hour) "Hms" else "hms"),
            )
        }
    LaunchedEffect(viewModel, format) { viewModel.setFormat(format) }
    val currentOnProjectMissing by rememberUpdatedState(onProjectMissing)
    LaunchedEffect(state.projectMissing) {
        if (state.projectMissing) currentOnProjectMissing()
    }
    CounterHistoryContent(state, onBack)
}

@Composable
internal fun CounterHistoryContent(
    state: CounterHistoryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolScreenScaffold(
        title = stringResource(R.string.counter_history_title),
        onBack = onBack,
        modifier = modifier,
        wrapTitle = true,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(CounterDimens.WorkspaceGroupSpacing),
            verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceGroupSpacing),
        ) {
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.projectMissing -> Unit
                else -> {
                    item(key = "project") {
                        Text(
                            state.projectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.knitToolsColors.brandWine,
                        )
                    }
                    if (state.days.isEmpty()) {
                        item(key = "empty") { CounterHistoryEmpty() }
                    }
                    state.days.forEach { day ->
                        item(key = "date:${day.key}", contentType = "date") {
                            Text(
                                day.date,
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.knitToolsColors.brandWine,
                            )
                        }
                        items(day.rows, key = { it.event.id }, contentType = { "event" }) { row ->
                            CounterHistoryEvent(row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterHistoryEmpty() {
    Column(verticalArrangement = Arrangement.spacedBy(CounterDimens.ExtraCounterContentSpacing)) {
        Text(stringResource(R.string.counter_history_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.counter_history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CounterHistoryEvent(row: CounterHistoryRow) {
    val action =
        stringResource(
            when (row.event.action) {
                CounterHistoryAction.INCREASE -> R.string.counter_history_increase
                CounterHistoryAction.DECREASE -> R.string.counter_history_decrease
                CounterHistoryAction.RESET -> R.string.counter_history_reset
                CounterHistoryAction.CHANGED -> R.string.counter_history_changed
            },
        )
    val description =
        stringResource(
            R.string.counter_history_event_description,
            action,
            row.previousValue,
            row.newValue,
            row.date,
            row.time,
        )
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(CounterDimens.WorkspaceSectionActionIconSpacing),
    ) {
        Text(row.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(action, style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.counter_history_transition, row.previousValue, row.newValue),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
