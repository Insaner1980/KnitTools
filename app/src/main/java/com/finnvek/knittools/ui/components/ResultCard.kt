package com.finnvek.knittools.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finnvek.knittools.ui.theme.ComponentDimens

@Composable
fun ResultCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ComponentDimens.ResultCardCornerRadius),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ),
        border =
            BorderStroke(
                ComponentDimens.ResultCardBorderWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentDimens.FlatElevation),
    ) {
        Column(modifier = Modifier.padding(ComponentDimens.LargeContentPadding)) {
            Text(
                text = title.localizedUppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(ComponentDimens.ContentSpacing))
            content()
        }
    }
}
