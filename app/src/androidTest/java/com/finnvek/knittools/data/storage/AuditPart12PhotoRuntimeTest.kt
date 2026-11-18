package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.repository.ProgressPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.math.abs

class AuditPart12PhotoRuntimeTest {
    @Test
    fun photoNoteBoundarySurvivesRealSaveUpdateAndDatabaseReopen() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val projectId = System.nanoTime()
            val name = "audit12-note-${UUID.randomUUID()}.db"
            var db = photoNoteTestDatabase(context, name)
            val source = File(context.cacheDir, "$name.png")
            try {
                writeTestBitmap(source)
                db.counterProjectDao().insert(CounterProjectEntity(id = projectId, name = "Synthetic note"))
                val repository =
                    ProgressPhotoRepository(
                        db.progressPhotoDao(),
                        ProgressPhotoStorage(),
                        context,
                        Dispatchers.IO,
                        com.finnvek.knittools.data.local
                            .RoomDatabaseTransactionRunner(db),
                    )
                repository.savePhoto(projectId, source.toUri(), 42, "a".repeat(99) + "\uD83D\uDE00")
                val original = db.progressPhotoDao().getAllPhotosOnce().single()
                assertEquals("a".repeat(99), original.note)
                val values =
                    listOf(
                        null to null,
                        "" to null,
                        "a".repeat(99) to "a".repeat(99),
                        "a".repeat(100) to "a".repeat(100),
                        "a".repeat(101) to "a".repeat(100),
                    ) + photoNoteEmojiCases()
                for ((value, expected) in values) {
                    repository.updatePhotoNote(original.id, value)
                    assertEquals(original.copy(note = expected), db.progressPhotoDao().getAllPhotosOnce().single())
                }
                db.close()
                db = photoNoteTestDatabase(context, name)
                assertEquals(original, db.progressPhotoDao().getAllPhotosOnce().single())
                assertTrue(File(requireNotNull(original.photoUri.toUri().path)).exists())
            } finally {
                db.close()
                context.deleteDatabase(name)
                source.delete()
                File(context.filesDir, "progress_photos/$projectId").deleteRecursively()
            }
        }

    @Test
    fun progressPhotoPreservesAllExifOrientationsAndVisibleCorners() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest"))
        val directory = File(context.cacheDir, "audit12-photo-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val source = File(directory, "source.jpg")
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            try {
                for (y in 0 until 40) {
                    for (x in 0 until 80) {
                        bitmap.setPixel(
                            x,
                            y,
                            listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)[
                                (y / 20) * 2 +
                                    x / 40,
                            ],
                        )
                    }
                }
                source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            } finally {
                bitmap.recycle()
            }
            val corners =
                listOf(
                    listOf(0, 1, 2, 3),
                    listOf(1, 0, 3, 2),
                    listOf(3, 2, 1, 0),
                    listOf(2, 3, 0, 1),
                    listOf(0, 2, 1, 3),
                    listOf(2, 0, 3, 1),
                    listOf(3, 1, 2, 0),
                    listOf(1, 3, 0, 2),
                )
            // Start with the common camera rotation so a missing orientation decode fails explicitly.
            for (orientation in listOf(6, 1, 2, 3, 4, 5, 7, 8)) {
                ExifInterface(source.path).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val output = File(directory, "$orientation.jpg")
                assertTrue(ProgressPhotoStorage().compressAndSave(context, source.toUri(), output))
                val decoded = requireNotNull(BitmapFactory.decodeFile(output.path))
                try {
                    assertEquals("orientation=$orientation width", if (orientation >= 5) 40 else 80, decoded.width)
                    assertEquals("orientation=$orientation height", if (orientation >= 5) 80 else 40, decoded.height)
                    val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
                    corners[orientation - 1].forEachIndexed { index, color ->
                        val actual =
                            decoded.getPixel(
                                decoded.width * (
                                    if (index % 2 ==
                                        0
                                    ) {
                                        1
                                    } else {
                                        3
                                    }
                                ) / 4,
                                decoded.height * (if (index < 2) 1 else 3) / 4,
                            )
                        for (shift in listOf(0, 8, 16)) {
                            assertTrue(
                                "orientation=$orientation corner=$index",
                                abs((actual shr shift and 255) - (colors[color] shr shift and 255)) < 30,
                            )
                        }
                    }
                } finally {
                    decoded.recycle()
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun progressPhotoLongEdgeBoundaryProducesBoundedReadableJpeg() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest"))
        val directory = File(context.cacheDir, "audit12-edge-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            for (edge in listOf(1919, 1920, 1921)) {
                val source = File(directory, "$edge.png")
                val bitmap = Bitmap.createBitmap(edge, 20, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.RED)
                    source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally {
                    bitmap.recycle()
                }
                val output = File(directory, "$edge.jpg")
                assertTrue(ProgressPhotoStorage().compressAndSave(context, source.toUri(), output))
                val decoded = requireNotNull(BitmapFactory.decodeFile(output.path))
                try {
                    assertTrue(decoded.width in 1..1920)
                    assertTrue(decoded.height in 1..1920)
                    if (edge <= 1920) assertEquals(edge, decoded.width)
                } finally {
                    decoded.recycle()
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
