package com.finnvek.knittools.ui.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private const val WEEKS = 8
private const val DAYS_PER_WEEK = 7
private val CELL_GAP = 3.dp
private val CELL_SHAPE = RoundedCornerShape(3.dp)
private val DAY_LABEL_WIDTH = 20.dp
private val MAX_CELL_SIZE = 28.dp
private val CARD_PADDING_H = 16.dp
private val CARD_PADDING_V = 12.dp

@Composable
fun ActivityGrid(
    dailyActivity: Map<LocalDate, Int>,
    currentStreak: Int,
    bestStreak: Int,
    isPro: Boolean,
    onProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (isPro) {
            ActivityGridContent(
                dailyActivity = dailyActivity,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
            )
        } else {
            ActivityGridPlaceholder(onClick = onProUpgrade)
        }
    }
}

@Composable
private fun ActivityGridContent(
    dailyActivity: Map<LocalDate, Int>,
    currentStreak: Int,
    bestStreak: Int,
) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val dayOrder =
        remember(firstDayOfWeek) {
            (0 until DAYS_PER_WEEK).map { firstDayOfWeek.plus(it.toLong()) }
        }
    val today = remember { LocalDate.now() }
    val gridStartDate =
        remember(today, firstDayOfWeek) {
            val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            currentWeekStart.minusWeeks(WEEKS.toLong() - 1)
        }

    var tooltipState by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CARD_PADDING_H, vertical = CARD_PADDING_V),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidth = maxWidth - DAY_LABEL_WIDTH
        val totalGaps = CELL_GAP * (WEEKS - 1)
        val naturalCellSize = (availableWidth - totalGaps) / WEEKS
        val cellSize = min(naturalCellSize, MAX_CELL_SIZE)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ActivityGridRows(
                dayOrder = dayOrder,
                gridStartDate = gridStartDate,
                today = today,
                dailyActivity = dailyActivity,
                locale = locale,
                cellSize = cellSize,
                onCellLongPress = { date, minutes -> tooltipState = date to minutes },
            )

            Spacer(modifier = Modifier.height(8.dp))
            StreakRow(currentStreak = currentStreak, bestStreak = bestStreak)

            ActivityGridTooltip(tooltipState = tooltipState, locale = locale)
        }
    }
}

@Composable
private fun ActivityGridRows(
    dayOrder: List<DayOfWeek>,
    gridStartDate: LocalDate,
    today: LocalDate,
    dailyActivity: Map<LocalDate, Int>,
    locale: Locale,
    cellSize: Dp,
    onCellLongPress: (LocalDate, Int) -> Unit,
) {
    for (rowIndex in 0 until DAYS_PER_WEEK) {
        val dayOfWeek = dayOrder[rowIndex]
        ActivityGridRow(
            dayOfWeek = dayOfWeek,
            rowIndex = rowIndex,
            gridStartDate = gridStartDate,
            today = today,
            dailyActivity = dailyActivity,
            locale = locale,
            cellSize = cellSize,
            onCellLongPress = onCellLongPress,
        )
        if (rowIndex < DAYS_PER_WEEK - 1) {
            Spacer(modifier = Modifier.height(CELL_GAP))
        }
    }
}

@Composable
private fun ActivityGridRow(
    dayOfWeek: DayOfWeek,
    rowIndex: Int,
    gridStartDate: LocalDate,
    today: LocalDate,
    dailyActivity: Map<LocalDate, Int>,
    locale: Locale,
    cellSize: Dp,
    onCellLongPress: (LocalDate, Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (rowIndex % 2 == 0) {
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                modifier = Modifier.width(DAY_LABEL_WIDTH),
            )
        } else {
            Spacer(modifier = Modifier.width(DAY_LABEL_WIDTH))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            for (colIndex in 0 until WEEKS) {
                val date = gridStartDate.plusWeeks(colIndex.toLong()).plusDays(rowIndex.toLong())
                val minutes = dailyActivity[date] ?: 0

                ActivityCell(
                    minutes = minutes,
                    isFuture = date.isAfter(today),
                    isToday = date == today,
                    cellSize = cellSize,
                    columnIndex = colIndex,
                    rowIndex = rowIndex,
                    onLongPress = { onCellLongPress(date, minutes) },
                )
            }
        }
    }
}

@Composable
private fun StreakRow(
    currentStreak: Int,
    bestStreak: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StreakStat(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.insights_current_streak),
            value = stringResource(R.string.streak_format, currentStreak),
        )
        Spacer(modifier = Modifier.width(12.dp))
        StreakStat(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.insights_best_streak),
            value = stringResource(R.string.streak_format, bestStreak),
            alignEnd = true,
        )
    }
}

@Composable
private fun ActivityGridTooltip(
    tooltipState: Pair<LocalDate, Int>?,
    locale: Locale,
) {
    tooltipState?.let { (date, minutes) ->
        Spacer(modifier = Modifier.height(8.dp))
        val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        val dateStr = date.format(formatter)
        val minutesText =
            if (minutes > 0) {
                stringResource(R.string.time_spent_minutes_format, minutes)
            } else {
                stringResource(R.string.no_knitting)
            }
        Text(
            text = "$dayName $dateStr — $minutesText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreakStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityCell(
    minutes: Int,
    isFuture: Boolean,
    isToday: Boolean,
    cellSize: Dp,
    columnIndex: Int,
    rowIndex: Int,
    onLongPress: () -> Unit,
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = 300,
                    delayMillis = 700 + (columnIndex + rowIndex) * 20,
                    easing = FastOutSlowInEasing,
                ),
        )
    }

    val hasActivity = !isFuture && minutes > 0
    val fillColor = cellFillColor(minutes = minutes, isFuture = isFuture)
    val outlineColor = MaterialTheme.knitToolsColors.onSurfaceMuted.copy(alpha = 0.2f)
    val todayBorderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            Modifier
                .size(cellSize)
                .alpha(animatable.value)
                .clip(CELL_SHAPE)
                .background(fillColor)
                .then(
                    if (isToday) {
                        Modifier.border(2.dp, todayBorderColor, CELL_SHAPE)
                    } else if (!hasActivity) {
                        Modifier.border(1.dp, outlineColor, CELL_SHAPE)
                    } else {
                        Modifier
                    },
                ).combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress,
                ),
    )
}

@Composable
private fun cellFillColor(
    minutes: Int,
    isFuture: Boolean,
): Color {
    if (isFuture) return Color.Transparent
    val tertiary = MaterialTheme.colorScheme.tertiary
    return when {
        minutes <= 0 -> Color.Transparent
        minutes <= 15 -> tertiary.copy(alpha = 0.4f)
        minutes <= 45 -> tertiary.copy(alpha = 0.7f)
        else -> tertiary
    }
}

@Composable
private fun ActivityGridPlaceholder(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_feature),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
