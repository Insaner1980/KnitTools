package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.theme.ComponentDimens

/**
 * Lukijan merkintätila. Lukutilassa ohje täyttää näytön; työkalut tulevat esiin vasta
 * Merkitse-toiminnosta. Aina näkyvät tasot ja työkalurivi veivät 40 % näytöstä.
 */
internal data class AnnotationModeControls(
    val active: Boolean,
    val toggle: () -> Unit,
    val showLayers: Boolean,
    val openLayers: () -> Unit,
    val closeLayers: () -> Unit,
)

@Composable
internal fun rememberAnnotationModeControls(
    patternUri: String?,
    activeTool: PatternAnnotationTool,
    onToolSelected: (PatternAnnotationTool) -> Unit,
): AnnotationModeControls {
    var active by rememberSaveable(patternUri) { mutableStateOf(false) }
    var showLayers by rememberSaveable(patternUri) { mutableStateOf(false) }
    // Lukutilassa kosketus selaa: edellisellä kerralla valittu kynä ei saa jäädä piirtämään.
    LaunchedEffect(active, activeTool) {
        if (!active && activeTool != PatternAnnotationTool.BROWSE) onToolSelected(PatternAnnotationTool.BROWSE)
    }
    return AnnotationModeControls(
        active = active,
        toggle = {
            val entering = !active
            active = entering
            if (entering && activeTool == PatternAnnotationTool.BROWSE) onToolSelected(PatternAnnotationTool.PEN)
        },
        showLayers = showLayers,
        openLayers = { showLayers = true },
        closeLayers = { showLayers = false },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PatternAnnotationLayersSheet(
    state: PatternAnnotationUiState,
    onMasterVisibilityChange: (Boolean) -> Unit,
    onProjectVisibilityChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ComponentDimens.FormSheetHorizontalPadding)
                    .padding(bottom = ComponentDimens.LargeContentPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
        ) {
            SheetTitle(text = stringResource(R.string.pattern_annotations_layers))
            PatternAnnotationLayerPanel(
                state = state,
                onMasterVisibilityChange = onMasterVisibilityChange,
                onProjectVisibilityChange = onProjectVisibilityChange,
                showTitle = false,
            )
        }
    }
}
