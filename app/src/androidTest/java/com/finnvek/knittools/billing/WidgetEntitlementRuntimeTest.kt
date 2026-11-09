package com.finnvek.knittools.billing

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.finnvek.knittools.App
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.widget.CounterWidgetActions
import com.finnvek.knittools.widget.CounterWidgetReceiver
import com.finnvek.knittools.widget.CounterWidgetState
import com.finnvek.knittools.widget.toWidgetData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic BillingClient responses through real billing, Pro, RemoteViews and receiver paths. */
class WidgetEntitlementRuntimeTest {
    @Test
    fun unresolvedOwnershipAndLaterResultsUpdatePresentationAndActions() =
        withWidget {
            assertRetry()
            assertFalse(billing.purchaseStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertCountActionBlocked()

            query(purchased = true)
            awaitCounter("0")
            click(app.getString(R.string.counter_increase), description = true)
            awaitCount(1)

            query(code = BillingClient.BillingResponseCode.NETWORK_ERROR)
            assertTrue(billing.isProPurchased.value)
            assertTrue(billing.purchaseStateReady.value)
            click(app.getString(R.string.counter_increase), description = true)
            awaitCount(2)

            query()
            await {
                app.getString(R.string.widget_pro_required) in texts() ||
                    app.getString(R.string.upgrade_to_pro) in texts()
            }
            assertCountActionBlocked(expected = 2)

            ProPolicyTestControl.setTrialState(TrialState(hasStarted = true, isActive = true, daysRemaining = 7))
            awaitCounter("2")
            query()
            assertEquals(
                ProStatus.TRIAL_ACTIVE,
                app.proManager
                    .get()
                    .proState.value.status,
            )
            click(app.getString(R.string.counter_increase), description = true)
            awaitCount(3)
        }

    @Test
    fun failedOwnershipQueryOffersWorkingRetryWithoutGrantingAccess() =
        withWidget(initialFailure = true) {
            assertRetry()
            assertFalse(billing.purchaseStateReady.value)
            assertTrue(billing.purchaseQueryFailed.value)
            assertCountActionBlocked()

            val queries = client.queries
            click(app.getString(R.string.retry))
            await { client.queries > queries }
            assertRetry()
            assertFalse(billing.purchaseStateReady.value)

            ProPolicyTestControl.setTrialState(TrialState(hasStarted = true, isActive = true, daysRemaining = 7))
            awaitCounter("0")
            assertFalse(billing.purchaseStateReady.value)
            assertFalse(billing.isProPurchased.value)
            click(app.getString(R.string.counter_increase), description = true)
            awaitCount(1)
            ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
            awaitText(app.getString(R.string.retry))

            withContext(Dispatchers.Main) {
                client.queryCode = BillingClient.BillingResponseCode.OK
                client.purchases = listOf(purchase())
            }
            click(app.getString(R.string.retry))
            awaitCounter("1")
            click(app.getString(R.string.counter_increase), description = true)
            awaitCount(2)
        }

    @Test
    fun retryTimeoutKeepsUnknownStateAndReleasesReceiver() =
        withWidget(initialFailure = true) {
            assertRetry()
            client.deferQueries = true
            sendAction(CounterWidgetActions.retryIntent(app))
            assertRetry()
            assertFalse(billing.purchaseStateReady.value)
            withContext(Dispatchers.Main) {
                client.deferQueries = false
                client.queryCode = BillingClient.BillingResponseCode.OK
                client.purchases = listOf(purchase())
                checkNotNull(client.deferredQuery).onQueryPurchasesResponse(
                    BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(),
                    client.purchases,
                )
            }
            assertFalse("A cancelled retry must not grant access from its late response", billing.isProPurchased.value)
            click(app.getString(R.string.retry))
            awaitCounter("0")
        }

    private fun withWidget(
        initialFailure: Boolean = false,
        block: suspend Fixture.() -> Unit,
    ) = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<App>()
        check(app.packageName.endsWith(".activepolicytest"))
        assertTrue(BuildConfig.DEBUG)
        assertTrue(ProPolicyTestControl.isActive)
        assertFalse("Debug unlocking must not bypass this decision", ProState().hasFeature(ProFeature.WIDGET))
        val fixture = Fixture(app)
        try {
            fixture.start(initialFailure)
            fixture.block()
        } finally {
            fixture.close()
        }
    }

