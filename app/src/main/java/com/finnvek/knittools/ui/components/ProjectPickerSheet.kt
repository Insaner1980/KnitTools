package com.finnvek.knittools.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject

/** Projektit valintariveinä: aktiiviset ensin, valmiit omana osionaan. */
@Composable
fun ProjectOptionRows(
    projects: List<CounterProject>,
    onSelect: (projectId: Long) -> Unit,
    enabled: Boolean = true,
) {
    val (completed, active) = projects.partition { it.isCompleted }
    active.forEach { project ->
        SheetOptionRow(title = project.name, onClick = { onSelect(project.id) }, enabled = enabled)
    }
    if (completed.isNotEmpty()) {
        SectionLabel(text = stringResource(R.string.section_completed))
        completed.forEach { project ->
            SheetOptionRow(title = project.name, onClick = { onSelect(project.id) }, enabled = enabled)
        }
    }
}

/** Kysyy, mihin projektiin jokin liitetään, eikä oleta laskurissa valittuna olevaa projektia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPickerSheet(
    title: String,
    projects: List<CounterProject>,
    onSelect: (projectId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetContentColumn {
            SheetTitle(text = title)
            if (projects.isEmpty()) {
                OverviewEmptyText(R.string.project_picker_empty)
            } else {
                ProjectOptionRows(projects = projects, onSelect = onSelect)
            }
            CancelButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End))
        }
    }
}
