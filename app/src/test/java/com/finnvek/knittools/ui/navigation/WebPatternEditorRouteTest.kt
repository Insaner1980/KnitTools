package com.finnvek.knittools.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebPatternEditorRouteTest {
    @Test
    fun `manual share project and edit use one route with stable identifiers`() {
        assertEquals(
            "web_pattern_editor?origin=manual",
            Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Manual),
        )
        assertEquals(
            "web_pattern_editor?origin=share",
            Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Share),
        )
        assertEquals(
            "web_pattern_editor?origin=project&projectId=42",
            Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Project, projectId = 42L),
        )
        assertEquals(
            "web_pattern_editor?origin=edit&patternId=7",
            Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Edit, patternId = 7L),
        )
    }

    @Test
    fun `route argument parsing rejects mismatched or nonpositive identifiers`() {
        assertNull(WebPatternEditorRoute.from(WebPatternEditorOrigin.Project.persistedValue, null, null))
        assertNull(WebPatternEditorRoute.from(WebPatternEditorOrigin.Project.persistedValue, 0L, null))
        assertNull(WebPatternEditorRoute.from(WebPatternEditorOrigin.Edit.persistedValue, null, -1L))
        assertNull(WebPatternEditorRoute.from(WebPatternEditorOrigin.Manual.persistedValue, 4L, null))
        assertNull(WebPatternEditorRoute.from("unknown", null, null))
    }

    @Test
    fun `route argument parsing preserves the requested origin`() {
        assertEquals(
            WebPatternEditorRoute(WebPatternEditorOrigin.Project, projectId = 99L),
            WebPatternEditorRoute.from("project", 99L, null),
        )
        assertEquals(
            WebPatternEditorRoute(WebPatternEditorOrigin.Edit, patternId = 12L),
            WebPatternEditorRoute.from("edit", null, 12L),
        )
    }
}
