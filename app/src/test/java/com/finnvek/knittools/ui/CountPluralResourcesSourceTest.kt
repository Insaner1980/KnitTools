package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountPluralResourcesSourceTest {
    @Test
    fun `counts use plural resources in every locale`() {
        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            COUNT_PLURAL_KEYS.forEach { key ->
                assertTrue("Missing plural $key in $stringsFile", strings.contains("<plurals name=\"$key\""))
                assertFalse("$key string still present in $stringsFile", hasStringResource(strings, key))
            }
        }

        SOURCE_USAGES.forEach { (sourceFile, pluralKey) ->
            assertTrue(
                "$sourceFile must select the quantity-specific resource",
                Regex("""pluralStringResource\s*\(\s*R\.plurals\.$pluralKey""")
                    .containsMatchIn(ProjectSourceFiles.read(sourceFile)),
            )
        }
        // Main-counter deltas can be rounds, repeats or custom units; their neutral
        // progress label has no quantity-dependent noun. Keep actual row plurals elsewhere.
        listOf(
            "counter/CounterScreen.kt",
            "insights/InsightsSections.kt",
        ).forEach {
            val source = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/screens/$it")
            assertTrue("$it must retain neutral counter progress", source.contains("R.string.counter_progress_format"))
            assertFalse("$it must not relabel counter deltas as rows", source.contains("R.plurals.rows_format"))
        }
    }

    private fun hasStringResource(
        strings: String,
        key: String,
    ): Boolean = Regex("""<string\s+name="$key"(\s|>)""").containsMatchIn(strings)

    private companion object {
        val COUNT_PLURAL_KEYS =
            listOf(
                "rows_format",
                "delete_photos_confirm",
                "delete_patterns_confirm",
                "complete_n_projects",
                "delete_n_projects",
                "delete_yarn_cards_confirm",
                "stitches_result",
                "relative_time_months_ago",
            )
        val SOURCE_USAGES =
            listOf(
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsSections.kt" to
                    "relative_time_months_ago",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt" to
                    "delete_photos_confirm",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt" to
                    "delete_patterns_confirm",
                "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt" to
                    "complete_n_projects",
                "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt" to
                    "delete_n_projects",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt" to
                    "delete_yarn_cards_confirm",
                "app/src/main/java/com/finnvek/knittools/ui/screens/caston/CastOnScreen.kt" to
                    "stitches_result",
            )
    }
}
