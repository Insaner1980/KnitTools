package com.finnvek.knittools.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.TrialEndedDialog
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.project.ProjectSetupActions
import com.finnvek.knittools.ui.screens.project.ProjectSetupSection
import com.finnvek.knittools.ui.screens.project.ProjectSetupVisit
import com.finnvek.knittools.ui.screens.project.ProjectsStartState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class OnboardingLocalePresentationTest(
    private val language: String,
) {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.localizedTestContext(language)
    private val configuration = context.resources.configuration
    private val currency =
        when (language) {
            "pt" -> "BRL"
            "pt-BR" -> "EUR"
            else -> "USD"
        }
    private val price =
        when (currency) {
            "BRL" -> "R$ 24,90"
            "EUR" -> "9,99 €"
            else -> "US$9.99"
        }
    private val quote =
        SelectedOneTimeOffer(
            price,
            "test-offer",
            if (currency == "BRL") 24_900_000 else 9_990_000,
            currency,
            launchDetails = LaunchOfferDetails(1, 1_800_000_000_000, if (currency == "BRL") 32_900_000 else 14_990_000),
        )

    @Test
    fun welcomeUsesLocalizedCopyAndIntactAction() {
        show {
            ProjectsStartState(
                false,
                CraftType.KNITTING,
                {},
                {},
                {},
                {},
                modifier = Modifier.verticalScroll(rememberScrollState()),
                proState = ProState(),
                proAccessReady = true,
            )
        }
        compose.onNodeWithText(text(R.string.welcome_free_pro_trial)).assertExists()
        actionIsNotTruncated(R.string.welcome_learn_pro)
    }

    @Test
    fun restoredFormShowsLocalizedAdvancedControls() {
        show {
            ProjectDetailsSheet(
                text(R.string.new_project_details_title),
                text(R.string.create_project),
                ProjectDetailsValues("Test", CraftType.KNITTING, MainCounterLabelType.ROWS, null, 0, 120),
                {},
                {},
                mode = ProjectDetailsMode.Create,
            )
        }
        compose.onNodeWithText(text(R.string.project_target_label)).assertExists()
        actionIsNotTruncated(R.string.project_create_fewer_options)
        actionIsNotTruncated(R.string.create_project)
    }

    @Test
    fun suggestionsRemainOptionalInEveryLanguage() {
        show {
            ProjectSetupSection(
                CounterUiState(
                    projectId = 7,
                    projectName = "Test",
                    projectsLoaded = true,
                    preferencesLoaded = true,
                    proStateLoaded = true,
                    activeSessionLoaded = true,
                    initialSessionRefreshFinished = true,
                    remindersLoadedProjectId = 7,
                    countersLoadedProjectId = 7,
                    documentsLoadedProjectId = 7,
                ),
                OnboardingState(initializedVersion = 1, starterProjectId = 7),
                ProjectSetupVisit(7, 1),
                null,
                ProjectSetupActions({}, {}, {}),
                canCelebrate = true,
            )
        }
        compose.onNodeWithText(text(R.string.project_setup_optional_body)).assertExists()
        compose.onNodeWithText(text(R.string.project_setup_target)).assertExists()
    }

    @Test
    fun contextualTrialPromptShowsCommercialTermsBeforeItsAction() {
        show {
            ProPromptContent(
                ProPromptRequest(ProPromptSource.Projects),
                ProStatus.TRIAL_NOT_STARTED,
                quote,
                BillingProductStatus.Available,
                false,
                false,
                {},
                {},
                {},
                {},
            )
        }
        compose.onNodeWithText(text(R.string.onboarding_pro_trial_terms)).assertExists()
        compose.onNodeWithText(text(R.string.pro_trial_does_not_reserve_price)).assertExists()
        compose.onNodeWithText(text(R.string.pro_launch_price, price)).assertExists()
        actionIsNotTruncated(R.string.pro_start_free_trial)
    }

    @Test
    fun priceArgumentsFollowPlayCurrencyAndLocalizedDateKeepsTimezone() {
        show { ProPriceDetails(quote, BillingProductStatus.Available, true, {}) }
        compose.onNodeWithText(text(R.string.pro_launch_price, price)).assertExists()
        val fullPrice = formatOfferFullPrice(quote, Locale.forLanguageTag(language))
        assertTrue(fullPrice != null)
        compose.onNodeWithText(text(R.string.pro_price_after_launch, fullPrice)).assertExists()
        val prefix = text(R.string.pro_launch_ends_at, "").trim()
        val deadline = compose.onNodeWithText(prefix, substring = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        deadline.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertTrue(layouts.isNotEmpty())
        assertFalse(
            layouts
                .single()
                .layoutInput.text.text
                .contains("%1" + '$' + "s"),
        )
    }

    @Test
    fun trialSummaryNeverReplacesRetentionAndContinueFreeRemainsAvailable() {
        show {
            TrialEndedDialog(
                false,
                "Test summary",
                {},
                {},
                quote,
                BillingProductStatus.Available,
                false,
                {},
                {},
                null,
            )
        }
        compose.onNodeWithText("Test summary").assertExists()
        compose.onNodeWithText(text(R.string.pro_trial_ended_body)).assertExists()
        compose.onNodeWithText(text(R.string.pro_buy_for_price, price)).assertExists()
        actionIsNotTruncated(R.string.pro_continue_free, scroll = false)
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                KnitToolsTheme { content() }
            }
        }
    }

    private fun text(
        id: Int,
        vararg args: Any?,
    ): String = context.getString(id, *args)

    private fun actionIsNotTruncated(
        id: Int,
        scroll: Boolean = true,
    ) {
        val node = compose.onNodeWithText(text(id), useUnmergedTree = true)
        if (scroll) node.performScrollTo()
        val results = mutableListOf<TextLayoutResult>()
        node
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        assertTrue("Missing text layout for $language/$id", results.isNotEmpty())
        assertFalse("Truncated action for $language/$id", results.any { it.hasVisualOverflow })
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages() = listOf("en", "fi", "sv", "de", "fr", "es", "pt", "pt-BR", "it", "nb", "da", "nl")
    }
}
