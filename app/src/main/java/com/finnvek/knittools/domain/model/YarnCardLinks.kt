package com.finnvek.knittools.domain.model

fun parseYarnCardIds(value: String): List<Long> =
    value
        .split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .distinct()

fun formatYarnCardIds(ids: Iterable<Long>): String = ids.distinct().joinToString(",")
