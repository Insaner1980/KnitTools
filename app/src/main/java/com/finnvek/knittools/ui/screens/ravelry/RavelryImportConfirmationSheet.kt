package com.finnvek.knittools.ui.screens.ravelry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.ui.components.StatusMessage
import com.finnvek.knittools.ui.components.StatusMessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RavelryImportConfirmationSheet(
    state: RavelryImportConfirmationState,
    onConfirmImport: () -> Unit,
    onSave: () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onOpenSavedPattern: (Long) -> Unit,
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.status) {
                RavelryImportStatus.AwaitingUserConfirmation ->
                    ImportMessageContent(
                        message = stringResource(R.string.ravelry_import_title),
                        actionText = stringResource(R.string.pattern_picker_import_from_ravelry),
                        onAction = onConfirmImport,
                    )

                RavelryImportStatus.Loading -> LoadingImportContent()
                RavelryImportStatus.Ready -> ReadyImportContent(state = state, onSave = onSave)
                RavelryImportStatus.AlreadySaved ->
                    AlreadySavedImportContent(
                        state = state,
                        onOpenSavedPattern = { onOpenSavedPattern(state.savedPatternId) },
                    )

                RavelryImportStatus.NeedsSignIn ->
                    ImportMessageContent(
                        message = stringResource(R.string.ravelry_import_needs_sign_in),
                        actionText = stringResource(R.string.ravelry_sign_in),
                        onAction = onSignIn,
                    )

                RavelryImportStatus.CouldNotImport ->
                    ImportMessageContent(
                        message = stringResource(R.string.ravelry_import_could_not_import),
                        actionText = stringResource(R.string.retry),
                        onAction = onRetry,
                        type = StatusMessageType.Error,
                    )

                RavelryImportStatus.BackendUnavailable ->
                    ImportMessageContent(
                        message = stringResource(R.string.ravelry_import_backend_unavailable),
                        actionText = stringResource(R.string.retry),
                        onAction = onRetry,
                        type = StatusMessageType.Error,
                    )
            }
        }
    }
}

@Composable
private fun LoadingImportContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.ravelry_import_loading),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReadyImportContent(
    state: RavelryImportConfirmationState,
    onSave: () -> Unit,
) {
    val pattern = state.pattern ?: return
    ImportPatternHeader(
        title = stringResource(R.string.ravelry_import_title),
        pattern = pattern,
    )
    Button(
        onClick = onSave,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isSaving) {
            CircularProgressIndicator()
        } else {
            Text(stringResource(R.string.save_pattern))
        }
    }
}

@Composable
private fun AlreadySavedImportContent(
    state: RavelryImportConfirmationState,
    onOpenSavedPattern: () -> Unit,
) {
    val pattern = state.pattern
    if (pattern != null) {
        ImportPatternHeader(
            title = stringResource(R.string.ravelry_import_already_saved),
            pattern = pattern,
        )
    } else {
        Text(
            text = stringResource(R.string.ravelry_import_already_saved),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Button(
        onClick = onOpenSavedPattern,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ravelry_open_saved_pattern))
    }
}

@Composable
private fun ImportPatternHeader(
    title: String,
    pattern: PatternDetail,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    PatternCard(
        state =
            PatternCardState(
                name = pattern.name,
                designerName = pattern.designer?.name ?: "",
                thumbnailUrl = pattern.mainPhotoUrl,
                difficulty = pattern.difficultyAverage,
                availability = pattern.availability,
            ),
        onClick = {},
    )
}

@Composable
private fun ColumnScope.ImportMessageContent(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    type: StatusMessageType = StatusMessageType.Info,
) {
    StatusMessage(
        message = message,
        type = type,
    )
    TextButton(
        onClick = onAction,
        modifier = Modifier.align(Alignment.End),
    ) {
        Text(actionText)
    }
}
