package com.finnvek.knittools.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

data class SelectionAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/**
 * Valintamoodin alapalkki: yksi matala rivi tekstitoimintoja. Kolme täysleveää täytettyä
 * painiketta peitti neljänneksen listasta, ja kaksi oranssia kilpaili keskenään.
 * Poisto on punainen teksti kuten valikoissa.
 */
@Composable
fun SelectionActionBar(
    visible: Boolean,
    actions: List<SelectionAction>,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Surface(color = MaterialTheme.knitToolsColors.cardContainer) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(ComponentDimens.StandardSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    TextButton(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        modifier =
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ComponentDimens.SelectionBarActionMinHeight),
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor =
                                    if (action.destructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.knitToolsColors.primaryReadable
                                    },
                            ),
                    ) {
                        Text(text = action.label, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
