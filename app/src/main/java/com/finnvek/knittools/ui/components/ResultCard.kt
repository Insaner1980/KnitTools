package com.finnvek.knittools.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun ResultCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.knitToolsColors.actionContainer,
            ),
        border =
            BorderStroke(
                ComponentDimens.ResultCardBorderWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentDimens.FlatElevation),
    ) {
        Column(modifier = Modifier.padding(ComponentDimens.LargeContentPadding)) {
            SectionLabel(text = title)
            Spacer(modifier = Modifier.height(ComponentDimens.ContentSpacing))
            content()
        }
    }
}
