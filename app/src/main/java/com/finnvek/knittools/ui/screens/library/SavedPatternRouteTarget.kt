package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.ui.screens.pattern.isLocalPatternUri

internal sealed interface SavedPatternRouteTarget {
    data class LocalPattern(
        val savedPatternId: Long,
    ) : SavedPatternRouteTarget

    data class RavelryPattern(
        val ravelryId: Int,
    ) : SavedPatternRouteTarget
}

internal fun SavedPattern.routeTarget(): SavedPatternRouteTarget =
    if (patternUrl.isLocalPatternUri() || ravelryId <= 0) {
        SavedPatternRouteTarget.LocalPattern(id)
    } else {
        SavedPatternRouteTarget.RavelryPattern(ravelryId)
    }
