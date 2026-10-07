package com.finnvek.knittools.ui.screens.yarn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MeasurementNumberParser
import com.finnvek.knittools.domain.calculator.YarnEstimator
import com.finnvek.knittools.domain.calculator.formatDecimalForDisplay
import com.finnvek.knittools.domain.model.YarnEstimate
import com.finnvek.knittools.ui.components.AnimatedResultNumber
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.InfoNote
import com.finnvek.knittools.ui.components.NumberInputField
import com.finnvek.knittools.ui.components.NumberInputOptions
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ResultCard
import com.finnvek.knittools.ui.components.ResultPlaceholder
import com.finnvek.knittools.ui.components.ToolInputCard
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.components.skeinCountText
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import java.util.Locale
import kotlin.math.ceil

@Composable
fun YarnEstimatorScreen(
    onBack: () -> Unit,
    onSavedYarns: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()

    ToolScreenScaffold(
        title = stringResource(R.string.tool_yarn_estimator),
        onBack = onBack,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            YarnEstimatorContent(
                onSavedYarns = onSavedYarns,
                useImperial = useImperial,
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun YarnEstimatorContent(
    onSavedYarns: () -> Unit,
    useImperial: Boolean,
) {
    var totalYarn by rememberSaveable { mutableStateOf("") }
    var yarnPerSkein by rememberSaveable { mutableStateOf("") }
    var weightPerSkein by rememberSaveable { mutableStateOf("") }
    val locale = rememberCurrentLocale()

    val result by remember(totalYarn, yarnPerSkein, weightPerSkein, locale) {
        derivedStateOf { calculateYarnEstimate(totalYarn, yarnPerSkein, weightPerSkein, locale) }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SavedYarnActionBar(onSavedYarns = onSavedYarns)

        ToolInputCard {
            YarnInputFields(
                totalYarn = totalYarn,
                yarnPerSkein = yarnPerSkein,
                weightPerSkein = weightPerSkein,
                useImperial = useImperial,
                onTotalYarnChange = { totalYarn = it },
                onYarnPerSkeinChange = { yarnPerSkein = it },
                onWeightPerSkeinChange = { weightPerSkein = it },
            )
        }

        val currentResult = result
        if (currentResult == null) {
            ResultPlaceholder(text = stringResource(R.string.yarn_result_placeholder))
        } else {
            YarnResultCard(currentResult)
        }
    }
}

// Tekstitoiminto laatikkoikonin tilalla: ikonista ei voinut arvata, että se hakee langan My Yarnista.
@Composable
private fun SavedYarnActionBar(onSavedYarns: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        OverviewTextAction(
            label = stringResource(R.string.choose_from_my_yarn),
            onClick = onSavedYarns,
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        )
    }
}

@Composable
private fun YarnInputFields(
    totalYarn: String,
    yarnPerSkein: String,
    weightPerSkein: String,
    useImperial: Boolean,
    onTotalYarnChange: (String) -> Unit,
    onYarnPerSkeinChange: (String) -> Unit,
    onWeightPerSkeinChange: (String) -> Unit,
) {
    val lengthUnit = stringResource(if (useImperial) R.string.unit_yards else R.string.unit_meters)
    // Esimerkit tyypillisestä villapuserosta ja 50 g:n kerästä yksikön mukaan.
    val totalExample = if (useImperial) EXAMPLE_TOTAL_YARDS else EXAMPLE_TOTAL_METERS
    val perSkeinExample = if (useImperial) EXAMPLE_YARDS_PER_SKEIN else EXAMPLE_METERS_PER_SKEIN
    NumberInputField(
        value = totalYarn,
        onValueChange = onTotalYarnChange,
        label = stringResource(R.string.total_yarn_needed),
        modifier = Modifier.fillMaxWidth(),
        options = NumberInputOptions(isDecimal = true, suffix = lengthUnit, placeholder = totalExample),
    )
    NumberInputField(
        value = yarnPerSkein,
        onValueChange = onYarnPerSkeinChange,
        label = stringResource(R.string.yarn_per_skein, lengthUnit),
        modifier = Modifier.fillMaxWidth(),
        options = NumberInputOptions(isDecimal = true, suffix = lengthUnit, placeholder = perSkeinExample),
    )
    NumberInputField(
        value = weightPerSkein,
        onValueChange = onWeightPerSkeinChange,
        label = stringResource(R.string.weight_per_skein),
        modifier = Modifier.fillMaxWidth(),
        options =
            NumberInputOptions(
                isDecimal = true,
                suffix = stringResource(R.string.unit_g),
                isLast = true,
                placeholder = EXAMPLE_GRAMS_PER_SKEIN,
            ),
    )
}

private const val EXAMPLE_TOTAL_METERS = "1200"
private const val EXAMPLE_TOTAL_YARDS = "1300"
private const val EXAMPLE_METERS_PER_SKEIN = "200"
private const val EXAMPLE_YARDS_PER_SKEIN = "220"
private const val EXAMPLE_GRAMS_PER_SKEIN = "50"

@Composable
private fun YarnResultCard(result: YarnEstimate) {
    val locale = rememberCurrentLocale()
    ResultCard(title = stringResource(R.string.result)) {
        AnimatedResultNumber(
            targetValue = skeinCountText(result.skeinsNeeded),
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BadgePill(
            text =
                stringResource(
                    R.string.total_weight,
                    formatDecimalForDisplay(result.totalWeight, locale, 0, 0),
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                stringResource(
                    R.string.estimated_skeins,
                    formatSkeinsEstimateForDisplay(result.exactSkeins, locale),
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    InfoNote(text = stringResource(R.string.extra_skein_note))
}

private fun calculateYarnEstimate(
    totalYarn: String,
    yarnPerSkein: String,
    weightPerSkein: String,
    locale: Locale,
): YarnEstimate? {
    val total = MeasurementNumberParser.parse(totalYarn, locale).value ?: return null
    val perSkein = MeasurementNumberParser.parse(yarnPerSkein, locale).value ?: return null
    val weight = MeasurementNumberParser.parse(weightPerSkein, locale).value ?: return null
    return YarnEstimator.estimate(total, perSkein, weight)
}

private fun formatSkeinsEstimateForDisplay(
    exactSkeins: Double,
    locale: Locale,
): String {
    val roundedUp = ceil(exactSkeins * 100.0 - DISPLAY_ROUNDING_EPSILON) / 100.0
    return formatDecimalForDisplay(roundedUp, locale, minimumFractionDigits = 2, maximumFractionDigits = 2)
}

private const val DISPLAY_ROUNDING_EPSILON = 1e-9
