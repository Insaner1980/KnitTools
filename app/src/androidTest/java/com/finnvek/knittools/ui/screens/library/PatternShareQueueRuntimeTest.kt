package com.finnvek.knittools.ui.screens.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternShareQueueRuntimeTest {
    @get:Rule(order = 0)
    val timeout = Timeout.seconds(90)

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app: App = ApplicationProvider.getApplicationContext()
    private lateinit var activity: MainActivity

    @After
    fun closeActivity() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
    }

    @Test
    fun queuedLinksWaitForEachChoiceAndPreserveDraftWithoutSaving() {
        assertEquals("com.finnvek.knittools.activepolicytest", app.packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, app.checkSelfPermission(Manifest.permission.INTERNET))
        assertEquals(PackageManager.PERMISSION_DENIED, app.checkSelfPermission("com.android.vending.BILLING"))
        launchActivity()
        val patternsBefore = savedPatterns()

        for (useFirst in listOf(false, true)) {
            deliver("original", "Original draft")
            assertDraft("Original draft", "original")
            deliver("first", "First link")
            assertChoice()
            instrumentation.runOnMainSync {
                deliverOnMain("second", "Second link")
                deliverOnMain("third", "Third link")
            }
            assertChoice()
            assertDraft("Original draft", "original")

            choose(useFirst)
            assertDraft(if (useFirst) "First link" else "Original draft", if (useFirst) "first" else "original")
            assertChoice()

            choose(!useFirst)
            assertDraft(if (useFirst) "First link" else "Second link", if (useFirst) "first" else "second")
            assertChoice()

            choose(useFirst)
            assertDraft(if (useFirst) "Third link" else "Second link", if (useFirst) "third" else "second")
            waitUntil {
                !hasChoice(R.string.web_pattern_keep_current_draft) &&
                    !hasChoice(R.string.web_pattern_use_shared_link)
            }
            instrumentation.runOnMainSync {
                assertEquals(Intent.ACTION_MAIN, activity.intent.action)
                assertNull(activity.intent.type)
                assertNull(activity.intent.getStringExtra(Intent.EXTRA_TEXT))
                assertNull(activity.intent.getStringExtra(Intent.EXTRA_SUBJECT))
            }
            rule
                .onNodeWithText(app.getString(R.string.cancel))
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            waitUntil { editorTexts().isEmpty() }
        }

        assertEquals(patternsBefore, savedPatterns())
    }

    @Test
    fun linkSharedDuringSaveOpensAfterTheSavingEditorCloses() {
        assertEquals("com.finnvek.knittools.activepolicytest", app.packageName)
        launchActivity()
        val database = app.database.get()
        val savedPath = "queued-while-saving-first"
        val savedUrl = "https://example.invalid/$savedPath"
        try {
            deliver(savedPath, "First saved pattern")
            assertDraft("First saved pattern", savedPath)
            runBlocking {
                val transactionStarted = CompletableDeferred<Unit>()
                val releaseTransaction = CompletableDeferred<Unit>()
                launch(Dispatchers.IO) {
                    database.withTransaction {
                        transactionStarted.complete(Unit)
                        releaseTransaction.await()
                    }
                }
                try {
                    transactionStarted.await()
                    val save = hasText(app.getString(R.string.save)) and hasClickAction()
                    rule.onNode(save).performScrollTo().performClick()
                    waitUntil { rule.onAllNodes(save).fetchSemanticsNodes().isEmpty() }
                    deliverDirectly("https://example.invalid/queued-while-saving-next", "Next shared pattern")
                    rule.waitForIdle()
                } finally {
                    releaseTransaction.complete(Unit)
                }
            }
            assertDraft("Next shared pattern", "queued-while-saving-next")
        } finally {
            runBlocking(Dispatchers.IO) {
                val dao = database.savedPatternDao()
                dao.getByCanonicalUrl(savedUrl)?.let { dao.deleteById(it.id) }
            }
        }
    }

    @Test
    fun sharedRavelryLinkWaitsForAnOpenWebEditorDraft() {
        assertEquals("com.finnvek.knittools.activepolicytest", app.packageName)
        launchActivity()
        deliver("draft-before-ravelry", "Draft before Ravelry")
        assertDraft("Draft before Ravelry", "draft-before-ravelry")

        deliverDirectly("https://www.ravelry.com/patterns/library/synthetic-pattern", "Ravelry pattern")
        rule.waitForIdle()
        assertDraft("Draft before Ravelry", "draft-before-ravelry")
        rule.onNodeWithText(app.getString(R.string.cancel)).performScrollTo().performClick()

        waitUntil { editorTexts().isEmpty() }
        rule.onNodeWithText(app.getString(R.string.ravelry_browser_hint)).assertIsDisplayed()
    }

    private fun savedPatterns() =
        runBlocking(Dispatchers.IO) {
            app.database
                .get()
                .savedPatternDao()
                .getAllOnce()
        }

    private fun launchActivity() {
        val intent =
            Intent(app, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            )
        instrumentation.runOnMainSync { app.startActivity(intent) }
        waitUntil {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .singleOrNull()
                    ?.let { activity = it }
            }
            ::activity.isInitialized
        }
    }

    private fun deliver(
        path: String,
        title: String,
    ) {
        instrumentation.runOnMainSync { deliverOnMain(path, title) }
    }

    private fun deliverOnMain(
        path: String,
        title: String,
    ) {
        app.startActivity(
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://example.invalid/$path")
                .putExtra(Intent.EXTRA_SUBJECT, title),
        )
    }

    private fun deliverDirectly(
        url: String,
        title: String,
    ) {
        instrumentation.runOnMainSync {
            instrumentation.callActivityOnNewIntent(
                activity,
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, url)
                    .putExtra(Intent.EXTRA_SUBJECT, title),
            )
        }
    }

    private fun assertDraft(
        title: String,
        path: String,
    ) {
        val expected = listOf(title, "https://example.invalid/$path").sorted()
        waitUntil { editorTexts().sorted() == expected }
        assertEquals(expected, editorTexts().sorted())
    }

    private fun editorTexts(): List<String> =
        listOfNotNull(fieldText(WEB_PATTERN_TITLE_FIELD_TAG), fieldText(WEB_PATTERN_URL_FIELD_TAG))
            .filter(String::isNotEmpty)

    private fun fieldText(tag: String): String? =
        rule
            .onAllNodesWithTag(tag)
            .fetchSemanticsNodes()
            .singleOrNull()
            ?.config
            ?.getOrNull(SemanticsProperties.EditableText)
            ?.text

    private fun choice(resource: Int) = hasText(app.getString(resource)) and hasClickAction()

    private fun hasChoice(resource: Int): Boolean = rule.onAllNodes(choice(resource)).fetchSemanticsNodes().isNotEmpty()

    private fun assertChoice() {
        waitUntil {
            hasChoice(R.string.web_pattern_keep_current_draft) && hasChoice(R.string.web_pattern_use_shared_link)
        }
        rule.onNode(choice(R.string.web_pattern_keep_current_draft)).assertIsDisplayed()
        rule.onNode(choice(R.string.web_pattern_use_shared_link)).assertIsDisplayed()
    }

    private fun choose(useIncoming: Boolean) {
        val label = if (useIncoming) R.string.web_pattern_use_shared_link else R.string.web_pattern_keep_current_draft
        rule.onNode(choice(label)).assertIsDisplayed().performClick()
    }

    private fun waitUntil(condition: () -> Boolean) = rule.waitUntil(timeoutMillis = 10_000L, condition = condition)
}
