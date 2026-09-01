package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.components.HubListItem
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    onLaunchCounter: (Long) -> Unit = {},
    onSessionHistory: (Long) -> Unit = {},
    viewModelProvider: @Composable () -> InsightsViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }
    var selectedFabricDate by remember { mutableStateOf<LocalDate?>(null) }

    // Valinta on ohimenevää käyttöliittymätilaa: se nollautuu, kun aikaväli tai projektisuodatin vaihtuu.
    LaunchedEffect(uiState.timeRange, uiState.selectedProjectId) {
        selectedBucketIndex = null
        selectedFabricDate = null
    }
    LaunchedEffect(uiState.projectFabric, selectedFabricDate) {
        val selectedDate = selectedFabricDate
        if (selectedDate != null && uiState.projectFabric?.days?.none { it.date == selectedDate } != false) {
            selectedFabricDate = null
        }
    }
    val effectiveBucketIndex =
        selectedBucketIndex ?: defaultSelectedBucketIndex(uiState.chartBuckets)
    val rangeLabel = insightsRangeLabel(uiState)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Otsikko on valittu aikaväli, ei näytön nimi: alanavigaatio kertoo jo missä ollaan,
        // ja aikavälivalitsin omana rivinään söi 124 dp ennen kuin yhtään dataa näkyi.
        topBar = {
            TopAppBar(
                title = { InsightsRangeTitle(uiState = uiState, onSelectRange = viewModel::selectTimeRange) },
                // Palkki maalaa oman taustansa, muuten vieritetty sisältö näkyy otsikon
                // takaa katkaistuina kirjaimina. Väri on sama kuin Scaffoldin.
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
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
            when {
                uiState.isLoading -> item { InsightsSkeleton() }
                !uiState.hasAnySessionData -> item { InsightsEmptyState() }
                else ->
                    insightsContent(
                        uiState = uiState,
                        rangeLabel = rangeLabel,
                        selectedBucketIndex = effectiveBucketIndex,
                        hasUserSelection = selectedBucketIndex != null,
                        selectedFabricDate = selectedFabricDate,
                        onSelectBucket = { selectedBucketIndex = it },
                        onSelectFabricDate = { selectedFabricDate = it },
                        onSelectProject = viewModel::selectProject,
                        onProUpgrade = onProUpgrade,
                        onLaunchCounter = onLaunchCounter,
                        onSessionHistory = onSessionHistory,
                    )
            }
        }
    }
}

