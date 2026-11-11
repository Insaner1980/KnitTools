package com.finnvek.knittools.ui.screens.pattern

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.finnvek.knittools.domain.calculator.PatternPageCoordinateTransform
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class PatternAnnotationPointerCancellationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun documentReplacementCancelsOldPointerBeforeNextStrokePersists() {
        val fixture = AnnotationTextTestFixture()
        val documentKey = mutableStateOf("first-document")
        val cancellations = mutableListOf<String>()
        try {
            runBlocking { fixture.start() }
            compose.runOnIdle { fixture.vm.setActiveTool(PatternAnnotationTool.PEN) }
            compose.setContent {
                val key = documentKey.value
                rememberSaveableStateHolder().SaveableStateProvider("$key:0") {
                    PatternAnnotationInputOverlay(
                        activeTool = PatternAnnotationTool.PEN,
                        coordinateTransform = PatternPageCoordinateTransform(0f, 0f, 1_000f, 1_000f, scale = 1f),
                        viewportScale = 1f,
                        pressureEnabled = false,
                        actions =
                            PatternAnnotationInputActions(
                                onBeginStroke = fixture.vm::beginStroke,
                                onAppendStrokePoint = fixture.vm::appendStrokePoint,
                                onCommitStroke = fixture.vm::commitStroke,
                                onCancelStroke = {
                                    cancellations += key
                                    fixture.vm.cancelStroke()
                                },
                                onEraseStroke = { fixture.vm.eraseStrokeAt(it) },
                                onSelectAnnotation = { fixture.vm.selectAnnotationAt(it) },
                            ),
                        modifier = Modifier.fillMaxSize().testTag("annotation-input"),
                    )
                }
            }
            val input = compose.onNodeWithTag("annotation-input")
            input.performTouchInput {
                down(percentOffset(0.2f, 0.3f))
                moveTo(percentOffset(0.4f, 0.3f))
            }
            compose.runOnIdle { assertNotNull(fixture.vm.uiState.value.draftStroke) }
            runBlocking {
                fixture.layers.activateProjectLayer(1, "next-document")
                fixture.awaitState { it.editableLayerId == fixture.nextLayer && it.draftStroke == null }
            }
            compose.runOnIdle { documentKey.value = "next-document" }
            compose.waitForIdle()
            input.performTouchInput { cancel() }
            compose.runOnIdle { assertEquals(listOf("first-document"), cancellations) }

            input.performTouchInput {
                down(percentOffset(0.2f, 0.7f))
                moveTo(percentOffset(0.6f, 0.7f))
            }
            compose.runOnIdle {
                assertNotNull(fixture.vm.uiState.value.draftStroke)
                assertEquals(listOf("first-document"), cancellations)
            }
            input.performTouchInput { up() }
            runBlocking {
                fixture.awaitState { !it.isSaving && it.canUndo && it.projectAnnotations.size == 1 }
                val original = fixture.repository.getForLayers(listOf(fixture.layer)).single()
                assertEquals(PatternAnnotationKind.TEXT_BOX, original.kind)
                val saved = fixture.repository.getForLayers(listOf(fixture.nextLayer)).single()
                assertEquals(PatternAnnotationKind.FREEHAND, saved.kind)
                assertEquals(fixture.nextLayer, saved.layerId)
                assertEquals(0, saved.page)
                assertFalse((saved.payload as FreehandPayload).points.isEmpty())
            }
        } finally {
            runBlocking { fixture.close() }
        }
    }
}
