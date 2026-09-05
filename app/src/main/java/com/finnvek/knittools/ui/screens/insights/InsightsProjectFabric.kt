package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor

@Composable
internal fun InsightsProjectFabric(
    model: InsightsProjectFabricModel,
    selectedDate: LocalDate?,
    contentDescription: String,
    previousActiveDayLabel: String,
    nextActiveDayLabel: String,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val locale = rememberCurrentLocale()
    val textMeasurer = rememberTextMeasurer()
    val monthFormatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMM"), locale)
        }
    val monthLabels =
        remember(model, monthFormatter, locale) {
            projectFabricMonthLabels(model, monthFormatter, locale)
        }
    val monthLabelStyle =
        MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.knitToolsColors.onSurfaceMuted)
    val palette = MaterialTheme.knitToolsColors.yarnPalette
    val emptyOutlineColor = projectFabricEmptyOutlineColor(MaterialTheme.colorScheme.outlineVariant)
    val selectionColor = MaterialTheme.colorScheme.onSurface
    val stitchShadowColor =
        MaterialTheme.colorScheme.scrim.copy(alpha = InsightsDimens.ChartStitchShadowAlpha)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = InsightsDimens.ProjectFabricCellGap
        val cellSize = (maxWidth - gap * (PROJECT_FABRIC_WEEK_COUNT - 1)) / PROJECT_FABRIC_WEEK_COUNT
        val canvasHeight =
            InsightsDimens.ProjectFabricMonthLabelHeight +
                cellSize * DAYS_PER_WEEK +
                gap * (DAYS_PER_WEEK - 1)
        val cellSizePx = with(density) { cellSize.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val monthLabelHeightPx = with(density) { InsightsDimens.ProjectFabricMonthLabelHeight.toPx() }
        val strokeWidthPx = with(density) { InsightsDimens.ProjectFabricSelectionStroke.toPx() }
        val cornerPx = with(density) { InsightsDimens.ChartBarCorner.toPx() }
        val lattice =
            remember(
                cellSizePx,
                density,
                InsightsDimens.ChartStitchAspect,
                InsightsDimens.ChartStitchStrokeRatio,
            ) {
                with(density) {
                    knitStitchLattice(
                        widthPx = cellSizePx,
                        heightPx = cellSizePx,
                        targetStitchWidthPx = InsightsDimens.ChartStitchTargetWidth.toPx(),
                    )
                }
            }

        val currentOnSelectDay by rememberUpdatedState(onSelectDay)
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .pointerInput(model, monthLabelHeightPx, gapPx) {
                        detectTapGestures { position ->
                            projectFabricDateAt(
                                x = position.x,
                                y = position.y,
                                availableWidth = size.width.toFloat(),
                                monthLabelHeight = monthLabelHeightPx,
                                gap = gapPx,
                                model = model,
                            )?.let(currentOnSelectDay)
                        }
                    }.clearAndSetSemantics {
                        this.contentDescription = contentDescription
                        customActions =
                            listOf(
                                CustomAccessibilityAction(previousActiveDayLabel) {
                                    moveProjectFabricSelection(model.days, selectedDate, STEP_PREVIOUS)?.let {
                                        currentOnSelectDay(it)
                                        true
                                    } ?: false
                                },
                                CustomAccessibilityAction(nextActiveDayLabel) {
                                    moveProjectFabricSelection(model.days, selectedDate, STEP_NEXT)?.let {
                                        currentOnSelectDay(it)
                                        true
                                    } ?: false
                                },
                            )
                    },
        ) {
            val pitch = cellSizePx + gapPx
            monthLabels.forEach { label ->
                val layout = textMeasurer.measure(text = label.text, style = monthLabelStyle, maxLines = 1)
                drawText(
                    textLayoutResult = layout,
                    topLeft =
                        Offset(
                            x = label.column * pitch,
                            y = (monthLabelHeightPx - layout.size.height) / 2f,
                        ),
                )
            }

            model.days.forEach { day ->
                val dayOffset = ChronoUnit.DAYS.between(model.startDate, day.date).toInt()
                val column = dayOffset / DAYS_PER_WEEK
                val row = projectFabricRowIndex(day.date, model.firstDayOfWeek)
                val topLeft = Offset(column * pitch, monthLabelHeightPx + row * pitch)
                val cellPath = projectFabricCellPath(topLeft, cellSizePx, cornerPx)

                if (day.projectIds.isEmpty()) {
                    drawRoundRect(
                        color = emptyOutlineColor,
                        topLeft = topLeft,
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(cornerPx),
                        style = Stroke(width = strokeWidthPx),
                    )
                } else {
                    clipPath(cellPath) {
                        projectFabricStripeBounds(cellSizePx, day.projectIds.size)
                            .forEachIndexed { index, stripe ->
                                drawRect(
                                    color = yarnColorForId(day.projectIds[index], palette),
                                    topLeft = Offset(topLeft.x + stripe.left, topLeft.y),
                                    size = Size(stripe.right - stripe.left, cellSizePx),
                                )
                            }
                        translate(left = topLeft.x, top = topLeft.y) {
                            drawPath(
                                path = lattice.path,
                                color = stitchShadowColor,
                                style = Stroke(width = lattice.strokeWidthPx, cap = StrokeCap.Round),
                            )
                        }
                    }
                }

                if (day.date == selectedDate) {
                    drawRoundRect(
                        color = selectionColor,
                        topLeft = topLeft,
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(cornerPx),
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
        }
    }
}

internal data class ProjectFabricMonthLabel(
    val column: Int,
    val text: String,
)

internal fun projectFabricMonthLabels(
    model: InsightsProjectFabricModel,
    formatter: DateTimeFormatter,
    locale: Locale,
): List<ProjectFabricMonthLabel> {
    var previousMonth: YearMonth? = null
    return model.days
        .mapNotNull { day ->
            val month = YearMonth.from(day.date)
            if (month == previousMonth) return@mapNotNull null
            previousMonth = month
            ProjectFabricMonthLabel(
                column = ChronoUnit.DAYS.between(model.startDate, day.date).toInt() / DAYS_PER_WEEK,
                text =
                    day.date
                        .format(formatter)
                        .uppercase(locale),
            )
        }.distinctBy { it.column }
}

internal fun projectFabricEmptyOutlineColor(color: Color): Color =
    color.copy(alpha = color.alpha * InsightsDimens.ProjectFabricEmptyCellAlpha)

private fun projectFabricCellPath(
    topLeft: Offset,
    cellSize: Float,
    corner: Float,
): Path =
    Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(offset = topLeft, size = Size(cellSize, cellSize)),
                cornerRadius = CornerRadius(corner.coerceAtMost(cellSize / 2f)),
            ),
        )
    }

