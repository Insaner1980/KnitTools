package com.finnvek.knittools.ui.screens.ravelry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.auth.RavelryAuthState
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.MoreOptionsIconButton
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun RavelryAccountHeader(
    authState: RavelryAuthState,
    onSignIn: () -> Unit,
    onBrowseRavelry: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                // Ei omaa sivupehmustetta: lista antaa sen jo, ja kortti jäi hakukenttää kapeammaksi.
                .padding(vertical = headerVerticalPadding),
        color = MaterialTheme.knitToolsColors.cardContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(headerContentPadding),
            verticalArrangement = Arrangement.spacedBy(headerActionSpacing),
        ) {
            // Katkaisuvalikko on tilarivin päässä: omalla rivillään painikkeen alla ⋮ jäi irralliseksi.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = authState.messageText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (authState is RavelryAuthState.Connected) {
                    RavelryAccountMenu(onDisconnect = onDisconnect)
                }
            }
            RavelryAccountActions(
                authState = authState,
                onSignIn = onSignIn,
                onBrowseRavelry = onBrowseRavelry,
            )
        }
    }
}

@Composable
private fun RavelryAccountActions(
    authState: RavelryAuthState,
    onSignIn: () -> Unit,
    onBrowseRavelry: () -> Unit,
) {
    when (authState) {
        is RavelryAuthState.Connected -> {
            Button(onClick = onBrowseRavelry) {
                Text(stringResource(R.string.ravelry_browse))
            }
        }

        RavelryAuthState.Starting,
        RavelryAuthState.Disconnecting,
        -> {
            CircularProgressIndicator()
        }

        else -> {
            Button(onClick = onSignIn) {
                Text(stringResource(R.string.ravelry_sign_in))
            }
        }
    }
}

@Composable
private fun RavelryAccountMenu(onDisconnect: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showDisconnectDialog by rememberSaveable { mutableStateOf(false) }

    if (showDisconnectDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.ravelry_disconnect),
            message = stringResource(R.string.ravelry_disconnect_confirm),
            confirmText = stringResource(R.string.ravelry_disconnect),
            isDestructive = true,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnect()
            },
            onDismiss = { showDisconnectDialog = false },
        )
    }

    MoreOptionsIconButton(onClick = { expanded = true })
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ravelry_disconnect)) },
            onClick = {
                expanded = false
                showDisconnectDialog = true
            },
        )
    }
}

private val headerVerticalPadding = 6.dp
private val headerContentPadding = 12.dp
private val headerActionSpacing = 8.dp

@Composable
private fun RavelryAuthState.messageText(): String =
    when (this) {
        RavelryAuthState.NotConnected -> {
            stringResource(R.string.ravelry_not_connected)
        }

        RavelryAuthState.Starting -> {
            stringResource(R.string.ravelry_connecting)
        }

        RavelryAuthState.AwaitingBrowser -> {
            stringResource(R.string.ravelry_auth_pending)
        }

        is RavelryAuthState.Connected -> {
            if (username.isNullOrBlank()) {
                stringResource(R.string.ravelry_connected)
            } else {
                stringResource(R.string.ravelry_connected_as, username)
            }
        }

        RavelryAuthState.Cancelled -> {
            stringResource(R.string.ravelry_auth_cancelled)
        }

        RavelryAuthState.Expired -> {
            stringResource(R.string.ravelry_auth_expired)
        }

        RavelryAuthState.BackendUnavailable -> {
            stringResource(R.string.ravelry_backend_unavailable)
        }

        RavelryAuthState.Disconnecting -> {
            stringResource(R.string.ravelry_disconnecting)
        }
    }
