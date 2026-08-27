package com.finnvek.knittools.data.storage

import java.io.File
import java.io.InputStream

internal class PatternImageTooLargeException : Exception()

internal object PatternImageStagingFiles {
    fun sessionDirectory(
        filesDir: File,
        projectId: Long,
        sessionId: String,
    ): File = File(filesDir, "pattern_captures/$projectId/$sessionId")

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
