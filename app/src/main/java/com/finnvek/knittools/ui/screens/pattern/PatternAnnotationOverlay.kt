package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import com.finnvek.knittools.data.storage.PatternAnnotationCanvasRenderer
import com.finnvek.knittools.domain.calculator.PatternPageCoordinateTransform
import com.finnvek.knittools.domain.calculator.PatternScreenPoint
import com.finnvek.knittools.domain.calculator.patternAnnotationBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.ui.theme.PatternAnnotationTokens
import com.finnvek.knittools.ui.theme.rememberPatternAnnotationRenderStyle

@Composable
internal fun PatternAnnotationOverlay(
    masterAnnotations: List<PatternAnnotation>,
    projectAnnotations: List<PatternAnnotation>,
    masterVisible: Boolean,
    projectVisible: Boolean,
    inProgressAnnotation: PatternAnnotation?,
    inProgressVisible: Boolean,
    selectedAnnotationId: Long?,
    modifier: Modifier = Modifier,
) {
    val renderStyle = rememberPatternAnnotationRenderStyle()
    val selectionColor = MaterialTheme.colorScheme.primary
    val visibleAnnotations =
        visiblePatternAnnotations(
            masterAnnotations = masterAnnotations,
            projectAnnotations = projectAnnotations,
            masterVisible = masterVisible,
            projectVisible = projectVisible,
        )
    val annotationsWithDraft =
        if (inProgressVisible) visibleAnnotations + listOfNotNull(inProgressAnnotation) else visibleAnnotations
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            PatternAnnotationCanvasRenderer.render(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                annotations = annotationsWithDraft,
                style = renderStyle,
            )
        }
        visibleAnnotations
            .firstOrNull { it.id == selectedAnnotationId }
            ?.let(::patternAnnotationBounds)
            ?.let { bounds ->
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(bounds.left * size.width, bounds.top * size.height),
                    size = Size((bounds.right - bounds.left) * size.width, (bounds.bottom - bounds.top) * size.height),
                    style = Stroke(width = PatternAnnotationTokens.SELECTION_OUTLINE_WIDTH),
                )
            }
    }
}

internal data class PatternAnnotationInputActions(
    val onBeginStroke: (NormalizedPatternPoint) -> Unit,
    val onAppendStrokePoint: (NormalizedPatternPoint) -> Unit,
    val onCommitStroke: (Float) -> Unit,
    val onCancelStroke: () -> Unit,
    val onEraseStroke: (NormalizedPatternPoint) -> Unit,
    val onSelectAnnotation: (NormalizedPatternPoint) -> Unit,
)

@Composable
internal fun PatternAnnotationInputOverlay(
    activeTool: PatternAnnotationTool,
    coordinateTransform: PatternPageCoordinateTransform,
    viewportScale: Float,
    pressureEnabled: Boolean,
    actions: PatternAnnotationInputActions,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier.pointerInput(activeTool, coordinateTransform, viewportScale, pressureEnabled) {
                val simplificationTolerance = 1f / (minOf(size.width, size.height) * viewportScale).coerceAtLeast(1f)
                awaitEachGesture {
                    var gestureState = PatternAnnotationPointerGestureState()
                    while (!gestureState.finished) {
                        val event = awaitPointerEvent()
                        gestureState =
                            processAnnotationPointerEvent(
                                event = event,
                                state = gestureState,
                                activeTool = activeTool,
                                coordinateTransform = coordinateTransform,
                                pressureEnabled = pressureEnabled,
                                simplificationTolerance = simplificationTolerance,
                                actions = actions,
                            )
                    }
                }
            },
    ) {}
}

private data class PatternAnnotationPointerGestureState(
    val activePointerId: Long? = null,
    val gestureTool: PatternAnnotationTool? = null,
    val finished: Boolean = false,
)

private fun processAnnotationPointerEvent(
    event: PointerEvent,
    state: PatternAnnotationPointerGestureState,
    activeTool: PatternAnnotationTool,
    coordinateTransform: PatternPageCoordinateTransform,
    pressureEnabled: Boolean,
    simplificationTolerance: Float,
    actions: PatternAnnotationInputActions,
): PatternAnnotationPointerGestureState {
    val pressedChanges = event.changes.filter(PointerInputChange::pressed)
    val stylusPresent = pressedChanges.any { it.type == PointerType.Stylus || it.type == PointerType.Eraser }
    val annotationPointers =
        if (stylusPresent) {
            pressedChanges.filter { it.type != PointerType.Touch }
        } else {
            pressedChanges
        }
    return when {
        annotationPointers.size >= 2 -> {
            if (state.activePointerId != null) actions.onCancelStroke()
            state.copy(finished = true)
        }
        state.activePointerId == null ->
            beginAnnotationPointerGesture(
                event = event,
                state = state,
                activeTool = activeTool,
                coordinateTransform = coordinateTransform,
                pressureEnabled = pressureEnabled,
                stylusPresent = stylusPresent,
                annotationPointerCount = annotationPointers.size,
                actions = actions,
            )
        else ->
            updateAnnotationPointerGesture(
                event = event,
                state = state,
                coordinateTransform = coordinateTransform,
                pressureEnabled = pressureEnabled,
                simplificationTolerance = simplificationTolerance,
                actions = actions,
            )
    }
}

