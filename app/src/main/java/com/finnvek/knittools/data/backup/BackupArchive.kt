package com.finnvek.knittools.data.backup

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupArchive {
    fun write(
        directory: File,
        archive: File,
        manifest: BackupManifest,
        check: () -> Unit = {},
    ) {
        FileOutputStream(archive).use { file ->
            ZipOutputStream(file.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST))
                zip.write(BackupFormat.json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                manifest.entries.forEach { entry ->
                    check()
                    zip.putNextEntry(ZipEntry(entry.path))
                    File(directory, entry.path).inputStream().use { input ->
                        BackupFormat.copy(input, zip, entry.size, check)
                    }
                    zip.closeEntry()
                }
                zip.finish()
                zip.flush()
                file.fd.sync()
            }
        }
    }

    fun extract(
        archive: File,
        directory: File,
        check: () -> Unit = {},
    ): BackupManifest {
        BackupZipStructure.verify(archive, check)
        val manifest =
            ZipFile(archive).use { zip ->
                val seen = mutableSetOf<String>()
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    check()
                    val entry = entries.nextElement()
                    BackupFormat.requireValid(seen.size < BackupFormat.MAX_ENTRIES)
                    BackupFormat.requireValid(seen.add(entry.name) && !entry.isDirectory)
                    BackupFormat.requireValid(
                        entry.name == BackupFormat.MANIFEST || BackupFormat.allowedPath(entry.name),
                    )
                }
                val header = zip.getEntry(BackupFormat.MANIFEST) ?: throw BackupException(BackupError.INVALID)
                BackupFormat.requireValid(header.size in 1..BackupFormat.MAX_MANIFEST)
                val bytes = java.io.ByteArrayOutputStream()
                zip.getInputStream(header).use { BackupFormat.copy(it, bytes, BackupFormat.MAX_MANIFEST, check) }
                val headerText = bytes.toString(Charsets.UTF_8.name())
                BackupFormat.requireJsonDepth(headerText, 4)
                val manifest = BackupFormat.json.decodeFromString<BackupManifest>(headerText)
                validateManifest(manifest)
                BackupFormat.requireValid(seen == manifest.entries.map { it.path }.toSet() + BackupFormat.MANIFEST)
                val total = manifest.entries.sumOf { it.size }
                BackupFormat.space(directory, total)
                manifest.entries.forEach { item ->
                    check()
                    val entry = zip.getEntry(item.path)
                    BackupFormat.requireValid(entry.size == item.size)
                    val target = File(directory, item.path)
                    BackupFormat.requireValid(target.canonicalPath.startsWith(directory.canonicalPath + File.separator))
                    check(target.parentFile?.mkdirs() == true || target.parentFile?.isDirectory == true)
                    FileOutputStream(target).use { output ->
                        zip.getInputStream(entry).use { input ->
                            val actual = BackupFormat.copy(input, output, item.size, check)
                            BackupFormat.requireValid(actual == item.size)
                        }
                    }
                    BackupFormat.requireValid(BackupFormat.digest(target, check) == item.sha256)
                }
                manifest
            }
        verifyLocalHeaders(archive, manifest, check)
        return manifest
    }

    private fun verifyLocalHeaders(
        archive: File,
        manifest: BackupManifest,
        check: () -> Unit,
    ) {
        // Tarkistetaan myös paikalliset otsakkeet: keskusluettelo voi piilottaa ylimääräisiä tai toistuvia jäseniä.
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            val expected = manifest.entries.associateBy { it.path }
            val names = mutableSetOf<String>()
            var entry = zip.nextEntry
            while (entry != null) {
                check()
                val name = entry.name
                BackupFormat.requireValid(names.add(name))
                val max =
                    if (name == BackupFormat.MANIFEST) {
                        BackupFormat.MAX_MANIFEST
                    } else {
                        expected[name]?.size ?: throw BackupException(BackupError.CORRUPT)
                    }
                BackupFormat.copy(zip, discardOutput, max, check)
                entry = zip.nextEntry
            }
            BackupFormat.requireValid(names == expected.keys + BackupFormat.MANIFEST)
        }
    }

    private fun validateManifest(manifest: BackupManifest) {
        BackupFormat.requireValid(manifest.format == "KnitTools", BackupError.INVALID)
        BackupFormat.requireValid(
            manifest.formatVersion == 1 && manifest.dataVersion == 1 && manifest.schemaVersion == 25,
            BackupError.UNSUPPORTED,
        )
        BackupFormat.requireValid(
            manifest.createdAt > 0 && manifest.versionCode >= 0 && manifest.versionName.length in 1..80,
        )
        BackupFormat.requireValid(manifest.entries.size in BackupFormat.tables.size until BackupFormat.MAX_ENTRIES)
        BackupFormat.requireValid(
            manifest.entries
                .map { it.path }
                .toSet()
                .size == manifest.entries.size,
        )
        BackupFormat.requireValid(manifest.entries.map { it.path }.containsAll(BackupFormat.tablePaths))
        var total = 0L
        manifest.entries.forEach { entry ->
            BackupFormat.requireValid(BackupFormat.allowedPath(entry.path))
            BackupFormat.requireValid(entry.size in 0..BackupFormat.limit(entry.path))
            BackupFormat.requireValid(BackupFormat.hashPattern.matches(entry.sha256))
            total += entry.size
            BackupFormat.requireValid(total <= BackupFormat.MAX_TOTAL)
        }
    }

    private val discardOutput =
        object : java.io.OutputStream() {
            override fun write(value: Int) = Unit

            override fun write(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ) = Unit
        }
}
