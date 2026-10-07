package com.finnvek.knittools.ui.theme

/**
 * Neulepintatilkkujen geometria suhteina tilkun kokoon, jotta sama tilkku näyttää samalta
 * 48 dp:n lankarivissä ja 72 dp:n projektikortissa. Mitat on johdettu hyväksytystä mockupista,
 * jossa neule- ja virkkaustilkun viitekoko oli 72 ja lankatilkun 48 yksikköä.
 */
@Suppress("MayBeConstant")
object FabricSwatchDimens {
    // --- Yhteiset ---
    val ProjectReferenceSize = 72f
    val YarnReferenceSize = 48f

    // Sisävarjo reunoille: tilkku näyttää painetulta pinnalta eikä tasaiselta kuvalta.
    val VignetteCenterX = 0.42f
    val VignetteCenterY = 0.36f
    val VignetteRadius = 0.78f
    val VignetteClearStop = 0.62f
    val VignetteAlpha = 0.28f

    // --- Sileäneule: jokainen silmukka on kaksi pulleaa jalkaa (⋁) ---
    val KnitStitchWidth = 17f
    val KnitRowRatio = 0.74f
    val KnitLegTopInset = 0.44f
    val KnitLegBottomInset = 0.05f
    val KnitLegTopLift = 0.02f
    val KnitLegBottomDrop = 1.1f
    val KnitLegExtraLength = 0.04f
    val KnitLegHalfThickness = 0.25f
    val KnitEdgeWidth = 0.035f

    // --- Virkkaus: vinot säieparit, rivit puolen silmukan verran limittäin ---
    val CrochetStitchWidth = 11f
    val CrochetRowRatio = 0.72f
    val CrochetStrandThickness = 0.4f
    val CrochetShortStrandRatio = 0.86f
    val CrochetEdgeWidth = 0.03f
    val CrochetStartX = 0.1f
    val CrochetStartDrop = 1.05f
    val CrochetLongReach = 0.7f
    val CrochetLongRise = 1.15f
    val CrochetShortStartX = 0.42f
    val CrochetShortStartRise = 0.45f
    val CrochetShortReach = 1f
    val CrochetShortRise = 1.05f

    // --- Lanka: kerratut säikeet vinottain ---
    val YarnPly = 9f
    val YarnRowStep = 0.92f
    val YarnColumnStep = 1.9f
    val YarnStrandHalfLength = 1.05f
    val YarnStrandHalfThickness = 0.42f
    val YarnStrandAngle = -35f
    val YarnEdgeWidth = 0.06f
}
