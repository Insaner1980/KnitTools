package com.finnvek.knittools.pro

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.storage.writeOnePagePdf
import com.finnvek.knittools.data.time.AndroidSessionTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.io.File
import java.util.concurrent.TimeUnit

/** Synteettinen oikeustila, oikea Activity, navigaatio ja atomiset DataStore-varaukset. */
class TrialNoticeRestorationTest {
    @get:Rule
    val timeout = Timeout.seconds(60)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var app: App
    private lateinit var trialManager: TrialManager
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalAccessibilityFlags: Int? = null
    private var originalTrialPreferences: Preferences? = null

    @Before
    fun setup() =
        runBlocking<Unit>(Dispatchers.IO) {
            app = ApplicationProvider.getApplicationContext()
            check(app.packageName == "com.finnvek.knittools.activepolicytest" && ProPolicyTestControl.isActive)
            instrumentation.uiAutomation.serviceInfo =
                instrumentation.uiAutomation.serviceInfo.apply {
                    originalAccessibilityFlags = flags
                    flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                }
            ProPolicyTestControl.setPurchased(false)
            ProPolicyTestControl.setTrialState(TrialState())
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .initialStateReady
                    .first { it }
            }
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            // These fixtures represent an existing user after onboarding.
            val onboarding = app.onboardingStateRepository.get()
            onboarding.dismissWelcome()
            onboarding.draft
                .first()
                ?.takeIf { !it.completed || it.exitPending }
                ?.let { draft -> check(onboarding.saveDraft(draft.copy(completed = true, exitPending = false))) }
            // The Activity claims notices through the application's TrialManager.
            trialManager =
                ProManager::class.java
                    .getDeclaredField("trialManager")
                    .apply { isAccessible = true }
                    .get(app.proManager.get()) as TrialManager
            val store = trialDataStore()
            originalTrialPreferences = store.data.first()
            val now = AndroidSessionTimeSource(app).snapshot()
            store.seedTrialClock(now, TimeUnit.DAYS.toMillis(15))
            trialManager.initialize()
            ProPolicyTestControl.setTrialState(trialManager.trialState.value)
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == ProStatus.TRIAL_EXPIRED }
            }
        }

    @After
    fun close() {
        try {
            scenario?.onActivity { it.intent = launcherIntent() }
            scenario?.close()
        } finally {
            try {
                originalTrialPreferences?.let { previous ->
                    runBlocking(Dispatchers.IO) {
                        trialDataStore().updateData { previous }
                        trialManager.initialize()
                    }
                }
            } finally {
                if (ProPolicyTestControl.isActive) ProPolicyTestControl.reset()
                originalAccessibilityFlags?.let { original ->
                    instrumentation.uiAutomation.serviceInfo =
                        instrumentation.uiAutomation.serviceInfo.apply { flags = original }
                }
            }
        }
    }

    @Test
    fun visibleNoticeSurvivesRecreationWithoutAnotherClaim() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        assertFalse(runBlocking { trialManager.claimTrialEndNotice() })
        recreate()
        waitText(R.string.trial_ended_screen_title)
        assertFalse(runBlocking { trialManager.claimTrialEndNotice() })
    }

    @Test
    fun purchaseFeedbackSurvivesTheNoticeBeingHiddenDuringCheckout() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        checkNotNull(scenario).onActivity { activity ->
            val field = activity.billingManager.javaClass.getDeclaredField("_purchaseFlowInFlight")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val inFlight = field.get(activity.billingManager) as MutableStateFlow<Boolean>
            inFlight.value = true
        }
        waitForTrialUi("Notice hidden during checkout") { findText(text(R.string.trial_ended_screen_title)) == null }
        checkNotNull(scenario).onActivity { activity ->
            activity.billingManager.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED).build(),
                null,
            )
        }
        waitText(R.string.billing_purchase_cancelled)
        waitText(R.string.pro_continue_free)
    }

    @Test
    fun continueFreeStaysDismissedAfterRecreation() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        click(R.string.pro_continue_free)
        waitRootReady()
        recreate()
        waitRootReady()
        assertNoNotice()
        assertFalse(runBlocking { trialManager.claimTrialEndNotice() })
    }

    @Test
    fun seeProRestoresOneUpgradeDestinationWithoutNotice() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        click(R.string.pro_prompt_see_pro)
        waitText(R.string.onboarding_pro_intro)
        recreate()
        waitText(R.string.onboarding_pro_intro)
        assertNoNotice()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitRootReady()
        waitForTrialUi("Upgrade destination closed") { findText(text(R.string.onboarding_pro_intro)) == null }
        assertNoNotice()
    }

    @Test
    fun purchasedStateConsumesRestoredObsoleteNotice() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
        ProPolicyTestControl.setPurchased(true)
        runBlocking {
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == ProStatus.PRO_PURCHASED }
            }
        }
        recreate()
        checkNotNull(scenario).moveToState(Lifecycle.State.RESUMED)
        waitRootReady()
        assertNoNotice()
        ProPolicyTestControl.setPurchased(false)
        runBlocking {
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == ProStatus.TRIAL_EXPIRED }
            }
        }
        recreate()
        waitRootReady()
        assertNoNotice()
    }

    @Test
    fun pendingNoticeWaitsForEntitlementReadinessAfterRecreation() {
        launch()
        waitText(R.string.trial_ended_screen_title)
        val ready = initialStateReady()
        try {
            ready.value = false
            waitRootReady()
            recreate()
            waitRootReady()
            assertNoNotice()
        } finally {
            ready.value = true
        }
        waitText(R.string.trial_ended_screen_title)
        assertFalse(runBlocking { trialManager.claimTrialEndNotice() })
    }

    @Test
    fun widgetPromptSuppressesPendingNoticeAcrossRecreation() {
        verifySuppressedLaunch(
            MainActivity.createWidgetProPromptLaunchIntent(app),
            R.string.pro_prompt_widget_pin_title,
            restoredText = null,
        )
    }

    @Test
    fun consumedShareSuppressesPendingNoticeAcrossRecreation() {
        verifySuppressedLaunch(
            Intent(app, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://example.com/synthetic-pattern")
            },
            R.string.web_pattern_confirm_details,
        )
    }

    @Test
    fun incomingPdfSuppressesPendingNoticeAcrossRecreation() {
        val file = File.createTempFile("trial-notice-test-", ".pdf", app.cacheDir)
        try {
            writeOnePagePdf(file)
            val resolver = app.contentResolver
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val source = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            try {
                checkNotNull(resolver.openOutputStream(source)).use { output ->
                    file.inputStream().use { it.copyTo(output) }
                }
                verifySuppressedLaunch(
                    Intent(app, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        setDataAndType(source, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    R.string.incoming_pdf_title,
                )
            } finally {
                resolver.delete(source, null, null)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun consumedCounterLaunchSuppressesPendingNoticeAcrossRecreation() {
        val id =
            runBlocking(Dispatchers.IO) {
                app.database
                    .get()
                    .counterProjectDao()
                    .insert(CounterProjectEntity(name = "Trial notice target"))
            }
        try {
            launch()
            waitText(R.string.trial_ended_screen_title)
            deliver(MainActivity.createCounterLaunchIntent(app, id))
            waitText("Trial notice target")
            assertNoNotice()
            recreate()
            waitText("Trial notice target")
            assertNoNotice()
        } finally {
            runBlocking(Dispatchers.IO) {
                app.database
                    .get()
                    .counterProjectDao()
                    .delete(id)
            }
        }
    }

    @Test
    fun concurrentClaimsHaveOneWinnerAndReopenedManagerCannotReclaim() =
        runBlocking {
            val results = List(4) { async(Dispatchers.IO) { trialManager.claimTrialEndNotice() } }.awaitAll()
            assertEquals(1, results.count { it })
            assertFalse(TrialManager(app, Dispatchers.IO, AndroidSessionTimeSource(app)).claimTrialEndNotice())
        }

    @Test
    fun threeDayNoticeClaimsRealPersistedTrialOnce() {
        seedActiveTrial(daysRemaining = 3)
        launch()
        waitText(R.string.trial_three_days_left)

        assertEquals(3, trialManager.trialState.value.daysRemaining)
        assertFalse(runBlocking { trialManager.claimTrialThreeDayNotice() })
        assertEquals(null, findText(text(R.string.trial_ended_screen_title)))
    }

    @Test
    fun threeDayNoticeSurvivesRecreationWithoutAnotherClaim() {
        seedActiveTrial(daysRemaining = 3)
        launch()
        waitText(R.string.trial_three_days_left)
        assertFalse(runBlocking { trialManager.claimTrialThreeDayNotice() })

        recreate()

        waitText(R.string.trial_three_days_left)
        assertFalse(runBlocking { trialManager.claimTrialThreeDayNotice() })
    }

    @Test
    fun dismissedThreeDayNoticeStaysDismissedAfterRecreationAndRelaunch() {
        seedActiveTrial(daysRemaining = 3)
        launch()
        waitText(R.string.trial_three_days_left)
        click(R.string.trial_notice_acknowledge)
        waitRootReady()
        recreate()
        waitRootReady()
        assertNoNotice()

        checkNotNull(scenario).close()
        scenario = null
        launch()
        waitRootReady()
        assertNoNotice()
        assertFalse(runBlocking { trialManager.claimTrialThreeDayNotice() })
    }

    @Test
    fun twoRemainingDaysDoNotShowThreeDayNotice() {
        seedActiveTrial(daysRemaining = 2)
        launch()
        waitRootReady()

        assertNoNotice()
        assertEquals(2, trialManager.trialState.value.daysRemaining)
        assertFalse(runBlocking { trialManager.claimTrialThreeDayNotice() })
    }

    private fun seedActiveTrial(daysRemaining: Int) =
        runBlocking(Dispatchers.IO) {
            val now = AndroidSessionTimeSource(app).snapshot()
            val elapsed = TimeUnit.DAYS.toMillis((TrialManager.TRIAL_DURATION_DAYS - daysRemaining).toLong())
            trialDataStore().seedTrialClock(now, elapsed)
            trialManager.initialize()
            ProPolicyTestControl.setTrialState(trialManager.trialState.value)
            withTimeout(10_000) {
                app.proManager.get().proState.first {
                    it.status == ProStatus.TRIAL_ACTIVE && it.trialDaysRemaining == daysRemaining
                }
            }
        }

    private fun verifySuppressedLaunch(
        intent: Intent,
        expectedText: Int,
        restoredText: Int? = expectedText,
    ) {
        launch()
        waitText(R.string.trial_ended_screen_title)
        deliver(intent)
        waitText(expectedText)
        assertNoNotice()
        recreate()
        if (restoredText == null) waitRootReady() else waitText(restoredText)
        assertNoNotice()
    }

    private fun launcherIntent() =
        Intent(app, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

    private fun launch() {
        scenario = ActivityScenario.launch(launcherIntent())
        lateinit var facts: StateFlow<TrialJourneyFacts?>
        checkNotNull(scenario).onActivity { facts = it.trialJourneyStateRepository.state }
        val expectedTrialStart =
            app.proManager
                .get()
                .proState.value.trialStartTimestamp
        runBlocking { withTimeout(10_000) { facts.first { it?.trialStartTimestamp == expectedTrialStart } } }
    }

    private fun deliver(intent: Intent) {
        checkNotNull(scenario).onActivity {
            instrumentation.callActivityOnNewIntent(it, intent)
        }
    }

    private fun recreate() {
        val current = checkNotNull(scenario)
        var previous: MainActivity? = null
        current.onActivity {
            previous = it
            // Requests have been consumed; keep Scenario's lifecycle matching identity stable.
            it.intent = launcherIntent()
        }
        current.recreate()
        if (current.state == Lifecycle.State.RESUMED) {
            current.onActivity { assertNotSame(previous, it) }
            instrumentation.waitForIdleSync()
        }
    }

    private fun text(id: Int) = app.getString(id)

    private fun waitText(id: Int) = waitText(text(id))

    private fun waitText(value: String) {
        waitForTrialUi("Visible text: $value") { findText(value)?.isVisibleToUser == true }
    }

    private fun assertNoNotice() {
        instrumentation.waitForIdleSync()
        assertEquals(null, findText(text(R.string.trial_ended_screen_title)))
        assertEquals(null, findText(text(R.string.trial_three_days_left)))
    }

    private fun waitRootReady() {
        waitForTrialUi("Projects root after splash and startup gates") {
            instrumentation.uiAutomation.rootInActiveWindow?.let(::hasVisibleStartupReady) == true
        }
    }

    private fun hasVisibleStartupReady(node: AccessibilityNodeInfo): Boolean {
        // Composen virtuaalisolmu löytyy puusta, vaikka suora resurssitunnistehaku palauttaa tyhjän listan.
        if (node.viewIdResourceName == "startup_ready" && node.isVisibleToUser) return true
        for (index in 0 until node.childCount) {
            if (node.getChild(index)?.let(::hasVisibleStartupReady) == true) return true
        }
        return false
    }

    private fun click(id: Int) = instrumentation.clickTrialText(text(id), includeContentDescription = true)

    private fun findText(value: String): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) instrumentation.uiAutomation.clearCache()
        return instrumentation.findTrialText(value, includeContentDescription = true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun trialDataStore(): DataStore<Preferences> =
        TrialManager.Companion::class.java
            .getDeclaredMethod("getTrialDataStore", Context::class.java)
            .apply { isAccessible = true }
            .invoke(TrialManager.Companion, app) as DataStore<Preferences>

    @Suppress("UNCHECKED_CAST")
    private fun initialStateReady(): MutableStateFlow<Boolean> =
        ProManager::class.java
            .getDeclaredField("_initialStateReady")
            .apply { isAccessible = true }
            .get(app.proManager.get()) as MutableStateFlow<Boolean>
}
