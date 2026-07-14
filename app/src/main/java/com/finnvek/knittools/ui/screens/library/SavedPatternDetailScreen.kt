package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.screens.ravelry.openRavelryUrl
import com.finnvek.knittools.ui.screens.ravelry.ravelryExternalUrlOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPatternDetailScreen(
    pattern: SavedPattern,
    onBack: () -> Unit,
    onOpenPattern: () -> Unit,
    onAttachToProject: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    deleteErrorId: Long = 0L,
) {
    var showRemoveConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val openFailedMessage = stringResource(R.string.pattern_open_failed)
    val deleteFailedMessage = stringResource(R.string.generic_error_unknown)
    val ravelryUrl = pattern.ravelryUrlOrNull()

    LaunchedEffect(deleteErrorId) {
        if (deleteErrorId > 0L) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
        }
    }

    if (showRemoveConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.remove_pattern),
            message = stringResource(R.string.saved_pattern_detail_remove_confirm),
            confirmText = stringResource(R.string.remove_pattern),
            isDestructive = true,
            onConfirm = {
                showRemoveConfirmDialog = false
                onRemove()
            },
            onDismiss = { showRemoveConfirmDialog = false },
        )
    }

    ToolScreenScaffold(
        title = pattern.name,
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SavedPatternDetailHeader(pattern = pattern)
            SavedPatternAvailability(pattern = pattern, canOpenRavelry = ravelryUrl != null)
            SavedPatternDetailActions(
                canOpenPattern = pattern.hasAttachedPdf,
                canOpenRavelry = ravelryUrl != null,
                onOpenPattern = onOpenPattern,
                onOpenRavelry = {
                    ravelryUrl?.let { url ->
                        openRavelryUrl(
                            context = context,
                            url = url,
                            failureMessage = openFailedMessage,
                        )
                    }
                },
                onAttachToProject = onAttachToProject,
                onRemove = { showRemoveConfirmDialog = true },
            )
        }
    }
}

@Composable
private fun SavedPatternDetailHeader(pattern: SavedPattern) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pattern.thumbnailUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { thumbnailUrl ->
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = stringResource(R.string.saved_pattern_detail_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .clip(RoundedCornerShape(12.dp)),
                )
            }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = pattern.designerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPatternAvailability(
    pattern: SavedPattern,
    canOpenRavelry: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pattern.hasAttachedPdf) {
            SavedPatternAvailabilityChip(text = stringResource(R.string.saved_pattern_detail_pdf_attached))
        }
        if (pattern.isAvailableOffline) {
            SavedPatternAvailabilityChip(text = stringResource(R.string.saved_pattern_detail_available_offline))
        }
        if (canOpenRavelry) {
            SavedPatternAvailabilityChip(text = stringResource(R.string.saved_pattern_detail_open_on_ravelry))
        }
        if (pattern.requiresRavelryAccess) {
            SavedPatternAvailabilityChip(text = stringResource(R.string.saved_pattern_detail_requires_ravelry))
        }
    }
}

@Composable
private fun SavedPatternAvailabilityChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SavedPatternDetailActions(
    canOpenPattern: Boolean,
    canOpenRavelry: Boolean,
    onOpenPattern: () -> Unit,
    onOpenRavelry: () -> Unit,
    onAttachToProject: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onOpenPattern,
            enabled = canOpenPattern,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Filled.AutoStories, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.saved_pattern_detail_open_pattern))
        }
        OutlinedButton(
            onClick = onOpenRavelry,
            enabled = canOpenRavelry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Filled.AutoStories, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.open_in_ravelry))
        }
        OutlinedButton(
            onClick = onAttachToProject,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.saved_pattern_detail_attach_to_project))
        }
        TextButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.remove_pattern))
        }
    }
}

private val SavedPattern.hasAttachedPdf: Boolean
    get() = !localPdfUri.isNullOrBlank()

private val SavedPattern.requiresRavelryAccess: Boolean
    get() = source == SavedPatternSource.Ravelry && !hasAttachedPdf

private fun SavedPattern.ravelryUrlOrNull(): String? =
    canonicalUrl
        .ifBlank { originalUrl }
        .let(::ravelryExternalUrlOrNull)
