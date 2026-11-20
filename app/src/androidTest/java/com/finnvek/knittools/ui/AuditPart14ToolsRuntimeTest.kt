package com.finnvek.knittools.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.ui.screens.abbreviations.AbbreviationsScreen
import com.finnvek.knittools.ui.screens.caston.CastOnScreen
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import com.finnvek.knittools.ui.screens.increase.IncreaseDecreaseScreen
import com.finnvek.knittools.ui.screens.sizecharts.SizeChartScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuditPart14ToolsRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val app = ApplicationProvider.getApplicationContext<App>()
    private val store = ViewModelStore()
    private lateinit var home: HomeViewModel

    @Before fun prepare() {
        check(app.packageName.endsWith(".activepolicytest"))
        runBlocking {
            if (app.preferencesManager
                    .get()
                    .preferences
                    .first()
                    .useImperial
            ) {
                app.preferencesManager.get().setUseImperial(false)
            }
        }
        rule.runOnUiThread {
            home = HomeViewModel(app.proManager.get(), app.preferencesManager.get())
            store.put("home", home)
        }
    }

    @After fun close() = rule.runOnUiThread { store.clear() }

    @Test fun castOnRendersSnappedCountAndDropsInvalidResult() {
        rule.setContent { KnitToolsTheme { CastOnScreen(onBack = {}, homeViewModel = home) } }
        input(0, "10")
        input(1, "23")
        input(2, "4")
        input(3, "2")
        val result = rule.activity.resources.getQuantityString(R.plurals.stitches_result, 26, 26)
        rule.onNodeWithText(result).performScrollTo().assertIsDisplayed()
        input(1, "")
        rule.onNodeWithText(result).assertDoesNotExist()
    }

    @Test fun increaseAndDecreaseModesReplaceTheVisibleTotal() {
        rule.setContent { KnitToolsTheme { IncreaseDecreaseScreen(onBack = {}, homeViewModel = home) } }
        input(0, "17")
        input(1, "3")
        val unit = rule.activity.getString(R.string.unit_st)
        val increased =
            rule.activity
                .getString(
                    R.string.total_stitches_format,
                    20,
                    unit,
                ).uppercase(rule.activity.resources.configuration.locales[0])
        rule.onNodeWithText(increased).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.style_circular)).performScrollTo().performClick()
        rule.onNodeWithText(increased).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.mode_decrease)).performScrollTo().performClick()
        val decreased =
            rule.activity
                .getString(
                    R.string.total_stitches_format,
                    14,
                    unit,
                ).uppercase(rule.activity.resources.configuration.locales[0])
        rule.onNodeWithText(decreased).performScrollTo().assertIsDisplayed()
        input(1, "17")
        rule.onNodeWithText(decreased).assertDoesNotExist()
    }

    @Test fun choosingSavedYarnUpdatesPerSkeinValuesAndKeepsTotalNeeded() {
        val name = "Estimator sample yarn"
        val database = app.database.get()
        val repository =
            YarnCardRepository(
                database.yarnCardDao(),
                database.counterProjectDao(),
                app,
                RoomDatabaseTransactionRunner(database),
                Dispatchers.IO,
            )
        val card = YarnCard(yarnName = name, lengthMeters = "251", weightGrams = "100")
        val cardId = checkNotNull(runBlocking { repository.saveCard(card) })
        val estimator = YarnEstimatorViewModel(repository)
        rule.runOnUiThread { store.put("estimator", estimator) }
        try {
            rule.setContent {
                KnitToolsTheme { YarnEstimatorScreen(onBack = {}, homeViewModel = home, viewModel = estimator) }
            }
            input(0, "501")
            input(1, "200")
            input(2, "50")
            val originalWeight = rule.activity.getString(R.string.total_weight, "150")
            rule.onNodeWithText(originalWeight).performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(rule.activity.getString(R.string.choose_from_my_yarn)).performScrollTo().performClick()
            rule.onNodeWithText(name).assertIsDisplayed().performClick()
            val selectedWeight = rule.activity.getString(R.string.total_weight, "200")
            rule.onNodeWithText(selectedWeight).performScrollTo().assertIsDisplayed()
            input(1, "0")
            rule.onNodeWithText(selectedWeight).assertDoesNotExist()
        } finally {
            runBlocking { repository.deleteCard(cardId) }
        }
    }

    @Test fun everySizeCategoryDisplaysItsOwnHeaderAndFirstRow() {
        rule.setContent { KnitToolsTheme { SizeChartScreen(onBack = {}, homeViewModel = home) } }
        val cases =
            listOf(
                Triple(R.string.category_baby, R.string.size_col_chest, R.string.size_row_age_0_3m),
                Triple(R.string.category_child_youth, R.string.size_col_chest, R.string.size_row_child_2),
                Triple(R.string.category_women, R.string.size_col_bust, R.string.size_row_women_xs_2_4),
                Triple(R.string.category_men, R.string.size_col_chest, R.string.size_row_men_s_34_36),
                Triple(R.string.category_head, R.string.size_col_circumference, R.string.size_row_preemie),
                Triple(R.string.category_hand, R.string.size_col_circumference, R.string.size_row_child_s),
            )
        val context = rule.activity
        for (unit in listOf(R.string.unit_cm, R.string.measurement_unit_inch_short)) {
            runBlocking { app.preferencesManager.get().setUseImperial(unit != R.string.unit_cm) }
            for ((category, header, firstRow) in cases) {
                rule.onNodeWithText(context.getString(category)).performClick()
                val measurementHeader =
                    context.getString(
                        R.string.size_chart_header_with_unit,
                        context.getString(header),
                        context.getString(unit),
                    )
                rule.onNodeWithText(measurementHeader).assertIsDisplayed()
                rule.onNodeWithText(context.getString(firstRow)).assertIsDisplayed()
            }
        }
    }

    @Test fun abbreviationQueryAndExpansionSurviveCraftContextChange() {
        val craft = mutableStateOf(CraftType.KNITTING)
        rule.setContent { KnitToolsTheme { AbbreviationsScreen(craftType = craft.value, onBack = {}) } }
        input(0, "k2TOG")
        rule.onNodeWithText("K2tog").assertIsDisplayed().performClick()
        val description = rule.activity.getString(R.string.abbr_k2tog_desc)
        rule.onNodeWithText(description).assertIsDisplayed()
        rule.runOnUiThread { craft.value = CraftType.CROCHET }
        rule.onNodeWithText(description).assertIsDisplayed()
        rule.onAllNodesWithText("K2tog").onFirst().assertIsDisplayed()
    }

    private fun input(
        index: Int,
        text: String,
    ) {
        rule.onAllNodes(hasSetTextAction())[index].performScrollTo().performTextReplacement(text)
    }
}
