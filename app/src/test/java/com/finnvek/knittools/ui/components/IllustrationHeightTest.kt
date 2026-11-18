package com.finnvek.knittools.ui.components

import androidx.compose.ui.unit.dp
import com.finnvek.knittools.ui.theme.IllustrationDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IllustrationHeightTest {
    @Test
    fun `tall viewport keeps the regular illustration`() {
        assertEquals(170.dp, illustrationHeightFor(600.dp, 170.dp))
        assertEquals(170.dp, illustrationHeightFor(480.dp, 170.dp, fontScale = 2f))
    }

    @Test
    fun `short viewport shrinks and very short viewport hides the illustration`() {
        assertEquals(IllustrationDimens.ShortHeight, illustrationHeightFor(479.dp, 170.dp))
        assertEquals(IllustrationDimens.ShortHeight, illustrationHeightFor(360.dp, 170.dp))
        assertNull(illustrationHeightFor(359.dp, 170.dp))
    }

    @Test
    fun `short viewport with large font gives the space to text`() {
        assertEquals(IllustrationDimens.ShortHeight, illustrationHeightFor(420.dp, 170.dp, fontScale = 1.3f))
        assertNull(illustrationHeightFor(420.dp, 170.dp, fontScale = 1.31f))
    }

    @Test
    fun `short height never enlarges a smaller illustration`() {
        assertEquals(60.dp, illustrationHeightFor(420.dp, 60.dp))
    }
}
