package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ChipShape = RoundedCornerShape(percent = 50)
private val ChipHorizontalPadding = 16.dp
private val ChipVerticalPadding = 10.dp
private val ChipSpacing = 8.dp
private val ChipIndicatorSpacing = 4.dp
private val ChipIndicatorSize = 18.dp

@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }

    // Valinta on ohimenevää käyttöliittymätilaa: se nollautuu, kun aikaväli tai projektisuodatin vaihtuu.
    LaunchedEffect(uiState.timeRange, uiState.selectedProjectId) {
        selectedBucketIndex = null
    }
    val effectiveBucketIndex =
        selectedBucketIndex ?: defaultSelectedBucketIndex(uiState.chartBuckets)
    val rangeLabel = insightsRangeLabel(uiState)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = InsightsDimens.ScreenHorizontalPadding),
        ) {
            item {
                InsightsFilters(
                    uiState = uiState,
                    onSelectRange = viewModel::selectTimeRange,
                    onSelectProject = viewModel::selectProject,
                )
            }

            when {
                uiState.isLoading -> item { InsightsSkeleton() }
                !uiState.hasAnySessionData -> item { InsightsEmptyState() }
                else ->
                    insightsContent(
                        uiState = uiState,
                        rangeLabel = rangeLabel,
                        selectedBucketIndex = effectiveBucketIndex,
                        onSelectBucket = { selectedBucketIndex = it },
                        onProUpgrade = onProUpgrade,
                    )
            }
        }
    }
}

