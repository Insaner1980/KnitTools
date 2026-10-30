package com.finnvek.knittools.baselineprofile

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.json.JSONArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class OptimizedSmokeTest {
    @get:Rule
    internal val ui = SmokeDevice()

    @Before
    fun freshDisposableInstallation() = ui.reset()

    @Test
    fun projectCounterSessionAndNavigation() =
        with(ui) {
            // R01: this is the unseeded release launcher with no INTERNET permission.
            text(string("no_active_projects"))
            capture("R01-empty")
            createProject("Smoke project")
            tapDesc("Add row")
            counter(1)
            tapDesc("Add row")
            counter(2)
            tapDesc("Subtract row")
            counter(1)
            tapDesc("Undo last change")
            counter(2)
            projects()
            tap("Smoke project")
            tapDesc("Project actions")
            tap("Edit details")
            edit(0, "Edited smoke project")
            scrollTo("Save").click()
            text("Edited smoke project")
            device.pressBack()
            listOf("Library", "Tools", "Insights", "Settings", "Projects").forEach(::tap)
            openCounter("Edited smoke project")
            counter(2)
            tapDesc("Project actions")
            scrollTo("Start work session").click()
            tapDesc("Add row")
            counter(3)
            restart()
            openCounter("Edited smoke project")
            counter(3)
            tapDesc("Project actions")
            scrollTo("Stop work session").click()
            text(string("work_session_stop_summary_title"))
            tap("Save session")
            projects()
            tap("Insights")
            scrollTo(string("insights_section_where_time_went"))
            text("Edited smoke project")
            assertFalse(device.hasObject(By.text(string("insights_empty_title"))))
            capture("R04-insights")
            tap("Projects")
            openCounter("Edited smoke project")
            counter(3)
            tapDesc("Project actions")
            scrollTo("Start work session")
            assertFalse(device.hasObject(By.text("Stop work session")))
        }

    @Test
    fun freeGateTrialAndAdaptiveTransitions() =
        with(ui) {
            createProject("Free project")
            projects()
            tap("New Project")
            text("Create another active project with Pro")
            capture("R11-free-gate")
            tap("Not now")
            startTrial()
            createProject("Trial project")
            tapDesc("Add row")
            counter(1)
            listOf("2560x1600", "1600x800", "720x1600").forEach { size ->
                shell("wm size $size")
                counter(1)
                text("Trial project")
                capture("R13-$size")
            }
            projects()
            listOf("Library", "Tools", "Insights", "Settings", "Projects").forEach(::tap)
            restart()
            openCounter("Trial project")
            counter(1)
        }

    @Test
    fun themeLanguageAndOfflineBrowser() =
        with(ui) {
            createProject("Locale project")
            projects()
            tap("Settings")
            tap("Dark")
            capture("R09-dark")
            tap("Light")
            tap("App language")
            tap("Suomi")
            text("Asetukset")
            capture("R09-finnish")
            shell("am force-stop ${SmokeDevice.PACKAGE}")
            launch()
            text("Projektit")
            tap("Asetukset")
            tap("Sovelluksen kieli")
            tap("English")
            tap("Tools")
            scrollTo("Ravelry").click()
            find(By.clazz("android.webkit.WebView"))
            text("https://www.ravelry.com/patterns/search")
            shell("wm size 2560x1600")
            find(By.clazz("android.webkit.WebView"))
            text("https://www.ravelry.com/patterns/search")
            capture("R10-offline-browser")
            killInBackground()
            device.pressRecentApps()
            find(By.res("com.google.android.apps.nexuslauncher:id/snapshot").desc("KnitTools")).click()
            find(By.clazz("android.webkit.WebView"))
            text("https://www.ravelry.com/patterns/search")
            capture("R10-restored-browser")
            device.pressBack()
            text("Tools")
            // An external caller cannot forge the app-owned widget/counter launch token.
            restart()
            shell(
                "am start -W -n ${SmokeDevice.PACKAGE}/com.finnvek.knittools.MainActivity --ez com.finnvek.knittools.extra.OPEN_COUNTER true --el com.finnvek.knittools.extra.PROJECT_ID 1 --es com.finnvek.knittools.extra.COUNTER_LAUNCH_ID forged-smoke-token",
            )
            desc("New Project")
            assertFalse(device.hasObject(By.desc("Add row")))
        }

    @Test
    fun yarnWebPatternAndBackupRestore() =
        with(ui) {
            createProject("Backup project")
            tapDesc("Add row")
            counter(1)
            projects()
            startTrial()
            tap("Library")
            tap("My Yarn")
            find(By.descStartsWith(string("add_yarn_to_my_yarn"))).click()
            edit(0, "Smoke yarn")
            scrollTo("Save").click()
            tap("Smoke yarn")
            scrollTo("Add").click()
            edit(0, "Edited smoke yarn")
            scrollTo("Save").click()
            text("Edited smoke yarn")
            device.pressBack()
            device.pressBack()
            tap("Saved Patterns")
            tap(string("web_pattern_add"))
            edit(0, "Smoke web pattern")
            edit(1, "https://example.com/Pattern?Part=One")
            scrollTo("Save").click()
            tap("Smoke web pattern")
            tapDesc("More options")
            tap(string("web_pattern_edit"))
            edit(0, "Edited smoke pattern")
            scrollTo("Save").click()
            text("Edited smoke pattern")
            restart()
            tap("Library")
            tap("My Yarn")
            text("Edited smoke yarn")
            device.pressBack()
            tap("Saved Patterns")
            text("Edited smoke pattern")
            device.pressBack()
            tap("Settings")
            scrollTo("Backup & Restore").click()
            val backupName = "optimized-smoke-backup-${SystemClock.uptimeMillis()}.knittools-backup"
            exportBackup(backupName)
            capture("R08-exported")
            device.pressBack()
            tap("Projects")
            createProject("Not in backup")
            projects()
            tap("Settings")
            scrollTo("Backup & Restore").click()
            tap(string("backup_restore"))
            downloads()
            text(backupName).click()
            scrollTo(string("backup_restore")).click()
            tap(string("backup_replace"))
            text("Backup restored.")
            tap(string("backup_continue"))
            text("Backup project")
            assertFalse(device.hasObject(By.text("Not in backup")))
            openCounter("Backup project")
            counter(1)
            projects()
            tap("Library")
            tap("My Yarn")
            text("Edited smoke yarn")
            device.pressBack()
            tap("Saved Patterns")
            tap("Edited smoke pattern")
            capture("R07-R08-restored")
        }

    @Test
    fun pdfImportBookmarksAnnotationsAndExternalShare() =
        with(ui) {
            val pdfName = "optimized-smoke-pattern-${SystemClock.uptimeMillis()}"
            val displayName = pdfName.replaceFirstChar(Char::uppercase)
            val source = createPdf("$pdfName.pdf")
            createProject("PDF project")
            tapDesc("Add row")
            counter(1)
            scrollTo("Add Pattern").click()
            tap("Import PDF")
            downloads()
            text("$pdfName.pdf").click()
            scrollTo(string("saved_pattern_detail_open_pattern")).click()
            find(By.text("Page 1 of 2"))
            tapDesc("Next page")
            text("Page 2 of 2")
            tapDesc("Bookmarks")
            tap("Add bookmark")
            edit(0, "Smoke bookmark")
            tap("Save")
            text("Smoke bookmark")
            tapDesc("Close sheet")
            tap("Mark up")
            tap("Pen")
            val page = desc(displayName).visibleBounds
            device.swipe(
                page.left + page.width() / 3,
                page.top + page.height() / 3,
                page.left + page.width() * 2 / 3,
                page.top + page.height() / 2,
                25,
            )
            tap("Done")
            capture("R05-annotation")
            restart()
            openCounter("PDF project")
            scrollTo(string("saved_pattern_detail_open_pattern")).click()
            text("Page 2 of 2")
            capture("R05-reopened")
            tapDesc("Bookmarks")
            text("Smoke bookmark")
            tapDesc("Close sheet")
            text("Mark up")
            device.pressBack()
            text("PDF project")
            projects()
            tap("Settings")
            scrollTo("Backup & Restore").click()
            ZipFile(exportBackup("$pdfName.knittools-backup")).use { backup ->
                listOf("pattern_annotations", "pattern_bookmarks", "project_documents").forEach { table ->
                    val entry = checkNotNull(backup.getEntry("tables/$table.jsonl"))
                    val rows = backup.getInputStream(entry).bufferedReader().use { it.readLines() }
                    assertTrue("Persisted $table is empty", rows.size > 1)
                    assertTrue(JSONArray(rows[1]).length() > 0)
                }
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    component = ComponentName(SmokeDevice.PACKAGE, "com.finnvek.knittools.MainActivity")
                    setDataAndType(source, "application/pdf")
                    clipData = ClipData.newRawUri("Synthetic PDF", source)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
            text("Save pattern")
            scrollTo("Save to Library only").click()
            restart()
            tap("Library")
            tap("Saved Patterns")
            text(displayName)
            capture("R12-external-pdf-saved")
        }

    @Test
    fun widgetLaunchAndCounter() =
        with(ui) {
            createProject("Widget project")
            tapDesc("Add row")
            counter(1)
            projects()
            startTrial()
            openCounter("Widget project")
            tapDesc("Add row")
            counter(2)
            projects()
            device.pressHome()
            val launcher = "com.google.android.apps.nexuslauncher"
            find(By.res("$launcher:id/workspace"))
            removeCounterWidget()
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight / 2,
                device.displayWidth / 2,
                device.displayHeight / 2,
                200,
            )
            tap("Widgets")
            find(By.res("$launcher:id/widgets_search_bar_edit_text")).text = "KnitTools"
            find(By.res("$launcher:id/app_title").text("KnitTools")).click()
            find(By.res("$launcher:id/widget_preview_container")).click()
            tapDesc("Add Project Counter widget")
            device.pressBack()
            // Select the project's counter through app UI after placing the widget.
            restart()
            openCounter("Widget project")
            tapDesc("Add row")
            counter(3)
            device.pressHome()
            text("Widget project")
            text("3")
            killInBackground()
            tap("Widget project")
            text("Widget project")
            counter(3)
            tapDesc("Add row")
            counter(4)
            capture("R12-widget-launch")
            device.pressHome()
            text("4")
            capture("R12-widget-count")
        }

    @Test
    fun cameraCaptureAndCancellation() =
        with(ui) {
            assertTrue(
                shell(
                    "cmd package resolve-activity --brief -a android.media.action.IMAGE_CAPTURE",
                ).contains("com.android.camera2"),
            )
            createProject("Camera project")
            tapDesc("Add row")
            counter(1)
            projects()
            startTrial()
            openCounter("Camera project")
            scrollTo("Photos").click()
            find(By.descStartsWith("Take photo")).click()
            find(By.res("com.android.camera2:id/shutter_button"))
            device.pressBack()
            text(string("no_photos"))
            expectMediaFiles("progress_photos", 0)
            find(By.descStartsWith("Take photo")).click()
            find(By.res("com.android.camera2:id/shutter_button")).click()
            find(By.res("com.android.camera2:id/done_button")).click()
            desc(string("row_label_format", 1))
            expectMediaFiles("progress_photos", 1)
            capture("R06-progress-capture")
            device.pressBack()
            scrollTo("Add Pattern").click()
            scrollTo("Take pattern photo").click()
            find(By.res("com.android.camera2:id/shutter_button"))
            device.pressBack()
            expectMediaFiles("pattern_captures", 0)
            scrollTo("Take pattern photo").click()
            find(By.res("com.android.camera2:id/shutter_button")).click()
            find(By.res("com.android.camera2:id/done_button")).click()
            tap("Create pattern PDF")
            scrollTo(string("saved_pattern_detail_open_pattern")).click()
            text("Page 1 of 1")
            expectMediaFiles("pattern_pdfs", 1)
            expectMediaFiles("pattern_captures", 0)
            capture("R06-pattern-capture")
            restart()
            openCounter("Camera project")
            scrollTo(string("saved_pattern_detail_open_pattern")).click()
            text("Page 1 of 1")
            capture("R06-reopened")
        }
}
