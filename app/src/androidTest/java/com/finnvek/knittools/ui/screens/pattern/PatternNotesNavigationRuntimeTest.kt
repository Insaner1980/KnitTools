package com.finnvek.knittools.ui.screens.pattern

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowInsets
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.ProjectDocumentFileAvailability
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatternNotesNavigationRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private lateinit var app: App
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private lateinit var documents: ProjectDocumentRepository
    private lateinit var savedPatterns: SavedPatternRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private val projectIds = mutableListOf<Long>()
    private val fixtureFiles = mutableListOf<File>()
    private val savedPatternIds = mutableSetOf<Long>()
    private val suffix = UUID.randomUUID().toString().take(8)
    private val firstName = "Notes cardigan $suffix"
    private val secondName = "Notes pullover $suffix"
    private val primaryLabel = "Main instructions $suffix"
    private val secondaryLabel = "Sleeve chart $suffix"
    private val isNarrowLayout =
        InstrumentationRegistry.getArguments().getString("notesLayout") == "fi-320dp-font2"
    private lateinit var primary: ProjectDocument
    private lateinit var secondary: ProjectDocument
    private var previousTheme: ThemeMode? = null

    @Before
    fun setUp() {
        // Puhelimella sallitaan vain erillinen verkkoluvaton testipaketti.
        check(InstrumentationRegistry.getArguments().getString("notesNavigationIsolated") == "true")
        app = ApplicationProvider.getApplicationContext()
        if (app.packageName == "com.finnvek.knittools.notestest") {
            check(app.checkSelfPermission(android.Manifest.permission.INTERNET) == android.content.pm.PackageManager.PERMISSION_DENIED)
            check(app.checkSelfPermission("com.android.vending.BILLING") == android.content.pm.PackageManager.PERMISSION_DENIED)
        } else {
            check(shell("getprop ro.kernel.qemu").trim() == "1")
            check(app.getSystemService(ConnectivityManager::class.java).activeNetwork == null)
        }
        database = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        val transactions = RoomDatabaseTransactionRunner(database)
        savedPatterns = SavedPatternRepository(
            database.savedPatternDao(), app, database.counterProjectDao(), transactions,
            Dispatchers.IO, database.projectDocumentDao(),
        )
        documents = ProjectDocumentRepository(
            database.projectDocumentDao(), database.counterProjectDao(), savedPatterns,
            PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), transactions),
            transactions, ProjectDocumentFileAvailability(app, Dispatchers.IO),
        )
        runBlocking(Dispatchers.IO) {
            InstrumentationRegistry.getArguments().getString("readerTheme")?.let { theme ->
                previousTheme = app.preferencesManager.get().preferences.first().themeMode
                app.preferencesManager.get().setThemeMode(ThemeMode.valueOf(theme))
            }
            projectIds += database.counterProjectDao().insert(
                CounterProjectEntity(name = firstName, count = 72, notes = FIRST_NOTES, notesCreated = true),
            )
            projectIds += database.counterProjectDao().insert(
                CounterProjectEntity(name = secondName, count = 9, notes = SECOND_NOTES, notesCreated = true),
            )
            val sharedPdf = createPdf("sleeve")
            primary = addPdf(projectIds.first(), createPdf("main"), primaryLabel)
            secondary = addPdf(projectIds.first(), sharedPdf, secondaryLabel)
            addPdf(projectIds.last(), sharedPdf, secondaryLabel)
            documents.updateViewerState(projectIds.first(), secondary.id, if (isNarrowLayout) 2 else 0, "[]", true, 0.63f, false, true, 0.37f)
            database.patternBookmarkDao().insert(
                PatternBookmarkEntity(
                    projectId = projectIds.first(), documentKey = secondary.documentKey,
                    name = "Continue sleeve", pageIndex = 2, yFraction = 0.63f, createdAt = 1L,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        try {
            scenario?.close()
        } finally {
            if (::repository.isInitialized) {
                runBlocking(Dispatchers.IO) {
                    val active = database.sessionDao().getActiveSession()
                    if (active?.projectId in projectIds) repository.discardActiveSession(checkNotNull(active).sessionToken)
                    projectIds.forEach { repository.deleteProject(it) }
                    savedPatternIds.forEach { savedPatterns.deleteById(it) }
                    previousTheme?.let { app.preferencesManager.get().setThemeMode(it) }
                }
            }
            fixtureFiles.forEach { it.delete() }
        }
    }

    @Test
    fun secondaryPdfReturnsAfterEditingRecreationAndRepeatedVisitsWithoutSharingNotes() {
        runBlocking(Dispatchers.IO) {
            assertTrue(repository.startSession(projectIds.first()) is StartSessionResult.Started)
        }
        launchAndOpenSecondary()
        val before = snapshot()
        openNotes(repeatActivation = true)
        assertEquals(FIRST_NOTES, editable().text.toString())
        setNotes(EDITED_NOTES)
        capture("editor")
        checkNotNull(scenario).recreate()
        waitUntil("restored note draft") { find { it.isEditable }?.text?.toString() == EDITED_NOTES }

        // Ensimmäinen järjestelmän Back saa sulkea näppäimistön poistumatta editorista.
        systemBackFromEditor()
        assertSecondaryPage()
        assertEquals(EDITED_NOTES, persistedNotes(projectIds.first()))
        assertEquals(SECOND_NOTES, persistedNotes(projectIds.last()))
        assertPreserved(before)
        capture("returned-after-recreation")

        repeat(2) {
            openNotes()
            assertEquals(EDITED_NOTES, editable().text.toString())
            clickDescription(text(R.string.back))
            assertSecondaryPage()
            assertPreserved(before)
        }
        // Yksi Back riittää takaisin counteriin: editorikohteita ei saa jäädä pinoon.
        clickDescription(text(R.string.back))
        waitFor { it.contentDescription?.toString() == text(R.string.project_actions_title) }
        clickDescription(text(R.string.back))
        openProjectDocument(secondName, secondaryLabel)
        openNotes(secondName)
        assertEquals(SECOND_NOTES, editable().text.toString())
        assertEquals(EDITED_NOTES, persistedNotes(projectIds.first()))
    }

    @Test
    fun libraryReaderOfTheSharedPdfDoesNotOfferProjectNotes() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        val before = snapshot()
        clickText(text(R.string.tab_library))
        clickText(text(R.string.saved_patterns_title))
        scrollToText(secondaryLabel)
        clickText(secondaryLabel)
        scrollToText(text(R.string.saved_pattern_detail_open_pattern))
        clickText(text(R.string.saved_pattern_detail_open_pattern))
        waitForText(secondaryLabel)
        clickDescription(text(R.string.more_options))
        assertNull(find(visibleOnly = false) { hasText(it, text(R.string.pattern_open_project_notes)) })
        capture("library-menu")
        assertPreserved(before)
    }

    @Test
    fun deletedSecondaryDocumentFallsBackWithoutRecreatingItOrStartingASession() {
        launchAndOpenSecondary()
        assertNull(runBlocking(Dispatchers.IO) { database.sessionDao().getActiveSession() })
        openNotes()
        runBlocking(Dispatchers.IO) {
            assertTrue(documents.remove(projectIds.first(), secondary.id) is ProjectDocumentMutationResult.Removed)
        }
        clickDescription(text(R.string.back))
        waitForText(primaryLabel)
        runBlocking(Dispatchers.IO) {
            assertNull(documents.getDocument(secondary.id))
            assertEquals(primary.id, documents.getPrimary(projectIds.first())?.id)
            assertEquals(72, database.counterProjectDao().getProject(projectIds.first())?.count)
            assertNull(database.sessionDao().getActiveSession())
        }
    }

    @Test
    fun deletedInitiatingProjectLeavesEditorWithoutWritingToTheOtherProject() {
        launchAndOpenSecondary()
        openNotes()
        runBlocking(Dispatchers.IO) { repository.deleteProject(projectIds.first()) }
        waitUntil("missing project editor closed") { find { it.isEditable } == null }
        assertEquals(SECOND_NOTES, persistedNotes(projectIds.last()))
        runBlocking(Dispatchers.IO) {
            assertNull(database.counterProjectDao().getProject(projectIds.first()))
            assertNull(database.sessionDao().getActiveSession())
        }
    }

    @Test
    fun visibleProjectPageControlsWorkBeforeAndAfterNotes() {
        check(!isNarrowLayout) // Tämä regressio alkaa aina ensimmäiseltä sivulta.
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        openProjectDocument(firstName, secondaryLabel)
        waitForText(text(R.string.pattern_page_indicator, 1, 4))
        recordReaderLayout("project-first")
        physicalPageChange(next = true, expectedPage = 2)
        physicalPageChange(next = true, expectedPage = 3)
        assertReadablePdf()
        val before = snapshot()
        assertNull(before.activeToken)
        recordReaderLayout("project-third")
        openNotes()
        setNotes(EDITED_NOTES)
        physicalTapDescription(text(R.string.back))
        assertSecondaryPage()
        assertEquals(EDITED_NOTES, persistedNotes(projectIds.first()))
        assertPreserved(before)
        assertReadablePdf()
        recordReaderLayout("project-returned")
        physicalPageChange(next = true, expectedPage = 4)
        physicalPageChange(next = false, expectedPage = 3)
        physicalPageChange(next = false, expectedPage = 2)
        physicalPageChange(next = true, expectedPage = 3)
        // Sivunvaihdot päivittävät dokumentin aikaleiman normaalisti.
        assertPreserved(before, pageControlsUsed = true)
        scrollToText(text(R.string.pattern_annotation_export_pdf))
        assertReadablePdf()
        recordReaderLayout("project-tools-scrolled")
    }

    @Test
    fun visibleLibraryPageControlsKeepPdfUsable() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        clickText(text(R.string.tab_library))
        clickText(text(R.string.saved_patterns_title))
        scrollToText(secondaryLabel)
        clickText(secondaryLabel)
        scrollToText(text(R.string.saved_pattern_detail_open_pattern))
        clickText(text(R.string.saved_pattern_detail_open_pattern))
        waitForText(text(R.string.pattern_page_indicator, 1, 4))
        recordReaderLayout("library-first")
        physicalPageChange(next = true, expectedPage = 2)
        physicalPageChange(next = true, expectedPage = 3)
        assertReadablePdf()
        recordReaderLayout("library-third")
        physicalPageChange(next = false, expectedPage = 2)
        assertNull(find { it.contentDescription?.toString() == text(R.string.counter_increase) })
        clickDescription(text(R.string.more_options))
        assertNull(find(visibleOnly = false) { hasText(it, text(R.string.pattern_open_project_notes)) })
    }

    private fun physicalPageChange(next: Boolean, expectedPage: Int) {
        physicalTapDescription(text(if (next) R.string.pattern_next_page else R.string.pattern_previous_page))
        waitForText(text(R.string.pattern_page_indicator, expectedPage, 4))
    }

    private fun physicalTapDescription(description: String) {
        var node: AccessibilityNodeInfo? = waitFor { it.contentDescription?.toString() == description }
        while (node != null && !node.isClickable) node = node.parent
        val target = checkNotNull(node)
        assertTrue(target.isEnabled)
        val bounds = Rect().also(target::getBoundsInScreen)
        val usable = usableWindowBounds()
        assertTrue("Control outside usable window: $description $bounds / $usable", usable.contains(bounds))
        val density = app.resources.displayMetrics.density
        assertTrue("Touch target too small: $description $bounds", bounds.width() >= 47 * density && bounds.height() >= 47 * density)
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEachIndexed { index, action ->
            val event = MotionEvent.obtain(time, time + index * 50L, action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        automation.waitForIdle(500, 5_000)
    }

    private fun usableWindowBounds(): Rect {
        var bounds = Rect()
        checkNotNull(scenario).onActivity { activity ->
            val metrics = activity.windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            bounds = Rect(metrics.bounds).apply { inset(insets.left, insets.top, insets.right, insets.bottom) }
        }
        return bounds
    }

    private fun assertReadablePdf() {
        val pdf = waitFor { it.contentDescription?.toString() == secondaryLabel && it.className == "android.widget.ImageView" }
        val visible = Rect().also(pdf::getBoundsInScreen)
        assertTrue(visible.intersect(usableWindowBounds()))
        assertTrue("PDF viewport is too short: $visible", visible.height() >= 120 * app.resources.displayMetrics.density)
    }

    private fun recordReaderLayout(name: String) {
        capture(name)
        val layout = InstrumentationRegistry.getArguments().getString("notesLayout", "default")
        val folder = File(app.cacheDir, "pattern-notes-navigation")
        checkNotNull(scenario).onActivity { activity ->
            val config = activity.resources.configuration
            val metrics = activity.windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            File(folder, "$layout-$name.txt").writeText(
                "windowPx=${metrics.bounds}; screenDp=${config.screenWidthDp}x${config.screenHeightDp}; " +
                    "density=${activity.resources.displayMetrics.density}; densityDpi=${config.densityDpi}; " +
                    "fontScale=${config.fontScale}; locale=${config.locales.toLanguageTags()}; " +
                    "systemInsetsPx=$insets; theme=${InstrumentationRegistry.getArguments().getString("readerTheme")}",
            )
        }
    }

    private fun launchAndOpenSecondary() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        openProjectDocument(firstName, secondaryLabel)
        if (!isNarrowLayout) {
            waitForText(text(R.string.pattern_page_indicator, 1, 4))
            repeat(2) { index ->
                clickDescription(text(R.string.pattern_next_page))
                waitForText(text(R.string.pattern_page_indicator, index + 2, 4))
            }
        }
        assertSecondaryPage()
    }

    private fun openProjectDocument(projectName: String, documentLabel: String) {
        scrollToText(projectName)
        clickText(projectName)
        clickDescription(text(R.string.project_actions_title))
        waitForText(text(R.string.project_documents_title))
        clickText(text(R.string.project_documents_title))
        clickText(documentLabel)
        waitForText(documentLabel)
        waitFor { it.contentDescription?.toString() == text(R.string.more_options) }
    }

    private fun openNotes(projectName: String = firstName, repeatActivation: Boolean = false) {
        clickDescription(text(R.string.more_options))
        scrollToText(text(R.string.pattern_open_project_notes))
        capture("reader-menu")
        click(repeatActivation) { hasText(it, text(R.string.pattern_open_project_notes)) }
        waitForText(text(R.string.notes_editor_title, projectName))
        editable()
    }

    private fun systemBackFromEditor() {
        val keyboardVisible = shell("dumpsys input_method").contains("mInputShown=true")
        shell("input keyevent KEYCODE_BACK")
        if (keyboardVisible) {
            waitUntil("keyboard hidden") { !shell("dumpsys input_method").contains("mInputShown=true") }
            assertNotNull(find { it.isEditable })
            shell("input keyevent KEYCODE_BACK")
        }
    }

    private fun assertSecondaryPage() {
        waitForText(secondaryLabel)
        waitForText(text(R.string.pattern_page_indicator, 3, 4))
        assertNull(find { it.isEditable })
        assertEquals(2, runBlocking(Dispatchers.IO) { documents.getDocument(secondary.id)?.currentPage })
    }

    private fun persistedNotes(id: Long): String? =
        runBlocking(Dispatchers.IO) { database.counterProjectDao().getProject(id)?.notes }

    private data class Snapshot(
        val documents: List<ProjectDocument>,
        val activeToken: String?,
        val sessionCount: Long,
        val bookmarkAndAnnotationRows: List<List<String?>>,
    )

    private fun snapshot(): Snapshot = runBlocking(Dispatchers.IO) {
        Snapshot(
            documents.getDocuments(projectIds.first()),
            database.sessionDao().getActiveSession()?.sessionToken,
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sessions").use {
                check(it.moveToFirst())
                it.getLong(0)
            },
            listOf("pattern_bookmarks", "pattern_annotations").flatMap { table ->
                database.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY id").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add((0 until cursor.columnCount).map(cursor::getString))
                    }
                }
            },
        )
    }

    private fun assertPreserved(before: Snapshot, pageControlsUsed: Boolean = false) {
        val after = snapshot()
        val expected = if (pageControlsUsed) {
            before.copy(documents = before.documents.map { document ->
                document.copy(updatedAt = after.documents.single { it.id == document.id }.updatedAt)
            })
        } else {
            before
        }
        assertEquals(expected, after)
        runBlocking(Dispatchers.IO) {
            assertEquals(primary.id, documents.getPrimary(projectIds.first())?.id)
            assertEquals(72, database.counterProjectDao().getProject(projectIds.first())?.count)
            assertEquals(9, database.counterProjectDao().getProject(projectIds.last())?.count)
        }
    }

    private suspend fun addPdf(projectId: Long, file: File, label: String): ProjectDocument {
        val result = documents.addImportedPdf(projectId, Uri.fromFile(file).toString(), label)
        val document = (result as ProjectDocumentMutationResult.Added).document
        document.savedPatternId?.let(savedPatternIds::add)
        return document
    }

    private fun createPdf(name: String): File {
        val file = File(app.filesDir, "pattern_pdfs/notes-navigation-$suffix-$name.pdf")
        check(file.parentFile?.mkdirs() == true || file.parentFile?.isDirectory == true)
        val pdf = PdfDocument()
        try {
            repeat(4) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(600, 800, index + 1).create())
                page.canvas.drawText("$name page ${index + 1}", 40f, 100f, Paint().apply { textSize = 24f })
                pdf.finishPage(page)
            }
            file.outputStream().use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        fixtureFiles += file
        return file
    }

    private fun editable(): AccessibilityNodeInfo = waitFor { it.isEditable }

    private fun setNotes(value: String) {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        assertTrue(editable().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        waitUntil("edited notes") { find { it.isEditable }?.text?.toString() == value }
    }

    private fun text(id: Int, vararg arguments: Any): String = app.getString(id, *arguments)

    private fun hasText(node: AccessibilityNodeInfo, value: String): Boolean =
        node.text?.toString()?.lineSequence()?.any { it.equals(value, ignoreCase = true) } == true

    private fun waitForText(value: String) = waitFor { hasText(it, value) }

    private fun clickText(value: String) = click { hasText(it, value) }

    private fun clickDescription(value: String) = click { it.contentDescription?.toString() == value }

    private fun click(repeatActivation: Boolean = false, matches: (AccessibilityNodeInfo) -> Boolean) {
        var node: AccessibilityNodeInfo? = waitFor(matches)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue(checkNotNull(node).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        if (repeatActivation) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        automation.waitForIdle(500, 5_000)
    }

    private fun scrollToText(value: String) {
        automation.waitForIdle(500, 5_000)
        var direction = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        waitUntil("scroll to $value") {
            if (find { hasText(it, value) } != null) {
                true
            } else {
                val scrollable = find { it.isScrollable }
                if (scrollable != null && scrollable.actionList.none { it.id == direction }) {
                    direction = if (direction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    } else {
                        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    }
                }
                scrollable?.performAction(direction)
                automation.waitForIdle(500, 5_000)
                false
            }
        }
    }

    private fun waitFor(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        var result: AccessibilityNodeInfo? = null
        waitUntil("visible UI node") {
            result = find(matches = matches)
            result != null
        }
        return checkNotNull(result)
    }

    private fun find(
        visibleOnly: Boolean = true,
        matches: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (android.os.Build.VERSION.SDK_INT >= 33) automation.clearCache()
        val root = automation.rootInActiveWindow ?: return null
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if ((!visibleOnly || node.isVisibleToUser) && matches(node)) return node
            repeat(node.childCount) { index ->
                node.getChild(index)?.let { child -> visit(child)?.let { return it } }
            }
            return null
        }
        root.refresh()
        return visit(root)
    }

    private fun waitUntil(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        val root = automation.rootInActiveWindow
        val visibleText = buildList {
            fun collect(node: AccessibilityNodeInfo) {
                if (node.packageName?.toString() != app.packageName) return
                node.text?.let { add(it.toString()) }
                node.contentDescription?.let { add(it.toString()) }
                repeat(node.childCount) { node.getChild(it)?.let(::collect) }
            }
            root?.let(::collect)
        }
        if (root?.packageName?.toString() == app.packageName) capture("failure")
        throw AssertionError("Timed out: $description; visible: $visibleText")
    }

    private fun shell(command: String): String = automation.executeShellCommand(command).use {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
    }

    private fun capture(name: String) {
        val layout = InstrumentationRegistry.getArguments().getString("notesLayout", "default")
        val folder = File(app.cacheDir, "pattern-notes-navigation").apply { mkdirs() }
        val bitmap = checkNotNull(automation.takeScreenshot())
        File(folder, "$layout-$name.png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }

    private companion object {
        const val FIRST_NOTES = "Left front: 72 rows finished. Next: chart row 5. Check armhole before continuing."
        const val SECOND_NOTES = "This project uses the same PDF but has its own notes."
        const val EDITED_NOTES = "Left front checked. Continue chart row 5 with the smaller needle."
    }
}
