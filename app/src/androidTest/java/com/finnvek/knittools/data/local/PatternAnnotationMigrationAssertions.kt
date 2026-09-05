package com.finnvek.knittools.data.local

import android.graphics.Color
import com.finnvek.knittools.domain.model.FreehandPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

internal fun assertMigratedLegacyFreehand(
    layerId: Long,
    page: Int,
    kind: String,
    payloadVersion: Int,
    payloadJson: String,
    zIndex: Long,
    createdAt: Long,
    updatedAt: Long,
    expectedPathData: String,
    expectedColor: String,
    expectedStrokeWidth: Float,
) {
    val annotation =
        requireNotNull(
            PatternAnnotationEntity(
                id = 1L,
                layerId = layerId,
                page = page,
                kind = kind,
                payloadVersion = payloadVersion,
                payloadJson = payloadJson,
                zIndex = zIndex,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ).toDomain(),
        )
    val payload = annotation.payload as FreehandPayload

    assertTrue(payload.points.isEmpty())
    assertEquals(Color.parseColor(expectedColor), payload.argb)
    assertEquals(expectedStrokeWidth, payload.strokeWidth, 0.0001f)
    assertFalse(payload.pressureEnabled)
    assertEquals(expectedPathData, payload.legacyPathData)
    assertEquals(expectedColor, payload.legacyColor)
}
