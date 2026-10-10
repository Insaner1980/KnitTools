package com.finnvek.knittools.ui.components

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
import com.finnvek.knittools.R

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
    scrollableMessage: Boolean = false,
    confirmEnabled: Boolean = true,
    confirmModifier: Modifier = Modifier,
    dismissModifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Text(
                text = message,
                modifier = if (scrollableMessage) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            )
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
            CancelButton(onClick = onDismiss, modifier = dismissModifier)
        },
    )
}
