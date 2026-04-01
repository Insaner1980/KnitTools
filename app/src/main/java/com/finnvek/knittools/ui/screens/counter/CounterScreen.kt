package com.finnvek.knittools.ui.screens.counter

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.ToolScreenScaffold

@Composable
fun CounterScreen(
    onBack: () -> Unit,
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    if (state.keepScreenAwake) {
        DisposableEffect(Unit) {
            val window = (view.context as? android.app.Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val vibrator =
        remember {
            val context = view.context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
            }
        }

    fun performHaptic() {
        if (state.hapticFeedback) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 60))
        }
    }

    if (showResetDialog) {
        ConfirmationDialog(
            title = "Reset Counter",
            message = "Reset the counter to 0?",
            confirmText = "Reset",
            onConfirm = {
                viewModel.reset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
        )
    }

    ToolScreenScaffold(title = "Row Counter", onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = state.projectName,
                onValueChange = { viewModel.setProjectName(it) },
                label = { Text("Project name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer
            val minutes = state.sessionSeconds / 60
            val seconds = state.sessionSeconds % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Main count display
            Text(
                text = "${state.counter.count}",
                fontSize = 96.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "step: ${state.counter.stepSize}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Secondary counter (Pro)
            if (state.isPro) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pattern repeat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${state.secondaryCount}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        FilledTonalButton(onClick = {
                            performHaptic()
                            viewModel.decrementSecondary()
                        }) {
                            Text("-1")
                        }
                        FilledTonalButton(onClick = {
                            performHaptic()
                            viewModel.incrementSecondary()
                        }) {
                            Text("+1")
                        }
                        TextButton(onClick = { viewModel.resetSecondary() }) {
                            Text("Reset")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = {
                    performHaptic()
                    viewModel.decrement()
                }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                }
                FloatingActionButton(
                    onClick = {
                        performHaptic()
                        viewModel.increment()
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = {
                    performHaptic()
                    viewModel.undo()
                }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { showResetDialog = true }) {
                Text("Reset")
            }

            // Notes (Pro)
            if (state.isPro) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }
    }
}
