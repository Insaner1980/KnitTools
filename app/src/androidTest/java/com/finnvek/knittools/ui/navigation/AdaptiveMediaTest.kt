package com.finnvek.knittools.ui.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.PatternAnnotationLayerEntity
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.pro.ProPolicyTestControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Actual media routes with local fixtures; never use the personal application package. */
class AdaptiveMediaTest : AdaptiveRouteTestFixture() {
    private val matrix = listOf(360 to 800, 800 to 400, 800 to 1280, 1280 to 800, 1600 to 900)
    private val vertical =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            !hasTestTag("navigation-rail") and !hasAnyAncestor(hasTestTag("navigation-rail"))
    private val fixtures by lazy { File(app.filesDir, "adaptive-media-fixtures").apply { mkdirs() } }

    @Before
    fun seed() {
        prepareResize()
        runBlocking(Dispatchers.IO) {
            setAdaptivePreferences()
            db.clearAllTables()
            db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Media cardigan", count = 24))
            db.counterProjectDao().insert(CounterProjectEntity(id = 2, name = "Media shawl"))
            val photo = makePhoto()
            for (id in 1..100) {
                db.progressPhotoDao().insert(
                    ProgressPhotoEntity(
                        id = id.toLong(),
                        projectId = if (id % 2 == 1) 1 else 2,
                        photoUri = photo,
                        rowNumber = id,
                        note = "Photo $id",
                        createdAt = 1000L - id,
                    ),
                )
            }
            for (id in 1..2) {
                val uri = makePdf(id)
                db.projectDocumentDao().insert(
                    ProjectDocumentEntity(
                        id = id.toLong(),
                        projectId = 1,
                        savedPatternId = null,
                        documentKey = "media-document-$id",
                        label = "Media PDF $id",
                        localPdfUri = uri,
                        sortOrder = id,
                        isPrimary = id == 1,
                        currentPage = if (id == 1) 0 else 1,
                        rowMapping = "[]",
                        readingLineEnabled = true,
                        readingLineYFraction = 0.63f,
                        readingLineFollowCurrentRow = true,
                        verticalReadingGuideEnabled = true,
                        verticalReadingGuideXFraction = 0.37f,
                        createdAt = 1,
                        updatedAt = 1,
                    ),
                )
            }
            db.patternAnnotationLayerDao().insert(
                PatternAnnotationLayerEntity(
                    id = 1,
                    projectId = 1,
                    savedPatternId = null,
                    documentKey = "media-document-2",
                    isActive = true,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
            db.patternAnnotationDao().insert(
                PatternAnnotation(
                    layerId = 1,
                    page = 1,
                    kind = PatternAnnotationKind.FREEHAND,
                    payload =
                        FreehandPayload(
                            listOf(NormalizedPatternPoint(0.2f, 0.3f), NormalizedPatternPoint(0.8f, 0.3f)),
                            Color.RED,
                            3f,
                        ),
                    zIndex = 0,
                    createdAt = 1,
                    updatedAt = 1,
                ).toEntity(),
            )
            db.patternBookmarkDao().insert(
                PatternBookmarkEntity(
                    projectId = 1,
                    documentKey = "media-document-2",
                    name = "Sleeve checkpoint",
                    pageIndex = 1,
                    yFraction = 0.63f,
                    createdAt = 1,
                ),
            )
        }
        resize(360, 800)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Media cardigan").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun restore() {
        if (!initialized) return
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        shell("wm size ${originalSize ?: "reset"}")
        ProPolicyTestControl.setPurchased(false)
    }

    @Test
    fun allPhotosRetainsFilterSelectionAndVisibleItemAcrossResizingAndRecreation() {
        tab(TopLevelDestination.Library)
        text(R.string.all_photos_title).performClick()
        waitText(label(R.string.all_projects))
        text(R.string.all_projects).performClick()
        rule.onNode(hasText("Media cardigan") and hasAnyAncestor(isPopup())).performClick()
        list().performScrollToIndex(24)
        text("Photo 49").performTouchInput { longClick() }
        for ((width, height) in matrix) {
            resize(width, height)
            rule.activityRule.scenario.recreate()
            waitText("Photo 49")
            text("Photo 49").assertIsDisplayed().assertIsSelected()
            assertThumbnails()
            text(R.string.delete).assertIsDisplayed()
            screenshot("gallery-$width-$height")
        }
        back()
        text("Media cardigan").assertIsDisplayed()
        assertEquals(100, runBlocking { db.progressPhotoDao().getAllPhotosOnce().size })
    }

    @Test
    fun projectGalleryViewerAndRenameDraftRemainUsableAcrossTheMatrix() {
        openProjectPhotos()
        list().performScrollToIndex(12)
        for ((width, height) in matrix) {
            resize(width, height)
            rule.activityRule.scenario.recreate()
            waitText("Photo 25")
            text("Photo 25").assertIsDisplayed()
            assertThumbnails()
        }
        val captionsBefore = photoCaptions()
        text("Photo 25").performTouchInput { longClick() }
        rule.onNode(hasSetTextAction()).performTextReplacement("Retained photo caption")
        rule.onNode(hasSetTextAction()).performClick()
        keyboard(true)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performScrollTo().assertTextContains("Retained photo caption")
        Espresso.closeSoftKeyboard()
        keyboard(false)
        text(R.string.cancel).performScrollTo().performClick()
        screenshot("gallery-after-caption")
        val anchorAfterCaption = list().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue("Caption editing must not reset the gallery to the beginning", anchorAfterCaption > 0)
        val captionsAfter = photoCaptions()
        assertTrue(
            "Resizing preserves a nearby photo row: $captionsBefore -> $captionsAfter",
            captionsBefore.intersect(captionsAfter).isNotEmpty(),
        )
        list().performScrollToNode(hasText("Photo 25"))
        text("Photo 25").performClick()
        for ((width, height) in matrix) {
            resize(width, height)
            rule.activityRule.scenario.recreate()
            rule.waitUntil(10_000) {
                rule.onAllNodes(hasTestTag("photo-viewport")).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("photo-viewport").assertIsDisplayed().performTouchInput {
                down(0, center - Offset(30f, 0f))
                down(1, center + Offset(30f, 0f))
                moveTo(0, center - Offset(90f, 0f))
                moveTo(1, center + Offset(90f, 0f))
                up(0)
                up(1)
            }
            text(R.string.share_photo).assertIsDisplayed()
            text(R.string.cancel).assertIsDisplayed()
            screenshot("photo-$width-$height")
        }
        text(R.string.cancel).performClick()
        text("Photo 25").assertIsDisplayed()
    }

    @Test
    fun pdfKeepsSecondaryDocumentPageGuidesBookmarksAndStoredGeometry() {
        openSecondaryPdf()
        val before = documents()
        val annotations = runBlocking { db.patternAnnotationDao().getPageBounded(1, 1) }
        for ((width, height) in matrix) {
            resize(width, height)
            runBlocking {
                app.preferencesManager.get().setThemeMode(
                    if (width >=
                        1280
                    ) {
                        ThemeMode.DARK
                    } else {
                        ThemeMode.LIGHT
                    },
                )
            }
            rule.activityRule.scenario.recreate()
            waitText("Media PDF 2")
            waitText(label(R.string.pattern_mark_up))
            text(rule.activity.getString(R.string.pattern_page_indicator, 2, 3)).assertIsDisplayed()
            text(R.string.pattern_next_page).assertIsDisplayed()
            text(R.string.pattern_bookmarks).assertIsDisplayed()
            text(R.string.pattern_mark_up).performClick()
            text(R.string.pattern_mark_up_done).assertIsDisplayed()
            rule.onNodeWithContentDescription(label(R.string.pattern_annotation_undo)).assertIsDisplayed()
            rule.onNodeWithContentDescription(label(R.string.pattern_annotation_redo)).assertIsDisplayed()
            menu()
            scrollTo(label(R.string.pattern_annotation_clear_page)).assertIsDisplayed()
            back()
            screenshot("pdf-markup-$width-$height")
            text(R.string.pattern_mark_up_done).performClick()
            menu()
            scrollTo(label(R.string.pattern_annotation_export_pdf)).assertIsDisplayed()
            back()
            assertEquals(before, documents())
            assertEquals(annotations, runBlocking { db.patternAnnotationDao().getPageBounded(1, 1) })
        }
        text(R.string.pattern_bookmarks).performClick()
        text("Sleeve checkpoint").assertIsDisplayed()
        text(R.string.pattern_bookmark_add_here).performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Retained bookmark draft")
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performScrollTo().assertTextContains("Retained bookmark draft")
        Espresso.closeSoftKeyboard()
        keyboard(false)
        text(R.string.cancel).performScrollTo().performClick()
        back()
        assertEquals(before, documents())
        menu()
        scrollTo(label(R.string.pattern_annotation_export_pdf)).performClick()
        rule.waitUntil(10_000) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation.rootInActiveWindow
                ?.packageName
                ?.toString()
                ?.contains("documentsui") == true
        }
        // The first Back can close the document picker's keyboard without closing the picker.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(3) {
            if (automation.rootInActiveWindow
                    ?.packageName
                    ?.toString()
                    ?.contains("documentsui") == true
            ) {
                shell("input keyevent 4")
                val deadline = SystemClock.uptimeMillis() + 1_000
                while (SystemClock.uptimeMillis() < deadline &&
                    automation.rootInActiveWindow?.packageName?.toString() != app.packageName
                ) {
                    SystemClock.sleep(50)
                }
            }
        }
        waitText("Media PDF 2")
        assertEquals(before, documents())
    }

    @Test
    fun displayDensityChangesKeepDocumentGuidesAndAnnotations() {
        val originalDensity =
            shell("wm density")
                .lineSequence()
                .firstOrNull { it.startsWith("Override density:") }
                ?.substringAfter(":")
                ?.trim()
        openSecondaryPdf()
        val before = documents()
        val annotations = runBlocking { db.patternAnnotationDao().getPageBounded(1, 1) }
        try {
            shell("wm size 1280x800")
            for (dpi in listOf(160, 240, 200, 160)) {
                shell("wm density $dpi")
                rule.waitUntil(15_000) { rule.activity.resources.configuration.densityDpi == dpi }
                // Display-manager cross-fades are outside Compose's idle synchronization.
                SystemClock.sleep(1_000)
                rule.waitForIdle()
                waitText("Media PDF 2")
                text(rule.activity.getString(R.string.pattern_page_indicator, 2, 3)).assertIsDisplayed()
                text(R.string.pattern_bookmarks).assertIsDisplayed()
                assertEquals(before, documents())
                assertEquals(annotations, runBlocking { db.patternAnnotationDao().getPageBounded(1, 1) })
                val config = rule.activity.resources.configuration
                println("ADAPTIVE_DENSITY dpi=$dpi window=${config.screenWidthDp}x${config.screenHeightDp}")
                screenshot("pdf-density-$dpi")
            }
        } finally {
            shell("wm density ${originalDensity ?: "reset"}")
        }
    }

    @Test
    fun calibrationDraftRetainsItsFirstPointAcrossShortWindowRecreation() {
        openSecondaryPdf()
        rule.onNodeWithContentDescription(label(R.string.pattern_horizontal_reading_line)).performClick()
        scrollTo(label(R.string.pattern_calibrate_rows)).performClick()
        // Starting calibration explicitly enables the guide and updates its timestamp.
        rule.waitUntil(10_000) { documents().last().updatedAt > 1 }
        val before = documents()
        rule.onNode(hasSetTextAction()).performTextReplacement("20")
        Espresso.closeSoftKeyboard()
        keyboard(false)
        text(R.string.save).performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("31")
        Espresso.closeSoftKeyboard()
        keyboard(false)
        resize(800, 400)
        rule.activityRule.scenario.recreate()
        waitText(label(R.string.pattern_calibration_step_last))
        rule.onNode(hasSetTextAction()).assertTextContains("31")
        text(R.string.pattern_calibration_step_last).assertIsDisplayed()
        text(R.string.cancel).performClick()
        assertEquals(before, documents())
    }

    @Test
    fun photoAndPdfSeparateWindowsAvoidBookAndTabletopHinges() {
        resize(1280, 800)
        openProjectPhotos()
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold = showFold(bounds, orientation)
            text("Photo 1").performClick()
            text(R.string.cancel).assertIsDisplayed()
            assertWindowOutside(fold)
            text(R.string.delete_photo).performClick()
            text(R.string.cancel).assertIsDisplayed()
            assertWindowOutside(fold)
            text(R.string.cancel).performClick()
            text(R.string.cancel).performClick()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
        back()
        back()
        openSecondaryPdf()
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold = FoldingFeature(bounds, center = 220, size = 24, orientation = orientation)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            rule.waitForIdle()
            menu()
            scrollTo(label(R.string.pattern_annotation_export_pdf))
            assertWindowOutside(fold)
            back()
            text(R.string.pattern_bookmarks).performClick()
            text("Sleeve checkpoint").performScrollTo().assertIsDisplayed()
            assertWindowOutside(fold)
            text(
                rule.activity.getString(
                    R.string.pattern_bookmark_action_accessibility_description,
                    label(R.string.more_options),
                    "Sleeve checkpoint",
                ),
            ).performClick()
            text(R.string.pattern_bookmark_rename).assertIsDisplayed()
            assertWindowOutside(fold)
            back()
            text(R.string.pattern_bookmark_add_here).performClick()
            text(R.string.cancel).performScrollTo().assertIsDisplayed()
            assertWindowOutside(fold)
            text(R.string.cancel).performClick()
            back()
            menu()
            scrollTo(label(R.string.pattern_annotations_layers)).performClick()
            text(R.string.pattern_annotations_layers).assertIsDisplayed()
            assertWindowOutside(fold)
            back()
            menu()
            scrollTo(label(R.string.pattern_page_jump)).performClick()
            text(R.string.cancel).performScrollTo().assertIsDisplayed()
            assertWindowOutside(fold)
            text(R.string.cancel).performClick()
            menu()
            scrollTo(label(R.string.project_documents_manage)).performClick()
            text("Media PDF 2").performScrollTo().assertIsDisplayed()
            assertWindowOutside(fold)
            text(rule.activity.getString(R.string.project_documents_actions, "Media PDF 2")).performClick()
            scrollTo(label(R.string.project_documents_rename)).performClick()
            text(R.string.cancel).performScrollTo().assertIsDisplayed()
            assertWindowOutside(fold)
            text(R.string.cancel).performClick()
            back()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
    }

    private fun openProjectPhotos() {
        text("Media cardigan").performClick()
        rule
            .onNode(
                SemanticsMatcher("Overview photo action") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == label(R.string.project_overview_open_photos)
                },
            ).performClick()
        waitText("Photo 1")
    }

