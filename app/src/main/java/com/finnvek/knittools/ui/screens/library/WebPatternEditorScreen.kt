package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.WEB_PATTERN_TEXT_MAX_LENGTH
import com.finnvek.knittools.domain.model.WebPatternDesignerValidation
import com.finnvek.knittools.domain.model.WebPatternTitleValidation
import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.navigation.PatternShareImportRequest
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin

internal const val WEB_PATTERN_TITLE_FIELD_TAG = "web_pattern_title_field"
internal const val WEB_PATTERN_URL_FIELD_TAG = "web_pattern_url_field"
internal const val WEB_PATTERN_DESIGNER_FIELD_TAG = "web_pattern_designer_field"

@Composable
fun WebPatternEditorScreen(
    request: PatternShareImportRequest?,
    onRequestStored: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenRavelry: (String) -> Unit,
    viewModelProvider: @Composable () -> WebPatternEditorViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(request?.requestId) {
        val currentRequest = request ?: return@LaunchedEffect
        if (viewModel.offerSharedRequest(currentRequest) == WebPatternShareAcceptResult.Stored) {
            onRequestStored(currentRequest.requestId)
        }
    }
    LaunchedEffect(state.completion?.eventId) {
        val completion = state.completion ?: return@LaunchedEffect
        viewModel.consumeCompletion(completion.eventId)
        when (completion) {
            is WebPatternEditorCompletion.OpenDetail -> onOpenDetail(completion.patternId)
            is WebPatternEditorCompletion.OpenProject -> onOpenProject(completion.projectId)
            is WebPatternEditorCompletion.OpenRavelry -> onOpenRavelry(completion.url)
        }
    }

    WebPatternEditorContent(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::updateTitle,
        onDesignerChange = viewModel::updateDesigner,
        onUrlChange = viewModel::updateUrl,
        onSave = viewModel::save,
        onKeepDraft = { viewModel.resolveIncomingShare(useIncoming = false) },
        onUseSharedLink = { viewModel.resolveIncomingShare(useIncoming = true) },
        onDismissReplacement = viewModel::dismissReplacement,
        onConfirmReplacement = viewModel::confirmReplacement,
    )
}

