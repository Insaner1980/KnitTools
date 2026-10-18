package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Process
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PdfPageRenderer
import com.finnvek.knittools.repository.counterHistoryTestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class AuditPart16RestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val checkpoint get() = File(context.filesDir, "audit16-state.bin")

    @Test
    fun prepareBeforeProcessDeath() =
        runBlocking {
            assumeTrue(InstrumentationRegistry.getArguments().getString("audit16ProcessPhase") == "prepare")
            check(context.packageName.endsWith(".activepolicytest"))
            assertFalse("Run only in an empty isolated fixture", checkpoint.exists())
            val database = database()
            try {
                val saved = Bundle().apply { putInt("pid", Process.myPid()) }
                for (id in 16001L..16003L) {
                    database.counterProjectDao().insert(CounterProjectEntity(id = id, name = "Synthetic import $id"))
                    val handle = SavedStateHandle()
                    val vm = onMain { viewModel(database, handle) }
                    if (id == 16002L) {
                        onMain { vm.authorizeCameraCapture(id) }
                        val target = requireNotNull(vm.createCameraCaptureTarget(id))
                        saved.putString("capturePath", target.first.path)
                        saved.putString("captureUri", target.second.toString())
                        assertEquals(0L, target.first.length())
                    } else {
                        val sources =
                            (1..2).map { index ->
                                File(context.cacheDir, "audit16-$id-$index.png").also(::image)
                            }
                        val request = onMain { vm.authorizeGalleryPicker(id) }
                        onMain { vm.onGalleryPickerResult(request, sources.map(File::toUri)) }
                        awaitReady(vm)
                        onMain {
                            vm.moveEarlier(
                                vm.uiState.value.selection.pages
                                    .last()
                                    .id,
                            )
                        }
                        assertEquals(
                            sources.last().toUri().toString(),
                            vm.uiState.value.selection.pages
                                .first()
                                .sourceUri,
                        )
                        sources.forEach { assertTrue(it.delete()) }
                        assertTrue(
                            vm.uiState.value.selection.pages
                                .all { File(it.stagedPath).isFile },
                        )
                    }
                    saved.putBundle(
                        id.toString(),
                        onMain {
                            bundleOf(*handle.keys().map { it to handle.get<Any?>(it) }.toTypedArray())
                        },
                    )
                }
                val parcel = Parcel.obtain()
                try {
                    parcel.writeBundle(saved)
                    checkpoint.writeBytes(parcel.marshall())
                } finally {
                    parcel.recycle()
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun resumeAfterProcessDeath() =
        runBlocking {
            assumeTrue(InstrumentationRegistry.getArguments().getString("audit16ProcessPhase") == "resume")
            check(context.packageName.endsWith(".activepolicytest"))
            val parcel = Parcel.obtain()
            val saved =
                try {
                    val bytes = checkpoint.readBytes()
                    parcel.unmarshall(bytes, 0, bytes.size)
                    parcel.setDataPosition(0)
                    requireNotNull(parcel.readBundle(javaClass.classLoader))
                } finally {
                    parcel.recycle()
                }
            assertNotEquals("Harness must kill the original process", saved.getInt("pid"), Process.myPid())
            val database = database()
            try {
                for (id in 16001L..16003L) {
                    val bundle = requireNotNull(saved.getBundle(id.toString()))

                    @Suppress("DEPRECATION")
                    val handle = SavedStateHandle(bundle.keySet().associateWith { bundle.get(it) })
                    val vm = onMain { viewModel(database, handle) }
                    if (id == 16002L) {
                        val file = File(requireNotNull(saved.getString("capturePath")))
                        image(file)
                        onMain { vm.acceptCameraCapture(id, Uri.parse(saved.getString("captureUri")), file) }
                        awaitReady(vm)
                        assertEquals(
                            file.path,
                            vm.uiState.value.selection.pages
                                .single()
                                .stagedPath,
                        )
                    } else {
                        assertEquals(2, vm.uiState.value.selection.pages.size)
                        assertTrue(
                            vm.uiState.value.selection.pages
                                .first()
                                .sourceUri
                                .endsWith("-2.png"),
                        )
                        assertTrue(
                            vm.uiState.value.selection.pages
                                .all { !File(Uri.parse(it.sourceUri).path!!).exists() },
                        )
                    }
                    if (id == 16003L) {
                        assertTrue(
                            File(
                                vm.uiState.value.selection.pages
                                    .first()
                                    .stagedPath,
                            ).delete(),
                        )
                    }
                    onMain { vm.createPatternPdf(replaceExisting = false) }
                    val result =
                        withTimeout(20_000) {
                            vm.uiState.first {
                                it.closeReady ||
                                    it.phase == PatternImageImportPhase.ERROR
                            }
                        }
                    val documents = database.projectDocumentDao().getForProject(id)
                    if (id == 16003L) {
                        assertEquals(PatternImageImportPhase.ERROR, result.phase)
                        assertTrue(documents.isEmpty())
                        assertFalse(result.closeReady)
                        assertTrue(File(context.filesDir, "pattern_pdfs/$id").listFiles().isNullOrEmpty())
                    } else {
                        assertEquals(PatternImageImportPhase.SUCCESS, result.phase)
                        assertEquals(1, documents.size)
                        PdfPageRenderer(context, documents.single().localPdfUri.toUri()).use {
                            assertEquals(if (id == 16001L) 2 else 1, it.pageCount)
                        }
                        assertTrue(File(context.filesDir, "pattern_captures/$id").listFiles().isNullOrEmpty())
                    }
                }
            } finally {
                database.close()
            }
        }

    private fun database() = Room.databaseBuilder(context, KnitToolsDatabase::class.java, "audit16-restore.db").build()

    private fun viewModel(
        database: KnitToolsDatabase,
        handle: SavedStateHandle,
    ) = PatternImageImportViewModel(
        PatternDocumentStorage(Dispatchers.IO),
        counterHistoryTestRepository(database, context),
        context,
        Dispatchers.IO,
        handle,
    )

    private suspend fun awaitReady(vm: PatternImageImportViewModel) {
        withTimeout(15_000) { vm.uiState.first { it.phase == PatternImageImportPhase.READY } }
    }

    private fun image(file: File) {
        file.parentFile!!.mkdirs()
        val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
