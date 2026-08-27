package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.finnvek.knittools.domain.calculator.PatternPageCoordinateTransform

internal const val PATTERN_VIEWPORT_MIN_SCALE = 1f
internal const val PATTERN_VIEWPORT_MAX_SCALE = 5f

internal data class PatternViewportState(
    val scale: Float = PATTERN_VIEWPORT_MIN_SCALE,
    val offset: Offset = Offset.Zero,
) {
    fun applyTransform(
        zoomChange: Float,
        panChange: Offset,
    ): PatternViewportState {
        val nextScale = (scale * zoomChange).coerceIn(PATTERN_VIEWPORT_MIN_SCALE, PATTERN_VIEWPORT_MAX_SCALE)
        return copy(
            scale = nextScale,
            offset = if (nextScale > PATTERN_VIEWPORT_MIN_SCALE) offset + panChange else Offset.Zero,
        )
    }

    fun reset(): PatternViewportState = PatternViewportState()

    fun toPageCoordinateTransform(
        pageSize: Size,
        viewportOrigin: Offset = Offset.Zero,
    ): PatternPageCoordinateTransform {
        val transformedWidth = pageSize.width * scale
        val transformedHeight = pageSize.height * scale
        return PatternPageCoordinateTransform(
            pageLeft = viewportOrigin.x + (pageSize.width - transformedWidth) / 2f,
            pageTop = viewportOrigin.y + (pageSize.height - transformedHeight) / 2f,
            pageWidth = pageSize.width,
            pageHeight = pageSize.height,
            scale = scale,
            offsetX = offset.x,
            offsetY = offset.y,
        )
    }
}

internal data class PatternViewportLayout(
    val state: PatternViewportState,
    val coordinateTransform: PatternPageCoordinateTransform,
)

internal data class PatternViewportFocusRequest(
    val requestId: Long,
    val pageIndex: Int,
    val yFraction: Float,
)

@Composable
internal fun PatternDocumentViewport(
    renderedBitmapProvider: @Composable () -> ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    currentPage: Int = 0,
    focusRequest: PatternViewportFocusRequest? = null,
    onFocusRequestConsumed: (Long) -> Unit = {},
    overlay: @Composable BoxScope.(PatternViewportLayout) -> Unit = {},
    interactionOverlay: @Composable BoxScope.(PatternViewportLayout) -> Unit = {},
) {
    val renderedBitmap = renderedBitmapProvider()
    var viewportState by remember { mutableStateOf(PatternViewportState()) }
    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var pageHeightPx by remember { mutableIntStateOf(0) }
    val transformableState =
        rememberTransformableState { _, zoomChange, panChange, _ ->
            viewportState = viewportState.applyTransform(zoomChange, panChange)
        }

    val eligibleFocusRequest =
        eligiblePatternViewportFocusRequest(
            currentPage = currentPage,
            renderedPageReady = renderedBitmap.width > 0 && renderedBitmap.height > 0,
            request = focusRequest,
        )
    LaunchedEffect(
        eligibleFocusRequest?.requestId,
        viewportHeightPx,
        pageHeightPx,
        scrollState.maxValue,
    ) {
        val request = eligibleFocusRequest ?: return@LaunchedEffect
        if (viewportHeightPx <= 0 || pageHeightPx <= 0) return@LaunchedEffect
        val target =
            patternViewportFocusScrollOffset(
                pageHeightPx = pageHeightPx,
                viewportHeightPx = viewportHeightPx,
                yFraction = request.yFraction,
                scale = viewportState.scale,
                translationY = viewportState.offset.y,
            ).coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
        onFocusRequestConsumed(request.requestId)
    }

    Column(
        modifier =
            modifier
                .onSizeChanged { viewportHeightPx = it.height }
                .verticalScroll(scrollState),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val aspectRatio = renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat()
            val density = LocalDensity.current
            val pageSize =
                with(density) {
                    Size(
                        width = maxWidth.toPx(),
                        height = (maxWidth / aspectRatio).toPx(),
                    )
                }
            val layout =
                PatternViewportLayout(
                    state = viewportState,
                    coordinateTransform = viewportState.toPageCoordinateTransform(pageSize),
                )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(maxWidth / aspectRatio)
                        .onSizeChanged { pageHeightPx = it.height }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { viewportState = viewportState.reset() })
                        }.transformable(state = transformableState),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = viewportState.scale,
                                scaleY = viewportState.scale,
                                translationX = viewportState.offset.x,
                                translationY = viewportState.offset.y,
                            ),
                ) {
                    Image(
                        bitmap = renderedBitmap,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxSize(),
                    )
                    overlay(layout)
                }
                interactionOverlay(layout)
            }
        }
    }
}

internal fun eligiblePatternViewportFocusRequest(
    currentPage: Int,
    renderedPageReady: Boolean,
    request: PatternViewportFocusRequest?,
): PatternViewportFocusRequest? = request?.takeIf { renderedPageReady && it.pageIndex == currentPage }

internal fun patternViewportFocusScrollOffset(
    pageHeightPx: Int,
    viewportHeightPx: Int,
    yFraction: Float,
    scale: Float,
    translationY: Float,
): Int {
    val pageCenter = pageHeightPx / 2f
    val targetOnTransformedPage =
        pageCenter + (((pageHeightPx * yFraction.coerceIn(0f, 1f)) - pageCenter) * scale) + translationY
    return (targetOnTransformedPage - (viewportHeightPx / 2f)).toInt().coerceAtLeast(0)
}

internal fun clampPatternPage(
    page: Int,
    pageCount: Int,
): Int = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
