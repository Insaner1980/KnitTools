package com.finnvek.knittools.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppLanguage
import com.finnvek.knittools.data.datastore.ThemeMode
import com.finnvek.knittools.ui.components.InfoTip
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onUpgradeToPro: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    val voiceUsage by viewModel.voiceLiveUsage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showLanguageSheet = remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, context) {
        viewModel.messages.collect { messageRes ->
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.languageChanged.collect {
            context.findActivity()?.recreate()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_settings))

            SettingsSelectionRow(
                label = stringResource(R.string.settings_language),
                supportingText = stringResource(R.string.settings_language_change_hint),
                valueText = stringResource(prefs.appLanguage.labelResId()),
                onClick = { showLanguageSheet.value = true },
            )

            HorizontalDivider()

            ThemeRow(
                label = stringResource(R.string.theme_light),
                selected = prefs.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
            )
            ThemeRow(
                label = stringResource(R.string.theme_dark),
                selected = prefs.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
            )
            ThemeRow(
                label = stringResource(R.string.theme_system),
                selected = prefs.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
            )

            HorizontalDivider()

            SwitchRow(
                label = stringResource(R.string.haptic_feedback),
                checked = prefs.hapticFeedback,
                onCheckedChange = { viewModel.setHapticFeedback(it) },
            )
            SwitchRow(
                label = stringResource(R.string.keep_screen_awake),
                checked = prefs.keepScreenAwake,
                onCheckedChange = { viewModel.setKeepScreenAwake(it) },
            )
            SwitchRowWithTip(
                label = stringResource(R.string.use_imperial_units),
                checked = prefs.useImperial,
                onCheckedChange = { viewModel.setUseImperial(it) },
                tipTitle = stringResource(R.string.tip_imperial_units_title),
                tipDescription = stringResource(R.string.tip_imperial_units_desc),
            )
            SwitchRow(
                label = stringResource(R.string.show_knitting_tips),
                checked = prefs.showKnittingTips,
                onCheckedChange = { viewModel.setShowKnittingTips(it) },
            )
            if (proState.isPro || BuildConfig.DEBUG) {
                SwitchRow(
                    label = stringResource(R.string.voice_natural_response),
                    checked = prefs.voiceLiveEnabled,
                    onCheckedChange = { viewModel.setVoiceLiveEnabled(it) },
                )
                Text(
                    text =
                        stringResource(
                            R.string.voice_live_quota_remaining,
                            voiceUsage.remainingMinutes,
                            voiceUsage.monthlyAllowance,
                        ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.knitToolsColors.onSurfaceMuted,
                )
            }

            if (!proState.isPro) {
                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_section_pro))

                SettingsActionRow(
                    label = stringResource(R.string.upgrade_to_pro),
                    onClick = onUpgradeToPro,
                )
                HorizontalDivider()
                SettingsActionRow(
                    label = stringResource(R.string.restore_purchases),
                    onClick = viewModel::restorePurchases,
                )
            }

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_info))

            SettingsActionRow(
                label = stringResource(R.string.help_and_guide),
                onClick = {
                    val intent =
                        androidx.browser.customtabs.CustomTabsIntent
                            .Builder()
                            .build()
                    intent.launchUrl(context, android.net.Uri.parse("https://knittools.app/guide"))
                },
            )

            HorizontalDivider()
            SettingsInfoText(
                text = stringResource(R.string.privacy_summary),
            )
            HorizontalDivider()
            SettingsInfoText(
                text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            )
        }
    }

    if (showLanguageSheet.value) {
        LanguagePickerBottomSheet(
            selectedLanguage = prefs.appLanguage,
            onDismiss = { showLanguageSheet.value = false },
            onSelectLanguage = { language ->
                viewModel.setAppLanguage(language)
                showLanguageSheet.value = false
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SettingsActionRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
    }
}

@Composable
private fun SettingsSelectionRow(
    label: String,
    supportingText: String,
    valueText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsInfoText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.knitToolsColors.onSurfaceMuted,
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SwitchRowWithTip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tipTitle: String,
    tipDescription: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        InfoTip(title = tipTitle, description = tipDescription)
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerBottomSheet(
    selectedLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_language_change_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
            Spacer(modifier = Modifier.height(20.dp))
            AppLanguage.entries.forEach { language ->
                LanguageOptionRow(
                    label = stringResource(language.labelResId()),
                    selected = language == selectedLanguage,
                    onClick = { onSelectLanguage(language) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun AppLanguage.labelResId(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.FINNISH -> R.string.settings_language_finnish
        AppLanguage.ENGLISH -> R.string.settings_language_english
        AppLanguage.SWEDISH -> R.string.settings_language_swedish
        AppLanguage.GERMAN -> R.string.settings_language_german
        AppLanguage.FRENCH -> R.string.settings_language_french
        AppLanguage.SPANISH -> R.string.settings_language_spanish
        AppLanguage.PORTUGUESE -> R.string.settings_language_portuguese
        AppLanguage.ITALIAN -> R.string.settings_language_italian
        AppLanguage.NORWEGIAN -> R.string.settings_language_norwegian
        AppLanguage.DANISH -> R.string.settings_language_danish
        AppLanguage.DUTCH -> R.string.settings_language_dutch
    }
