package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.WEB_PATTERN_TEXT_MAX_LENGTH
import com.finnvek.knittools.domain.model.WebPatternDesignerValidation
import com.finnvek.knittools.domain.model.WebPatternTitleValidation
import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.FormSheet
import com.finnvek.knittools.ui.components.FormSheetConfirm
import com.finnvek.knittools.ui.components.LabelWithInfo
import com.finnvek.knittools.ui.components.cardTextFieldColors
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

    fun attemptSave() {
        validationAttempted = true
        when (state.firstInvalidField) {
            WebPatternEditorField.Title -> titleFocus.requestFocus()
            WebPatternEditorField.Url -> urlFocus.requestFocus()
            WebPatternEditorField.Designer -> designerFocus.requestFocus()
            null -> if (state.canSave) onSave()
        }
    }

    // Sama lisäyssheet kuin uudella projektilla ja langalla; reitti on dialogikohde, joten
    // edellinen näkymä näkyy taustalla kuten muissakin sheeteissä.
    FormSheet(
        title = editorTitle(state.route?.origin),
        onDismiss = onBack,
        cancelEnabled = !state.isSaving,
        confirm =
            FormSheetConfirm(
                text = stringResource(R.string.save),
                onClick = { attemptSave() },
                enabled = !state.isLoading && !state.didPersist && state.route != null,
                busy = state.isSaving,
            ),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            WebPatternEditorFields(
                state = state,
                validationAttempted = validationAttempted,
                focus = WebPatternEditorFocus(titleFocus, urlFocus, designerFocus),
                callbacks =
                    WebPatternEditorFieldCallbacks(
                        onTitleChange = onTitleChange,
                        onUrlChange = onUrlChange,
                        onDesignerChange = onDesignerChange,
                        onDone = {
                            focusManager.clearFocus()
                            attemptSave()
                        },
                    ),
            )
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
                CancelButton(onClick = onDismissReplacement, enabled = !state.isSaving)
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

private class WebPatternEditorFocus(
    val title: FocusRequester,
    val url: FocusRequester,
    val designer: FocusRequester,
)

private class WebPatternEditorFieldCallbacks(
    val onTitleChange: (String) -> Unit,
    val onUrlChange: (String) -> Unit,
    val onDesignerChange: (String) -> Unit,
    val onDone: () -> Unit,
)

@Composable
private fun WebPatternEditorFields(
    state: WebPatternEditorUiState,
    validationAttempted: Boolean,
    focus: WebPatternEditorFocus,
    callbacks: WebPatternEditorFieldCallbacks,
) {
    val editable = !state.isSaving && !state.didPersist
    if (state.route?.origin == WebPatternEditorOrigin.Share) {
        Text(
            text = stringResource(R.string.web_pattern_shared_link),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    TextField(
        value = state.title,
        onValueChange = callbacks.onTitleChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focus.title).testTag(WEB_PATTERN_TITLE_FIELD_TAG),
        enabled = editable,
        label = { Text(stringResource(R.string.web_pattern_title_label)) },
        minLines = 1,
        maxLines = 3,
        isError = validationAttempted && state.titleValidation !is WebPatternTitleValidation.Valid,
        supportingText = titleSupportingText(state, validationAttempted),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.url.requestFocus() }),
        shape = MaterialTheme.shapes.large,
        colors = cardTextFieldColors(),
    )
    TextField(
        value = state.url,
        onValueChange = callbacks.onUrlChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focus.url).testTag(WEB_PATTERN_URL_FIELD_TAG),
        enabled = editable,
        label = { Text(stringResource(R.string.web_pattern_url_label)) },
        singleLine = true,
        isError = validationAttempted && state.urlValidation !is WebPatternUrlValidation.Valid,
        supportingText = urlSupportingText(state, validationAttempted),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.designer.requestFocus() }),
        shape = MaterialTheme.shapes.large,
        colors = cardTextFieldColors(),
    )
    TextField(
        value = state.designer,
        onValueChange = callbacks.onDesignerChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focus.designer).testTag(WEB_PATTERN_DESIGNER_FIELD_TAG),
        enabled = editable,
        label = { Text(stringResource(R.string.web_pattern_designer_label)) },
        singleLine = true,
        isError = validationAttempted && state.designerValidation !is WebPatternDesignerValidation.Valid,
        supportingText = designerSupportingText(state, validationAttempted),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { callbacks.onDone() }),
        shape = MaterialTheme.shapes.large,
        colors = cardTextFieldColors(),
    )
    state.sourceHost?.let { host ->
        LabelWithInfo(label = stringResource(R.string.web_pattern_website_label), info = null)
        Text(text = host, style = MaterialTheme.typography.bodyMedium)
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
}

@Composable
private fun editorTitle(origin: WebPatternEditorOrigin?): String =
    stringResource(
        when (origin) {
            WebPatternEditorOrigin.Edit -> R.string.web_pattern_edit_title

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
                    when (state.urlValidation) {
                        WebPatternUrlValidation.Required -> R.string.web_pattern_error_url_required
                        WebPatternUrlValidation.WebOnly -> R.string.web_pattern_error_url_web_only
                        WebPatternUrlValidation.Invalid -> R.string.web_pattern_error_url_invalid
                        is WebPatternUrlValidation.Valid -> error("Valid URL does not have supporting error text")
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
                    stringResource(R.string.web_pattern_error_designer_too_long)
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
