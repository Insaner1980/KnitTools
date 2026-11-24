package com.finnvek.knittools.ui.screens.notes

import android.icu.text.BreakIterator
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
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
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
import kotlinx.coroutines.flow.combine

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
    val editorState =
        remember {
            TextFieldState(
                initialText = state.notes,
                initialSelection =
                    TextRange(
                        selectionStart.coerceAtMost(state.notes.length),
                        selectionEnd.coerceAtMost(state.notes.length),
                    ),
            )
        }
    var synchronizedNotes by remember { mutableStateOf(state.notes) }
    val publishEditorEdit: () -> Boolean = {
        val notes = editorState.text.toString()
        val changed = notes != synchronizedNotes && viewModel.uiState.value.canEditNotes
        if (changed) {
            synchronizedNotes = notes
            viewModel.onNotesChanged(notes)
        }
        changed
    }
    LaunchedEffect(viewModel, editorState, lifecycle) {
        val fieldChanges = snapshotFlow { editorState.text to editorState.selection }
        combine(viewModel.uiState, fieldChanges) { _, _ -> Unit }.collect {
            val latest = viewModel.uiState.value
            if (!latest.isLoaded) return@collect
            val edited = publishEditorEdit()
            if (!edited && latest.notes != editorState.text.toString()) {
                synchronizedNotes = latest.notes
                editorState.edit {
                    replace(0, length, latest.notes)
                    selection = TextRange(selectionStart.coerceAtMost(length), selectionEnd.coerceAtMost(length))
                }
            }
            // Preserve the last active selection if focus teardown moves the cursor.
            if (edited || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                selectionStart = editorState.selection.start
                selectionEnd = editorState.selection.end
            }
        }
    }
    var showProPrompt by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (state.saveFailed) {
            showDiscardConfirmation = true
        } else {
            publishEditorEdit()
            viewModel.cancelFirstNotesCreation()
            viewModel.saveImmediately(exitAfterSave = true)
        }
    }

    NotesEditorExitEffect(viewModel, lifecycle, state.isMissingProject, state.exitRequested, onBack)

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
                        state = editorState,
                        enabled = state.canEditNotes,
                        outputTransformation = NotesOutputTransformation,
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
                                ProActionAccess.Allowed, ProActionAccess.Trial -> {
                                    viewModel
                                        .authorizeFirstNotesCreation()
                                }

                                ProActionAccess.Locked -> {
                                    showProPrompt = true
                                }

                                ProActionAccess.Unknown -> {
                                    Unit
                                }
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

private const val MAX_LAYOUT_PARAGRAPH_LENGTH = 2048

private val NotesOutputTransformation =
    OutputTransformation {
        if (length <= MAX_LAYOUT_PARAGRAPH_LENGTH) return@OutputTransformation
        val text = asCharSequence().toString()
        val characters = BreakIterator.getCharacterInstance().apply { setText(text) }
        var lineStart = 0
        while (lineStart < text.length) {
            val lineEnd = text.indexOf('\n', lineStart).takeIf { it >= 0 } ?: text.length
            var paragraphStart = lineStart
            while (lineEnd - paragraphStart > MAX_LAYOUT_PARAGRAPH_LENGTH) {
                val limit = paragraphStart + MAX_LAYOUT_PARAGRAPH_LENGTH
                val space =
                    (limit - 1 downTo paragraphStart + MAX_LAYOUT_PARAGRAPH_LENGTH / 2)
                        .firstOrNull { text[it] == ' ' && characters.isBoundary(it + 1) }
                val end =
                    space?.plus(1)
                        ?: characters.preceding(limit + 1).takeIf { it > paragraphStart }
                        ?: characters.following(paragraphStart)
                // Bound Android's native layout work for long unbroken text while preserving edit offsets.
                addStyle(ParagraphStyle(), paragraphStart, end)
                paragraphStart = end
            }
            lineStart = lineEnd + 1
        }
    }

@Composable
private fun NotesEditorExitEffect(
    viewModel: NotesEditorViewModel,
    lifecycle: Lifecycle,
    isMissingProject: Boolean,
    exitRequested: Boolean,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, lifecycle, isMissingProject, exitRequested) {
        if (isMissingProject || exitRequested) {
            lifecycle.withResumed {
                if (viewModel.consumeExitRequest() || viewModel.uiState.value.isMissingProject) {
                    currentOnBack()
                }
            }
        }
    }
}
