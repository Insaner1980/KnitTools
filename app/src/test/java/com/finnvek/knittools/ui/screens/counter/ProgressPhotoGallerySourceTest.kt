package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPhotoGallerySourceTest {
    @Test
    fun `photo gallery requests camera permission before capture`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertTrue(source.contains("ActivityResultContracts.RequestPermission()"))
        assertTrue(source.contains("permissionLauncher.launch(Manifest.permission.CAMERA)"))
    }

    @Test
    fun `photo gallery deletes pending capture file when capture is not saved`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertTrue(source.contains("pendingPhotoFilePath"))
        assertTrue(source.contains("deletePendingPhotoFile"))
        assertTrue(source.contains("if (!success)"))
    }

    private companion object {
        private const val PHOTO_GALLERY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt"
    }
}
