package com.finnvek.knittools.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.SessionItem
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun SessionHistoryScreen(onBack: () -> Unit) {
    val viewModel: SessionHistoryViewModel = hiltViewModel()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val projectMissing by viewModel.projectMissing.collectAsStateWithLifecycle()
    val projectName by viewModel.projectName.collectAsStateWithLifecycle()
    var pendingDeleteSessionId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(projectMissing) {
        if (projectMissing) {
            onBack()
        }
    }

    pendingDeleteSessionId?.let { sessionId ->
        ConfirmationDialog(
            title = stringResource(R.string.delete_session_title),
            message = stringResource(R.string.delete_session_message),
            confirmText = stringResource(R.string.delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSession(sessionId)
                pendingDeleteSessionId = null
            },
            onDismiss = { pendingDeleteSessionId = null },
        )
    }

    ToolScreenScaffold(
        title = stringResource(R.string.session_history_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (sessions.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_sessions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Insightsista tullessa otsikko on pelkkä "History", joten projekti
                    // pitää lukea ruudulta. Sama osiomerkki kuin Libraryssa.
                    projectName?.let { name ->
                        item {
                            Text(
                                text = name.localizedUppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.knitToolsColors.brandWine,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                    items(sessions, key = { it.id }) { session ->
                        SessionItem(
                            startedAt = session.startedAt,
                            durationMinutes = session.durationMinutes,
                            startRow = session.startRow,
                            endRow = session.endRow,
                            onDelete = { pendingDeleteSessionId = session.id },
                        )
                    }
                }
            }
        }
    }
}
