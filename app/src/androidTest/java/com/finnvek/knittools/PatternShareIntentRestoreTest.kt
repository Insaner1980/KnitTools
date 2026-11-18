package com.finnvek.knittools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.SpannableString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.data.storage.writeOnePagePdf
import com.finnvek.knittools.ui.navigation.IncomingPdfViewModel
import com.finnvek.knittools.ui.navigation.PatternShareCoordinatorViewModel
import com.finnvek.knittools.ui.navigation.PatternShareError
import com.finnvek.knittools.ui.navigation.PatternShareOfferResult
import com.finnvek.knittools.ui.navigation.PatternSharePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PatternShareIntentRestoreTest {
    @Test
    fun externalExtrasCannotSeedPrivateShareState() {
        val launch =
            isolatedMainIntent().apply {
                putExtra("pattern_share_pending_request_id", 41L)
                putExtra("pattern_share_pending_kind", "web")
                putExtra("pattern_share_pending_url", "https://example.test/unrequested")
                putExtra("pattern_share_next_request_id", 99L)
            }
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            scenario.onActivity { activity ->
                val coordinator = ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java]
                assertNull(coordinator.pending.value)
                val offered = coordinator.offer(PatternSharePayload.Error(PatternShareError.Empty))
                assertEquals(1L, (offered as PatternShareOfferResult.Accepted).request.requestId)
                coordinator.acknowledge(offered.request.requestId)
            }
        }
    }

    @Test
    fun wronglyTypedExternalShareStateDoesNotInterruptActivityCreation() {
        val launch =
            isolatedMainIntent().apply {
                putExtra("pattern_share_pending_request_id", "not-a-long")
            }
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            scenario.onActivity { activity ->
                assertNull(ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java].pending.value)
            }
        }
    }

    @Test
    fun stagedPdfIsNotCopiedAgainWhenTheOriginalSystemIntentIsRestored() = verifyPdfIntentRestore(discard = false)

    @Test
    fun discardedPdfIsNotReopenedByRestorationAndANewShareIsAccepted() = verifyPdfIntentRestore(discard = true)

    @Test
    fun restoredTextConsumptionDoesNotClearPdfSendIntent() {
        val launch = isolatedMainIntent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("pdf-after-text-", ".pdf", context.cacheDir)
        try {
            writeOnePagePdf(source)
            ActivityScenario.launch<MainActivity>(launch).use { scenario ->
                scenario.onActivity {
                    val textShare =
                        Intent(launch).apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://example.org/pattern")
                        }
                    InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, textShare)
                }
                awaitConsumed(scenario)
                val current = AtomicReference<MainActivity>()
                scenario.onActivity { current.set(it) }
                val resumed = CountDownLatch(1)
                val callback =
                    ActivityLifecycleCallback { activity, stage ->
                        if (activity is MainActivity && stage == Stage.PRE_ON_CREATE) {
                            activity.intent =
                                Intent(launch).apply {
                                    action = Intent.ACTION_SEND
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(source))
                                }
                            current.set(activity)
                        }
                        if (activity === current.get() && stage == Stage.RESUMED) resumed.countDown()
                    }
                val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
                lifecycle.addLifecycleCallback(callback)
                try {
                    onActivity(current) { it.recreate() }
                    assertTrue(resumed.await(15, TimeUnit.SECONDS))
                    awaitPdfIntentSettled(current, expectedPending = true)
                } finally {
                    lifecycle.removeLifecycleCallback(callback)
                    onActivity(current) { ViewModelProvider(it)[IncomingPdfViewModel::class.java].discard() }
                    awaitPdfIntentSettled(current, expectedPending = false)
                }
            }
        } finally {
            source.delete()
        }
    }

    private fun isolatedMainIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest") || context.packageName.endsWith(".notestest"))
        return Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
    }

    private fun verifyPdfIntentRestore(discard: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest") || context.packageName.endsWith(".notestest"))
        val source = File.createTempFile("pdf-intent-", ".pdf", context.cacheDir)
        writeOnePagePdf(source)
        val original =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(Uri.fromFile(source), "application/pdf")
            }
        try {
            val launch = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            ActivityScenario.launch<MainActivity>(launch).use { scenario ->
                val current = AtomicReference<MainActivity>()
                scenario.onActivity { current.set(it) }
                try {
                    onActivity(current) {
                        InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, Intent(original))
                    }
                    awaitPdfIntentSettled(current, expectedPending = true)
                    var stagedUri = ""
                    onActivity(current) { activity ->
                        val model = ViewModelProvider(activity)[IncomingPdfViewModel::class.java]
                        stagedUri = checkNotNull(model.pending.value).localUri
                        if (discard) model.discard()
                    }
                    if (discard) awaitPdfIntentSettled(current, expectedPending = false)
                    if (discard) {
                        onActivity(current) {
                            InstrumentationRegistry
                                .getInstrumentation()
                                .callActivityOnNewIntent(it, isolatedMainIntent())
                        }
                    }
                    var restored = false
                    val resumed = CountDownLatch(1)
                    val callback =
                        ActivityLifecycleCallback { activity, stage ->
                            if (activity is MainActivity && stage == Stage.PRE_ON_CREATE) {
                                activity.intent = Intent(original)
                                current.set(activity)
                                restored = true
                            }
                            if (restored && activity === current.get() && stage == Stage.RESUMED) resumed.countDown()
                        }
                    val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
                    lifecycle.addLifecycleCallback(callback)
                    try {
                        // The restored VIEW intent differs from Scenario's MAIN launch filter.
                        // Follow the actual lifecycle instead of Scenario's intent-matching recreation wait.
                        onActivity(current) { it.recreate() }
                        assertTrue("Recreated Activity must resume", resumed.await(15, TimeUnit.SECONDS))
                    } finally {
                        lifecycle.removeLifecycleCallback(callback)
                    }
                    assertTrue("Original PDF intent must be restored before onCreate", restored)
                    awaitPdfIntentSettled(current)
                    onActivity(current) { activity ->
                        val pending = ViewModelProvider(activity)[IncomingPdfViewModel::class.java].pending.value
                        assertEquals(if (discard) null else stagedUri, pending?.localUri)
                    }
                    if (discard) {
                        onActivity(current) {
                            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, Intent(original))
                        }
                        awaitPdfIntentSettled(current, expectedPending = true)
                    }
                } finally {
                    onActivity(current) { ViewModelProvider(it)[IncomingPdfViewModel::class.java].discard() }
                    awaitPdfIntentSettled(current, expectedPending = false)
                    onActivity(current) { it.finish() }
                }
            }
        } finally {
            source.delete()
        }
    }

    private fun awaitPdfIntentSettled(
        current: AtomicReference<MainActivity>,
        expectedPending: Boolean? = null,
    ) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var settled = false
            onActivity(current) { activity ->
                val model = ViewModelProvider(activity)[IncomingPdfViewModel::class.java]
                settled =
                    activity.intent.action == Intent.ACTION_MAIN &&
                    !model.receiving.value &&
                    (expectedPending == null || (model.pending.value != null) == expectedPending)
            }
            if (settled) return
            SystemClock.sleep(50)
        }
        throw AssertionError("PDF intent did not settle with expected pending state: $expectedPending")
    }

    private fun onActivity(
        current: AtomicReference<MainActivity>,
        action: (MainActivity) -> Unit,
    ) = InstrumentationRegistry.getInstrumentation().runOnMainSync { action(checkNotNull(current.get())) }

    @Test
    fun consumedShareDoesNotReplayWhenRecreationReceivesOriginalSystemIntent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, SpannableString("https://example.org/first"))
                putExtra(Intent.EXTRA_SUBJECT, "Synthetic pattern")
            }
        val launch = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            scenario.onActivity {
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, Intent(original))
                val coordinator = ViewModelProvider(it)[PatternShareCoordinatorViewModel::class.java]
                assertEquals(
                    PatternSharePayload.WebLink("https://example.org/first", "Synthetic pattern"),
                    coordinator.pending.value?.payload,
                )
            }
            awaitConsumed(scenario)
            var originalIntentRestored = false
            val restoreIntent =
                ActivityLifecycleCallback { activity, stage ->
                    if (activity is MainActivity && stage == Stage.PRE_ON_CREATE) {
                        activity.intent = Intent(original)
                        originalIntentRestored = true
                    }
                }
            val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
            lifecycle.addLifecycleCallback(restoreIntent)
            try {
                scenario.recreate()
            } finally {
                lifecycle.removeLifecycleCallback(restoreIntent)
            }
            assertTrue("Original system intent must be restored before onCreate", originalIntentRestored)
            awaitConsumed(scenario)
            scenario.onActivity { activity ->
                val coordinator = ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java]
                val offered = coordinator.offer(PatternSharePayload.WebLink("https://example.org/second", "Second"))
                assertTrue(offered is PatternShareOfferResult.Accepted)
                assertEquals(2L, (offered as PatternShareOfferResult.Accepted).request.requestId)
                coordinator.acknowledge(offered.request.requestId)
            }
        }
    }

    private fun awaitConsumed(scenario: ActivityScenario<MainActivity>) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var consumed = false
            scenario.onActivity { activity ->
                consumed =
                    activity.intent.action == Intent.ACTION_MAIN &&
                    !activity.intent.hasExtra(Intent.EXTRA_TEXT) &&
                    ViewModelProvider(activity)[PatternShareCoordinatorViewModel::class.java].pending.value == null
            }
            if (consumed) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Share was not acknowledged and cleared")
    }
}
