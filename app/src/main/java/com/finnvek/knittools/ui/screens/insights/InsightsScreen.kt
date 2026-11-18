package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    onLaunchCounter: (Long) -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDataIndex by remember { mutableStateOf<Int?>(null) }

    // Valinta on ohimenevää käyttöliittymätilaa: se nollautuu, kun aikaväli tai projektisuodatin vaihtuu.
    LaunchedEffect(uiState.timeRange, uiState.selectedProjectId) {
        selectedDataIndex = null
    }
    val rangeLabel = insightsRangeLabel(uiState)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Insights oli ainoa ylätason välilehti ilman otsikkoa: sisältö alkoi suoraan
        // tilapalkin alta. Sama TopAppBar kuin Libraryssa ja Toolsissa.
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.insights_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
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
                        selectedDataIndex = selectedDataIndex,
                        onSelectData = { selectedDataIndex = it },
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
    selectedDataIndex: Int?,
    onSelectData: (Int) -> Unit,
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
            if (uiState.timeRange == TimeRange.ALL_TIME) {
                uiState.fabric?.let { fabric ->
                    InsightsFabric(
                        model = fabric,
                        selectedIndex = selectedDataIndex,
                        onSelectDay = onSelectData,
                    )
                }
            } else {
                InsightsChart(
                    buckets = uiState.chartBuckets,
                    timeRange = uiState.timeRange,
                    today = uiState.rangeEnd,
                    selectedIndex = selectedDataIndex ?: defaultSelectedBucketIndex(uiState.chartBuckets),
                    contentDescription = chartContentDescription(uiState, rangeLabel),
                    onSelectBucket = onSelectData,
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(InsightsDimens.FilterChipSpacing),
    ) {
        // Sama segmenttivalitsin kuin muualla sovelluksessa. Oma toteutus piirsi
        // valitun leiman kahdesti, ja päällimmäiseksi jäänyt onSurfaceVariant-teksti
        // antoi oranssia vasten vain 2:1 kontrastin.
        SegmentedToggle(
            options = insightsRangeOptions.map { stringResource(it.labelResource) },
            selectedIndex = insightsRangeOptions.indexOfFirst { it.range == uiState.timeRange },
            onSelect = { index -> onSelectRange(insightsRangeOptions[index].range) },
            fraction = 1f,
        )
        InsightsProjectFilter(uiState = uiState, onSelectProject = onSelectProject)
    }
}

private data class InsightsRangeOption(
    val range: TimeRange,
    val labelResource: Int,
)

/** Näyttöjärjestys, ei TimeRange-enumin oma järjestys. */
private val insightsRangeOptions =
    listOf(
        InsightsRangeOption(TimeRange.THIS_WEEK, R.string.insights_this_week),
        InsightsRangeOption(TimeRange.THIS_MONTH, R.string.insights_this_month),
        InsightsRangeOption(TimeRange.ALL_TIME, R.string.insights_all_time),
    )

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
                    .heightIn(min = InsightsDimens.FilterChipMinTouchTarget)
                    .clip(InsightsDimens.FilterChipShape)
                    // Täytetty pinta ääriviivan sijaan: kaksi eri levyistä
                    // ääriviivastadionia päällekkäin luki yhtenä rikkinäisenä komponenttina.
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    }.padding(
                        horizontal = InsightsDimens.FilterChipHorizontalPadding,
                        vertical = InsightsDimens.FilterChipVerticalPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedId != null) {
                Box(
                    modifier =
                        Modifier
                            .size(InsightsDimens.FilterChipDotSize)
                            .background(yarnColorForId(selectedId), CircleShape),
                )
                Spacer(modifier = Modifier.width(InsightsDimens.FilterChipDotSpacing))
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
                        .padding(start = InsightsDimens.FilterChipIndicatorSpacing)
                        .size(InsightsDimens.FilterChipIndicatorSize),
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
        R.string.insights_chart_a11y_daily,
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
    val dayMonthPattern = remember(locale) { localizedDateTimePattern(locale, "MMMd") }
    val dayMonthFormatter =
        remember(locale, dayMonthPattern) {
            DateTimeFormatter.ofPattern(dayMonthPattern, locale)
        }
    return if (uiState.timeRange == TimeRange.ALL_TIME || start == null) {
        stringResource(R.string.insights_range_open_format, (start ?: end).format(monthFormatter))
    } else {
        val startLabel =
            if (
                start.month == end.month &&
                start.year == end.year &&
                datePatternPlacesDayBeforeMonth(dayMonthPattern)
            ) {
                formatIntegerForDisplay(start.dayOfMonth.toLong(), locale)
            } else {
                start.format(dayMonthFormatter)
            }
        stringResource(R.string.insights_range_format, startLabel, end.format(dayMonthFormatter))
    }
}

internal fun datePatternPlacesDayBeforeMonth(pattern: String): Boolean {
    var quoted = false
    var dayIndex = -1
    var monthIndex = -1
    var index = 0
    while (index < pattern.length) {
        val character = pattern[index]
        if (character == '\'') {
            if (index + 1 < pattern.length && pattern[index + 1] == '\'') {
                index++
            } else {
                quoted = !quoted
            }
        } else if (!quoted) {
            when (character) {
                'd' -> if (dayIndex < 0) dayIndex = index
                'M', 'L' -> if (monthIndex < 0) monthIndex = index
            }
        }
        index++
    }
    return dayIndex >= 0 && monthIndex >= 0 && dayIndex < monthIndex
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
