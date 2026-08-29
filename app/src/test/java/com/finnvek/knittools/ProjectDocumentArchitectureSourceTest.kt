package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDocumentArchitectureSourceTest {
    @Test
    fun `schema 22 has one canonical project document entity`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val entity = ProjectSourceFiles.read(ENTITY)

        assertTrue(database.contains("ProjectDocumentEntity::class"))
        assertTrue(database.contains("version = 24"))
        assertTrue(database.contains("MIGRATION_21_22"))
        assertTrue(entity.contains("tableName = \"project_documents\""))
        assertTrue(entity.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(entity.contains("onDelete = ForeignKey.SET_NULL"))
    }

    @Test
    fun `production viewer and counter state read canonical project documents`() {
        val viewer = ProjectSourceFiles.read(VIEWER)
        val counter = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertTrue(viewer.contains("val selectedDocument = documentState.selectedDocument"))
        assertTrue(counter.contains("projectDocumentRepository.observeDocuments(projectId)"))
        assertTrue(repository.contains("projectDocumentRepository.getActiveDocument(id)"))
        assertFalse(viewer.contains("val currentPage = counterState.currentPatternPage"))
    }

    @Test
    fun `feature adds no entitlement permission or provider surface`() {
        val proFeature = ProjectSourceFiles.read(PRO_FEATURE)
        val manifest = ProjectSourceFiles.read(MANIFEST)

        assertFalse(proFeature.contains("MULTIPLE_PROJECT_DOCUMENTS"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("MANAGE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("project_documents"))
    }

    private companion object {
        const val DATABASE = "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        const val ENTITY = "app/src/main/java/com/finnvek/knittools/data/local/ProjectDocumentEntity.kt"
        const val VIEWER = "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        const val PRO_FEATURE = "app/src/main/java/com/finnvek/knittools/pro/ProState.kt"
        const val MANIFEST = "app/src/main/AndroidManifest.xml"
    }
}
