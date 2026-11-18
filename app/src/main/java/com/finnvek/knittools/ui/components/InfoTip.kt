package com.finnvek.knittools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.ComponentDimens

/** Vihjeen otsikko ja selitys; otsikko on myös ikonin ruudunlukijateksti. */
data class InfoTipText(
    val title: String,
    val description: String,
)

@Composable
fun InfoTip(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier,
    ) {
        InfoIcon(title)
    }

    if (showDialog) InfoTipDialog(InfoTipText(title, description)) { showDialog = false }
}

/**
 * Nimike ja sen vieressä info-vihje. Vihje kuuluu nimikkeen viereen eikä kontrollin päähän:
 * kontrollin vieressä se kavensi kenttää tai valitsinta, eivätkä reunat enää linjautuneet.
 * Ikoni on nimikkeen kokoinen, ja Compose laajentaa osuma-alueen 48 dp:hen rivikorkeutta kasvattamatta.
 */
@Composable
fun LabelWithInfo(
    label: String,
    info: InfoTipText?,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        info?.let {
            InfoIcon(
                title = it.title,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { showDialog = true },
            )
        }
    }
    if (showDialog && info != null) InfoTipDialog(info) { showDialog = false }
}

@Composable
private fun InfoIcon(
    title: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = title,
        modifier = modifier.size(ComponentDimens.InfoIconSize),
        tint = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun InfoTipDialog(
    info: InfoTipText,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = info.description,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
