package com.finnvek.knittools.data.local

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomIndexSourceTest {
    @Test
    fun `foreign key like cleanup columns have room indexes`() {
        val counterProjectEntity = ProjectSourceFiles.read(COUNTER_PROJECT_ENTITY)
        val yarnCardEntity = ProjectSourceFiles.read(YARN_CARD_ENTITY)
        val database = ProjectSourceFiles.read(DATABASE)
        val databaseModule = ProjectSourceFiles.read(DATABASE_MODULE)
        val migrationTest = ProjectSourceFiles.read(MIGRATION_TEST)

        assertTrue(counterProjectEntity.contains("Index(\"linkedPatternId\")"))
        assertTrue(yarnCardEntity.contains("Index(\"linkedProjectId\")"))
        assertTrue(database.contains("version = 15"))
        assertTrue(database.contains("MIGRATION_14_15"))
        assertTrue(database.contains("index_counter_projects_linkedPatternId"))
        assertTrue(database.contains("index_yarn_cards_linkedProjectId"))
        assertTrue(databaseModule.contains("KnitToolsDatabase.MIGRATION_14_15"))
        assertTrue(migrationTest.contains("KnitToolsDatabase.MIGRATION_14_15"))
    }

    private companion object {
        private const val COUNTER_PROJECT_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/CounterProjectEntity.kt"
        private const val YARN_CARD_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/YarnCardEntity.kt"
        private const val DATABASE =
            "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val DATABASE_MODULE =
            "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt"
        private const val MIGRATION_TEST =
            "app/src/androidTest/java/com/finnvek/knittools/data/local/MigrationTest.kt"
    }
}
