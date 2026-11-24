package com.finnvek.knittools

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.finnvek.knittools.analytics.PostHogAnalytics
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var analytics: PostHogAnalytics

    @Inject
    lateinit var preferencesManager: dagger.Lazy<PreferencesManager>

    @Inject
    lateinit var billingManager: dagger.Lazy<BillingManager>

    @Inject
    lateinit var proManager: dagger.Lazy<ProManager>

    @Inject
    lateinit var yarnCardRepository: dagger.Lazy<YarnCardRepository>

    @Inject
    lateinit var backupRepository: dagger.Lazy<BackupRepository>

    @Inject
    lateinit var savedPatternRepository: dagger.Lazy<SavedPatternRepository>

    @Inject
    lateinit var onboardingStateRepository: dagger.Lazy<OnboardingStateRepository>

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
        if (BuildConfig.POSTHOG_ENABLED && BuildConfig.POSTHOG_PROJECT_TOKEN.isNotBlank()) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(analytics)
            applicationScope.launch(ioDispatcher) {
                preferencesManager
                    .get()
                    .preferences
                    .map { it.usageAnalyticsEnabled to it.usageAnalyticsGeneration }
                    .distinctUntilChanged()
                    .collect { (enabled, generation) ->
                        analytics.setEnabled(enabled, generation)
                        analytics.initialConsentLoaded.complete(Unit)
                    }
            }
        }
        applicationScope.launch {
            preferencesManager.get().applyStoredAppLanguage()
        }
        initializeOnboarding()
        applicationScope.launch {
            try {
                backupRepository.get().recoverInterruptedOperations()
                yarnCardRepository.get().pruneUnreferencedPhotoFiles()
                // Perehdytysluonnoksen PDF on DataStoressa eikä tietokannassa, joten se säilytetään erikseen.
                val onboardingPdf = onboardingStateRepository.get().draft.first()?.pdfUri
                savedPatternRepository.get().pruneUnreferencedStagedPdfs(keepUris = setOfNotNull(onboardingPdf))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Lankakuvien best-effort-siivous ei saa kaataa sovellusta.
                // Sama koskee hylättyjen jaettujen PDF-kopioiden siivousta.
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

    private fun initializeOnboarding() {
        applicationScope.launch(ioDispatcher) {
            try {
                onboardingStateRepository.get().initializeIfNeeded(
                    today = LocalDate.now().toEpochDay(),
                )
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                // Opastuksen alustuksen virhe ei estä sovelluksen käyttöä.
                Log.w("App", "Onboarding initialization failed")
            }
        }
    }

    private fun observeWidgetProState() {
        applicationScope.launch {
            val manager = proManager.get()
            manager
                .featureAccessFlow(ProFeature.WIDGET)
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
