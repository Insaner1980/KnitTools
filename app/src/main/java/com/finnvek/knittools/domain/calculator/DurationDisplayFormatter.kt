package com.finnvek.knittools.domain.calculator

private const val MINUTES_PER_HOUR = 60

/** Kolme muotoa, joihin kesto voi asettua. Desimaalitunteja ei käytetä koskaan. */
enum class DurationShape {
    /** Alle tunnin kesto, esimerkiksi `48 min`. */
    MINUTES_ONLY,

    /** Tasatunnit ilman minuuttiosaa, esimerkiksi `2h`. */
    WHOLE_HOURS,

    /** Tunnit ja minuutit, esimerkiksi `3h 12min`. */
    HOURS_AND_MINUTES,
}

/**
 * Näyttömalli kestolle. Domain tuottaa slotit ja Compose-kerros kääntää ne
 * lokalisoiduiksi teksteiksi, jotta puhdas logiikka pysyy testattavana eikä
 * sisällä Android-resursseja.
 */
data class DurationDisplay(
    val shape: DurationShape,
    val hours: Int?,
    val minutes: Int?,
)

/** Yksi totuuden lähde kestojen näyttömuodolle. */
object DurationDisplayFormatter {
    /** Muodostaa näyttömallin kokonaisista minuuteista. Negatiivinen syöte tulkitaan nollaksi. */
    fun fromMinutes(totalMinutes: Int): DurationDisplay {
        val safeMinutes = totalMinutes.coerceAtLeast(0)
        val hours = safeMinutes / MINUTES_PER_HOUR
        val minutes = safeMinutes % MINUTES_PER_HOUR
        return when {
            hours == 0 -> DurationDisplay(DurationShape.MINUTES_ONLY, hours = null, minutes = minutes)
            minutes == 0 -> DurationDisplay(DurationShape.WHOLE_HOURS, hours = hours, minutes = null)
            else -> DurationDisplay(DurationShape.HOURS_AND_MINUTES, hours = hours, minutes = minutes)
        }
    }
}
