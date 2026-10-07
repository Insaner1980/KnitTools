package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.OverviewEmptyText
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewSectionHeader
import com.finnvek.knittools.ui.components.RemotePatternImage
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.rememberScrollTitleState
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.screens.ravelry.PatternAvailabilityBadge
import com.finnvek.knittools.ui.screens.ravelry.openRavelryUrl
import com.finnvek.knittools.ui.screens.ravelry.ravelryExternalUrlOrNull
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
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
    val attachmentViewModel: SavedPatternAttachmentViewModel = viewModel(key = "web-attachment-${pattern.id}")
    val attachmentState by attachmentViewModel.state.collectAsStateWithLifecycle()
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

    LaunchedEffect(attachmentState.result) {
        attachmentState.result?.let { result ->
            attachmentViewModel.consumeResult()
            handleWebAttachResult(result)
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
            scrollableMessage = true,
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
                attachmentViewModel.attach(expectedExistingId, onAttachWebPattern)
            },
            onDismiss = { pendingReplacementId = null },
        )
    }

    // Nimi on sisällön otsikkona; yläpalkki näyttää sen vasta kun nimi on vieritetty pois.
    val scrollTitle = rememberScrollTitleState()
    val openWebsite: () -> Unit = {
        webUrl?.let { url ->
            val result = onOpenWebsite?.invoke(url.originalUrl) ?: openExternalWebLink(context, url.originalUrl)
            val message =
                when (result) {
                    ExternalWebLinkOpenResult.NoBrowser -> noBrowserMessage

                    ExternalWebLinkOpenResult.InvalidUrl,
                    ExternalWebLinkOpenResult.Failed,
                    -> webOpenFailedMessage

                    ExternalWebLinkOpenResult.Opened -> null
                }
            message?.let { coroutineScope.launch { snackbarHostState.showSnackbar(it) } }
        }
    }
    val openRavelry: () -> Unit = {
        ravelryUrl?.let { url -> openRavelryUrl(context = context, url = url, failureMessage = openFailedMessage) }
    }
    ToolScreenScaffold(
        title = pattern.name,
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        showTitle = scrollTitle.showTitle,
        actions = {
            SavedPatternDetailMenu(
                onEdit = onEditWebPattern.takeIf { isWebPattern },
                deleteLabel =
                    stringResource(if (isWebPattern) R.string.web_pattern_delete else R.string.remove_pattern),
                onDelete = { showRemoveConfirmDialog = true },
            )
        },
    ) {
        // Sama rakenne kuin projektinäkymässä ja langan sivulla: nimi, yksi päätoiminto ja
        // hiusviivaosiot. Muokkaus ja poisto ovat ylivuotovalikossa eivätkä painikepinossa.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollTitle.scrollState)
                    .padding(
                        start = ProjectOverviewDimens.ScreenHorizontalPadding,
                        top = ProjectOverviewDimens.TopContentGap,
                        end = ProjectOverviewDimens.ScreenHorizontalPadding,
                        bottom = ProjectOverviewDimens.ContentBottomPadding,
                    ),
        ) {
            SavedPatternDetailHeader(
                pattern = pattern,
                showImage = !isWebPattern,
                modifier = scrollTitle.headerModifier,
            )
            if (webUrl != null && isWebPattern) {
                SavedPatternPrimaryAction(
                    label = stringResource(R.string.web_pattern_open_website),
                    onClick = openWebsite,
                    description =
                        stringResource(R.string.web_pattern_open_website_description, pattern.name, webUrl.host),
                )
                WebPatternSourceSection(url = webUrl)
                SavedPatternProjectSection(
                    label = stringResource(R.string.web_pattern_attach),
                    description = stringResource(R.string.web_pattern_attach_description, pattern.name),
                    onAttach = { attachmentViewModel.attach(null, onAttachWebPattern) },
                )
            } else {
                when {
                    pattern.hasAttachedPdf -> {
                        SavedPatternPrimaryAction(
                            label = stringResource(R.string.saved_pattern_detail_open_pattern),
                            onClick = onOpenPattern,
                        )
                    }

                    ravelryUrl != null -> {
                        SavedPatternPrimaryAction(stringResource(R.string.open_in_ravelry), openRavelry)
                    }
                }
                RavelrySourceSection(
                    pattern = pattern,
                    canOpenRavelry = ravelryUrl != null,
                    // Ravelry-linkki rivinä vain kun päätoiminto on PDF; muuten se on jo painike.
                    onOpenRavelry = openRavelry.takeIf { pattern.hasAttachedPdf && ravelryUrl != null },
                )
                SavedPatternProjectSection(
                    label = stringResource(R.string.saved_pattern_detail_attach_to_project),
                    description = null,
                    onAttach = onAttachToProject,
                )
            }
        }
    }
}

