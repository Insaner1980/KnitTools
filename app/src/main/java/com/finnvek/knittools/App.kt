package com.finnvek.knittools

import android.app.Application
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.pro.ProManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var preferencesManager: dagger.Lazy<PreferencesManager>

    @Inject
    lateinit var billingManager: dagger.Lazy<BillingManager>

    @Inject
    lateinit var proManager: dagger.Lazy<ProManager>

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            preferencesManager.get().applyStoredAppLanguage()
        }
        billingManager.get().initialize()
        proManager.get().initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.get().destroy()
    }
}
