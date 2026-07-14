package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterImageButtonSourceTest {
    @Test
    fun `row counter controls use image buttons for plus minus and undo`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)

        assertTrue(workspace.contains("CounterImageButton("))
        assertTrue(Regex("CounterImageButton\\(").findAll(workspace).count() == 3)
        assertTrue(workspace.contains("imageRes = R.drawable.counter_minus_button"))
        assertTrue(workspace.contains("imageRes = R.drawable.counter_plus_button"))
        assertTrue(workspace.contains("imageRes = R.drawable.counter_undo_button"))
        assertTrue(workspace.contains("visualSize = CounterDimens.CounterMinusVisualSize"))
        assertTrue(workspace.contains("visualOffsetY = CounterDimens.CounterMinusOpticalOffsetY"))
        assertTrue(workspace.contains("visualSize = CounterDimens.CounterPrimaryVisualSize"))
        assertTrue(workspace.contains("visualSize = CounterDimens.CounterUndoVisualSize"))
        assertTrue(workspace.contains("onClick = onDecrement"))
        assertTrue(workspace.contains("onClick = onIncrement"))
        assertTrue(workspace.contains("onClick = onUndo"))
        assertFalse(workspace.contains("CounterHeroActionButton("))
        assertFalse(workspace.contains("CounterCraftButton("))
        assertFalse(workspace.contains("CounterCraftButtonSymbol"))
        assertFalse(workspace.contains("CounterCraftButtonTone"))
        assertFalse(workspace.contains("CroppedWood"))
        assertFalse(workspace.contains("R.drawable.plus_button"))
        assertFalse(workspace.contains("R.drawable.minus_button"))
    }

    @Test
    fun `counter image button keeps custom button interaction semantics`() {
        val imageButtonFile = ProjectSourceFiles.file(COUNTER_IMAGE_BUTTON)

        assertTrue("CounterImageButton.kt is missing", Files.exists(imageButtonFile))
        assertTrue(
            "counter_minus_button.webp is missing",
            Files.exists(ProjectSourceFiles.file(COUNTER_MINUS_BUTTON_ASSET)),
        )
        assertTrue(
            "counter_plus_button.webp is missing",
            Files.exists(ProjectSourceFiles.file(COUNTER_PLUS_BUTTON_ASSET)),
        )
        assertTrue(
            "counter_undo_button.webp is missing",
            Files.exists(ProjectSourceFiles.file(COUNTER_UNDO_BUTTON_ASSET)),
        )
        assertFalse(
            "counter_undo_button.png should not remain after WebP conversion",
            Files.exists(ProjectSourceFiles.file(COUNTER_UNDO_BUTTON_PNG_ASSET)),
        )

        val button = ProjectSourceFiles.read(imageButtonFile)
        listOf(
            "fun CounterImageButton(",
            "imageRes: Int",
            "visualSize: Dp",
            "visualOffsetY: Dp = 0.dp",
            "MutableInteractionSource",
            "collectIsPressedAsState()",
            "Role.Button",
            "indication = null",
            "painterResource(id = imageRes)",
            ".size(visualSize)",
            ".offset(y = visualOffsetY)",
            "contentDescription = contentDescription",
        ).forEach { required ->
            assertTrue("Counter image button should contain $required", button.contains(required))
        }
    }

    private companion object {
        private const val COUNTER_IMAGE_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterImageButton.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val COUNTER_MINUS_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_minus_button.webp"
        private const val COUNTER_PLUS_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_plus_button.webp"
        private const val COUNTER_UNDO_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_undo_button.webp"
        private const val COUNTER_UNDO_BUTTON_PNG_ASSET =
            "app/src/main/res/drawable-nodpi/counter_undo_button.png"
    }
}
