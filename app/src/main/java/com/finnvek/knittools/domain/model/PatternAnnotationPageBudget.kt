package com.finnvek.knittools.domain.model

class PatternAnnotationPageLimitException : IllegalStateException("Pattern annotation page budget exceeded")

/** Tallennuksen, varmuuskopion ja osumatestin yhteinen budjetti yhdelle sivulle ja tasolle. */
class PatternAnnotationPageBudget {
    private var count = 0
    private var work = 0L
    private var bytes = 0L

    fun add(
        payload: PatternAnnotationPayload,
        payloadBytes: Long = 0L,
    ) {
        count++
        work += workUnits(payload)
        bytes += payloadBytes
        if (count > MAX_ANNOTATIONS || work > MAX_WORK || bytes > MAX_PAYLOAD_BYTES || payloadBytes < 0L) {
            throw PatternAnnotationPageLimitException()
        }
    }

    companion object {
        const val MAX_ANNOTATIONS = 256
        const val MAX_WORK = 16_384L
        const val MAX_PAYLOAD_BYTES = 2L * 1_024L * 1_024L
        private const val SIMPLE_WORK = 8L

        fun workUnits(payload: PatternAnnotationPayload): Long =
            when (payload) {
                is FreehandPayload -> {
                    if (payload.points.size > PatternAnnotationLimits.MAX_FREEHAND_POINTS) {
                        throw PatternAnnotationPageLimitException()
                    }
                    if (payload.points.isEmpty()) {
                        payload.legacyPathData
                            .orEmpty()
                            .length
                            .coerceAtLeast(1)
                            .toLong()
                    } else {
                        payload.points.size.toLong()
                    }
                }
                else -> SIMPLE_WORK
            }
    }
}
