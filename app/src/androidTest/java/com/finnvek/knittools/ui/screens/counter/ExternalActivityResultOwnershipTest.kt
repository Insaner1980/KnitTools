package com.finnvek.knittools.ui.screens.counter

import android.Manifest
import android.app.Activity
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
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.ui.screens.pattern.PatternPickerSheet
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.io.File
import java.util.UUID

class ExternalActivityResultOwnershipTest {
    // These tests replace activity content; its startup splash must not block the test clock's first draw.
    @get:Rule(order = 0)
    val splash =
        object : ExternalResource() {
            private val callback =
                ActivityLifecycleCallback { activity, stage ->
                    if (activity is MainActivity && stage == Stage.CREATED) {
                        MainActivity::class.java
                            .getDeclaredField(
                                "startupThemeLoaded",
                            ).apply { isAccessible = true }
                            .setBoolean(activity, true)
                    }
                }

            override fun before() = ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)

            override fun after() = ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback)
        }

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

    @Before
    fun setUp() {
        assertTrue("Changing tests require the isolated notes profile", app.packageName.endsWith(".notestest"))
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
            if (ownerA > 0) repository.deleteProject(ownerA)
            if (ownerB > 0) repository.deleteProject(ownerB)
        }
        pendingCaptureFile?.delete()
        pdf?.delete()
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
    fun permissionFromAAfterSelectingBDoesNotLaunchBsCamera() {
        showGallery()
        rule.onNodeWithContentDescription(photoAction()).performClick()
        val request = results.permission()
        selectB()
        rule.runOnUiThread { results.grant(request) }
        rule.waitForIdle()
        assertTrue("A's permission must not launch a camera for B", results.launches.none { it.kind == "camera" })
        assertTrue(photos(ownerA).isEmpty())
        assertTrue(photos(ownerB).isEmpty())
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
        writeJpeg(request.input as Uri)
        runBlocking(Dispatchers.IO) { repository.deleteProject(ownerA) }
        rule.waitUntil(5_000) {
            model.value!!
                .uiState.value.projectId != ownerA
        }
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK) }
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
    fun patternPermissionFromAAfterSelectingBDoesNotLaunchCamera() {
        openPatternPicker(R.string.pattern_picker_camera_scan, standalone = true)
        val request = results.permission()
        selectB()
        rule.runOnUiThread { results.grant(request) }
        rule.waitForIdle()
        assertTrue(results.launches.none { it.kind == "camera" })
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
                                photos = photos,
                                projectId = state.projectId,
                                canCreatePhoto = true,
                                proStatus = ProStatus.PRO_PURCHASED,
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
                                savedPatterns = emptyList(),
                                canUseCameraScan = true,
                                proStatus = ProStatus.PRO_PURCHASED,
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
        rule.runOnUiThread { results.grant(results.permission()) }
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
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performSemanticsAction(SemanticsActions.Expand) { it() }
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
        override val activityResultRegistry: ActivityResultRegistry get() = this

        override fun <I, O> onLaunch(
            code: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            val kind =
                when (contract) {
                    is ActivityResultContracts.RequestPermission -> "permission"
                    is ActivityResultContracts.TakePicture -> "camera"
                    is ActivityResultContracts.OpenDocument -> "document"
                    else -> error("Unexpected external contract ${contract.javaClass.simpleName}")
                }
            launches += Launch(code, kind, input)
        }

        fun permission() = launches.last { it.kind == "permission" }

        fun camera() = launches.last { it.kind == "camera" }

        fun document() = launches.last { it.kind == "document" }

        fun grant(request: Launch) {
            dispatchResult(
                request.code,
                Activity.RESULT_OK,
                Intent()
                    .putExtra(
                        "androidx.activity.result.contract.extra.PERMISSIONS",
                        arrayOf(Manifest.permission.CAMERA),
                    ).putExtra(
                        "androidx.activity.result.contract.extra.PERMISSION_GRANT_RESULTS",
                        intArrayOf(PackageManager.PERMISSION_GRANTED),
                    ),
            )
        }

        fun reply(
            request: Launch,
            code: Int,
            uri: Uri? = null,
        ) {
            dispatchResult(request.code, code, Intent().setData(uri))
        }
    }

    private fun preparePatternCapture(standalone: Boolean = false): Launch {
        openPatternPicker(R.string.pattern_picker_camera_scan, standalone = standalone)
        rule.runOnUiThread { results.grant(results.permission()) }
        rule.waitUntil(5_000) { results.launches.any { it.kind == "camera" } }
        return results.camera()
    }

    private fun discardCaptureAfterSwitch(request: Launch) {
        writeJpeg(request.input as Uri)
        selectB()
        rule.runOnUiThread { results.reply(request, Activity.RESULT_OK) }
        rule.waitUntil(5_000) { !requireNotNull(pendingCaptureFile).exists() }
    }
}
