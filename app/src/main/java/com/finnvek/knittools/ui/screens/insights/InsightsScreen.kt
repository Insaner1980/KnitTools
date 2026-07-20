package com.finnvek.knittools.ui.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightChartColors
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalMinutes = uiState.totalMinutes
    val locale = rememberCurrentLocale()
    val avgPace = uiState.avgPace
    val completedCount = uiState.completedCount
    val currentStreak = uiState.currentStreak
    val bestStreak = uiState.bestStreak
    val projects = uiState.projects
    val selectedProjectId = uiState.selectedProjectId
    val timePerProject = uiState.timePerProject
    val paceOverTime = uiState.paceOverTime
    val dailyActivity = uiState.dailyActivity
    val timeRange = uiState.timeRange
    val hasSessionData = uiState.hasSessionData
    val isPro = uiState.isPro
    val canUseStreak = uiState.canUseStreak
    val resources = LocalResources.current

    var showProjectPicker by remember { mutableStateOf(false) }
    val selectedName =
        if (selectedProjectId == null) {
            stringResource(R.string.all_projects)
        } else {
            projects.find { it.id == selectedProjectId }?.name ?: stringResource(R.string.all_projects)
        }
    val animationKey = remember(selectedProjectId, timeRange) { "${selectedProjectId ?: "all"}:${timeRange.name}" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { showProjectPicker = !showProjectPicker },
                    label = { Text(selectedName) },
                )
                DropdownMenu(
                    expanded = showProjectPicker,
                    onDismissRequest = { showProjectPicker = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_projects)) },
                        onClick = {
                            viewModel.selectProject(null)
                            showProjectPicker = false
                        },
                    )
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.name) },
                            onClick = {
                                viewModel.selectProject(project.id)
                                showProjectPicker = false
                            },
                        )
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeRangeChip(
                        label = stringResource(R.string.insights_all_time),
                        selected = timeRange == TimeRange.ALL_TIME,
                        onClick = { viewModel.selectTimeRange(TimeRange.ALL_TIME) },
                    )
                    TimeRangeChip(
                        label = stringResource(R.string.insights_this_week),
                        selected = timeRange == TimeRange.THIS_WEEK,
                        onClick = { viewModel.selectTimeRange(TimeRange.THIS_WEEK) },
                    )
                    TimeRangeChip(
                        label = stringResource(R.string.insights_this_month),
                        selected = timeRange == TimeRange.THIS_MONTH,
                        onClick = { viewModel.selectTimeRange(TimeRange.THIS_MONTH) },
                    )
                }
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedMetricCard(
                        label = stringResource(R.string.total_time_label),
                        targetValue = totalMinutes / 60f,
                        formatValue = { resources.getString(R.string.time_format_hours, it) },
                        labelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.knitToolsColors.primaryTintContainer,
                        animationDelay = 0,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AnimatedMetricCard(
                        label = stringResource(R.string.avg_pace_label),
                        targetValue = avgPace,
                        formatValue = { resources.getString(R.string.pace_format, it) },
                        labelColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        animationDelay = 80,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AnimatedMetricCard(
                        label = stringResource(R.string.completed_label),
                        targetValue = completedCount.toFloat(),
                        formatValue = { formatIntegerForDisplay(it.toLong(), locale) },
                        labelColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        animationDelay = 160,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.avg_pace_label).localizedUppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            item {
                PaceOverTimeChart(
                    data = paceOverTime,
                    isPro = isPro,
                    onProUpgrade = onProUpgrade,
                    primaryColor = MaterialTheme.colorScheme.secondary,
                    animationKey = animationKey,
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.insights_knitting_activity).localizedUppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            item {
                ActivityGrid(
                    dailyActivity = dailyActivity,
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    isPro = isPro,
                    canUseStreak = canUseStreak,
                    onProUpgrade = onProUpgrade,
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.time_per_project).localizedUppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            item {
                TimePerProjectChart(
                    data = timePerProject,
                    isPro = isPro,
                    onProUpgrade = onProUpgrade,
                    animationKey = animationKey,
                )
            }

            if (hasSessionData || BuildConfig.DEBUG) {
                item {
                    val footerMessages =
                        listOf(
                            R.string.insights_footer_1,
                            R.string.insights_footer_2,
                            R.string.insights_footer_3,
                            R.string.insights_footer_4,
                            R.string.insights_footer_5,
                        )
                    val messageIndex =
                        remember {
                            LocalDate.now().toEpochDay().mod(footerMessages.size)
                        }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = stringResource(footerMessages[messageIndex]),
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
    )
}

@Composable
private fun AnimatedMetricCard(
    label: String,
    targetValue: Float,
    formatValue: (Float) -> String,
    labelColor: Color,
    containerColor: Color,
    animationDelay: Int,
    animationKey: Any,
    modifier: Modifier = Modifier,
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue, animationKey) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = targetValue,
            animationSpec =
                tween(
                    durationMillis = 600,
                    delayMillis = animationDelay,
                    easing = FastOutSlowInEasing,
                ),
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label.localizedUppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatValue(animatable.value),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PaceOverTimeChart(
    data: List<PaceOverTimePoint>,
    isPro: Boolean,
    onProUpgrade: () -> Unit,
    primaryColor: Color,
    animationKey: Any,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        when {
            !isPro -> ChartProPlaceholder(onClick = onProUpgrade)
            data.isEmpty() -> ChartEmptyState()
            else -> AnimatedPaceLineChart(data = data, primaryColor = primaryColor, animationKey = animationKey)
        }
    }
}

@Composable
private fun AnimatedPaceLineChart(
    data: List<PaceOverTimePoint>,
    primaryColor: Color,
    animationKey: Any,
) {
    val animatable = remember { Animatable(0f) }
    val maxPace =
        data
            .maxOf { point -> point.rowsPerHour.takeIf { it.isFinite() } ?: 0f }
            .coerceAtLeast(1f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val pointRingColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(data, animationKey) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = 700,
                    delayMillis = 240,
                    easing = FastOutSlowInEasing,
                ),
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPaceBucketLabel(data.first()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.pace_format, data.last().rowsPerHour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPaceBucketLabel(data.last()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(128.dp),
        ) {
            val chartHeight = size.height
            val chartWidth = size.width
            val zeroY = chartHeight
            drawLine(
                color = trackColor,
                start = Offset(0f, zeroY),
                end = Offset(chartWidth, zeroY),
                strokeWidth = 1.dp.toPx(),
            )

            val points =
                data.mapIndexed { index, point ->
                    val x =
                        if (data.size == 1) {
                            chartWidth / 2f
                        } else {
                            chartWidth * (index / (data.size - 1).toFloat())
                        }
                    val valueFraction = (point.rowsPerHour / maxPace).coerceIn(0f, 1f)
                    Offset(x, chartHeight - chartHeight * valueFraction)
                }

            // Viiva piirtyy vasemmalta oikealle; täyttö paljastuu ja pisteet ponnahtavat kynän mukana
            val progress = animatable.value
            var penX = points.first().x

            if (points.size > 1 && progress > 0f) {
                val linePath = smoothedLinePath(points)
                val pathMeasure = PathMeasure().apply { setPath(linePath, forceClosed = false) }
                val drawnLength = pathMeasure.length * progress
                penX = pathMeasure.getPosition(drawnLength).x

                val fillPath =
                    Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, zeroY)
                        lineTo(points.first().x, zeroY)
                        close()
                    }
                clipRect(right = penX) {
                    drawPath(
                        path = fillPath,
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        primaryColor.copy(alpha = 0.30f),
                                        primaryColor.copy(alpha = 0.02f),
                                    ),
                                startY = 0f,
                                endY = zeroY,
                            ),
                    )
                }

                val drawnLinePath = Path()
                if (pathMeasure.getSegment(0f, drawnLength, drawnLinePath, startWithMoveTo = true)) {
                    drawPath(
                        path = drawnLinePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            if (progress > 0f) {
                val popRamp = 24.dp.toPx()
                points.forEach { point ->
                    val dotScale =
                        if (points.size == 1) {
                            progress
                        } else {
                            ((penX - point.x + popRamp) / popRamp).coerceIn(0f, 1f)
                        }
                    if (dotScale > 0f) {
                        drawCircle(color = pointRingColor, radius = 5.dp.toPx() * dotScale, center = point)
                        drawCircle(color = primaryColor, radius = 3.dp.toPx() * dotScale, center = point)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        PaceStatsRow(point = data.last())
    }
}

// Pehmennetty käyrä pisteiden välisillä keskipistekontrolleilla
private fun smoothedLinePath(points: List<Offset>): Path =
    Path().apply {
        moveTo(points.first().x, points.first().y)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val midX = (previous.x + current.x) / 2f
            cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
        }
    }

@Composable
private fun TimePerProjectChart(
    data: List<ProjectTime>,
    isPro: Boolean,
    onProUpgrade: () -> Unit,
    animationKey: Any,
) {
    val barColors = InsightChartColors

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        when {
            !isPro -> ChartProPlaceholder(onClick = onProUpgrade)
            data.isEmpty() -> ChartEmptyState()
            else -> {
                val maxMinutes = data.maxOf { it.totalMinutes }.coerceAtLeast(1)
                Column(modifier = Modifier.padding(16.dp)) {
                    data.forEachIndexed { index, project ->
                        val targetFraction = project.totalMinutes / maxMinutes.toFloat()
                        val color = barColors[index % barColors.size]

                        AnimatedBar(
                            state =
                                AnimatedBarState(
                                    projectName =
                                        project.projectName
                                            ?: stringResource(R.string.new_project_name_format, project.projectId),
                                    targetFraction = targetFraction,
                                    totalMinutes = project.totalMinutes,
                                    totalRows = project.totalRows,
                                    lastSessionAt = project.lastSessionAt,
                                    color = color,
                                    animationDelay = 400 + (index * 60),
                                    animationKey = animationKey,
                                ),
                        )

                        if (index < data.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartProPlaceholder(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_feature),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChartEmptyState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(148.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_knitting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaceStatsRow(point: PaceOverTimePoint) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatProjectTime(point.totalMinutes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = pluralStringResource(R.plurals.insights_rows_count, point.totalRows, point.totalRows),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun formatPaceBucketLabel(point: PaceOverTimePoint): String {
    val locale = currentInsightsLocale()
    return when (point.interval) {
        PaceGroupingInterval.DAY -> {
            val formatter =
                remember(locale) {
                    DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "EMMMd"), locale)
                }
            point.bucketStart.format(formatter)
        }

        PaceGroupingInterval.MONTH -> {
            val formatter =
                remember(locale) {
                    DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "yMMM"), locale)
                }
            point.bucketStart.format(formatter)
        }
    }
}

// Tilan ryhmittely AnimatedBarille (S107)
private data class AnimatedBarState(
    val projectName: String,
    val targetFraction: Float,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
    val color: Color,
    val animationDelay: Int,
    val animationKey: Any,
)

@Composable
private fun AnimatedBar(state: AnimatedBarState) {
    val animatable = remember { Animatable(0f) }
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    LaunchedEffect(state.targetFraction, state.animationKey) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = state.targetFraction,
            animationSpec =
                tween(
                    durationMillis = 500,
                    delayMillis = state.animationDelay,
                    easing = FastOutSlowInEasing,
                ),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.projectName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDaysAgo(state.lastSessionAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(20.dp),
    ) {
        val radius = 4.dp.toPx()
        val minVisibleWidth =
            if (state.targetFraction > 0f && animatable.value > 0f) {
                size.width * 0.03f
            } else {
                0f
            }
        val barWidth = maxOf(size.width * animatable.value, minVisibleWidth).coerceAtMost(size.width)
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = CornerRadius(radius, radius),
        )
        if (barWidth > 0f) {
            drawRoundRect(
                color = state.color,
                size = Size(width = barWidth, height = size.height),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
    ProjectStatsRow(totalMinutes = state.totalMinutes, totalRows = state.totalRows)
}

@Composable
private fun ProjectStatsRow(
    totalMinutes: Int,
    totalRows: Int,
) {
    val timeLabel = formatProjectTime(totalMinutes)
    val showRows = totalRows > 0

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (showRows) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = pluralStringResource(R.plurals.insights_rows_count, totalRows, totalRows),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatProjectTime(totalMinutes: Int): String =
    when {
        totalMinutes == 0 -> stringResource(R.string.insights_no_time)
        totalMinutes < 6 -> stringResource(R.string.insights_less_than_tenth_hour)
        else -> stringResource(R.string.time_format_hours, totalMinutes / 60f)
    }

@Composable
private fun formatDaysAgo(timestamp: Long): String {
    val days =
        java.util.concurrent.TimeUnit.MILLISECONDS
            .toDays(System.currentTimeMillis() - timestamp)
            .toInt()
    return when {
        days <= 0 -> stringResource(R.string.relative_time_today)
        days == 1 -> stringResource(R.string.relative_time_yesterday)
        days < 7 -> stringResource(R.string.relative_time_days_ago, days)
        days < 30 -> stringResource(R.string.relative_time_weeks_ago, days / 7)
        else -> stringResource(R.string.relative_time_months_ago, days / 30)
    }
}
