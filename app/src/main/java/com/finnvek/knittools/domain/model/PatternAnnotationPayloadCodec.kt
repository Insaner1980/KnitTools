package com.finnvek.knittools.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.math.round

object PatternAnnotationPayloadCodec {
    const val CURRENT_VERSION = 1
    const val MAX_PAYLOAD_BYTES = 256 * 1_024

    private val payloadJson =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun encode(
        kind: PatternAnnotationKind,
        payload: PatternAnnotationPayload,
    ): EncodedPatternAnnotationPayload? {
        val sanitized = sanitizePayload(kind, payload) ?: return null
        val json = encodeSanitizedPayload(kind, sanitized) ?: return null
        if (json.encodeToByteArray().size > MAX_PAYLOAD_BYTES) return null
        return EncodedPatternAnnotationPayload(CURRENT_VERSION, json)
    }

    fun decode(
        kind: PatternAnnotationKind,
        encoded: EncodedPatternAnnotationPayload,
    ): PatternAnnotationPayload? = decode(kind, encoded.payloadVersion, encoded.payloadJson)

    fun decode(
        kind: PatternAnnotationKind,
        payloadVersion: Int,
        json: String,
    ): PatternAnnotationPayload? {
        if (payloadVersion != CURRENT_VERSION || json.encodeToByteArray().size > MAX_PAYLOAD_BYTES) return null
        val decoded =
            runCatching {
                when (kind) {
                    PatternAnnotationKind.FREEHAND,
                    PatternAnnotationKind.HIGHLIGHTER,
                    -> payloadJson.decodeFromString(FreehandPayload.serializer(), json)

                    PatternAnnotationKind.LINE,
                    PatternAnnotationKind.ARROW,
                    PatternAnnotationKind.RECTANGLE,
                    PatternAnnotationKind.ELLIPSE,
                    -> payloadJson.decodeFromString(ShapePayload.serializer(), json)

                    PatternAnnotationKind.TEXT_BOX ->
                        payloadJson.decodeFromString(
                            TextBoxPayload.serializer(),
                            json,
                        )

                    PatternAnnotationKind.CALLOUT ->
                        payloadJson.decodeFromString(
                            CalloutPayload.serializer(),
                            json,
                        )

                    PatternAnnotationKind.CHART_REGION ->
                        payloadJson.decodeFromString(
                            ChartRegionPayload.serializer(),
                            json,
                        )

                    PatternAnnotationKind.CHART_TRACKER ->
                        payloadJson.decodeFromString(
                            ChartTrackerPayload.serializer(),
                            json,
                        )
                }
            }.getOrNull() ?: return null
        return sanitizePayload(kind, decoded)
    }

