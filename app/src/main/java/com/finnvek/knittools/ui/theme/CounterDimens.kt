package com.finnvek.knittools.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CounterDimens {
    val StepRingMinimumSize = 48.dp
    val StepRingMaximumSize = 96.dp
    val StepRingTextPadding = 8.dp
    val StepRingButtonGap = 8.dp
    val StepFeedbackWidth = 72.dp
    const val STEP_FEEDBACK_DURATION_MILLIS = 900L

    // Tekstillinen kolmiulotteinen nappi (uusi projekti): pilleri, jonka päässä on nappikuva.
    val LabeledImageButtonElevation = 3.dp
    val LabeledImageButtonTextStart = 20.dp
    val LabeledImageButtonTextEnd = 6.dp

    val ScreenHorizontalPadding = 24.dp
    val ContentBottomPadding = 24.dp
    val WorkspaceSectionSpacing = 20.dp
    val WorkspaceGroupSpacing = 16.dp
    val HeroBottomBreathingSpace = 24.dp
    val CounterProjectRevealGap = 48.dp

    @Suppress("MayBeConstant")
    val CounterHeroTopWeight = 0.32f

    @Suppress("MayBeConstant")
    val CounterHeroBottomWeight = 0.68f

    val HeroButtonSpacing = 64.dp
    val HeroButtonCompactSpacing = 8.dp

    // Pinottu laskuri saa venyä tarvitsemaansa korkeammaksi enintään tämän verran; isolla näytöllä
    // sisältöruudut näkyvät heti laskurin alla eikä niiden yllä ole puolen ruudun tyhjää.
    val HeroMaxExtraHeight = 64.dp

    // Vierekkäinen laskuri valitaan, kun pinottu ei mahdu korkeuteen ja leveyttä on tämän verran.
    val ShortHeroMinWidth = 552.dp
    val ShortHeroSpacing = 16.dp
    val ShortHeroUndoSize = 80.dp
    val ShortHeroUndoGap = 8.dp

    // Iso laskuri tabletilla: luku ja napit luetaan telineestä kauempaa.
    val LargeHeroMinWidth = 600.dp
    val LargeHeroMinHeight = 840.dp

    @Suppress("MayBeConstant")
    val LargeHeroScale = 1.4f
    val CounterMainNumberFontSize = 115.sp
    val CounterMainNumberMinimumFontSize = 48.sp

    @Suppress("MayBeConstant")
    val CounterMainNumberFitIterations = 8

    val CounterTargetHelperSpacing = 8.dp
    val CounterTargetHelperFontSize = 19.sp
    val CounterRepeatMinWidth = 232.dp
    val CounterRepeatMaxWidth = 256.dp
    val CounterRepeatHeight = 48.dp
    val CounterRepeatCornerRadius = 24.dp
    val CounterRepeatHorizontalPadding = 16.dp
    val CounterRepeatLabelFontSize = 15.sp
    val CounterRepeatValueFontSize = 21.sp
    val CounterRepeatButtonTouchSize = 48.dp
    val CounterRepeatButtonVisualSize = 32.dp
    val CounterRepeatIconSize = 16.dp
    val CounterRepeatToRowSpacing = 32.dp
    val CounterPrimaryTouchSize = 144.dp
    val CounterPrimaryVisualSize = 125.dp
    val CounterMinusTouchSize = 144.dp
    val CounterMinusVisualSize = 123.dp
    val CounterMinusOpticalOffsetY = 1.dp
    val CounterUndoVisualSize = 92.dp
    val CounterUndoTouchSize = CounterUndoVisualSize
    val CounterUndoVerticalSpacing = 16.dp
    val CounterControlsMaxWidth = 360.dp
    val CounterControlsToStitchTrackerSpacing = 72.dp
    val CounterControlsToStitchTrackerCompactSpacing = 8.dp
    val ProjectCardGridSpacing = 12.dp
    val ProjectCardGridMaxWidth = 360.dp

    // Tästä leveydestä alkaen viisi ruutua ladotaan 3 + 2 koko sisältöleveydelle.
    val ProjectCardThreeColumnMinWidth = 480.dp
    val ProjectCardPadding = 12.dp
    val ProjectCardIconSize = 56.dp
    val ProjectCardCompactIconSize = 32.dp
    val ProjectCardIconTitleSpacing = 12.dp
    val ExtraCounterCardMinHeight = 88.dp
    val ExtraCounterCardCornerRadius = 16.dp
    val ExtraCounterCardHorizontalPadding = 22.dp
    val ExtraCounterCardVerticalPadding = 18.dp
    val ExtraCounterContentSpacing = 8.dp
    val ExtraCounterStepperTouchSize = 56.dp
    val ExtraCounterStepperVisualSize = 42.dp
    val ExtraCounterStepperIconSize = 20.dp
    val ExtraCounterOverflowTouchSize = 48.dp
    val ExtraCounterOverflowIconSize = 22.dp
    val RepeatSectionProgressHeight = 8.dp
    val WorkspaceSectionActionIconSize = 18.dp
    val WorkspaceSectionActionIconSpacing = 4.dp

    @Suppress("MayBeConstant")
    val StitchTrackerContainerAlpha = 0.42f

    val StitchTrackerMinHeight = 68.dp
    val StitchTrackerCornerRadius = 16.dp
    val StitchTrackerHorizontalPadding = 16.dp
    val StitchTrackerVerticalPadding = 6.dp
    val StitchTrackerContentSpacing = 12.dp
    val TopBarTitleEndPadding = 8.dp
    val ProjectActionsHorizontalPadding = 22.dp
    val ProjectActionsRowVerticalPadding = 12.dp
    val ProjectActionsSectionTopPadding = 14.dp
    val ProjectActionsSectionBottomPadding = 8.dp
    val ProjectActionsDividerVerticalPadding = 4.dp
    val WorkSessionStartPadding = 16.dp
    val WorkSessionEndPadding = 8.dp
    val WorkSessionVerticalPadding = 8.dp
    val WorkSessionStackBreakpoint = 360.dp
    val WorkSessionActionMinHeight = 48.dp
    val WorkSessionStackedSpacing = 4.dp
    val WorkSessionInlineSpacing = 12.dp
}