    private class Fixture(
        val app: App,
    ) {
        val billing = app.billingManager.get()
        val client = AuditPart15BillingClient()
        private val repository = app.counterRepository.get()
        private val host = AppWidgetHost(app, 110014)
        private val instrumentation = InstrumentationRegistry.getInstrumentation()
        private var scenario: ActivityScenario<ComponentActivity>? = null
        private var view: AppWidgetHostView? = null
        private var widgetId = 0
        private var projectId = 0L

        suspend fun start(initialFailure: Boolean) {
            withContext(Dispatchers.Main) {
                billing.destroy()
                BillingManager::class.java
                    .getDeclaredField(
                        "billingClient",
                    ).apply { isAccessible = true }
                    .set(billing, client)
                ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
            }
            await {
                !app.proManager
                    .get()
                    .initialStateReady.value
            }
            if (initialFailure) query(code = BillingClient.BillingResponseCode.NETWORK_ERROR)
            val created = repository.createProject("Entitlement test", canCreateAdditionalProjects = true)
            check(created is ProjectCreationResult.Created)
            projectId = created.projectId
            CounterWidgetState.save(app, checkNotNull(repository.getProject(projectId)).toWidgetData())
            instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            host.startListening()
            val manager = AppWidgetManager.getInstance(app)
            widgetId = host.allocateAppWidgetId()
            val options =
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160)
                }
            assertTrue(
                manager.bindAppWidgetIdIfAllowed(
                    widgetId,
                    ComponentName(app, CounterWidgetReceiver::class.java),
                    options,
                ),
            )
            scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario?.onActivity { activity ->
                val hostView = host.createView(activity, widgetId, manager.getAppWidgetInfo(widgetId))
                view = hostView
                val size = (160 * activity.resources.displayMetrics.density).toInt()
                activity.setContentView(
                    FrameLayout(activity).apply { addView(hostView, FrameLayout.LayoutParams(size, size)) },
                )
            }
            CounterWidgetState.refreshAll(app)
        }

        suspend fun query(
            purchased: Boolean = false,
            code: Int = BillingClient.BillingResponseCode.OK,
        ) {
            withContext(Dispatchers.Main) {
                client.purchases = if (purchased) listOf(purchase()) else emptyList()
                client.queryCode = code
                assertEquals(code == BillingClient.BillingResponseCode.OK, billing.queryPurchases())
            }
        }

        suspend fun assertRetry() {
            val outcomes =
                listOf(
                    R.string.widget_pro_required,
                    R.string.upgrade_to_pro,
                    R.string.retry,
                ).map(app::getString)
            await { texts().any { it in outcomes } }
            val labels = texts()
            assertFalse(
                "Unresolved ownership must not ask for a purchase: $labels",
                app.getString(R.string.widget_pro_required) in labels,
            )
            assertFalse(
                "Unresolved ownership must not ask for an upgrade: $labels",
                app.getString(R.string.upgrade_to_pro) in labels,
            )
            assertTrue("A neutral retry must be rendered: $labels", app.getString(R.string.retry) in labels)
        }

        suspend fun assertCountActionBlocked(expected: Int = 0) {
            sendAction(CounterWidgetActions.incrementIntent(app))
            assertEquals(expected, repository.getProject(projectId)?.count)
        }

        suspend fun sendAction(intent: Intent) {
            val completed = CompletableDeferred<Unit>()
            app.sendOrderedBroadcast(
                intent,
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        completed.complete(Unit)
                    }
                },
                Handler(Looper.getMainLooper()),
                0,
                null,
                null,
            )
            withTimeout(10_000) { completed.await() }
        }

        suspend fun awaitCount(count: Int) = await { repository.getProject(projectId)?.count == count }

        suspend fun awaitCounter(count: String) {
            awaitText(count)
            assertFalse(app.getString(R.string.widget_pro_required) in texts())
        }

        suspend fun awaitText(text: String) = await { text in texts() }

        fun click(
            label: String,
            description: Boolean = false,
        ) {
            scenario?.onActivity {
                var action =
                    descendants(checkNotNull(view)).first {
                        if (description) {
                            it.contentDescription?.toString() == label
                        } else {
                            it is TextView &&
                                it.text.toString() == label
                        }
                    }
                while (!action.isClickable && action.parent is View) action = action.parent as View
                assertTrue(action.performClick())
            }
        }

        fun texts(): List<String> {
            var result = emptyList<String>()
            scenario?.onActivity {
                result =
                    descendants(checkNotNull(view)).filterIsInstance<TextView>().map { it.text.toString() }
            }
            return result
        }

        suspend fun close() {
            if (widgetId != 0) host.deleteAppWidgetId(widgetId)
            host.stopListening()
            instrumentation.uiAutomation.dropShellPermissionIdentity()
            scenario?.close()
            if (projectId != 0L) repository.deleteProject(projectId)
            withContext(Dispatchers.Main) {
                client.deferQueries = false
                client.queryCode = BillingClient.BillingResponseCode.OK
                client.purchases = emptyList()
                billing.queryPurchases()
                ProPolicyTestControl.setTrialState(TrialState(hasStarted = true))
                billing.destroy()
            }
        }
    }

    private companion object {
        fun purchase() =
            Purchase("""{"productId":"knittools_pro","purchaseState":0,"acknowledged":true}""", "synthetic-signature")

        fun descendants(view: View): List<View> =
            listOf(view) +
                if (view is ViewGroup) {
                    (0 until view.childCount).flatMap {
                        descendants(
                            view.getChildAt(it),
                        )
                    }
                } else {
                    emptyList()
                }

        suspend fun await(condition: suspend () -> Boolean) = withTimeout(15_000) { while (!condition()) delay(50) }
    }
}
