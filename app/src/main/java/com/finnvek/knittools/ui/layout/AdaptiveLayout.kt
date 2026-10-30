package com.finnvek.knittools.ui.layout

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class ContentWidthPolicy(
    val maxWidth: Dp,
) {
    Form(600.dp),
    ListDetail(840.dp),
    Chart(1200.dp),
    Unrestricted(Dp.Infinity),
}

fun Modifier.contentWidth(policy: ContentWidthPolicy): Modifier = widthIn(max = policy.maxWidth)

enum class AdaptiveWidth {
    Compact,
    Medium,
    Expanded,
}

/** Measured constraints of this host, after its parent's navigation and inset allocation. */
@Immutable
data class AdaptiveLayoutSize(
    val width: Dp,
    val height: Dp,
) {
    val widthClass: AdaptiveWidth
        get() =
            when {
                width < 600.dp -> AdaptiveWidth.Compact
                width < 840.dp -> AdaptiveWidth.Medium
                else -> AdaptiveWidth.Expanded
            }

    val isCompactHeight: Boolean get() = height < 480.dp
    val useNavigationRail: Boolean get() = widthClass != AdaptiveWidth.Compact && !isCompactHeight
}

/** Re-provided by AdaptiveContent at narrower content boundaries. */
val LocalAdaptiveContentSize = compositionLocalOf { AdaptiveLayoutSize(0.dp, 0.dp) }

@Immutable
internal data class WindowFold(
    val bounds: IntRect,
    val isVertical: Boolean,
    val isSeparating: Boolean,
    val isOccluding: Boolean,
)

internal val LocalWindowFolds = compositionLocalOf<List<WindowFold>> { emptyList() }

/** Keep one content pane; ties prefer the top/start pane. Coordinates are physical window pixels. */
internal fun unobstructedWindowBounds(
    available: IntRect,
    folds: List<WindowFold>,
    layoutDirection: LayoutDirection,
): IntRect {
    var panes = listOf(available)
    for (fold in folds.filter { it.isSeparating || it.isOccluding }) {
        panes = panes.flatMap { pane -> splitAroundFold(pane, fold) }
    }
    return panes.maxWithOrNull(
        compareBy<IntRect> { it.width.toLong() * it.height }
            .thenBy { -it.top }
            .thenBy { if (layoutDirection == LayoutDirection.Ltr) -it.left else it.right },
    ) ?: available
}

private fun splitAroundFold(
    pane: IntRect,
    fold: WindowFold,
): List<IntRect> {
    val hinge = fold.bounds
    val intersects =
        hinge.left < pane.right && hinge.right > pane.left && hinge.top < pane.bottom && hinge.bottom > pane.top
    if (!intersects) return listOf(pane)
    val candidates =
        if (fold.isVertical) {
            listOf(
                IntRect(pane.left, pane.top, hinge.left.coerceIn(pane.left, pane.right), pane.bottom),
                IntRect(hinge.right.coerceIn(pane.left, pane.right), pane.top, pane.right, pane.bottom),
            )
        } else {
            listOf(
                IntRect(pane.left, pane.top, pane.right, hinge.top.coerceIn(pane.top, pane.bottom)),
                IntRect(pane.left, hinge.bottom.coerceIn(pane.top, pane.bottom), pane.right, pane.bottom),
            )
        }
    return candidates.filter { it.width > 0 && it.height > 0 }
}
