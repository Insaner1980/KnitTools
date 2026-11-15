package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.repository.RoomCounterTestFixture
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class LibraryPatternAvailabilityRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun missingOwnedPdfLeavesSavedPatternMetadataAndCanStillBeDeleted() =
        runBlocking {
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            val missing = File(context.filesDir, "pattern_pdfs/0/review05-${UUID.randomUUID()}.pdf")
            assertFalse(missing.exists())
            val database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            try {
                val id =
                    database.savedPatternDao().insert(
                        SavedPatternEntity(
                            source = SavedPatternSource.Ravelry.persistedValue,
                            ravelryPatternId = 123,
                            name = "Unavailable PDF",
                            designerName = "",
                            originalUrl = "https://www.ravelry.com/patterns/library/fixture",
                            localPdfUri = missing.toUri().toString(),
                            isAvailableOffline = true,
                        ),
                    )
                val repository = RoomCounterTestFixture(database, context).savedPatternRepository

                val pattern = requireNotNull(repository.observeById(id).first())
                assertFalse(repository.isLocalPdfAvailable(pattern))
                assertEquals(123, repository.getById(id)?.ravelryPatternId)
                assertEquals(pattern.originalUrl, repository.getById(id)?.originalUrl)
                assertEquals(pattern.localPdfUri, repository.getById(id)?.localPdfUri)
                assertFalse(missing.exists())

                repository.deleteById(id)
                assertNull(repository.observeById(id).first())
            } finally {
                database.close()
            }
        }

    @Test
    fun missingFileAndUnavailableProviderReportRenderFailureWithoutEditableContent() {
        check(context.packageName == "com.finnvek.knittools.activepolicytest")
        val missing = File(context.cacheDir, "review05-${UUID.randomUUID()}.pdf")
        assertFalse(missing.exists())
        val sources =
            listOf(
                missing.toUri().toString(),
                "content://${context.packageName}.review05-unavailable/document.pdf",
            )
        val source = mutableStateOf(sources.first())
        var observedSource: String? = null
        var rendered: PatternRenderState? = null
        composeRule.setContent {
            KnitToolsTheme {
                val state = rememberPatternRenderState(source.value, 0) {}
                SideEffect {
                    observedSource = source.value
                    rendered = state
                }
            }
        }

        sources.forEach { uri ->
            composeRule.runOnIdle { source.value = uri }
            composeRule.waitUntil(15_000L) { observedSource == uri && rendered?.rendererError != null }
            composeRule.runOnIdle {
                val state = requireNotNull(rendered)
                assertNotNull(state.rendererError)
                assertTrue(state.rendererError.orEmpty().isNotBlank())
                assertNull(state.renderer)
                assertNull(state.renderedBitmap)
                assertFalse(state.canAnnotate)
            }
        }
    }
}
