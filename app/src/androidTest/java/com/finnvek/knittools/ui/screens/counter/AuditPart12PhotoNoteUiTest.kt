package com.finnvek.knittools.ui.screens.counter

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.storage.photoNoteEmojiCases
import com.finnvek.knittools.data.storage.photoNoteTestDatabase
import com.finnvek.knittools.data.storage.writeTestBitmap
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.ui.localeThemeCases
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale
import java.util.UUID

@RunWith(Parameterized::class)
class AuditPart12PhotoNoteUiTest(
    private val language: String,
    private val dark: Boolean,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun photoNoteEmojiBoundaryPersistsThroughActualEditorAndRoomReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".activepolicytest"))
        val arguments = InstrumentationRegistry.getArguments()
        arguments
            .getString(
                "textWidth",
            )?.let { assertEquals(it.toInt(), context.resources.configuration.screenWidthDp) }
        arguments.getString("textFontScale")?.let {
            assertEquals(it.toFloat(), context.resources.configuration.fontScale, 0.01f)
        }
        val projectId = System.nanoTime()
        val name = "audit12-photo-${UUID.randomUUID()}.db"
        var db = photoNoteTestDatabase(context, name)
        val source = File(context.cacheDir, "$name.png")
        try {
            writeTestBitmap(source)
            val repository =
                ProgressPhotoRepository(db.progressPhotoDao(), ProgressPhotoStorage(), context, Dispatchers.IO)
            runBlocking {
                db.counterProjectDao().insert(CounterProjectEntity(id = projectId, name = "Synthetic photo"))
                repository.savePhoto(projectId, source.toUri(), 42, "a".repeat(99) + "\uD83D\uDE00")
            }
            var photo by mutableStateOf(
                runBlocking {
                    db
                        .progressPhotoDao()
                        .getAllPhotosOnce()
                        .single()
                        .toDomain()
                },
            )
            assertEquals("a".repeat(99), photo.note)
            val uri = photo.photoUri
            val config =
                Configuration(
                    context.resources.configuration,
                ).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = context.createConfigurationContext(config)
            compose.setContent {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides compose.activity,
                    LocalContext provides localized,
                    LocalResources provides localized.resources,
                    LocalConfiguration provides config,
                ) {
                    KnitToolsTheme(isDarkTheme = dark) {
                        PhotoGalleryScreen(
                            photos = listOf(photo),
                            projectId = projectId,
                            canCreatePhoto = true,
                            proState = ProState(status = ProStatus.PRO_PURCHASED),
                            proAccessReady = true,
                            onBack = {},
                            onSeePro = {},
                            actions =
                                PhotoGalleryActions({}, {}, { _, _ -> }, {}, { _, _ -> false }, {}, { id, note ->
                                    runBlocking {
                                        repository.updatePhotoNote(id, note)
                                        photo =
                                            db
                                                .progressPhotoDao()
                                                .getAllPhotosOnce()
                                                .single()
                                                .toDomain()
                                    }
                                }),
                        )
                    }
                }
            }
            val values = photoNoteEmojiCases() + ("b".repeat(101) to "b".repeat(100))
            for ((input, expected) in values) {
                openEditor()
                val field = compose.onNode(hasSetTextAction())
                field.performClick().assertIsFocused().performTextReplacement(input)
                field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)))
                val save = compose.onNode(hasText(localized.getString(R.string.confirm)) and hasAnyAncestor(isDialog()))
                save.assertIsDisplayed()
                val density = localized.resources.displayMetrics.density
                val bounds = save.fetchSemanticsNode().touchBoundsInRoot
                assertTrue(bounds.height / density >= 47.5f && bounds.width / density >= 47.5f)
                save.performTouchInput { click() }
                compose.waitForIdle()
                assertEquals(
                    expected,
                    runBlocking {
                        db
                            .progressPhotoDao()
                            .getAllPhotosOnce()
                            .single()
                            .note
                    },
                )
            }
            openEditor()
            compose.onNode(hasSetTextAction()).performTextReplacement("Cancelled change")
            compose
                .onNode(
                    hasText(localized.getString(R.string.cancel)) and hasAnyAncestor(isDialog()),
                ).assertIsDisplayed()
                .performTouchInput { click() }
            assertEquals(
                "b".repeat(100),
                runBlocking {
                    db
                        .progressPhotoDao()
                        .getAllPhotosOnce()
                        .single()
                        .note
                },
            )
            db.close()
            db = photoNoteTestDatabase(context, name)
            val stored = runBlocking { db.progressPhotoDao().getAllPhotosOnce().single() }
            assertEquals("b".repeat(100), stored.note)
            assertEquals(uri, stored.photoUri)
            assertEquals(projectId, stored.projectId)
            assertEquals(42, stored.rowNumber)
        } finally {
            db.close()
            context.deleteDatabase(name)
            source.delete()
            File(context.filesDir, "progress_photos/$projectId").deleteRecursively()
        }
    }

    private fun openEditor() {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).performTouchInput { longClick() }
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-dark={1}")
        fun parameters(): List<Array<Any>> = localeThemeCases()
    }
}