@Composable
private fun SavedPatternDetailMenu(
    onEdit: (() -> Unit)?,
    deleteLabel: String,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        onEdit?.let { edit ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.web_pattern_edit)) },
                onClick = {
                    expanded = false
                    edit()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(deleteLabel) },
            onClick = {
                expanded = false
                onDelete()
            },
        )
    }
}

/** Nimi ja suunnittelija (Ravelry-kaavalla myös kuva); yläpalkin otsikko näkyy kun lohko on vieritetty pois. */
@Composable
private fun SavedPatternDetailHeader(
    pattern: SavedPattern,
    showImage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
        if (showImage) {
            RemotePatternImage(
                imageUrl = pattern.thumbnailUrl,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(ProjectOverviewDimens.HeroPhotoHeight)
                        .clip(RoundedCornerShape(ProjectOverviewDimens.HeroPhotoCornerRadius)),
            )
        }
        Text(
            text = pattern.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        pattern.designerName.takeIf { it.isNotBlank() }?.let { designer ->
            Text(
                text = designer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Kaavan ainoa täytetty painike: sivulla on yksi päätoiminto, kuten projektinäkymän jatka-kortti. */
@Composable
private fun SavedPatternPrimaryAction(
    label: String,
    onClick: () -> Unit,
    description: String? = null,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = ProjectOverviewDimens.HeaderTopGap)
                .heightIn(min = ProjectOverviewDimens.ActionTouchSize)
                .semantics { description?.let { contentDescription = it } },
    ) {
        Text(label)
    }
}

@Composable
private fun WebPatternSourceSection(url: com.finnvek.knittools.domain.model.WebPatternUrl) {
    OverviewSectionHeader(R.string.web_pattern_website_label)
    Column(verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
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
        listOf(
            R.string.web_pattern_opens_original,
            R.string.web_pattern_not_offline,
            R.string.web_pattern_source_controlled,
        ).forEach { OverviewEmptyText(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RavelrySourceSection(
    pattern: SavedPattern,
    canOpenRavelry: Boolean,
    onOpenRavelry: (() -> Unit)?,
) {
    OverviewSectionHeader(R.string.tool_ravelry)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
        verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
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
    if (pattern.requiresRavelryAccess) {
        OverviewEmptyText(R.string.saved_pattern_detail_no_pdf_explanation)
    }
    onOpenRavelry?.let { OverviewLinkRow(stringResource(R.string.open_in_ravelry), null, null, onClick = it) }
}

@Composable
private fun SavedPatternAvailabilityChip(text: String) {
    Surface(
        color = MaterialTheme.knitToolsColors.cardContainer,
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
private fun SavedPatternProjectSection(
    label: String,
    description: String?,
    onAttach: () -> Unit,
) {
    OverviewSectionHeader(R.string.project_content_title)
    OverviewLinkRow(
        title = label,
        subtitle = null,
        icon = null,
        onClick = onAttach,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                description?.let { contentDescription = it }
            },
    )
}

private val SavedPattern.hasAttachedPdf: Boolean
    get() = !localPdfUri.isNullOrBlank()

private val SavedPattern.requiresRavelryAccess: Boolean
    get() = source == SavedPatternSource.Ravelry && !hasAttachedPdf

private fun SavedPattern.ravelryUrlOrNull(): String? =
    canonicalUrl
        .ifBlank { originalUrl }
        .let(::ravelryExternalUrlOrNull)
