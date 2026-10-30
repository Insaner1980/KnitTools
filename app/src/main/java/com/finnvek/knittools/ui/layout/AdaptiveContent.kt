package com.finnvek.knittools.ui.layout

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Use at a content boundary so children see available space, not physical display dimensions. */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        // Read density in composition even when the pixel constraints have not changed.
        val size =
            with(LocalDensity.current) {
                AdaptiveLayoutSize(
                    width = if (constraints.hasBoundedWidth) constraints.maxWidth.toDp() else Dp.Infinity,
                    height = if (constraints.hasBoundedHeight) constraints.maxHeight.toDp() else Dp.Infinity,
                )
            }
        CompositionLocalProvider(
            LocalAdaptiveContentSize provides size,
            content = content,
        )
    }
}

/**
 * Insets and folds constrain the existing content instance. Changing the selected pane never
 * moves content to a different composition branch or creates a second navigation host.
 */
@Composable
internal fun UnobstructedWindowContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val folds = rememberWindowFolds()
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    Layout(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .onGloballyPositioned { origin = it.positionInWindow().round() },
        content = {
            CompositionLocalProvider(LocalWindowFolds provides folds) {
                Box(Modifier.fillMaxSize().clipToBounds()) { content() }
            }
        },
    ) { measurables, constraints ->
        val available = IntRect(origin, IntSize(constraints.maxWidth, constraints.maxHeight))
        val pane = unobstructedWindowBounds(available, folds, layoutDirection)
        val placeable = measurables.single().measure(Constraints.fixed(pane.width, pane.height))
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Fold bounds are physical coordinates; do not mirror the chosen pane in RTL.
            placeable.place(pane.left - origin.x, pane.top - origin.y)
        }
    }
}

@Composable
internal fun rememberWindowFolds(): List<WindowFold> {
    val activity = LocalActivity.current
    val foldFlow =
        remember(activity) {
            activity?.let {
                WindowInfoTracker.getOrCreate(it).windowLayoutInfo(it).map { info ->
                    info.displayFeatures.filterIsInstance<FoldingFeature>().map { feature ->
                        val bounds = feature.bounds
                        WindowFold(
                            bounds = IntRect(bounds.left, bounds.top, bounds.right, bounds.bottom),
                            isVertical = feature.orientation == FoldingFeature.Orientation.VERTICAL,
                            isSeparating = feature.isSeparating,
                            isOccluding = feature.occlusionType == FoldingFeature.OcclusionType.FULL,
                        )
                    }
                }
            } ?: flowOf(emptyList())
        }
    val folds by foldFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    return folds
}
