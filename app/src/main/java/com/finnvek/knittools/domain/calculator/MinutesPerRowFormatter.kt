package com.finnvek.knittools.domain.calculator

import kotlin.math.roundToInt

/**
 * Näyttömalli tahtiluvulle "minuuttia per rivi". Suoraan käyttökelpoinen luku
 * neulojalle, joka suunnittelee istuntoa: toisin kuin rivit tunnissa, se ei
 * romahda alle ykkösen hitailla pitsiriveillä.
 */
sealed interface MinutesPerRowDisplay {
    /** Rivejä ei ole, joten jakolaskua ei tehdä lainkaan. */
    data object Unavailable : MinutesPerRowDisplay

    /** Tulos pyöristyy nollaan, joten näytetään alarajamerkintä. */
    data object UnderOneMinute : MinutesPerRowDisplay

    /** Lähimpään kokonaiseen minuuttiin pyöristetty tahti. */
    data class Minutes(
        val minutes: Int,
    ) : MinutesPerRowDisplay
}

/** Yksi totuuden lähde tahtiluvun laskennalle. */
object MinutesPerRowFormatter {
    fun fromTotals(
        totalMinutes: Int,
        totalRows: Int,
    ): MinutesPerRowDisplay = fromSeconds(totalMinutes.coerceAtLeast(0).toLong() * 60L, totalRows)

    fun fromSeconds(
        totalSeconds: Long,
        totalRows: Int,
    ): MinutesPerRowDisplay {
        if (totalRows <= 0) return MinutesPerRowDisplay.Unavailable
        val rounded = (totalSeconds.coerceAtLeast(0).toDouble() / 60.0 / totalRows).roundToInt()
        return if (rounded <= 0) {
            MinutesPerRowDisplay.UnderOneMinute
        } else {
            MinutesPerRowDisplay.Minutes(rounded)
        }
    }
}
