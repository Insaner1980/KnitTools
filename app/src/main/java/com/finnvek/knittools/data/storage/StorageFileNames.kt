package com.finnvek.knittools.data.storage

import java.io.File

internal object StorageFileNames {
    fun uniqueTimestampedFile(
        directory: File,
        prefix: String,
        extension: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ): File {
        var suffix = 0
        while (true) {
            val suffixText = if (suffix == 0) "" else "_$suffix"
            val candidate = File(directory, "$prefix$timestampMillis$suffixText$extension")
            if (!candidate.exists()) return candidate
            suffix += 1
        }
    }
}
