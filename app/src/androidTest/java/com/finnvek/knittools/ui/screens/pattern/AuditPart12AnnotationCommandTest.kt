package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuditPart12AnnotationCommandTest {
    @Test
    fun everyShapeAndLiteralEditorRoundTripsThroughRoomUndoRedoAndContextSwitch() =
        runBlocking {
            val fixture = AnnotationTextTestFixture()
            try {
                fixture.start()
                val master = fixture.db.patternAnnotationDao().getPageBounded(fixture.master, 0)
                val tools =
                    listOf(
                        PatternAnnotationTool.LINE,
                        PatternAnnotationTool.ARROW,
                        PatternAnnotationTool.RECTANGLE,
                        PatternAnnotationTool.ELLIPSE,
                        PatternAnnotationTool.CHART,
                        PatternAnnotationTool.TEXT,
                        PatternAnnotationTool.CALLOUT,
                    )
                for (tool in tools) {
                    main {
                        fixture.vm.setActiveTool(tool)
                        when (tool) {
                            PatternAnnotationTool.TEXT -> {
                                fixture.vm.addTextBox("Literal äö 日本語")
                            }

                            PatternAnnotationTool.CALLOUT -> {
                                fixture.vm.addCallout(
                                    "Title",
                                    "Description",
                                    PatternCalloutSymbol.NOTE,
                                )
                            }

                            else -> {
                                fixture.vm.beginStroke(NormalizedPatternPoint(0.2f, 0.3f))
                                fixture.vm.appendStrokePoint(NormalizedPatternPoint(0.7f, 0.8f))
                                fixture.vm.commitStroke(0f)
                            }
                        }
                    }
                    val inserted =
                        fixture
                            .awaitState {
                                !it.isSaving && it.projectAnnotations.size == 2
                            }.projectAnnotations
                            .first {
                                it.id !=
                                    fixture.id
                            }
                    val expectedKind =
                        when (tool) {
                            PatternAnnotationTool.TEXT -> PatternAnnotationKind.TEXT_BOX
                            PatternAnnotationTool.CHART -> PatternAnnotationKind.CHART_REGION
                            else -> PatternAnnotationKind.valueOf(tool.name)
                        }
                    assertEquals(expectedKind, inserted.kind)
                    val persisted =
                        fixture.db
                            .patternAnnotationDao()
                            .getPageBounded(fixture.layer, 0)
                            .first {
                                it.id ==
                                    inserted.id
                            }.toDomain()
                    assertEquals(inserted.payload, persisted?.payload)
                    main { fixture.vm.undo() }
                    fixture.awaitState { !it.isSaving && it.projectAnnotations.size == 1 && it.canRedo }
                    main { fixture.vm.redo() }
                    val restored =
                        fixture
                            .awaitState {
                                !it.isSaving && it.projectAnnotations.size == 2
                            }.projectAnnotations
                            .first {
                                it.id ==
                                    inserted.id
                            }
                    assertEquals(inserted.payload, restored.payload)
                    main {
                        fixture.vm.setActiveTool(PatternAnnotationTool.LINE)
                        fixture.vm.beginStroke(NormalizedPatternPoint(0.1f, 0.1f))
                        fixture.vm.setCurrentPage(1)
                        fixture.vm.commitStroke(0f)
                    }
                    fixture.awaitState { it.currentPage == 1 && !it.isSaving }
                    assertEquals(emptyList<Any>(), fixture.db.patternAnnotationDao().getPageBounded(fixture.layer, 1))
                    main { fixture.vm.setCurrentPage(0) }
                    fixture.awaitState { it.currentPage == 0 && it.projectAnnotations.size == 2 }
                    fixture.repository.deleteAnnotation(inserted.id)
                    fixture.awaitState { it.projectAnnotations.size == 1 }
                }
                assertEquals(master, fixture.db.patternAnnotationDao().getPageBounded(fixture.master, 0))
            } finally {
                fixture.close()
            }
        }

    @Test
    fun fiftyUndoCommandsRetainTheEvictedFirstInsertInRealRoom() =
        runBlocking {
            val fixture = AnnotationTextTestFixture()
            try {
                fixture.start()
                repeat(51) { index ->
                    main { fixture.vm.addTextBox("History $index") }
                    fixture.awaitState { !it.isSaving && it.projectAnnotations.size == index + 2 }
                }
                repeat(50) { index ->
                    main { fixture.vm.undo() }
                    fixture.awaitState { !it.isSaving && it.projectAnnotations.size == 51 - index }
                }
                assertFalse(fixture.vm.uiState.value.canUndo)
                main { fixture.vm.undo() }
                assertEquals(
                    2,
                    fixture.db
                        .patternAnnotationDao()
                        .getPageBounded(fixture.layer, 0)
                        .size,
                )
                assertEquals(
                    1,
                    fixture.db
                        .patternAnnotationDao()
                        .getPageBounded(fixture.master, 0)
                        .size,
                )
            } finally {
                fixture.close()
            }
        }
}
