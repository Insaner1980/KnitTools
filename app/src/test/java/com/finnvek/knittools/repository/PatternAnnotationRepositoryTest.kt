package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATIONS
import com.finnvek.knittools.data.storage.PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `export query accepts annotation boundary and rejects boundary plus one`() =
        runTest {
            val acceptedRows =
                (1L..PATTERN_PDF_EXPORT_MAX_ANNOTATIONS.toLong()).map { id ->
                    annotation(id = id, layerId = 100L, zIndex = id).toEntity()
                }
            val acceptedDao = FakePatternAnnotationDao(patternAnnotations = acceptedRows)
            val acceptedRepository = PatternAnnotationRepository(acceptedDao)

            val accepted =
                acceptedRepository.getForLayersForExport(
                    layerIds = listOf(100L),
                    maxAnnotations = PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                    maxPayloadBytes = PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
                )

            assertEquals(PATTERN_PDF_EXPORT_MAX_ANNOTATIONS + 1, acceptedDao.lastExportQueryLimit)
            assertEquals(PATTERN_PDF_EXPORT_MAX_ANNOTATIONS, accepted.size)

            val rejectedDao =
                FakePatternAnnotationDao(
                    patternAnnotations = acceptedRows + annotation(2_001L, 100L, 2_001L).toEntity(),
                )
            val rejectedRepository = PatternAnnotationRepository(rejectedDao)
            val failure =
                runCatching {
                    rejectedRepository.getForLayersForExport(
                        layerIds = listOf(100L),
                        maxAnnotations = PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                        maxPayloadBytes = PATTERN_PDF_EXPORT_MAX_ANNOTATION_PAYLOAD_BYTES,
                    )
                }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
            assertEquals(null, rejectedDao.lastExportQueryLimit)
        }

    @Test
    fun `export rejects aggregate payload bytes before row materialization`() =
        runTest {
            val oversized =
                annotation(id = 1L, layerId = 100L, zIndex = 0L)
                    .toEntity()
                    .copy(payloadJson = "x".repeat(11))
            val dao = FakePatternAnnotationDao(patternAnnotations = listOf(oversized))
            val repository = PatternAnnotationRepository(dao)

            val failure =
                runCatching {
                    repository.getForLayersForExport(
                        layerIds = listOf(100L),
                        maxAnnotations = PATTERN_PDF_EXPORT_MAX_ANNOTATIONS,
                        maxPayloadBytes = 10L,
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertEquals(null, dao.lastExportQueryLimit)
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
