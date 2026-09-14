package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun LazyListScope.completionSection(state: InsightsUiState) {
    item(key = "completion_heading") {
        InsightsSectionHeader(title = stringResource(R.string.insights_completions_title))
        Text(completionCount(state.completions.events.size), style = MaterialTheme.typography.titleMedium)
    }
    if (state.completions.events.isEmpty()) {
        item(key = "completion_empty") {
            Text(
                stringResource(
                    if (state.selectedProjectId ==
                        null
                    ) {
                        R.string.insights_completions_empty
                    } else {
                        R.string.insights_completions_project_empty
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        if (state.completions.buckets.isNotEmpty()) {
            item(key = "completion_timeline") { CompletionTimeline(state.completions.buckets) }
        }
        item(key = "completion_events") { CompletionHistory(state) }
    }
}

private const val COMPLETION_PREVIEW_LIMIT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionHistory(state: InsightsUiState) {
    var expanded by remember(state.timeRange, state.selectedProjectId) { mutableStateOf(false) }
    Column {
        state.completions.events
            .take(COMPLETION_PREVIEW_LIMIT)
            .forEach { CompletionEventRow(it, state) }
        if (state.completions.events.size > COMPLETION_PREVIEW_LIMIT) {
            TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.show_more)) }
        }
    }
    if (expanded) {
        ModalBottomSheet(onDismissRequest = { expanded = false }) {
            LazyColumn(
                Modifier.testTag("completion_history").padding(horizontal = InsightsDimens.ScreenHorizontalPadding),
            ) {
                item { InsightsSectionHeader(title = stringResource(R.string.insights_completions_title)) }
                items(state.completions.events, key = { it.id }) { CompletionEventRow(it, state) }
            }
        }
    }
}

@Composable
private fun CompletionEventRow(
    event: InsightsCompletionEvent,
    state: InsightsUiState,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = InsightsDimens.FilterChipVerticalPadding),
    ) {
        Text(
            state.projects.firstOrNull { it.id == event.projectId }?.name
                ?: stringResource(R.string.new_project_name_format, event.projectId),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            completionDate(event.date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompletionTimeline(buckets: List<InsightsCompletionBucket>) {
    val maximum = remember(buckets) { buckets.maxOf { it.count }.coerceAtLeast(1) }
    LazyRow(
        modifier = Modifier.padding(vertical = InsightsDimens.FilterChipSpacing),
        horizontalArrangement = Arrangement.spacedBy(InsightsDimens.FilterChipSpacing),
    ) {
        items(buckets, key = { it.start.toEpochDay() }) { bucket ->
            Column(
                Modifier.width(InsightsDimens.CompletionBucketWidth).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.height(InsightsDimens.CompletionPlotHeight).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .width(InsightsDimens.CompletionBarWidth)
                            .height(InsightsDimens.CompletionPlotHeight * (bucket.count.toFloat() / maximum))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(completionCount(bucket.count), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (bucket.start ==
                        bucket.end
                    ) {
                        completionDate(bucket.start)
                    } else {
                        stringResource(
                            R.string.insights_completions_period,
                            completionDate(bucket.start),
                            completionDate(bucket.end),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun completionCount(count: Int): String =
    pluralStringResource(R.plurals.insights_completion_count, count, count)

@Composable
private fun completionDate(date: LocalDate): String {
    val locale = rememberCurrentLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMMd"), locale) }
    return date.format(formatter)
}