private fun LazyListScope.insightsContent(
    uiState: InsightsUiState,
    rangeLabel: String,
    selectedBucketIndex: Int?,
    onSelectBucket: (Int) -> Unit,
    onProUpgrade: () -> Unit,
) {
    item { InsightsHero(state = uiState) }
    item { InsightsStrongRule() }
    item { InsightsStatsRow(state = uiState) }
    item { InsightsHairline() }

    item {
        InsightsSectionHeader(
            title = stringResource(R.string.insights_section_every_day),
            meta = daysMetaText(uiState),
        )
    }
    item {
        if (uiState.isPro) {
            InsightsChart(
                buckets = uiState.chartBuckets,
                interval = uiState.chartInterval,
                timeRange = uiState.timeRange,
                today = uiState.rangeEnd,
                selectedIndex = selectedBucketIndex,
                contentDescription = chartContentDescription(uiState, rangeLabel),
                onSelectBucket = onSelectBucket,
            )
        } else {
            InsightsProChartCard(onProUpgrade = onProUpgrade)
        }
    }

    item {
        InsightsSectionHeader(title = stringResource(R.string.insights_section_where_time_went))
    }
    if (uiState.timePerProject.isEmpty()) {
        item { InsightsRangeEmptyNote(timeRange = uiState.timeRange) }
    } else {
        item { InsightsHairline() }
        itemsIndexed(uiState.timePerProject, key = { _, project -> project.projectId }) { index, project ->
            InsightsProjectRow(project = project, today = uiState.rangeEnd)
            if (index < uiState.timePerProject.lastIndex) {
                InsightsHairline()
            }
        }
    }
    item { InsightsHairline() }
    item { InsightsFooterNote(text = stringResource(footerMessageResource())) }
    item { Spacer(modifier = Modifier.height(InsightsDimens.ContentBottomPadding)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsFilters(
    uiState: InsightsUiState,
    onSelectRange: (TimeRange) -> Unit,
    onSelectProject: (Long?) -> Unit,
) {
    var showProjectPicker by remember { mutableStateOf(false) }
    val allProjectsLabel = stringResource(R.string.all_projects)

    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.FiltersTopPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        InsightsChip(
            label = stringResource(R.string.insights_this_week),
            selected = uiState.timeRange == TimeRange.THIS_WEEK,
            onClick = { onSelectRange(TimeRange.THIS_WEEK) },
        )
        InsightsChip(
            label = stringResource(R.string.insights_this_month),
            selected = uiState.timeRange == TimeRange.THIS_MONTH,
            onClick = { onSelectRange(TimeRange.THIS_MONTH) },
        )
        InsightsChip(
            label = stringResource(R.string.insights_all_time),
            selected = uiState.timeRange == TimeRange.ALL_TIME,
            onClick = { onSelectRange(TimeRange.ALL_TIME) },
        )
        Box {
            // Suodatin avaa valikon, joten se ei ole neljäs aikaväli: rooli ja
            // laajennustila kertovat ruudunlukijalle mitä napautus tekee, ja
            // kärkikolmio erottaa sen aikavälisiruista myös katseella.
            InsightsChip(
                label = uiState.selectedProjectName ?: allProjectsLabel,
                selected = uiState.selectedProjectId != null,
                onClick = { showProjectPicker = !showProjectPicker },
                role = Role.DropdownList,
                showMenuIndicator = true,
                modifier =
                    Modifier.semantics {
                        if (showProjectPicker) {
                            collapse {
                                showProjectPicker = false
                                true
                            }
                        } else {
                            expand {
                                showProjectPicker = true
                                true
                            }
                        }
                    },
            )
            DropdownMenu(
                expanded = showProjectPicker,
                onDismissRequest = { showProjectPicker = false },
            ) {
                DropdownMenuItem(
                    text = { Text(allProjectsLabel) },
                    onClick = {
                        onSelectProject(null)
                        showProjectPicker = false
                    },
                )
                uiState.projects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name) },
                        onClick = {
                            onSelectProject(project.id)
                            showProjectPicker = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab,
    showMenuIndicator: Boolean = false,
) {
    val backgroundModifier =
        if (selected) {
            Modifier.background(MaterialTheme.colorScheme.primary)
        } else {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .clip(ChipShape)
                .then(backgroundModifier)
                .clickable(role = role, onClick = onClick)
                .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showMenuIndicator) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = contentColor,
                modifier =
                    Modifier
                        .padding(start = ChipIndicatorSpacing)
                        .size(ChipIndicatorSize),
            )
        }
    }
}

/**
 * Kaavio luetaan ruudunlukijalle yhtenä elementtinä: 31 nimeämätöntä pylvästä ei
 * palvele ketään.
 */
@Composable
private fun chartContentDescription(
    uiState: InsightsUiState,
    rangeLabel: String,
): String {
    val longestMinutes = uiState.chartBuckets.maxOfOrNull { it.totalMinutes } ?: 0
    return stringResource(
        if (uiState.chartInterval == PaceGroupingInterval.MONTH) {
            R.string.insights_chart_a11y_monthly
        } else {
            R.string.insights_chart_a11y_daily
        },
        craftVerbText(uiState.craftMix),
        rangeLabel,
        daysMetaText(uiState),
        durationText(DurationDisplayFormatter.fromMinutes(longestMinutes)),
    )
}

/** "19 / 28 päivää" — näytön informatiivisin luku. */
@Composable
private fun daysMetaText(uiState: InsightsUiState): String =
    pluralStringResource(
        R.plurals.insights_days_meta_format,
        uiState.daysInRange,
        uiState.activeDays,
        uiState.daysInRange,
    )

/** Kickerin oikea puoli: valitun aikavälin päivämäärät. */
@Composable
private fun insightsRangeLabel(uiState: InsightsUiState): String {
    val locale = rememberCurrentLocale()
    val start = uiState.rangeStart
    val end = uiState.rangeEnd
    val monthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMM"), locale)
        }
    val dayMonthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMMd"), locale)
        }
    return if (uiState.timeRange == TimeRange.ALL_TIME || start == null) {
        stringResource(R.string.insights_range_since_format, (start ?: end).format(monthFormatter))
    } else {
        val startLabel =
            if (start.month == end.month && start.year == end.year) {
                formatIntegerForDisplay(start.dayOfMonth.toLong(), locale)
            } else {
                start.format(dayMonthFormatter)
            }
        stringResource(R.string.insights_range_format, startLabel, end.format(dayMonthFormatter))
    }
}

private fun footerMessageResource(): Int {
    val footerMessages =
        listOf(
            R.string.insights_footer_1,
            R.string.insights_footer_2,
            R.string.insights_footer_3,
            R.string.insights_footer_4,
            R.string.insights_footer_5,
        )
    return footerMessages[LocalDate.now().toEpochDay().mod(footerMessages.size)]
}
