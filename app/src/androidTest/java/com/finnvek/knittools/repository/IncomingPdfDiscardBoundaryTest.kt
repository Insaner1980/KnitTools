package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.remote.WebPdfDownloader
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.writeTestPdf
import com.finnvek.knittools.ui.navigation.IncomingPdfViewModel
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class IncomingPdfDiscardBoundaryTest(
    private val prompt: Int,
) {
    @Test
    fun authoredDiscardBoundary() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            val file = File(context.filesDir, "pattern_pdfs/0/discard-$prompt.pdf")
            val other = File(context.filesDir, "pattern_pdfs/0/discard-$prompt-other.pdf")
            file.parentFile!!.mkdirs()
            writeTestPdf(file, 100, 100, color = null)
            val originalBytes = file.readBytes().toList()
            other.writeBytes(file.readBytes())
            val uri = Uri.fromFile(file).toString()
            val store = ViewModelStore()
            val applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            var cleanupReads = 0
            var failCleanup = false
            val reading = CompletableDeferred<Unit>()
            val releaseRead = CompletableDeferred<Unit>()
            val dao =
                object : SavedPatternDao by database.savedPatternDao() {
                    override suspend fun getByLocalPdfUri(localPdfUri: String): SavedPatternEntity? {
                        cleanupReads++
                        reading.complete(Unit)
                        releaseRead.await()
                        if (failCleanup) throw IOException("Controlled reference-read failure")
                        return database.savedPatternDao().getByLocalPdfUri(localPdfUri)
                    }
                }
            val fixture = RoomCounterTestFixture(database, context)
            val saved =
                SavedPatternRepository(
                    dao,
                    context,
                    database.counterProjectDao(),
                    fixture.transactionRunner,
                    Dispatchers.IO,
                    database.projectDocumentDao(),
                )
            val handle = SavedStateHandle(mapOf("incoming_pdf_local_uri" to uri, "incoming_pdf_name" to "Pending"))
            try {
                val model =
                    withContext(Dispatchers.Main) {
                        IncomingPdfViewModel(
                            handle,
                            PatternDocumentStorage(Dispatchers.IO),
                            fixture.counterRepository(),
                            saved,
                            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager(),
                            WebPdfDownloader(context, Dispatchers.IO),
                            context,
                            Dispatchers.IO,
                            applicationScope,
                        ).also { store.put("pending", it) }
                    }
                assertNotNull(model.pending.value)
                withContext(Dispatchers.Main) {
                    model.discard()
                    if (prompt == 993) model.discard()
                    if (prompt == 994) store.clear()
                }
                assertNull(model.pending.value)
                assertNull(handle.get<String>("incoming_pdf_local_uri"))
                reading.await()
                if (prompt == 991) {
                    // Publish a real reference before the queued application-owned cleanup reads Room.
                    assertNotNull(fixture.savedPatternRepository.saveImportedPatternIfMissing(uri, "Published"))
                }
                if (prompt == 992) failCleanup = true
                releaseRead.complete(Unit)
                applicationScope.coroutineContext[Job]!!
                    .children
                    .toList()
                    .forEach { it.join() }
                assertEquals(1, cleanupReads)
                assertEquals(prompt == 991 || prompt == 992, file.exists())
                assertTrue(other.isFile)
                assertEquals(originalBytes, other.readBytes().toList())
                if (prompt == 991) {
                    assertEquals(
                        uri,
                        database
                            .savedPatternDao()
                            .getAllOnce()
                            .single()
                            .localPdfUri,
                    )
                } else {
                    assertTrue(database.savedPatternDao().getAllOnce().isEmpty())
                }
                assertFalse(model.busy.value)
            } finally {
                withContext(Dispatchers.Main) { store.clear() }
                applicationScope.cancel()
                database.close()
                file.delete()
                other.delete()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "prompt_{0}")
        fun prompts(): List<Array<Int>> = (991..994).map { arrayOf(it) }
    }
}
