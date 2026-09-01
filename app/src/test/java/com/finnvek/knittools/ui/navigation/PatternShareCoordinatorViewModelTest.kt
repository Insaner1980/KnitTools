package com.finnvek.knittools.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.parseWebPatternSharedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PatternShareCoordinatorViewModelTest {
    @Test
    fun `share parser outcomes keep Ravelry ownership and typed local errors`() {
        assertEquals(
            PatternSharePayload.WebLink("https://example.com/pattern", "Cardigan"),
            parseWebPatternSharedText("https://example.com/pattern", "Cardigan").toPatternSharePayload(),
        )
        assertEquals(
            PatternSharePayload.Ravelry(
                "https://www.ravelry.com/patterns/library/cozy-hat",
                "",
            ),
            parseWebPatternSharedText(
                "https://www.ravelry.com/patterns/library/cozy-hat",
                null,
            ).toPatternSharePayload(),
        )
        assertEquals(
            PatternSharePayload.Error(PatternShareError.Ambiguous),
            parseWebPatternSharedText(
                "https://example.com/one https://example.com/two",
                null,
            ).toPatternSharePayload(),
        )
    }

    @Test
    fun `pending web request restores with its stable id and is acknowledged once`() {
        val handle = SavedStateHandle()
        val model = PatternShareCoordinatorViewModel(handle)

        val offered =
            model.offer(
                PatternSharePayload.WebLink(
                    url = "https://example.com/pattern",
                    titleSuggestion = "Cardigan",
                ),
            )
        val request = requireNotNull((offered as PatternShareOfferResult.Accepted).request)

        val restoredHandle = handle.restoredCopy()
        val restored = PatternShareCoordinatorViewModel(restoredHandle)
        assertEquals(request, restored.pending.value)

        restored.acknowledge(request.requestId + 1)
        assertNotNull(restored.pending.value)

        restored.acknowledge(request.requestId)
        assertNull(restored.pending.value)

        restored.acknowledge(request.requestId)
        assertNull(restored.pending.value)
        assertNull(PatternShareCoordinatorViewModel(restoredHandle.restoredCopy()).pending.value)
    }

    @Test
    fun `replayed source intent keeps the pending request identity`() {
        val model = PatternShareCoordinatorViewModel(SavedStateHandle())
        val payload =
            PatternSharePayload.Ravelry(
                url = "https://www.ravelry.com/patterns/library/cozy-hat",
                titleSuggestion = "Cozy hat",
            )

        val first = (model.offer(payload) as PatternShareOfferResult.Accepted).request
        val replay = (model.offer(payload) as PatternShareOfferResult.Accepted).request

        assertEquals(first, replay)
        assertEquals(first.requestId, model.pending.value?.requestId)
    }

    @Test
    fun `ambiguous share error survives restoration until the editor acknowledges it`() {
        val handle = SavedStateHandle()
        val model = PatternShareCoordinatorViewModel(handle)
        val request =
            (
                model.offer(PatternSharePayload.Error(PatternShareError.Ambiguous)) as
                    PatternShareOfferResult.Accepted
            ).request

        val restored = PatternShareCoordinatorViewModel(handle.restoredCopy())

        assertEquals(request, restored.pending.value)
        restored.acknowledge(request.requestId)
        assertNull(restored.pending.value)
    }

    @Test
    fun `a second intent remains queued until the first handoff completes`() {
        val model = PatternShareCoordinatorViewModel(SavedStateHandle())
        val first =
            (
                model.offer(PatternSharePayload.WebLink("https://example.com/one", "One")) as
                    PatternShareOfferResult.Accepted
            ).request

        val second =
            (
                model.offer(PatternSharePayload.WebLink("https://example.com/two", "Two")) as
                    PatternShareOfferResult.Queued
            ).request

        assertEquals(first, model.pending.value)

        model.acknowledge(first.requestId)

        assertEquals(second, model.pending.value)
    }

    private fun SavedStateHandle.restoredCopy(): SavedStateHandle =
        SavedStateHandle(keys().associateWith { key -> get<Any?>(key) })
}
