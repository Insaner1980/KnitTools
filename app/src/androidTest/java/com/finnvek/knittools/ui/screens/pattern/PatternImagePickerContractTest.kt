package com.finnvek.knittools.ui.screens.pattern

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PatternImageStageException
import com.finnvek.knittools.data.storage.PatternImageStageFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternImagePickerContractTest {
    private val contract = PatternImagePickerContract()

    private fun intent(
        width: Int,
        fontScale: Float,
    ): Intent {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration =
            Configuration(context.resources.configuration).apply {
                screenWidthDp = width
                this.fontScale = fontScale
            }
        return contract.createIntent(
            context.createConfigurationContext(configuration),
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    @Test
    fun narrowLargeFontUsesOpenableMultipleImages() {
        for (scale in listOf(1.5f, 2f)) {
            val intent = intent(320, scale)
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
            assertEquals(listOf("image/*"), intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList())
            assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
            assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun regularFontKeepsPhotoPickerContract() {
        assertEquals(MediaStore.ACTION_PICK_IMAGES, intent(320, 1f).action)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun widerWindowKeepsPhotoPickerContract() {
        assertEquals(MediaStore.ACTION_PICK_IMAGES, intent(360, 2f).action)
    }

    @Test
    fun multipleDocumentResultsKeepOrderAndRemoveDuplicates() {
        val first = Uri.parse("content://audit/images/first")
        val second = Uri.parse("content://audit/images/second")
        val result =
            Intent().apply {
                clipData =
                    ClipData.newRawUri("images", first).apply {
                        addItem(ClipData.Item(second))
                        addItem(ClipData.Item(first))
                    }
            }
        assertEquals(listOf(first, second), contract.parseResult(Activity.RESULT_OK, result))
    }

    @Test
    fun cancellationReturnsNoImages() {
        val result = Intent().setData(Uri.parse("content://audit/images/first"))
        assertTrue(contract.parseResult(Activity.RESULT_CANCELED, result).isEmpty())
    }

    @Test
    fun documentPickerCannotBypassPageLimit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uris = (0..PatternImageImportLimits.MAX_PAGES).map { Uri.parse("content://audit/images/$it") }
        val failure =
            assertThrows(PatternImageStageException::class.java) {
                runBlocking {
                    PatternDocumentStorage().stageSelectedImages(
                        context = context,
                        projectId = System.currentTimeMillis(),
                        sessionId = "page-limit",
                        existingSelection = PatternImageSelection(),
                        sourceUris = uris,
                    )
                }
            }
        assertEquals(PatternImageStageFailure.PAGE_LIMIT, failure.reason)
    }
}
