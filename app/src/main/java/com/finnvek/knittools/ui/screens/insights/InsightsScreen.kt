package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ChipShape = RoundedCornerShape(percent = 50)
private val ChipHorizontalPadding = 16.dp
private val ChipVerticalPadding = 10.dp
private val ChipSpacing = 8.dp
private val ChipIndicatorSpacing = 4.dp
private val ChipIndicatorSize = 18.dp
private val ChipMinTouchTarget = 48.dp
private val ChipDotSize = 10.dp
private val ChipDotSpacing = 8.dp

@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    onLaunchCounter: (Long) -> Unit = {},
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
                        onLaunchCounter = onLaunchCounter,
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
    onLaunchCounter: (Long) -> Unit,
) {
    item { InsightsRangeKicker(text = rangeLabel) }
    item { InsightsHero(state = uiState) }
    item { InsightsStrongRule() }
    item { InsightsStatsRow(state = uiState) }
    item { InsightsTrendLine(state = uiState) }
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
            InsightsProjectRow(
                project = project,
                today = uiState.rangeEnd,
                rangeTotalMinutes = uiState.totalMinutes,
                onClick = { onLaunchCounter(project.projectId) },
            )
            if (index < uiState.timePerProject.lastIndex) {
                InsightsHairline()
            }
        }
    }
    item { InsightsHairline() }
    item { InsightsFooterNote(text = stringResource(footerMessageResource())) }
    item { Spacer(modifier = Modifier.height(InsightsDimens.ContentBottomPadding)) }
}

@Composable
private fun InsightsFilters(
    uiState: InsightsUiState,
    onSelectRange: (TimeRange) -> Unit,
    onSelectProject: (Long?) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.FiltersTopPadding),
        verticalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        // Aikaväli on yksi valintaryhmä: ruudunlukija ilmoittaa "valittu 3:sta".
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        ) {
            InsightsRangeChip(
                label = stringResource(R.string.insights_this_week),
                selected = uiState.timeRange == TimeRange.THIS_WEEK,
                onClick = { onSelectRange(TimeRange.THIS_WEEK) },
            )
            InsightsRangeChip(
                label = stringResource(R.string.insights_this_month),
                selected = uiState.timeRange == TimeRange.THIS_MONTH,
                onClick = { onSelectRange(TimeRange.THIS_MONTH) },
            )
            InsightsRangeChip(
                label = stringResource(R.string.insights_all_time),
                selected = uiState.timeRange == TimeRange.ALL_TIME,
                onClick = { onSelectRange(TimeRange.ALL_TIME) },
            )
        }
        InsightsProjectFilter(uiState = uiState, onSelectProject = onSelectProject)
    }
}

/** Aikavälisiru: täytetty kun valittu, ja valintatila kulkee semantiikassa. */
@Composable
private fun InsightsRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundModifier =
        if (selected) {
            Modifier.background(MaterialTheme.colorScheme.primary)
        } else {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
        }
    Row(
        modifier =
            Modifier
                .heightIn(min = ChipMinTouchTarget)
                .clip(ChipShape)
                .then(backgroundModifier)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Projektisuodatin ei ole neljäs aikaväli, joten se ei koskaan saa täytettyä
 * primary-tyyliä: valittu projekti tunnistetaan omasta väripisteestään, joka on
 * sama väri kuin kaaviossa ja listassa.
 */
@Composable
private fun InsightsProjectFilter(
    uiState: InsightsUiState,
    onSelectProject: (Long?) -> Unit,
) {
    var showProjectPicker by remember { mutableStateOf(false) }
    val allProjectsLabel = stringResource(R.string.all_projects)
    val selectedId = uiState.selectedProjectId

    Box {
        Row(
            modifier =
                Modifier
                    .heightIn(min = ChipMinTouchTarget)
                    .clip(ChipShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ChipShape)
                    .clickable(role = Role.DropdownList) { showProjectPicker = !showProjectPicker }
                    .semantics {
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
                    }.padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedId != null) {
                Box(
                    modifier =
                        Modifier
                            .size(ChipDotSize)
                            .background(yarnColorForId(selectedId), CircleShape),
                )
                Spacer(modifier = Modifier.width(ChipDotSpacing))
            }
            Text(
                text = uiState.selectedProjectName ?: allProjectsLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(start = ChipIndicatorSpacing)
                        .size(ChipIndicatorSize),
            )
        }
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
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMMM"), locale)
        }
    val dayMonthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMMd"), locale)
        }
    return if (uiState.timeRange == TimeRange.ALL_TIME || start == null) {
        stringResource(R.string.insights_range_open_format, (start ?: end).format(monthFormatter))
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
