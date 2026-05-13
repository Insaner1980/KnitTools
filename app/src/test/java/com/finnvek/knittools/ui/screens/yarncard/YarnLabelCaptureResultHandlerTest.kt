package com.finnvek.knittools.ui.screens.yarncard

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class YarnLabelCaptureResultHandlerTest {
    @Test
    fun `successful capture forwards photo and clears pending state`() {
        val photoUri = mockk<Uri>()
        every { photoUri.toString() } returns "content://scan/success"
        var capturedUri: Uri? = null
        var deletedUri: String? = null
        var clearCount = 0

        handleYarnLabelCaptureResult(
            success = true,
            pendingPhotoUri = photoUri,
            onCaptured = { capturedUri = it },
            onDeleteUnusedPhoto = { deletedUri = it },
            clearPendingPhoto = { clearCount += 1 },
        )

        assertSame(photoUri, capturedUri)
        assertNull(deletedUri)
        assertEquals(1, clearCount)
    }

    @Test
    fun `failed capture deletes reserved photo and clears pending state`() {
        val photoUri = mockk<Uri>()
        every { photoUri.toString() } returns "content://scan/cancelled"
        var capturedUri: Uri? = null
        var deletedUri: String? = null
        var clearCount = 0

        handleYarnLabelCaptureResult(
            success = false,
            pendingPhotoUri = photoUri,
            onCaptured = { capturedUri = it },
            onDeleteUnusedPhoto = { deletedUri = it },
            clearPendingPhoto = { clearCount += 1 },
        )

        assertNull(capturedUri)
        assertEquals("content://scan/cancelled", deletedUri)
        assertEquals(1, clearCount)
    }

    @Test
    fun `missing pending photo is ignored`() {
        var capturedUri: Uri? = null
        var deletedUri: String? = null
        var clearCount = 0

        handleYarnLabelCaptureResult(
            success = false,
            pendingPhotoUri = null,
            onCaptured = { capturedUri = it },
            onDeleteUnusedPhoto = { deletedUri = it },
            clearPendingPhoto = { clearCount += 1 },
        )

        assertNull(capturedUri)
        assertNull(deletedUri)
        assertEquals(0, clearCount)
    }
}
