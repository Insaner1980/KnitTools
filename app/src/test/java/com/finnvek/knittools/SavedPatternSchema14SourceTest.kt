package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPatternSchema14SourceTest {
    @Test
    fun `database moves saved patterns to schema 14 with source metadata migration`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val module = ProjectSourceFiles.read(DATABASE_MODULE)
        val entity = ProjectSourceFiles.read(SAVED_PATTERN_ENTITY)

        assertTrue(database.contains("version = 14"))
        assertTrue(database.contains("MIGRATION_13_14"))
        assertTrue(module.contains("KnitToolsDatabase.MIGRATION_13_14"))
        assertTrue(entity.contains("val source: String"))
        assertTrue(entity.contains("val ravelryPatternId: Int? = null"))
        assertTrue(entity.contains("val originalUrl: String = \"\""))
        assertTrue(entity.contains("val canonicalUrl: String = \"\""))
        assertTrue(entity.contains("val localPdfUri: String? = null"))
        assertTrue(entity.contains("val isAvailableOffline: Boolean = false"))
        assertTrue(entity.contains("val updatedAt: Long"))
        assertTrue(entity.contains("val lastSyncedAt: Long? = null"))
        assertFalse(entity.contains("val ravelryId: Int,"))
        assertFalse(entity.contains("val patternUrl: String = \"\""))
    }

    @Test
    fun `migration 13 to 14 backfills source metadata and preserves saved pattern ids`() {
        val database = ProjectSourceFiles.read(DATABASE)
        val localFileBackfill =
            "WHEN patternUrl LIKE 'content://%' OR patternUrl LIKE 'file://%' THEN 'LOCAL_FILE'"

        assertTrue(database.contains("CREATE TABLE IF NOT EXISTS `saved_patterns_new`"))
        assertTrue(database.contains("`source` TEXT NOT NULL"))
        assertTrue(database.contains("`ravelryPatternId` INTEGER"))
        assertTrue(database.contains("`originalUrl` TEXT NOT NULL"))
        assertTrue(database.contains("`canonicalUrl` TEXT NOT NULL"))
        assertTrue(database.contains("`localPdfUri` TEXT"))
        assertTrue(database.contains("`isAvailableOffline` INTEGER NOT NULL"))
        assertTrue(database.contains("`updatedAt` INTEGER NOT NULL"))
        assertTrue(database.contains("`lastSyncedAt` INTEGER"))
        assertTrue(database.contains("INSERT INTO `saved_patterns_new`"))
        assertTrue(database.contains("id, source, ravelryPatternId"))
        assertTrue(database.contains("WHEN ravelryId > 0 THEN 'RAVELRY'"))
        assertTrue(database.contains(localFileBackfill))
        assertTrue(database.contains("ELSE 'OTHER'"))
        assertTrue(database.contains("DROP TABLE `saved_patterns`"))
        assertTrue(database.contains("ALTER TABLE `saved_patterns_new` RENAME TO `saved_patterns`"))
    }

    @Test
    fun `saved pattern repository detects duplicates by id canonical url original url then confirmed title designer`() {
        val dao = ProjectSourceFiles.read(SAVED_PATTERN_DAO)
        val repository = ProjectSourceFiles.read(SAVED_PATTERN_REPOSITORY)

        assertTrue(dao.contains("getByRavelryPatternId"))
        assertTrue(dao.contains("getByCanonicalUrl"))
        assertTrue(dao.contains("getAllOnce"))
        assertTrue(dao.contains("getByTitleAndDesignerName"))
        assertTrue(repository.contains("findDuplicateCandidate"))
        assertTrue(repository.contains("pattern.ravelryPatternId?.let"))
        assertTrue(repository.contains("dao.getByRavelryPatternId"))
        assertTrue(repository.contains("dao.getByCanonicalUrl"))
        assertTrue(repository.contains("normalizedOriginalUrl"))
        assertTrue(repository.contains("includeTitleDesigner"))
        assertTrue(repository.indexOf("dao.getByRavelryPatternId") < repository.indexOf("dao.getByCanonicalUrl"))
        assertTrue(repository.indexOf("dao.getByCanonicalUrl") < repository.indexOf("normalizedOriginalUrl"))
        assertTrue(repository.indexOf("normalizedOriginalUrl") < repository.indexOf("if (includeTitleDesigner"))
    }

    private companion object {
        private const val DATABASE = "app/src/main/java/com/finnvek/knittools/data/local/KnitToolsDatabase.kt"
        private const val DATABASE_MODULE = "app/src/main/java/com/finnvek/knittools/di/DatabaseModule.kt"
        private const val SAVED_PATTERN_ENTITY =
            "app/src/main/java/com/finnvek/knittools/data/local/SavedPatternEntity.kt"
        private const val SAVED_PATTERN_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/SavedPatternDao.kt"
        private const val SAVED_PATTERN_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/SavedPatternRepository.kt"
    }
}
