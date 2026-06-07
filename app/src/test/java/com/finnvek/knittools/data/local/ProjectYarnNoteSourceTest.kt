package com.finnvek.knittools.data.local

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectYarnNoteSourceTest {
    @Test
    fun `database declares project yarn notes with version 13 migration chain and cascade cleanup`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val entity = ProjectSourceFiles.read(PROJECT_YARN_NOTE_ENTITY)
        val module = ProjectSourceFiles.read(DATABASE_MODULE)

        assertTrue(database.contains("ProjectYarnNoteEntity::class"))
        assertTrue(database.contains("version = 13"))
        assertTrue(database.contains("abstract fun projectYarnNoteDao(): ProjectYarnNoteDao"))
        assertTrue(database.contains("MIGRATION_11_12"))
        assertTrue(database.contains("MIGRATION_12_13"))
        assertTrue(database.contains("CREATE TABLE IF NOT EXISTS `project_yarn_notes`"))
        assertTrue(database.contains("CREATE INDEX IF NOT EXISTS `index_project_yarn_notes_projectId`"))
        assertTrue(module.contains("KnitToolsDatabase.MIGRATION_11_12"))
        assertTrue(module.contains("fun provideProjectYarnNoteDao"))
        assertTrue(entity.contains("tableName = \"project_yarn_notes\""))
        assertTrue(entity.contains("ForeignKey.CASCADE"))
        assertTrue(entity.contains("indices = [Index(\"projectId\")]"))
    }

    private companion object {
        private const val DATABASE = "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val PROJECT_YARN_NOTE_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/ProjectYarnNoteEntity.kt"
        private const val DATABASE_MODULE = "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt"
    }
}