@Composable
@Suppress("kotlin:S107", "kotlin:S3776") // Editorisisältö pitää kenttäkohtaiset muutokset ja validoinnin näkyvinä.
internal fun WebPatternEditorContent(
    state: WebPatternEditorUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDesignerChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onKeepDraft: () -> Unit,
    onUseSharedLink: () -> Unit,
    onDismissReplacement: () -> Unit,
    onConfirmReplacement: () -> Unit,
) {
    val titleFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    val designerFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var validationAttempted by rememberSaveable { mutableStateOf(false) }
    var initialShareFocusHandled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.route?.origin, state.title, state.url) {
        if (
            !initialShareFocusHandled &&
            state.route?.origin == WebPatternEditorOrigin.Share &&
            state.title.isBlank() &&
            state.url.isNotBlank()
        ) {
            initialShareFocusHandled = true
            titleFocus.requestFocus()
        }
    }

    LaunchedEffect(validationAttempted, state.firstInvalidField) {
        if (!validationAttempted) return@LaunchedEffect
        when (state.firstInvalidField) {
            WebPatternEditorField.Title -> titleFocus.requestFocus()
            WebPatternEditorField.Url -> urlFocus.requestFocus()
            WebPatternEditorField.Designer -> designerFocus.requestFocus()
            null -> Unit
        }
    }

    ToolScreenScaffold(
        title = editorTitle(state.route?.origin),
        onBack = onBack,
    ) { contentPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@ToolScreenScaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.route?.origin == WebPatternEditorOrigin.Share) {
                Text(
                    text = stringResource(R.string.web_pattern_shared_link),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus).testTag(WEB_PATTERN_TITLE_FIELD_TAG),
                enabled = !state.isSaving && !state.didPersist,
                label = { Text(stringResource(R.string.web_pattern_title_label)) },
                minLines = 1,
                maxLines = 3,
                isError = validationAttempted && state.titleValidation !is WebPatternTitleValidation.Valid,
                supportingText = titleSupportingText(state, validationAttempted),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { urlFocus.requestFocus() }),
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth().focusRequester(urlFocus).testTag(WEB_PATTERN_URL_FIELD_TAG),
                enabled = !state.isSaving && !state.didPersist,
                label = { Text(stringResource(R.string.web_pattern_url_label)) },
                singleLine = true,
                isError = validationAttempted && state.urlValidation !is WebPatternUrlValidation.Valid,
                supportingText = urlSupportingText(state, validationAttempted),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { designerFocus.requestFocus() }),
            )
            OutlinedTextField(
                value = state.designer,
                onValueChange = onDesignerChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(designerFocus)
                        .testTag(WEB_PATTERN_DESIGNER_FIELD_TAG),
                enabled = !state.isSaving && !state.didPersist,
                label = { Text(stringResource(R.string.web_pattern_designer_label)) },
                singleLine = true,
                isError = validationAttempted && state.designerValidation !is WebPatternDesignerValidation.Valid,
                supportingText = designerSupportingText(state, validationAttempted),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            validationAttempted = true
                            if (state.canSave) onSave()
                        },
                    ),
            )

            state.sourceHost?.let { host ->
                Text(
                    text = stringResource(R.string.web_pattern_website_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.showsHttpWarning) {
                Text(
                    text = stringResource(R.string.web_pattern_http_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.web_pattern_source_controlled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.web_pattern_not_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let { error ->
                Text(
                    text = editorError(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        validationAttempted = true
                        if (state.canSave) onSave()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !state.isLoading && !state.isSaving && !state.didPersist && state.route != null,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    val replacement = state.pendingReplacement
    if (replacement != null) {
        AlertDialog(
            onDismissRequest = onDismissReplacement,
            title = { Text(stringResource(R.string.web_pattern_replace_confirm_title)) },
            text = { Text(stringResource(R.string.web_pattern_replace_confirm_message, state.title)) },
            confirmButton = {
                TextButton(onClick = onConfirmReplacement, enabled = !state.isSaving) {
                    Text(stringResource(R.string.web_pattern_attach))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissReplacement, enabled = !state.isSaving) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    } else if (state.pendingIncomingShare != null) {
        AlertDialog(
            onDismissRequest = onKeepDraft,
            title = { Text(stringResource(R.string.web_pattern_shared_link)) },
            text = { Text(stringResource(R.string.web_pattern_discard_current_draft)) },
            confirmButton = {
                TextButton(onClick = onUseSharedLink) {
                    Text(stringResource(R.string.web_pattern_use_shared_link))
                }
            },
            dismissButton = {
                TextButton(onClick = onKeepDraft) {
                    Text(stringResource(R.string.web_pattern_keep_current_draft))
                }
            },
        )
    }
}

@Composable
private fun editorTitle(origin: WebPatternEditorOrigin?): String =
    stringResource(
        when (origin) {
            WebPatternEditorOrigin.Edit -> R.string.web_pattern_edit
            WebPatternEditorOrigin.Share -> R.string.web_pattern_confirm_details
            WebPatternEditorOrigin.Manual,
            WebPatternEditorOrigin.Project,
            null,
            -> R.string.web_pattern_add
        },
    )

private fun titleSupportingText(
    state: WebPatternEditorUiState,
    validationAttempted: Boolean,
): (@Composable () -> Unit)? =
    if (!validationAttempted || state.titleValidation is WebPatternTitleValidation.Valid) {
        null
    } else {
        {
            Text(
                stringResource(
                    if (state.title.isBlank()) {
                        R.string.web_pattern_error_title_required
                    } else {
                        R.string.web_pattern_error_title_too_long
                    },
                ),
            )
        }
    }

private fun urlSupportingText(
    state: WebPatternEditorUiState,
    validationAttempted: Boolean,
): (@Composable () -> Unit)? =
    if (!validationAttempted || state.urlValidation is WebPatternUrlValidation.Valid) {
        null
    } else {
        {
            Text(
                stringResource(
                    when {
                        state.url.isBlank() -> R.string.web_pattern_error_url_required
                        !state.url.trim().startsWith("http://", ignoreCase = true) &&
                            !state.url.trim().startsWith("https://", ignoreCase = true) ->
                            R.string.web_pattern_error_url_web_only

                        else -> R.string.web_pattern_error_url_invalid
                    },
                ),
            )
        }
    }

private fun designerSupportingText(
    state: WebPatternEditorUiState,
    validationAttempted: Boolean,
): (@Composable () -> Unit)? =
    if (!validationAttempted || state.designerValidation is WebPatternDesignerValidation.Valid) {
        null
    } else {
        {
            Text(
                if (state.designer.length > WEB_PATTERN_TEXT_MAX_LENGTH) {
                    stringResource(R.string.web_pattern_error_title_too_long)
                } else {
                    stringResource(R.string.generic_error_unknown)
                },
            )
        }
    }

@Composable
private fun editorError(error: WebPatternEditorError): String =
    stringResource(
        when (error) {
            WebPatternEditorError.AlreadySaved -> R.string.web_pattern_already_saved
            WebPatternEditorError.SaveFailed -> R.string.web_pattern_save_failed
            WebPatternEditorError.UpdateFailed,
            WebPatternEditorError.StaleAction,
            -> R.string.web_pattern_update_failed

            WebPatternEditorError.ProjectUnavailable -> R.string.web_pattern_project_unavailable
            WebPatternEditorError.PatternUnavailable,
            WebPatternEditorError.NotEditable,
            -> R.string.generic_error_unknown

            WebPatternEditorError.SharedLinkInvalid -> R.string.web_pattern_error_url_invalid
            WebPatternEditorError.SharedLinkAmbiguous -> R.string.web_pattern_share_ambiguous
        },
    )
