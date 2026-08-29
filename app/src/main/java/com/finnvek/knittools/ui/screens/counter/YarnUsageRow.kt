package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import com.finnvek.knittools.domain.model.YarnUsageUnit

@Composable
internal fun YarnUsageRow(
    item: ProjectYarnUsageItem,
    onUsage: (YarnUsageOpenRequest) -> Unit,
    requestFocus: Boolean = false,
) {
    val name =
        item.name.ifBlank {
            stringResource(
                R.string.yarn_card_number_fallback,
                item.source.yarnCardId ?: item.usage?.id ?: 0,
            )
        }
    val focus = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(requestFocus, windowFocused) {
        if (requestFocus && windowFocused) {
            bring.bringIntoView()
            focus.requestFocus()
        }
    }
    Column(modifier = Modifier.fillMaxWidth().testTag("yarn_usage_row_${item.key}")) {
        if (item.status != YarnUsageSourceStatus.AVAILABLE) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    if (item.status ==
                        YarnUsageSourceStatus.UNLINKED
                    ) {
                        R.string.yarn_usage_unlinked
                    } else {
                        R.string.yarn_usage_unavailable
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item.usage?.amounts?.let { amounts ->
            amounts.usedMeters?.let { used ->
                yarnUsageAmount(used, YarnUsageUnit.METERS, amounts)?.let { value ->
                    Text(
                        stringResource(R.string.yarn_usage_used_format, value),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(yarnUsageRemaining(amounts, YarnUsageUnit.METERS), style = MaterialTheme.typography.bodySmall)
        }
        val action = if (item.usage == null) R.string.yarn_usage_track else R.string.yarn_usage_edit
        val description =
            stringResource(
                if (item.usage ==
                    null
                ) {
                    R.string.yarn_usage_track_named
                } else {
                    R.string.yarn_usage_edit_named
                },
                name,
            )
        TextButton(
            onClick = { onUsage(YarnUsageOpenRequest(item, name)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 48.dp,
                    ).bringIntoViewRequester(bring)
                    .focusRequester(focus)
                    .focusProperties { canFocus = true }
                    .semantics {
                        contentDescription =
                            description
                    },
        ) {
            Text(stringResource(action))
        }
        if (item.status != YarnUsageSourceStatus.AVAILABLE && item.usage != null) {
            val deleteLabel = stringResource(R.string.yarn_usage_delete_named, name)
            TextButton(
                onClick = { onUsage(YarnUsageOpenRequest(item, name, delete = true)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = deleteLabel },
            ) {
                Text(stringResource(R.string.yarn_usage_delete))
            }
        }
    }
}
