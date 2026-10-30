package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage

internal data class PhotoViewportState(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    fun offset(
        viewport: Size,
        image: Size,
    ): Offset {
        if (image.width <= 0f || image.height <= 0f) return Offset.Zero
        val fit = minOf(viewport.width / image.width, viewport.height / image.height)
        val maxX = ((image.width * fit * scale - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((image.height * fit * scale - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset((panX * viewport.width).coerceIn(-maxX, maxX), (panY * viewport.height).coerceIn(-maxY, maxY))
    }

    fun transform(
        zoom: Float,
        pan: Offset,
        viewport: Size,
        image: Size,
    ): PhotoViewportState {
        val nextScale = (scale * zoom).coerceIn(1f, 5f)
        if (nextScale == 1f || viewport.width <= 0f || viewport.height <= 0f) return PhotoViewportState()
        val moved = offset(viewport, image) + pan
        val next = PhotoViewportState(nextScale, moved.x / viewport.width, moved.y / viewport.height)
        val bounded = next.offset(viewport, image)
        return next.copy(panX = bounded.x / viewport.width, panY = bounded.y / viewport.height)
    }

    companion object {
        val Saver =
            listSaver<PhotoViewportState, Float>(
                save = { listOf(it.scale, it.panX, it.panY) },
                restore = { PhotoViewportState(it[0], it[1], it[2]) },
            )
    }
}

@Composable
internal fun PhotoViewport(
    uri: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    var state by rememberSaveable(uri, stateSaver = PhotoViewportState.Saver) { mutableStateOf(PhotoViewportState()) }
    var imageSize by remember(uri) { mutableStateOf(Size.Zero) }
    BoxWithConstraints(modifier.clipToBounds().testTag("photo-viewport")) {
        val viewport = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val offset = state.offset(viewport, imageSize)
        val gestures =
            rememberTransformableState { _, zoom, pan, _ ->
                state = state.transform(zoom, pan, viewport, imageSize)
            }
        AsyncImage(
            model = uri,
            contentDescription = description,
            onSuccess = { imageSize = it.painter.intrinsicSize },
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(uri) { detectTapGestures(onDoubleTap = { state = PhotoViewportState() }) }
                    .transformable(gestures, canPan = { state.scale > 1f })
                    .graphicsLayer {
                        scaleX = state.scale
                        scaleY = state.scale
                        translationX = offset.x
                        translationY = offset.y
                    },
        )
    }
}
