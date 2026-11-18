package com.finnvek.knittools.repository

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AuditPart16ShareTest {
    @Test
    fun grantedReaderSurvivesUnlinkButNewReaderFailsAndSiblingRemainsPrivate() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
            val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            val root = File(context.filesDir, "progress_photos/16076").apply { mkdirs() }
            val replies = LinkedBlockingQueue<Intent>()
            val replyAction = context.packageName + ".AUDIT16." + UUID.randomUUID()
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        replies.add(intent)
                    }
                }
            context.registerReceiver(receiver, IntentFilter(replyAction), Context.RECEIVER_EXPORTED)
            try {
                database.counterProjectDao().insert(CounterProjectEntity(id = 16076, name = "Synthetic share"))
                val source = File(root, "shared.jpg").apply { writeBytes(byteArrayOf(1, 3, 5, 7)) }
                val sibling = File(root, "private.jpg").apply { writeBytes(byteArrayOf(2, 4, 6, 8)) }
                val bytes = source.readBytes()
                database.progressPhotoDao().insert(
                    ProgressPhotoEntity(projectId = 16076, photoUri = source.toUri().toString(), rowNumber = 1),
                )
                val repository =
                    ProgressPhotoRepository(
                        database.progressPhotoDao(),
                        ProgressPhotoStorage(),
                        context,
                        Dispatchers.IO,
                        transactionRunner = RoomDatabaseTransactionRunner(database),
                    )
                val photo = repository.getPhotosForProject(16076).first().single()
                val uri = requireNotNull(AppFileStorage.shareUriForAppOwnedFile(context, source.toUri()))
                val other = AppFileStorage.fileProviderUri(context, sibling)

                fun launch(hold: Boolean) {
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            component =
                                ComponentName(testPackage, "com.finnvek.knittools.audit.AuditPart16ShareReceiver")
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri("Synthetic photo", uri)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra("hold", hold)
                            putExtra("other", other.toString())
                            putExtra("replyAction", replyAction)
                            putExtra("owner", context.packageName)
                        },
                    )
                }
                launch(true)
                val opened = requireNotNull(replies.poll(15, TimeUnit.SECONDS))
                assertEquals("opened", opened.getStringExtra("stage"))
                assertNotEquals(Process.myUid(), opened.getIntExtra("uid", -1))
                repository.deletePhoto(photo)
                assertFalse(source.exists())
                context.sendBroadcast(Intent(testPackage + ".CONTINUE").setPackage(testPackage))
                val read = requireNotNull(replies.poll(15, TimeUnit.SECONDS))
                assertEquals("read", read.getStringExtra("stage"))
                assertArrayEquals(bytes, read.getByteArrayExtra("bytes"))
                assertTrue(read.getBooleanExtra("otherDenied", false))
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                launch(false)
                val missing = requireNotNull(replies.poll(15, TimeUnit.SECONDS))
                assertEquals("failed", missing.getStringExtra("stage"))
                assertEquals("FileNotFoundException", missing.getStringExtra("failure"))
                assertTrue(database.progressPhotoDao().getAllPhotosOnce().isEmpty())
                assertArrayEquals(byteArrayOf(2, 4, 6, 8), sibling.readBytes())
            } finally {
                context.unregisterReceiver(receiver)
                database.close()
                root.deleteRecursively()
            }
        }
}
