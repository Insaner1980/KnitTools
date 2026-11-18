package com.finnvek.knittools.ui.screens.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.R
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.PatternFileReferenceCoordinator
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.interceptTestDatabase
import com.finnvek.knittools.repository.isIsolatedRoomTestPackage
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BackupPostCommitCancellationTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun finishingActivityAfterCommitPreservesReplacementAndReopenedBackupIsUsable() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(app.isIsolatedRoomTestPackage)
        val root = File(app.cacheDir, "br-g04-${UUID.randomUUID()}").apply { mkdirs() }
        val context = BackupTestContext(app, root)
        val gate = CommitGate()
        val databaseFile = File(root, "live.db")
        val db =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, databaseFile.path)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .openHelperFactory(gate.factory)
                .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository =
            BackupRepository(
                context,
                db,
                PatternFileReferenceCoordinator(),
                YarnCardRepository(
                    db.yarnCardDao(),
                    db.counterProjectDao(),
                    context,
                    RoomDatabaseTransactionRunner(db),
                    Dispatchers.IO,
                ),
                Dispatchers.IO,
                scope,
                LocalBackupIo(),
            )
        val onboarding =
            OnboardingStateRepository(
                PreferenceDataStoreFactory.create(scope = scope) { File(root, "onboarding.preferences_pb") },
            )
        val archive = File(root, "source.zip")
        val reopenedArchive = File(root, "reopened.zip")
        var scenario: ActivityScenario<ComponentActivity>? = null
        var firstScope: Job? = null
        try {
            val original = File(context.filesDir, "pattern_pdfs/1/source.pdf").apply { parentFile?.mkdirs() }
            val pdf = PdfDocument()
            try {
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
                pdf.finishPage(page)
                original.outputStream().use(pdf::writeTo)
            } finally {
                pdf.close()
            }
            val expectedBytes = original.readBytes()
            runBlocking(Dispatchers.IO) {
                val sourceId =
                    db.counterProjectDao().insert(
                        CounterProjectEntity(name = "Restored project", count = 12),
                    )
                db.projectDocumentDao().insert(document(sourceId, original))
                repository.export(Uri.fromFile(archive))
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE counter_projects SET name = 'Old destination', count = 99",
                )
                db.counterProjectDao().insert(CounterProjectEntity(name = "Destination only"))
            }
            assertEquals(2L, projectCount(databaseFile))
            lateinit var vm: BackupViewModel
            scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario.onActivity { activity ->
                vm = attach(activity, repository, onboarding, archive)
                firstScope = vm.viewModelScope.coroutineContext[Job]
                vm.prepare(Uri.fromFile(archive))
            }
            rule.waitUntil(15_000) { vm.state.value.phase == BackupPhase.PREVIEW }
            gate.armed.set(true)
            scenario.onActivity { vm.restore(checkNotNull(vm.state.value.previewSelectionId)) }
            assertTrue("Restore did not reach committed boundary", gate.committed.await(20, TimeUnit.SECONDS))
            val committed = snapshot(databaseFile)
            assertEquals("Restored project", committed.name)
            assertEquals(12, committed.count)
            assertEquals(1L, projectCount(databaseFile))
            assertNotEquals(1L, committed.projectId)
            assertEquals(committed.projectId, committed.documentProjectId)
            assertEquals(BackupPhase.RESTORING, vm.state.value.phase)
            rule.onNodeWithText(app.getString(R.string.backup_restoring)).performScrollTo().assertIsDisplayed()

            // Finishing the owner cancels viewModelScope through Android's real lifecycle.
            scenario.close()
            scenario = null
            assertTrue(requireNotNull(firstScope).isCancelled)
            assertNull(vm.state.value.error)
            gate.release.countDown()
            runBlocking { withTimeout(15_000) { requireNotNull(firstScope).join() } }
            assertEquals(BackupPhase.RESTORING, vm.state.value.phase)
            assertEquals(committed, snapshot(databaseFile))
            val restoredFile = requireNotNull(AppFileStorage.resolveAppOwnedFile(context, Uri.parse(committed.uri)))
            assertArrayEquals(expectedBytes, restoredFile.readBytes())
            assertFalse("Obsolete original must be removed only after commit", original.exists())
            assertTrue(File(context.noBackupFilesDir, "manual-backup").listFiles().orEmpty().isEmpty())
            assertDatabaseIntegrity(databaseFile)

            lateinit var reopened: BackupViewModel
            scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario.onActivity { reopened = attach(it, repository, onboarding, reopenedArchive) }
            assertEquals(BackupUiState(), reopened.state.value)
            rule.onNodeWithText(app.getString(R.string.backup_restoring)).assertDoesNotExist()
            rule.onNodeWithText(app.getString(R.string.backup_error_restore)).assertDoesNotExist()
            rule.onNodeWithText(app.getString(R.string.backup_restored)).assertDoesNotExist()
            rule.onNodeWithText(app.getString(R.string.backup_continue)).assertDoesNotExist()
            rule.onNodeWithText(app.getString(R.string.backup_restore)).assertIsEnabled()
            rule.onNodeWithText(app.getString(R.string.backup_export)).performClick()
            rule.waitUntil(15_000) { reopened.state.value.phase == BackupPhase.EXPORTED }
            rule.onNodeWithText(app.getString(R.string.backup_exported)).performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(app.getString(R.string.backup_restore)).performScrollTo().performClick()
            rule.waitUntil(15_000) { reopened.state.value.phase == BackupPhase.PREVIEW }
            assertEquals(
                1,
                reopened.state.value.preview
                    ?.projects,
            )
            assertEquals(
                1,
                reopened.state.value.preview
                    ?.files,
            )
            rule
                .onNodeWithText(
                    app.getString(R.string.backup_preview_projects, 1),
                ).performScrollTo()
                .assertIsDisplayed()
            rule.onNodeWithText(app.getString(R.string.backup_preview_files, 1)).performScrollTo().assertIsDisplayed()
            assertFalse(reopened.state.value.busy)
            assertNull(reopened.state.value.error)
            assertEquals(committed, snapshot(databaseFile))
            assertArrayEquals(expectedBytes, restoredFile.readBytes())
        } finally {
            gate.release.countDown()
            scenario?.close()
            runBlocking {
                withTimeout(15_000) { firstScope?.join() }
                scope.coroutineContext[Job]?.cancelAndJoin()
            }
            db.close()
            check(root.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            check(root.deleteRecursively())
        }
    }

    private fun attach(
        activity: ComponentActivity,
        repository: BackupRepository,
        onboarding: OnboardingStateRepository,
        archive: File,
    ): BackupViewModel {
        val vm =
            ViewModelProvider(
                activity,
                viewModelFactory {
                    initializer { BackupViewModel(repository, onboarding) }
                },
            )[BackupViewModel::class.java]
        activity.setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            KnitToolsTheme {
                BackupContent(
                    state,
                    activity::finish,
                    { vm.export(Uri.fromFile(archive)) },
                    { vm.prepare(Uri.fromFile(archive)) },
                    vm::restore,
                    vm::cancel,
                    activity::finish,
                )
            }
        }
        return vm
    }

    private fun document(
        projectId: Long,
        file: File,
    ) = ProjectDocumentEntity(
        projectId = projectId,
        savedPatternId = null,
        documentKey = "local:synthetic",
        label = "Synthetic PDF",
        localPdfUri = Uri.fromFile(file).toString(),
        sortOrder = 0,
        isPrimary = true,
        currentPage = 0,
        rowMapping = null,
        readingLineEnabled = true,
        readingLineYFraction = 0.4f,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = 0.5f,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun projectCount(file: File): Long =
        readDatabase(file) { sql ->
            sql.rawQuery("SELECT COUNT(*) FROM counter_projects", null).use {
                it.moveToFirst()
                it.getLong(0)
            }
        }

    private fun snapshot(file: File): Snapshot =
        readDatabase(file) { sql ->
            sql
                .rawQuery(
                    "SELECT p.id, p.name, p.count, d.projectId, d.localPdfUri FROM counter_projects p " +
                        "JOIN project_documents d ON d.projectId = p.id WHERE d.isPrimary = 1",
                    null,
                ).use {
                    assertEquals(1, it.count)
                    check(it.moveToFirst())
                    Snapshot(it.getLong(0), it.getString(1), it.getInt(2), it.getLong(3), it.getString(4))
                }
        }

    private fun assertDatabaseIntegrity(file: File) =
        readDatabase(file) { sql ->
            sql.rawQuery("PRAGMA foreign_key_check", null).use { assertEquals(0, it.count) }
            sql.rawQuery("PRAGMA quick_check", null).use {
                check(it.moveToFirst())
                assertEquals("ok", it.getString(0))
            }
        }

    private fun <T> readDatabase(
        file: File,
        block: (SQLiteDatabase) -> T,
    ): T = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use(block)

    private data class Snapshot(
        val projectId: Long,
        val name: String,
        val count: Int,
        val documentProjectId: Long,
        val uri: String,
    )

    private class CommitGate {
        val armed = AtomicBoolean(false)
        val committed = CountDownLatch(1)
        val release = CountDownLatch(1)
        private val changed = AtomicBoolean(false)
        val factory =
            interceptTestDatabase { database ->
                object : SupportSQLiteDatabase by database {
                    override fun execSQL(sql: String) {
                        database.execSQL(sql)
                        if (armed.get() && sql.startsWith("DELETE FROM ")) changed.set(true)
                    }

                    override fun endTransaction() {
                        database.endTransaction()
                        if (changed.get() && !database.inTransaction() && armed.compareAndSet(true, false)) {
                            committed.countDown()
                            check(release.await(30, TimeUnit.SECONDS)) { "Post-commit gate was not released" }
                        }
                    }
                }
            }
    }

    private class LocalBackupIo : BackupProviderIo {
        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            check()
            File(requireNotNull(source.path)).copyTo(target, overwrite = true)
        }

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) {
            check()
            source.copyTo(File(requireNotNull(destination.path)), overwrite = true)
        }
    }
}
