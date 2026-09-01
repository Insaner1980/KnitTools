package com.finnvek.knittools.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.finnvek.knittools.domain.model.WebPatternShareParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface PatternSharePayload {
    val url: String
    val titleSuggestion: String

    data class WebLink(
        override val url: String,
        override val titleSuggestion: String,
    ) : PatternSharePayload

    data class Ravelry(
        override val url: String,
        override val titleSuggestion: String,
    ) : PatternSharePayload

    data class Error(
        val error: PatternShareError,
    ) : PatternSharePayload {
        override val url: String = ""
        override val titleSuggestion: String = ""
    }
}

enum class PatternShareError {
    Empty,
    Invalid,
    Ambiguous,
    TooLong,
}

internal fun WebPatternShareParseResult.toPatternSharePayload(): PatternSharePayload =
    when (this) {
        is WebPatternShareParseResult.WebLink -> PatternSharePayload.WebLink(url.originalUrl, titleSuggestion)
        is WebPatternShareParseResult.Ravelry -> PatternSharePayload.Ravelry(url.originalUrl, titleSuggestion)
        WebPatternShareParseResult.Empty -> PatternSharePayload.Error(PatternShareError.Empty)
        WebPatternShareParseResult.Invalid -> PatternSharePayload.Error(PatternShareError.Invalid)
        WebPatternShareParseResult.Ambiguous -> PatternSharePayload.Error(PatternShareError.Ambiguous)
        WebPatternShareParseResult.TooLong -> PatternSharePayload.Error(PatternShareError.TooLong)
    }

data class PatternShareImportRequest(
    val requestId: Long,
    val payload: PatternSharePayload,
)

sealed interface PatternShareOfferResult {
    data class Accepted(
        val request: PatternShareImportRequest,
    ) : PatternShareOfferResult

    data class Queued(
        val request: PatternShareImportRequest,
    ) : PatternShareOfferResult

    data object Busy : PatternShareOfferResult
}

@HiltViewModel
class PatternShareCoordinatorViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val mutablePending = MutableStateFlow(savedStateHandle.restorePendingShareRequest())
        val pending = mutablePending.asStateFlow()

        fun offer(payload: PatternSharePayload): PatternShareOfferResult {
            mutablePending.value?.let { current ->
                if (current.payload == payload) {
                    return PatternShareOfferResult.Accepted(current)
                }
                savedStateHandle.restoreQueuedShareRequest()?.let { queued ->
                    return if (queued.payload == payload) {
                        PatternShareOfferResult.Queued(queued)
                    } else {
                        PatternShareOfferResult.Busy
                    }
                }
                val queued = nextRequest(payload)
                savedStateHandle.saveQueuedShareRequest(queued)
                return PatternShareOfferResult.Queued(queued)
            }

            val request = nextRequest(payload)
            savedStateHandle.savePendingShareRequest(request)
            mutablePending.value = request
            return PatternShareOfferResult.Accepted(request)
        }

        fun acknowledge(requestId: Long) {
            if (mutablePending.value?.requestId != requestId) return
            val queued = savedStateHandle.restoreQueuedShareRequest()
            savedStateHandle.saveQueuedShareRequest(null)
            savedStateHandle.savePendingShareRequest(queued)
            mutablePending.value = queued
        }

        private fun nextRequest(payload: PatternSharePayload): PatternShareImportRequest {
            val requestId = (savedStateHandle.get<Long>(KEY_NEXT_REQUEST_ID) ?: 0L) + 1L
            savedStateHandle[KEY_NEXT_REQUEST_ID] = requestId
            return PatternShareImportRequest(requestId, payload)
        }
    }

private const val KEY_NEXT_REQUEST_ID = "pattern_share_next_request_id"
private const val KEY_PENDING_REQUEST_ID = "pattern_share_pending_request_id"
private const val KEY_PENDING_KIND = "pattern_share_pending_kind"
private const val KEY_PENDING_URL = "pattern_share_pending_url"
private const val KEY_PENDING_TITLE = "pattern_share_pending_title"
private const val KEY_PENDING_ERROR = "pattern_share_pending_error"
private const val KEY_QUEUED_REQUEST_ID = "pattern_share_queued_request_id"
private const val KEY_QUEUED_KIND = "pattern_share_queued_kind"
private const val KEY_QUEUED_URL = "pattern_share_queued_url"
private const val KEY_QUEUED_TITLE = "pattern_share_queued_title"
private const val KEY_QUEUED_ERROR = "pattern_share_queued_error"
private const val KIND_WEB = "web"
private const val KIND_RAVELRY = "ravelry"
private const val KIND_ERROR = "error"

