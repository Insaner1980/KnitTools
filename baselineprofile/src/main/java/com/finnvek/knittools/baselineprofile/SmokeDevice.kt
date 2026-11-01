package com.finnvek.knittools.baselineprofile

import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

/** External driver: no target classes, database access, entitlement injection or startup hooks. */
internal class SmokeDevice : TestWatcher() {
    val context = InstrumentationRegistry.getInstrumentation().context
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val evidence = File(context.getExternalFilesDir(null), "optimized-smoke")

    override fun failed(
        error: Throwable,
        description: Description,
    ) {
        evidence.mkdirs()
        device.dumpWindowHierarchy(File(evidence, "${description.methodName}.xml"))
        device.takeScreenshot(File(evidence, "${description.methodName}.png"))
        File(evidence, "${description.methodName}-logcat.txt").writeText(shell("logcat -d -t 1500"))
    }

    fun reset() {
        assertEquals(
            "Explicit disposable-emulator opt-in is required",
            "true",
            InstrumentationRegistry.getArguments().getString("optimizedSmokeAllowed"),
        )
        assertEquals("Only an emulator may be reset", "1", shell("getprop ro.kernel.qemu").trim())
        val info = context.packageManager.getApplicationInfo(PACKAGE, 0)
        assertEquals(0, info.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        assertTrue(info.flags and ApplicationInfo.FLAG_TEST_ONLY != 0)
        val permissions =
            context.packageManager
                .getPackageInfo(
                    PACKAGE,
                    PackageManager.GET_PERMISSIONS,
                ).requestedPermissions
                .orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(permissions.contains("com.android.vending.BILLING"))
        assertFalse(permissions.contains("android.permission.CAMERA"))
        recoverSystemStartupDialog()
        assertEquals("Success", shell("pm clear $PACKAGE").trim())
        shell("wm size 720x1600")
        shell("wm density 320")
        shell("settings put system font_scale 1.0")
        shell("settings put system accelerometer_rotation 0")
        shell("settings put system user_rotation 0")
        launch()
        tapDesc(string("welcome_dismiss_a11y"))
        text("Projects")
        find(By.descStartsWith(string("new_project")))
    }

    private fun recoverSystemStartupDialog() {
        val isFirstReset = firstReset
        firstReset = false
        val systemAnr =
            By.pkg("android").res("android:id/alertTitle").text(
                Pattern.compile("Process system isn't responding|System UI isn't responding"),
            )
        if (!device.hasObject(systemAnr)) return
        check(isFirstReset) { "Android system ANR after smoke tests started" }
        // Cold-boot dialogs can outlive a recovered system_server or System UI. Preserve
        // the exact system dialog and dismiss it once, before launching the target app.
        capture("startup-system-anr")
        find(By.pkg("android").res("android:id/aerr_wait")).click()
        assertTrue("Android system did not recover", device.wait(Until.gone(systemAnr), 15_000))
    }

    fun shell(command: String): String = device.executeShellCommand(command)

    fun launch() {
        shell(
            "am start -W -n $PACKAGE/com.finnvek.knittools.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER",
        )
    }

    fun restart() {
        shell("am force-stop $PACKAGE")
        launch()
        text("Projects")
    }

    fun killInBackground() {
        device.pressHome()
        val deadline = SystemClock.uptimeMillis() + 15_000
        val activityState =
            Regex("(?s)packageName=$PACKAGE processName=$PACKAGE.*?mHaveState=(true|false).*?\\n\\s*state=(\\w+)")
        var saved = false
        do {
            val state = activityState.find(shell("dumpsys activity activities"))
            saved = state?.groupValues?.get(1) == "true" && state.groupValues[2] == "STOPPED"
            if (saved) break
            SystemClock.sleep(300)
        } while (SystemClock.uptimeMillis() < deadline)
        check(saved) { "Target Activity has not stopped with saved state" }
        // am kill excludes packages associated with active instrumentation. This userdebug
        // emulator permits terminating only the isolated target PID without force-stopping its task.
        val pid = shell("pidof $PACKAGE").trim()
        check(pid.matches(Regex("[0-9]+"))) { "Expected one isolated target process" }
        shell("su 0 kill -9 $pid")
        SystemClock.sleep(300)
        check(shell("pidof $PACKAGE").trim() != pid) { "Isolated target process survived termination" }
        evidence.mkdirs()
        File(evidence, "terminated-$pid.txt").writeText("$PACKAGE: mHaveState=true, state=STOPPED; terminated PID $pid")
    }

    fun removeCounterWidget() {
        val selector = By.clazz("com.android.launcher3.widget.LauncherAppWidgetHostView").desc("Project Counter")
        val widget = device.findObject(selector) ?: return
        var point = widget.visibleCenter
        shell("input motionevent DOWN ${point.x} ${point.y}")
        try {
            SystemClock.sleep(700)
            shell("input motionevent MOVE ${point.x} ${point.y + 15}")
            point = find(By.res("com.google.android.apps.nexuslauncher:id/delete_target_text")).visibleCenter
            shell("input motionevent MOVE ${point.x} ${point.y}")
        } finally {
            shell("input motionevent UP ${point.x} ${point.y}")
        }
        assertTrue(device.wait(Until.gone(selector), 15_000))
    }

    fun expectMediaFiles(
        directory: String,
        count: Int,
    ) {
        val root = "/data/user/0/$PACKAGE/files/"
        val deadline = SystemClock.uptimeMillis() + 10_000
        do {
            val files = shell("su 0 find $root -type f").lineSequence().filter(String::isNotBlank).toList()
            check(files.all { it.startsWith(root) }) { "Cannot inspect isolated app-owned files" }
            if (files.count { it.startsWith("$root$directory/") } == count) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        error("Expected $count app-owned files in $directory")
    }

    fun find(selector: BySelector): UiObject2 =
        device.wait(Until.findObject(selector), 15_000) ?: error("Missing UI: $selector")

    fun text(value: String): UiObject2 = find(By.text(pattern(value)))

    private fun pattern(value: String): Pattern =
        Pattern.compile(
            Pattern.quote(value),
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE,
        )

    fun desc(value: String): UiObject2 = find(By.desc(value))

    fun tap(value: String) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                label(value)?.let {
                    it.click()
                    return
                }
            } catch (_: StaleObjectException) {
                // Reacquire a node replaced during a system picker or Compose transition.
            }
            SystemClock.sleep(100)
        }
        error("Missing action: $value")
    }

    private fun label(value: String): UiObject2? =
        device.findObjects(By.text(pattern(value))).lastOrNull() ?: device.findObject(By.desc(value))

    fun tapDesc(value: String) {
        desc(value).click()
    }

    fun string(
        name: String,
        vararg args: Any,
    ): String {
        val target = context.createPackageContext(PACKAGE, 0)
        val config = Configuration(target.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val resources = target.createConfigurationContext(config).resources
        val id = resources.getIdentifier(name, "string", PACKAGE)
        check(id != 0) { "Missing packaged string: $name" }
        return resources.getString(id, *args)
    }

    fun edit(
        index: Int,
        value: String,
    ) {
        find(By.clazz("android.widget.EditText"))
        val field = device.findObjects(By.clazz("android.widget.EditText"))[index]
        field.text = value
    }

    fun scrollTo(value: String): UiObject2 {
        repeat(8) {
            label(value)?.let { return it }
            val bounds =
                device.findObject(By.pkg(PACKAGE).scrollable(true))?.visibleBounds
                    ?: Rect(0, 0, device.displayWidth, device.displayHeight)
            device.swipe(
                bounds.centerX(),
                bounds.top + bounds.height() * 3 / 4,
                bounds.centerX(),
                bounds.top + bounds.height() / 3,
                35,
            )
            // Let the scrolling node settle before using its on-screen coordinates.
            device.waitForIdle()
            SystemClock.sleep(250)
        }
        return text(value)
    }

    fun createProject(name: String) {
        find(By.descStartsWith(string("new_project"))).click()
        edit(0, name)
        scrollTo("Create project").click()
        desc(string("current_row_short", 0))
        text(name)
    }

    fun counter(value: Int) {
        desc(string("current_row_short", value))
    }

    fun openCounter(name: String) {
        tapDesc("Continue $name")
        text(name)
        desc("Add row")
    }

    fun projects() {
        // From the counter, Back returns to the Projects list.
        device.pressBack()
        find(By.descStartsWith(string("new_project")))
    }

    fun startTrial() {
        // Exercise the real local trial action from the second-project gate.
        find(By.descStartsWith(string("new_project"))).click()
        scrollTo(string("pro_start_free_trial")).click()
        find(By.clazz("android.widget.EditText"))
        tapDesc("Close sheet")
        // This billing-disabled package cannot load the purchase state required by the journey sheet.
        // The caller verifies the local trial by creating a second active project.
        find(By.descStartsWith(string("new_project")))
    }

    fun capture(name: String) {
        evidence.mkdirs()
        device.dumpWindowHierarchy(File(evidence, "$name.xml"))
        device.takeScreenshot(File(evidence, "$name.png"))
    }

    fun createPdf(name: String): Uri {
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val pdf = PdfDocument()
        try {
            repeat(2) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(400, 600, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawText("Synthetic pattern page ${index + 1}", 20f, 35f, Paint().apply { textSize = 18f })
                pdf.finishPage(page)
            }
            checkNotNull(resolver.openOutputStream(uri)).use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        return uri
    }

    fun downloads() {
        tapDesc("Show roots")
        val roots = By.res(Pattern.compile(".*:id/roots_list"))
        find(roots)
        tap("Downloads")
        assertTrue(device.wait(Until.gone(roots), 15_000))
    }

    fun exportBackup(name: String): File {
        tap(string("backup_export"))
        downloads()
        find(By.res("android:id/title").clazz("android.widget.EditText")).text = name
        tap("Save")
        text("Backup exported.")
        evidence.mkdirs()
        val result = File(evidence, name)
        shell("cp /sdcard/Download/$name ${result.absolutePath}")
        check(result.isFile && result.length() > 0) { "Missing exported backup $name" }
        return result
    }

    companion object {
        const val PACKAGE = "com.finnvek.knittools.optimizedtest"
        private var firstReset = true
    }
}
