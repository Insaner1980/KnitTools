package com.finnvek.knittools.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.journal.JournalEntryBottomSheet
import com.finnvek.knittools.ai.journal.JournalProcessResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    viewModel: NotesEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showJournalSheet = remember { mutableStateOf(false) }

    val offlineMessage = stringResource(R.string.journal_offline_notice)
    val quotaMessage = stringResource(R.string.ai_quota_exhausted)

    BackHandler {
        viewModel.saveImmediately()
        onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notes_editor_title, state.projectName),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveImmediately()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (!state.isPro) {
                                viewModel.saveImmediately()
                                onUpgradeToPro()
                            } else {
                                showJournalSheet.value = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.journal_ai_badge),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = stringResource(R.string.write_your_notes_here),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        }
    }

    if (showJournalSheet.value) {
        JournalEntryBottomSheet(
            onDismiss = { showJournalSheet.value = false },
            onEntryReady = { text, aiUsed, reason ->
                viewModel.appendJournalEntry(text)
                showJournalSheet.value = false
                if (!aiUsed) {
                    val message =
                        when (reason) {
                            JournalProcessResult.Fallback.Reason.QuotaExhausted -> quotaMessage
                            else -> offlineMessage
                        }
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
        )
    }

    // Auto-focus kun data on ladattu
    LaunchedEffect(state.isLoaded) {
        if (state.isLoaded) {
            focusRequester.requestFocus()
        }
    }
}
