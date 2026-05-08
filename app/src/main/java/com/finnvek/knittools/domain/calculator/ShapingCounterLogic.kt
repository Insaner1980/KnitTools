package com.finnvek.knittools.domain.calculator

/**
 * Laskentalogiikka Shaping-tyyppisille laskureille.
 * Seuraa silmukkamäärää muotoilurivien perusteella.
 */
object ShapingCounterLogic {
    /**
     * Laskee nykyisen silmukkamäärän muotoilun perusteella.
     * @param startingStitches aloitussilmukkamäärä
     * @param stitchChange muutos per muotoilurivi (negatiivinen = kavennukset)
     * @param shapeEveryN muotoilurivi joka N:s rivi
     * @param currentRow nykyinen rivi
     */
    fun calculateCurrentStitches(
        startingStitches: Int,
        stitchChange: Int,
        shapeEveryN: Int,
        currentRow: Int,
    ): Int {
        if (shapeEveryN <= 0 || currentRow <= 0) return startingStitches
        val shapingsDone = currentRow / shapeEveryN
        return (startingStitches + shapingsDone * stitchChange).coerceAtLeast(0)
    }

    /**
     * Palauttaa seuraavan muotoilurivin numeron.
     */
    fun nextShapingRow(
        shapeEveryN: Int,
        currentRow: Int,
    ): Int {
        if (shapeEveryN <= 0) return 0
        return ((currentRow / shapeEveryN) + 1) * shapeEveryN
    }

    /**
     * Onko nykyinen rivi muotoilurivi.
     */
    fun isShapingRow(
        shapeEveryN: Int,
        currentRow: Int,
    ): Boolean {
        if (shapeEveryN <= 0 || currentRow <= 0) return false
        return currentRow % shapeEveryN == 0
    }
}
