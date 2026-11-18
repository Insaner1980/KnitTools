package com.finnvek.knittools.domain.model

/**
 * Lyhentää tekstin enintään [maxLength] merkkiin katkaisematta surrogaattiparia: jos raja osuu
 * parin keskelle, yksinäinen korkea surrogaatti pudotetaan pois.
 */
internal fun String.takeWithoutSplittingSurrogatePair(maxLength: Int): String {
    val truncated = take(maxLength)
    return if (truncated.lastOrNull()?.isHighSurrogate() == true) truncated.dropLast(1) else truncated
}
