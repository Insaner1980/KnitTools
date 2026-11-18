package com.finnvek.knittools.ui

import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.MainActivity
import org.junit.rules.ExternalResource

class TestContentSplashRule : ExternalResource() {
    private val callback =
        ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity && stage == Stage.CREATED) {
                MainActivity::class.java
                    .getDeclaredField("startupThemeLoaded")
                    .apply { isAccessible = true }
                    .setBoolean(activity, true)
            }
        }

    override fun before() = ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)

    override fun after() = ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback)
}
