package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import java.io.File

@Composable
@Suppress("kotlin:S107") // Tuontipinta pitää jokaisen sivutoiminnon erillisenä callbackina.
internal fun PatternImageImportSurface(
    state: PatternImageImportUiState,
    onAddMore: () -> Unit,
    onMoveEarlier: (String) -> Unit,
    onMoveLater: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
    onPreviewFailed: (String) -> Unit,
) {
    val busy = state.isBusy
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.pattern_image_import_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text =
                pluralStringResource(
                    R.plurals.pattern_images_selected,
                    state.selection.pages.size,
                    state.selection.pages.size,
                ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageResource()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        if (state.duplicatesIgnored > 0) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.pattern_image_duplicates_ignored,
                        state.duplicatesIgnored,
                        state.duplicatesIgnored,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = state.selection.pages,
                key = { _, page -> page.id },
            ) { index, page ->
                PatternImagePageRow(
                    page = page,
                    index = index,
                    totalPages = state.selection.pages.size,
                    enabled = !busy,
                    onMoveEarlier = { onMoveEarlier(page.id) },
                    onMoveLater = { onMoveLater(page.id) },
                    onRemove = { onRemove(page.id) },
                    onPreviewFailed = { onPreviewFailed(page.id) },
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PatternImageProgress(state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onAddMore,
                    enabled =
                        !busy &&
                            state.origin == PatternImageImportOrigin.GALLERY &&
                            state.selection.pages.size < PatternImageImportLimits.MAX_PAGES,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.pattern_image_add_more))
                }
                Button(
                    onClick = onCreate,
                    enabled = !busy && state.selection.pages.isNotEmpty() && state.invalidPageIds.isEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.pattern_image_create_pdf))
                }
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (busy) R.string.pattern_image_cancel_conversion else R.string.cancel,
                    ),
                )
            }
        }
    }

    if (state.replacementConfirmationPending) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.pattern_image_replace_title)) },
            text = { Text(stringResource(R.string.pattern_image_replace_message)) },
            confirmButton = {
                TextButton(onClick = onCreate) {
                    Text(stringResource(R.string.pattern_image_replace_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
@Suppress("kotlin:S107") // Sivurivi välittää järjestys- ja poistotoiminnot eksplisiittisesti.
private fun PatternImagePageRow(
    page: StagedPatternPage,
    index: Int,
    totalPages: Int,
    enabled: Boolean,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
    onPreviewFailed: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = File(page.stagedPath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { onPreviewFailed() },
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(R.string.pattern_image_page_position, index + 1, totalPages),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = onMoveEarlier,
                enabled = enabled && index > 0,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pattern_image_move_earlier, index + 1),
                )
            }
            IconButton(
                onClick = onMoveLater,
                enabled = enabled && index < totalPages - 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.pattern_image_move_later, index + 1),
                )
            }
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.pattern_image_remove_page, index + 1),
                )
            }
        }
    }
}

@Composable
private fun PatternImageProgress(state: PatternImageImportUiState) {
    when (state.phase) {
        PatternImageImportPhase.CONVERTING -> {
            val progress = state.progress
            LinearProgressIndicator(
                progress = {
                    if (progress == null || progress.totalPages == 0) {
                        0f
                    } else {
                        progress.currentPage.toFloat() / progress.totalPages
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (progress != null) {
                Text(
                    text =
                        stringResource(
                            R.string.pattern_image_conversion_progress,
                            progress.currentPage,
                            progress.totalPages,
                        ),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        PatternImageImportPhase.ATTACHING ->
            Text(
                text = stringResource(R.string.pattern_image_attaching),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

        else -> Unit
    }
}

private fun PatternImageImportError.messageResource(): Int =
    when (this) {
        PatternImageImportError.PAGE_LIMIT -> R.string.pattern_image_error_max_count
        PatternImageImportError.IMAGE_TOO_LARGE -> R.string.pattern_image_error_image_too_large
        PatternImageImportError.TOTAL_TOO_LARGE -> R.string.pattern_image_error_total_too_large
        PatternImageImportError.UNSUPPORTED -> R.string.pattern_image_error_unsupported
        PatternImageImportError.UNREADABLE -> R.string.pattern_image_error_unreadable
        PatternImageImportError.ANIMATED -> R.string.pattern_image_error_animated
        PatternImageImportError.LOW_STORAGE -> R.string.pattern_image_error_low_storage
        PatternImageImportError.STAGING -> R.string.pattern_image_error_staging
        PatternImageImportError.CONVERSION -> R.string.pattern_image_error_conversion
        PatternImageImportError.ATTACHMENT -> R.string.pattern_image_error_attachment
    }
