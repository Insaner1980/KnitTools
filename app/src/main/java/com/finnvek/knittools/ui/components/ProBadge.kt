package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus

enum class ProBadgeState {
    Locked,
    Trial,
    Hidden,
}

internal fun proBadgeState(status: ProStatus): ProBadgeState =
    when (status) {
        ProStatus.TRIAL_NOT_STARTED,
        ProStatus.TRIAL_EXPIRED,
        -> ProBadgeState.Locked
        ProStatus.TRIAL_ACTIVE -> ProBadgeState.Trial
        ProStatus.PRO_PURCHASED -> ProBadgeState.Hidden
    }

@Composable
fun ProBadge(
    status: ProStatus,
    modifier: Modifier = Modifier,
) {
    val state = proBadgeState(status)
    if (state == ProBadgeState.Hidden) return
    val description =
        stringResource(
            if (state == ProBadgeState.Trial) {
                R.string.pro_badge_trial_description
            } else {
                R.string.pro_badge_locked_description
            },
        )
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringResource(R.string.pro_badge),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
