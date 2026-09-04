package com.finnvek.knittools.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorScreen(
    onBack: () -> Unit,
    onSeePro: () -> Unit,
    viewModelProvider: @Composable () -> NotesEditorViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var showProPrompt by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isMissingProject) {
        if (state.isMissingProject) {
            onBack()
        }
    }

    BackHandler {
        viewModel.cancelFirstNotesCreation()
        viewModel.saveImmediately(onBack)
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
                        viewModel.cancelFirstNotesCreation()
                        viewModel.saveImmediately(onBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (state.canEditNotes) {
                TextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChanged,
                    enabled = state.canEditNotes,
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
            } else if (state.isLoaded) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.write_your_notes_here),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { showProPrompt = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.add_note))
                            ProBadge(status = proState.status)
                        }
                    }
                }
            }
        }
    }

    if (showProPrompt) {
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.Notes,
                ),
            onDismiss = { showProPrompt = false },
            onTrialStarted = {
                showProPrompt = false
                viewModel.authorizeFirstNotesCreation()
            },
            onSeePro = onSeePro,
        )
    }

    // Auto-focus kun data on ladattu
    LaunchedEffect(state.isLoaded) {
        if (state.isLoaded && state.canEditNotes) {
            focusRequester.requestFocus()
        }
    }
}
