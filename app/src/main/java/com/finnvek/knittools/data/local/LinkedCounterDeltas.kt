package com.finnvek.knittools.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object LinkedCounterDeltas {
    fun encode(deltas: Map<Long, Int>): String = Json.encodeToString(deltas)

    fun decode(value: String): Map<Long, Int> =
        Json.decodeFromString<Map<Long, Int>>(value).also { deltas ->
            require(deltas.all { (id, delta) -> id > 0L && delta != Int.MIN_VALUE })
        }
}
