package com.finnvek.knittools.data.local

private const val SQLITE_SAFE_BIND_PARAMETER_COUNT = 900

internal fun <T> Iterable<T>.distinctSqliteQueryChunks(): List<List<T>> =
    distinct().chunked(SQLITE_SAFE_BIND_PARAMETER_COUNT)
