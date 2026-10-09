package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.StandardYarnWeight
import com.finnvek.knittools.ui.components.DropdownIndicator
import com.finnvek.knittools.ui.components.ProjectYarnTextField
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Langan paksuus valitaan vakiopaksuuksista: vapaana tekstinä sama paksuus kirjoitettiin monella tavalla.
 * "Other" ja Ravelrystä tuodut muut arvot (esim. "Light Fingering") jäävät muokattavaksi tekstiksi.
 */
@Composable
internal fun YarnWeightField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.weight_category)
    var customMode by rememberSaveable { mutableStateOf(!StandardYarnWeight.isStandardOrBlank(value)) }
    if (customMode) {
        ProjectYarnTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            singleLine = true,
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    // Valikko on kentän levyinen kuten Gaugen valitsimissa: sisällön levyisenä se jäi irralliseksi kaistaleeksi.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val menuWidth = maxWidth
        ProjectYarnTextField(
            value = value,
            onValueChange = {},
            label = label,
            singleLine = true,
            readOnly = true,
            trailingIcon = { DropdownIndicator() },
        )
        // Vain luku -kenttä ei avaa listaa itse, joten napautus otetaan koko kentän päältä.
        Spacer(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = label) { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            StandardYarnWeight.entries.forEach { weight ->
                val selected = weight.label.equals(value.trim(), ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Text(
                            weight.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (selected) {
                                    MaterialTheme.knitToolsColors.primaryReadable
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    trailingIcon = {
                        Text(
                            stringResource(R.string.yarn_weight_ply_format, weight.ply),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                        )
                    },
                    onClick = {
                        onValueChange(weight.label)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.yarn_weight_other), style = MaterialTheme.typography.bodyLarge) },
                onClick = {
                    customMode = true
                    expanded = false
                },
            )
        }
    }
}
