package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.displayName
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YarnCardPickerSheet(
    savedYarnCards: List<YarnCard>,
    onSelect: (YarnCard) -> Unit,
    onDismiss: () -> Unit,
    onAddYarn: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberExpandedModalSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
    ) {
        AdaptiveModalWindow()
        LazyColumn(
            modifier =
                Modifier
                    .padding(horizontal = ComponentDimens.FormSheetHorizontalPadding)
                    .padding(bottom = ComponentDimens.FormSheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.FormSheetItemSpacing),
        ) {
            item { SheetTitle(text = stringResource(R.string.select_yarn_card)) }
            if (savedYarnCards.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_saved_yarns),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.knitToolsColors.emptyStateText,
                    )
                }
                if (onAddYarn != null) {
                    item {
                        OverviewTextAction(
                            label = stringResource(R.string.yarn_picker_empty_action),
                            onClick = onAddYarn,
                        )
                    }
                }
            } else {
                items(savedYarnCards, key = { it.id }) { card ->
                    val fallback = stringResource(R.string.yarn_card_number_fallback, card.id)
                    val color =
                        listOfNotNull(
                            card.colorName.takeIf { it.isNotBlank() },
                            card.colorNumber.takeIf { it.isNotBlank() }?.let {
                                stringResource(R.string.yarn_color_number_short, it)
                            },
                        ).joinToString(", ")
                    SheetOptionRow(
                        title = card.displayName { fallback },
                        body = color.ifBlank { null },
                        onClick = { onSelect(card) },
                    )
                }
            }
        }
    }
}
