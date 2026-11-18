package com.finnvek.knittools.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun TestScope.assertPurchaseBlocked(
    manager: BillingManager,
    client: BillingClient,
    activity: Activity,
    expectedMessage: BillingUserMessage,
    displayedOffer: SelectedOneTimeOffer? = manager.selectedOffer.value,
) {
    val message = async(UnconfinedTestDispatcher(testScheduler)) { manager.purchaseMessages.first() }
    manager.launchPurchaseFlow(activity, displayedOffer)
    verify(exactly = 0) { client.launchBillingFlow(any(), any()) }
    assertEquals(expectedMessage, message.await())
}
