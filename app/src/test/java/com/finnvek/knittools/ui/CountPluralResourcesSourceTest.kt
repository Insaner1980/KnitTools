package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountPluralResourcesSourceTest {
    @Test
    fun `row photo and pattern counts use plural resources in every locale`() {
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
    }

    private fun hasStringResource(
        strings: String,
        key: String,
    ): Boolean = Regex("""<string\s+name="$key"(\s|>)""").containsMatchIn(strings)

    private companion object {
        val COUNT_PLURAL_KEYS =
            listOf(
                "insights_rows_count",
                "delete_photos_confirm",
                "delete_patterns_confirm",
            )
        val SOURCE_USAGES =
            mapOf(
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt" to
                    "insights_rows_count",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/AllPhotosScreen.kt" to
                    "delete_photos_confirm",
                "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt" to
                    "delete_patterns_confirm",
                "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt" to
                    "delete_patterns_confirm",
            )
    }
}
