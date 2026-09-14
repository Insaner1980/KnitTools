package com.finnvek.knittools.billing

import android.content.Context
import androidx.lifecycle.ViewModelStore
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.ui.screens.pro.ProUpgradeViewModel
import com.finnvek.knittools.ui.screens.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class BillingRecoveryTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val client = mockk<BillingClient>(relaxed = true)
    private val builder = mockk<BillingClient.Builder>(relaxed = true)
    private lateinit var connection: BillingClientStateListener
    private lateinit var billing: BillingManager
    private lateinit var pro: ProManager
    private lateinit var viewModel: ProUpgradeViewModel
    private val viewModels = ViewModelStore()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(BillingClient::class)
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        every { BillingClient.newBuilder(any()) } returns builder
        every { builder.setListener(any()) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.enableAutoServiceReconnection() } returns builder
        every { builder.build() } returns client
        every { client.startConnection(any()) } answers {
            connection = firstArg()
        }
        every { client.isReady } returns true
        coEvery { client.queryPurchasesAsync(any()) } returns purchases(BillingClient.BillingResponseCode.ERROR)
        coEvery { client.queryProductDetails(any()) } returns
            ProductDetailsResult(result(BillingClient.BillingResponseCode.OK), emptyList())
        billing = BillingManager(mockk<Context>(relaxed = true))
        val trial = mockk<TrialManager>(relaxed = true)
        every { trial.trialState } returns MutableStateFlow(TrialState(hasStarted = true))
        pro = ProManager(trial, billing)
        billing.initialize()
        pro.initialize()
        viewModel = ProUpgradeViewModel(pro, billing)
        viewModels.put("pro", viewModel)
    }

    @After
    fun tearDown() {
        billing.destroy()
        viewModels.clear()
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial failure then public restore resolves real Pro presentation`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            assertFalse(billing.purchaseStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertFalse(viewModel.proStateReady.value)
            assertTrue(viewModel.purchaseQueryFailed.value)
            val widgetReadiness = async { pro.initialStateReady.first { it } }

            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))
            assertEquals(RestorePurchasesResult.RESTORED, billing.restorePurchasesWithResult())

            assertTrue("Successful restore must resolve purchase readiness", billing.purchaseStateReady.value)
            assertTrue(billing.isProPurchased.value)
            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
            assertFalse(viewModel.purchaseQueryFailed.value)
            assertTrue(widgetReadiness.await())
        }

    @Test
    fun `empty restore resolves expired trial presentation without granting Pro`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            coEvery { client.queryPurchasesAsync(any()) } returns purchases(BillingClient.BillingResponseCode.OK)

            viewModel.restorePurchases()

            assertTrue(viewModel.proStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertFalse(viewModel.proState.value.isPro)
            assertEquals(ProStatus.TRIAL_EXPIRED, viewModel.proState.value.status)
            assertEquals(com.finnvek.knittools.R.string.no_purchases_found, viewModel.statusMessageRes.value)
            assertFalse(viewModel.isRestoring.value)
            assertFalse(viewModel.purchaseQueryFailed.value)
            assertEquals(null, viewModel.selectedOffer.value)
            assertTrue(viewModel.productStatus.value is BillingProductStatus.Unavailable)
        }

    @Test
    fun `failed recovery releases busy guard and permits a successful retry`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            val pending = CompletableDeferred<PurchasesResult>()
            coEvery { client.queryPurchasesAsync(any()) } coAnswers { pending.await() }
            viewModel.restorePurchases()
            viewModel.restorePurchases()
            assertTrue(viewModel.isRestoring.value)
            coVerify(exactly = 2) { client.queryPurchasesAsync(any()) }
            pending.complete(purchases(BillingClient.BillingResponseCode.ERROR))
            assertFalse(viewModel.isRestoring.value)
            assertTrue(viewModel.purchaseQueryFailed.value)
            assertFalse(viewModel.proStateReady.value)
            assertFalse(billing.isProPurchased.value)

            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))
            viewModel.restorePurchases()
            assertFalse(viewModel.isRestoring.value)
            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
        }

    @Test
    fun `failed refresh preserves known purchased entitlement and readiness`() =
        runTest(dispatcher) {
            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            coEvery { client.queryPurchasesAsync(any()) } returns purchases(BillingClient.BillingResponseCode.ERROR)

            assertEquals(RestorePurchasesResult.FAILED, billing.restorePurchasesWithResult())

            assertTrue(billing.isProPurchased.value)
            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
            assertFalse(viewModel.purchaseQueryFailed.value)
        }

    @Test
    fun `pending and unrelated purchases resolve without granting entitlement`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            val pending = purchased()
            every { pending.purchaseState } returns Purchase.PurchaseState.PENDING
            val unrelated = purchased()
            every { unrelated.products } returns listOf("another_product")
            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(pending, unrelated))

            assertEquals(RestorePurchasesResult.NOT_FOUND, billing.restorePurchasesWithResult())

            assertTrue(viewModel.proStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertFalse(viewModel.proState.value.isPro)
            verify(exactly = 0) { client.acknowledgePurchase(any(), any()) }
        }

    @Test
    fun `late initial failure cannot undo successful overlapping restore`() =
        runTest(dispatcher) {
            val initial = CompletableDeferred<PurchasesResult>()
            coEvery { client.queryPurchasesAsync(any()) } coAnswers { initial.await() }
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))
            viewModel.restorePurchases()
            initial.complete(purchases(BillingClient.BillingResponseCode.ERROR))

            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
            assertFalse(viewModel.purchaseQueryFailed.value)
        }

    @Test
    fun `exhausted setup retries expose recovery and restore reconnects`() =
        runTest(dispatcher) {
            every { client.isReady } returns false
            every { client.startConnection(any()) } answers {
                val listener = firstArg<BillingClientStateListener>()
                listener.onBillingSetupFinished(result(BillingClient.BillingResponseCode.ERROR))
            }
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.ERROR))
            advanceUntilIdle()
            assertTrue(viewModel.purchaseQueryFailed.value)
            assertFalse(viewModel.proStateReady.value)

            viewModel.restorePurchases()
            advanceUntilIdle()
            assertFalse(viewModel.isRestoring.value)
            assertTrue(viewModel.purchaseQueryFailed.value)
            assertFalse(billing.purchaseStateReady.value)

            every { client.startConnection(any()) } answers {
                every { client.isReady } returns true
                val listener = firstArg<BillingClientStateListener>()
                listener.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            }
            coEvery { client.queryPurchasesAsync(any()) } returns purchases(BillingClient.BillingResponseCode.OK)
            viewModel.restorePurchases()
            assertTrue(viewModel.proStateReady.value)
            assertFalse(viewModel.isRestoring.value)
            assertFalse(viewModel.purchaseQueryFailed.value)
        }

    @Test
    fun `settings restore propagates to existing Pro view model`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            val preferences = mockk<PreferencesManager>()
            every { preferences.preferences } returns MutableStateFlow(AppPreferences())
            val settings = SettingsViewModel(preferences, billing, pro)
            viewModels.put("settings", settings)
            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))

            settings.restorePurchases()

            assertFalse(settings.isRestoring.value)
            assertTrue(settings.proStateReady.value)
            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
        }

    // CPD-OFF: Keep callback ordering and virtual-time assertions local to each recovery race scenario.
    @Test
    fun `cancelled restore releases view model busy state without resolving purchase`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            val pending = CompletableDeferred<PurchasesResult>()
            coEvery { client.queryPurchasesAsync(any()) } coAnswers { pending.await() }
            viewModel.restorePurchases()
            // CPD-ON
            assertTrue(viewModel.isRestoring.value)

            viewModels.clear()

            assertFalse(viewModel.isRestoring.value)
            assertFalse(billing.purchaseStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertTrue(billing.purchaseQueryFailed.value)
        }

    @Test
    fun `old restore and setup callbacks cannot write into a new lifecycle`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            val oldConnection = connection
            val pending = CompletableDeferred<PurchasesResult>()
            coEvery { client.queryPurchasesAsync(any()) } coAnswers { pending.await() }
            val restore = async { billing.restorePurchasesWithResult() }
            billing.destroy()
            val nextClient = mockk<BillingClient>(relaxed = true)
            every { builder.build() } returns nextClient
            billing.initialize()
            oldConnection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            oldConnection.onBillingServiceDisconnected()
            pending.complete(purchases(BillingClient.BillingResponseCode.OK, listOf(purchased())))

            assertEquals(RestorePurchasesResult.FAILED, restore.await())
            assertFalse(billing.purchaseStateReady.value)
            assertFalse(billing.isProPurchased.value)
            assertFalse(billing.purchaseQueryFailed.value)
            assertEquals(BillingProductStatus.Loading, billing.productStatus.value)
        }

    @Test
    fun `already owned recovery resolves initial readiness`() =
        runTest(dispatcher) {
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
            coEvery { client.queryPurchasesAsync(any()) } returns
                purchases(BillingClient.BillingResponseCode.OK, listOf(purchased()))

            billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED), null)

            assertTrue(viewModel.proStateReady.value)
            assertEquals(ProStatus.PRO_PURCHASED, viewModel.proState.value.status)
            assertFalse(viewModel.purchaseQueryFailed.value)
        }

    @Test
    fun `destroy cancels initial query and its followup product query`() =
        runTest(dispatcher) {
            val pending = CompletableDeferred<PurchasesResult>()
            var cancelled = false
            coEvery { client.queryPurchasesAsync(any()) } coAnswers {
                try {
                    pending.await()
                } finally {
                    cancelled = true
                }
            }
            connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))

            billing.destroy()

            assertTrue(cancelled)
            assertFalse(billing.purchaseStateReady.value)
            coVerify(exactly = 0) { client.queryProductDetails(any()) }
        }

    @Test
    fun `stale transient acknowledgement cannot retry or release current in flight purchase`() =
        runTest(dispatcher) {
            val oldCallbacks = startAcknowledgementLifecycle(client)
            val nextClient = mockk<BillingClient>(relaxed = true)
            val currentCallbacks = startAcknowledgementLifecycle(nextClient)

            oldCallbacks.single().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.NETWORK_ERROR))
            assertTrue(billing.purchaseStateReady.value)
            assertTrue(billing.isProPurchased.value)
            assertFalse(billing.purchaseQueryFailed.value)
            advanceTimeBy(5_001)
            runCurrent()

            coVerify(exactly = 1) { nextClient.queryPurchasesAsync(any()) }
            coVerify(exactly = 1) { nextClient.queryProductDetails(any()) }
            verify(exactly = 1) { nextClient.startConnection(any()) }
            assertEquals(RestorePurchasesResult.RESTORED, billing.restorePurchasesWithResult())
            assertEquals(1, currentCallbacks.size)
            verify(exactly = 1) { client.acknowledgePurchase(any(), any()) }

            currentCallbacks.single().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.OK))
            billing.restorePurchasesWithResult()
            advanceUntilIdle()
            assertEquals(1, currentCallbacks.size)
            coVerify(exactly = 3) { nextClient.queryPurchasesAsync(any()) }
        }

    @Test
    fun `stale permanent acknowledgement cannot release current in flight purchase`() =
        runTest(dispatcher) {
            val oldCallbacks = startAcknowledgementLifecycle(client)
            val currentCallbacks = startAcknowledgementLifecycle(mockk(relaxed = true))

            oldCallbacks
                .single()
                .onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
            billing.restorePurchasesWithResult()

            assertEquals(1, currentCallbacks.size)
            assertTrue(billing.purchaseStateReady.value)
            assertTrue(billing.isProPurchased.value)
            assertFalse(billing.purchaseQueryFailed.value)
        }

    @Test
    fun `stale successful acknowledgement cannot mark current token completed`() =
        runTest(dispatcher) {
            val oldCallbacks = startAcknowledgementLifecycle(client)
            val currentCallbacks = startAcknowledgementLifecycle(mockk(relaxed = true))

            oldCallbacks.single().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.OK))
            currentCallbacks
                .single()
                .onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
            billing.restorePurchasesWithResult()

            assertEquals(2, currentCallbacks.size)
            currentCallbacks.last().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.OK))
            billing.restorePurchasesWithResult()
            assertEquals(2, currentCallbacks.size)
            assertTrue(billing.purchaseStateReady.value)
            assertTrue(billing.isProPurchased.value)
            assertFalse(billing.purchaseQueryFailed.value)
        }

    @Test
    fun `destroy cancels scheduled acknowledgement retry`() =
        runTest(dispatcher) {
            val callbacks = startAcknowledgementLifecycle(client)
            callbacks.single().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.NETWORK_ERROR))
            billing.destroy()
            val nextClient = mockk<BillingClient>(relaxed = true)
            val nextCallbacks = startAcknowledgementLifecycle(nextClient)
            advanceTimeBy(5_001)
            runCurrent()

            assertEquals(1, nextCallbacks.size)
            coVerify(exactly = 1) { client.queryPurchasesAsync(any()) }
            coVerify(exactly = 1) { nextClient.queryPurchasesAsync(any()) }
        }

    @Test
    fun `completed acknowledgement belongs to one lifecycle`() =
        runTest(dispatcher) {
            repeat(3) {
                val callbacks = startAcknowledgementLifecycle(mockk(relaxed = true))
                repeat(3) { retry ->
                    assertEquals(retry + 1, callbacks.size)
                    callbacks
                        .last()
                        .onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.NETWORK_ERROR))
                    advanceTimeBy(5_001)
                    runCurrent()
                }
                assertEquals(4, callbacks.size)
                callbacks.last().onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.OK))
                billing.restorePurchasesWithResult()
                assertEquals(4, callbacks.size)
            }
        }

    @Test
    fun `exhausted acknowledgement retry budget resets in the next lifecycle`() =
        runTest(dispatcher) {
            repeat(2) {
                val callbacks = startAcknowledgementLifecycle(mockk(relaxed = true))
                repeat(4) { attempt ->
                    assertEquals(attempt + 1, callbacks.size)
                    callbacks
                        .last()
                        .onAcknowledgePurchaseResponse(result(BillingClient.BillingResponseCode.NETWORK_ERROR))
                    advanceTimeBy(5_001)
                    runCurrent()
                }
                assertEquals(4, callbacks.size)
            }
        }

    @Test
    fun `worker success acknowledgement is confined across lifecycle replacement`() =
        verifyWorkerAcknowledgement(BillingClient.BillingResponseCode.OK)

    @Test
    fun `worker transient acknowledgement is confined across lifecycle replacement`() =
        verifyWorkerAcknowledgement(BillingClient.BillingResponseCode.NETWORK_ERROR)

    @Test
    fun `worker permanent acknowledgement is confined across lifecycle replacement`() =
        verifyWorkerAcknowledgement(BillingClient.BillingResponseCode.DEVELOPER_ERROR)

    private fun verifyWorkerAcknowledgement(responseCode: Int) =
        runTest(dispatcher) {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val mainThread = Thread.currentThread()
            val responseThread = AtomicReference<Thread>()
            val response = mockk<BillingResult>()
            every { response.responseCode } answers {
                responseThread.set(Thread.currentThread())
                responseCode
            }
            val worker = Executors.newSingleThreadExecutor()
            try {
                val oldCallbacks = startAcknowledgementLifecycle(client)
                runCurrent()
                val oldCallback = oldCallbacks.single()
                worker.submit { oldCallback.onAcknowledgePurchaseResponse(response) }.get(5, TimeUnit.SECONDS)
                assertEquals("Worker must not process acknowledgement state", null, responseThread.get())

                val nextClient = mockk<BillingClient>(relaxed = true)
                val currentCallbacks = startAcknowledgementLifecycle(nextClient)
                runCurrent()
                worker.submit { oldCallback.onAcknowledgePurchaseResponse(response) }.get(5, TimeUnit.SECONDS)
                runCurrent()
                // CPD-OFF: Keep callback ordering and virtual-time assertions local to each recovery race scenario.
                assertEquals("Stale response must be rejected before processing", null, responseThread.get())
                advanceTimeBy(5_001)
                runCurrent()
                coVerify(exactly = 1) { nextClient.queryPurchasesAsync(any()) }
                coVerify(exactly = 1) { nextClient.queryProductDetails(any()) }
                verify(exactly = 1) { nextClient.startConnection(any()) }
                billing.restorePurchasesWithResult()
                // CPD-ON
                assertEquals(1, currentCallbacks.size)

                val currentCallback = currentCallbacks.single()
                worker.submit { currentCallback.onAcknowledgePurchaseResponse(response) }.get(5, TimeUnit.SECONDS)
                assertEquals(null, responseThread.get())
                runCurrent()
                assertEquals(mainThread, responseThread.get())
                advanceTimeBy(5_001)
                runCurrent()
                billing.restorePurchasesWithResult()
                assertEquals(if (responseCode == BillingClient.BillingResponseCode.OK) 1 else 2, currentCallbacks.size)
                assertTrue(billing.purchaseStateReady.value)
                assertTrue(billing.isProPurchased.value)
                assertFalse(billing.purchaseQueryFailed.value)
            } finally {
                worker.shutdownNow()
                assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS))
            }
        }

    @Test
    fun `worker acknowledgement keeps in flight guard until Main handles result`() =
        runTest(dispatcher) {
            val callbacks = startAcknowledgementLifecycle(client)
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))

            deliverOnWorker(callbacks.single(), result(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
            billing.restorePurchasesWithResult()
            assertEquals("Worker must not release the in-flight guard", 1, callbacks.size)

            runCurrent()
            billing.restorePurchasesWithResult()
            assertEquals(2, callbacks.size)
            deliverOnWorker(callbacks.last(), result(BillingClient.BillingResponseCode.OK))
            runCurrent()
            billing.restorePurchasesWithResult()
            assertEquals(2, callbacks.size)
        }

    @Test
    fun `queued worker success cannot complete replacement lifecycle token`() =
        verifyQueuedWorkerCallback(BillingClient.BillingResponseCode.OK)

    @Test
    fun `queued worker transient failure cannot retry replacement lifecycle`() =
        verifyQueuedWorkerCallback(BillingClient.BillingResponseCode.NETWORK_ERROR)

    @Test
    fun `queued worker permanent failure cannot release replacement lifecycle guard`() =
        verifyQueuedWorkerCallback(BillingClient.BillingResponseCode.DEVELOPER_ERROR)

    private fun verifyQueuedWorkerCallback(responseCode: Int) =
        runTest(dispatcher) {
            val oldCallbacks = startAcknowledgementLifecycle(client)
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val oldResult = mockk<BillingResult>()
            every { oldResult.responseCode } returns responseCode

            deliverOnWorker(oldCallbacks.single(), oldResult)
            verify(exactly = 0) { oldResult.responseCode }

            val nextClient = mockk<BillingClient>(relaxed = true)
            val currentCallbacks = startAcknowledgementLifecycle(nextClient)
            billing.restorePurchasesWithResult()
            assertEquals(1, currentCallbacks.size)
            runCurrent()

            deliverOnWorker(oldCallbacks.single(), oldResult)
            runCurrent()
            advanceTimeBy(5_001)
            runCurrent()
            verify(exactly = 0) { oldResult.responseCode }
            coVerify(exactly = 2) { nextClient.queryPurchasesAsync(any()) }
            coVerify(exactly = 1) { nextClient.queryProductDetails(any()) }
            billing.restorePurchasesWithResult()
            assertEquals(1, currentCallbacks.size)

            deliverOnWorker(currentCallbacks.single(), result(BillingClient.BillingResponseCode.DEVELOPER_ERROR))
            runCurrent()
            billing.restorePurchasesWithResult()
            assertEquals("Old success must not mark the replacement token completed", 2, currentCallbacks.size)
            deliverOnWorker(currentCallbacks.last(), result(BillingClient.BillingResponseCode.OK))
            runCurrent()
            billing.restorePurchasesWithResult()
            assertEquals(2, currentCallbacks.size)
            assertTrue(billing.purchaseStateReady.value)
            assertTrue(billing.isProPurchased.value)
            assertFalse(billing.purchaseQueryFailed.value)
        }

    @Test
    fun `current worker transient failures retain five second delay and three retry budget`() =
        runTest(dispatcher) {
            val callbacks = startAcknowledgementLifecycle(client)
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            repeat(4) { attempt ->
                deliverOnWorker(callbacks.last(), result(BillingClient.BillingResponseCode.NETWORK_ERROR))
                runCurrent()
                advanceTimeBy(4_999)
                runCurrent()
                assertEquals(attempt + 1, callbacks.size)
                advanceTimeBy(1)
                runCurrent()
                assertEquals(minOf(attempt + 2, 4), callbacks.size)
            }
            advanceUntilIdle()
            coVerify(exactly = 4) { client.queryPurchasesAsync(any()) }
            assertEquals(4, callbacks.size)
        }

    private fun deliverOnWorker(
        callback: AcknowledgePurchaseResponseListener,
        result: BillingResult,
    ) {
        val delivery = FutureTask { callback.onAcknowledgePurchaseResponse(result) }
        val worker = Thread(delivery, "Fake-PlayBillingLibrary-worker")
        worker.start()
        try {
            delivery.get(5, TimeUnit.SECONDS)
        } finally {
            worker.join(5_000)
            assertFalse("Callback worker must finish", worker.isAlive)
        }
    }

    private fun startAcknowledgementLifecycle(target: BillingClient): MutableList<AcknowledgePurchaseResponseListener> {
        billing.destroy()
        val callbacks = mutableListOf<AcknowledgePurchaseResponseListener>()
        val purchase = purchased()
        every { purchase.isAcknowledged } returns false
        every { purchase.purchaseToken } returns "test-token"
        every { builder.build() } returns target
        every { target.isReady } returns true
        every { target.startConnection(any()) } answers { connection = firstArg() }
        every { target.acknowledgePurchase(any(), any()) } answers {
            callbacks.add(secondArg())
        }
        coEvery { target.queryPurchasesAsync(any()) } returns
            purchases(BillingClient.BillingResponseCode.OK, listOf(purchase))
        coEvery { target.queryProductDetails(any()) } returns
            ProductDetailsResult(result(BillingClient.BillingResponseCode.OK), emptyList())
        billing.initialize()
        connection.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
        return callbacks
    }

    private fun purchased(): Purchase =
        mockk {
            every { products } returns listOf(BillingManager.PRODUCT_ID)
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns true
        }

    private fun result(code: Int): BillingResult = BillingResult.newBuilder().setResponseCode(code).build()

    private fun purchases(
        code: Int,
        owned: List<Purchase> = emptyList(),
    ): PurchasesResult = PurchasesResult(result(code), owned)
}
