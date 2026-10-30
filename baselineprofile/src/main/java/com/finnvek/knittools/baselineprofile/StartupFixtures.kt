package com.finnvek.knittools.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import java.io.File

/** Shared UI fixture preparation, always outside collection and measurement. */
internal fun SmokeDevice.prepareStartupFixture(populated: Boolean) {
    val args = InstrumentationRegistry.getArguments()
    val fixture = if (populated) "populated" else "empty"
    if (args.getString("startupFixtureResetAllowed") == "true") {
        // Reset is guarded by SmokeDevice: explicit consent, emulator and isolated test package only.
        reset()
        if (populated) {
            createProject("Startup scarf")
            repeat(4) { tapDesc("Add row") }
            projects()
            startTrial()
            createProject("Startup socks")
            repeat(8) { tapDesc("Add row") }
            projects()
            createProject("Startup cardigan")
            repeat(12) { tapDesc("Add row") }
            tapDesc("Project actions")
            scrollTo("Start work session").click()
            assertTrue(device.wait(Until.gone(By.desc("Close sheet")), 30_000))
            device.waitForIdle()
            projects()
        }
    } else {
        // Physical devices require an already prepared synthetic fixture; never clear their data.
        check(args.getString("startupPreparedFixture") == fixture) { "Prepare the isolated $fixture fixture first" }
        launch()
    }
}

/** Read-only inspection of this isolated emulator fixture, outside every measured block. */
internal fun SmokeDevice.startupSessionIdentity(): String {
    check(shell("getprop ro.kernel.qemu").trim() == "1")
    val query = File(context.cacheDir, "startup-session.sql")
    query.writeText(
        "SELECT sessionToken, projectId, startedAtWallMillis, startRow FROM active_sessions; " +
            "SELECT count(*) FROM sessions;",
    )
    return try {
        // UiAutomation splits command arguments without interpreting shell quotes.
        shell(
            "su 0 sqlite3 -readonly -init ${query.absolutePath} " +
                "/data/user/0/${SmokeDevice.PACKAGE}/databases/knittools.db .quit",
        ).trim().also { check(it.isNotBlank() && !it.contains("Error", ignoreCase = true)) }
    } finally {
        query.delete()
    }
}
