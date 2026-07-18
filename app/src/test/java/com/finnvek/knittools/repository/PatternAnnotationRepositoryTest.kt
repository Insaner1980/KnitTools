package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PatternAnnotationRepositoryTest {
    @Test
    fun `export annotations preserve requested layer order`() =
        runTest {
            val master = annotation(id = 1L, layerId = 100L, zIndex = 10L)
            val project = annotation(id = 2L, layerId = 200L, zIndex = 0L)
            val repository =
                PatternAnnotationRepository(
                    FakePatternAnnotationDao(
                        patternAnnotations = listOf(project.toEntity(), master.toEntity()),
                    ),
                )

            val result = repository.getForLayers(listOf(master.layerId, project.layerId))

            assertEquals(listOf(master, project), result)
        }

    private fun annotation(
        id: Long,
        layerId: Long,
        zIndex: Long,
    ) = PatternAnnotation(
        id = id,
        layerId = layerId,
        page = 0,
        kind = PatternAnnotationKind.LINE,
        payload =
            ShapePayload(
                start = NormalizedPatternPoint(0.1f, 0.1f),
                end = NormalizedPatternPoint(0.9f, 0.9f),
                strokeArgb = 0xFF000000.toInt(),
                strokeWidth = 2f,
            ),
        zIndex = zIndex,
        createdAt = id,
        updatedAt = id,
    )
}
