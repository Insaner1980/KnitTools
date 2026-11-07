package com.finnvek.knittools.widget

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

/** Widgetin järjestelmäesikatselu on RemoteViews-layout: tuntematon näkymäluokka kaataa sen launcherissa. */
class WidgetPreviewLayoutSourceTest {
    @Test
    fun `preview layout uses only RemoteViews supported views`() {
        val layout = ProjectSourceFiles.read("app/src/main/res/layout/widget_counter_preview.xml")
        val tags =
            Regex("<([A-Za-z][A-Za-z.]*)")
                .findAll(layout)
                .map { it.groupValues[1] }
                .toSet()
        val unsupported = tags - REMOTE_VIEWS_TAGS
        assertTrue("Not supported by RemoteViews: $unsupported", unsupported.isEmpty())
    }

    private companion object {
        val REMOTE_VIEWS_TAGS = setOf("FrameLayout", "LinearLayout", "RelativeLayout", "ImageView", "TextView")
    }
}
