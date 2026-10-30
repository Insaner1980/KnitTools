package com.finnvek.knittools.ui.screens.counter

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.storage.CounterLaunchTokenStore
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.io.File
import java.util.UUID

/** Real external camera and Android Activity results; synthetic data and entitlement only. */
class ExternalCameraCaptureTest {
    @get:Rule
    val timeout = Timeout.seconds(90)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app: App = ApplicationProvider.getApplicationContext()
    private lateinit var repository: CounterRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private var owner = 0L
    private val projectName = "External camera ${UUID.randomUUID()}"

    @Before
    fun setup() {
        check(app.packageName.endsWith(".activepolicytest") && ProPolicyTestControl.isActive)
        instrumentation.uiAutomation.serviceInfo =
            instrumentation.uiAutomation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            }
        assumeTrue(
            "This external UI driver requires the AOSP camera handler",
            instrumentation.uiAutomation
                .executeShellCommand("cmd package resolve-activity --brief -a android.media.action.IMAGE_CAPTURE")
                .use { descriptor ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
                        it.readText().contains("com.android.camera2/com.android.camera.CaptureActivity")
                    }
                },
        )
        assertFalse(
            app.packageManager
                .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .contains(Manifest.permission.CAMERA),
        )
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            setTestProStatus(ProStatus.PRO_PURCHASED)
            withTimeout(10_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == ProStatus.PRO_PURCHASED }
            }
            app.preferencesManager.get().setAppLanguage(AppLanguage.ENGLISH)
            owner =
                app.database.get().counterProjectDao().insert(
                    CounterProjectEntity(name = projectName, count = 23),
                )
        }
        scenario =
            ActivityScenario.launch(
                launcherIntent().apply {
                    putExtra("com.finnvek.knittools.extra.OPEN_COUNTER", true)
                    putExtra("com.finnvek.knittools.extra.PROJECT_ID", owner)
                    putExtra(
                        "com.finnvek.knittools.extra.COUNTER_LAUNCH_ID",
                        CounterLaunchTokenStore.issueLaunchId(app),
                    )
                },
            )
        waitUntil("Counter ready") { findText(projectName) != null }
        scenario?.onActivity { it.intent = launcherIntent() }
    }

    @After
    fun cleanup() {
        if (instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName
                ?.toString() == CAMERA_PACKAGE
        ) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        }
        scenario?.close()
        if (owner > 0) runBlocking(Dispatchers.IO) { repository.deleteProject(owner) }
    }

    @Test
    fun progressPhotoReturnsOnceAfterActivityRecreationInExternalCamera() {
        openCamera(pattern = false)
        recreateBehindCamera()
        finishCamera()
        waitUntil("One captured progress photo") { photos().size == 1 }
        assertEquals(23, photos().single().rowNumber)
        assertEquals(owner, photos().single().projectId)
        val file = File(requireNotNull(Uri.parse(photos().single().photoUri).path))
        assertTrue(file.length() > 0)
        waitUntil("Progress capture source cleaned") {
            captureFiles(pattern = false).map { it.canonicalPath } == listOf(file.canonicalPath)
        }
    }

    @Test
    fun patternPhotoReturnsOnceAfterActivityRecreationAndCreatesReadablePdf() {
        openCamera(pattern = true)
        recreateBehindCamera()
        finishCamera()
        clickText(R.string.pattern_image_create_pdf)
        waitUntil("One attached pattern PDF") { documents().size == 1 }
        val document = documents().single()
        assertEquals(owner, document.projectId)
        PdfRenderer(requireNotNull(app.contentResolver.openFileDescriptor(Uri.parse(document.localPdfUri), "r"))).use {
            assertEquals(1, it.pageCount)
        }
        waitUntil("Pattern capture cleaned") { captureFiles(pattern = true).isEmpty() }
    }

    @Test
    fun cancellingExternalProgressCameraDeletesTarget() = verifyCancellation(pattern = false)

    @Test
    fun cancellingExternalPatternCameraDeletesTarget() = verifyCancellation(pattern = true)

    private fun verifyCancellation(pattern: Boolean) {
        openCamera(pattern)
        assertEquals(1, captureFiles(pattern).size)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitUntil("App resumed after cancellation") {
            instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName
                ?.toString() == app.packageName
        }
        waitUntil("Cancelled capture cleaned") { captureFiles(pattern).isEmpty() }
        assertTrue(photos().isEmpty())
        assertTrue(documents().isEmpty())
    }

    private fun openCamera(pattern: Boolean) {
        if (pattern) {
            clickText(R.string.project_content_add_pattern)
            waitUntil("Pattern picker can expand") {
                findNode { node ->
                    node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_EXPAND }
                }?.performAction(AccessibilityNodeInfo.ACTION_EXPAND) == true
            }
            clickText(R.string.pattern_picker_camera_scan)
        } else {
            clickText(R.string.project_content_photos)
            clickText(R.string.take_photo)
        }
        waitUntil("External camera opened without an app permission prompt") { findId("shutter_button") != null }
    }

    private fun recreateBehindCamera() {
        var previous: MainActivity? = null
        scenario?.onActivity { previous = it }
        val activity = requireNotNull(previous)
        instrumentation.runOnMainSync { activity.recreate() }
        waitUntil("Original Activity destroyed while camera is active") { activity.isDestroyed }
        assertEquals(
            CAMERA_PACKAGE,
            instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName
                ?.toString(),
        )
        // Compare with the resumed instance after the actual external result is delivered.
        previousActivity = activity
    }

    private var previousActivity: MainActivity? = null

    private fun finishCamera() {
        clickNode(requireNotNull(findId("shutter_button")))
        waitUntil("Camera confirmation") { findId("done_button") != null }
        clickNode(requireNotNull(findId("done_button")))
        waitUntil("App resumed after camera result") {
            instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName
                ?.toString() == app.packageName
        }
        scenario?.onActivity { assertNotSame(previousActivity, it) }
    }

    private fun clickText(id: Int) {
        val value = app.getString(id)
        waitUntil("Visible action: $value") {
            if (findText(value) != null) {
                true
            } else {
                val expanded =
                    findNode { node ->
                        node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_EXPAND }
                    }?.performAction(AccessibilityNodeInfo.ACTION_EXPAND) == true
                if (!expanded) {
                    findNode { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
                false
            }
        }
        clickNode(requireNotNull(findText(value)))
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable) target = requireNotNull(target.parent)
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun findId(id: String) = findNode { it.viewIdResourceName == "$CAMERA_PACKAGE:id/$id" }

    private fun findText(value: String) =
        findNode {
            it.packageName?.toString() == app.packageName &&
                (it.text?.toString() == value || it.contentDescription?.toString() == value)
        }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && predicate(node)) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { visit(it)?.let { match -> return match } }
            }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }

    private fun waitUntil(
        message: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { message }
            SystemClock.sleep(100)
        }
    }

    private fun photos() =
        runBlocking(Dispatchers.IO) {
            app.database
                .get()
                .progressPhotoDao()
                .getAllPhotosOnce()
                .filter { it.projectId == owner }
        }

    private fun documents() =
        runBlocking(Dispatchers.IO) {
            app.database
                .get()
                .projectDocumentDao()
                .getForProject(owner)
        }

    private fun captureFiles(pattern: Boolean) =
        File(app.filesDir, "${if (pattern) "pattern_captures" else "progress_photos"}/$owner")
            .walkTopDown()
            .filter { it.isFile }
            .toList()

    private fun launcherIntent() =
        Intent(app, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

    private companion object {
        const val CAMERA_PACKAGE = "com.android.camera2"
    }
}
