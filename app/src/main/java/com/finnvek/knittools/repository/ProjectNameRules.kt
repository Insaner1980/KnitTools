package com.finnvek.knittools.repository

import java.util.Locale

internal object ProjectNameRules {
    fun normalize(name: String): String? = name.trim().takeIf { it.isNotEmpty() }

    fun uniqueName(
        requestedName: String,
        existingNames: Collection<String>,
    ): String? {
        val baseName = normalize(requestedName) ?: return null
        val existingKeys = existingNames.map { it.normalizedKey() }.toSet()
        if (baseName.normalizedKey() !in existingKeys) return baseName

        var suffix = 2
        while (true) {
            val candidate = "$baseName ($suffix)"
            if (candidate.normalizedKey() !in existingKeys) return candidate
            suffix += 1
        }
    }

    private fun String.normalizedKey(): String = trim().lowercase(Locale.ROOT)
}
