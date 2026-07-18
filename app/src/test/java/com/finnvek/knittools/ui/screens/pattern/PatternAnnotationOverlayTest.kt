package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternAnnotationOverlayTest {
    @Test
    fun `master is rendered below project and each layer follows z order`() {
        val master = listOf(annotation(id = 2L, layerId = 10L, zIndex = 9L), annotation(1L, 10L, 1L))
        val project = listOf(annotation(id = 4L, layerId = 20L, zIndex = 8L), annotation(3L, 20L, 2L))

        val ordered =
            visiblePatternAnnotations(
                masterAnnotations = master,
                projectAnnotations = project,
                masterVisible = true,
                projectVisible = true,
            )

        assertEquals(listOf(1L, 2L, 3L, 4L), ordered.map(PatternAnnotation::id))
    }

    @Test
    fun `hidden layers are excluded independently`() {
        val master = listOf(annotation(1L, 10L, 0L))
        val project = listOf(annotation(2L, 20L, 0L))

        assertEquals(
            listOf(2L),
            visiblePatternAnnotations(master, project, masterVisible = false, projectVisible = true)
                .map(PatternAnnotation::id),
        )
        assertEquals(
            listOf(1L),
            visiblePatternAnnotations(master, project, masterVisible = true, projectVisible = false)
                .map(PatternAnnotation::id),
        )
    }

    private fun annotation(
        id: Long,
        layerId: Long,
        zIndex: Long,
    ) = PatternAnnotation(
        id = id,
        layerId = layerId,
        page = 0,
        kind = PatternAnnotationKind.FREEHAND,
        payload =
            FreehandPayload(
                points = listOf(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f)),
                argb = 0xFF000000.toInt(),
                strokeWidth = 2f,
            ),
        zIndex = zIndex,
    )
}
