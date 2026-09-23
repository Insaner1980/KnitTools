package com.finnvek.knittools.data.backup

import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.ShapePayload
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupAnnotationBudgetTest {
    @Test fun countLimitAppliesWithAndWithoutIdentityTracking() {
        for (track in listOf(true, false)) {
            val budget = BackupBudget(trackEmbeddedIdentities = track)
            val row = backupAnnotationRow()
            repeat(256) { budget.observeRow("pattern_annotations", row) }
            budget.observeRow("pattern_annotations", row + ("page" to JsonPrimitive(1)))
            budget.observeRow("pattern_annotations", row + ("layerId" to JsonPrimitive(2L)))
            assertThrows(BackupException::class.java) { budget.observeRow("pattern_annotations", row) }
        }
    }

    @Test fun pointWorkLimitAppliesWithAndWithoutIdentityTracking() {
        val stroke = FreehandPayload(List(2_048) { NormalizedPatternPoint(0f, 0f) }, 0, 2f)
        val payload =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.HIGHLIGHTER, stroke),
            ).payloadJson
        val point =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(
                    PatternAnnotationKind.HIGHLIGHTER,
                    stroke.copy(points = stroke.points.take(1)),
                ),
            ).payloadJson
        for (track in listOf(true, false)) {
            val budget = BackupBudget(trackEmbeddedIdentities = track)
            val row =
                backupAnnotationRow() +
                    mapOf("kind" to JsonPrimitive("HIGHLIGHTER"), "payloadJson" to JsonPrimitive(payload))
            repeat(8) { budget.observeRow("pattern_annotations", row) }
            assertThrows(BackupException::class.java) {
                budget.observeRow("pattern_annotations", row + ("payloadJson" to JsonPrimitive(point)))
            }
        }
    }
}

internal fun backupAnnotationRow(): Map<String, JsonElement> {
    val payload = ShapePayload(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f), 0, 2f)
    return mapOf(
        "id" to JsonPrimitive(1L),
        "layerId" to JsonPrimitive(1L),
        "page" to JsonPrimitive(0),
        "payloadVersion" to JsonPrimitive(1),
        "kind" to JsonPrimitive("LINE"),
        "payloadJson" to
            JsonPrimitive(
                requireNotNull(PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.LINE, payload)).payloadJson,
            ),
    )
}
