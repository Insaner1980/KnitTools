package com.finnvek.knittools.ui.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.floor

@Composable
internal fun InsightsFabric(
    model: InsightsFabricModel,
    selectedIndex: Int?,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val emptyInk = MaterialTheme.colorScheme.onSurface.copy(alpha = InsightsDimens.RuleHairlineAlpha)
    val selectedInk = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.knitToolsColors.onSurfaceMuted
    val labelStyle =
        MaterialTheme.typography.labelMedium.copy(
            color = labelColor,
            fontSize = InsightsDimens.ChartAxisLabelFontSize,
            fontWeight = FontWeight.SemiBold,
        )
    val textMeasurer = rememberTextMeasurer()
    val locale = currentInsightsLocale()
    val monthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMM"), locale)
        }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthPx = constraints.maxWidth.toFloat()
        val minCellSizePx = with(density) { InsightsDimens.FabricMinCellSize.toPx() }
        val maxVisibleWeeks = floor(availableWidthPx / minCellSizePx).toInt().coerceAtLeast(1)
        val slice = remember(model, maxVisibleWeeks) { visibleFabricSlice(model, maxVisibleWeeks) }
        val cellSizePx =
            if (slice.weekCount > 0) {
                (availableWidthPx / slice.weekCount).coerceAtLeast(minCellSizePx)
            } else {
                minCellSizePx
            }
        val cellSize = with(density) { cellSizePx.toDp() }
        val effectiveSelectedIndex = resolveFabricSelectedDayIndex(slice, selectedIndex)
        val currentSelectedIndex by rememberUpdatedState(effectiveSelectedIndex)
        val currentOnSelectDay by rememberUpdatedState(onSelectDay)
        val selectedDay = effectiveSelectedIndex?.let(model.days::getOrNull)
        val monthLabelIndices =
            remember(slice, model.firstDayOfWeek) {
                fabricMonthLabelIndices(slice, model.firstDayOfWeek, FABRIC_MONTH_MAX_LABELS)
            }
        val monthLabels =
            remember(monthLabelIndices, model.days, monthFormatter, labelStyle, textMeasurer) {
                monthLabelIndices.mapNotNull { originalIndex ->
                    val day = model.days.getOrNull(originalIndex) ?: return@mapNotNull null
                    val cell = fabricCellForDate(day.date, slice, model.firstDayOfWeek) ?: return@mapNotNull null
                    cell to textMeasurer.measure(day.date.format(monthFormatter), labelStyle, maxLines = 1)
                }
            }
        val accessibility = remember(slice) { fabricAccessibilitySummary(slice) }
        val accessibilityDescription =
            stringResource(
                R.string.insights_fabric_a11y_summary,
                accessibility.activeDays,
                accessibility.shownDays,
                durationText(DurationDisplayFormatter.fromMinutes(accessibility.totalMinutes)),
            )
        val nextLabel = stringResource(R.string.insights_fabric_a11y_next)
        val previousLabel = stringResource(R.string.insights_fabric_a11y_previous)
        val entranceProgress =
            remember(slice.originalStartIndex, slice.days.size) {
                Animatable(if (slice.days.size < FABRIC_ANIMATION_MIN_DAYS) 1f else 0f)
            }
        val dayIndexAtPosition: (Offset) -> Int? = { position ->
            if (position.y !in 0f..<(cellSizePx * DAYS_PER_WEEK)) {
                null
            } else {
                fabricOriginalIndexAtCell(
                    slice,
                    InsightsFabricCell(
                        weekColumn = floor(position.x / cellSizePx).toInt(),
                        weekdayRow = floor(position.y / cellSizePx).toInt(),
                    ),
                )
            }
        }

        LaunchedEffect(slice.originalStartIndex, slice.days.size) {
            if (slice.days.size < FABRIC_ANIMATION_MIN_DAYS) {
                entranceProgress.snapTo(1f)
            } else {
                entranceProgress.snapTo(0f)
                entranceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(FABRIC_ENTRANCE_MILLIS, easing = FastOutSlowInEasing),
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            ChartReadout(
                readout =
                    selectedDay?.let { day ->
                        InsightsReadout(day.date, day.totalMinutes, day.totalRows)
                    },
                fallbackDate = slice.days.firstOrNull()?.date,
                hasAnyData = model.days.any { it.totalMinutes > 0 },
            )
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(cellSize * DAYS_PER_WEEK + InsightsDimens.FabricLabelBandHeight)
                        .clearAndSetSemantics {
                            contentDescription = accessibilityDescription
                            val visibleSelectedIndex =
                                effectiveSelectedIndex?.minus(slice.originalStartIndex)
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(nextLabel) {
                                        moveSelection(slice.days.size, visibleSelectedIndex, STEP_NEXT) {
                                            onSelectDay(slice.originalStartIndex + it)
                                        }
                                    },
                                    CustomAccessibilityAction(previousLabel) {
                                        moveSelection(slice.days.size, visibleSelectedIndex, STEP_PREVIOUS) {
                                            onSelectDay(slice.originalStartIndex + it)
                                        }
                                    },
                                )
                        }.pointerInput(
                            slice.originalStartIndex,
                            slice.days.size,
                            slice.weekCount,
                            cellSizePx,
                        ) {
                            fun selectWithHaptic(index: Int) {
                                if (index != currentSelectedIndex) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                currentOnSelectDay(index)
                            }

                            detectTapGestures { position ->
                                dayIndexAtPosition(position)?.let(::selectWithHaptic)
                            }
                        }.pointerInput(
                            slice.originalStartIndex,
                            slice.days.size,
                            slice.weekCount,
                            cellSizePx,
                        ) {
                            var lastSelectedIndex: Int? = null
                            detectHorizontalDragGestures(
                                onDragStart = { position ->
                                    lastSelectedIndex =
                                        dayIndexAtPosition(position)?.also { index ->
                                            if (index != currentSelectedIndex) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            currentOnSelectDay(index)
                                        }
                                },
                            ) { change, _ ->
                                val index = dayIndexAtPosition(change.position)
                                if (index != null && index != lastSelectedIndex) {
                                    lastSelectedIndex = index
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnSelectDay(index)
                                }
                            }
                        },
            ) {
                slice.days.forEach { day ->
                    val cell = fabricCellForDate(day.date, slice, model.firstDayOfWeek) ?: return@forEach
                    val columnProgress =
                        (entranceProgress.value * slice.weekCount - cell.weekColumn).coerceIn(0f, 1f)
                    if (columnProgress <= 0f) return@forEach
                    val ink =
                        if (day.level == 0) {
                            emptyInk.copy(alpha = emptyInk.alpha * columnProgress)
                        } else {
                            yarnColorForId(day.dominantProjectId ?: 0L).copy(
                                alpha =
                                    when (day.level) {
                                        1 -> InsightsDimens.FabricInkLevel1
                                        2 -> InsightsDimens.FabricInkLevel2
                                        else -> InsightsDimens.FabricInkLevel3
                                    } * columnProgress,
                            )
                        }
                    val left = cell.weekColumn * cellSizePx
                    val top = cell.weekdayRow * cellSizePx
                    val strokeWidth = cellSizePx / InsightsDimens.FabricStitchStrokeRatio
                    val stitchTop = top + cellSizePx * FABRIC_STITCH_TOP_FRACTION
                    val stitchBottom = top + cellSizePx * FABRIC_STITCH_BOTTOM_FRACTION
                    val stitchMiddleX = left + cellSizePx / 2f
                    drawLine(
                        color = ink,
                        start = Offset(left + cellSizePx * FABRIC_STITCH_SIDE_FRACTION, stitchTop),
                        end = Offset(stitchMiddleX, stitchBottom),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = ink,
                        start = Offset(stitchMiddleX, stitchBottom),
                        end = Offset(left + cellSizePx * (1f - FABRIC_STITCH_SIDE_FRACTION), stitchTop),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                effectiveSelectedIndex
                    ?.let(model.days::getOrNull)
                    ?.let { fabricCellForDate(it.date, slice, model.firstDayOfWeek) }
                    ?.let { cell ->
                        val outlineWidth = InsightsDimens.FabricSelectedOutlineWidth.toPx()
                        val inset = outlineWidth / 2f
                        drawRoundRect(
                            color = selectedInk,
                            topLeft =
                                Offset(
                                    cell.weekColumn * cellSizePx + inset,
                                    cell.weekdayRow * cellSizePx + inset,
                                ),
                            size = Size(cellSizePx - outlineWidth, cellSizePx - outlineWidth),
                            cornerRadius = CornerRadius(cellSizePx * FABRIC_OUTLINE_CORNER_FRACTION),
                            style = Stroke(width = outlineWidth),
                        )
                    }

                val labelBandTop = cellSizePx * DAYS_PER_WEEK
                monthLabels.forEach { (cell, layout) ->
                    drawText(
                        textLayoutResult = layout,
                        topLeft =
                            Offset(
                                x = cell.weekColumn * cellSizePx + (cellSizePx - layout.size.width) / 2f,
                                y = labelBandTop + (size.height - labelBandTop - layout.size.height) / 2f,
                            ),
                    )
                }
            }
        }
    }
}