    private fun encodeSanitizedPayload(
        kind: PatternAnnotationKind,
        payload: PatternAnnotationPayload,
    ): String? =
        when (kind) {
            PatternAnnotationKind.FREEHAND,
            PatternAnnotationKind.HIGHLIGHTER,
            -> encodeTyped(payload, FreehandPayload.serializer())

            PatternAnnotationKind.LINE,
            PatternAnnotationKind.ARROW,
            PatternAnnotationKind.RECTANGLE,
            PatternAnnotationKind.ELLIPSE,
            -> encodeTyped(payload, ShapePayload.serializer())

            PatternAnnotationKind.TEXT_BOX -> encodeTyped(payload, TextBoxPayload.serializer())
            PatternAnnotationKind.CALLOUT -> encodeTyped(payload, CalloutPayload.serializer())
            PatternAnnotationKind.CHART_REGION -> encodeTyped(payload, ChartRegionPayload.serializer())
            PatternAnnotationKind.CHART_TRACKER -> encodeTyped(payload, ChartTrackerPayload.serializer())
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : PatternAnnotationPayload> encodeTyped(
        payload: PatternAnnotationPayload,
        serializer: KSerializer<T>,
    ): String? = runCatching { payloadJson.encodeToString(serializer, payload as T) }.getOrNull()
}

private fun sanitizePayload(
    kind: PatternAnnotationKind,
    payload: PatternAnnotationPayload,
): PatternAnnotationPayload? =
    when (kind) {
        PatternAnnotationKind.FREEHAND,
        PatternAnnotationKind.HIGHLIGHTER,
        -> (payload as? FreehandPayload)?.sanitize()

        PatternAnnotationKind.LINE,
        PatternAnnotationKind.ARROW,
        PatternAnnotationKind.RECTANGLE,
        PatternAnnotationKind.ELLIPSE,
        -> (payload as? ShapePayload)?.sanitize()

        PatternAnnotationKind.TEXT_BOX -> (payload as? TextBoxPayload)?.sanitize()
        PatternAnnotationKind.CALLOUT -> (payload as? CalloutPayload)?.sanitize()
        PatternAnnotationKind.CHART_REGION -> (payload as? ChartRegionPayload)?.sanitize()
        PatternAnnotationKind.CHART_TRACKER -> (payload as? ChartTrackerPayload)?.sanitize()
    }

private fun FreehandPayload.sanitize(): FreehandPayload? {
    if (points.isEmpty() || points.size > PatternAnnotationLimits.MAX_FREEHAND_POINTS) return null
    val sanitizedPoints = points.map { point -> point.sanitize() ?: return null }
    val sanitizedWidth = strokeWidth.sanitizeStrokeWidth() ?: return null
    return copy(points = sanitizedPoints, strokeWidth = sanitizedWidth)
}

private fun ShapePayload.sanitize(): ShapePayload? =
    copy(
        start = start.sanitize() ?: return null,
        end = end.sanitize() ?: return null,
        strokeWidth = strokeWidth.sanitizeStrokeWidth() ?: return null,
        fillAlpha = fillAlpha.sanitizeUnit() ?: return null,
    )

private fun TextBoxPayload.sanitize(): TextBoxPayload? =
    copy(
        bounds = bounds.sanitize() ?: return null,
        textSizeSp =
            textSizeSp
                .takeIf(Float::isFinite)
                ?.coerceIn(PatternAnnotationLimits.MIN_TEXT_SIZE_SP, PatternAnnotationLimits.MAX_TEXT_SIZE_SP)
                ?.roundToAnnotationPrecision() ?: return null,
        backgroundAlpha = backgroundAlpha.sanitizeUnit() ?: return null,
    )

private fun CalloutPayload.sanitize(): CalloutPayload? = copy(bounds = bounds.sanitize() ?: return null)

private fun ChartRegionPayload.sanitize(): ChartRegionPayload? =
    copy(
        bounds = bounds.sanitize() ?: return null,
        rows = rows.coerceIn(1, 999),
        columns = columns.coerceIn(1, 999),
    )

private fun ChartTrackerPayload.sanitize(): ChartTrackerPayload? {
    val sanitizedExtraCounterId =
        when (counterType) {
            ChartCounterType.MAIN -> null
            ChartCounterType.EXTRA -> extraCounterId?.takeIf { it > 0L } ?: return null
        }
    return copy(
        region = region.sanitize() ?: return null,
        extraCounterId = sanitizedExtraCounterId,
        highlightAlpha = highlightAlpha.sanitizeUnit() ?: return null,
    )
}

private fun NormalizedPatternPoint.sanitize(): NormalizedPatternPoint? =
    copy(
        x = x.sanitizeUnit() ?: return null,
        y = y.sanitizeUnit() ?: return null,
        pressure = pressure.sanitizeUnit() ?: return null,
    )

private fun NormalizedPatternBounds.sanitize(): NormalizedPatternBounds? {
    val sanitizedLeft = left.sanitizeUnit() ?: return null
    val sanitizedTop = top.sanitizeUnit() ?: return null
    val sanitizedRight = right.sanitizeUnit() ?: return null
    val sanitizedBottom = bottom.sanitizeUnit() ?: return null
    return NormalizedPatternBounds(
        left = minOf(sanitizedLeft, sanitizedRight),
        top = minOf(sanitizedTop, sanitizedBottom),
        right = maxOf(sanitizedLeft, sanitizedRight),
        bottom = maxOf(sanitizedTop, sanitizedBottom),
    )
}

private fun Float.sanitizeUnit(): Float? =
    takeIf(Float::isFinite)
        ?.coerceIn(0f, 1f)
        ?.roundToAnnotationPrecision()

private fun Float.sanitizeStrokeWidth(): Float? =
    takeIf(Float::isFinite)
        ?.coerceIn(PatternAnnotationLimits.MIN_STROKE_WIDTH, PatternAnnotationLimits.MAX_STROKE_WIDTH)
        ?.roundToAnnotationPrecision()

private fun Float.roundToAnnotationPrecision(): Float {
    val factor = 10f.pow(PatternAnnotationLimits.COORDINATE_DECIMAL_PLACES)
    return round(this * factor) / factor
}
