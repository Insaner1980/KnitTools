package com.finnvek.knittools.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.layout.LocalWindowFolds
import com.finnvek.knittools.ui.layout.rememberWindowFolds
import com.finnvek.knittools.ui.screens.library.IncomingPdfSheet
import com.finnvek.knittools.ui.screens.library.IncomingPdfSheetActions

/** Näyttää vastaanotetun PDF:n Tallenna ohje -sheetin ja vie tallennuksen jälkeen oikeaan näkymään. */
@Composable
fun IncomingPdfHost(
    viewModelProvider: @Composable () -> IncomingPdfViewModel,
    onNavigate: (IncomingPdfOutcome) -> Unit,
    onStartupReady: (Boolean) -> Unit = {},
) {
    val viewModel = viewModelProvider()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    val proAccessReady by viewModel.proAccessReady.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SideEffect { onStartupReady(pending != null && choices != null && !busy) }

    CollectWithLifecycleEffect({ viewModel.outcomes }) { outcome ->
        when (outcome) {
            IncomingPdfOutcome.OpenFailed ->
                Toast.makeText(context, R.string.incoming_pdf_open_failed, Toast.LENGTH_SHORT).show()

            IncomingPdfOutcome.SaveFailed ->
                Toast.makeText(context, R.string.incoming_pdf_save_failed, Toast.LENGTH_SHORT).show()

            IncomingPdfOutcome.Downloading ->
                Toast.makeText(context, R.string.incoming_pdf_downloading, Toast.LENGTH_SHORT).show()

            IncomingPdfOutcome.NotPdf ->
                Toast.makeText(context, R.string.incoming_pdf_not_pdf, Toast.LENGTH_SHORT).show()

            is IncomingPdfOutcome.OpenProject,
            is IncomingPdfOutcome.OpenSavedPattern,
            -> onNavigate(outcome)
        }
    }

    pending?.let { pdf ->
        CompositionLocalProvider(
            LocalAdaptiveModalWindow provides true,
            LocalWindowFolds provides rememberWindowFolds(),
        ) {
            IncomingPdfSheet(
                pdf = pdf,
                projects = choices?.projects.orEmpty(),
                canStartProject = choices?.canStartProject == true,
                proState = choices?.proState ?: ProState(),
                proAccessReady = proAccessReady && choices != null,
                busy = busy,
                actions =
                    IncomingPdfSheetActions(
                        onAttach = viewModel::attachToProject,
                        onStartProject = viewModel::startNewProject,
                        onSaveToLibrary = viewModel::saveToLibrary,
                        onDismiss = viewModel::discard,
                    ),
            )
        }
    }
}
