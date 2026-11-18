package com.finnvek.knittools.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.components.localizedDateTimePattern
import com.finnvek.knittools.ui.screens.insights.currentInsightsFirstDayOfWeek
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Real routes, SQLite, windows, IME and accessibility in the disposable offline package only. */
class AuditPart08RuntimeMatrixTest {
    @get:Rule val testName = TestName()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val arguments = InstrumentationRegistry.getArguments()
    private val withTalkBack get() = arguments.getString("auditTalkBack", "false").toBoolean()
    private val automation get() =
        if (withTalkBack) {
            instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        } else {
            instrumentation.uiAutomation
        }
    private lateinit var app: App
    private lateinit var db: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private val projectName = "Audit cardigan with long sleeves"
    private val secondName = "Audit second project"
    private val yarnName = "Audit extra fine merino and alpaca yarn"
    private val patternName = "Audit cable cardigan with long instructions"
    private var projectId = 0L
    private var secondId = 0L
    private var cardId = 0L
    private var patternId = 0L
    private lateinit var photo: File
    private lateinit var output: File
    private var initialAccessibilityFlags: Int? = null
    private var pickerMonitor: android.app.Instrumentation.ActivityMonitor? = null

    @Before
    fun prepare() {
        app = ApplicationProvider.getApplicationContext()
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        check(ProPolicyTestControl.isActive)
        if (withTalkBack) {
            val manager = app.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
            assertTrue(manager.isTouchExplorationEnabled)
            assertTrue(
                manager
                    .getEnabledAccessibilityServiceList(
                        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
                    ).any { it.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback" },
            )
        }
        initialAccessibilityFlags = automation.serviceInfo.flags
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        if (testName.methodName.startsWith("p1196")) {
            val filter =
                android.content.IntentFilter(android.provider.MediaStore.ACTION_PICK_IMAGES).apply {
                    addDataType("image/*")
                }
            pickerMonitor =
                android.app.Instrumentation
                    .ActivityMonitor(filter, null, true)
                    .also(instrumentation::addMonitor)
        }
        db = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        output =
            File(
                app.getExternalFilesDir(null),
                "audit18/${arguments.getString("auditCase", "initial")}/${testName.methodName}",
            )
        check(output.mkdirs() || output.isDirectory)
        ProPolicyTestControl.setPurchased(true)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
            app.preferencesManager.get().setAppLanguage(
                AppLanguage.valueOf(arguments.getString("auditLanguage", "ENGLISH")),
            )
            app.preferencesManager.get().setThemeMode(ThemeMode.valueOf(arguments.getString("auditTheme", "LIGHT")))
            projectId =
                db.counterProjectDao().insert(
                    CounterProjectEntity(
                        name = projectName,
                        count = 42,
                        targetRows = 96,
                        notes = "Audit editable notes",
                        notesCreated = true,
                        secondaryCounterUsed = true,
                        secondaryCount = 2,
                        stitchTrackingEnabled = true,
                        stitchCount = 80,
                    ),
                )
            secondId = db.counterProjectDao().insert(CounterProjectEntity(name = secondName))
            cardId =
                db.yarnCardDao().upsert(
                    YarnCardEntity(
                        yarnName = yarnName,
                        brand = "Audit brand",
                        fiberContent = "75% merino, 25% alpaca",
                        quantityInStash = 8,
                        linkedProjectId = projectId,
                    ),
                )
            photo = File(app.filesDir, "progress_photos/$projectId/audit.jpg").apply { parentFile?.mkdirs() }
            val bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(140, 80, 110))
            photo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            bitmap.recycle()
            db.progressPhotoDao().insert(
                ProgressPhotoEntity(
                    projectId = projectId,
                    photoUri = Uri.fromFile(photo).toString(),
                    rowNumber = 42,
                    note = "Audit photograph",
                ),
            )
            db.progressPhotoDao().insert(
                ProgressPhotoEntity(
                    projectId = secondId,
                    photoUri = Uri.fromFile(photo).toString(),
                    rowNumber = 3,
                    note = "Audit second photograph",
                ),
            )
            db.sessionDao().insert(
                SessionEntity(
                    projectId = projectId,
                    startedAt = 1780000000000,
                    endedAt = 1780003661000,
                    startRow = 1,
                    endRow = 42,
                    durationMinutes = 61,
                    durationSeconds = 3661,
                    rowsWorked = 41,
                    zoneId = "Europe/Helsinki",
                ),
            )
            val pdf = File(app.filesDir, "pattern_pdfs/$projectId/audit.pdf").apply { parentFile?.mkdirs() }
            val document = PdfDocument()
            try {
                repeat(3) { index ->
                    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    page.canvas.drawText(
                        "Synthetic knitting instructions ${index + 1}",
                        30f,
                        70f,
                        Paint().apply {
                            textSize = 22f
                            color = Color.BLACK
                        },
                    )
                    document.finishPage(page)
                }
                pdf.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            val uri = Uri.fromFile(pdf).toString()
            repository.attachPattern(projectId, uri, patternName, 0, null)
            val saved = checkNotNull(db.savedPatternDao().getByLocalPdfUri(uri))
            patternId = saved.id
            db.savedPatternDao().update(saved.copy(designerName = "Audit designer with a long name"))
        }
    }

    @After
    fun finish() {
        if (::output.isInitialized && scenario != null) capture("final")
        scenario?.close()
        pickerMonitor?.let(instrumentation::removeMonitor)
        initialAccessibilityFlags?.let { original ->
            automation.serviceInfo = automation.serviceInfo.apply { flags = original }
        }
    }

