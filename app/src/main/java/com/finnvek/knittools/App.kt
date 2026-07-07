package com.finnvek.knittools

import android.app.Application
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        SentryInit.init(this)
        applicationScope.launch {
            preferencesManager.get().applyStoredAppLanguage()
        }
        applicationScope.launch {
            yarnCardRepository.get().pruneUnreferencedPhotoFiles()
        }
        applicationScope.launch {
            patternDocumentStorage.get().pruneStaleCaptureImages(this@App)
        }
        billingManager.get().initialize()
        proManager.get().initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        billingManager.get().destroy()
    }
}
