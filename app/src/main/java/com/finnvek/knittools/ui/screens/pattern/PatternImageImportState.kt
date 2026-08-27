package com.finnvek.knittools.ui.screens.pattern

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object PatternImageImportLimits {
    const val MAX_PAGES = 20
    const val MAX_BYTES_PER_IMAGE = 25L * 1024L * 1024L
    const val MAX_TOTAL_BYTES = 200L * 1024L * 1024L
    const val MAX_LONG_EDGE_PIXELS = 1800
    const val MIN_FREE_SPACE_RESERVE_BYTES = 32L * 1024L * 1024L
}

@Serializable
internal data class StagedPatternPage(
    val id: String,
    val sourceUri: String,
    val stagedPath: String,
    val byteCount: Long,
    val width: Int,
    val height: Int,
)

internal data class PatternImageSelection(
    val pages: List<StagedPatternPage> = emptyList(),
) {
    val totalBytes: Long = pages.sumOf(StagedPatternPage::byteCount)

    fun append(batch: List<StagedPatternPage>): PatternImageAppendResult {
        if (batch.any { it.byteCount > PatternImageImportLimits.MAX_BYTES_PER_IMAGE }) {
            return PatternImageAppendResult.ImageBytesExceeded
        }
        val seen = pages.mapTo(linkedSetOf(), StagedPatternPage::sourceUri)
        var duplicatesIgnored = 0
        val uniqueBatch =
            batch.filter { page ->
                if (seen.add(page.sourceUri)) {
                    true
                } else {
                    duplicatesIgnored += 1
                    false
                }
            }
        if (pages.size + uniqueBatch.size > PatternImageImportLimits.MAX_PAGES) {
            return PatternImageAppendResult.PageLimitExceeded
        }
        if (totalBytes + uniqueBatch.sumOf(StagedPatternPage::byteCount) > PatternImageImportLimits.MAX_TOTAL_BYTES) {
            return PatternImageAppendResult.TotalBytesExceeded
        }
        return PatternImageAppendResult.Accepted(
            selection = copy(pages = pages + uniqueBatch),
            duplicatesIgnored = duplicatesIgnored,
        )
    }

    fun canMoveEarlier(index: Int): Boolean = index in 1 until pages.size

    fun canMoveLater(index: Int): Boolean = index in 0 until pages.lastIndex

    fun moveEarlier(index: Int): PatternImageSelection =
        if (canMoveEarlier(index)) copy(pages = pages.swap(index, index - 1)) else this

    fun moveLater(index: Int): PatternImageSelection =
        if (canMoveLater(index)) copy(pages = pages.swap(index, index + 1)) else this

    fun remove(pageId: String): PatternImageSelection = copy(pages = pages.filterNot { it.id == pageId })
}

internal sealed interface PatternImageAppendResult {
    data class Accepted(
        val selection: PatternImageSelection,
        val duplicatesIgnored: Int,
    ) : PatternImageAppendResult

    data object PageLimitExceeded : PatternImageAppendResult

    data object ImageBytesExceeded : PatternImageAppendResult

    data object TotalBytesExceeded : PatternImageAppendResult
}

internal enum class PatternImageImportPhase {
    IDLE,
    STAGING,
    READY,
    CONVERTING,
    ATTACHING,
    SUCCESS,
    CANCELLED,
    ERROR,
}

internal enum class PatternImageImportOrigin {
    GALLERY,
    CAMERA,
}

internal enum class PatternImageImportError {
    PAGE_LIMIT,
    IMAGE_TOO_LARGE,
    TOTAL_TOO_LARGE,
    UNSUPPORTED,
    UNREADABLE,
    ANIMATED,
    LOW_STORAGE,
    STAGING,
    CONVERSION,
    ATTACHMENT,
}

internal data class PatternImageProgress(
    val currentPage: Int,
    val totalPages: Int,
)

internal data class PatternImageImportUiState(
    val projectId: Long? = null,
    val sessionId: String? = null,
    val origin: PatternImageImportOrigin = PatternImageImportOrigin.GALLERY,
    val selection: PatternImageSelection = PatternImageSelection(),
    val phase: PatternImageImportPhase = PatternImageImportPhase.READY,
    val progress: PatternImageProgress? = null,
    val error: PatternImageImportError? = null,
    val duplicatesIgnored: Int = 0,
    val pickerRequestId: String? = null,
    val replacementConfirmationPending: Boolean = false,
    val closeReady: Boolean = false,
    val invalidPageIds: Set<String> = emptySet(),
) {
    val isBusy: Boolean
        get() =
            phase == PatternImageImportPhase.STAGING ||
                phase == PatternImageImportPhase.CONVERTING ||
                phase == PatternImageImportPhase.ATTACHING

    fun beginConversion(): PatternImageImportUiState? =
        takeIf { !isBusy && selection.pages.isNotEmpty() && invalidPageIds.isEmpty() }
            ?.copy(
                phase = PatternImageImportPhase.CONVERTING,
                progress = null,
                error = null,
                replacementConfirmationPending = false,
            )

    fun withProgress(
        currentPage: Int,
        totalPages: Int,
    ): PatternImageImportUiState =
        copy(
            progress =
                PatternImageProgress(
                    currentPage = currentPage.coerceIn(0, totalPages.coerceAtLeast(0)),
                    totalPages = totalPages.coerceAtLeast(0),
                ),
        )
}

internal fun encodeStagedPatternPages(pages: List<StagedPatternPage>): String = Json.encodeToString(pages)

internal fun decodeStagedPatternPages(encoded: String): List<StagedPatternPage> =
    runCatching { Json.decodeFromString<List<StagedPatternPage>>(encoded) }.getOrDefault(emptyList())

private fun <T> List<T>.swap(
    first: Int,
    second: Int,
): List<T> =
    toMutableList().apply {
        val value = this[first]
        this[first] = this[second]
        this[second] = value
    }
