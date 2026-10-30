package com.finnvek.knittools.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Collect on the isolated nonMinifiedRelease target; benchmark its optimized sibling separately. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @get:Rule
    internal val ui = SmokeDevice()

    @Before
    fun requireDisposableFixture() {
        check(InstrumentationRegistry.getArguments().getString("startupFixtureResetAllowed") == "true") {
            "Profile generation requires explicit disposable-emulator fixture consent"
        }
    }

    @Test
    fun emptyStartup() = startup(populated = false)

    @Test
    fun populatedStartup() = startup(populated = true)

    private fun startup(populated: Boolean) {
        ui.prepareStartupFixture(populated)
        val readyText = if (populated) "Startup cardigan" else ui.string("no_active_projects")
        val beforeSession = ui.startupSessionIdentity()
        ui.device.pressHome()
        ui.removeCounterWidget()
        baselineProfileRule.collect(
            packageName = SmokeDevice.PACKAGE,
            includeInStartupProfile = true,
        ) {
            killProcess()
            pressHome()
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.res("startup_ready")), 30_000))
            check(device.hasObject(By.text(readyText)))
        }
        assertEquals(beforeSession, ui.startupSessionIdentity())
    }

    @Test
    fun coreWorkflows() {
        ui.reset()
        val pdf = ui.createPdf("profile-pattern.pdf")
        try {
            prepareCoreFixture()
            val beforeSession = ui.startupSessionIdentity()
            assertEquals("3", beforeSession)
            baselineProfileRule.collect(
                packageName = SmokeDevice.PACKAGE,
                includeInStartupProfile = false,
            ) {
                pressHome()
                startActivityAndWait()
                check(device.wait(Until.hasObject(By.res("startup_ready")), 30_000))
                with(ui) {
                    tap("Profile scarf")
                    text("Continue knitting")
                    tapDesc("Continue Profile scarf")
                    counter(9)
                    tapDesc("Add row")
                    counter(10)
                    tapDesc("Subtract row")
                    counter(9)
                    device.pressBack()
                    device.pressBack()
                    desc("New Project")
                    tap("Library")
                    text("Saved Patterns")
                    tap("Tools")
                    tap("Increase / Decrease")
                    edit(0, "40")
                    edit(1, "8")
                    scrollTo("Easy to remember")
                    device.pressBack()
                    tap("Insights")
                    scrollTo(string("insights_section_where_time_went"))
                    text("Profile scarf")
                    assertFalse(device.hasObject(By.text(string("insights_empty_title"))))
                    tap("Projects")
                    openCounter("Profile scarf")
                    scrollTo(string("saved_pattern_detail_open_pattern")).click()
                    text("Page 1 of 2")
                    desc("Profile-pattern")
                    tapDesc("Next page")
                    text("Page 2 of 2")
                    desc("Profile-pattern")
                    tapDesc("Previous page")
                    text("Page 1 of 2")
                    desc("Profile-pattern")
                    device.pressBack()
                    projects()
                }
            }
            assertEquals("Collection must preserve completed history", beforeSession, ui.startupSessionIdentity())
            ui.launch()
            ui.openCounter("Profile scarf")
            ui.counter(9)
            ui.capture("profile-core-complete")
        } finally {
            ui.context.contentResolver.delete(pdf, null, null)
        }
    }

    private fun prepareCoreFixture() =
        with(ui) {
            createProject("Profile scarf")
            repeat(3) {
                tapDesc("Project actions")
                device.waitForIdle()
                scrollTo("Start work session").click()
                assertTrue(device.wait(Until.gone(By.desc("Close sheet")), 30_000))
                device.waitForIdle()
                repeat(3) { tapDesc("Add row") }
                tapDesc("Project actions")
                device.waitForIdle()
                scrollTo("Stop work session").click()
                text(string("work_session_stop_summary_title"))
                device.waitForIdle()
                tap("Save session")
                assertTrue(device.wait(Until.gone(By.text(string("work_session_stop_summary_title"))), 30_000))
                device.waitForIdle()
            }
            counter(9)
            projects()
            startTrial()
            openCounter("Profile scarf")
            scrollTo("Add Pattern").click()
            tap("Import PDF")
            downloads()
            tap("profile-pattern.pdf")
            scrollTo(string("saved_pattern_detail_open_pattern"))
            projects()
        }
}
