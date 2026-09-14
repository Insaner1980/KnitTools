package com.finnvek.knittools.ui.screens.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupFormat
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onRestored: () -> Unit,
) {
    val viewModel: BackupViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.MIME), viewModel::export)
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::prepare)
    BackHandler(state.busy || state.preview != null) { viewModel.cancel() }
    BackHandler(state.phase == BackupPhase.RESTORED) { onRestored() }
    BackupContent(
        state = state,
        onBack = {
            if (state.phase == BackupPhase.RESTORED) {
                onRestored()
            } else if (!state.busy) {
                viewModel.cancel()
                onBack()
            }
        },
        onExport = {
            if (viewModel.select()) {
                export.launch(
                    "KnitTools-backup-${LocalDate.now()}.${BackupFormat.EXTENSION}",
                )
            }
        },
        onSelectRestore = { if (viewModel.select()) restore.launch(arrayOf("*/*")) },
        onConfirm = viewModel::restore,
        onCancel = viewModel::cancel,
        onRestored = onRestored,
    )
}

@Composable
internal fun BackupContent(
    state: BackupUiState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onSelectRestore: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRestored: () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    ToolScreenScaffold(title = stringResource(R.string.backup_title), onBack = onBack, wrapTitle = true) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.backup_description))
            Text(stringResource(R.string.backup_privacy), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.backup_exclusions), style = MaterialTheme.typography.bodySmall)
            val enabled =
                !state.busy &&
                    state.phase != BackupPhase.SELECTING &&
                    state.preview == null &&
                    state.phase != BackupPhase.RESTORED
            Button(onExport, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) {
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(onSelectRestore, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) {
                Text(stringResource(R.string.backup_restore))
            }
            if (state.busy) {
                BackupProgress(state.phase, onCancel)
            }
            state.error?.let { error ->
                Text(stringResource(errorMessage(error)), Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
            state.preview?.let { preview ->
                Text(
                    stringResource(
                        R.string.backup_preview_date,
                        DateFormat.getDateTimeInstance().format(Date(preview.createdAt)),
                    ),
                )
                Text(stringResource(R.string.backup_preview_version, preview.versionName))
                Text(stringResource(R.string.backup_preview_projects, preview.projects))
                Text(stringResource(R.string.backup_preview_patterns, preview.patterns))
                Text(stringResource(R.string.backup_preview_yarn, preview.yarnCards))
                Text(stringResource(R.string.backup_preview_files, preview.files))
                Button({ confirm = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.backup_restore))
                }
                TextButton(onCancel, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
            }
            if (state.phase == BackupPhase.EXPORTED || state.phase == BackupPhase.RESTORED) {
                Text(
                    stringResource(phaseMessage(state.phase)),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (state.phase == BackupPhase.RESTORED) {
                    Button(
                        onRestored,
                        Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.backup_continue)) }
                }
            }
        }
    }
    if (confirm && state.preview != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_message)) },
            confirmButton = {
                TextButton({
                    confirm = false
                    onConfirm()
                }) { Text(stringResource(R.string.backup_replace)) }
            },
            dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun BackupProgress(
    phase: BackupPhase,
    onCancel: () -> Unit,
) {
    LinearProgressIndicator(Modifier.fillMaxWidth())
    Text(
        stringResource(phaseMessage(phase)),
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    if (phase != BackupPhase.RESTORING) {
        TextButton(onCancel, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
    }
}

private fun phaseMessage(phase: BackupPhase): Int =
    when (phase) {
        BackupPhase.EXPORTING -> R.string.backup_exporting
        BackupPhase.VALIDATING -> R.string.backup_validating
        BackupPhase.RESTORING -> R.string.backup_restoring
        BackupPhase.RESTORED -> R.string.backup_restored
        else -> R.string.backup_exported
    }

private fun errorMessage(error: BackupError): Int =
    when (error) {
        BackupError.UNSUPPORTED -> R.string.backup_error_unsupported
        BackupError.INVALID -> R.string.backup_error_invalid
        BackupError.CORRUPT, BackupError.VALIDATION -> R.string.backup_error_corrupt
        BackupError.SPACE -> R.string.backup_error_space
        BackupError.READ -> R.string.backup_error_read
        BackupError.WRITE -> R.string.backup_error_write
        BackupError.RESTORE -> R.string.backup_error_restore
    }
