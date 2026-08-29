package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ProjectYarnUsage
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageSource
import com.finnvek.knittools.domain.model.YarnUsageSourceStatus
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.repository.ProjectYarnUsageRepository
import com.finnvek.knittools.repository.YarnUsageResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectYarnUsageViewModelTest {
    private val repository = mockk<ProjectYarnUsageRepository>()
    private val preferences = mockk<PreferencesManager>()
    private val preferenceState = MutableStateFlow(AppPreferences())
    private val note = ProjectYarnUsageItem(YarnUsageSource(projectYarnNoteId = 2), "Mohair")
    private val card = ProjectYarnUsageItem(YarnUsageSource(yarnCardId = 3), "Wool")
    private val rows = MutableStateFlow<List<ProjectYarnUsageItem>?>(listOf(note, card))
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeForProject(any()) } returns rows
        every { preferences.preferences } returns preferenceState
    }

    @After
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle()): ProjectYarnUsageViewModel =
        ProjectYarnUsageViewModel(repository, preferences, handle).also {
            stores.add(ViewModelStore().apply { put("usage", it) })
            it.observe(1)
        }

    private fun ProjectYarnUsageViewModel.edit(
        field: YarnUsageField,
        text: String,
    ) = edit(field, text, Locale.forLanguageTag("fi"))

    private fun ProjectYarnUsageViewModel.draft(): YarnUsageDraft = requireNotNull(editor.value.draft)

    private fun usage(source: YarnUsageSource = note.source): ProjectYarnUsage =
        ProjectYarnUsage(
            4,
            1,
            source,
            "Original name",
            YarnUsageAmounts(allocatedMeters = 600.0, usedMeters = 350.0),
            10,
            20,
        )

    @Test
    fun `note and card open independently and preference only seeds the unit`() =
        runTest {
            val model = model()
            model.open(note, note.name)
            assertEquals(note.source, model.draft().source)
            model.edit(YarnUsageField.PLANNED, "12")
            preferenceState.value = AppPreferences(useImperial = true)
            model.open(card, card.name)
            assertEquals(card.source, model.draft().source)
            assertEquals("", model.draft().planned.text)
            assertEquals(YarnUsageUnit.YARDS, model.draft().unit)
            model.unit(YarnUsageUnit.METERS)
            assertTrue(preferenceState.value.useImperial)
        }

    @Test
    fun `logical pair and source-less row keep one persisted identity`() =
        runTest {
            val pair = note.copy(source = YarnUsageSource(3, 2), usage = usage(YarnUsageSource(3, 2)))
            rows.value = listOf(pair)
            val model = model()
            model.open(pair, pair.name)
            assertEquals(4L, model.draft().usageId)
            assertEquals(YarnUsageSource(3, 2), model.draft().source)
            val orphan =
                pair.copy(
                    source = YarnUsageSource(),
                    usage = usage(YarnUsageSource()),
                    status = YarnUsageSourceStatus.UNAVAILABLE,
                )
            rows.value = listOf(orphan)
            model.open(orphan, orphan.name)
            assertEquals(4L, model.draft().usageId)
            assertEquals(250.0, model.draft().remaining)
        }

    @Test
    fun `raw comma and point input stays intact while invalid input stays invalid`() =
        runTest {
            val model = model()
            model.open(note, note.name)
            listOf("1,25", "1.25").forEach { text ->
                model.edit(YarnUsageField.USED, text)
                assertEquals(text, model.draft().used.text)
                assertEquals(1.25, model.draft().used.value)
                assertTrue(model.draft().canSave)
            }
            listOf("-2", "1e3", "1,2,3", "1.2.3", "NaN", "Infinity", "1,").forEach { text ->
                model.edit(YarnUsageField.USED, text)
                assertEquals(text, model.draft().used.text)
                assertFalse(model.draft().canSave)
                model.unit(YarnUsageUnit.YARDS)
                assertEquals(YarnUsageUnit.METERS, model.draft().unit)
            }
            model.edit(YarnUsageField.USED, "0")
            assertEquals(0.0, model.draft().used.value)
            assertTrue(model.draft().canSave)
        }

    @Test
    fun `unit changes preserve precision and unknown zero and overage differ`() =
        runTest {
            val model = model()
            model.open(note, note.name)
            model.edit(YarnUsageField.ALLOCATED, "123.12345678901234")
            val canonical = model.draft().allocated.value
            repeat(20) {
                model.unit(YarnUsageUnit.YARDS)
                model.unit(YarnUsageUnit.METERS)
            }
            assertEquals(canonical, model.draft().allocated.value)
            assertNull(model.draft().remaining)
            model.edit(YarnUsageField.ALLOCATED, "0")
            model.edit(YarnUsageField.USED, "0")
            assertEquals(0.0, model.draft().remaining)
            model.edit(YarnUsageField.USED, "2")
            assertEquals(-2.0, model.draft().remaining)
            model.edit(YarnUsageField.ALLOCATED, "")
            assertNull(model.draft().remaining)
        }

    @Test
    fun `grams and skeins require an explicit pair without losing meter amounts`() =
        runTest {
            val model = model()
            model.open(note, note.name)
            model.edit(YarnUsageField.ALLOCATED, "600")
            listOf(YarnUsageUnit.GRAMS, YarnUsageUnit.SKEINS).forEach { target ->
                model.unit(target)
                assertEquals(target, model.draft().pendingUnit)
                assertFalse(model.draft().canSave)
                assertEquals(600.0, model.draft().allocated.value)
            }
            model.edit(YarnUsageField.LENGTH, "200")
            assertFalse(model.draft().canSave)
            model.edit(YarnUsageField.WEIGHT, "0")
            assertFalse(model.draft().canSave)
            model.edit(YarnUsageField.WEIGHT, "100")
            assertEquals(YarnUsageUnit.SKEINS, model.draft().unit)
            assertEquals("3", model.draft().allocated.text)
            model.unit(YarnUsageUnit.GRAMS)
            model.edit(YarnUsageField.USED, "175")
            assertEquals(350.0, model.draft().used.value)
            assertEquals(250.0, model.draft().remaining)
            model.conversion(false)
            assertEquals(YarnUsageUnit.METERS, model.draft().unit)
            assertEquals(600.0, model.draft().allocated.value)
            assertEquals(350.0, model.draft().used.value)
            assertNull(model.draft().amounts.metersPerSkein)
        }

    @Test
    fun `save waits for persistence suppresses repeated taps and clears the restorable draft`() =
        runTest {
            val result = CompletableDeferred<YarnUsageResult>()
            coEvery { repository.create(any(), any(), any(), any()) } coAnswers { result.await() }
            val handle = SavedStateHandle()
            val model = model(handle)
            model.open(note, note.name)
            model.edit(YarnUsageField.USED, "0")
            model.save()
            model.save()
            assertTrue(model.editor.value.busy)
            assertFalse(model.editor.value.completed)
            assertNotNull(handle.restoreYarnUsage())
            result.complete(YarnUsageResult.Created(usage()))
            assertTrue(model.editor.value.completed)
            assertNull(handle.restoreYarnUsage())
            model.save()
            coVerify(exactly = 1) { repository.create(1, note.source, any(), note.name) }
        }

    @Test
    fun `failed saves and stale sources preserve the draft`() =
        runTest {
            val model = model()
            model.open(note, note.name)
            model.edit(YarnUsageField.PLANNED, "12,34")
            val failures =
                listOf(
                    YarnUsageResult.PersistenceFailure,
                    YarnUsageResult.SourceMissing,
                    YarnUsageResult.ProjectMissing,
                    YarnUsageResult.AlreadyExists(usage()),
                )
            failures.forEach { failure ->
                coEvery { repository.create(any(), any(), any(), any()) } returns failure
                model.save()
                assertEquals("12,34", model.draft().planned.text)
                assertEquals(failure, model.editor.value.error)
                assertFalse(model.editor.value.busy)
            }
        }

    @Test
    fun `existing edits use the revision and failed deletes retain the summary`() =
        runTest {
            val existing = note.copy(usage = usage())
            rows.value = listOf(existing)
            val model = model()
            model.open(existing, note.name)
            coEvery { repository.update(any(), any(), any(), any()) } returns YarnUsageResult.PersistenceFailure
            model.edit(YarnUsageField.PLANNED, "0")
            model.save()
            coVerify { repository.update(1, 4, 20, any()) }
            assertEquals(4L, model.draft().usageId)
            coEvery { repository.delete(1, 4, 20) } returns YarnUsageResult.PersistenceFailure
            model.delete()
            assertNotNull(model.editor.value.error)
            assertNotNull(model.editor.value.draft)
            coEvery { repository.delete(1, 4, 20) } returns YarnUsageResult.Deleted
            model.delete()
            assertTrue(model.editor.value.completed)
            model.delete()
            coVerify(exactly = 2) { repository.delete(1, 4, 20) }
        }

    @Test
    fun `recreation restores raw draft precision and pending conversion`() =
        runTest {
            val handle = SavedStateHandle()
            val model = model(handle)
            model.open(note, note.name)
            model.edit(YarnUsageField.ALLOCATED, "123,123456789")
            model.unit(YarnUsageUnit.GRAMS)
            model.edit(YarnUsageField.LENGTH, "200,")
            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            val restored = model(restoredHandle)
            assertEquals(model.draft(), restored.draft())
            restored.closeDraft()
            assertNull(restoredHandle.restoreYarnUsage())
            assertNull(restored.editor.value.draft)
        }
}
