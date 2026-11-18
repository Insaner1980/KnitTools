package com.finnvek.knittools

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
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
import java.util.UUID
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
    fun privateCachePdfCannotBeOpenedByFileUri() {
        val launch = isolatedMainIntent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("private-pdf-", ".pdf", context.cacheDir)
        try {
            writeOnePagePdf(source)
            val view = launch.setAction(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(source), "application/pdf")
            verifyRejectedPdf(view)
        } finally {
            source.delete()
        }
    }

    @Test
    fun explicitlyUnsupportedMimeTypeCannotOpenPdfEvenIfTheProviderReportsPdf() =
        withPdfSource { source ->
            verifyRejectedPdf(isolatedMainIntent().setAction(Intent.ACTION_VIEW).setDataAndType(source, "image/png"))
        }

    @Test
    fun pdfSendDoesNotFollowAnIntentInTheStreamExtra() {
        val send =
            isolatedMainIntent()
                .setAction(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, Intent(Intent.ACTION_VIEW, Uri.parse("https://example.invalid/")))
        verifyRejectedPdf(send)
    }

    private fun verifyRejectedPdf(delivery: Intent) {
        val launch = isolatedMainIntent()
        val current = AtomicReference<MainActivity>()
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            scenario.onActivity {
                current.set(it)
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, delivery)
            }
            try {
                val deadline = SystemClock.elapsedRealtime() + 15_000
                while (SystemClock.elapsedRealtime() < deadline) {
                    var receiving = true
                    var staged = false
                    onActivity(current) { activity ->
                        val model = ViewModelProvider(activity)[IncomingPdfViewModel::class.java]
                        receiving = model.receiving.value
                        staged = model.pending.value != null
                    }
                    if (!receiving) {
                        assertTrue("Unsupported input must not stage a PDF", !staged)
                        return
                    }
                    SystemClock.sleep(50)
                }
                throw AssertionError("PDF intent did not settle")
            } finally {
                onActivity(current) {
                    ViewModelProvider(it)[IncomingPdfViewModel::class.java].discard()
                    it.intent = launch
                }
            }
        }
    }

    @Test
    fun restoredTextConsumptionDoesNotClearPdfSendIntent() {
        val launch = isolatedMainIntent()
        withPdfSource { source ->
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
                                    putExtra(Intent.EXTRA_STREAM, source)
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
        }
    }

    private fun isolatedMainIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest") || context.packageName.endsWith(".notestest"))
        return Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
    }

    private fun verifyPdfIntentRestore(discard: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        withPdfSource { source ->
            val original =
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    setData(source)
                }
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
        }
    }

    private fun withPdfSource(test: (Uri) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName.endsWith(".activepolicytest") || context.packageName.endsWith(".notestest"))
        val resolver = context.contentResolver
        val file = File.createTempFile("pdf-share-", ".pdf", context.cacheDir)
        try {
            writeOnePagePdf(file)
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "knittools-test-${UUID.randomUUID()}.pdf")
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            try {
                checkNotNull(resolver.openOutputStream(uri)).use { output ->
                    file.inputStream().use { it.copyTo(output) }
                }
                assertEquals("application/pdf", resolver.getType(uri))
                test(uri)
            } finally {
                resolver.delete(uri, null, null)
            }
        } finally {
            file.delete()
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
