package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.PROJECT_DOCUMENT_LABEL_MAX_LENGTH
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProjectOptionRows
import com.finnvek.knittools.ui.components.ProjectYarnTextField
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.SheetContentColumn
import com.finnvek.knittools.ui.components.SheetOptionRow
import com.finnvek.knittools.ui.components.SheetTitle
import com.finnvek.knittools.ui.navigation.IncomingPdf

data class IncomingPdfSheetActions(
    val onAttach: (projectId: Long, name: String) -> Unit,
    val onStartProject: (name: String) -> Unit,
    val onSaveToLibrary: (name: String) -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Toisesta sovelluksesta tullut PDF-ohje: projektit näkyvät heti listana, joten ohjeen saa
 * liitettyä yhdellä napautuksella. Uusi projekti ja pelkkä Kirjasto ovat listan alla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingPdfSheet(
    pdf: IncomingPdf,
    projects: List<CounterProject>,
    canStartProject: Boolean,
    proStatus: ProStatus,
    busy: Boolean,
    actions: IncomingPdfSheetActions,
) {
    var name by rememberSaveable(pdf.localUri) { mutableStateOf(pdf.suggestedName) }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) actions.onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetContentColumn {
            SheetTitle(text = stringResource(R.string.incoming_pdf_title))
            ProjectYarnTextField(
                value = name,
                // Nimike tallentuu projektin dokumentiksi, jonka enimmäispituus on rajattu.
                onValueChange = { name = it.take(PROJECT_DOCUMENT_LABEL_MAX_LENGTH) },
                label = stringResource(R.string.web_pattern_title_label),
                singleLine = true,
            )
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (projects.isNotEmpty()) {
                SectionLabel(text = stringResource(R.string.web_pattern_attach))
                ProjectOptionRows(
                    projects = projects,
                    onSelect = { projectId -> actions.onAttach(projectId, name.trim()) },
                    enabled = !busy,
                )
                SectionLabel(text = stringResource(R.string.incoming_pdf_other_section))
            }
            SheetOptionRow(
                title = stringResource(R.string.incoming_pdf_new_project),
                onClick = { actions.onStartProject(name.trim()) },
                enabled = !busy && canStartProject,
                badge = if (canStartProject) null else ({ ProBadge(status = proStatus) }),
            )
            SheetOptionRow(
                title = stringResource(R.string.incoming_pdf_library_only),
                onClick = { actions.onSaveToLibrary(name.trim()) },
                enabled = !busy,
            )
            CancelButton(
                onClick = actions.onDismiss,
                enabled = !busy,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
