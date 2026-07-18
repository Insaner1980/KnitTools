package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternAnnotationOwner

@Composable
internal fun PatternAnnotationLayerPanel(
    state: PatternAnnotationUiState,
    onMasterVisibilityChange: (Boolean) -> Unit,
    onProjectVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val projectViewer = state.owner is PatternAnnotationOwner.Project
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.pattern_annotations_layers),
                style = MaterialTheme.typography.labelLarge,
            )
            PatternAnnotationLayerRow(
                title = stringResource(R.string.pattern_annotations_master),
                annotationCount = state.masterAnnotations.size,
                visible = state.masterLayerVisible,
                readOnly = projectViewer,
                onVisibilityChange = onMasterVisibilityChange,
            )
            if (projectViewer) {
                PatternAnnotationLayerRow(
                    title = stringResource(R.string.pattern_annotations_project),
                    annotationCount = state.projectAnnotations.size,
                    visible = state.projectLayerVisible,
                    readOnly = false,
                    onVisibilityChange = onProjectVisibilityChange,
                )
            }
        }
    }
}

@Composable
private fun PatternAnnotationLayerRow(
    title: String,
    annotationCount: Int,
    visible: Boolean,
    readOnly: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pattern_annotations_count, title, annotationCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (readOnly) {
                Text(
                    text = stringResource(R.string.pattern_annotations_read_only),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = visible,
            onCheckedChange = onVisibilityChange,
        )
    }
}
