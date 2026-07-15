package com.finnvek.knittools

import android.app.Application
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var preferencesManager: dagger.Lazy<PreferencesManager>

    @Inject
    lateinit var billingManager: dagger.Lazy<BillingManager>

    @Inject
    lateinit var proManager: dagger.Lazy<ProManager>

    @Inject
    lateinit var yarnCardRepository: dagger.Lazy<YarnCardRepository>

    @Inject
    lateinit var patternDocumentStorage: dagger.Lazy<PatternDocumentStorage>

    @Inject
    lateinit var database: dagger.Lazy<KnitToolsDatabase>

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        SentryInit.init(this)
        applicationScope.launch {
            preferencesManager.get().applyStoredAppLanguage()
        }
        applicationScope.launch {
            yarnCardRepository.get().pruneUnreferencedPhotoFiles()
        }
        applicationScope.launch(ioDispatcher) {
            patternDocumentStorage.get().pruneStaleCaptureImages(this@App)
        }
        DemoDataSeeder.seedIfNeeded(applicationScope, ioDispatcher, database)
        billingManager.get().initialize()
        proManager.get().initialize()
        observeWidgetProState()
    }

    private fun observeWidgetProState() {
        applicationScope.launch {
            val manager = proManager.get()
            manager.initialStateReady.first { it }
            manager
                .hasFeatureFlow(ProFeature.WIDGET)
                .collect { CounterWidgetState.refreshAll(this@App) }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        billingManager.get().destroy()
    }
}
