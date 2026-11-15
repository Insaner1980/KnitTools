package com.finnvek.knittools.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.parseWebPatternSharedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PatternShareCoordinatorViewModelTest {
    @Test
    fun `wrongly typed pending id is discarded and a new share is accepted`() {
        val model =
            PatternShareCoordinatorViewModel(
                SavedStateHandle(mapOf("pattern_share_pending_request_id" to "not-a-long")),
            )

        assertNull(model.pending.value)
        val payload = PatternSharePayload.WebLink("https://example.com/pattern", "Pattern")
        val offered = model.offer(payload) as PatternShareOfferResult.Accepted
        assertEquals(1L, offered.request.requestId)
        assertEquals(payload, offered.request.payload)
    }

    @Test
    fun `wrongly typed restored payload fields do not interrupt restoration`() {
        val valid =
            mapOf<String, Any>(
                "pattern_share_pending_request_id" to 1L,
                "pattern_share_pending_kind" to "web",
                "pattern_share_pending_url" to "https://example.com/pattern",
                "pattern_share_pending_title" to "Pattern",
            )
        for (key in listOf("pattern_share_pending_kind", "pattern_share_pending_url")) {
            val model = PatternShareCoordinatorViewModel(SavedStateHandle(valid + (key to 7L)))
            assertNull(key, model.pending.value)
        }
        val missingTitle =
            PatternShareCoordinatorViewModel(
                SavedStateHandle(valid + ("pattern_share_pending_title" to 7L)),
            )
        assertEquals(
            "",
            missingTitle.pending.value
                ?.payload
                ?.titleSuggestion,
        )
        val malformedError =
            PatternShareCoordinatorViewModel(
                SavedStateHandle(
                    valid + mapOf("pattern_share_pending_kind" to "error", "pattern_share_pending_error" to 7L),
                ),
            )
        assertNull(malformedError.pending.value)
    }

    @Test
    fun `wrongly typed queued id does not prevent acknowledging the pending share`() {
        val handle = SavedStateHandle()
        val model = PatternShareCoordinatorViewModel(handle)
        val payload = PatternSharePayload.WebLink("https://example.com/pattern", "Pattern")
        val request = (model.offer(payload) as PatternShareOfferResult.Accepted).request
        handle["pattern_share_queued_request_id"] = "not-a-long"

        model.acknowledge(request.requestId)

        assertNull(model.pending.value)
        assertEquals(2L, (model.offer(payload) as PatternShareOfferResult.Accepted).request.requestId)
    }

    @Test
    fun `wrongly typed next id does not prevent offering a share`() {
        val model =
            PatternShareCoordinatorViewModel(
                SavedStateHandle(mapOf("pattern_share_next_request_id" to "not-a-long")),
            )
        val payload = PatternSharePayload.WebLink("https://example.com/pattern", "Pattern")

        val request = (model.offer(payload) as PatternShareOfferResult.Accepted).request

        assertEquals(1L, request.requestId)
        assertEquals(payload, model.pending.value?.payload)
    }

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
                "https://www.ravelry\u3002com/patterns/library/cozy-hat",
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
    fun `identical deliberate shares keep separate identities across restoration`() {
        val handle = SavedStateHandle()
        val model = PatternShareCoordinatorViewModel(handle)
        val payload =
            PatternSharePayload.Ravelry(
                url = "https://www.ravelry.com/patterns/library/cozy-hat",
                titleSuggestion = "Cozy hat",
            )

        val first = (model.offer(payload) as PatternShareOfferResult.Accepted).request
        val second = (model.offer(payload) as PatternShareOfferResult.Queued).request
        val restored = PatternShareCoordinatorViewModel(handle.restoredCopy())

        assertEquals(first.requestId + 1L, second.requestId)
        assertEquals(first, restored.pending.value)
        restored.acknowledge(first.requestId)
        restored.acknowledge(first.requestId)
        assertEquals(second, restored.pending.value)
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
