package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.finnvek.knittools.ui.theme.ComponentDimens

/** Lomakesheetin vahvistus: teksti, käytettävyys ja tallennuksen aikainen latausilmaisin. */
data class FormSheetConfirm(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val busy: Boolean = false,
)

/**
 * Kaikkien lisäysten ja muokkausten yhteinen sheet: uusi projekti, lanka ja verkkokaava.
 * Aiemmin ne olivat dialogi, sheet ja koko näytön lomake omilla kenttä- ja painiketyyleillään.
 * Otsikko, kentät ja painikerivi (neutraali Cancel, täytetty päätoiminto) ovat aina samat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String,
    confirm: FormSheetConfirm,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    cancelEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = ComponentDimens.FormSheetHorizontalPadding)
                    .padding(bottom = ComponentDimens.FormSheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.FormSheetItemSpacing),
        ) {
            SheetTitle(text = title)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
            FormSheetActions(confirm = confirm, onCancel = onDismiss, cancelEnabled = cancelEnabled)
        }
    }
}

/** Sheetin otsikko: sama taso kuin dialogien otsikoissa kaikissa sheeteissä. */
@Composable
fun SheetTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/**
 * Lomakkeen painikerivi: neutraali Cancel ja täytetty päätoiminto oikeassa reunassa. Jaettu myös
 * sheeteille, jotka tarvitsevat oman runkonsa (esim. langan käytön editori), jotta painikkeet ovat
 * kaikkialla samassa järjestyksessä eivätkä allekkain.
 */
@Composable
internal fun FormSheetActions(
    confirm: FormSheetConfirm,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelEnabled: Boolean = true,
    confirmModifier: Modifier = Modifier,
    cancelModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CancelButton(onClick = onCancel, enabled = cancelEnabled, modifier = cancelModifier)
        Button(
            onClick = confirm.onClick,
            enabled = confirm.enabled && !confirm.busy,
            modifier = confirmModifier.heightIn(min = ComponentDimens.FormSheetActionMinHeight),
        ) {
            if (confirm.busy) {
                CircularProgressIndicator(modifier = Modifier.size(ComponentDimens.InfoIconSize))
            } else {
                Text(confirm.text)
            }
        }
    }
}

/**
 * Navigaation dialogikohde, jonka ainoa sisältö on [FormSheet]: sheet piirtää oman ikkunansa ja
 * himmennyksensä, joten tyhjän isäntäikkunan oma himmennys poistetaan, ettei tausta tummu kahdesti.
 */
@Composable
fun FormSheetDialogHost() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
}
