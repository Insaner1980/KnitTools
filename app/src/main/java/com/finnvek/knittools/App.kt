package com.finnvek.knittools

import android.app.Application
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.pro.ProManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var billingManager: dagger.Lazy<BillingManager>

    @Inject
    lateinit var proManager: dagger.Lazy<ProManager>

    override fun onCreate() {
        super.onCreate()
        billingManager.get().initialize()
        proManager.get().initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.get().destroy()
    }
}