internal data class InsightsFabricVisibleSlice(
    val days: List<InsightsFabricDay>,
    val originalStartIndex: Int,
    val weekCount: Int,
)

internal data class InsightsFabricCell(
    val weekColumn: Int,
    val weekdayRow: Int,
)

internal data class InsightsFabricAccessibilitySummary(
    val activeDays: Int,
    val shownDays: Int,
    val totalMinutes: Int,
)

internal fun visibleFabricSlice(
    model: InsightsFabricModel,
    maxVisibleWeeks: Int,
): InsightsFabricVisibleSlice {
    if (model.days.isEmpty() || model.weekCount <= 0) {
        return InsightsFabricVisibleSlice(emptyList(), originalStartIndex = 0, weekCount = 0)
    }
    val visibleWeekCount = minOf(model.weekCount, maxVisibleWeeks.coerceAtLeast(1))
    val originalStartIndex = ((model.weekCount - visibleWeekCount) * DAYS_PER_WEEK).coerceAtMost(model.days.size)
    return InsightsFabricVisibleSlice(
        days = model.days.drop(originalStartIndex),
        originalStartIndex = originalStartIndex,
        weekCount = visibleWeekCount,
    )
}

internal fun defaultSelectedFabricDayIndex(slice: InsightsFabricVisibleSlice): Int? {
    val visibleIndex = slice.days.indexOfLast { it.totalMinutes > 0 }
    return visibleIndex.takeIf { it >= 0 }?.let(slice.originalStartIndex::plus)
}

