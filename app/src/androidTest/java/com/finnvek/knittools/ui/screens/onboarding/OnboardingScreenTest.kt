package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OnboardingScreenTest(
    private val language: String,
) {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.localizedTestContext(language)
    private val configuration = context.resources.configuration
    private val draft = mutableStateOf(OnboardingDraft("test"))
    private val dark = mutableStateOf(false)
    private val price = mutableStateOf(OnboardingPriceState(ready = true))
    private var created = 0
    private var finished = 0
    private val quote = SelectedOneTimeOffer("9,99 €", "test-offer", 9_990_000, "EUR")

    @Test fun allStepsAndPriceActionsUseLocalizedCopyInBothThemes() {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                KnitToolsTheme(isDarkTheme = dark.value) {
                    OnboardingScreen(
                        draft.value,
                        price.value,
                        false,
                        null,
                        OnboardingActions(
                            edit = { draft.value = it(draft.value) },
                            next = { draft.value = draft.value.next() },
                            previous = { draft.value = draft.value.previous() },
                            skip = { draft.value = draft.value.copy(step = OnboardingStep.Price) },
                            create = { created++ },
                            finish = { finished++ },
                        ),
                    )
                }
            }
        }
        listOf(false, true).forEach { isDark ->
            compose.runOnIdle {
                dark.value = isDark
                draft.value = OnboardingDraft("test")
                price.value = OnboardingPriceState(ready = true)
            }
            intactText(R.string.journey_welcome_title)
            capture("welcome", isDark)
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_needs_title)
            compose.onNodeWithText(text(R.string.journey_need_yarn)).performScrollTo().performClick()
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_counter_title)
            compose.onNodeWithTag("onboarding_demo_undo").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("onboarding_demo_plus").performScrollTo().performClick()
            compose
                .onNodeWithTag("onboarding_demo_undo")
                .performScrollTo()
                .assertIsEnabled()
                .performClick()
            compose.onNodeWithTag("onboarding_demo_count").assertTextEquals("47")
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_benefits_title)
            intactText(R.string.journey_yarn_benefit)
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_project_title)
            compose.onNodeWithTag("onboarding_project_name").assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                    androidx.compose.ui.text
                        .AnnotatedString(""),
                ),
            )
            compose
                .onNodeWithText(
                    context.getString(R.string.journey_name_placeholder, text(R.string.journey_example_name)),
                ).assertExists()
            compose.onNodeWithTag("onboarding_next").assertIsNotEnabled()
            capture("project", isDark)
            compose.onNodeWithTag("onboarding_project_name").performTextInput("My project")
            compose.runOnIdle { draft.value = draft.value.copy(targetRows = "100000", showOptions = false) }
            compose.onNodeWithTag("onboarding_project_target").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("onboarding_next").assertIsNotEnabled()
            compose.runOnIdle { draft.value = draft.value.copy(targetRows = "") }
            compose.onNodeWithTag("onboarding_next").assertIsEnabled().performClick()
            compose.runOnIdle {
                assertEquals(if (isDark) 2 else 1, created)
                draft.value = draft.value.copy(step = OnboardingStep.Price)
                price.value =
                    OnboardingPriceState(ready = false, offer = quote, productStatus = BillingProductStatus.Available)
            }
            compose.onNodeWithTag("onboarding_loading_action").assertIsNotEnabled()
            compose.onNodeWithText(quote.formattedPrice).assertDoesNotExist()
            compose.onNodeWithTag("onboarding_continue_free").assertIsEnabled()
            compose.runOnIdle {
                price.value =
                    OnboardingPriceState(ready = true, offer = quote, productStatus = BillingProductStatus.Available)
            }
            intactText(R.string.journey_trial_title)
            compose.onNodeWithTag("onboarding_start_trial").assertIsDisplayed().assertIsEnabled()
            intactText(R.string.journey_today_body)
            capture("price", isDark)
            compose.onNodeWithTag("onboarding_buy_inline").performScrollTo().assertIsEnabled()
            compose.runOnIdle {
                price.value = price.value.copy(pro = ProState(status = ProStatus.TRIAL_EXPIRED))
            }
            compose.onNodeWithTag("onboarding_buy_primary").assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithTag("onboarding_start_trial").assertDoesNotExist()
            compose.onNodeWithTag("onboarding_buy_inline").assertDoesNotExist()
            compose.runOnIdle { price.value = price.value.copy(pending = true) }
            compose.onNodeWithTag("onboarding_buy_primary").assertIsNotEnabled()
            compose.onNodeWithTag("onboarding_continue_free").assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals(if (isDark) 2 else 1, finished) }
        }
    }

    private fun text(id: Int) = context.getString(id)

    private fun capture(
        page: String,
        isDark: Boolean,
    ) {
        if (language != "fi") return
        val directory = java.io.File(context.cacheDir, "onboarding-check").apply { mkdirs() }
        val output = java.io.File(directory, "android-fi-$page-${if (isDark) "dark" else "light"}.png")
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(
                android.graphics.Bitmap.CompressFormat.PNG,
                100,
                it,
            )
        }
    }

    private fun intactText(id: Int) {
        val node = compose.onNodeWithText(text(id)).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Missing text layout: $language/$id", layouts.isNotEmpty())
        if (layouts.any { it.hasVisualOverflow }) capture("overflow-$id", dark.value)
        assertFalse(
            "Clipped text: $language/$id " +
                layouts.joinToString {
                    "size=${it.size}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}, " +
                        "lines=${it.lineCount}, lastBottom=${it.getLineBottom(it.lineCount - 1)}, " +
                        "lastRight=${it.getLineRight(it.lineCount - 1)}"
                },
            layouts.any { it.hasVisualOverflow },
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages() = listOf("en", "da", "de", "es", "fi", "fr", "it", "nb", "nl", "pt", "pt-BR", "sv")
    }
}
