package com.finnvek.knittools.data.storage

import android.content.Context
import android.util.AtomicFile
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NotesDraftStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val context = mockk<Context>()
    private val id = UUID.randomUUID()
    private lateinit var cache: File

    @Before
    fun setUp() {
        cache = temporary.newFolder("cache")
        val privateFiles = temporary.newFolder("no-backup")
        every { context.cacheDir } returns cache
        every { context.noBackupFilesDir } returns privateFiles
        mockkConstructor(AtomicFile::class)
        for (directory in listOf(cache, privateFiles)) {
            val target = File(directory, "notes-draft-1-$id.json")
            every { constructedWith<AtomicFile>(EqMatcher(target)).startWrite() } answers { FileOutputStream(target) }
            every { constructedWith<AtomicFile>(EqMatcher(target)).finishWrite(any()) } answers {
                firstArg<FileOutputStream>().close()
            }
            every { constructedWith<AtomicFile>(EqMatcher(target)).readFully() } answers { target.readBytes() }
            every { constructedWith<AtomicFile>(EqMatcher(target)).delete() } answers {
                target.delete()
            }
        }
    }

    @After
    fun tearDown() {
        unmockkConstructor(AtomicFile::class)
    }

    @Test
    fun `unsaved draft survives cache eviction and storage recreation`() =
        runTest(dispatcher) {
            val draft = NotesDraft("long draft ".repeat(3_000), "Saved notes")
            NotesDraftStorage(context, dispatcher).write(1, id, draft)
            cache.listFiles().orEmpty().forEach { it.delete() }

            assertEquals(draft, NotesDraftStorage(context, dispatcher).read(1, id))
        }

    @Test
    fun `saved or discarded draft is removed from persistent storage`() =
        runTest(dispatcher) {
            val storage = NotesDraftStorage(context, dispatcher)
            storage.write(1, id, NotesDraft("Draft", "Base"))
            storage.delete(1, id)

            assertNull(NotesDraftStorage(context, dispatcher).read(1, id))
        }
}
