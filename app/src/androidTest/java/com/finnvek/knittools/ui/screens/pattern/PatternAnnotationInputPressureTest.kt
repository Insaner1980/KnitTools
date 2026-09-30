package com.finnvek.knittools.ui.screens.pattern

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.finnvek.knittools.domain.calculator.PatternPageCoordinateTransform
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PatternAnnotationInputPressureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun stylusPressureIsNormalizedForHighlighterAndPreservedForPenAcrossToolSwitches() {
        val tool = mutableStateOf(PatternAnnotationTool.PEN)
        val points = mutableListOf<NormalizedPatternPoint>()
        var commits = 0
        composeRule.setContent {
            PatternAnnotationInputOverlay(
                activeTool = tool.value,
                coordinateTransform = PatternPageCoordinateTransform(0f, 0f, 1_000f, 1_000f, scale = 1f),
                viewportScale = 1f,
                pressureEnabled = tool.value == PatternAnnotationTool.PEN,
                actions = PatternAnnotationInputActions(
                    onBeginStroke = { points += it },
                    onAppendStrokePoint = { points += it },
                    onCommitStroke = { commits++ },
                    onCancelStroke = {},
                    onEraseStroke = {},
                    onSelectAnnotation = {},
                ),
                modifier = Modifier.fillMaxSize().testTag("input"),
            )
        }
        for (next in listOf(PatternAnnotationTool.PEN, PatternAnnotationTool.HIGHLIGHTER, PatternAnnotationTool.PEN)) {
            composeRule.runOnIdle {
                tool.value = next
                points.clear()
            }
            val bounds = composeRule.onNodeWithTag("input").fetchSemanticsNode().boundsInRoot
            val downTime = SystemClock.uptimeMillis()
            val properties = MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_STYLUS
            }
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP).forEachIndexed { index, action ->
                val coordinates = MotionEvent.PointerCoords().apply {
                    x = bounds.left + bounds.width * (0.2f + index * 0.2f)
                    y = bounds.center.y
                    pressure = if (index == 0) 0.2f else 0.9f
                    size = 1f
                }
                composeRule.runOnUiThread {
                    val event = MotionEvent.obtain(
                        downTime, downTime + index * 20L, action, 1, arrayOf(properties), arrayOf(coordinates),
                        0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0,
                    )
                    try {
                        composeRule.activity.dispatchTouchEvent(event)
                    } finally {
                        event.recycle()
                    }
                }
            }
            composeRule.runOnIdle {
                assertTrue("No stroke points for $next", points.size >= 2)
                assertEquals(if (next == PatternAnnotationTool.PEN) 0.2f else 1f, points.first().pressure, 0.001f)
                assertEquals(if (next == PatternAnnotationTool.PEN) 0.9f else 1f, points.last().pressure, 0.001f)
            }
        }
        composeRule.runOnIdle { assertEquals(3, commits) }
    }
}
