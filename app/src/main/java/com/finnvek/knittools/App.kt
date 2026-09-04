package com.finnvek.knittools

import android.app.Application
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
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
    lateinit var projectCounterRepository: dagger.Lazy<ProjectCounterRepository>

    @Inject
    lateinit var patternDocumentStorage: dagger.Lazy<PatternDocumentStorage>

    @Inject
    lateinit var database: dagger.Lazy<KnitToolsDatabase>

    @Inject
    lateinit var transactionRunner: dagger.Lazy<DatabaseTransactionRunner>

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
            try {
                yarnCardRepository.get().pruneUnreferencedPhotoFiles()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Lankakuvien best-effort-siivous ei saa kaataa sovellusta.
            }
        }
        applicationScope.launch(ioDispatcher) {
            try {
                patternDocumentStorage.get().pruneStaleCaptureImages(this@App)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Kaavakuvien best-effort-siivous ei saa kaataa sovellusta.
            }
        }
        DemoDataSeeder.seedIfNeeded(
            applicationScope,
            ioDispatcher,
            database,
            transactionRunner,
            projectCounterRepository,
            yarnCardRepository,
        )
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
                .collect {
                    try {
                        CounterWidgetState.refreshAll(this@App)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Widgetin päivitys ei saa kaataa muuta sovellusta.
                    }
                }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        billingManager.get().destroy()
    }
}
