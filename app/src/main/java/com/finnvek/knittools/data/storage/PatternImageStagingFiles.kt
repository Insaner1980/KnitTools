package com.finnvek.knittools.data.storage

import android.annotation.SuppressLint
import com.finnvek.knittools.ui.screens.pattern.PatternImageImportLimits
import java.io.File
import java.io.IOException
import java.io.InputStream

internal class PatternImageTooLargeException : Exception()

internal class PatternImageInsufficientSpaceException : IOException("Insufficient space to stage image")

internal object PatternImageStagingFiles {
    fun sessionDirectory(
        filesDir: File,
        projectId: Long,
        sessionId: String,
    ): File = File(filesDir, "pattern_captures/$projectId/$sessionId")

    @Synchronized
    fun copyBounded(
        input: InputStream,
        target: File,
        maxBytes: Long,
    ): Long {
        target.parentFile?.mkdirs()
        var copiedBytes = 0L
        var completed = false
        try {
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copiedBytes += read
                    if (copiedBytes > maxBytes) throw PatternImageTooLargeException()
                    // Preserve space that is free now; allocatable bytes also include reclaimable caches.
                    @SuppressLint("UsableSpace")
                    val usableSpace = target.usableSpace
                    if (usableSpace - read < PatternImageImportLimits.MIN_FREE_SPACE_RESERVE_BYTES) {
                        throw PatternImageInsufficientSpaceException()
                    }
                    output.write(buffer, 0, read)
                }
            }
            completed = true
            return copiedBytes
        } finally {
            if (!completed && target.exists() && !target.delete()) target.deleteOnExit()
        }
    }
}
