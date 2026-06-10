package com.finnvek.knittools

import android.content.Context
import io.sentry.android.core.SentryAndroid

object SentryInit {
    fun init(context: Context) {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return

        SentryAndroid.init(context) { options ->
            options.dsn = dsn
            options.environment = "debug"
            options.isDebug = true
            options.tracesSampleRate = 0.0
            options.profilesSampleRate = 0.0
            options.isEnableAutoSessionTracking = false
            options.isEnableUserInteractionBreadcrumbs = false
            options.isEnableAutoActivityLifecycleTracing = false
            options.isEnableActivityLifecycleTracingAutoFinish = false
            options.isEnableFramesTracking = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isEnableNdk = false
            options.isEnablePerformanceV2 = false
            options.isEnableAutoTraceIdGeneration = false
            options.maxBreadcrumbs = 0
            options.enableAllAutoBreadcrumbs(false)
        }
    }
}
