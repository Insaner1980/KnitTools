package com.finnvek.knittools.ui.screens.counter

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.TestContentSplashRule
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationTool
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationViewModel
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationWriteError
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.screens.pattern.PatternViewerScreen
import com.finnvek.knittools.ui.screens.pattern.PatternViewerViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class ExternalActivityResultOwnershipTest {
    // These tests replace activity content; its startup splash must not block the test clock's first draw.
    @get:Rule(order = 0)
    val splash =
        TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()
    private val app: App = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: CounterRepository
    private val results = DelayedResults()
    private val deliveredDocuments = mutableListOf<String>()
    private val model = mutableStateOf<CounterViewModel?>(null)
    private var saveables = SaveableStateRegistry(null) { true }
    private var ownerA = 0L
    private var ownerB = 0L
    private var pendingCaptureFile: File? = null
    private var pdf: File? = null
    private val captureAllowed = mutableStateOf(true)
    private val regressionFiles = mutableListOf<File>()
    private val regressionTriggers = mutableListOf<String>()

    @Before
    fun setUp() {
        assertTrue(
            "Changing tests require an isolated profile",
            app.packageName.endsWith(".notestest") || app.packageName.endsWith(".activepolicytest"),
        )
        if (ProPolicyTestControl.isActive) setTestProStatus(ProStatus.PRO_PURCHASED)
        assertFalse(
            app.packageManager
                .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .contains(Manifest.permission.CAMERA),
        )
        val activity = rule.activity
        rule.runOnUiThread { activity.setContent { } }
        database = app.database.get()
        repository = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java).counterRepository()
        runBlocking(Dispatchers.IO) {
            ownerA =
                database.counterProjectDao().insert(
                    CounterProjectEntity(name = "External A ${UUID.randomUUID()}", count = 37),
                )
            ownerB =
                database.counterProjectDao().insert(
                    CounterProjectEntity(name = "External B ${UUID.randomUUID()}", count = 91),
                )
        }
        rule.runOnUiThread { model.value = freshModel(ownerA) }
        waitForOwner(ownerA)
    }

    @After
    fun tearDown() {
        rule.runOnUiThread {
            rule.activity.setContent { }
            rule.activity.viewModelStore.clear()
        }
        runBlocking(Dispatchers.IO) {
            regressionTriggers.forEach { database.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS $it") }
            if (ownerA > 0) repository.deleteProject(ownerA)
            if (ownerB > 0) repository.deleteProject(ownerB)
        }
        pendingCaptureFile?.delete()
        pdf?.delete()
        regressionFiles.forEach { it.delete() }
    }

    @Test
    fun rejectedPdfShowsDocumentsErrorAndExplicitReselectionSucceeds() {
        openPatternPicker(R.string.pattern_picker_import_pdf)
        val missing = File(app.cacheDir, "rw03b-missing-${UUID.randomUUID()}.pdf")
        rule.runOnUiThread { results.reply(results.document(), Activity.RESULT_OK, Uri.fromFile(missing)) }
        waitForText(R.string.project_documents_error_generic)
        rule.onNodeWithText(text(R.string.project_documents_error_generic)).assertIsDisplayed()
        assertTrue(documents(ownerA).isEmpty())
        assertTrue(documents(ownerB).isEmpty())
        rule.onNodeWithText(text(R.string.project_documents_add)).performClick()
        rule.onNodeWithText(text(R.string.project_documents_error_generic)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.pattern_picker_import_pdf)).performScrollTo().performClick()
        val uri = pdfFixture()
        rule.runOnUiThread { results.reply(results.document(), Activity.RESULT_OK, uri) }
        rule.waitUntil(10_000) { documents(ownerA).size == 1 }
        assertTrue(documents(ownerB).isEmpty())
    }

    @Test
    fun captureTargetFailureRendersFeedbackAndExplicitRetryLaunchesOnce() {
        val blocked = File(app.filesDir, "progress_photos/$ownerA")
        check(!blocked.exists())
        blocked.parentFile!!.mkdirs()
        blocked.writeText("Synthetic directory collision")
        regressionFiles += blocked
        showGallery()
        rule.onNodeWithContentDescription(photoAction()).performClick()
        waitForText(R.string.generic_error_unknown)
        rule.onNodeWithText(text(R.string.generic_error_unknown)).assertIsDisplayed()
        assertTrue(results.launches.isEmpty())
        assertTrue(photos(ownerA).isEmpty())
        assertTrue(blocked.delete())
        requestPhoto()
        assertEquals(1, results.launches.count { it.kind == "camera" })
        val request = results.camera()
        rule.runOnUiThread { results.reply(request, Activity.RESULT_CANCELED) }
        assertTrue(photos(ownerA).isEmpty())
    }

    @Test
    fun imageDraftsStayWithTheirProjectsAndLateGalleryResultIsConsumedOnce() {
        openPatternPicker(R.string.pattern_picker_choose_images, standalone = true)
        val first = results.launches.last { it.kind == "images" }
        val image = File(app.cacheDir, "rw03b-image-${UUID.randomUUID()}.jpg")
        regressionFiles += image
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        selectB()
        rule.runOnUiThread { results.reply(first, Activity.RESULT_OK, Uri.fromFile(image)) }
        rule.onNodeWithText(text(R.string.pattern_image_create_pdf)).assertDoesNotExist()
        rule.runOnUiThread { model.value!!.selectProjectById(ownerA) }
        waitForOwner(ownerA)
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasText(text(R.string.pattern_image_create_pdf))).fetchSemanticsNodes().isNotEmpty()
        }
        rule.runOnUiThread { results.reply(first, Activity.RESULT_OK, Uri.fromFile(image)) }
        rule
            .onNodeWithText(
                app.resources.getQuantityString(R.plurals.pattern_images_selected, 1, 1),
            ).assertIsDisplayed()
        selectB()
        rule.onNodeWithText(text(R.string.pattern_image_create_pdf)).assertDoesNotExist()
        rule.runOnUiThread { model.value!!.selectProjectById(ownerA) }
        waitForOwner(ownerA)
        rule.onNodeWithText(text(R.string.pattern_image_create_pdf)).performScrollTo().performClick()
        rule.waitUntil(15_000) { documents(ownerA).size == 1 }
        assertTrue(documents(ownerB).isEmpty())
        assertTrue(image.exists())
    }

    @Test
    fun bookmarkJumpFailureKeepsSheetAndRoomPositionUntilExplicitRetry() {
        val (viewer, _) = showRegressionViewer()
        for ((index, name) in listOf("First", "Middle", "Last").withIndex()) {
            rule.runOnUiThread { viewer.addBookmark(name, 0, 0.2f + index * 0.3f) }
            rule.waitUntil(
                5_000,
            ) { viewer.uiState.value.bookmarks.size == index + 1 && !viewer.uiState.value.isMutating }
        }
        val middle = viewer.uiState.value.bookmarks[1]
        rule.runOnUiThread { viewer.jumpToBookmark(middle.id) }
        rule.waitUntil(5_000) {
            viewer.uiState.value.selectedBookmarkId == middle.id &&
                !viewer.uiState.value.isMutating &&
                documents(ownerA).single().let {
                    it.readingLineYFraction == middle.yFraction && !it.readingLineFollowCurrentRow
                }
        }
        rule.waitForIdle()
        val before = documents(ownerA)
        val trigger = "rw03b_bookmark_$ownerA"
        regressionTriggers += trigger
        runBlocking(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER $trigger BEFORE UPDATE ON project_documents WHEN OLD.projectId = $ownerA " +
                    "BEGIN SELECT RAISE(ABORT, 'Synthetic bookmark failure'); END",
            )
        }
        rule.onNodeWithContentDescription(text(R.string.pattern_bookmarks)).performClick()
        for (action in listOf(
            "First",
            text(R.string.pattern_bookmark_previous),
            text(R.string.pattern_bookmark_next),
        )) {
            rule.onNodeWithText(action).performScrollTo().performClick()
            rule.waitUntil(5_000) { viewer.uiState.value.error != null && !viewer.uiState.value.isMutating }
            rule.onNodeWithText(text(R.string.generic_error_unknown)).assertIsDisplayed()
            assertEquals(before, documents(ownerA))
            rule.onNodeWithText(text(R.string.dismiss)).performClick()
        }
        runBlocking(Dispatchers.IO) { database.openHelper.writableDatabase.execSQL("DROP TRIGGER $trigger") }
        rule.onNodeWithText("First").performScrollTo().performClick()
        rule.waitUntil(5_000) { documents(ownerA).single().readingLineYFraction == 0.2f }
        rule.onNodeWithText(text(R.string.pattern_bookmark_add_here)).assertDoesNotExist()
    }

    @Test
    fun annotationFailureRemainsVisibleAfterDoneAndDismissDoesNotReplay() {
        val (_, annotations) = showRegressionViewer()
        val layer = requireNotNull(annotations.uiState.value.editableLayerId)
        val trigger = "rw03b_annotation_$ownerA"
        regressionTriggers += trigger
        runBlocking(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER $trigger BEFORE INSERT ON pattern_annotations WHEN NEW.layerId = $layer " +
                    "BEGIN SELECT RAISE(ABORT, 'Synthetic annotation failure'); END",
            )
        }
        rule.onNodeWithText(text(R.string.pattern_mark_up)).performClick()
        rule.runOnUiThread {
            annotations.setActiveTool(PatternAnnotationTool.PEN)
            annotations.beginStroke(NormalizedPatternPoint(0.2f, 0.2f))
            annotations.appendStrokePoint(NormalizedPatternPoint(0.4f, 0.4f))
            annotations.commitStroke(0f)
        }
        rule.waitUntil(5_000) { annotations.uiState.value.writeError == PatternAnnotationWriteError.WRITE_FAILED }
        rule.onNodeWithText(text(R.string.pattern_annotation_command_failed)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.pattern_mark_up_done)).performClick()
        rule.onNodeWithText(text(R.string.pattern_annotation_command_failed)).assertIsDisplayed()
        assertNull(annotations.uiState.value.inProgressAnnotation)
        assertFalse(annotations.uiState.value.canUndo)
        runBlocking(Dispatchers.IO) { database.openHelper.writableDatabase.execSQL("DROP TRIGGER $trigger") }
        rule.onNodeWithText(text(R.string.dismiss)).performClick()
        rule.waitForIdle()
        assertTrue(runBlocking(Dispatchers.IO) { database.patternAnnotationDao().getPageBounded(layer, 0).isEmpty() })
        assertNull(annotations.uiState.value.inProgressAnnotation)
        rule.onNodeWithText(text(R.string.pattern_annotation_command_failed)).assertDoesNotExist()
    }

    @Test
    fun exportDestinationFromOldDocumentCannotWriteAndCurrentDocumentExportsNativePdf() {
        val (viewer, annotations) = showRegressionViewer()
        requestExport()
        val stale = results.launches.last { it.kind == "export" }
        val original = documents(ownerA).single()
        val secondSource = pdfFixture()
        rule.runOnUiThread {
            model.value!!.attachPattern(secondSource.toString(), "Second", expectedProjectId = ownerA)
        }
        rule.waitUntil(5_000) { viewer.documentUiState.value.documents.size == 2 }
        val second =
            viewer.documentUiState.value.documents
                .first { it.id != original.id }
        rule.runOnUiThread { viewer.selectDocument(second.id) }
        rule.waitUntil(5_000) { viewer.documentUiState.value.selectedDocumentId == second.id }
        rule.waitForIdle()
        val target = File(app.cacheDir, "rw03b-export-${UUID.randomUUID()}.pdf")
        regressionFiles += target
        target.writeText("Unchanged destination")
        rule.runOnUiThread { results.reply(stale, Activity.RESULT_OK, Uri.fromFile(target)) }
        rule.waitForIdle()
        assertEquals("Unchanged destination", target.readText())
        assertFalse(annotations.uiState.value.exportFailed)
        requestExport()
        val current = results.launches.last { it.kind == "export" }
        rule.runOnUiThread { results.reply(current, Activity.RESULT_OK, Uri.fromFile(target)) }
        rule.waitUntil(15_000) { target.length() > 100 && !annotations.uiState.value.isExporting }
        android.os.ParcelFileDescriptor.open(target, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            android.graphics.pdf
                .PdfRenderer(descriptor)
                .use { assertEquals(1, it.pageCount) }
        }
        val bytes = target.readBytes()
        rule.runOnUiThread { results.reply(current, Activity.RESULT_OK, Uri.fromFile(target)) }
        rule.waitForIdle()
        org.junit.Assert.assertArrayEquals(bytes, target.readBytes())
    }

    private fun showRegressionViewer(): Pair<PatternViewerViewModel, PatternAnnotationViewModel> {
        val source = pdfFixture()
        rule.runOnUiThread {
            model.value!!.attachPattern(source.toString(), "RW03B PDF", expectedProjectId = ownerA)
        }
        rule.waitUntil(10_000) { documents(ownerA).size == 1 }
        lateinit var viewer: PatternViewerViewModel
        lateinit var annotations: PatternAnnotationViewModel
        rule.runOnUiThread {
            val extras =
                MutableCreationExtras(rule.activity.defaultViewModelCreationExtras).apply {
                    set(DEFAULT_ARGS_KEY, Bundle().apply { putLong("projectId", ownerA) })
                }
            val provider =
                ViewModelProvider(rule.activity.viewModelStore, rule.activity.defaultViewModelProviderFactory, extras)
            viewer = provider.get("rw03b-viewer-$ownerA", PatternViewerViewModel::class.java)
            annotations = provider.get("rw03b-annotations-$ownerA", PatternAnnotationViewModel::class.java)
            rule.activity.setContent {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
                    KnitToolsTheme {
                        PatternViewerScreen({}, {}, {}, {}, {}, {}, { model.value!! }, { viewer }, { annotations })
                    }
                }
            }
        }
        rule.waitUntil(10_000) {
            annotations.uiState.value.editableLayerId != null &&
                rule.onAllNodes(hasText(text(R.string.pattern_mark_up))).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        return viewer to annotations
    }

    private fun requestExport() {
        val count = results.launches.count { it.kind == "export" }
        rule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        rule.onNodeWithText(text(R.string.pattern_annotation_export_pdf)).performClick()
        rule.waitUntil(10_000) { results.launches.count { it.kind == "export" } == count + 1 }
        rule.waitForIdle()
    }

    private fun waitForText(id: Int) {
        rule.waitUntil(10_000) { rule.onAllNodes(hasText(text(id))).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun restoredProjectPhotoWaitsForRoomAndKeepsTheRealRowNumber() {
        showGallery()
        requestPhoto()
        val request = results.camera()
        writeJpeg(request.input as Uri)
        restoreSurface(gallery = true, beforeRegistration = { results.reply(request, Activity.RESULT_OK) })
        waitForOwner(ownerA)
        rule.waitUntil(5_000) { photos(ownerA).size == 1 }
        assertEquals(37, photos(ownerA).single().rowNumber)
        assertTrue(photos(ownerB).isEmpty())
        assertFalse(pendingCaptureFile!!.exists())
    }

    @Test
    fun lockedPhotoActionDoesNotLaunchCameraOrCreateTarget() {
        setPolicyStatus(ProStatus.TRIAL_EXPIRED)
        captureAllowed.value = false
        showGallery()
        rule.onNodeWithContentDescription(photoAction()).performClick()
        rule.waitForIdle()
        assertTrue(results.launches.isEmpty())
        assertTrue(photos(ownerA).isEmpty())
        assertTrue(photos(ownerB).isEmpty())
    }

    @Test
    fun trialStartContinuesPendingPhotoCaptureWithoutPermissionRequest() {
        setPolicyStatus(ProStatus.TRIAL_NOT_STARTED)
        ProPolicyTestControl.setTrialStartResult(TrialStartResult.Started)
        captureAllowed.value = false
        showGallery()
        rule.onNodeWithContentDescription(photoAction()).performClick()
        rule.onNodeWithText(text(R.string.pro_start_free_trial)).performClick()
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        val request = results.camera()
        writeJpeg(request.input as Uri)
        setPolicyStatus(ProStatus.TRIAL_EXPIRED)
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK) }
        rule.waitUntil(5_000) { photos(ownerA).size == 1 }
        assertEquals(37, photos(ownerA).single().rowNumber)
        assertTrue(photos(ownerB).isEmpty())
    }

    @Test
    fun photoLaunchFailuresCleanTargetsWithoutWritingRoom() {
        showGallery()
        for (failure in launchFailures()) {
            results.cameraFailure = failure
            requestPhoto()
            val file = requireNotNull(AppFileStorage.resolveAppOwnedFile(app, results.camera().input as Uri))
            rule.waitUntil(5_000) { !file.exists() }
            assertTrue(photos(ownerA).isEmpty())
            rule.runOnUiThread { results.launches.clear() }
        }
    }

    @Test
    fun photoFromAAfterSelectingBIsRejectedAndItsFileIsDeleted() {
        showGallery()
        requestPhoto()
        val request = results.camera()
        discardCaptureAfterSwitch(request)
        assertTrue(photos(ownerA).isEmpty())
        assertTrue(photos(ownerB).isEmpty())
    }

    @Test
    fun deletedPhotoOwnerIsNotRecreatedByALateResult() {
        showGallery()
        requestPhoto()
        val request = results.camera()
        deliverCaptureAfterOwnerDeletion(request)
        rule.waitUntil(5_000) { !pendingCaptureFile!!.exists() }
        assertNull(runBlocking(Dispatchers.IO) { repository.getProject(ownerA) })
        assertTrue(photos(ownerB).isEmpty())
        assertFalse(pendingCaptureFile!!.exists())
    }

    @Test
    fun repeatedCameraResultWritesOnlyOnePhoto() {
        showGallery()
        requestPhoto()
        val request = results.camera()
        writeJpeg(request.input as Uri)
        rule.runOnUiThread {
            results.reply(request, Activity.RESULT_OK)
            results.reply(request, Activity.RESULT_OK)
        }
        rule.waitUntil(5_000) { photos(ownerA).isNotEmpty() }
        rule.waitForIdle()
        assertEquals(1, photos(ownerA).size)
        assertEquals(37, photos(ownerA).single().rowNumber)
    }

    @Test
    fun cancelledCameraResultRemovesItsFileWithoutWritingRoom() {
        showGallery()
        requestPhoto()
        val request = results.camera()
        pendingCaptureFile = AppFileStorage.resolveAppOwnedFile(app, request.input as Uri)
        rule.runOnUiThread { results.reply(request, Activity.RESULT_CANCELED) }
        rule.waitUntil(5_000) { !pendingCaptureFile!!.exists() }
        assertTrue(photos(ownerA).isEmpty())
        assertTrue(photos(ownerB).isEmpty())
    }

    @Test
    fun restoredSafResultWaitsForRoomAndAttachesOnceToA() {
        openPatternPicker(R.string.pattern_picker_import_pdf)
        val request = results.document()
        val uri = pdfFixture()
        restoreSurface(gallery = false, beforeRegistration = { results.reply(request, Activity.RESULT_OK, uri) })
        waitForOwner(ownerA)
        rule.waitUntil(5_000) { documents(ownerA).size == 1 }
        assertEquals(1, documents(ownerA).size)
        assertTrue(documents(ownerB).isEmpty())
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK, uri) }
        rule.waitForIdle()
        assertEquals(1, documents(ownerA).size)
    }

    @Test
    fun safResultFromAAfterSelectingBDoesNotAttachToB() {
        openPatternPicker(R.string.pattern_picker_import_pdf, standalone = true)
        val request = results.document()
        val uri = pdfFixture()
        selectB()
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK, uri) }
        rule.waitForIdle()
        assertTrue("A's result must not reach B's attachment callback", deliveredDocuments.isEmpty())
        assertTrue(documents(ownerA).isEmpty())
        assertTrue(documents(ownerB).isEmpty())
    }

    @Test
    fun deletedSafOwnerIsNotRecreatedByALateResult() {
        openPatternPicker(R.string.pattern_picker_import_pdf)
        val request = results.document()
        val uri = pdfFixture()
        runBlocking(Dispatchers.IO) { repository.deleteProject(ownerA) }
        rule.waitUntil(5_000) {
            model.value!!
                .uiState.value.projectId != ownerA
        }
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK, uri) }
        rule.waitForIdle()
        assertNull(runBlocking(Dispatchers.IO) { repository.getProject(ownerA) })
        assertTrue(documents(ownerB).isEmpty())
    }

    @Test
    fun cancelledSafResultDoesNotWriteRoom() {
        openPatternPicker(R.string.pattern_picker_import_cloud_pdf)
        rule.runOnUiThread { results.reply(results.document(), Activity.RESULT_CANCELED) }
        rule.waitForIdle()
        assertTrue(documents(ownerA).isEmpty())
        assertTrue(documents(ownerB).isEmpty())
    }

    @Test
    fun restoredPatternCameraResultKeepsItsPageWhileRoomRestores() {
        val request = preparePatternCapture()
        writeJpeg(request.input as Uri)
        restoreSurface(gallery = false, beforeRegistration = { results.reply(request, Activity.RESULT_OK) })
        waitForOwner(ownerA)
        rule.waitUntil(5_000) {
            rule
                .onAllNodes(hasText(rule.activity.resources.getQuantityString(R.plurals.pattern_images_selected, 1, 1)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertTrue(documents(ownerA).isEmpty())
        assertTrue(documents(ownerB).isEmpty())
        assertTrue(pendingCaptureFile!!.exists())
    }

    @Test
    fun lockedPatternPhotoActionDoesNotLaunchCameraOrCreateTarget() {
        setPolicyStatus(ProStatus.TRIAL_EXPIRED)
        captureAllowed.value = false
        openPatternPicker(R.string.pattern_picker_camera_scan, standalone = true)
        rule.waitForIdle()
        assertTrue(results.launches.isEmpty())
        assertTrue(documents(ownerA).isEmpty())
    }

    @Test
    fun trialCameraRequestUsesExistingCaptureOnceAcrossSurfaceRestoration() {
        mount(gallery = false, standalone = true, initialCameraRequestId = "trial-camera")
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        val request = results.camera()
        rule.runOnUiThread {
            val saved = saveables.performSave()
            rule.activity.setContent { }
            saveables = SaveableStateRegistry(saved) { true }
        }
        rule.waitForIdle()
        mount(gallery = false, standalone = true, initialCameraRequestId = "trial-camera")
        assertEquals(1, results.launches.count { it.kind == "camera" })
        cancelPatternCapture(request)
    }

    @Test
    fun trialStartContinuesPendingPatternCaptureWithoutPermissionRequest() {
        setPolicyStatus(ProStatus.TRIAL_NOT_STARTED)
        ProPolicyTestControl.setTrialStartResult(TrialStartResult.Started)
        captureAllowed.value = false
        openPatternPicker(R.string.pattern_picker_camera_scan, standalone = true)
        rule.onNodeWithText(text(R.string.pro_start_free_trial)).performClick()
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        val request = results.camera()
        cancelPatternCapture(request)
    }

    @Test
    fun cancelledPatternCameraResultRemovesItsFileWithoutAttachment() {
        val request = preparePatternCapture(standalone = true)
        cancelPatternCapture(request)
    }

    @Test
    fun patternLaunchFailuresCleanTargetsWithoutAttachment() {
        for (failure in launchFailures()) {
            results.cameraFailure = failure
            val request = preparePatternCapture(standalone = true)
            val file = requireNotNull(AppFileStorage.resolveAppOwnedFile(app, request.input as Uri))
            rule.waitUntil(5_000) { !file.exists() }
            assertTrue(documents(ownerA).isEmpty())
            rule.runOnUiThread { results.launches.clear() }
        }
    }

    @Test
    fun deletedPatternPhotoOwnerDiscardsLateResult() {
        val request = preparePatternCapture(standalone = true)
        deliverCaptureAfterOwnerDeletion(request)
        rule.waitUntil(5_000) { !requireNotNull(pendingCaptureFile).exists() }
        assertNull(runBlocking(Dispatchers.IO) { repository.getProject(ownerA) })
        assertTrue(documents(ownerB).isEmpty())
    }

    @Test
    fun admittedPatternResultCreatesOneReadablePdfAfterUiGateCloses() {
        val request = preparePatternCapture(standalone = true)
        writeJpeg(request.input as Uri)
        rule.runOnUiThread {
            captureAllowed.value = false
            results.reply(request, Activity.RESULT_OK)
            results.reply(request, Activity.RESULT_OK)
        }
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText(text(R.string.pattern_image_create_pdf))).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text(R.string.pattern_image_create_pdf)).performScrollTo().performClick()
        rule.waitUntil(10_000) { documents(ownerA).size == 1 }
        val document = documents(ownerA).single()
        requireNotNull(app.contentResolver.openFileDescriptor(Uri.parse(document.localPdfUri), "r")).use { descriptor ->
            android.graphics.pdf
                .PdfRenderer(descriptor)
                .use { assertEquals(1, it.pageCount) }
        }
        assertTrue(documents(ownerB).isEmpty())
        rule.waitUntil(5_000) { !requireNotNull(pendingCaptureFile).exists() }
    }

    @Test
    fun patternPhotoFromAAfterSelectingBIsDiscarded() {
        val request = preparePatternCapture(standalone = true)
        discardCaptureAfterSwitch(request)
        assertTrue(documents(ownerA).isEmpty())
        assertTrue(documents(ownerB).isEmpty())
    }

    private fun showGallery() = mount(gallery = true)

    private fun mount(
        gallery: Boolean,
        restoreModel: Boolean = false,
        standalone: Boolean = false,
        initialCameraRequestId: String? = null,
    ) {
        rule.runOnUiThread {
            rule.activity.setContent {
                val current =
                    if (restoreModel) {
                        remember {
                            freshModel(
                                ownerA,
                            ).also { model.value = it }
                        }
                    } else {
                        model.value!!
                    }
                val state by current.uiState.collectAsState()
                val photos by current.allPhotos.collectAsState()
                CompositionLocalProvider(
                    LocalSaveableStateRegistry provides saveables,
                    LocalActivityResultRegistryOwner provides results,
                ) {
                    KnitToolsTheme(isDarkTheme = false) {
                        if (gallery) {
                            PhotoGalleryScreen(
                                errorsProvider = { current.photoErrors },
                                photos = photos,
                                projectId = state.projectId,
                                canCreatePhoto = captureAllowed.value,
                                proState = ProState(status = ProStatus.PRO_PURCHASED),
                                proAccessReady = true,
                                onBack = {},
                                onSeePro = {},
                                actions =
                                    PhotoGalleryActions(
                                        authorizePhotoCreation = { model.value!!.authorizeProgressPhotoCreation() },
                                        cancelPhotoCreation = { model.value!!.cancelProgressPhotoCreation() },
                                        createPhotoCaptureTarget = { id, callback ->
                                            model.value!!.createPhotoCaptureTarget(id, callback)
                                        },
                                        deletePendingPhotoFile = { model.value!!.deletePendingPhotoFile(it) },
                                        savePhoto = { uri, id -> model.value!!.savePhoto(uri, id) },
                                        deletePhoto = { model.value!!.deletePhoto(it) },
                                    ),
                            )
                        } else if (standalone) {
                            PatternPickerSheet(
                                projectId = state.projectId,
                                initialCameraRequestId = initialCameraRequestId,
                                savedPatterns = emptyList(),
                                canUseCameraScan = captureAllowed.value,
                                proState = ProState(status = ProStatus.PRO_PURCHASED),
                                proAccessReady = true,
                                hasExistingPattern = false,
                                onSavedPatternSelected = {},
                                isRestoringProject = state.isRestoringProject,
                                onDocumentSelected = { uri, name, owner ->
                                    deliveredDocuments += uri
                                    current.attachPattern(uri, name, expectedProjectId = owner)
                                },
                                onImportFromRavelry = {},
                                onSeePro = {},
                                onDismiss = {},
                            )
                        } else {
                            ProjectContentHost(CounterScreenActions(), { current }, overview = false)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun restoreSurface(
        gallery: Boolean,
        beforeRegistration: () -> Unit,
    ) {
        rule.runOnUiThread {
            val saved = saveables.performSave()
            rule.activity.setContent { }
            saveables = SaveableStateRegistry(saved) { true }
        }
        rule.waitForIdle()
        rule.runOnUiThread { beforeRegistration() }
        mount(gallery, restoreModel = true)
    }

    private fun freshModel(owner: Long): CounterViewModel {
        val args =
            Bundle().apply {
                putLong("counter.selected_project_id", owner)
                putBoolean("counter.progress_photo_creation_authorized", true)
                putLong("counter.progress_photo_creation_project_id", owner)
            }
        val extras =
            MutableCreationExtras(
                rule.activity.defaultViewModelCreationExtras,
            ).apply { set(DEFAULT_ARGS_KEY, args) }
        return ViewModelProvider(rule.activity.viewModelStore, rule.activity.defaultViewModelProviderFactory, extras)
            .get("external-results-${UUID.randomUUID()}", CounterViewModel::class.java)
    }

    private fun waitForOwner(owner: Long) {
        rule.waitUntil(5_000) {
            model.value
                ?.uiState
                ?.value
                ?.let { it.projectId == owner && !it.isRestoringProject } ==
                true
        }
        rule.waitForIdle()
    }

    private fun selectB() {
        rule.runOnUiThread { model.value!!.selectProjectById(ownerB) }
        waitForOwner(ownerB)
    }

    private fun requestPhoto() {
        rule.onNodeWithContentDescription(photoAction()).performClick()
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        rule.waitForIdle()
    }

    private fun openPatternPicker(
        action: Int,
        standalone: Boolean = false,
    ) {
        mount(gallery = false, standalone = standalone)
        if (!standalone) {
            rule
                .onNode(
                    hasScrollToIndexAction(),
                ).performScrollToNode(hasText(text(R.string.project_content_add_pattern)))
            rule.onNodeWithText(text(R.string.project_content_add_pattern)).performClick()
        }
        val expandable = SemanticsMatcher.keyIsDefined(SemanticsActions.Expand)
        if (rule.onAllNodes(expandable).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNode(expandable).performSemanticsAction(SemanticsActions.Expand) { it() }
        }
        rule.onNodeWithText(text(action)).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun photoAction(): String = text(R.string.take_photo)

    private fun text(id: Int): String = rule.activity.getString(id)

    private fun writeJpeg(uri: Uri) {
        val file = requireNotNull(AppFileStorage.resolveAppOwnedFile(app, uri))
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        pendingCaptureFile = file
    }

    private fun pdfFixture(): Uri {
        val file = File(app.cacheDir, "external-results-${UUID.randomUUID()}.pdf")
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(100, 100, 1).create())
            page.canvas.drawColor(android.graphics.Color.WHITE)
            document.finishPage(page)
            file.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        pdf = file
        regressionFiles += file
        return Uri.fromFile(file)
    }

    private fun photos(owner: Long) =
        runBlocking(Dispatchers.IO) {
            database.progressPhotoDao().getAllPhotosOnce().filter {
                it.projectId ==
                    owner
            }
        }

    private fun documents(owner: Long) =
        runBlocking(Dispatchers.IO) { database.projectDocumentDao().getForProject(owner) }

    private data class Launch(
        val code: Int,
        val kind: String,
        val input: Any?,
    )

    private class DelayedResults :
        ActivityResultRegistry(),
        ActivityResultRegistryOwner {
        val launches = mutableListOf<Launch>()
        var cameraFailure: RuntimeException? = null
        override val activityResultRegistry: ActivityResultRegistry get() = this

        override fun <I, O> onLaunch(
            code: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            val kind =
                when (contract) {
                    is ActivityResultContracts.TakePicture -> "camera"
                    is ActivityResultContracts.OpenDocument -> "document"
                    is ActivityResultContracts.PickMultipleVisualMedia -> "images"
                    is ActivityResultContracts.CreateDocument -> "export"
                    else -> error("Unexpected external contract ${contract.javaClass.simpleName}")
                }
            launches += Launch(code, kind, input)
            if (kind == "camera") cameraFailure?.let { throw it }
        }

        fun camera() = launches.last { it.kind == "camera" }

        fun document() = launches.last { it.kind == "document" }

        fun reply(
            request: Launch,
            code: Int,
            uri: Uri? = null,
        ) {
            dispatchResult(request.code, code, Intent().setData(uri))
        }
    }

    private fun deliverCaptureAfterOwnerDeletion(request: Launch) {
        writeJpeg(request.input as Uri)
        runBlocking(Dispatchers.IO) { repository.deleteProject(ownerA) }
        rule.waitUntil(5_000) {
            model.value!!
                .uiState.value.projectId != ownerA
        }
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK) }
    }

    private fun cancelPatternCapture(request: Launch) {
        val file = requireNotNull(AppFileStorage.resolveAppOwnedFile(app, request.input as Uri))
        rule.runOnUiThread { results.reply(request, Activity.RESULT_CANCELED) }
        rule.waitUntil(5_000) { !file.exists() }
        assertTrue(documents(ownerA).isEmpty())
    }

    private fun preparePatternCapture(standalone: Boolean = false): Launch {
        openPatternPicker(R.string.pattern_picker_camera_scan, standalone = standalone)
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        return results.camera()
    }

    private fun discardCaptureAfterSwitch(request: Launch) {
        writeJpeg(request.input as Uri)
        selectB()
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK) }
        rule.waitUntil(5_000) { !requireNotNull(pendingCaptureFile).exists() }
    }

    private fun launchFailures(): List<RuntimeException> =
        listOf(
            ActivityNotFoundException("No handler"),
            IllegalStateException("Unavailable"),
            SecurityException("Denied"),
        )

    private fun setPolicyStatus(status: ProStatus) {
        assumeTrue("Entitlement checks require the activepolicy profile", ProPolicyTestControl.isActive)
        setTestProStatus(status)
        runBlocking {
            withTimeout(5_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == status }
            }
        }
    }
}
