package com.finnvek.knittools.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

private const val INITIAL_RETRY_DELAY_MS = 250L
private const val MAX_RETRY_DELAY_MS = 5_000L
private const val MAX_RETRY_EXPONENT = 5

internal fun <T> Flow<T>.retryOnRepositoryReadFailure(): Flow<T> =
    retryWhen { cause, attempt ->
        if (cause is CancellationException || cause !is Exception) {
            return@retryWhen false
        }

        // Havaintovirta luodaan uudelleen viiveellä, jotta yksittäinen lukuvirhe ei pysäytä UI-päivityksiä.
        delay(repositoryReadRetryDelayMillis(attempt))
        true
    }

internal fun repositoryReadRetryDelayMillis(attempt: Long): Long {
    val exponent = attempt.coerceAtMost(MAX_RETRY_EXPONENT.toLong()).toInt()
    return (INITIAL_RETRY_DELAY_MS * (1L shl exponent)).coerceAtMost(MAX_RETRY_DELAY_MS)
}