internal data class ProjectFabricStripeBounds(
    val left: Float,
    val right: Float,
)

internal fun projectFabricStripeBounds(
    width: Float,
    projectCount: Int,
): List<ProjectFabricStripeBounds> {
    if (width <= 0f || projectCount <= 0) return emptyList()
    val stripeWidth = width / projectCount
    return List(projectCount) { index ->
        ProjectFabricStripeBounds(
            left = stripeWidth * index,
            right = if (index == projectCount - 1) width else stripeWidth * (index + 1),
        )
    }
}

internal fun projectFabricRowIndex(
    date: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Int = (date.dayOfWeek.value - firstDayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK

internal fun moveProjectFabricSelection(
    days: List<InsightsProjectFabricDay>,
    selectedDate: LocalDate?,
    step: Int,
): LocalDate? {
    val activeDates = days.filter { it.projectIds.isNotEmpty() }.map { it.date }
    if (activeDates.isEmpty()) return null
    if (selectedDate == null) return if (step < 0) activeDates.last() else activeDates.first()
    val selectedIndex = activeDates.indexOf(selectedDate)
    if (selectedIndex < 0) return if (step < 0) activeDates.last() else activeDates.first()
    return activeDates[(selectedIndex + step).mod(activeDates.size)]
}

internal fun projectFabricDateAt(
    x: Float,
    y: Float,
    availableWidth: Float,
    monthLabelHeight: Float,
    gap: Float,
    model: InsightsProjectFabricModel,
): LocalDate? {
    if (x < 0f || y < monthLabelHeight || availableWidth <= 0f || gap < 0f) return null
    val cellSize = (availableWidth - (PROJECT_FABRIC_WEEK_COUNT - 1) * gap) / PROJECT_FABRIC_WEEK_COUNT
    if (cellSize <= 0f) return null
    val pitch = cellSize + gap
    val column = floor(x / pitch).toInt()
    val gridY = y - monthLabelHeight
    val row = floor(gridY / pitch).toInt()
    if (column !in 0 until PROJECT_FABRIC_WEEK_COUNT || row !in 0 until DAYS_PER_WEEK) return null
    if (x - column * pitch >= cellSize || gridY - row * pitch >= cellSize) return null

    val date = model.startDate.plusDays((column * DAYS_PER_WEEK + row).toLong())
    return date.takeIf { !it.isAfter(model.endDate) && model.days.any { day -> day.date == it } }
}

private const val DAYS_PER_WEEK = 7
