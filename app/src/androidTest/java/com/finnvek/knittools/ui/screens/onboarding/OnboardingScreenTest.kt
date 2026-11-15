package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.ui.theme.OnboardingDimens
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

    @Test fun leavingCraftCancelsDelayedAdvance() {
        draft.value = draft.value.copy(step = OnboardingStep.Craft)
        var advances = 0
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                KnitToolsTheme {
                    // AnimatedContent säilyttää poistuvan vaiheen hetken sommittelussa.
                    JourneyCraft(
                        draft.value,
                        false,
                        OnboardingActions(
                            edit = { draft.value = it(draft.value) },
                            next = {
                                advances++
                                draft.value = draft.value.next()
                            },
                        ),
                    )
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("onboarding_craft_knitting").performClick()
        compose.runOnIdle { draft.value = draft.value.previous() }
        compose.mainClock.advanceTimeBy(
            OnboardingDimens.CRAFT_ADVANCE_DELAY_MILLIS + OnboardingDimens.STEP_TRANSITION_MILLIS,
        )
        compose.runOnIdle {
            assertEquals(0, advances)
            assertEquals(OnboardingStep.Welcome, draft.value.step)
        }
    }

    @Test fun priceActionsRemainReachableInShortWindows() {
        val windowHeight = mutableStateOf(240.dp)
        val clicked = mutableListOf<String>()
        val tags = listOf("onboarding_start_trial", "onboarding_buy_inline", "onboarding_continue_free")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration) {
                KnitToolsTheme {
                    Box(Modifier.height(windowHeight.value)) {
                        OnboardingScreen(
                            draft = OnboardingDraft("price", step = OnboardingStep.Price),
                            price =
                                OnboardingPriceState(
                                    ready = true,
                                    offer = quote,
                                    productStatus = BillingProductStatus.Available,
                                ),
                            busy = false,
                            messageRes = null,
                            actions =
                                OnboardingActions(
                                    startTrial = { clicked += "trial" },
                                    purchase = { clicked += "purchase" },
                                    finish = { clicked += "free" },
                                ),
                        )
                    }
                }
            }
        }
        listOf(240.dp, 600.dp).forEach { height ->
            compose.runOnIdle { windowHeight.value = height }
            tags.forEach { tag ->
                if (height == 600.dp) compose.onNodeWithTag(tag).assertIsDisplayed()
                compose
                    .onNodeWithTag(tag)
                    .performScrollTo()
                    .assertIsDisplayed()
                    .performClick()
            }
        }
        compose.runOnIdle { assertEquals(List(2) { listOf("trial", "purchase", "free") }.flatten(), clicked) }
    }

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
                        readyProject = CounterProject(id = 1L, name = "Committed project", count = 4),
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
            intactText(R.string.journey_craft_title)
            compose.onNodeWithTag("onboarding_next").assertDoesNotExist()
            capture("craft", isDark)
            compose.onNodeWithTag("onboarding_craft_knitting").performClick()
            // Laatta etenee lyhyen viiveen jälkeen; testikello ei ohita viivettä itsestään.
            compose.mainClock.advanceTimeBy(
                OnboardingDimens.CRAFT_ADVANCE_DELAY_MILLIS + OnboardingDimens.STEP_TRANSITION_MILLIS,
            )
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(text(R.string.journey_needs_title)).fetchSemanticsNodes().isNotEmpty()
            }
            intactText(R.string.journey_needs_title)
            compose.onNodeWithText(text(R.string.journey_need_yarn)).performScrollTo().performClick()
            intactText(R.string.journey_needs_reaction)
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_counter_title)
            compose.onNodeWithTag("onboarding_demo_undo").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithTag("onboarding_demo_plus").performScrollTo().performClick()
            compose
                .onNodeWithTag("onboarding_demo_undo")
                .performScrollTo()
                .assertIsEnabled()
                .performClick()
            compose.onNodeWithTag("onboarding_demo_count").assertTextEquals("0")
            compose.onNodeWithTag("onboarding_demo_return").assertDoesNotExist()
            compose.onNodeWithTag("onboarding_demo_plus").performScrollTo().performClick()
            compose.onNodeWithTag("onboarding_demo_plus").performScrollTo().performClick()
            compose.onNodeWithTag("onboarding_demo_count").assertTextEquals("2")
            compose.onNodeWithTag("onboarding_demo_return").performScrollTo().assertIsDisplayed()
            capture("counter", isDark)
            compose.onNodeWithTag("onboarding_next").performClick()
            intactText(R.string.journey_toolbox_title)
            // Needs-vaiheessa valittu lanka on ruudukon ensimmäinen ja merkitty valituksi.
            compose
                .onNodeWithTag("onboarding_tool_Yarn")
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsSelected()
            compose.onNodeWithTag("onboarding_tool_Counter").assertIsNotSelected()
            compose.onNodeWithTag("onboarding_tool_Counter").assertExists()
            compose.onNodeWithTag("onboarding_tool_Reminders").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("onboarding_tool_more").performScrollTo().assertIsDisplayed()
            capture("toolbox", isDark)
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
                draft.value = draft.value.copy(step = OnboardingStep.Ready, projectId = 1)
            }
            intactText(R.string.journey_ready_title)
            intactText(R.string.journey_ready_body)
            // Yhteenveto kertoo seuraavan rivin eikä näytä napautettavaa jatka-korttia.
            compose.onNodeWithText("Committed project").assertExists()
            compose.onNodeWithText(context.getString(R.string.project_start_count_next_rows, 5)).assertExists()
            compose.onNodeWithTag("onboarding_back").assertDoesNotExist()
            capture("ready", isDark)
            compose.onNodeWithTag("onboarding_next").assertIsEnabled().performClick()
            compose.runOnIdle {
                assertEquals(OnboardingStep.Price, draft.value.step)
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
            compose
                .onNodeWithTag("onboarding_buy_inline")
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsEnabled()
            intactText(R.string.journey_today_body)
            capture("price", isDark)
            compose
                .onNodeWithTag("onboarding_buy_inline")
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsEnabled()
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