private fun SavedStateHandle.savePendingShareRequest(request: PatternShareImportRequest?) {
    if (request == null) {
        remove<Long>(KEY_PENDING_REQUEST_ID)
        remove<String>(KEY_PENDING_KIND)
        remove<String>(KEY_PENDING_URL)
        remove<String>(KEY_PENDING_TITLE)
        remove<String>(KEY_PENDING_ERROR)
        return
    }
    this[KEY_PENDING_REQUEST_ID] = request.requestId
    this[KEY_PENDING_KIND] =
        when (request.payload) {
            is PatternSharePayload.WebLink -> KIND_WEB
            is PatternSharePayload.Ravelry -> KIND_RAVELRY
            is PatternSharePayload.Error -> KIND_ERROR
        }
    this[KEY_PENDING_URL] = request.payload.url
    this[KEY_PENDING_TITLE] = request.payload.titleSuggestion
    this[KEY_PENDING_ERROR] = (request.payload as? PatternSharePayload.Error)?.error?.name
}

private fun SavedStateHandle.restorePendingShareRequest(): PatternShareImportRequest? =
    restoreShareRequest(
        requestIdKey = KEY_PENDING_REQUEST_ID,
        kindKey = KEY_PENDING_KIND,
        urlKey = KEY_PENDING_URL,
        titleKey = KEY_PENDING_TITLE,
        errorKey = KEY_PENDING_ERROR,
    )

private fun SavedStateHandle.saveQueuedShareRequest(request: PatternShareImportRequest?) {
    if (request == null) {
        remove<Long>(KEY_QUEUED_REQUEST_ID)
        remove<String>(KEY_QUEUED_KIND)
        remove<String>(KEY_QUEUED_URL)
        remove<String>(KEY_QUEUED_TITLE)
        remove<String>(KEY_QUEUED_ERROR)
        return
    }
    this[KEY_QUEUED_REQUEST_ID] = request.requestId
    this[KEY_QUEUED_KIND] =
        when (request.payload) {
            is PatternSharePayload.WebLink -> KIND_WEB
            is PatternSharePayload.Ravelry -> KIND_RAVELRY
            is PatternSharePayload.Error -> KIND_ERROR
        }
    this[KEY_QUEUED_URL] = request.payload.url
    this[KEY_QUEUED_TITLE] = request.payload.titleSuggestion
    this[KEY_QUEUED_ERROR] = (request.payload as? PatternSharePayload.Error)?.error?.name
}

private fun SavedStateHandle.restoreQueuedShareRequest(): PatternShareImportRequest? =
    restoreShareRequest(
        requestIdKey = KEY_QUEUED_REQUEST_ID,
        kindKey = KEY_QUEUED_KIND,
        urlKey = KEY_QUEUED_URL,
        titleKey = KEY_QUEUED_TITLE,
        errorKey = KEY_QUEUED_ERROR,
    )

private fun SavedStateHandle.restoreShareRequest(
    requestIdKey: String,
    kindKey: String,
    urlKey: String,
    titleKey: String,
    errorKey: String,
): PatternShareImportRequest? {
    val requestId = get<Long>(requestIdKey)?.takeIf { it > 0L } ?: return null
    val kind = get<String>(kindKey)
    val url = get<String>(urlKey).orEmpty()
    if (kind != KIND_ERROR && url.isBlank()) return null
    val title = get<String>(titleKey).orEmpty()
    val payload =
        when (kind) {
            KIND_WEB -> PatternSharePayload.WebLink(url, title)
            KIND_RAVELRY -> PatternSharePayload.Ravelry(url, title)
            KIND_ERROR ->
                get<String>(errorKey)
                    ?.let { runCatching { PatternShareError.valueOf(it) }.getOrNull() }
                    ?.let(PatternSharePayload::Error)
                    ?: return null
            else -> return null
        }
    return PatternShareImportRequest(requestId, payload)
}
