package com.finnvek.knittools.ai.journal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryBottomSheet(
    onDismiss: () -> Unit,
    onEntryReady: (text: String, aiUsed: Boolean, fallbackReason: JournalProcessResult.Fallback.Reason?) -> Unit,
    viewModel: JournalEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.startListening()
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is JournalEvent.EntryReady) {
                onEntryReady(event.text, event.aiUsed, event.reason)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .heightIn(min = 280.dp),
        ) {
            JournalEntrySheetContent(
                state = state,
                onSelectSpeakMode = {
                    viewModel.selectSpeakMode()
                    startListeningIfPermitted(context, micPermissionLauncher, viewModel::startListening)
                },
                onSelectTypeMode = viewModel::selectTypeMode,
                onToggleMic = {
                    if (state.isListening) {
                        viewModel.stopListening()
                    } else {
                        startListeningIfPermitted(context, micPermissionLauncher, viewModel::startListening)
                    }
                },
                onBackToModeSelect = viewModel::backToModeSelect,
                onTypedTextChange = viewModel::onTypedTextChange,
                onSubmit = viewModel::submit,
            )
        }
    }
}

private fun startListeningIfPermitted(
    context: Context,
    micPermissionLauncher: ActivityResultLauncher<String>,
    onGranted: () -> Unit,
) {
    val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    if (granted) {
        onGranted()
    } else {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
private fun JournalEntrySheetContent(
    state: JournalEntryUiState,
    onSelectSpeakMode: () -> Unit,
    onSelectTypeMode: () -> Unit,
    onToggleMic: () -> Unit,
    onBackToModeSelect: () -> Unit,
    onTypedTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val errorMessage by rememberUpdatedState(state.errorMessage?.let { stringResource(it) })

    when (state.mode) {
        JournalMode.ModeSelect -> {
            ModeSelectContent(
                onSpeak = onSelectSpeakMode,
                onType = onSelectTypeMode,
                speechAvailable = state.speechAvailable,
            )
        }

        JournalMode.Speak -> {
            SpeakContent(
                isListening = state.isListening,
                partial = state.partialSpeech,
                errorMessage = errorMessage,
                onToggleMic = onToggleMic,
                onBack = onBackToModeSelect,
                onSubmit = onSubmit,
                canSubmit = state.partialSpeech.isNotBlank() && !state.isListening,
            )
        }

        JournalMode.Type -> {
            TypeContent(
                text = state.typedText,
                errorMessage = errorMessage,
                onTextChange = onTypedTextChange,
                onBack = onBackToModeSelect,
                onSubmit = onSubmit,
                canSubmit = state.typedText.isNotBlank(),
            )
        }

        JournalMode.Processing -> {
            ProcessingContent()
        }
    }
}

@Composable
private fun ModeSelectContent(
    onSpeak: () -> Unit,
    onType: () -> Unit,
    speechAvailable: Boolean,
) {
    Text(
        text = stringResource(R.string.journal_add_entry_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
    )
    Button(
        onClick = onSpeak,
        enabled = speechAvailable,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(text = stringResource(R.string.journal_mode_speak))
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onType,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(text = stringResource(R.string.journal_mode_type))
    }
    if (!speechAvailable) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.journal_speech_unavailable),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SpeakContent(
    isListening: Boolean,
    partial: String,
    errorMessage: String?,
    onToggleMic: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
) {
    HeaderRow(
        title = stringResource(R.string.journal_speak_title),
        onBack = onBack,
    )
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        MicPulse(isActive = isListening, onClick = onToggleMic)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text =
            if (isListening) {
                stringResource(
                    R.string.journal_listening,
                )
            } else {
                stringResource(R.string.journal_tap_to_speak)
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ).padding(12.dp),
    ) {
        val scroll = rememberScrollState()
        Text(
            text = partial.ifBlank { stringResource(R.string.journal_transcription_placeholder) },
            color =
                if (partial.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.verticalScroll(scroll),
        )
    }
    if (!errorMessage.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(Modifier.height(20.dp))
    ActionRow(onSubmit = onSubmit, canSubmit = canSubmit)
}

@Composable
private fun TypeContent(
    text: String,
    errorMessage: String?,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
) {
    HeaderRow(
        title = stringResource(R.string.journal_type_title),
        onBack = onBack,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = { Text(stringResource(R.string.journal_type_placeholder)) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        shape = RoundedCornerShape(12.dp),
        minLines = 4,
        isError = !errorMessage.isNullOrBlank(),
    )
    if (!errorMessage.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(Modifier.height(20.dp))
    ActionRow(onSubmit = onSubmit, canSubmit = canSubmit)
}

@Composable
private fun ProcessingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.journal_processing),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderRow(
    title: String,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionRow(
    onSubmit: () -> Unit,
    canSubmit: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = stringResource(R.string.journal_add_button))
        }
    }
}

@Composable
private fun MicPulse(
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.15f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "mic_pulse_scale",
    )
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .size(88.dp)
                .scale(scale)
                .background(color = tint.copy(alpha = 0.12f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.journal_mode_speak),
                tint = tint,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