    @Test
    fun galleryFilterAndIncomingPdfSheetUseTheirOwnSafeWindows() {
        resize(1280, 800)
        tab(TopLevelDestination.Library)
        text(R.string.all_photos_title).performClick()
        waitText(label(R.string.all_projects))
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity).bounds
        for (orientation in listOf(FoldingFeature.Orientation.VERTICAL, FoldingFeature.Orientation.HORIZONTAL)) {
            val fold = showFold(bounds, orientation)
            text(R.string.all_projects).performClick()
            rule.onNode(hasText("Media cardigan") and hasAnyAncestor(isPopup())).assertIsDisplayed()
            assertWindowOutside(fold)
            back()
            val incomingSource = File(app.cacheDir, "incoming-media.pdf")
            File(checkNotNull(Uri.parse(makePdf(3)).path)).copyTo(incomingSource, overwrite = true)
            rule.runOnIdle {
                ViewModelProvider(rule.activity)[IncomingPdfViewModel::class.java].receive(Uri.fromFile(incomingSource))
            }
            waitText(label(R.string.incoming_pdf_title))
            // The test publisher has no replay; deliver the current posture to the new host collector.
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
            rule.waitForIdle()
            list().performTouchInput { swipeUp() }
            scrollTo(label(R.string.cancel))
            assertWindowOutside(fold)
            val pending =
                rule.runOnIdle {
                    ViewModelProvider(
                        rule.activity,
                    )[IncomingPdfViewModel::class.java].pending.value
                }
            val copiedFile = File(checkNotNull(Uri.parse(checkNotNull(pending).localUri).path))
            assertTrue(copiedFile.isFile)
            text(R.string.cancel).performClick()
            rule.waitUntil(10_000) { !copiedFile.exists() }
            incomingSource.delete()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(emptyList()))
        }
    }

    private fun openSecondaryPdf() {
        text("Media cardigan").performClick()
        scrollTo(label(R.string.project_documents_manage)).performClick()
        scrollTo("Media PDF 2").performClick()
        waitText(label(R.string.pattern_mark_up))
        text("Media PDF 2").assertIsDisplayed()
    }

    private fun documents() = runBlocking { db.projectDocumentDao().getForProject(1) }

    private fun menu() = text(R.string.more_options).performClick()

    private fun back() {
        shell("input keyevent 4")
        rule.waitForIdle()
    }

    private fun keyboard(visible: Boolean) = rule.awaitKeyboard(visible)

    private fun tab(destination: TopLevelDestination) {
        rule
            .onNode(
                isSelectable() and hasAnyDescendant(hasText(label(destination.labelRes).uppercase())),
                useUnmergedTree = true,
            ).performClick()
    }

    private fun label(resource: Int) = app.getString(resource)

    private fun text(resource: Int) = text(label(resource))

    private fun list() = rule.onAllNodes(vertical).onLast()

    private fun waitText(value: String) =
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText(value).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }

    private fun scrollTo(value: String) = rule.scrollToText(value, ::list)

    private fun assertThumbnails() {
        val notes =
            rule
                .onAllNodes(
                    SemanticsMatcher("photo caption") {
                        it.config.getOrElse(SemanticsProperties.Text) { emptyList() }.any { value ->
                            value.text.matches(Regex("Photo \\d+"))
                        }
                    },
                ).fetchSemanticsNodes()
        assertTrue(notes.isNotEmpty())
        val maximumWidth = 220 * rule.activity.resources.displayMetrics.density + 1
        notes.forEach { assertTrue("Bounded thumbnail ${it.boundsInWindow}", it.boundsInWindow.width <= maximumWidth) }
    }

    private fun photoCaptions(): Set<String> =
        rule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .flatMap { it.config[SemanticsProperties.Text] }
            .map { it.text }
            .filter { it.matches(Regex("Photo \\d+")) }
            .toSet()

    private fun assertWindowOutside(fold: FoldingFeature) {
        var measured = ""
        try {
            rule.awaitWindowOutsideFold(fold) { measured = it }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Modal overlaps fold: $measured", failure)
        }
        screenshot("modal-${fold.orientation}")
    }

    private fun resize(
        width: Int,
        height: Int,
    ) {
        val density = rule.activity.resources.displayMetrics.density
        rule.resizeWindow(width, height, density)
        println("ADAPTIVE_MEDIA window=${width}x$height density=$density")
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        captureScreenshot(app, "adaptive-media", name)
    }

    private fun makePhoto(): String {
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        for (x in 0..5) {
            for (y in 0..3) {
                paint.color = if ((x + y) % 2 == 0) Color.RED else Color.BLUE
                canvas.drawRect(x * 100f, y * 100f, (x + 1) * 100f, (y + 1) * 100f, paint)
            }
        }
        val file = File(fixtures, "photo.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(file).toString()
    }

    private fun makePdf(id: Int): String {
        val file = File(fixtures, "document-$id.pdf")
        val pdf = PdfDocument()
        try {
            repeat(3) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(400, 800, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                val paint =
                    Paint().apply {
                        color = Color.BLACK
                        textSize = 20f
                    }
                page.canvas.drawText("Document $id page ${index + 1}", 30f, 40f, paint)
                for (line in 1..15) page.canvas.drawLine(30f, line * 50f, 370f, line * 50f, paint)
                pdf.finishPage(page)
            }
            file.outputStream().use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        return Uri.fromFile(file).toString()
    }
}
