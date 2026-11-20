package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.InsightsDimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun LazyListScope.completionSection(state: InsightsUiState) {
    // Osio jätetään kokonaan pois ilman valmistumisia, kuten Insightsin muutkin tyhjät osiot:
    // pelkkä "ei valmistumisia" -lause oli kohinaa jokaisella aikavälillä.
    if (state.completions.events.isEmpty()) return
    insightsItem("completion_heading") {
        Column {
            InsightsSectionHeader(title = stringResource(R.string.insights_completions_title))
            Text(completionCount(state.completions.events.size), style = MaterialTheme.typography.titleMedium)
        }
    }
    val nonEmptyBuckets = state.completions.buckets.filter { it.count > 0 }
    if (nonEmptyBuckets.size >= MINIMUM_CHART_BARS) {
        insightsItem("completion_timeline", ContentWidthPolicy.Chart) { CompletionTimeline(nonEmptyBuckets) }
    }
    insightsItem("completion_events", ContentWidthPolicy.ListDetail) { CompletionHistory(state) }
}

private const val COMPLETION_PREVIEW_LIMIT = 3
private const val MINIMUM_CHART_BARS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionHistory(state: InsightsUiState) {
    var expanded by rememberSaveable(state.timeRange, state.selectedProjectId) { mutableStateOf(false) }
    Column {
        state.completions.events
            .take(COMPLETION_PREVIEW_LIMIT)
            .forEach { CompletionEventRow(it, state) }
        if (state.completions.events.size > COMPLETION_PREVIEW_LIMIT) {
            TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.show_more)) }
        }
    }
    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AdaptiveModalWindow()
            LazyColumn(
                Modifier.testTag("completion_history").padding(horizontal = InsightsDimens.ScreenHorizontalPadding),
            ) {
                // Sheetin otsikko kuten muissa sheeteissä; osio-otsikko kuuluu näytön sisältöön.
                item { SheetTitle(text = stringResource(R.string.insights_completions_title)) }
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
    val gap = InsightsDimens.FilterChipSpacing
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = gap)) {
        if (InsightsDimens.CompletionBucketWidth * buckets.size + gap * (buckets.size - 1) <= maxWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                buckets.forEach { CompletionTimelineBucket(it, maximum, Modifier.weight(1f)) }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(gap),
                contentPadding = PaddingValues(end = InsightsDimens.CompletionTimelineEndPadding),
            ) {
                items(buckets, key = { it.start.toEpochDay() }) {
                    CompletionTimelineBucket(it, maximum, Modifier.width(InsightsDimens.CompletionBucketWidth))
                }
            }
        }
    }
}

@Composable
private fun CompletionTimelineBucket(
    bucket: InsightsCompletionBucket,
    maximum: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics(mergeDescendants = true) {},
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
        Text(
            formatIntegerForDisplay(bucket.count.toLong(), rememberCurrentLocale()),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val start = completionDate(bucket.start, skeleton = "Md")
        val periodLabel =
            if (bucket.start == bucket.end) {
                start
            } else {
                stringResource(R.string.insights_completions_period, start, completionDate(bucket.end, skeleton = "Md"))
            }
        Text(periodLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun completionCount(count: Int): String =
    pluralStringResource(R.plurals.insights_completion_count, count, count)

@Composable
private fun completionDate(
    date: LocalDate,
    skeleton: String = "yMMMd",
): String {
    val locale = rememberCurrentLocale()
    val formatter =
        remember(locale, skeleton) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, skeleton), locale)
        }
    return date.format(formatter)
}
