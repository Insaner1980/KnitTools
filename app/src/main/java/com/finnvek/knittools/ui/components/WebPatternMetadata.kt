package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R

@Composable
internal fun WebPatternMetadata(
    designerName: String?,
    host: String?,
    uppercaseBadge: Boolean = true,
) {
    designerName?.takeIf(String::isNotBlank)?.let { designer ->
        Text(
            text = designer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    BadgePill(text = stringResource(R.string.web_pattern_label), uppercase = uppercaseBadge)
    host?.takeIf(String::isNotBlank)?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
