package com.finnvek.knittools.ui.components

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterImageButtonSourceTest {
    @Test
    fun `row counter controls use image buttons for primary plus and secondary minus`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)

        assertTrue(workspace.contains("CounterImageButton("))
        assertTrue(Regex("CounterImageButton\\(").findAll(workspace).count() == 2)
        assertTrue(workspace.contains("imageRes = R.drawable.counter_minus_button"))
        assertTrue(workspace.contains("imageRes = R.drawable.counter_plus_button"))
        assertTrue(workspace.contains("visualSize = CounterDimens.CounterMinusVisualSize"))
        assertTrue(workspace.contains("visualSize = CounterDimens.CounterPrimaryVisualSize"))
        assertTrue(workspace.contains("onClick = onDecrement"))
        assertTrue(workspace.contains("onClick = onIncrement"))
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

        val button = ProjectSourceFiles.read(imageButtonFile)
        listOf(
            "fun CounterImageButton(",
            "imageRes: Int",
            "visualSize: Dp",
            "MutableInteractionSource",
            "collectIsPressedAsState()",
            "Role.Button",
            "indication = null",
            "painterResource(id = imageRes)",
            ".size(visualSize)",
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
    }
}
