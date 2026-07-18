package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.finnvek.knittools.data.storage.PatternAnnotationCanvasRenderer
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.ui.theme.rememberPatternAnnotationRenderStyle

@Composable
internal fun PatternAnnotationOverlay(
    masterAnnotations: List<PatternAnnotation>,
    projectAnnotations: List<PatternAnnotation>,
    masterVisible: Boolean,
    projectVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val renderStyle = rememberPatternAnnotationRenderStyle()
    val visibleAnnotations =
        visiblePatternAnnotations(
            masterAnnotations = masterAnnotations,
            projectAnnotations = projectAnnotations,
            masterVisible = masterVisible,
            projectVisible = projectVisible,
        )
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            PatternAnnotationCanvasRenderer.render(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                annotations = visibleAnnotations,
                style = renderStyle,
            )
        }
    }
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
