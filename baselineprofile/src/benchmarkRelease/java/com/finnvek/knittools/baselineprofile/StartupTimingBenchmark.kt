package com.finnvek.knittools.baselineprofile

import android.content.pm.ApplicationInfo
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Timing only. Profile generation and the CI smoke selection remain separate. */
@RunWith(AndroidJUnit4::class)
class StartupTimingBenchmark {
    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun emptyNoProfile() = startup(populated = false, compilation = CompilationMode.None())

    @Test
    fun populatedNoProfile() = startup(populated = true, compilation = CompilationMode.None())

    @Test
    fun emptyRequiredProfile() = startup(populated = false, compilation = requiredProfile())

    @Test
    fun populatedRequiredProfile() = startup(populated = true, compilation = requiredProfile())

    private fun requiredProfile() =
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 0)

    private fun startup(
        populated: Boolean,
        compilation: CompilationMode,
    ) {
        val ui = SmokeDevice()
        org.junit.Assume.assumeTrue(
            "Compilation reset requires API 34+ or an already rooted shell to preserve fixture data",
            android.os.Build.VERSION.SDK_INT >= 34 || ui.shell("id -u").trim() == "0",
        )
        val args = InstrumentationRegistry.getArguments()
        val fixture = if (populated) "populated" else "empty"
        val info = ui.context.packageManager.getApplicationInfo(SmokeDevice.PACKAGE, 0)
        assertEquals(0, info.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        assertTrue(info.flags and ApplicationInfo.FLAG_TEST_ONLY != 0)
        assertTrue(info.isProfileableByShell)
        ui.prepareStartupFixture(populated)
        val readyText = if (populated) "Startup cardigan" else ui.string("no_active_projects")
        ui.text(readyText)
        val beforeSession = if (ui.shell("getprop ro.kernel.qemu").trim() == "1") ui.startupSessionIdentity() else null
        if (beforeSession != null) {
            assertEquals(if (populated) 2 else 1, beforeSession.lineSequence().count())
            assertEquals("0", beforeSession.lineSequence().last())
        }
        if (args.getString("startupFixtureResetAllowed") == "true") {
            // A pinned fixture widget can restart the process while Macrobenchmark is preparing a cold start.
            ui.device.pressHome()
            ui.removeCounterWidget()
        }
        benchmark.measureRepeated(
            packageName = SmokeDevice.PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            iterations = args.getString("startupIterations")?.toInt() ?: 10,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.res("startup_ready")), READY_TIMEOUT_MILLIS))
            check(device.hasObject(By.text(readyText)))
        }
        if (beforeSession != null) {
            assertEquals(
                "Startup must preserve active and completed sessions",
                beforeSession,
                ui.startupSessionIdentity(),
            )
            File(ui.context.getExternalFilesDir(null), "startup-$fixture-session.txt").writeText(beforeSession)
        }
    }

    private companion object {
        const val READY_TIMEOUT_MILLIS = 30_000L
    }
}
