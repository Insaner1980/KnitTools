package com.finnvek.knittools.ui.screens.pattern

import android.graphics.Bitmap
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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

@Composable
internal fun PatternDocumentViewport(
    renderedBitmap: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.(PatternViewportLayout) -> Unit = {},
) {
    var viewportState by remember { mutableStateOf(PatternViewportState()) }
    val transformableState =
        rememberTransformableState { _, zoomChange, panChange, _ ->
            viewportState = viewportState.applyTransform(zoomChange, panChange)
        }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
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
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { viewportState = viewportState.reset() })
                        }.transformable(state = transformableState)
                        .graphicsLayer(
                            scaleX = viewportState.scale,
                            scaleY = viewportState.scale,
                            translationX = viewportState.offset.x,
                            translationY = viewportState.offset.y,
                        ),
            ) {
                Image(
                    bitmap = renderedBitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize(),
                )
                overlay(layout)
            }
        }
    }
}

internal fun clampPatternPage(
    page: Int,
    pageCount: Int,
): Int = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
