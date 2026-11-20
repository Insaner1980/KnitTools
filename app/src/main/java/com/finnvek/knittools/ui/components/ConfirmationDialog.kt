package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.layout.LocalWindowFolds
import com.finnvek.knittools.ui.layout.dialogWidth

/**
 * Yhtenäinen vahvistusdialogi destruktiivisille ja muille toiminnoille.
 * Destruktiivisissa (isDestructive=true) vahvista-nappi saa error-värin.
 * [confirmEnabled] pitää vahvistuksen poissa käytöstä kesken tallennuksen, ja painikkeiden
 * modifierit tuovat kosketuskoon ja testitunnisteet ilman omaa AlertDialogia.
 */
@Composable
@Suppress("kotlin:S107") // Valinnaiset parametrit korvaavat kutsukohtaiset AlertDialogit.
fun ConfirmationDialog(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.confirm),
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    scrollableMessage: Boolean = true,
    confirmEnabled: Boolean = true,
    confirmModifier: Modifier = Modifier,
    dismissModifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    errorMessage: String? = null,
    dismissText: String = stringResource(R.string.cancel),
) {
    val usePaneCoordinates =
        LocalAdaptiveModalWindow.current && LocalWindowFolds.current.any { it.isSeparating || it.isOccluding }
    AlertDialog(
        onDismissRequest = { if (dismissEnabled) onDismiss() },
        modifier = modifier.dialogWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = !usePaneCoordinates),
        title = {
            AdaptiveModalWindow()
            Text(title)
        },
        text = {
            Column(
                modifier = if (scrollableMessage) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(message)
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = confirmModifier,
                colors =
                    if (isDestructive) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            CancelButton(text = dismissText, onClick = onDismiss, modifier = dismissModifier, enabled = dismissEnabled)
        },
    )
}