private fun beginAnnotationPointerGesture(
    event: PointerEvent,
    state: PatternAnnotationPointerGestureState,
    activeTool: PatternAnnotationTool,
    coordinateTransform: PatternPageCoordinateTransform,
    pressureEnabled: Boolean,
    stylusPresent: Boolean,
    annotationPointerCount: Int,
    actions: PatternAnnotationInputActions,
): PatternAnnotationPointerGestureState {
    val down =
        event.changes.firstOrNull { change ->
            change.changedToDownIgnoreConsumed() && (!stylusPresent || change.type != PointerType.Touch)
        }
    val pointerType = down?.type?.toPatternInputPointerType()
    val handlesPointer =
        pointerType != null &&
            shouldHandleAnnotationPointer(
                activeTool = activeTool,
                pointerType = pointerType,
                activePointerCount = annotationPointerCount,
                stylusPresent = stylusPresent,
            )
    val point =
        if (handlesPointer) {
            down.toNormalizedPoint(coordinateTransform, pressureEnabled)
        } else {
            null
        }
    if (down == null || pointerType == null || point == null) {
        return state.copy(finished = event.changes.none(PointerInputChange::pressed))
    }
    val gestureTool = resolvedAnnotationTool(activeTool, pointerType)
    when (gestureTool) {
        PatternAnnotationTool.ERASER -> actions.onEraseStroke(point)
        PatternAnnotationTool.SELECT -> actions.onSelectAnnotation(point)
        else -> actions.onBeginStroke(point)
    }
    down.consume()
    return state.copy(activePointerId = down.id.value, gestureTool = gestureTool)
}

private fun updateAnnotationPointerGesture(
    event: PointerEvent,
    state: PatternAnnotationPointerGestureState,
    coordinateTransform: PatternPageCoordinateTransform,
    pressureEnabled: Boolean,
    simplificationTolerance: Float,
    actions: PatternAnnotationInputActions,
): PatternAnnotationPointerGestureState {
    val change = event.changes.firstOrNull { it.id.value == state.activePointerId }
    if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed) {
        if (state.gestureTool != PatternAnnotationTool.ERASER) {
            actions.onCommitStroke(simplificationTolerance)
        }
        change?.consume()
        return state.copy(finished = true)
    }
    val point = change.toNormalizedPoint(coordinateTransform, pressureEnabled) ?: return state
    when (state.gestureTool) {
        PatternAnnotationTool.ERASER -> actions.onEraseStroke(point)
        PatternAnnotationTool.SELECT -> Unit
        else -> actions.onAppendStrokePoint(point)
    }
    change.consume()
    return state
}

private fun PointerInputChange.toNormalizedPoint(
    transform: PatternPageCoordinateTransform,
    pressureEnabled: Boolean,
): NormalizedPatternPoint? {
    val normalized = transform.screenToPage(PatternScreenPoint(position.x, position.y)) ?: return null
    return normalized.copy(pressure = if (pressureEnabled) pressure.coerceIn(0f, 1f) else 1f)
}

private fun PointerType.toPatternInputPointerType(): PatternInputPointerType =
    when (this) {
        PointerType.Touch -> PatternInputPointerType.TOUCH
        PointerType.Stylus -> PatternInputPointerType.STYLUS
        PointerType.Eraser -> PatternInputPointerType.ERASER
        PointerType.Mouse -> PatternInputPointerType.MOUSE
        else -> PatternInputPointerType.UNKNOWN
    }

internal fun visiblePatternAnnotations(
    masterAnnotations: List<PatternAnnotation>,
    projectAnnotations: List<PatternAnnotation>,
    masterVisible: Boolean,
    projectVisible: Boolean,
): List<PatternAnnotation> =
    buildList {
        if (masterVisible) addAll(masterAnnotations.sortedWith(PATTERN_ANNOTATION_Z_ORDER))
        if (projectVisible) addAll(projectAnnotations.sortedWith(PATTERN_ANNOTATION_Z_ORDER))
    }

private val PATTERN_ANNOTATION_Z_ORDER = compareBy<PatternAnnotation>({ it.zIndex }, { it.id })
