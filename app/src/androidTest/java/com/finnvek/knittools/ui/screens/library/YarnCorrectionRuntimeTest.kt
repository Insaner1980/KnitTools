package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.App
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.setTestProStatus
import com.finnvek.knittools.ui.TestContentSplashRule
import com.finnvek.knittools.ui.screens.yarncard.YarnCardDetailActions
import com.finnvek.knittools.ui.screens.yarncard.YarnCardDetailScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class YarnCorrectionRuntimeTest {
    @get:Rule(order = 0)
    val splash = TestContentSplashRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()
    private val app: App = ApplicationProvider.getApplicationContext()
    private val prefix = "RW03B-${UUID.randomUUID()}"
    private var saveables = SaveableStateRegistry(null) { true }
    private lateinit var library: LibraryViewModel

    @Before fun setUp() {
        check(app.packageName == "com.finnvek.knittools.activepolicytest")
        policy(ProStatus.PRO_PURCHASED)
        rule.runOnUiThread {
            rule.activity.setContent { }
            library = freshLibrary()
        }
    }

    @After fun tearDown() {
        rule.runOnUiThread {
            rule.activity.setContent { }
            rule.activity.viewModelStore.clear()
        }
        runBlocking(Dispatchers.IO) {
            rows().forEach {
                app.database
                    .get()
                    .yarnCardDao()
                    .delete(it.id)
            }
        }
    }

    @Test fun creationRequiresPositiveQuantityAndWritesTheEnteredValue() {
        mountLibrary()
        click(R.string.add_yarn_to_my_yarn)
        fields()[0].performTextReplacement(prefix)
        fields()[2].performTextReplacement("0")
        node(R.string.save).assertIsNotEnabled()
        assertTrue(rows().isEmpty())
        fields()[2].performTextReplacement("3")
        node(R.string.save).performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(5_000) { rows().size == 1 }
        assertEquals(3, rows().single().quantityInStash)
    }

    @Test fun metadataEditorHidesQuantityAndPreservesZeroAndThreeInRoom() {
        for (quantity in listOf(0, 3)) {
            val id =
                runBlocking(Dispatchers.IO) {
                    app.database.get().yarnCardDao().upsert(
                        YarnCardEntity(
                            yarnName = "$prefix-$quantity",
                            quantityInStash = quantity,
                            brand = "Original",
                            colorName = "Blue",
                        ),
                    )
                }
            val before = rows().single { it.id == id }
            rule.runOnUiThread {
                val vm = ViewModelProvider(rule.activity)["$prefix-$quantity", YarnCardViewModel::class.java]
                vm.loadFromCard(before.toDomain())
                rule.activity.setContent { KnitToolsTheme { YarnCardDetailScreen({ vm }, YarnCardDetailActions({})) } }
            }
            node(R.string.project_overview_edit).performScrollTo().performClick()
            rule.onNode(hasSetTextAction() and hasText(label(R.string.quantity_label))).assertDoesNotExist()
            val editedName = "$prefix-edited-$quantity"
            // Set the field through its accessibility action without starting the platform IME.
            rule.onNode(hasSetTextAction() and hasText(before.yarnName))
                .assertIsEnabled()
                .performSemanticsAction(SemanticsActions.SetText) { assertTrue(it(AnnotatedString(editedName))) }
            rule.onNode(hasSetTextAction() and hasText(editedName)).assertIsDisplayed()
            node(R.string.save)
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()
            node(R.string.edit_yarn_details).assertDoesNotExist()
            rule.waitUntil(5_000) {
                rows().single { it.id == id }.yarnName == editedName
            }
            assertEquals(before.copy(yarnName = editedName), rows().single { it.id == id })
            rule.runOnUiThread { rule.activity.setContent { } }
            rule.waitForIdle()
        }
    }

    @Test fun restoredDraftWithFreshLibraryOwnerStaysOpenUntilExplicitSave() {
        mountLibrary()
        click(R.string.add_yarn_to_my_yarn)
        fields()[0].performTextReplacement(prefix)
        fields()[1].performTextReplacement("Restored brand")
        fields()[2].performTextReplacement("3")
        policy(ProStatus.TRIAL_NOT_STARTED)
        node(R.string.save).performScrollTo().performClick()
        node(R.string.pro_start_free_trial).assertIsDisplayed()
        assertTrue(rows().isEmpty())
        rule.runOnUiThread {
            val saved = saveables.performSave()
            rule.activity.setContent { }
            saveables = SaveableStateRegistry(saved) { true }
            library = freshLibrary()
        }
        rule.waitForIdle()
        mountLibrary()
        policy(ProStatus.PRO_PURCHASED)
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText(label(R.string.pro_start_free_trial)).fetchSemanticsNodes().isEmpty()
        }
        rule.waitForIdle()
        fields()[0].assertTextContains(prefix)
        fields()[1].assertTextContains("Restored brand")
        fields()[2].assertTextContains("3")
        assertTrue(rows().isEmpty())
        rule.runOnUiThread { assertFalse(library.retryCreateManualYarnCard()) }
        node(R.string.save).performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(5_000) { rows().size == 1 }
        assertEquals(3, rows().single().quantityInStash)
        assertEquals("Restored brand", rows().single().brand)
        rule.runOnUiThread { assertFalse(library.retryCreateManualYarnCard()) }
        assertEquals(1, rows().size)
    }

    private fun mountLibrary() {
        rule.runOnUiThread {
            rule.activity.setContent {
                CompositionLocalProvider(LocalSaveableStateRegistry provides saveables) {
                    val canCreate by library.canUseYarnCards.collectAsState()
                    val pro by library.proState.collectAsState()
                    val proAccessReady by library.proAccessReady.collectAsState()
                    KnitToolsTheme {
                        MyYarnScreen(
                            MyYarnState(emptyList(), emptyMap(), false, emptySet(), canCreate, pro, proAccessReady),
                            MyYarnActions(
                                {},
                                library::createManualYarnCard,
                                library::retryCreateManualYarnCard,
                                {},
                                {},
                                {},
                                {},
                                {},
                                {},
                                {},
                            ),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun freshLibrary() =
        ViewModelProvider(rule.activity)["$prefix-${UUID.randomUUID()}", LibraryViewModel::class.java]

    private fun policy(status: ProStatus) {
        setTestProStatus(status)
        runBlocking {
            withTimeout(5_000) {
                app.proManager
                    .get()
                    .proState
                    .first { it.status == status }
            }
        }
    }

    private fun rows() =
        runBlocking(Dispatchers.IO) {
            app.database
                .get()
                .yarnCardDao()
                .getAllCards()
                .first()
                .filter { it.yarnName.startsWith(prefix) }
        }

    private fun fields() = rule.onAllNodes(hasSetTextAction())

    private fun label(id: Int) = rule.activity.getString(id)

    private fun node(id: Int) = rule.onNodeWithText(label(id))

    private fun click(id: Int) = rule.onNodeWithContentDescription(label(id)).performClick()
}
