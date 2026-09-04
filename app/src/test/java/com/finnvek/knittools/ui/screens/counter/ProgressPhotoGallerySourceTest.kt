package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
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
    fun `photo gallery cleans up when no camera handler can launch`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertTrue(source.contains("catch (_: ActivityNotFoundException)"))
        assertTrue(source.contains("actions.deletePendingPhotoFile(captureTarget.filePath)"))
        assertTrue(source.contains("actions.cancelPhotoCreation()"))
    }

    @Test
    fun `photo gallery deletes pending capture file when capture is not saved`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertTrue(source.contains("pendingPhotoFilePath"))
        assertTrue(source.contains("actions.deletePendingPhotoFile"))
        assertTrue(source.contains("private fun handlePhotoCaptureResult("))
        assertTrue(source.contains("} else {\n        actions.deletePendingPhotoFile(pendingPhotoFilePath)"))
    }

    @Test
    fun `photo gallery delegates capture file creation outside the composable`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertFalse(source.contains("ProgressPhotoStorage"))
        assertTrue(source.contains("PhotoGalleryActions"))
        assertTrue(source.contains("createPhotoCaptureTarget"))
    }

    @Test
    fun `photo gallery delegates abandoned capture file deletion outside the composable`() {
        val source = ProjectSourceFiles.read(PHOTO_GALLERY_SCREEN)

        assertFalse(source.contains("java.io.File"))
        assertFalse(source.contains("file.delete()"))
        assertTrue(source.contains("deletePendingPhotoFile"))
    }

    @Test
    fun `progress photo capture file operations stay behind repository IO dispatcher`() {
        val source = ProjectSourceFiles.read(PROGRESS_PHOTO_REPOSITORY)

        assertTrue(source.contains("suspend fun createPhotoCaptureTarget("))
        assertTrue(source.contains("suspend fun deletePendingPhotoFile("))
        assertTrue(source.contains("withContext(ioDispatcher) {\n                storage.createPhotoFile"))
        assertTrue(source.contains("withContext(ioDispatcher) {\n                storage.deletePendingPhotoFile"))
    }

    @Test
    fun `photo share grants URI access through ClipData`() {
        val source = ProjectSourceFiles.read(PHOTO_COMPONENTS)

        assertTrue(source.contains("ClipData.newUri"))
        assertTrue(source.contains("clipData ="))
        assertTrue(source.contains("Intent.FLAG_GRANT_READ_URI_PERMISSION"))
    }

    @Test
    fun `all photos uses localized project fallback names`() {
        val source = ProjectSourceFiles.read(ALL_PHOTOS_SCREEN)

        assertTrue(source.contains("stringResource(R.string.new_project_name_format, projectId)"))
        assertTrue(source.contains("stringResource(R.string.new_project_name_format, photo.projectId)"))
        assertFalse(source.contains("\"Project \$projectId\""))
    }

    private companion object {
        private const val PHOTO_GALLERY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoGalleryScreen.kt"
        private const val PHOTO_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt"
        private const val ALL_PHOTOS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt"
        private const val PROGRESS_PHOTO_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/ProgressPhotoRepository.kt"
    }
}
