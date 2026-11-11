package com.finnvek.knittools.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ProjectNameLabel
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.contentWidth

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
    val proAccessReady by viewModel.proAccessReady.collectAsStateWithLifecycle()
    val notesAccess =
        proActionAccess(
            proState,
            proAccessReady && state.isLoaded,
            ProFeature.NOTES,
            alreadyUnlocked = state.notesCreated,
        )
    val focusRequester = remember { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var selectionStart by rememberSaveable { mutableIntStateOf(0) }
    var selectionEnd by rememberSaveable { mutableIntStateOf(0) }
    var editorValue by remember {
        mutableStateOf(TextFieldValue(state.notes, TextRange(selectionStart, selectionEnd)))
    }
    if (state.isLoaded && editorValue.text != state.notes) {
        editorValue = editorValue.copy(text = state.notes, selection = TextRange(selectionStart, selectionEnd))
        selectionStart = editorValue.selection.start
        selectionEnd = editorValue.selection.end
    }
    var showProPrompt by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (state.saveFailed) {
            showDiscardConfirmation = true
        } else {
            viewModel.cancelFirstNotesCreation()
            viewModel.saveImmediately(onBack)
        }
    }

    LaunchedEffect(state.isMissingProject) {
        if (state.isMissingProject) {
            onBack()
        }
    }

    BackHandler(onBack = requestBack)

    Scaffold(
        modifier = Modifier.fillMaxSize().wrapContentWidth().contentWidth(ContentWidthPolicy.Form),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    // Projektin nimi on osiomerkkinä sisällössä kuten historioissa; ajatusviivalla
                    // yhdistetty otsikko katkesi pitkillä nimillä.
                    Text(
                        text = stringResource(R.string.notes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
        ) {
            if (state.canEditNotes) {
                Column {
                    ProjectNameLabel(
                        name = state.projectName,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (state.saveFailed) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                text = stringResource(R.string.notes_save_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.saveImmediately() }) {
                                    Text(stringResource(R.string.retry))
                                }
                                TextButton(onClick = { showDiscardConfirmation = true }) {
                                    Text(stringResource(R.string.notes_discard_action))
                                }
                            }
                        }
                    }
                    TextField(
                        value = editorValue,
                        onValueChange = {
                            // Focus teardown must not replace the selection saved for recreation.
                            if (it.text != editorValue.text ||
                                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                            ) {
                                editorValue = it
                                selectionStart = it.selection.start
                                selectionEnd = it.selection.end
                            }
                            if (it.text != state.notes) viewModel.onNotesChanged(it.text)
                        },
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
                }
            } else if (state.isLoaded) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.write_your_notes_here),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            when (notesAccess) {
                                ProActionAccess.Allowed, ProActionAccess.Trial ->
                                    viewModel
                                        .authorizeFirstNotesCreation()
                                ProActionAccess.Locked -> showProPrompt = true
                                ProActionAccess.Unknown -> Unit
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.add_note))
                            ProMarker(notesAccess)
                        }
                    }
                }
            }
        }
    }

    if (showDiscardConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.notes_discard_title),
            message = stringResource(R.string.notes_discard_message),
            confirmText = stringResource(R.string.notes_discard_action),
            isDestructive = true,
            onConfirm = {
                showDiscardConfirmation = false
                viewModel.discardChanges()
                onBack()
            },
            onDismiss = { showDiscardConfirmation = false },
        )
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
    LaunchedEffect(state.isLoaded, state.canEditNotes) {
        if (state.isLoaded && state.canEditNotes) {
            focusRequester.requestFocus()
        }
    }
}