/** Aikaväli otsikkona: napautus avaa valikon. */
@Composable
private fun InsightsRangeTitle(
    uiState: InsightsUiState,
    onSelectRange: (TimeRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = insightsRangeOptions.firstOrNull { it.range == uiState.timeRange }

    Box {
        Row(
            modifier =
                Modifier
                    .clip(InsightsDimens.FilterChipShape)
                    .clickable(role = Role.DropdownList) { expanded = !expanded }
                    .semantics {
                        if (expanded) {
                            collapse {
                                expanded = false
                                true
                            }
                        } else {
                            expand {
                                expanded = true
                                true
                            }
                        }
                    }.padding(
                        horizontal = InsightsDimens.RangeTitleHorizontalPadding,
                        vertical = InsightsDimens.RangeTitleVerticalPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(selected?.labelResource ?: R.string.insights_all_time),
                style = MaterialTheme.typography.headlineMedium,
                // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(InsightsDimens.RangeTitleIndicatorSize),
                // CPD-ON
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Sama pinta ja muoto kuin projektivalikolla: kaksi eri näköistä pudotusvalikkoa
            // samalla näytöllä luki huolimattomuutena.
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            insightsRangeOptions.forEach { option ->
                InsightsMenuItem(
                    label = stringResource(option.labelResource),
                    selected = option.range == uiState.timeRange,
                    onClick = {
                        onSelectRange(option.range)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Suppress("kotlin:S107") // LazyList-sisältö tarvitsee yhden näkymän erilliset valinta- ja aikavälitilat.
private fun LazyListScope.insightsContent(
    uiState: InsightsUiState,
    rangeLabel: String,
    selectedBucketIndex: Int?,
    hasUserSelection: Boolean,
    selectedFabricDate: LocalDate?,
    onSelectBucket: (Int) -> Unit,
    onSelectFabricDate: (LocalDate) -> Unit,
    onSelectProject: (Long?) -> Unit,
    onProUpgrade: () -> Unit,
    onLaunchCounter: (Long) -> Unit,
    onSessionHistory: (Long) -> Unit,
) {
    // Aikaväli ja projektisuodatin jakavat rivin: molemmat kertovat mitä katsotaan,
    // eikä suodatin tarvitse omaa 52 dp risetiään puolityhjän rivin viereen.
    item {
        InsightsContextRow(
            rangeLabel = rangeLabel,
            uiState = uiState,
            onSelectProject = onSelectProject,
        )
    }
    item { InsightsHero(state = uiState) }
    item { InsightsStatsRow(state = uiState) }
    item { InsightsTrendLine(state = uiState) }

    chartSection(
        uiState = uiState,
        rangeLabel = rangeLabel,
        selectedBucketIndex = selectedBucketIndex,
        hasUserSelection = hasUserSelection,
        onSelectBucket = onSelectBucket,
        onProUpgrade = onProUpgrade,
    )

    projectFabricSection(
        uiState = uiState,
        selectedFabricDate = selectedFabricDate,
        onSelectFabricDate = onSelectFabricDate,
    )

    projectBreakdownSection(uiState = uiState, onLaunchCounter = onLaunchCounter)

    val selectedProjectId = uiState.selectedProjectId
    // Istuntolista on jo olemassa omana näyttönään, joten Insights linkittää siihen eikä
    // piirrä omaa kopiotaan. Linkki näkyy aina kun projekti on valittuna — myös silloin
    // kun tällä aikavälillä ei ole istuntoja, koska juuri silloin haluaa tietää milloin
    // projektia viimeksi teki.
    if (selectedProjectId != null) {
        item {
            HubListItem(
                title = stringResource(R.string.session_history_title),
                description =
                    uiState.selectedProjectName
                        ?: stringResource(R.string.new_project_name_format, selectedProjectId),
                onClick = { onSessionHistory(selectedProjectId) },
                modifier = Modifier.padding(top = InsightsDimens.SectionTopPadding),
            )
        }
    }
    item { Spacer(modifier = Modifier.height(InsightsDimens.ContentBottomPadding)) }
}

private fun LazyListScope.projectBreakdownSection(
    uiState: InsightsUiState,
    onLaunchCounter: (Long) -> Unit,
) {
    // Yhdelle projektille suodatettuna osio on kolme kertaa sama tieto: suodatinpilleri,
    // yksivärinen 100 %:n lankapalkki ja yksi rivi. Tyhjärivi näytetään silti.
    val showsProjectMix = uiState.selectedProjectId == null
    if (uiState.timePerProject.isEmpty()) {
        item { InsightsRangeEmptyNote(timeRange = uiState.timeRange) }
    } else if (!showsProjectMix) {
        // Suodatettuna listaosio jää pois, mutta "viimeksi" on ainoa tieto jota
        // hero, tilastot tai kaavio eivät kerro.
        item {
            InsightsLastWorkedNote(project = uiState.timePerProject.first(), today = uiState.rangeEnd)
        }
    } else {
        item {
            InsightsSectionHeader(title = stringResource(R.string.insights_section_where_time_went))
        }
        // Osuudet luetaan yhdestä lankapalkista; rivit kertovat nimen ja keston.
        item {
            InsightsProjectMixBar(
                projects = uiState.timePerProject,
                totalMinutes = uiState.totalMinutes,
            )
        }
        items(uiState.timePerProject, key = { project -> project.projectId }) { project ->
            InsightsProjectRow(
                project = project,
                today = uiState.rangeEnd,
                onClick = { onLaunchCounter(project.projectId) },
            )
        }
    }
}

private fun LazyListScope.projectFabricSection(
    uiState: InsightsUiState,
    selectedFabricDate: LocalDate?,
    onSelectFabricDate: (LocalDate) -> Unit,
) {
    val model = uiState.projectFabric ?: return
    item {
        InsightsSectionHeader(
            title = stringResource(R.string.insights_section_projects_by_day),
            meta = projectFabricHeaderMeta(model, selectedFabricDate),
            metaIsLive = true,
        )
    }
    item {
        val visibleRange = projectFabricRangeLabel(model)
        InsightsProjectFabric(
            model = model,
            selectedDate = selectedFabricDate,
            contentDescription =
                pluralStringResource(
                    R.plurals.insights_project_fabric_a11y_summary,
                    model.activeDayCount,
                    visibleRange,
                    model.activeDayCount,
                ),
            previousActiveDayLabel = stringResource(R.string.insights_project_fabric_previous_active_day),
            nextActiveDayLabel = stringResource(R.string.insights_project_fabric_next_active_day),
            onSelectDay = onSelectFabricDate,
        )
    }
    selectedFabricDate
        ?.let { selectedDate -> model.days.firstOrNull { it.date == selectedDate } }
        ?.let { day ->
            item {
                InsightsProjectFabricDayDetails(day = day, projects = uiState.projects)
            }
        }
}

@Composable
private fun projectFabricHeaderMeta(
    model: InsightsProjectFabricModel,
    selectedDate: LocalDate?,
): String = selectedDate?.let { projectFabricDateLabel(it) } ?: projectFabricRangeLabel(model)

@Composable
private fun projectFabricRangeLabel(model: InsightsProjectFabricModel): String {
    val locale = rememberCurrentLocale()
    val shortFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMMd"), locale)
        }
    val fullFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMMd"), locale)
        }
    val labels =
        projectFabricRangeDateLabels(
            startDate = model.startDate,
            endDate = model.endDate,
            shortFormatter = shortFormatter,
            fullFormatter = fullFormatter,
        )
    return stringResource(R.string.insights_range_format, labels.first, labels.second)
}

internal fun projectFabricRangeDateLabels(
    startDate: LocalDate,
    endDate: LocalDate,
    shortFormatter: DateTimeFormatter,
    fullFormatter: DateTimeFormatter,
): Pair<String, String> {
    val startLabel =
        if (startDate.year == endDate.year) {
            startDate.format(shortFormatter)
        } else {
            startDate.format(fullFormatter)
        }
    return startLabel to endDate.format(fullFormatter)
}

@Composable
private fun projectFabricDateLabel(date: LocalDate): String {
    val locale = rememberCurrentLocale()
    val formatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMMd"), locale)
        }
    return date.format(formatter)
}

@Composable
private fun InsightsProjectFabricDayDetails(
    day: InsightsProjectFabricDay,
    projects: List<CounterProject>,
) {
    val projectNames = projects.associate { it.id to it.name }
    val names = mutableListOf<String>()
    for (projectId in day.projectIds) {
        names += projectNames[projectId] ?: stringResource(R.string.new_project_name_format, projectId)
    }
    Text(
        text = names.joinToString().ifEmpty { stringResource(R.string.insights_no_tracked_activity) },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = InsightsDimens.ProjectFabricDetailTopPadding),
    )
}

/**
 * Ilman yhtäkään minuuttia kaavio olisi 168 dp tyhjää laatikkoa kahden apuviivan
 * välissä. Koko osio jätetään silloin pois ja projektilistan tyhjärivi kertoo saman.
 */
private fun LazyListScope.chartSection(
    uiState: InsightsUiState,
    rangeLabel: String,
    selectedBucketIndex: Int?,
    hasUserSelection: Boolean,
    onSelectBucket: (Int) -> Unit,
    onProUpgrade: () -> Unit,
) {
    if (uiState.totalMinutes <= 0) return
    // Yksi pylväs ei ole kaavio vaan luku: asteikko skaalautuu maksimiin, joten ainoa
    // pylväs piirtyy aina täyskorkeana. Yhden minuutin istunto näytti yhtä isolta kuin
    // kolmen tunnin urakka. Hero kertoo saman luvun rehellisemmin.
    //
    if (!uiState.hasMeaningfulChartData) return
    item {
        InsightsSectionHeader(
            title =
                stringResource(
                    when (uiState.chartInterval) {
                        PaceGroupingInterval.MONTH -> R.string.insights_section_month_by_month
                        PaceGroupingInterval.WEEK -> R.string.insights_section_week_by_week
                        PaceGroupingInterval.DAY -> R.string.insights_section_every_day
                    },
                ),
            // Sama rivi palvelee kahta tilaa: oletuksena aktiiviset päivät, ja
            // kosketuksen jälkeen valitun ämpärin lukema. Erillinen lukemarivi varasi
            // 25 dp näyttääkseen jotain mitä kukaan ei vielä kysynyt.
            meta = chartHeaderMeta(uiState, selectedBucketIndex, hasUserSelection),
            metaIsLive = true,
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
            InsightsProChartPrompt(onProUpgrade = onProUpgrade)
        }
    }
}

/** Kaavio-osion mittaluku: aktiiviset päivät, tai valitun ämpärin lukema kosketuksen jälkeen. */
@Composable
private fun chartHeaderMeta(
    uiState: InsightsUiState,
    selectedBucketIndex: Int?,
    hasUserSelection: Boolean,
): String {
    val bucket = selectedBucketIndex?.let(uiState.chartBuckets::getOrNull)
    if (!hasUserSelection || bucket == null) return daysMetaText(uiState)
    val label = bucketLabel(bucket.bucketStart, uiState.chartInterval)
    if (bucket.totalMinutes <= 0) return label
    return stringResource(
        R.string.insights_project_sub_format,
        label,
        durationText(DurationDisplayFormatter.fromMinutes(bucket.totalMinutes)),
    )
}

/** Aikavälirivi ja projektisuodatin samalla rivillä. */
@Composable
private fun InsightsContextRow(
    rangeLabel: String,
    uiState: InsightsUiState,
    onSelectProject: (Long?) -> Unit,
) {
    // Aikaväli vain All Timessa. Viikko- ja kuukausinäkymässä otsikko sanoo jo
    // "This Month" ja kaavion akseli näyttää samat päivät — kolmas toisto vain vei
    // tilaa suodattimelta. All Timessa päivämäärät ovat aitoa uutta tietoa.
    val showsKicker = uiState.timeRange == TimeRange.ALL_TIME
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .padding(top = InsightsDimens.FiltersTopPadding)

    if (showsKicker) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Painotus ilman täyttöä: kicker saa koko jäljelle jäävän tilan ja katkeaa
            // vasta kun se oikeasti loppuu. Erillinen painotettu Spacer jakoi vapaan
            // tilan tasan puoliksi, jolloin teksti katkesi 100 dp tyhjän vieressä.
            InsightsRangeKicker(
                text = rangeLabel,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(InsightsDimens.ContextRowGap))
            InsightsProjectFilter(uiState = uiState, onSelectProject = onSelectProject)
        }
    } else {
        // Yksin vasemmalla täytetty pilleri luki toisena otsikkona. Oikea yläkulma on
        // paikka jossa kontrollit asuvat, joten se lukee siellä suodattimena.
        Box(modifier = rowModifier, contentAlignment = Alignment.CenterEnd) {
            InsightsProjectFilter(uiState = uiState, onSelectProject = onSelectProject)
        }
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
                    // Kosketuskohde tulee Composen omasta laajennuksesta, joten pilleri saa
                    // olla visuaalisesti matalampi kuin 48 dp minimi. Täytetty 48 dp lohko
                    // oli heron jälkeen näytön äänekkäin elementti.
                    .minimumInteractiveComponentSize()
                    .heightIn(min = InsightsDimens.FilterPillHeight)
                    .clip(InsightsDimens.FilterChipShape)
                    // Täytetty pinta ääriviivan sijaan, jotta suodatin ja segmenttivalitsin
                    // ovat samaa pintakieltä eivätkä kahta eri levyistä ääriviivastadionia.
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
                            .background(
                                yarnColorForId(selectedId, MaterialTheme.knitToolsColors.yarnPalette),
                                CircleShape,
                            ),
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
            // Vakio-Material-pinta oli näytön ainoa tyylittelemätön kohta.
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            InsightsMenuItem(
                label = allProjectsLabel,
                selected = selectedId == null,
                onClick = {
                    onSelectProject(null)
                    showProjectPicker = false
                },
                dotColor = null,
                showsDot = true,
            )
            uiState.projects.forEach { project ->
                InsightsMenuItem(
                    label = project.name,
                    selected = selectedId == project.id,
                    onClick = {
                        onSelectProject(project.id)
                        showProjectPicker = false
                    },
                    dotColor = yarnColorForId(project.id, MaterialTheme.knitToolsColors.yarnPalette),
                    showsDot = true,
                )
            }
        }
    }
}

/**
 * Valikkorivi. Valittu merkitään lihavoinnilla ja checkillä — ilman merkintää valikosta
 * ei nähnyt mikä on päällä.
 *
 * Teksti on `bodyMedium` eikä `bodyLarge`: isommalla koolla check söi leveyttä juuri
 * valitulta riviltä, jolloin ainoa katkeava nimi oli se jota eniten halusi lukea.
 */
@Composable
private fun InsightsMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    showsDot: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon =
            if (showsDot) {
                {
                    Box(
                        modifier = Modifier.size(InsightsDimens.FilterChipDotSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        // "Kaikki projektit" saa vaimean pienemmän pisteen: täysi
                        // onSurfaceMuted luki kermalla yhtenä lankaväreistä.
                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        if (dotColor == null) {
                                            InsightsDimens.MenuNeutralDotSize
                                        } else {
                                            InsightsDimens.FilterChipDotSize
                                        },
                                    ).background(
                                        dotColor ?: MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape,
                                    ),
                        )
                    }
                }
            } else {
                null
            },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(InsightsDimens.FilterChipIndicatorSize),
                )
            }
        },
        onClick = onClick,
    )
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
        if (uiState.chartInterval == PaceGroupingInterval.DAY) {
            R.string.insights_chart_a11y_daily
        } else {
            R.string.insights_chart_a11y_monthly
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
    // Lyhyt kuukausi avoimelle välille: "July 2026 – August 2026" oli kaksi kertaa
    // pidempi kuin muut aikavälit ja söi projektisuodattimen tilan.
    val monthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMM"), locale)
        }
    val dayMonthPattern = remember(locale) { localizedDateTimePattern(locale, "MMMd") }
    val dayMonthFormatter =
        remember(locale, dayMonthPattern) {
            DateTimeFormatter.ofPattern(dayMonthPattern, locale)
        }
    return if (uiState.timeRange == TimeRange.ALL_TIME || start == null) {
        // Roikkuva ajatusviiva ("July 2026 –") luki katkenneena merkkijonona. Avoin väli
        // esitetään samalla kaksipuolisella muodolla kuin muutkin; yhden kuukauden
        // historia näyttää pelkän kuukauden.
        val first = (start ?: end).withDayOfMonth(1)
        val last = end.withDayOfMonth(1)
        if (first == last) {
            first.format(monthFormatter)
        } else {
            stringResource(
                R.string.insights_range_format,
                first.format(monthFormatter),
                last.format(monthFormatter),
            )
        }
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

@Suppress("kotlin:S3776") // Lokalisoitu päivämääräkuvio jäsennetään merkkitasolla lainaukset huomioiden.
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
