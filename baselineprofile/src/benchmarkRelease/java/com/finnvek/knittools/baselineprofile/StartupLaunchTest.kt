package com.finnvek.knittools.baselineprofile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cold-process configuration/session check; intentionally not part of the CI smoke selection. */
@RunWith(AndroidJUnit4::class)
class StartupLaunchTest {
    @get:Rule
    internal val ui = SmokeDevice()

    @Test
    fun coldFinnishDarkLauncherKeepsTheExistingSession() =
        with(ui) {
            reset()
            createProject("Startup cardigan")
            tapDesc("Add row")
            tapDesc("Project actions")
            scrollTo("Start work session").click()
            assertTrue(device.wait(Until.gone(By.desc("Close sheet")), 15_000))
            device.waitForIdle()
            projects()
            val before = startupSessionIdentity()
            assertTrue(before.lineSequence().count() == 2)
            tap("Settings")
            tap("Dark")
            tap("App language")
            tap("Suomi")
            text("Asetukset")
            shell("am force-stop ${SmokeDevice.PACKAGE}")
            launch()
            assertTrue(device.wait(Until.hasObject(By.res("startup_ready")), 30_000))
            text("Projektit")
            text("Startup cardigan")
            capture("S05-S06-cold-finnish-dark")
            assertEquals(before, startupSessionIdentity())
        }
}
