package com.finnvek.knittools.ui.screens.counter

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.RollingCounter
// MaterialTheme.colorScheme.primaryContainer korvattu primaryContainer-tokenilla

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onSettings: () -> Unit = {},
    onProjectList: () -> Unit = {},
    onSessionHistory: (Long) -> Unit = {},
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showYarnPicker by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }
    val savedYarnCards by viewModel.savedYarnCards.collectAsStateWithLifecycle()

    KeepScreenAwake(state.keepScreenAwake)

    val vibrator = rememberVibrator()

    fun performHaptic() {
        if (state.hapticFeedback) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 60))
        }
    }

    if (showResetDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.reset_counter),
            message = stringResource(R.string.reset_counter_message),
            confirmText = stringResource(R.string.reset),
            onConfirm = {
                viewModel.reset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
        )
    }
    if (showYarnPicker) {
        YarnPickerSheet(
            savedYarnCards = savedYarnCards,
            onSelect = {
                viewModel.linkYarnCard(it)
                showYarnPicker = false
            },
            onDismiss = { showYarnPicker = false },
        )
    }
    if (showNotesSheet) {
        NotesSheet(
            notes = state.notes,
            onNotesChange = { viewModel.setNotes(it) },
            onDismiss = { showNotesSheet = false },
        )
    }
    if (showSummarySheet) {
        SummarySheet(
            isLoading = state.isSummaryLoading,
            summary = state.projectSummary,
            onDismiss = {
                showSummarySheet = false
                viewModel.clearSummary()
            },
        )
    }

    val isDark = isSystemInDarkTheme()
    // + napin värit speksin mukaan: light = gold fill + cream icon, dark = cream fill + dark icon
    val plusButtonBg = if (isDark) Color(0xFFF0E6D8) else MaterialTheme.colorScheme.primary
    val plusButtonIcon = if (isDark) Color(0xFF1A1410) else Color(0xFFF0E6D8)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_row_counter),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isEditingName) {
                TextField(
                    value = state.projectName,
                    onValueChange = { viewModel.setProjectName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.default_project_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    trailingIcon = {
                        IconButton(onClick = { isEditingName = false }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            } else {
                ProjectInfoDisplay(
                    state = state,
                    onStartEditing = { isEditingName = true },
                    onProjectList = onProjectList,
                    onSummary = {
                        viewModel.generateSummary()
                        showSummarySheet = true
                    },
                    onShowYarnPicker = { showYarnPicker = true },
                    onShowNotes = { showNotesSheet = true },
                    onUnlinkYarn = { viewModel.unlinkYarnCard(it) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // TOTAL ROWS label
            Text(
                text = stringResource(R.string.current_row),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Suuri laskurinumero
            RollingCounter(
                count = state.counter.count,
                textStyle =
                    MaterialTheme.typography.displayMedium.copy(
                        fontSize = 96.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
            )

            if (state.isPro) {
                Spacer(modifier = Modifier.height(8.dp))
                PatternRepeatPill(
                    count = state.secondaryCount,
                    onDecrement = {
                        performHaptic()
                        viewModel.decrementSecondary()
                    },
                    onIncrement = {
                        performHaptic()
                        viewModel.incrementSecondary()
                    },
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            CounterButtons(
                plusButtonBg = plusButtonBg,
                plusButtonIcon = plusButtonIcon,
                onDecrement = {
                    performHaptic()
                    viewModel.decrement()
                },
                onIncrement = {
                    performHaptic()
                    viewModel.increment()
                },
                onUndo = {
                    performHaptic()
                    viewModel.undo()
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatsRow(
                stitchCount = state.stitchCount,
                sessionSeconds = state.sessionSeconds,
                onTimeClick = { state.projectId?.let { onSessionHistory(it) } },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reset Counter — muted teksti
            Text(
                text = stringResource(R.string.reset_counter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .clickable { showResetDialog = true }
                        .padding(8.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun rememberVibrator(): Vibrator? {
    val context = LocalView.current.context
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
}

@Composable
private fun ProjectInfoDisplay(
    state: CounterUiState,
    onStartEditing: () -> Unit,
    onProjectList: () -> Unit,
    onSummary: () -> Unit,
    onShowYarnPicker: () -> Unit,
    onShowNotes: () -> Unit,
    onUnlinkYarn: (Long) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.current_project),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                if (state.isPro) {
                    IconButton(onClick = onSummary, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = onProjectList, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.project_list_title),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = state.projectName.ifEmpty { stringResource(R.string.default_project_name) },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClick = onStartEditing),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    state.linkedYarns.forEach { (cardId, yarnName) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\uD83E\uDDF6 $yarnName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                onClick = { onUnlinkYarn(cardId) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.add_yarn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onShowYarnPicker),
                    )
                }
                if (state.isPro) {
                    IconButton(onClick = onShowNotes, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Subject,
                            contentDescription = stringResource(R.string.notes),
                            modifier = Modifier.size(20.dp),
                            tint =
                                if (state.notes.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternRepeatPill(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onDecrement,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(Icons.Filled.Remove, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.pattern_repeat).uppercase() + ": $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onIncrement,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(Icons.Filled.Add, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CounterButtons(
    plusButtonBg: Color,
    plusButtonIcon: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onDecrement,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(plusButtonBg)
                    .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.counter_increase),
                modifier = Modifier.size(32.dp),
                tint = plusButtonIcon,
            )
        }
        OutlinedButton(
            onClick = onUndo,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(0.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.counter_undo),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsRow(
    stitchCount: Int?,
    sessionSeconds: Long,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stitch_count_format, stitchCount ?: 0),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onTimeClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Timer,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "%02d:%02d".format(sessionSeconds / 60, sessionSeconds % 60),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YarnPickerSheet(
    savedYarnCards: List<YarnCardEntity>,
    onSelect: (Long) -> Unit,
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
        ) {
            Text(
                text = stringResource(R.string.select_yarn_card).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (savedYarnCards.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_saved_yarns),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedYarnCards, key = { it.id }) { card ->
                        YarnPickerItem(card = card, onSelect = { onSelect(card.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun YarnPickerItem(
    card: YarnCardEntity,
    onSelect: () -> Unit,
) {
    val name =
        listOfNotNull(
            card.brand.takeIf { it.isNotBlank() },
            card.yarnName.takeIf { it.isNotBlank() },
        ).joinToString(" ").ifEmpty { "Yarn #${card.id}" }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onSelect)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            if (card.colorName.isNotBlank()) {
                Text(
                    text = card.colorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (card.weightCategory.isNotBlank()) {
            Text(
                text = card.weightCategory,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.notes).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.add_note)) },
                minLines = 6,
                shape = RoundedCornerShape(16.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummarySheet(
    isLoading: Boolean,
    summary: String?,
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
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.project_summary),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.generating_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = summary ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
            }
        }
    }
}

@Composable
private fun KeepScreenAwake(enabled: Boolean) {
    if (!enabled) return
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
