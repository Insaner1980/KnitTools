package com.finnvek.knittools.ui.screens.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.theme.knitToolsColors

/** Langan tilan nimi ja pillerin värit; sama esitys My Yarn -kortissa ja langan sivun valitsimessa. */
data class YarnStatusUi(
    val key: String,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
fun yarnStatusUi(status: String): YarnStatusUi =
    when (status) {
        YarnCardStatus.IN_USE -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_in_use),
                // Käytössä oleva lanka on aktiivinen tila kuten BadgePill: petrooli + oranssi, ei ruskeaksi
                // sekoittuvaa oranssin läpikuultoa.
                containerColor = MaterialTheme.knitToolsColors.actionContainer,
                contentColor = MaterialTheme.knitToolsColors.primaryReadable,
            )
        }

        YarnCardStatus.FINISHED -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_finished),
                containerColor = MaterialTheme.knitToolsColors.onSurfaceMuted.copy(alpha = 0.14f),
                contentColor = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }

        else -> {
            YarnStatusUi(
                key = YarnCardStatus.IN_STASH,
                label = stringResource(R.string.status_in_stash),
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.secondary,
            )
        }
    }

/** Langan tila samana merkkinä kuin muut tilamerkit, mutta tilan omilla väreillä. */
@Composable
fun YarnStatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    val statusUi = yarnStatusUi(status)
    BadgePill(
        text = statusUi.label,
        modifier = modifier,
        containerColor = statusUi.containerColor,
        contentColor = statusUi.contentColor,
    )
}

@Composable
fun yarnStatusOptions(): List<YarnStatusUi> =
    listOf(
        yarnStatusUi(YarnCardStatus.IN_STASH),
        yarnStatusUi(YarnCardStatus.IN_USE),
        yarnStatusUi(YarnCardStatus.FINISHED),
    )
