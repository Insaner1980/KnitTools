package com.finnvek.knittools.ui.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    onProUpgrade: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val avgPace by viewModel.avgPace.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val bestStreak by viewModel.bestStreak.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val timePerProject by viewModel.timePerProject.collectAsStateWithLifecycle()
    val dailyActivity by viewModel.dailyActivity.collectAsStateWithLifecycle()
    val timeRange by viewModel.timeRange.collectAsStateWithLifecycle()
    val hasSessionData by viewModel.hasSessionData.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                        formatValue = { context.getString(R.string.time_format_hours, it) },
                        labelColor = MaterialTheme.colorScheme.primary,
                        animationDelay = 0,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AnimatedMetricCard(
                        label = stringResource(R.string.avg_pace_label),
                        targetValue = avgPace,
                        formatValue = { context.getString(R.string.pace_format, it) },
                        labelColor = MaterialTheme.colorScheme.secondary,
                        animationDelay = 80,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AnimatedMetricCard(
                        label = stringResource(R.string.completed_label),
                        targetValue = completedCount.toFloat(),
                        formatValue = { "%.0f".format(it) },
                        labelColor = MaterialTheme.colorScheme.tertiary,
                        animationDelay = 160,
                        animationKey = animationKey,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.insights_knitting_activity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            item {
                ActivityGrid(
                    dailyActivity = dailyActivity,
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    isPro = viewModel.isPro,
                    onProUpgrade = onProUpgrade,
                )
            }

            val displayTimePerProject =
                timePerProject.ifEmpty {
                    if (BuildConfig.DEBUG) {
                        listOf(
                            ProjectTime(1, "Preview Sweater", 142, 86, System.currentTimeMillis()),
                            ProjectTime(2, "Preview Socks", 55, 0, System.currentTimeMillis() - 86_400_000),
                            ProjectTime(3, "Preview Scarf", 23, 41, System.currentTimeMillis() - 432_000_000),
                        )
                    } else {
                        emptyList()
                    }
                }

            if (displayTimePerProject.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.time_per_project),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                item {
                    AnimatedTimePerProjectChart(
                        data = displayTimePerProject,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        animationKey = animationKey,
                    )
                }
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
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatValue(animatable.value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnimatedTimePerProjectChart(
    data: List<ProjectTime>,
    primaryColor: Color,
    animationKey: Any,
) {
    val maxMinutes = data.maxOf { it.totalMinutes }.coerceAtLeast(1)
    val barColors =
        listOf(
            primaryColor,
            Color(0xFF8BA44A),
            Color(0xFFC9A435),
            Color(0xFFB8908F),
            Color(0xFF5F8A8B),
        )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            data.forEachIndexed { index, project ->
                val targetFraction = project.totalMinutes / maxMinutes.toFloat()
                val color = barColors[index % barColors.size]

                AnimatedBar(
                    state =
                        AnimatedBarState(
                            projectName = project.projectName,
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
                text = stringResource(R.string.insights_rows_count, totalRows),
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
