package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finnvek.knittools.ui.theme.ComponentDimens

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = ComponentDimens.StandardSpacing),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = highContainerTextFieldColors(),
    )
}
