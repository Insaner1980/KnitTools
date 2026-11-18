package com.finnvek.knittools.ui.navigation

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Host-driven emulator controls run while the real Activity and its state remain alive. */
internal fun awaitAdaptivePlatformTransition(step: Int) {
    check(InstrumentationRegistry.getArguments().getString("adaptivePlatformAllowed") == "true")
    val context = ApplicationProvider.getApplicationContext<Context>()
    check(context.packageName == "com.finnvek.knittools.activepolicytest")
    val directory = File(context.getExternalFilesDir(null), "adaptive-platform").apply { mkdirs() }
    val resume = File(directory, "resume-$step")
    resume.delete()
    File(directory, "ready").writeText(step.toString())
    val deadline = SystemClock.uptimeMillis() + 60_000
    while (!resume.exists() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
    check(resume.exists()) { "Host did not perform platform transition $step" }
    resume.delete()
}
