package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.domain.model.PatternAvailability
import com.finnvek.knittools.domain.model.SavedPatternSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebPatternRepositoryTest {
    private lateinit var dao: SavedPatternDao
    private lateinit var projectDao: CounterProjectDao
    private lateinit var context: Context

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        projectDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { dao.getAll() } returns kotlinx.coroutines.flow.flowOf(emptyList())
    }

    @Test
    fun `create persists one normalized WEB_LINK record`() =
        runTest {
            coEvery { dao.getByCanonicalUrl("https://example.com/Pattern") } returns null
            coEvery { dao.getAllOnce() } returns emptyList()
            coEvery { dao.insert(any()) } returns 41L
            val inserted = slot<SavedPatternEntity>()
            val repository = repository()

            val result =
                repository.createWebPattern(
                    WebPatternInput(
                        title = "  Cozy Cardigan  ",
                        designer = "  Jane Doe  ",
                        url = "HTTPS://EXAMPLE.COM:443/Pattern",
                    ),
                )

            assertEquals(WebPatternMutationResult.Created(41L), result)
            coVerify(exactly = 1) { dao.insert(capture(inserted)) }
            with(inserted.captured) {
                assertEquals(SavedPatternSource.WebLink.persistedValue, source)
                assertEquals("Cozy Cardigan", name)
                assertEquals("Jane Doe", designerName)
                assertNull(thumbnailUrl)
                assertNull(difficulty)
                assertNull(gaugeStitches)
                assertNull(gaugeRows)
                assertNull(needleSize)
                assertNull(yarnWeight)
                assertNull(yardage)
                assertEquals(PatternAvailability.Unknown.persistedValue, availability)
                assertEquals("HTTPS://EXAMPLE.COM:443/Pattern", originalUrl)
                assertEquals("https://example.com/Pattern", canonicalUrl)
                assertNull(ravelryPatternId)
                assertNull(localPdfUri)
                assertEquals(false, isAvailableOffline)
                assertNull(lastSyncedAt)
            }
        }

    @Test
    fun `create returns duplicate without overwriting existing record`() =
        runTest {
            coEvery { dao.getByCanonicalUrl("https://example.com/pattern") } returns webEntity(id = 7L)
            val repository = repository()

            val result =
                repository.createWebPattern(
                    WebPatternInput("New title", "", "https://example.com/pattern"),
                )

            assertEquals(WebPatternMutationResult.Duplicate(7L), result)
            coVerify(exactly = 0) { dao.insert(any()) }
            coVerify(exactly = 0) { dao.update(any()) }
        }

    @Test
    fun `create detects equivalent legacy original URL across source types`() =
        runTest {
            val legacy =
                SavedPatternEntity(
                    id = 8L,
                    source = SavedPatternSource.Other.persistedValue,
                    name = "Legacy",
                    designerName = "",
                    originalUrl = "HTTPS://EXAMPLE.COM:443/pattern",
                    canonicalUrl = "",
                )
            coEvery { dao.getByCanonicalUrl("https://example.com/pattern") } returns null
            coEvery { dao.getAllOnce() } returns listOf(legacy)
            val repository = repository()

            val result =
                repository.createWebPattern(
                    WebPatternInput("New title", "", "https://example.com/pattern"),
                )

            assertEquals(WebPatternMutationResult.Duplicate(8L), result)
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `create validates input and preserves Ravelry ownership`() =
        runTest {
            val repository = repository()

            assertEquals(
                WebPatternMutationResult.InvalidTitle,
                repository.createWebPattern(WebPatternInput(" ", "", "https://example.com/pattern")),
            )
            assertEquals(
                WebPatternMutationResult.InvalidDesigner,
                repository.createWebPattern(WebPatternInput("Title", "Bad\nName", "https://example.com/pattern")),
            )
            assertEquals(
                WebPatternMutationResult.InvalidUrl,
                repository.createWebPattern(WebPatternInput("Title", "", "file:///pattern")),
            )
            assertEquals(
                WebPatternMutationResult.RavelryOwnedUrl,
                repository.createWebPattern(
                    WebPatternInput("Title", "", "https://www.ravelry.com/patterns/library/cozy-hat"),
                ),
            )
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    // CPD-OFF: Paivitystestien skenaariokohtainen DAO-fixture pidetaan testin yhteydessa.
    @Test
    fun `update excludes self upgrades compatible legacy and propagates linked title atomically`() =
        runTest {
            val current =
                SavedPatternEntity(
                    id = 7L,
                    source = SavedPatternSource.Other.persistedValue,
                    name = "Old title",
                    designerName = "Old designer",
                    thumbnailUrl = "https://example.com/thumb.jpg",
                    originalUrl = "https://example.com/old",
                    canonicalUrl = "https://example.com/old",
                    savedAt = 10L,
                    updatedAt = 50L,
                )
            coEvery { dao.getById(7L) } returns current
            coEvery { dao.getByCanonicalUrlExcludingId("https://example.com/new", 7L) } returns null
            coEvery { dao.getAllOnce() } returns listOf(current)
            val updated = slot<SavedPatternEntity>()
            val repository = repository()

            val result =
                repository.updateWebPattern(
                    patternId = 7L,
                    expectedUpdatedAt = 50L,
                    input = WebPatternInput("New title", "New designer", "https://example.com/new"),
                )

            assertEquals(WebPatternMutationResult.Updated(7L), result)
            coVerify(exactly = 1) { dao.update(capture(updated)) }
            assertEquals(SavedPatternSource.WebLink.persistedValue, updated.captured.source)
            assertEquals("New title", updated.captured.name)
            assertEquals("https://example.com/thumb.jpg", updated.captured.thumbnailUrl)
            assertTrue(updated.captured.updatedAt > current.updatedAt)
            coVerify(exactly = 1) { projectDao.updateLinkedPatternName(7L, "New title", updated.captured.updatedAt) }
        }

    // CPD-ON

    @Test
    fun `update accepts the same canonical URL when the only match is itself`() =
        runTest {
            val current = webEntity(id = 7L, updatedAt = 50L)
            coEvery { dao.getById(7L) } returns current
            coEvery { dao.getByCanonicalUrlExcludingId("https://example.com/pattern", 7L) } returns null
            coEvery { dao.getAllOnce() } returns listOf(current)
            val updated = slot<SavedPatternEntity>()
            val repository = repository()

            val result =
                repository.updateWebPattern(
                    patternId = 7L,
                    expectedUpdatedAt = 50L,
                    input = WebPatternInput("Renamed pattern", "", "HTTPS://EXAMPLE.COM:443/pattern"),
                )

            assertEquals(WebPatternMutationResult.Updated(7L), result)
            coVerify(exactly = 1) {
                dao.getByCanonicalUrlExcludingId("https://example.com/pattern", 7L)
            }
            coVerify(exactly = 1) { dao.update(capture(updated)) }
            assertEquals("https://example.com/pattern", updated.captured.canonicalUrl)
            assertEquals("HTTPS://EXAMPLE.COM:443/pattern", updated.captured.originalUrl)
        }

    @Test
    fun `update rejects an incompatible OTHER record`() =
        runTest {
            val incompatibleOther =
                webEntity(id = 7L, updatedAt = 50L).copy(
                    source = SavedPatternSource.Other.persistedValue,
                    localPdfUri = "content://pattern.pdf",
                    isAvailableOffline = true,
                )
            coEvery { dao.getById(7L) } returns incompatibleOther
            val repository = repository()

            val result =
                repository.updateWebPattern(
                    patternId = 7L,
                    expectedUpdatedAt = 50L,
                    input = WebPatternInput("Title", "", "https://example.com/new"),
                )

            assertEquals(WebPatternMutationResult.NotEditableAsWebPattern, result)
            coVerify(exactly = 0) { dao.getByCanonicalUrlExcludingId(any(), any()) }
            coVerify(exactly = 0) { dao.update(any()) }
        }

    @Test
    fun `update returns stale missing noneditable and other-record duplicate outcomes`() =
        runTest {
            val web = webEntity(id = 7L, updatedAt = 50L)
            val other = webEntity(id = 8L)
            val local =
                web.copy(
                    id = 9L,
                    source = SavedPatternSource.LocalFile.persistedValue,
                    localPdfUri = "content://pattern.pdf",
                )
            val repository = repository()

            coEvery { dao.getById(6L) } returns null
            assertEquals(
                WebPatternMutationResult.PatternMissing,
                repository.updateWebPattern(6L, 1L, WebPatternInput("Title", "", "https://example.com/new")),
            )

            coEvery { dao.getById(7L) } returns web
            assertEquals(
                WebPatternMutationResult.StaleAction,
                repository.updateWebPattern(7L, 49L, WebPatternInput("Title", "", "https://example.com/new")),
            )

            coEvery { dao.getById(9L) } returns local
            assertEquals(
                WebPatternMutationResult.NotEditableAsWebPattern,
                repository.updateWebPattern(9L, 50L, WebPatternInput("Title", "", "https://example.com/new")),
            )

            coEvery { dao.getById(7L) } returns web
            coEvery { dao.getByCanonicalUrlExcludingId("https://example.com/new", 7L) } returns other
            assertEquals(
                WebPatternMutationResult.Duplicate(8L),
                repository.updateWebPattern(7L, 50L, WebPatternInput("Title", "", "https://example.com/new")),
            )
        }

    @Test
    fun `repository preserves cancellation and types persistence failure`() =
        runTest {
            val repository = repository()
            coEvery { dao.getByCanonicalUrl(any()) } throws CancellationException("cancelled")

            try {
                repository.createWebPattern(WebPatternInput("Title", "", "https://example.com/pattern"))
                fail("CancellationException was not rethrown")
            } catch (_: CancellationException) {
                // Odotettu: peruutusta ei muuneta tavalliseksi virhetulokseksi.
            }

            coEvery { dao.getByCanonicalUrl(any()) } throws IllegalStateException("database unavailable")
            assertEquals(
                WebPatternMutationResult.PersistenceFailure,
                repository.createWebPattern(WebPatternInput("Title", "", "https://example.com/pattern")),
            )
        }

    @Test
    fun `web delete clears linked metadata only after finding a compatible record`() =
        runTest {
            coEvery { dao.getById(7L) } returns webEntity(id = 7L)
            val repository = repository()

            val result = repository.deleteWebPattern(7L)

            assertEquals(SavedPatternDeleteResult.Deleted, result)
            coVerify(exactly = 1) { projectDao.clearLinkedPatternIds(listOf(7L), any()) }
            coVerify(exactly = 1) { dao.deleteById(7L) }
        }

    @Test
    fun `web delete distinguishes a missing record without mutating projects`() =
        runTest {
            coEvery { dao.getById(7L) } returns null
            val repository = repository()

            val result = repository.deleteWebPattern(7L)

            assertEquals(SavedPatternDeleteResult.PatternMissing, result)
            coVerify(exactly = 0) { projectDao.clearLinkedPatternIds(any(), any()) }
            coVerify(exactly = 0) { dao.deleteById(any()) }
        }

    @Test
    fun `web delete rejects a local file record without deleting it`() =
        runTest {
            coEvery { dao.getById(7L) } returns
                webEntity(id = 7L).copy(
                    source = SavedPatternSource.LocalFile.persistedValue,
                    originalUrl = "content://pattern.pdf",
                    canonicalUrl = "",
                    localPdfUri = "content://pattern.pdf",
                    isAvailableOffline = true,
                )
            val repository = repository()

            val result = repository.deleteWebPattern(7L)

            assertEquals(SavedPatternDeleteResult.NotWebPattern, result)
            coVerify(exactly = 0) { projectDao.clearLinkedPatternIds(any(), any()) }
            coVerify(exactly = 0) { dao.deleteById(any()) }
        }

    @Test
    fun `web delete types persistence failure`() =
        runTest {
            coEvery { dao.getById(7L) } returns webEntity(id = 7L)
            coEvery { dao.deleteById(7L) } throws IllegalStateException("database unavailable")
            val repository = repository()

            assertEquals(SavedPatternDeleteResult.PersistenceFailure, repository.deleteWebPattern(7L))
        }

    @Test
    fun `web delete preserves cancellation`() =
        runTest {
            coEvery { dao.getById(7L) } returns webEntity(id = 7L)
            coEvery { dao.deleteById(7L) } throws CancellationException("cancelled")
            val repository = repository()

            try {
                repository.deleteWebPattern(7L)
                fail("CancellationException was not rethrown")
            } catch (_: CancellationException) {
                // Odotettu: peruutusta ei muuneta poistovirheeksi.
            }
        }

    private fun repository(): SavedPatternRepository =
        SavedPatternRepository(
            dao = dao,
            context = context,
            counterProjectDao = projectDao,
            transactionRunner = ImmediateDatabaseTransactionRunner,
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    private fun webEntity(
        id: Long,
        updatedAt: Long = 50L,
    ): SavedPatternEntity =
        SavedPatternEntity(
            id = id,
            source = SavedPatternSource.WebLink.persistedValue,
            name = "Pattern",
            designerName = "",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
            savedAt = 10L,
            updatedAt = updatedAt,
        )
}
