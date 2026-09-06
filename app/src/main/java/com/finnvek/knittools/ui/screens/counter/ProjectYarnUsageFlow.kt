package com.finnvek.knittools.ui.screens.counter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.pro.ProStatus
import kotlinx.coroutines.launch

data class YarnUsageOpenRequest(
    val item: ProjectYarnUsageItem,
    val name: String,
    val delete: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectYarnUsageFlow(
    projectId: Long,
    linkedYarns: List<Pair<Long, String>>,
    projectYarnNotes: List<ProjectYarnNote>,
    proStatus: ProStatus,
    actions: YarnManagementSheetActions,
    viewModelProvider: @Composable () -> ProjectYarnUsageViewModel = { hiltViewModel() },
) {
    val model = viewModelProvider()
    val items by model.items.collectAsStateWithLifecycle()
    val editor by model.editor.collectAsStateWithLifecycle()
    val managementSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val usageSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var transitioning by remember { mutableStateOf(false) }
    var returnKey by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(projectId) { mutableStateOf(false) }
    LaunchedEffect(projectId) { model.observe(projectId) }
    DisposableEffect(model) { onDispose { model.stopObserving() } }
    val returnToYarn: () -> Unit = {
        if (!transitioning && !editor.busy) {
            transitioning = true
            scope.launch {
                try {
                    usageSheet.hide()
                    model.closeDraft()
                } finally {
                    transitioning = false
                }
            }
        }
    }
    LaunchedEffect(editor.completed) {
        if (editor.completed) {
            try {
                usageSheet.hide()
            } finally {
                model.closeDraft()
            }
        }
    }
    if (editor.draft?.projectId != projectId) {
        YarnManagementSheet(
            linkedYarns = linkedYarns,
            projectYarnNotes = projectYarnNotes,
            proStatus = proStatus,
            actions = actions,
            sheetState = managementSheet,
            usageItems = items.orEmpty(),
            focusKey = returnKey,
            onUsage = { request ->
                if (!transitioning) {
                    transitioning = true
                    scope.launch {
                        try {
                            managementSheet.hide()
                            returnKey = request.item.key
                            confirmDelete = request.delete
                            model.open(request.item, request.name)
                            if (model.editor.value.draft == null) {
                                managementSheet.show()
                            }
                        } finally {
                            transitioning = false
                        }
                    }
                }
            },
        )
    } else {
        ProjectYarnUsageSheet(
            state = editor,
            sheetState = usageSheet,
            initiallyConfirmDelete = confirmDelete,
            actions =
                YarnUsageEditorActions(
                    onEdit = model::edit,
                    onUnit = model::unit,
                    onConversion = model::conversion,
                    onSave = model::save,
                    onDelete = model::delete,
                    onDismiss = returnToYarn,
                ),
        )
    }
}
