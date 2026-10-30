package com.finnvek.knittools.data.datastore

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.model.CraftType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastProjectCraftPreferenceTest {
    @Test
    fun `absent or unknown last craft defaults to knitting`() {
        assertEquals(CraftType.KNITTING, AppPreferences().lastProjectCraftType)
        assertEquals(CraftType.KNITTING, CraftType.fromPersistedValue(null))
        assertEquals(CraftType.KNITTING, CraftType.fromPersistedValue("UNKNOWN"))
        assertEquals(CraftType.CROCHET, CraftType.fromPersistedValue(CraftType.CROCHET.name))
    }

    @Test
    fun `last craft uses existing preferences store with stable names and safe writes`() {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt",
            )

        assertTrue(source.contains("stringPreferencesKey(\"last_project_craft_type\")"))
        assertTrue(
            source.contains("lastProjectCraftType = CraftType.fromPersistedValue(prefs[KEY_LAST_PROJECT_CRAFT])"),
        )
        assertTrue(
            source.contains(
                "suspend fun setLastProjectCraftType(type: CraftType) {\n" +
                    "            context.dataStore.editPreferencesSafely {\n" +
                    "                it[KEY_LAST_PROJECT_CRAFT] = type.name",
            ),
        )
    }
}
