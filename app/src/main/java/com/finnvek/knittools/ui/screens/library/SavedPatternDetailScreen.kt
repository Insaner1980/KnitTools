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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.RemotePatternImage
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.screens.ravelry.PatternAvailabilityBadge
import com.finnvek.knittools.ui.screens.ravelry.openRavelryUrl
import com.finnvek.knittools.ui.screens.ravelry.ravelryExternalUrlOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Detail-reitti välittää lähdekohtaiset käyttäjätoiminnot eksplisiittisesti.
fun SavedPatternDetailScreen(
    pattern: SavedPattern,
    onBack: () -> Unit,
    onOpenPattern: () -> Unit,
    onAttachToProject: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWebsite: ((String) -> ExternalWebLinkOpenResult)? = null,
    onEditWebPattern: () -> Unit = {},
    onAttachWebPattern: (Long?, (SavedPatternMetadataMutationResult) -> Unit) -> Unit = { _, onResult ->
        onResult(SavedPatternMetadataMutationResult.PersistenceFailure)
    },
    deleteErrorId: Long = 0L,
) {
    var showRemoveConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var pendingReplacementId by rememberSaveable(pattern.id) { mutableStateOf<Long?>(null) }
    var webAttachInFlight by rememberSaveable(pattern.id) { mutableStateOf(false) }
    var lastHandledDeleteErrorId by rememberSaveable(pattern.id) { mutableLongStateOf(deleteErrorId) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val openFailedMessage = stringResource(R.string.pattern_open_failed)
    val ravelryUrl = pattern.ravelryUrlOrNull()
    val webUrl = pattern.webPatternUrlOrNull
    val isWebPattern = pattern.isWebPatternCompatible && webUrl != null
    val deleteFailedMessage =
        stringResource(
            if (isWebPattern) R.string.web_pattern_delete_failed else R.string.generic_error_unknown,
        )
    val attachFailedMessage = stringResource(R.string.web_pattern_save_failed)
    val projectUnavailableMessage = stringResource(R.string.web_pattern_project_unavailable)
    val noBrowserMessage = stringResource(R.string.web_pattern_no_browser)
    val webOpenFailedMessage = stringResource(R.string.web_pattern_open_failed)
    val handleWebAttachResult: (SavedPatternMetadataMutationResult) -> Unit = { result ->
        webAttachInFlight = false
        when (result) {
            is SavedPatternMetadataMutationResult.Attached,
            is SavedPatternMetadataMutationResult.AlreadyAttached,
            -> {
                pendingReplacementId = null
                onAttachToProject()
            }

            is SavedPatternMetadataMutationResult.ReplacementRequired -> {
                pendingReplacementId = result.existingSavedPatternId
            }

            SavedPatternMetadataMutationResult.ProjectMissing -> {
                pendingReplacementId = null
                coroutineScope.launch { snackbarHostState.showSnackbar(projectUnavailableMessage) }
            }

            SavedPatternMetadataMutationResult.PatternMissing,
            SavedPatternMetadataMutationResult.NotWebPattern,
            SavedPatternMetadataMutationResult.StaleAction,
            SavedPatternMetadataMutationResult.PersistenceFailure,
            SavedPatternMetadataMutationResult.Unlinked,
            SavedPatternMetadataMutationResult.AlreadyUnlinked,
            -> {
                pendingReplacementId = null
                coroutineScope.launch { snackbarHostState.showSnackbar(attachFailedMessage) }
            }
        }
    }

    LaunchedEffect(deleteErrorId) {
        if (deleteErrorId > lastHandledDeleteErrorId) {
            lastHandledDeleteErrorId = deleteErrorId
            snackbarHostState.showSnackbar(deleteFailedMessage)
        }
    }

    if (showRemoveConfirmDialog) {
        ConfirmationDialog(
            title =
                stringResource(
                    if (isWebPattern) R.string.web_pattern_delete_confirm_title else R.string.remove_pattern,
                ),
            message =
                if (isWebPattern) {
                    stringResource(R.string.web_pattern_delete_confirm_message, pattern.name)
                } else {
                    stringResource(R.string.saved_pattern_detail_remove_confirm)
                },
            confirmText =
                stringResource(
                    if (isWebPattern) R.string.web_pattern_delete else R.string.remove_pattern,
                ),
            isDestructive = true,
            onConfirm = {
                showRemoveConfirmDialog = false
                onRemove()
            },
            onDismiss = { showRemoveConfirmDialog = false },
        )
    }
    pendingReplacementId?.let { expectedExistingId ->
        ConfirmationDialog(
            title = stringResource(R.string.web_pattern_replace_confirm_title),
            message = stringResource(R.string.web_pattern_replace_confirm_message, pattern.name),
            confirmText = stringResource(R.string.web_pattern_attach),
            onConfirm = {
                if (!webAttachInFlight) {
                    webAttachInFlight = true
                    onAttachWebPattern(expectedExistingId, handleWebAttachResult)
                }
            },
            onDismiss = { pendingReplacementId = null },
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
            if (webUrl != null && pattern.isWebPatternCompatible) {
                WebPatternDetailContent(
                    pattern = pattern,
                    url = webUrl,
                    onOpenWebsite = {
                        val result =
                            onOpenWebsite?.invoke(webUrl.originalUrl)
                                ?: openExternalWebLink(context, webUrl.originalUrl)
                        val message =
                            when (result) {
                                ExternalWebLinkOpenResult.NoBrowser -> noBrowserMessage
                                ExternalWebLinkOpenResult.InvalidUrl,
                                ExternalWebLinkOpenResult.Failed,
                                -> webOpenFailedMessage

                                ExternalWebLinkOpenResult.Opened -> null
                            }
                        message?.let { coroutineScope.launch { snackbarHostState.showSnackbar(it) } }
                    },
                    onEdit = onEditWebPattern,
                    onAttach = {
                        if (!webAttachInFlight) {
                            webAttachInFlight = true
                            onAttachWebPattern(null, handleWebAttachResult)
                        }
                    },
                    onDelete = { showRemoveConfirmDialog = true },
                )
            } else {
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
}

@Composable
private fun WebPatternDetailContent(
    pattern: SavedPattern,
    url: com.finnvek.knittools.domain.model.WebPatternUrl,
    onOpenWebsite: () -> Unit,
    onEdit: () -> Unit,
    onAttach: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = pattern.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        pattern.designerName.takeIf { it.isNotBlank() }?.let { designer ->
            Text(
                text = designer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.web_pattern_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = url.host, style = MaterialTheme.typography.titleMedium)
        SelectionContainer {
            Text(
                text = url.originalUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!url.isSecure) {
            Text(
                text = stringResource(R.string.web_pattern_http_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(stringResource(R.string.web_pattern_opens_original), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.web_pattern_not_offline), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.web_pattern_source_controlled), style = MaterialTheme.typography.bodyMedium)
    }

    val openDescription = stringResource(R.string.web_pattern_open_website_description, pattern.name, url.host)
    val editDescription = stringResource(R.string.web_pattern_edit_description, pattern.name)
    val attachDescription = stringResource(R.string.web_pattern_attach_description, pattern.name)
    val deleteDescription = stringResource(R.string.web_pattern_delete_description, pattern.name)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onOpenWebsite,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = openDescription },
        ) {
            Text(stringResource(R.string.web_pattern_open_website))
        }
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = editDescription },
        ) {
            Text(stringResource(R.string.web_pattern_edit))
        }
        OutlinedButton(
            onClick = onAttach,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = attachDescription },
        ) {
            Text(stringResource(R.string.web_pattern_attach))
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = deleteDescription },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.web_pattern_delete))
        }
    }
}

@Composable
private fun SavedPatternDetailHeader(pattern: SavedPattern) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RemotePatternImage(
            imageUrl = pattern.thumbnailUrl,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(12.dp)),
        )

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
        PatternAvailabilityBadge(availability = pattern.availability)
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
