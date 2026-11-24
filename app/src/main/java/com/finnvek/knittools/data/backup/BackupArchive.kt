package com.finnvek.knittools.data.backup

import com.finnvek.knittools.data.local.KNITTOOLS_DATABASE_VERSION
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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
        val header = BackupFormat.json.encodeToString(manifest).encodeToByteArray()
        validateManifest(manifest, header.size.toLong())
        FileOutputStream(archive).use { file ->
            ZipOutputStream(file.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST))
                zip.write(header)
                zip.closeEntry()
                manifest.entries.forEach { entry ->
                    check()
                    zip.putNextEntry(ZipEntry(entry.path))
                    File(directory, entry.path).inputStream().use { input ->
                        BackupFormat.requireValid(BackupFormat.copy(input, zip, entry.size, check) == entry.size)
                    }
                    zip.closeEntry()
                }
                zip.finish()
                zip.flush()
                file.fd.sync()
            }
        }
        BackupBudget().requireArchiveSize(archive.length(), BackupError.WRITE)
    }

    fun extract(
        archive: File,
        directory: File,
        check: () -> Unit = {},
    ): BackupManifest {
        BackupBudget().requireArchiveSize(archive.length(), BackupError.INVALID)
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
                val bytes = ByteArrayOutputStream()
                zip.getInputStream(header).use { BackupFormat.copy(it, bytes, BackupFormat.MAX_MANIFEST, check) }
                BackupFormat.requireValid(bytes.size().toLong() == header.size)
                val headerText = bytes.toString(Charsets.UTF_8.name())
                BackupFormat.requireJsonDepth(headerText, 4)
                val manifest = decodeManifest(headerText)
                validateManifest(manifest, header.size)
                BackupFormat.requireValid(seen == manifest.entries.map { it.path }.toSet() + BackupFormat.MANIFEST)
                val total =
                    manifest.entries.fold(0L) { current, entry ->
                        BackupFormat.addWithinLimit(current, entry.size, BackupLimits.MAX_EXTRACTED_BYTES)
                    }
                BackupFormat.space(directory, total)
                manifest.entries.forEach { item ->
                    check()
                    val entry = zip.getEntry(item.path)
                    BackupFormat.requireValid(entry.size == item.size)
                    val target = File(directory, item.path)
                    BackupFormat.requireValid(target.canonicalPath.startsWith(directory.canonicalPath + File.separator))
                    BackupFormat.requireValid(
                        target.parentFile?.mkdirs() == true || target.parentFile?.isDirectory == true,
                        BackupError.WRITE,
                    )
                    FileOutputStream(target).use { output ->
                        zip.getInputStream(entry).use { input ->
                            val actual = BackupFormat.copy(input, output, item.size, check)
                            BackupFormat.requireValid(actual == item.size)
                        }
                    }
                    BackupFormat.requireValid(BackupFormat.digest(target, check) == item.sha256)
                }
                verifyLocalHeaders(archive, manifest, zip, check)
                manifest
            }
        return manifest
    }

    private fun decodeManifest(text: String): BackupManifest {
        val header = BackupFormat.json.parseToJsonElement(text).jsonObject
        // The default numeric decoder also accepts quoted numbers.
        header.requirePrimitiveTypes(
            strings = listOf("format", "versionName"),
            numbers = listOf("formatVersion", "dataVersion", "schemaVersion", "createdAt", "versionCode"),
        )
        header.getValue("entries").jsonArray.forEach { entry ->
            entry.jsonObject.requirePrimitiveTypes(strings = listOf("path", "sha256"), numbers = listOf("size"))
        }
        return BackupFormat.json.decodeFromJsonElement<BackupManifest>(header)
    }

    private fun JsonObject.requirePrimitiveTypes(
        strings: List<String>,
        numbers: List<String>,
    ) {
        strings.forEach { name -> BackupFormat.requireValid((get(name) as? JsonPrimitive)?.isString == true) }
        numbers.forEach { name ->
            val value = get(name) as? JsonPrimitive
            BackupFormat.requireValid(value != null && !value.isString && value.longOrNull != null)
        }
    }

    private fun verifyLocalHeaders(
        archive: File,
        manifest: BackupManifest,
        centralDirectory: ZipFile,
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
                val central = centralDirectory.getEntry(name) ?: throw BackupException(BackupError.CORRUPT)
                BackupFormat.requireValid(
                    entry.size == central.size &&
                        entry.compressedSize == central.compressedSize &&
                        entry.crc == central.crc &&
                        entry.method == central.method,
                )
                entry = zip.nextEntry
            }
            BackupFormat.requireValid(names == expected.keys + BackupFormat.MANIFEST)
        }
    }

    internal fun validateManifest(
        manifest: BackupManifest,
        manifestBytes: Long =
            BackupFormat.json
                .encodeToString(manifest)
                .encodeToByteArray()
                .size
                .toLong(),
    ) {
        BackupFormat.requireValid(manifestBytes in 1..BackupFormat.MAX_MANIFEST)
        BackupFormat.requireValid(manifest.format == "KnitTools", BackupError.INVALID)
        BackupFormat.requireValid(
            manifest.formatVersion == 1 &&
                manifest.dataVersion in 1..2 &&
                manifest.schemaVersion in 25..KNITTOOLS_DATABASE_VERSION,
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
        val budget = BackupBudget()
        budget.addManifest(manifestBytes)
        manifest.entries.forEach { entry ->
            BackupFormat.requireValid(BackupFormat.allowedPath(entry.path))
            BackupFormat.requireValid(entry.size in 0..BackupFormat.limit(entry.path))
            BackupFormat.requireValid(BackupFormat.hashPattern.matches(entry.sha256))
            BackupFormat.requireValid(
                entry.path in BackupFormat.tablePaths || entry.path == "files/${entry.sha256}.bin",
            )
            budget.addArchiveEntry(entry.path, entry.size)
        }
    }

    private val discardOutput =
        object : OutputStream() {
            override fun write(value: Int) = Unit

            override fun write(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ) = Unit
        }
}