    @Test
    fun p0586_backupRestoreClearsSavedLibraryDetail() {
        val archive = File.createTempFile("part12-restore-", ".knittools-backup", app.cacheDir)
        val filter =
            IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addDataType("*/*")
            }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(archive)))
        val monitor = Instrumentation.ActivityMonitor(filter, result, true)
        try {
            runBlocking(Dispatchers.IO) { app.backupRepository.get().export(Uri.fromFile(archive)) }
            instrumentation.addMonitor(monitor)
            library()
            tap("saved_patterns_title")
            tapText(patternName)
            see("saved_pattern_detail_open_pattern")
            val libraryLabel = s("tab_library")
            val selectedTab =
                nodes().single {
                    it.isVisibleToUser && it.text?.toString()?.equals(libraryLabel, ignoreCase = true) == true
                }
            val bounds = Rect().also(selectedTab::getBoundsInScreen)
            repeat(2) { touch(bounds.centerX().toFloat(), bounds.centerY().toFloat(), 0) }
            automation.waitForIdle(300, 5_000)
            see("saved_pattern_detail_open_pattern")
            tap("tab_settings")
            tap("backup_title")
            tap("backup_restore")
            val restoreLabel = s("backup_restore")
            await {
                val previewRestore =
                    nodes()
                        .filter { it.isVisibleToUser && matches(it, restoreLabel) }
                        .firstNotNullOfOrNull { node ->
                            generateSequence(node) { it.parent }.firstOrNull { it.isClickable && it.isEnabled }
                        }
                if (previewRestore == null) scroll(true)
                previewRestore?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            }
            assertEquals(1, monitor.hits)
            see("backup_confirm_title")
            tap("backup_replace")
            see("backup_restored")
            tap("backup_continue")
            see("new_project")
            tap("tab_library")
            see("my_yarn_title")
            tap("saved_patterns_title")
            seeText(patternName)
        } finally {
            instrumentation.removeMonitor(monitor)
            archive.delete()
        }
    }

    @Test
    fun p0596_deletedProjectClearsOpenGallery() {
        overview()
        tap("project_overview_open_photos")
        seeText("Audit photograph")
        runBlocking(Dispatchers.IO) { repository.deleteProject(projectId) }
        see("new_project")
        assertFalse(nodes().any { it.isVisibleToUser && matches(it, "Audit photograph") })
    }

    @Test
    fun p0599_deletedSavedPatternClosesItsDetails() {
        library()
        tap("saved_patterns_title")
        tapText(patternName)
        see("saved_pattern_detail_open_pattern")
        runBlocking(Dispatchers.IO) { db.savedPatternDao().deleteById(patternId) }
        see("web_pattern_add")
        assertFalse(nodes().any { it.isVisibleToUser && matches(it, s("saved_pattern_detail_open_pattern")) })
    }

    @Test
    fun p1700_backupRestoreThroughDocumentPickerThenNewProjectAndSession() {
        val backup = app.backupRepository.get()
        val name = "audit13-${java.util.UUID.randomUUID()}.knittools-backup"
        val archive = File(app.cacheDir, name)
        runBlocking(Dispatchers.IO) {
            backup.export(Uri.fromFile(archive))
            val old = checkNotNull(db.counterProjectDao().getProject(projectId))
            db.counterProjectDao().update(old.copy(name = "Changed after backup"))
        }
        val resolver = app.contentResolver
        val values =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/KnitToolsAudit")
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val download = checkNotNull(resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            checkNotNull(resolver.openOutputStream(download)).use { target ->
                archive.inputStream().use { it.copyTo(target) }
            }
            values.clear()
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(download, values, null, null)
            launch()
            tap("tab_settings")
            tap("backup_title")
            tap("backup_restore")

            fun pickerClick(label: String) {
                automation.waitForIdle(300, 5_000)
                await {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        automation.clearCache()
                    }
                    var selected: AccessibilityNodeInfo? = null

                    fun find(node: AccessibilityNodeInfo?) {
                        if (node == null) return
                        if ((node.text?.toString() == label || node.contentDescription?.toString() == label) &&
                            node.isVisibleToUser
                        ) {
                            selected =
                                generateSequence(node) { it.parent }.firstOrNull {
                                    it.isClickable && it.isEnabled
                                } ?: selected
                        }
                        repeat(node.childCount) { find(node.getChild(it)) }
                    }
                    find(automation.rootInActiveWindow)
                    selected?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
                }
                automation.waitForIdle(300, 5_000)
            }
            await { automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true }
            val pickerPackage = checkNotNull(automation.rootInActiveWindow?.packageName).toString()
            fun shellText(command: String): String =
                android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
                    it.bufferedReader().readText().trim()
                }

            val userId = shellText("am get-current-user").toInt()
            fun pickerLabel(resourceName: String): String =
                shellText("cmd overlay lookup --user $userId $pickerPackage $pickerPackage:string/$resourceName")

            pickerClick(pickerLabel("drawer_open"))
            pickerClick(pickerLabel("downloads_label"))
            pickerClick("KnitToolsAudit")
            pickerClick(name)
            await {
                nodes().any {
                    it.isVisibleToUser &&
                        matches(it, app.getString(com.finnvek.knittools.R.string.backup_preview_projects, 2))
                }
            }
            var restore: AccessibilityNodeInfo? = null
            repeat(12) {
                if (restore == null) {
                    restore =
                        nodes()
                            .filter { it.isVisibleToUser && it.isEnabled && matches(it, s("backup_restore")) }
                            .firstNotNullOfOrNull { node ->
                                generateSequence(node) { it.parent }.firstOrNull {
                                    it.isClickable &&
                                        it.isEnabled
                                }
                            }
                    if (restore == null) scroll(true)
                }
            }
            assertTrue(checkNotNull(restore).performAction(AccessibilityNodeInfo.ACTION_CLICK))
            see("backup_confirm_title")
            tap("backup_replace")
            see("backup_restored")
            tap("backup_continue")
            see("tab_projects")
            assertTrue(
                runBlocking {
                    db.counterProjectDao().getAllProjectsOnce().any {
                        it.name == projectName &&
                            it.count == 42
                    }
                },
            )
            tap("new_project")
            see("new_project_details_title")
            edit("Audit after restore")
            hideKeyboard()
            tap("create_project")
            var createdId = 0L
            await {
                createdId =
                    runBlocking {
                        db
                            .counterProjectDao()
                            .getAllProjectsOnce()
                            .firstOrNull {
                                it.name ==
                                    "Audit after restore"
                            }?.id
                            ?: 0L
                    }
                createdId > 0L
            }
            tap("project_actions_title")
            tap("work_session_start")
            await { runBlocking { db.sessionDao().getActiveSession()?.projectId == createdId } }
            tap("counter_add_row")
            await { runBlocking { db.counterProjectDao().getProject(createdId)?.count == 1 } }
            tap("project_actions_title")
            tap("work_session_stop")
            tap("work_session_save")
            await { runBlocking { db.sessionDao().getActiveSession() == null } }
            db.openHelper.readableDatabase
                .query(
                    "SELECT COUNT(*) FROM sessions WHERE projectId = $createdId AND rowsWorked = 1",
                ).use {
                    assertTrue(it.moveToFirst())
                    assertEquals(1, it.getInt(0))
                }
            capture("restored-created-and-session-saved")
        } finally {
            resolver.delete(download, null, null)
            archive.delete()
        }
    }

    @Test
    fun p1142_projectListFoldersCreationAndEmptyState() {
        launch()
        tap("all_projects")
        see("folder_sheet_title")
        tap("folder_create")
        edit("Audit folder with a long name")
        hideKeyboard()
        capture("folder-form")
        tap("cancel")
        see("folder_sheet_title")
        back()
        see("tab_projects")
        tap("new_project")
        see("new_project_details_title")
        edit("Audit new project")
        capture("new-project-ime")
        hideKeyboard()
        tap("create_project")
        await { runBlocking { repository.getActiveProjectCount() == 3 } }
        capture("created-counter")
        scenario?.close()
        scenario = null
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
            app.onboardingStateRepository.get().dismissWelcome()
        }
        launch()
        tap("new_project")
        see("new_project_details_title")
    }

    @Test
    fun p1146_overviewSectionsAndActionSheet() {
        overview()
        capture("overview-header")
        seek(s("notes"))
        tapText("Audit editable notes")
        see("notes")
        hideKeyboard()
        tap("back")
        seek(s("reminders"))
        capture("overview-content")
        seek(s("session_history_title"))
        tap("session_history_title")
        see("session_history_title")
        back()
        top()
        tap("project_actions_title")
        capture("overview-actions")
        back()
    }

    @Test
    fun p1150_counterControlsWriteOnlyIntendedValues() {
        counter()
        val before = runBlocking { repository.getProject(projectId)!!.count }
        tap("counter_add_row")
        await { runBlocking { repository.getProject(projectId)!!.count == before + 1 } }
        tap("counter_decrease_row")
        await { runBlocking { repository.getProject(projectId)!!.count == before } }
        tap("counter_undo_last_change")
        await { runBlocking { repository.getProject(projectId)!!.count == before + 1 } }
        tapText(
            app.getString(
                com.finnvek.knittools.R.string.counter_step_change_description,
                s("counter_step_rows_title"),
                1,
            ),
        )
        edit("3")
        automation.waitForIdle(500, 5_000)
        await { nodes().any { it.isEditable && it.isVisibleToUser && it.text?.toString() == "3" } }
        seek(s("save"))
        capture("step-editor-ime")
        hideKeyboard()
        tap("cancel")
        await { nodes().none { it.isVisibleToUser && it.isEditable } }
        assertEquals(1, runBlocking { repository.getProject(projectId)!!.stepSize })
        tapText(
            app.getString(
                com.finnvek.knittools.R.string.counter_step_change_description,
                s("counter_step_rows_title"),
                1,
            ),
        )
        edit("3")
        hideKeyboard()
        tap("save")
        await { runBlocking { repository.getProject(projectId)!!.stepSize == 3 } }
        tapText(app.getString(com.finnvek.knittools.R.string.counter_step_increase, 3, s("main_counter_rows")))
        await { runBlocking { repository.getProject(projectId)!!.count == before + 4 } }
        capture("counter-controls")
        tap("project_actions_title")
        tap("work_session_start")
        await { runBlocking { db.sessionDao().getActiveSession() != null } }
        tap("project_actions_title")
        tap("work_session_stop")
        tap("work_session_save")
        await { runBlocking { db.sessionDao().getActiveSession() == null } }
        capture("counter-session")
    }

    @Test
    fun p1150_secondaryAndStitchControlsKeepMainCountUnchanged() {
        counter()
        top()
        capture("secondary-before")
        listOf("counter_increase", "counter_decrease").forEach { name ->
            val node = nodes().first { it.isVisibleToUser && matches(it, s(name)) }
            val button = generateSequence(node) { it.parent }.first { it.isClickable }
            val bounds = Rect().also(button::getBoundsInScreen)
            val minimum = (48 * app.resources.displayMetrics.density).toInt()
            assertTrue("48 dp target $name: $bounds", bounds.width() >= minimum && bounds.height() >= minimum)
        }
        tap("counter_increase")
        await { runBlocking { repository.getProject(projectId)!!.secondaryCount == 3 } }
        tap("counter_decrease")
        await { runBlocking { repository.getProject(projectId)!!.secondaryCount == 2 } }
        tapText(app.getString(com.finnvek.knittools.R.string.counter_increase_named, s("stitch_tracker_label")))
        await { runBlocking { repository.getProject(projectId)!!.currentStitch == 1 } }
        tapText(app.getString(com.finnvek.knittools.R.string.counter_decrease_named, s("stitch_tracker_label")))
        await { runBlocking { repository.getProject(projectId)!!.currentStitch == 0 } }
        assertEquals(42, runBlocking { repository.getProject(projectId)!!.count })
        capture("stitch-controls")
    }

    @Test
    fun p1154_galleryViewerAndEmptyState() {
        overview()
        tap("project_overview_open_photos")
        tapText("Audit photograph")
        capture("opaque-photo-viewer")
        back()
        longPress("Audit photograph")
        edit("Audit renamed photograph")
        capture("rename-photo-ime")
        hideKeyboard()
        tap("confirm")
        seeText("Audit renamed photograph")
        assertTrue(
            runBlocking { db.progressPhotoDao().getAllPhotosOnce().any { it.note == "Audit renamed photograph" } },
        )
        runBlocking(Dispatchers.IO) {
            db
                .progressPhotoDao()
                .getAllPhotosOnce()
                .filter {
                    it.projectId == projectId
                }.forEach { db.progressPhotoDao().delete(it.id) }
        }
        see("no_photos")
        capture("empty-gallery")
        tap("take_photo")
        capture("capture-entry")
        back()
    }

    @Test
    fun p1158_projectPdfReadingMarkupLayersAndExport() {
        overview()
        seek(patternName)
        tapText(patternName)
        exerciseReader(project = true)
    }

    @Test
    fun p1190_libraryPdfReadingMarkupLayersAndExport() {
        library()
        tap("saved_patterns_title")
        tapText(patternName)
        tap("saved_pattern_detail_open_pattern")
        exerciseReader(project = false)
    }

    private fun exerciseReader(project: Boolean) {
        // The controls appear before PdfRenderer has loaded this three-page fixture.
        seeText(app.getString(com.finnvek.knittools.R.string.pattern_page_indicator, 1, 3))
        tap("pattern_next_page")
        seeText(app.getString(com.finnvek.knittools.R.string.pattern_page_indicator, 2, 3))
        capture("page-two")
        tap("pattern_previous_page")
        seeText(app.getString(com.finnvek.knittools.R.string.pattern_page_indicator, 1, 3))
        tap("more_options")
        tap("pattern_show_reading_line")
        tap("more_options")
        tap("pattern_show_vertical_guide")
        capture("reading-guides")
        if (project) {
            tap("pattern_bookmarks")
            see("pattern_bookmark_empty")
            capture("bookmarks-empty")
            back()
            tap("more_options")
            tap("project_documents_title")
            capture("document-chooser")
            back()
        }
        tap("pattern_mark_up")
        capture("markup-tools")
        tap("pattern_mark_up_done")
        tap("more_options")
        tap("pattern_annotations_layers")
        capture("annotation-layers")
        back()
        tap("more_options")
        tap("pattern_annotation_export_pdf")
        await {
            automation.rootInActiveWindow
                ?.packageName
                ?.toString()
                ?.endsWith("documentsui") == true
        }
        capture("export-destination")
        // Back first navigates parent folders in DocumentsUI; leave the picker, not just one folder.
        repeat(8) {
            if (automation.rootInActiveWindow
                    ?.packageName
                    ?.toString()
                    ?.endsWith("documentsui") == true
            ) {
                back()
            }
        }
        see("pattern_next_page")
        tap("pattern_next_page")
        seeText(app.getString(com.finnvek.knittools.R.string.pattern_page_indicator, 2, 3))
    }

    @Test
    fun p1162_sessionHistoryDeleteConfirmation() {
        overview()
        seek(s("session_history_title"))
        tap("session_history_title")
        capture("session-history")
        tap("more_options")
        tap("delete")
        see("delete_session_title")
        capture("delete-confirmation")
        tap("delete")
        see("no_sessions")
        assertEquals(0L, runBlocking { db.sessionDao().countCompletedSessions() })
    }

    @Test
    fun p1162_sessionDeleteFailureKeepsRowAndAllowsRetry() {
        overview()
        tap("session_history_title")
        failSql("DELETE", "sessions") {
            tap("more_options")
            tap("delete")
            tap("delete")
            see("generic_error_unknown")
            capture("session-delete-error")
            assertEquals(1L, runBlocking { db.sessionDao().countCompletedSessions() })
        }
        tap("more_options")
        tap("delete")
        tap("delete")
        see("no_sessions")
    }

    @Test
    fun p1142_folderWriteFailureKeepsFormAndAllowsCancel() {
        launch()
        tap("all_projects")
        tap("folder_create")
        edit("Audit folder")
        hideKeyboard()
        failSql("INSERT", "project_folders") {
            tap("folder_create")
            // SQLite RAISE(ABORT) is a constraint failure, which the repository maps to a name conflict.
            see("folder_name_duplicate")
            capture("folder-write-error")
            assertTrue(nodes().any { it.isEditable && it.text?.toString() == "Audit folder" })
            tap("cancel")
        }
    }

    @Test
    fun p1186_patternDeleteFailureKeepsDetailAndPdf() {
        library()
        tap("saved_patterns_title")
        tapText(patternName)
        failSql("DELETE", "saved_patterns") {
            tap("more_options")
            tap("remove_pattern")
            tap("remove_pattern")
            see("generic_error_unknown")
            capture("pattern-delete-error")
            assertTrue(runBlocking { db.savedPatternDao().getById(patternId) != null })
            tap("saved_pattern_detail_open_pattern")
            see("pattern_next_page")
        }
    }

    @Test
    fun p1202_photoDeleteFailureKeepsPhotoAndFile() {
        library()
        tap("all_photos_title")
        longPress("Audit photograph")
        failSql("DELETE", "progress_photos") {
            tap("delete")
            tap("delete")
            see("generic_error_unknown")
            capture("photo-delete-error")
            assertEquals(2, runBlocking { db.progressPhotoDao().getAllPhotosOnce().size })
            assertTrue(photo.exists())
        }
    }

    @Test
    fun p1158_corruptProjectPdfShowsErrorAndKeepsNavigation() = corruptPdf(project = true)

    @Test
    fun p1190_corruptLibraryPdfShowsErrorAndKeepsNavigation() = corruptPdf(project = false)

    private fun corruptPdf(project: Boolean) {
        File(app.filesDir, "pattern_pdfs/$projectId/audit.pdf").writeText("Synthetic invalid PDF")
        if (project) {
            overview()
            tapText(patternName)
        } else {
            library()
            tap("saved_patterns_title")
            tapText(patternName)
        }
        see("pattern_open_failed")
        capture("pdf-error")
        tap("back")
        seeText(patternName)
    }

    private fun failSql(
        operation: String,
        table: String,
        action: () -> Unit,
    ) {
        runBlocking(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER audit18_fail BEFORE $operation ON $table BEGIN SELECT RAISE(ABORT, 'audit18 controlled failure'); END",
            )
        }
        try {
            action()
        } finally {
            runBlocking(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("DROP TRIGGER audit18_fail") }
        }
    }

    @Test
    fun p1166_counterHistoryRowsAndLiveRemoval() {
        counter()
        tap("counter_add_row")
        tap("project_actions_title")
        tap("counter_history_title")
        see("counter_history_title")
        capture("counter-history")
        back()
    }

    @Test
    fun p1170_notesImeSaveAndRecreation() {
        overview()
        seek("Audit editable notes")
        tapText("Audit editable notes")
        edit("Audit changed notes with long lines\nSecond line retained after recreation")
        capture("notes-ime")
        scenario!!.recreate()
        await { nodes().any { it.isEditable && it.text?.toString()?.contains("Audit changed notes") == true } }
        hideKeyboard()
        back()
        await { runBlocking { repository.getProject(projectId)!!.notes.contains("Audit changed notes") } }
    }

    @Test
    fun p1178_libraryAllDestinationsReachable() {
        library()
        listOf(
            "saved_patterns_title",
            "my_yarn_title",
            "all_photos_title",
            "tool_needle_sizes",
            "size_charts_title",
            "abbreviations_title",
            "chart_symbols_title",
        ).forEach { name ->
            seek(s(name))
            capture(name)
            tap(name)
            capture("opened-$name")
            back()
        }
    }

    @Test
    fun p1174_projectGaugeCopiesResultWithoutChangingProject() {
        counter()
        tap("project_actions_title")
        tap("measurement_title")
        listOf("measurement_per_10cm", "enter_directly").forEach { name ->
            seek(s(name))
            val option = nodes().first { it.isVisibleToUser && matches(it, s(name)) }
            if (generateSequence(option) { it.parent }.none { it.isChecked || it.isSelected }) tap(name)
        }
        listOf(
            "measurement_actual_stitches" to "22",
            "measurement_pattern_stitches" to "20",
            "stitches_in_pattern" to "100",
        ).forEach { (label, value) ->
            val description =
                if (label == "stitches_in_pattern") {
                    s(label)
                } else {
                    app.getString(
                        com.finnvek.knittools.R.string.measurement_field_with_unit,
                        s(label),
                        s("measurement_per_10cm"),
                    )
                }
            seek(description, descriptionOnly = true)
            val node = nodes().first { it.isVisibleToUser && it.contentDescription?.toString() == description }
            val field = generateSequence(node) { it.parent }.first { it.isEditable }
            assertTrue(
                field.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
                    },
                ),
            )
        }
        tap("measurement_copy")
        see("measurement_copied")
        scenario!!.onActivity { activity ->
            val clip = activity.getSystemService(android.content.ClipboardManager::class.java).primaryClip
            val text = checkNotNull(clip).getItemAt(0).text.toString()
            assertTrue(text.contains("110"))
            assertTrue(text.contains(projectName))
        }
        assertEquals(42, runBlocking { repository.getProject(projectId)!!.count })
        capture("gauge-copied-result")
        scenario!!.onActivity { activity ->
            activity.getSystemService(android.content.ClipboardManager::class.java).clearPrimaryClip()
        }
        SystemClock.sleep(400)
        tap("measurement_per_4in")
        tap("measurement_per_10cm")
        listOf("measurement_measure_gauge", "measurement_calculate", "measurement_convert").forEach { mode ->
            tap(mode)
            capture("gauge-$mode")
        }
        tap("back")
        see("counter_add_row")
    }

    @Test
    fun p1182_savedPatternsSelectionAndEmptyState() {
        library()
        tap("saved_patterns_title")
        tapText(patternName)
        capture("pattern-detail")
        back()
        longPress(patternName)
        capture("pattern-selection")
        back()
        tap("web_pattern_add")
        edit("https://example.invalid/audit-pattern")
        capture("web-pattern-ime")
        hideKeyboard()
        tap("cancel")
        runBlocking(Dispatchers.IO) { db.savedPatternDao().deleteById(patternId) }
        capture("pattern-removed")
    }

    @Test
    fun p1186_savedPatternDetailActions() {
        library()
        tap("saved_patterns_title")
        tapText(patternName)
        capture("detail-header")
        seek("Audit designer with a long name")
        tap("saved_pattern_detail_attach_to_project")
        seeText(secondName)
        capture("attach-project-chooser")
        back()
        seek(s("saved_pattern_detail_open_pattern"))
        tap("saved_pattern_detail_open_pattern")
        capture("reader-from-detail")
        back()
        capture("detail-return")
    }

    @Test
    fun p1194_myYarnListCreationSelection() {
        library()
        tap("my_yarn_title")
        longPress("Audit brand $yarnName")
        capture("yarn-selection")
        back()
        tap("add_yarn_to_my_yarn")
        edit("Audit new yarn with a long name")
        capture("yarn-ime")
        hideKeyboard()
        capture("yarn-form")
        back()
        tapText("Audit brand $yarnName")
        capture("yarn-detail")
        runBlocking(Dispatchers.IO) { db.yarnCardDao().delete(cardId) }
        see("empty_my_yarn")
        capture("yarn-empty")
    }

    @Test
    fun p1198_yarnDetailQuantityStatusAndProject() {
        yarnDetail()
        failSql("UPDATE", "yarn_cards") {
            tap("status_finished")
            see("generic_error_unknown")
            assertEquals("IN_STASH", runBlocking { db.yarnCardDao().getCard(cardId)!!.status })
            capture("yarn-status-write-error")
        }
        listOf("status_finished" to "FINISHED", "status_in_use" to "IN_USE", "status_in_stash" to "IN_STASH")
            .forEach { (label, status) ->
                tap(label)
                await { runBlocking { db.yarnCardDao().getCard(cardId)!!.status == status } }
            }
        seek(s("quantity_label"))
        tap("counter_increase")
        await { runBlocking { db.yarnCardDao().getCard(cardId)?.quantityInStash == 9 } }
        tap("counter_decrease")
        await { runBlocking { db.yarnCardDao().getCard(cardId)?.quantityInStash == 8 } }
        seek(s("yarn_details_title"))
        capture("yarn-details")
        seek(projectName)
        tapText(projectName)
        see("counter_add_row")
    }

    @Test
    fun p1196_pickerResultSurvivesRecreationAndKeepsItsCard() {
        yarnDetail()
        tap("add_yarn_photo")
        assertEquals(1, checkNotNull(pickerMonitor).hits)
        val requestCode = pickerRequestCode()
        scenario!!.recreate()
        scenario!!.onActivity { activity ->
            assertTrue(
                activity.activityResultRegistry.dispatchResult(
                    requestCode,
                    android.app.Activity.RESULT_OK,
                    Intent().setData(Uri.fromFile(photo)),
                ),
            )
        }
        await {
            runBlocking {
                !db
                    .yarnCardDao()
                    .getCard(cardId)!!
                    .photoUri
                    .isBlank()
            }
        }
        assertEquals(yarnName, runBlocking { db.yarnCardDao().getCard(cardId)!!.yarnName })
        capture("restored-picker-result")
    }

    @Test
    fun p1196_deletedCardCannotRedirectDelayedPickerToAnotherCard() {
        val other = runBlocking { db.yarnCardDao().upsert(YarnCardEntity(yarnName = "Audit other yarn")) }
        yarnDetail()
        tap("add_yarn_photo")
        assertEquals(1, checkNotNull(pickerMonitor).hits)
        val requestCode = pickerRequestCode()
        scenario!!.recreate()
        runBlocking { db.yarnCardDao().delete(cardId) }
        scenario!!.onActivity { activity ->
            activity.activityResultRegistry.dispatchResult(
                requestCode,
                android.app.Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(photo)),
            )
        }
        seeText("Audit other yarn")
        tapText("Audit other yarn")
        scenario!!.onActivity { activity ->
            activity.activityResultRegistry.dispatchResult(
                requestCode,
                android.app.Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(photo)),
            )
        }
        SystemClock.sleep(600)
        assertEquals(null, runBlocking { db.yarnCardDao().getCard(cardId) })
        assertTrue(
            runBlocking {
                db
                    .yarnCardDao()
                    .getCard(other)!!
                    .photoUri
                    .isBlank()
            },
        )
        capture("stale-picker-not-retargeted")
    }

    private fun pickerRequestCode(): Int {
        var code: Int? = null
        scenario!!.onActivity { activity ->
            val state = Bundle()
            activity.activityResultRegistry.onSaveInstanceState(state)
            val launched = checkNotNull(state.getStringArrayList("KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS"))
            val keys = checkNotNull(state.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"))
            val codes = checkNotNull(state.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS"))
            code = codes[keys.indexOf(launched.single())]
        }
        return checkNotNull(code)
    }

    @Test
    fun p1202_allPhotosFilterViewerRemoval() {
        library()
        tap("all_photos_title")
        tap("all_projects")
        tapText(projectName)
        seeText("Audit photograph")
        assertFalse(nodes().any { it.isVisibleToUser && it.text?.toString() == "Audit second photograph" })
        tapText("Audit photograph")
        capture("photo-viewer")
        back()
        scenario!!.recreate()
        seeText("Audit photograph")
        longPress("Audit photograph")
        capture("photo-selection")
        back()
        runBlocking(
            Dispatchers.IO,
        ) { db.progressPhotoDao().getAllPhotosOnce().forEach { db.progressPhotoDao().delete(it.id) } }
        see("no_photos")
    }

    @Test
    fun p1206_toolsAllFiveLaunchers() {
        launch()
        tap("tab_tools")
        listOf(
            "tool_gauge_converter",
            "tool_increase_decrease",
            "tool_cast_on_calculator",
            "tool_yarn_estimator",
            "tool_ravelry",
        ).forEach { name ->
            seek(s(name))
            capture(name)
            tap(name)
            capture("opened-$name")
            back()
        }
    }

    @Test
    fun p1315_weeklyChartDescriptionAndActionsUseTheSelectedLanguage() {
        val zone = ZoneId.of("Europe/Helsinki")
        val today = LocalDate.now(zone)
        val latestSessionDay = today.minusDays(1)
        runBlocking(Dispatchers.IO) {
            val session = checkNotNull(db.sessionDao().getLatestSession(projectId))
            db.sessionDao().deleteById(session.id)
            for (day in listOf(today.minusMonths(2), latestSessionDay)) {
                val start =
                    day
                        .atTime(12, 0)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                db.sessionDao().insert(session.copy(id = 0, startedAt = start, endedAt = start + 3_661_000))
            }
        }
        launch()
        tap("tab_insights")
        seek(s("insights_section_week_by_week"))
        val prefix = s("insights_chart_a11y_weekly").substringBefore('%')
        fun chartNode(): AccessibilityNodeInfo? =
            nodes().firstOrNull { it.isVisibleToUser && it.contentDescription?.startsWith(prefix) == true }

        fun AccessibilityNodeInfo.localizedAction(label: String): Int {
            assertTrue("Weekly chart must be enabled", isEnabled)
            return actionList.single { it.label?.toString() == label }.id
        }

        await { chartNode() != null }
        val node = checkNotNull(chartNode()) { "Weekly chart must expose its localized weekly description" }
        val previous = s("insights_chart_a11y_previous")
        val next = s("insights_chart_a11y_next")
        node.localizedAction(next)
        if (withTalkBack) {
            node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            await { node.refresh() && node.isAccessibilityFocused }
        }
        val locale = app.resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern(localizedDateTimePattern(locale, "MMMd"), locale)
        val latestWeek = latestSessionDay.with(TemporalAdjusters.previousOrSame(currentInsightsFirstDayOfWeek()))
        val latestWeekLabel = latestWeek.format(formatter).uppercase(locale)
        val previousWeekLabel = latestWeek.minusWeeks(1).format(formatter).uppercase(locale)
        assertTrue("Previous must select the preceding week", node.performAction(node.localizedAction(previous)))
        seeText(previousWeekLabel)

        // Resolve Next from the chart after the previous selection is visible in the header.
        val updatedChart = checkNotNull(chartNode())
        assertTrue("Next must return to the latest week", updatedChart.performAction(updatedChart.localizedAction(next)))
        await {
            nodes().any { it.isVisibleToUser && it.text?.startsWith(latestWeekLabel) == true }
        }
        capture("weekly-chart-actions")
    }

    @Test
    fun p1316_proWordingRemainsReachableWithoutStartingAPurchase() {
        launch()
        tap("tab_settings")
        tap("knittools_pro")
        tap("onboarding_pro_free_show_all")
        seek(s("onboarding_pro_free_project"))
        capture("pro-content-policy")
        seek(s("pro_page_purchased"))
        capture("pro-purchased-test-state")
        seek(s("restore_purchases"))
        capture("pro-restore-action")
        ProPolicyTestControl.setPurchased(false)
        ProPolicyTestControl.setTrialState(
            com.finnvek.knittools.pro
                .TrialState(),
        )
        ProPolicyTestControl.setTrialStartResult(com.finnvek.knittools.pro.TrialStartResult.Failed)
        tap("pro_start_14_day_trial")
        seek(s("pro_trial_start_failed"))
        capture("pro-trial-failed")
        ProPolicyTestControl.setTrialStartResult(com.finnvek.knittools.pro.TrialStartResult.Started)
        tap("pro_start_14_day_trial")
        see("trial_started_screen_title")
        tap("completed_close")
        val activeLabel = app.resources.getQuantityString(com.finnvek.knittools.R.plurals.pro_status_trial_days, 14, 14)
        seek(activeLabel)
        capture("pro-trial-active")
        ProPolicyTestControl.setTrialState(
            com.finnvek.knittools.pro
                .TrialState(hasStarted = true),
        )
        seek(s("pro_status_trial_ended"))
        capture("pro-trial-ended")
        // The offline fixture does not invoke Play billing or claim a real entitlement.
        back()
        see("settings")
    }

    @Test
    fun p1317_backupPrivacyAndSettingsLanguageSheetAreReachable() {
        launch()
        tap("tab_settings")
        tap("settings_language")
        capture("language-sheet")
        back()
        tap("backup_title")
        seek(s("backup_privacy"))
        capture("backup-privacy")
        seek(s("backup_exclusions"))
        seek(s("backup_export"))
        seek(s("backup_restore"))
        capture("backup-actions")
        back()
        seek(s("usage_analytics_description"))
        capture("analytics-consent-description")
        assertEquals(2, runBlocking { repository.getActiveProjectCount() })
    }

    @Test
    fun p1400_maximumCounterLiveUpdateRetainsItsValueAndActions() {
        counter()
        runBlocking(Dispatchers.IO) {
            val project = checkNotNull(db.counterProjectDao().getProject(projectId))
            db.counterProjectDao().update(project.copy(count = Int.MAX_VALUE, stepSize = 99))
        }
        val amount = app.getString(com.finnvek.knittools.R.string.counter_step_decrease, 99, s("main_counter_rows"))
        seek(amount)
        capture("maximum-counter-live-update")
        tapText(amount)
        await { runBlocking { repository.getProject(projectId)!!.count == Int.MAX_VALUE - 99 } }
        val increase = app.getString(com.finnvek.knittools.R.string.counter_step_increase, 99, s("main_counter_rows"))
        tapText(increase)
        await { runBlocking { repository.getProject(projectId)!!.count == Int.MAX_VALUE } }
        capture("maximum-counter-restored")
        scenario!!.recreate()
        seek(amount)
        assertEquals(Int.MAX_VALUE, runBlocking { repository.getProject(projectId)!!.count })
        capture("maximum-counter-recreated")
    }

    @Test
    fun p1404_keyboardFocusKeepsCounterActionsSeparate() {
        fun activate(
            label: String,
            reverse: Boolean = false,
        ) {
            repeat(60) {
                val target = nodes().firstOrNull { matches(it, label) }
                val button = target?.let { generateSequence(it) { node -> node.parent }.firstOrNull { it.isClickable } }
                if (button?.isFocused == true) {
                    File(output, "keyboard-focus.txt").appendText("$label inputFocus=${button.isFocused}\n")
                    capture("keyboard-focus-${label.take(15)}")
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
                    SystemClock.sleep(400)
                    return
                }
                val now = SystemClock.uptimeMillis()
                val event =
                    KeyEvent(
                        now,
                        now,
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_TAB,
                        0,
                        if (reverse) KeyEvent.META_SHIFT_ON else 0,
                    ).apply { source = android.view.InputDevice.SOURCE_KEYBOARD }
                instrumentation.sendKeySync(event)
                instrumentation.sendKeySync(KeyEvent.changeAction(event, KeyEvent.ACTION_UP))
                SystemClock.sleep(100)
            }
            capture("keyboard-focus-missing")
            error("Keyboard focus did not reach $label")
        }
        launch()
        activate(app.getString(com.finnvek.knittools.R.string.project_continue_content_description, projectName))
        see("counter_add_row")
        assertEquals(42, runBlocking { repository.getProject(projectId)!!.count })
        activate(s("counter_add_row"))
        await { runBlocking { repository.getProject(projectId)!!.count == 43 } }
        activate(s("counter_decrease_row"), reverse = true)
        await { runBlocking { repository.getProject(projectId)!!.count == 42 } }
        activate(
            app.getString(
                com.finnvek.knittools.R.string.counter_step_change_description,
                s("counter_step_rows_title"),
                1,
            ),
        )
        await { nodes().any { it.isEditable && it.isVisibleToUser } }
        assertEquals(1, runBlocking { repository.getProject(projectId)!!.stepSize })
        assertEquals(42, runBlocking { repository.getProject(projectId)!!.count })
        capture("keyboard-step-editor")
    }

    private fun launch() {
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        scenario!!.onActivity { activity ->
            val config = activity.resources.configuration
            File(
                output,
                "configuration.txt",
            ).writeText(
                "widthDp=${config.screenWidthDp}\nfontScale=${config.fontScale}\nuiMode=${config.uiMode}\nlocales=${config.locales.toLanguageTags()}",
            )
            arguments.getString("auditWidth")?.let { assertEquals(it.toInt(), config.screenWidthDp) }
            arguments.getString("auditFont")?.let { assertEquals(it.toFloat(), config.fontScale, 0.01f) }
        }
        see("tab_projects")
        capture("launch")
    }

    private fun overview() {
        launch()
        seek(projectName)
        tapText(projectName)
        see("project_actions_title")
    }

    private fun counter() {
        overview()
        tapText(app.getString(com.finnvek.knittools.R.string.project_continue_content_description, projectName))
        see("counter_add_row")
    }

    private fun library() {
        launch()
        tap("tab_library")
        see("my_yarn_title")
    }

    private fun yarnDetail() {
        library()
        tap("my_yarn_title")
        tapText("Audit brand $yarnName")
        seeText(yarnName)
    }

    private fun s(name: String): String {
        val id = app.resources.getIdentifier(name, "string", app.packageName)
        check(id != 0) { "Unknown string $name" }
        return app.getString(id)
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            automation.clearCache()
        }
        val result = mutableListOf<AccessibilityNodeInfo>()

        fun visit(node: AccessibilityNodeInfo) {
            result += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        automation.rootInActiveWindow?.let(::visit)
        return result.filter { it.packageName?.toString() == app.packageName }
    }

    private fun matches(
        node: AccessibilityNodeInfo,
        value: String,
    ): Boolean {
        fun same(candidate: CharSequence?): Boolean =
            candidate
                ?.toString()
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.equals(value, ignoreCase = true) == true
        return same(node.text) || same(node.contentDescription) || node.actionList.any { same(it.label) }
    }

    private fun see(name: String) = seeText(s(name))

    private fun seeText(value: String) = await { nodes().any { it.isVisibleToUser && matches(it, value) } }

    private fun seek(
        value: String,
        descriptionOnly: Boolean = false,
    ) {
        fun visibleMatch(node: AccessibilityNodeInfo) =
            node.isVisibleToUser &&
                if (descriptionOnly) node.contentDescription?.toString() == value else matches(node, value)
        if (nodes().any(::visibleMatch)) return
        top()
        repeat(15) {
            if (nodes().any(::visibleMatch)) return
            scroll(true)
        }
        capture("missing-${value.take(20)}")
        error("Could not scroll to $value")
    }

    private fun scroll(forward: Boolean) {
        val scrollNode = nodes().lastOrNull { it.isScrollable && it.isVisibleToUser }
        scrollNode?.performAction(
            if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
        )
        SystemClock.sleep(250)
    }

    private fun top() {
        repeat(10) { scroll(false) }
    }

    private fun tap(name: String) = tapText(s(name))

    private fun tapText(value: String) {
        seek(value)
        val action =
            checkNotNull(
                nodes().filter { it.isVisibleToUser && matches(it, value) }.firstNotNullOfOrNull { node ->
                    generateSequence(node) { it.parent }.firstOrNull { it.isClickable }
                },
            ) { "No clickable ancestor for $value" }
        assertTrue("Enabled $value", action.isEnabled)
        if (withTalkBack) {
            // TalkBack may already own this focus; the resulting focused state is the contract.
            action.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            await { action.refresh() && action.isAccessibilityFocused }
            assertTrue(action.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(400)
            return
        }
        // Ordinary touch runs keep touch exploration disabled; real TalkBack runs assert focus above.
        automation.waitForIdle(200, 5_000)
        // A visible node can still move during a sheet or scroll animation.
        var stableBounds = Rect()
        var stableSince = SystemClock.elapsedRealtime()
        await {
            check(action.refresh()) { "Touch target disappeared: $value" }
            val currentBounds = Rect().also(action::getBoundsInScreen)
            if (currentBounds != stableBounds) {
                stableBounds = currentBounds
                stableSince = SystemClock.elapsedRealtime()
            }
            SystemClock.elapsedRealtime() - stableSince >= 600
        }
        // seek already found a visible target; starting another scroll can consume the following touch.
        assertTrue(action.refresh())
        val bounds = Rect().also(action::getBoundsInScreen)
        generateSequence(action.parent) { it.parent }.filter { it.isScrollable }.forEach { ancestor ->
            assertTrue("Visible scroll bounds for $value", bounds.intersect(Rect().also(ancestor::getBoundsInScreen)))
        }
        scenario?.onActivity { activity ->
            val decor = activity.window.decorView
            val insets =
                androidx.core.view.ViewCompat
                    .getRootWindowInsets(decor)
                    ?.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type
                            .systemBars(),
                    )
            val origin = IntArray(2).also(decor::getLocationOnScreen)
            val viewport =
                Rect(
                    origin[0] + (insets?.left ?: 0),
                    origin[1] + (insets?.top ?: 0),
                    origin[0] + decor.width - (insets?.right ?: 0),
                    origin[1] + decor.height - (insets?.bottom ?: 0),
                )
            assertTrue("Touch must stay outside the system bars: $value $bounds", bounds.intersect(viewport))
        }
        assertTrue("Nonempty touch target $value: $bounds", bounds.width() > 0 && bounds.height() > 0)
        touch(bounds.centerX().toFloat(), bounds.centerY().toFloat(), 60)
        SystemClock.sleep(400)
    }

    private fun longPress(value: String) {
        seek(value)
        val node = nodes().first { it.isVisibleToUser && matches(it, value) }
        if (withTalkBack) {
            val action = generateSequence(node) { it.parent }.first { it.isLongClickable }
            action.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            await { action.refresh() && action.isAccessibilityFocused }
            assertTrue(action.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
            SystemClock.sleep(400)
            return
        }
        val bounds = Rect().also(node::getBoundsInScreen)
        touch(
            bounds.centerX().toFloat(),
            bounds.centerY().toFloat(),
            ViewConfiguration.getLongPressTimeout().toLong() + 200,
        )
        SystemClock.sleep(400)
    }

    private fun touch(
        x: Float,
        y: Float,
        duration: Long,
    ) {
        val coordinates = "${x.toInt()} ${y.toInt()}"
        val gesture = if (duration <= 100) "tap $coordinates" else "swipe $coordinates $coordinates $duration"
        val command = "input touchscreen -d 0 $gesture"
        File(output, "touches.txt").appendText("$command\n")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            assertEquals("Android input command must succeed", "", it.bufferedReader().readText().trim())
        }
    }

    private fun edit(value: String) {
        await { nodes().any { it.isEditable && it.isVisibleToUser } }
        // Wait for the sheet's entry and IME-related layout events before reading the field's touch bounds.
        automation.waitForIdle(500, 5_000)
        val field = nodes().first { it.isEditable && it.isVisibleToUser }
        val bounds = Rect().also(field::getBoundsInScreen)
        if (withTalkBack) {
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        } else {
            touch(bounds.centerX().toFloat(), bounds.centerY().toFloat(), 60)
        }
        assertTrue(
            field.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
                },
            ),
        )
        SystemClock.sleep(400)
    }

    private fun hideKeyboard() {
        val ime =
            automation.windows.any {
                it.type ==
                    android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        if (ime) back()
    }

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        SystemClock.sleep(400)
    }

    private fun capture(name: String) {
        val safe = name.replace(Regex("[^a-zA-Z0-9-]"), "_")
        val appNodes = nodes()
        if (appNodes.isEmpty()) {
            File(
                output,
                "$safe-outside-app.txt",
            ).writeText(
                "The active window is outside the isolated app; no screen contents captured. " +
                    "package=${automation.rootInActiveWindow?.packageName}",
            )
            return
        }
        val bitmap = automation.takeScreenshot()
        if (bitmap != null) {
            File(output, "$safe.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        File(output, "$safe.txt").writeText(
            appNodes.joinToString("\n") { node ->
                "${Rect().also(
                    node::getBoundsInScreen,
                )} visible=${node.isVisibleToUser} enabled=${node.isEnabled} click=${node.isClickable} focus=${node.isAccessibilityFocused} text=${node.text} description=${node.contentDescription}"
            },
        )
    }

    private fun await(check: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (check()) return
            SystemClock.sleep(100)
        }
        capture("timeout")
        error("Condition timed out in ${testName.methodName}")
    }
}
