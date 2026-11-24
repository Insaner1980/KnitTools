package com.finnvek.knittools.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.di.AppDispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Lint tulkitsee nimen perusteella widgetin läpinäkyvän reitityksen käynnistysnäkymäksi.

/** The launcher can reuse its click action while each tap gets a new single-use launch ID. */
@SuppressLint("CustomSplashScreen")
internal class WidgetCounterLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId =
            intent.data
                ?.lastPathSegment
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        lifecycleScope.launch {
            try {
                val launch =
                    withContext(AppDispatchers.IO) {
                        MainActivity.createCounterLaunchIntent(applicationContext, projectId)
                    }
                startActivity(launch)
            } finally {
                finish()
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            projectId: Long?,
        ): Intent =
            Intent(context, WidgetCounterLaunchActivity::class.java)
                .setData("knittools://widget/open/${projectId ?: 0L}".toUri())
    }
}
