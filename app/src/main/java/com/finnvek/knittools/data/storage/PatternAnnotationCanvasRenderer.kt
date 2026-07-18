package com.finnvek.knittools.data.storage

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object PatternAnnotationCanvasRenderer {
    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        annotations: List<PatternAnnotation>,
        style: PatternAnnotationRenderStyle,
    ) {
        if (width <= 0f || height <= 0f) return
        annotations.forEach { annotation -> renderAnnotation(canvas, width, height, annotation, style) }
    }

    private fun renderAnnotation(
        canvas: Canvas,
        width: Float,
        height: Float,
        annotation: PatternAnnotation,
        style: PatternAnnotationRenderStyle,
    ) {
        when (val payload = annotation.payload) {
            is FreehandPayload -> renderFreehand(canvas, width, height, annotation.kind, payload, style)
            is ShapePayload -> renderShape(canvas, width, height, annotation.kind, payload, style)
            is TextBoxPayload -> renderTextBox(canvas, width, height, payload, style)
            is CalloutPayload -> renderCallout(canvas, width, height, payload, style)
            is ChartRegionPayload -> renderChartRegion(canvas, width, height, payload, style)
            is ChartTrackerPayload -> renderChartTracker(canvas, width, height, payload, style)
        }
    }

    private fun renderFreehand(
        canvas: Canvas,
        width: Float,
        height: Float,
        kind: PatternAnnotationKind,
        payload: FreehandPayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val paint = strokePaint(payload.argb, scaledStrokeWidth(payload.strokeWidth, width, height, style))
        if (kind == PatternAnnotationKind.HIGHLIGHTER && Color.alpha(payload.argb) == 255) {
            paint.alpha = style.highlighterDefaultAlpha
        }
        if (payload.points.isNotEmpty()) {
            val first = payload.points.first()
            if (payload.pressureEnabled && payload.points.size > 1) {
                payload.points.zipWithNext().forEach { (start, end) ->
                    paint.strokeWidth =
                        scaledStrokeWidth(payload.strokeWidth, width, height, style) *
                        ((start.pressure + end.pressure) / 2f).coerceAtLeast(style.minimumPressureScale)
                    canvas.drawLine(start.x * width, start.y * height, end.x * width, end.y * height, paint)
                }
                return
            }
            val path = Path().apply { moveTo(first.x * width, first.y * height) }
            payload.points.drop(1).forEach { point -> path.lineTo(point.x * width, point.y * height) }
            if (payload.points.size == 1) {
                canvas.drawCircle(first.x * width, first.y * height, paint.strokeWidth / 2f, paint)
            } else {
                canvas.drawPath(path, paint)
            }
            return
        }
        renderLegacyPath(canvas, width, height, payload, paint)
    }

    private fun renderLegacyPath(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: FreehandPayload,
        paint: Paint,
    ) {
        val path =
            runCatching { PathParser.createPathFromPathData(payload.legacyPathData.orEmpty()) }.getOrNull() ?: return
        val bounds = RectF().also(path::computeBounds)
        if (bounds.right <= NORMALIZED_LEGACY_PATH_LIMIT && bounds.bottom <= NORMALIZED_LEGACY_PATH_LIMIT) {
            path.transform(Matrix().apply { setScale(width, height) })
        }
        canvas.drawPath(path, paint)
    }

    private fun renderShape(
        canvas: Canvas,
        width: Float,
        height: Float,
        kind: PatternAnnotationKind,
        payload: ShapePayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val startX = payload.start.x * width
        val startY = payload.start.y * height
        val endX = payload.end.x * width
        val endY = payload.end.y * height
        payload.fillArgb?.let { fillArgb ->
            if (payload.fillAlpha > 0f &&
                kind in setOf(PatternAnnotationKind.RECTANGLE, PatternAnnotationKind.ELLIPSE)
            ) {
                val fill = fillPaint(fillArgb, payload.fillAlpha)
                val bounds = RectF(minOf(startX, endX), minOf(startY, endY), maxOf(startX, endX), maxOf(startY, endY))
                if (kind ==
                    PatternAnnotationKind.RECTANGLE
                ) {
                    canvas.drawRect(bounds, fill)
                } else {
                    canvas.drawOval(bounds, fill)
                }
            }
        }
        val stroke = strokePaint(payload.strokeArgb, scaledStrokeWidth(payload.strokeWidth, width, height, style))
        when (kind) {
            PatternAnnotationKind.LINE -> canvas.drawLine(startX, startY, endX, endY, stroke)
            PatternAnnotationKind.ARROW -> drawArrow(canvas, startX, startY, endX, endY, stroke, style)
            PatternAnnotationKind.RECTANGLE ->
                canvas.drawRect(
                    minOf(startX, endX),
                    minOf(startY, endY),
                    maxOf(startX, endX),
                    maxOf(startY, endY),
                    stroke,
                )
            PatternAnnotationKind.ELLIPSE ->
                canvas.drawOval(
                    RectF(minOf(startX, endX), minOf(startY, endY), maxOf(startX, endX), maxOf(startY, endY)),
                    stroke,
                )
            else -> Unit
        }
    }

    private fun renderTextBox(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: TextBoxPayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val bounds = payload.bounds.toRect(width, height)
        payload.backgroundArgb?.let { canvas.drawRect(bounds, fillPaint(it, payload.backgroundAlpha)) }
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = payload.textArgb
                textSize = payload.textSizeSp * min(width, height) / style.referencePageSize
            }
        var baseline = bounds.top - paint.fontMetrics.top
        payload.text.lineSequence().forEach { line ->
            if (baseline <= bounds.bottom) canvas.drawText(line, bounds.left, baseline, paint)
            baseline += paint.fontSpacing
        }
    }

    private fun renderCallout(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: CalloutPayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val bounds = payload.bounds.toRect(width, height)
        val cornerRadius = style.calloutCornerRadius * min(width, height) / style.referencePageSize
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, fillPaint(payload.argb, style.calloutBackgroundAlpha))
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = payload.argb
                textSize = style.calloutTextSize * min(width, height) / style.referencePageSize
            }
        val text = listOf(payload.title, payload.description).filter(String::isNotBlank)
        var baseline = bounds.top - paint.fontMetrics.top
        text.forEach { line ->
            if (baseline <= bounds.bottom) canvas.drawText(line, bounds.left + paint.textSize / 2f, baseline, paint)
            baseline += paint.fontSpacing
        }
    }

    private fun renderChartRegion(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: ChartRegionPayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val bounds = payload.bounds.toRect(width, height)
        val paint =
            strokePaint(
                style.chartGridArgb,
                scaledStrokeWidth(style.chartGridWidth, width, height, style),
            )
        canvas.drawRect(bounds, paint)
        repeat(payload.columns - 1) { index ->
            val x = bounds.left + bounds.width() * (index + 1) / payload.columns
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint)
        }
        repeat(payload.rows - 1) { index ->
            val y = bounds.top + bounds.height() * (index + 1) / payload.rows
            canvas.drawLine(bounds.left, y, bounds.right, y, paint)
        }
    }

    private fun renderChartTracker(
        canvas: Canvas,
        width: Float,
        height: Float,
        payload: ChartTrackerPayload,
        style: PatternAnnotationRenderStyle,
    ) {
        val bounds = payload.region.bounds.toRect(width, height)
        canvas.drawRect(bounds, fillPaint(payload.highlightArgb, payload.highlightAlpha))
        renderChartRegion(canvas, width, height, payload.region, style)
    }

    private fun drawArrow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        paint: Paint,
        style: PatternAnnotationRenderStyle,
    ) {
        canvas.drawLine(startX, startY, endX, endY, paint)
        val angle = atan2(endY - startY, endX - startX)
        val size = paint.strokeWidth * style.arrowHeadMultiplier
        canvas.drawLine(
            endX,
            endY,
            endX - size * cos(angle - style.arrowHeadAngle),
            endY - size * sin(angle - style.arrowHeadAngle),
            paint,
        )
        canvas.drawLine(
            endX,
            endY,
            endX - size * cos(angle + style.arrowHeadAngle),
            endY - size * sin(angle + style.arrowHeadAngle),
            paint,
        )
    }

    private fun strokePaint(
        argb: Int,
        width: Float,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = argb
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun fillPaint(
        argb: Int,
        alpha: Float,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = argb
        this.alpha = (Color.alpha(argb) * alpha.coerceIn(0f, 1f)).toInt()
        style = Paint.Style.FILL
    }

    private fun scaledStrokeWidth(
        strokeWidth: Float,
        width: Float,
        height: Float,
        style: PatternAnnotationRenderStyle,
    ): Float = strokeWidth * min(width, height) / style.referencePageSize

    private fun NormalizedPatternBounds.toRect(
        width: Float,
        height: Float,
    ) = RectF(left * width, top * height, right * width, bottom * height)

    private const val NORMALIZED_LEGACY_PATH_LIMIT = 1.5f
}

data class PatternAnnotationRenderStyle(
    val referencePageSize: Float,
    val highlighterDefaultAlpha: Int,
    val calloutBackgroundAlpha: Float,
    val calloutTextSize: Float,
    val calloutCornerRadius: Float,
    val chartGridArgb: Int,
    val chartGridWidth: Float,
    val arrowHeadMultiplier: Float,
    val arrowHeadAngle: Float,
    val minimumPressureScale: Float,
)
