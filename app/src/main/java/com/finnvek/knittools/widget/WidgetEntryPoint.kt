package com.finnvek.knittools.widget

import com.finnvek.knittools.analytics.PostHogAnalytics
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
@Suppress("kotlin:S6517") // Hilt @EntryPoint vaatii interfacen
interface WidgetEntryPoint {
    fun analytics(): PostHogAnalytics

    @ApplicationScope
    fun applicationScope(): CoroutineScope

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher

    fun counterRepository(): CounterRepository

    fun proManager(): ProManager

    fun billingManager(): BillingManager
}