internal fun resolveFabricSelectedDayIndex(
    slice: InsightsFabricVisibleSlice,
    selectedIndex: Int?,
): Int? =
    selectedIndex
        ?.takeIf { it >= slice.originalStartIndex && it < slice.originalStartIndex + slice.days.size }
        ?: defaultSelectedFabricDayIndex(slice)

internal fun fabricCellForDate(
    date: LocalDate,
    slice: InsightsFabricVisibleSlice,
    firstDayOfWeek: DayOfWeek,
): InsightsFabricCell? {
    val firstDate = slice.days.firstOrNull()?.date ?: return null
    val visibleIndex = ChronoUnit.DAYS.between(firstDate, date).toInt()
    if (visibleIndex !in slice.days.indices || slice.days[visibleIndex].date != date) return null
    val weekdayRow = Math.floorMod(date.dayOfWeek.value - firstDayOfWeek.value, DAYS_PER_WEEK)
    val weekColumn = visibleIndex / DAYS_PER_WEEK
    return InsightsFabricCell(weekColumn = weekColumn, weekdayRow = weekdayRow)
}

internal fun fabricOriginalIndexAtCell(
    slice: InsightsFabricVisibleSlice,
    cell: InsightsFabricCell,
): Int? {
    if (cell.weekColumn !in 0 until slice.weekCount || cell.weekdayRow !in 0 until DAYS_PER_WEEK) return null
    val visibleIndex = cell.weekColumn * DAYS_PER_WEEK + cell.weekdayRow
    return visibleIndex.takeIf { it in slice.days.indices }?.let(slice.originalStartIndex::plus)
}

internal fun fabricMonthLabelIndices(
    slice: InsightsFabricVisibleSlice,
    firstDayOfWeek: DayOfWeek,
    maxLabels: Int,
): List<Int> {
    val candidates =
        slice.days.mapIndexedNotNull { visibleIndex, day ->
            if (day.date.dayOfMonth == 1 && fabricCellForDate(day.date, slice, firstDayOfWeek) != null) {
                slice.originalStartIndex + visibleIndex
            } else {
                null
            }
        }
    return axisLabelIndices(candidates.size, maxLabels).map(candidates::get)
}

internal fun fabricAccessibilitySummary(slice: InsightsFabricVisibleSlice): InsightsFabricAccessibilitySummary =
    InsightsFabricAccessibilitySummary(
        activeDays = slice.days.count { it.totalMinutes > 0 },
        shownDays = slice.days.size,
        totalMinutes = slice.days.sumOf { it.totalMinutes },
    )

private const val DAYS_PER_WEEK = 7
private const val FABRIC_MONTH_MAX_LABELS = 6
private const val FABRIC_ANIMATION_MIN_DAYS = 14
private const val FABRIC_ENTRANCE_MILLIS = 600
private const val FABRIC_STITCH_SIDE_FRACTION = 0.2f
private const val FABRIC_STITCH_TOP_FRACTION = 0.26f
private const val FABRIC_STITCH_BOTTOM_FRACTION = 0.74f
private const val FABRIC_OUTLINE_CORNER_FRACTION = 0.18f
