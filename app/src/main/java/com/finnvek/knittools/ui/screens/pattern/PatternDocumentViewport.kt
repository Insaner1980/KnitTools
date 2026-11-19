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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
@Suppress("kotlin:S107") // Viewport välittää renderöinnin, sivun ja fokuspyynnön tilat eksplisiittisesti.
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
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var scrollFraction by rememberSaveable { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var pageHeightPx by remember { mutableIntStateOf(0) }
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val aspectRatio = patternBitmapAspectRatio(renderedBitmap.width, renderedBitmap.height)
        val pageHeight = maxWidth / aspectRatio
        val pageSize =
            with(LocalDensity.current) {
                Size(maxWidth.toPx(), (maxWidth / aspectRatio).toPx())
            }
        val viewportState = PatternViewportState(scale, Offset(panX * pageSize.width, panY * pageSize.height))

        fun updateViewport(next: PatternViewportState) {
            scale = next.scale
            panX = next.offset.x / pageSize.width.coerceAtLeast(1f)
            panY = next.offset.y / pageSize.height.coerceAtLeast(1f)
        }
        val transformableState =
            rememberTransformableState { _, zoomChange, panChange, _ ->
                val current = PatternViewportState(scale, Offset(panX * pageSize.width, panY * pageSize.height))
                updateViewport(current.applyTransform(zoomChange, panChange))
            }
        val layout = PatternViewportLayout(viewportState, viewportState.toPageCoordinateTransform(pageSize))
        // Keep the page-relative top anchor; pixel offsets from another viewport are not portable.
        LaunchedEffect(pageHeightPx, viewportHeightPx, scrollState.maxValue) {
            if (pageHeightPx <= 0 || viewportHeightPx <= 0) return@LaunchedEffect
            scrollState.scrollTo((scrollFraction * pageHeightPx).toInt().coerceIn(0, scrollState.maxValue))
            var wasScrolling = false
            snapshotFlow { scrollState.value to scrollState.isScrollInProgress }.collect { (value, scrolling) ->
                // Layout clamping during a resize must not overwrite the saved reading position.
                if (scrolling || wasScrolling) scrollFraction = value.toFloat() / pageHeightPx
                wasScrolling = scrolling
            }
        }
        val eligibleFocusRequest =
            eligiblePatternViewportFocusRequest(
                currentPage = currentPage,
                renderedPageReady = renderedBitmap.width > 0 && renderedBitmap.height > 0,
                request = focusRequest,
            )
        LaunchedEffect(eligibleFocusRequest?.requestId, viewportHeightPx, pageHeightPx, scrollState.maxValue) {
            val request = eligibleFocusRequest ?: return@LaunchedEffect
            if (viewportHeightPx <= 0 || pageHeightPx <= 0) return@LaunchedEffect
            val focusViewport = viewportState.reset()
            updateViewport(focusViewport)
            val target =
                patternViewportFocusScrollOffset(
                    pageHeightPx = pageHeightPx,
                    viewportHeightPx = viewportHeightPx,
                    yFraction = request.yFraction,
                    scale = focusViewport.scale,
                    translationY = focusViewport.offset.y,
                ).coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
            scrollFraction = target.toFloat() / pageHeightPx
            onFocusRequestConsumed(request.requestId)
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(scrollState),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(pageHeight)
                        .onSizeChanged { pageHeightPx = it.height }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { updateViewport(viewportState.reset()) })
                        }.transformable(state = transformableState, canPan = { viewportState.scale > 1f }),
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().graphicsLayer(
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

private const val DEFAULT_PAGE_ASPECT_RATIO = 0.707f

internal fun patternBitmapAspectRatio(
    width: Int,
    height: Int,
): Float = if (width > 0 && height > 0) width.toFloat() / height else DEFAULT_PAGE_ASPECT_RATIO

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
