package com.finnvek.knittools.domain.model

import kotlinx.serialization.Serializable

sealed interface PatternAnnotationOwner {
    val documentKey: String

    data class Project(
        val projectId: Long,
        override val documentKey: String,
    ) : PatternAnnotationOwner

    data class SavedPattern(
        val savedPatternId: Long,
        override val documentKey: String,
    ) : PatternAnnotationOwner
}

object PatternAnnotationDocumentKey {
    fun savedPattern(savedPatternId: Long): String = "saved:$savedPatternId:v1"

    fun legacyProject(projectId: Long): String = "legacy-project:$projectId"
}

enum class PatternAnnotationKind {
    FREEHAND,
    HIGHLIGHTER,
    LINE,
    ARROW,
    RECTANGLE,
    ELLIPSE,
    TEXT_BOX,
    CALLOUT,
    CHART_REGION,
    CHART_TRACKER,
}

object PatternAnnotationLimits {
    const val MAX_FREEHAND_POINTS = 2_048
    const val MIN_STROKE_WIDTH = 0.5f
    const val MAX_STROKE_WIDTH = 64f
    const val MIN_TEXT_SIZE_SP = 8f
    const val MAX_TEXT_SIZE_SP = 96f
    const val COORDINATE_DECIMAL_PLACES = 5
}

sealed interface PatternAnnotationPayload

@Serializable
data class NormalizedPatternPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
)

@Serializable
data class NormalizedPatternBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

@Serializable
data class FreehandPayload(
    val points: List<NormalizedPatternPoint>,
    val argb: Int,
    val strokeWidth: Float,
    val pressureEnabled: Boolean = false,
) : PatternAnnotationPayload

@Serializable
data class ShapePayload(
    val start: NormalizedPatternPoint,
    val end: NormalizedPatternPoint,
    val strokeArgb: Int,
    val strokeWidth: Float,
    val fillArgb: Int? = null,
    val fillAlpha: Float = 0f,
) : PatternAnnotationPayload

@Serializable
data class TextBoxPayload(
    val bounds: NormalizedPatternBounds,
    val text: String,
    val textSizeSp: Float,
    val textArgb: Int,
    val backgroundArgb: Int? = null,
    val backgroundAlpha: Float = 0f,
) : PatternAnnotationPayload

enum class PatternCalloutSymbol {
    CHECK,
    CROSS,
    STAR,
    EXCLAMATION,
    NOTE,
}

@Serializable
data class CalloutPayload(
    val bounds: NormalizedPatternBounds,
    val symbol: PatternCalloutSymbol,
    val title: String,
    val description: String,
    val argb: Int,
) : PatternAnnotationPayload

enum class ChartRowDirection {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
}

enum class ChartColumnDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    ALTERNATING,
}

enum class ChartTrackingMode {
    ACTIVE_ROW,
    ACTIVE_COLUMN,
    CROSSHAIR,
    COMPLETED_CELLS,
    C2C_DIAGONAL,
}

enum class ChartCounterType {
    MAIN,
    EXTRA,
}

enum class ChartCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

@Serializable
data class ChartRegionPayload(
    val bounds: NormalizedPatternBounds,
    val name: String,
    val rows: Int,
    val columns: Int,
    val rowDirection: ChartRowDirection,
    val columnDirection: ChartColumnDirection,
) : PatternAnnotationPayload

@Serializable
data class ChartTrackerPayload(
    val region: ChartRegionPayload,
    val trackingMode: ChartTrackingMode,
    val counterType: ChartCounterType,
    val extraCounterId: Long? = null,
    val counterStartValue: Int,
    val gridStartIndex: Int,
    val wrapAtEnd: Boolean,
    val highlightArgb: Int,
    val highlightAlpha: Float,
    val c2cOrigin: ChartCorner = ChartCorner.BOTTOM_LEFT,
) : PatternAnnotationPayload

data class EncodedPatternAnnotationPayload(
    val payloadVersion: Int,
    val payloadJson: String,
)
