package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureSingleSourceSourceTest {
    @Test
    fun `project counter type rules stay centralized`() {
        val projectCounter = ProjectSourceFiles.read(PROJECT_COUNTER)
        val projectCounterDraft = ProjectSourceFiles.read(PROJECT_COUNTER_DRAFT)
        val projectCounterLogic = ProjectSourceFiles.read(PROJECT_COUNTER_LOGIC)
        val repeatSectionLogic = ProjectSourceFiles.read(REPEAT_SECTION_LOGIC)
        val projectCounterDao = ProjectSourceFiles.read(PROJECT_COUNTER_DAO)
        val multiCounterComponents = ProjectSourceFiles.read(MULTI_COUNTER_COMPONENTS)

        assertTrue(projectCounter.contains("val counterType: ProjectCounterType = ProjectCounterType.COUNT_UP"))
        assertTrue(projectCounterDraft.contains("val counterType: ProjectCounterType = ProjectCounterType.COUNT_UP"))
        assertFalse(projectCounterLogic.contains("counterType == \""))
        assertFalse(repeatSectionLogic.contains("counterType != \""))
        assertFalse(projectCounterDao.contains("WHEN counterType ="))
        assertFalse(multiCounterComponents.contains("counterTypeFromIndex(index: Int): String"))
    }

    @Test
    fun `project sort order uses typed values across datastore repository and UI`() {
        val projectSortOrder = ProjectSourceFiles.read(PROJECT_SORT_ORDER)
        val preferencesManager = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val counterRepository = ProjectSourceFiles.read(COUNTER_REPOSITORY)
        val projectListScreen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(projectSortOrder.contains("enum class ProjectSortOrder"))
        assertFalse(preferencesManager.contains("projectSortOrder: String = \"updated\""))
        assertFalse(counterRepository.contains("fun getActiveProjects(sortOrder: String)"))
        assertFalse(counterRepository.contains("fun getCompletedProjects(sortOrder: String)"))
        assertFalse(projectListScreen.contains("onSortOrderChange(\""))
    }

    @Test
    fun `yarn card ids and fallback names use shared helpers`() {
        val counterViewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val projectListViewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)
        val yarnCardRepository = ProjectSourceFiles.read(YARN_CARD_REPOSITORY)
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val yarnCardLinks = ProjectSourceFiles.read(YARN_CARD_LINKS)
        val yarnCard = ProjectSourceFiles.read(YARN_CARD)

        assertFalse(counterViewModel.contains(".split(\",\")"))
        assertFalse(projectListViewModel.contains(".split(\",\")"))
        assertFalse(yarnCardRepository.contains("private fun String.toYarnCardIds"))
        assertFalse(counterViewModel.contains("Yarn #"))
        assertFalse(projectListViewModel.contains("Yarn #"))
        assertFalse(counterScreen.contains("Yarn #"))
        assertTrue(yarnCardLinks.contains("fun parseYarnCardIds"))
        assertTrue(yarnCardLinks.contains("fun formatYarnCardIds"))
        assertTrue(yarnCard.contains("fun YarnCard.displayName"))
    }

    @Test
    fun `file provider URI creation stays behind storage helper`() {
        val appFileStorage = ProjectSourceFiles.read(APP_FILE_STORAGE)
        val progressPhotoStorage = ProjectSourceFiles.read(PROGRESS_PHOTO_STORAGE)
        val patternDocumentStorage = ProjectSourceFiles.read(PATTERN_DOCUMENT_STORAGE)
        val photoComponents = ProjectSourceFiles.read(PHOTO_COMPONENTS)

        assertTrue(appFileStorage.contains("fun fileProviderAuthority"))
        assertTrue(appFileStorage.contains("fun shareUriForAppOwnedFile"))
        assertFalse(progressPhotoStorage.contains("import androidx.core.content.FileProvider"))
        assertFalse(patternDocumentStorage.contains("import androidx.core.content.FileProvider"))
        assertFalse(photoComponents.contains("FileProvider.getUriForFile"))
        assertFalse(photoComponents.contains("path!!"))
    }

    @Test
    fun `pattern info labels use string resources`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertFalse(counterScreen.contains("label = \"Yardage\""))
        assertFalse(counterScreen.contains("value = \"${'$'}it rows\""))
        assertTrue(counterScreen.contains("R.string.pattern_detail_yardage"))
        assertTrue(counterScreen.contains("R.string.rows_format"))
    }

    @Test
    fun `top level navigation start routes reuse screen routes`() {
        val screen = ProjectSourceFiles.read(SCREEN)

        assertTrue(screen.contains("Screen.ProjectList.route"))
        assertTrue(screen.contains("Screen.Library.route"))
        assertTrue(screen.contains("Screen.Tools.route"))
        assertTrue(screen.contains("Screen.Insights.route"))
        assertTrue(screen.contains("Screen.Settings.route"))
    }

    @Test
    fun `PROJECT document matches current architecture decisions`() {
        val project = ProjectSourceFiles.read(PROJECT_MD)

        assertTrue(project.contains("Room schema version: `12`"))
        assertFalse(project.contains("Room schema version: `11`"))
        assertFalse(project.contains("VoiceCommandParser.kt"))
        assertFalse(project.contains("SpeechRecognizer"))
        assertFalse(project.contains("YarnLabelPhotoStorage"))
    }

    private companion object {
        private const val PROJECT_MD = "PROJECT.md"
        private const val PROJECT_COUNTER =
            "app/src/main/java/com/finnvek/knittools/domain/model/ProjectCounter.kt"
        private const val PROJECT_COUNTER_DRAFT =
            "app/src/main/java/com/finnvek/knittools/domain/model/ProjectCounterDraft.kt"
        private const val PROJECT_COUNTER_LOGIC =
            "app/src/main/java/com/finnvek/knittools/domain/calculator/ProjectCounterLogic.kt"
        private const val REPEAT_SECTION_LOGIC =
            "app/src/main/java/com/finnvek/knittools/domain/calculator/RepeatSectionLogic.kt"
        private const val PROJECT_COUNTER_DAO =
            "app/src/main/java/com/finnvek/knittools/data/local/ProjectCounterDao.kt"
        private const val MULTI_COUNTER_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/MultiCounterComponents.kt"
        private const val PROJECT_SORT_ORDER =
            "app/src/main/java/com/finnvek/knittools/domain/model/ProjectSortOrder.kt"
        private const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        private const val YARN_CARD_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/YarnCardRepository.kt"
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val YARN_CARD_LINKS =
            "app/src/main/java/com/finnvek/knittools/domain/model/YarnCardLinks.kt"
        private const val YARN_CARD =
            "app/src/main/java/com/finnvek/knittools/domain/model/YarnCard.kt"
        private const val APP_FILE_STORAGE =
            "app/src/main/java/com/finnvek/knittools/data/storage/AppFileStorage.kt"
        private const val PROGRESS_PHOTO_STORAGE =
            "app/src/main/java/com/finnvek/knittools/data/storage/ProgressPhotoStorage.kt"
        private const val PATTERN_DOCUMENT_STORAGE =
            "app/src/main/java/com/finnvek/knittools/data/storage/PatternDocumentStorage.kt"
        private const val PHOTO_COMPONENTS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/PhotoComponents.kt"
        private const val SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
    }
}
