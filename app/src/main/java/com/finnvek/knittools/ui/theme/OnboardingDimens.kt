package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp

object OnboardingDimens {
    val JourneyProgressHeight = 6.dp
    val JourneyProgressGap = 5.dp
    val JourneyToolbarHeight = 48.dp
    val JourneyDemoUndoSize = 48.dp
    val JourneyDemoUndoTouchSize = 56.dp
    val DemoPatternRowGap = 20.dp
    val JourneyDemoCounterSize = 80.dp
    val JourneySectionGap = 24.dp
    val JourneyDemoTouchSize = 88.dp
    val JourneyDemoButtonSize = 72.dp
    val JourneyDemoNumberMinWidth = 96.dp
    val CraftTileImageSize = 88.dp
    val CraftTileMinHeight = 104.dp
    val SelectionCheckSize = 24.dp
    val SelectionBorderWidth = 1.5.dp
    const val STEP_TRANSITION_MILLIS = 320
    const val STEP_SLIDE_FRACTION = 0.15f
    const val CRAFT_ADVANCE_DELAY_MILLIS = 250L
    const val DEMO_NUMBER_MILLIS = 220
    const val DEMO_LINE_MILLIS = 360

    // Työkalupakki: laattojen kuvakkeet ja kertaluontoiset elävät yksityiskohdat.
    val ToolboxGlyphPadSize = 52.dp
    val ToolboxGlyphSize = 36.dp
    val ToolboxLineStroke = 2.5.dp
    val ToolboxMoreOutline = DashedOutlineStyle(width = 1.dp, dash = 5.dp, gap = 4.dp, corner = 12.dp)
    const val TOOLBOX_COLUMNS = 2
    const val TOOLBOX_COMPACT_COLUMNS = 1
    const val TOOLBOX_DETAIL_MILLIS = 900

    // Laskurilaatan kuvake painuu kahdesti kuin plussaa napautettaessa.
    const val TOOLBOX_PRESS_DEPTH = 0.14f
    const val TOOLBOX_PRESS_COUNT = 2
    const val TOOLBOX_LINE_FROM = 0.3f
    const val TOOLBOX_LINE_TO = 0.72f
    const val TOOLBOX_CHART_BASE = 0.15f
    val JourneyStackedBreakpoint = 320.dp
    val ExampleCardPadding = 16.dp
    val SketchWidth = 132.dp
    val SketchHeight = 176.dp
    val SwatchWidth = 150.dp
    val FixedStitchScale = 1.dp
    val SwatchRowHeight = FixedStitchScale * FabricSwatchDimens.KnitStitchWidth * FabricSwatchDimens.KnitRowRatio
    val PlusVisualSize = 96.dp
    val PlusTouchSize = 104.dp
    val CaptionMinHeight = 48.dp
    val CompletedProjectImageSize = 160.dp
    val ReadingLineWidth = 2.dp
    val PaperInset = 10.dp
    val TextBarHeight = 4.dp
    val TextRowHeight = 16.dp
    val ReadingBandHeight = 14.dp
    val ChartCellWidth = 13.dp
    val ChartCellHeight = 10.dp
    val ChartLineWidth = 1.dp
    const val START_COUNT = 23
    const val START_ROWS = 4
    const val MAX_ROWS = 7
    const val GROWTH_DURATION_MILLIS = 250
    const val READING_BAND_ALPHA = 0.16f
    const val TEXT_ROWS = 8
    const val CHART_COLUMNS = 8
    const val CHART_ROWS = 2
    const val CHART_INK_SHADE = 0.15f
    val TextBarFractions = listOf(0.8f, 0.95f, 0.65f, 1f, 0.6f, 0.88f, 0.97f, 0.72f)
    val TipMaxWidth = 280.dp
    val TipPadding = 16.dp
    val TipNotchSize = 8.dp
    val TipMarkerSize = 14.dp
    val TipMarkerStroke = 2.dp
    val TipSlideDistance = 6.dp
    val TipScreenInset = 8.dp
    const val TIP_ENTER_DURATION_MILLIS = 200
    const val TIP_ENTRY_DELAY_MILLIS = 2_000L
    const val TIP_UNDO_HIDE_MILLIS = 6_000L
    const val TIP_MARKER_GAP_START = -45f
    const val TIP_MARKER_SWEEP = 300f
    const val TIP_CLASP_START_X = 0.6f
    const val TIP_CLASP_END_Y = 0.35f
}
