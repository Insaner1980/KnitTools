package com.finnvek.knittools.ui.screens.backup

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.R
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.PatternFileReferenceCoordinator
import com.finnvek.knittools.repository.RoomCounterTestFixture
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class BackupCancellationRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun cancelledPartialExportShowsGuidanceAndDeliberateRetryCanSucceed() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        val root = File(app.cacheDir, "rw03b-backup-${UUID.randomUUID()}").apply { mkdirs() }
        val context =
            object : ContextWrapper(app) {
                override fun getApplicationContext(): Context = this

                override fun getFilesDir() = File(root, "files").apply { mkdirs() }

                override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
            }
        val db = Room.databaseBuilder(context, KnitToolsDatabase::class.java, File(root, "test.db").path).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val target = File(root, "destination.zip")
        var writes = 0
        var backs = 0
        val io =
            object : BackupProviderIo {
                override suspend fun read(
                    source: Uri,
                    target: File,
                    maxBytes: Long,
                    check: () -> Unit,
                ) = error("This test must never restore")

                override suspend fun write(
                    source: File,
                    destination: Uri,
                    maxBytes: Long,
                    check: () -> Unit,
                ) {
                    writes += 1
                    if (writes == 1) {
                        target.writeBytes(source.inputStream().use { it.readNBytes(16) })
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled.complete(Unit)
                        }
                    } else {
                        source.copyTo(target, overwrite = true)
                    }
                }
            }
        val repository =
            BackupRepository(
                context,
                db,
                PatternFileReferenceCoordinator(),
                RoomCounterTestFixture(db, context).yarnCardRepository,
                Dispatchers.IO,
                scope,
                io,
            )
        val onboardingStore =
            PreferenceDataStoreFactory.create(
                scope = scope,
            ) { File(root, "onboarding.preferences_pb") }
        val vm = BackupViewModel(repository, OnboardingStateRepository(onboardingStore))
        try {
            rule.setContent {
                val state by vm.state.collectAsState()
                KnitToolsTheme {
                    BackupContent(state, { backs += 1 }, { vm.export(Uri.fromFile(target)) }, {}, {}, vm::cancel, {})
                }
            }
            rule.runOnUiThread { vm.export(Uri.fromFile(target)) }
            runBlocking { withTimeout(10_000) { started.await() } }
            rule.onNodeWithContentDescription(app.getString(R.string.back)).performClick()
            rule.runOnUiThread { vm.cancel() }
            rule.waitUntil(5_000) { vm.state.value.phase == BackupPhase.IDLE }
            rule.onNodeWithText(app.getString(R.string.backup_error_write)).assertIsDisplayed()
            assertEquals(BackupError.WRITE, vm.state.value.error)
            assertEquals(0, backs)
            runBlocking { withTimeout(5_000) { cancelled.await() } }
            assertEquals(16L, target.length())
            rule.runOnUiThread { vm.cancel() }
            assertEquals(BackupError.WRITE, vm.state.value.error)
            rule.runOnUiThread { vm.export(Uri.fromFile(target)) }
            rule.waitUntil(10_000) { vm.state.value.phase == BackupPhase.EXPORTED }
            rule.onNodeWithText(app.getString(R.string.backup_error_write)).assertDoesNotExist()
            assertTrue(target.length() > 16)
            assertEquals(2, writes)
        } finally {
            runBlocking {
                vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
                scope.coroutineContext[Job]?.cancelAndJoin()
            }
            db.close()
            check(root.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            root.deleteRecursively()
        }
    }
}
