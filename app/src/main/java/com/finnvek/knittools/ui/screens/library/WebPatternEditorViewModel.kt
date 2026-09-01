package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.WebPatternDesignerValidation
import com.finnvek.knittools.domain.model.WebPatternTitleValidation
import com.finnvek.knittools.domain.model.WebPatternUrlValidation
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.domain.model.validateWebPatternDesigner
import com.finnvek.knittools.domain.model.validateWebPatternTitle
import com.finnvek.knittools.domain.model.validateWebPatternUrl
import com.finnvek.knittools.domain.model.webPatternUrlOrNull
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.WebPatternInput
import com.finnvek.knittools.repository.WebPatternMutationResult
import com.finnvek.knittools.ui.navigation.PatternShareError
import com.finnvek.knittools.ui.navigation.PatternShareImportRequest
import com.finnvek.knittools.ui.navigation.PatternSharePayload
import com.finnvek.knittools.ui.navigation.Screen
import com.finnvek.knittools.ui.navigation.WebPatternEditorOrigin
import com.finnvek.knittools.ui.navigation.WebPatternEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WebPatternEditorField {
    Title,
    Url,
    Designer,
}

enum class WebPatternEditorError {
    AlreadySaved,
    SaveFailed,
    UpdateFailed,
    ProjectUnavailable,
    PatternUnavailable,
    NotEditable,
    StaleAction,
    SharedLinkInvalid,
    SharedLinkAmbiguous,
}

data class PendingIncomingWebPatternShare(
    val requestId: Long,
    val url: String,
    val titleSuggestion: String,
)

data class PendingWebPatternReplacement(
    val projectId: Long,
    val savedPatternId: Long,
    val expectedExistingSavedPatternId: Long,
)

sealed interface WebPatternEditorCompletion {
    val eventId: Long

    data class OpenDetail(
        override val eventId: Long,
        val patternId: Long,
    ) : WebPatternEditorCompletion

    data class OpenProject(
        override val eventId: Long,
        val projectId: Long,
    ) : WebPatternEditorCompletion

    data class OpenRavelry(
        override val eventId: Long,
        val url: String,
    ) : WebPatternEditorCompletion
}

enum class WebPatternShareAcceptResult {
    Stored,
    Ignored,
}

data class WebPatternEditorUiState(
    val route: WebPatternEditorRoute?,
    val title: String = "",
    val designer: String = "",
    val url: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val didPersist: Boolean = false,
    val error: WebPatternEditorError? = null,
    val pendingIncomingShare: PendingIncomingWebPatternShare? = null,
    val pendingReplacement: PendingWebPatternReplacement? = null,
    val completion: WebPatternEditorCompletion? = null,
    val expectedUpdatedAt: Long? = null,
) {
    val titleValidation: WebPatternTitleValidation
        get() = validateWebPatternTitle(title)

    val designerValidation: WebPatternDesignerValidation
        get() = validateWebPatternDesigner(designer)

    val urlValidation: WebPatternUrlValidation
        get() = validateWebPatternUrl(url)

    val canSave: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                !didPersist &&
                route != null &&
                titleValidation is WebPatternTitleValidation.Valid &&
                designerValidation is WebPatternDesignerValidation.Valid &&
                urlValidation is WebPatternUrlValidation.Valid

    val sourceHost: String?
        get() = (urlValidation as? WebPatternUrlValidation.Valid)?.value?.host

    val showsHttpWarning: Boolean
        get() = (urlValidation as? WebPatternUrlValidation.Valid)?.value?.isSecure == false

    val firstInvalidField: WebPatternEditorField?
        get() =
            when {
                titleValidation !is WebPatternTitleValidation.Valid -> WebPatternEditorField.Title
                urlValidation !is WebPatternUrlValidation.Valid -> WebPatternEditorField.Url
                designerValidation !is WebPatternDesignerValidation.Valid -> WebPatternEditorField.Designer
                else -> null
            }
}

@HiltViewModel
class WebPatternEditorViewModel
    @Inject
    constructor(
        private val savedPatternRepository: SavedPatternRepository,
        private val counterRepository: CounterRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route =
            WebPatternEditorRoute.from(
                originValue = savedStateHandle[Screen.WebPatternEditor.ARG_ORIGIN],
                projectId = savedStateHandle.get<Long>(Screen.WebPatternEditor.ARG_PROJECT_ID),
                patternId = savedStateHandle.get<Long>(Screen.WebPatternEditor.ARG_PATTERN_ID),
            )
        private val mutableUiState = MutableStateFlow(savedStateHandle.restoreWebPatternEditorState(route))
        val uiState = mutableUiState.asStateFlow()

        init {
            when {
                route == null ->
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            error = WebPatternEditorError.PatternUnavailable,
                        )
                    }
                savedStateHandle.get<Boolean>(KEY_INITIALIZED) == true -> Unit
                route.origin == WebPatternEditorOrigin.Edit -> loadPatternForEdit(requireNotNull(route.patternId))
                else -> {
                    savedStateHandle[KEY_INITIALIZED] = true
                    persistState()
                }
            }
        }

        fun updateTitle(value: String) = updateDraft { it.copy(title = value, error = null) }

        fun updateDesigner(value: String) = updateDraft { it.copy(designer = value, error = null) }

        fun updateUrl(value: String) = updateDraft { it.copy(url = value, error = null) }

        fun offerSharedRequest(request: PatternShareImportRequest): WebPatternShareAcceptResult {
            val acceptedRequestId = savedStateHandle.get<Long>(KEY_ACCEPTED_SHARE_REQUEST_ID)
            if (acceptedRequestId == request.requestId) return WebPatternShareAcceptResult.Stored
            val existingPending = mutableUiState.value.pendingIncomingShare
            if (existingPending?.requestId == request.requestId) return WebPatternShareAcceptResult.Stored

            return when (val payload = request.payload) {
                is PatternSharePayload.Ravelry -> WebPatternShareAcceptResult.Ignored
                is PatternSharePayload.Error -> {
                    savedStateHandle[KEY_ACCEPTED_SHARE_REQUEST_ID] = request.requestId
                    mutableUiState.update { state ->
                        state.copy(error = payload.error.toEditorError())
                    }
                    persistState()
                    WebPatternShareAcceptResult.Stored
                }

                is PatternSharePayload.WebLink -> {
                    val incoming =
                        PendingIncomingWebPatternShare(request.requestId, payload.url, payload.titleSuggestion)
                    if (hasCurrentDraft()) {
                        mutableUiState.update { it.copy(pendingIncomingShare = incoming) }
                    } else {
                        applyIncomingShare(incoming)
                    }
                    persistState()
                    WebPatternShareAcceptResult.Stored
                }
            }
        }

        fun resolveIncomingShare(useIncoming: Boolean) {
            val incoming = mutableUiState.value.pendingIncomingShare ?: return
            if (useIncoming) {
                applyIncomingShare(incoming)
            } else {
                savedStateHandle[KEY_ACCEPTED_SHARE_REQUEST_ID] = incoming.requestId
                mutableUiState.update { it.copy(pendingIncomingShare = null) }
            }
            persistState()
        }

        fun save() {
            val state = mutableUiState.value
            if (!state.canSave || state.completion != null) return
            val currentRoute = state.route ?: return
            val input = WebPatternInput(title = state.title, designer = state.designer, url = state.url)
            val submittedUrl = (state.urlValidation as WebPatternUrlValidation.Valid).value.originalUrl
            mutableUiState.update { it.copy(isSaving = true, error = null) }
            persistState()
            viewModelScope.launch {
                try {
                    val result =
                        if (currentRoute.origin == WebPatternEditorOrigin.Edit) {
                            val patternId = currentRoute.patternId ?: return@launch
                            val expectedUpdatedAt = state.expectedUpdatedAt ?: return@launch
                            savedPatternRepository.updateWebPattern(patternId, expectedUpdatedAt, input)
                        } else {
                            savedPatternRepository.createWebPattern(input)
                        }
                    handleMutationResult(result, currentRoute, submittedUrl)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.update {
                        it.copy(
                            error =
                                if (currentRoute.origin == WebPatternEditorOrigin.Edit) {
                                    WebPatternEditorError.UpdateFailed
                                } else {
                                    WebPatternEditorError.SaveFailed
                                },
                        )
                    }
                } finally {
                    mutableUiState.update { it.copy(isSaving = false) }
                    persistState()
                }
            }
        }

        fun confirmReplacement() {
            val pending = mutableUiState.value.pendingReplacement ?: return
            if (mutableUiState.value.isSaving) return
            mutableUiState.update { it.copy(isSaving = true, error = null) }
            persistState()
            viewModelScope.launch {
                try {
                    when (
                        counterRepository.attachSavedPatternMetadata(
                            projectId = pending.projectId,
                            savedPatternId = pending.savedPatternId,
                            expectedExistingSavedPatternId = pending.expectedExistingSavedPatternId,
                        )
                    ) {
                        is SavedPatternMetadataMutationResult.Attached,
                        is SavedPatternMetadataMutationResult.AlreadyAttached,
                        -> complete(WebPatternEditorCompletion.OpenProject(nextEventId(), pending.projectId))

                        is SavedPatternMetadataMutationResult.ReplacementRequired,
                        SavedPatternMetadataMutationResult.StaleAction,
                        -> {
                            mutableUiState.update {
                                it.copy(
                                    pendingReplacement = null,
                                    error = WebPatternEditorError.StaleAction,
                                )
                            }
                            persistState()
                        }

                        SavedPatternMetadataMutationResult.ProjectMissing ->
                            setError(
                                WebPatternEditorError.ProjectUnavailable,
                            )
                        SavedPatternMetadataMutationResult.PatternMissing ->
                            setError(
                                WebPatternEditorError.PatternUnavailable,
                            )
                        SavedPatternMetadataMutationResult.NotWebPattern,
                        SavedPatternMetadataMutationResult.PersistenceFailure,
                        SavedPatternMetadataMutationResult.Unlinked,
                        SavedPatternMetadataMutationResult.AlreadyUnlinked,
                        -> setError(WebPatternEditorError.SaveFailed)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    setError(WebPatternEditorError.SaveFailed)
                } finally {
                    mutableUiState.update { it.copy(isSaving = false) }
                    persistState()
                }
            }
        }

        fun dismissReplacement() {
            val pending = mutableUiState.value.pendingReplacement ?: return
            complete(WebPatternEditorCompletion.OpenProject(nextEventId(), pending.projectId))
        }

        fun consumeCompletion(eventId: Long) {
            if (mutableUiState.value.completion?.eventId != eventId) return
            mutableUiState.update { it.copy(completion = null) }
            persistState()
        }

        private fun loadPatternForEdit(patternId: Long) {
            mutableUiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    val pattern = savedPatternRepository.getById(patternId)
                    if (pattern == null || !pattern.isWebPatternCompatible) {
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                error =
                                    if (pattern == null) {
                                        WebPatternEditorError.PatternUnavailable
                                    } else {
                                        WebPatternEditorError.NotEditable
                                    },
                            )
                        }
                    } else {
                        initializeEditDraft(pattern)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            error = WebPatternEditorError.PatternUnavailable,
                        )
                    }
                } finally {
                    savedStateHandle[KEY_INITIALIZED] = true
                    persistState()
                }
            }
        }

        private fun initializeEditDraft(pattern: SavedPattern) {
            mutableUiState.update {
                it.copy(
                    title = pattern.name,
                    designer = pattern.designerName,
                    url = pattern.webPatternUrlOrNull?.originalUrl.orEmpty(),
                    expectedUpdatedAt = pattern.updatedAt,
                    isLoading = false,
                    error = null,
                )
            }
        }

        private suspend fun handleMutationResult(
            result: WebPatternMutationResult,
            currentRoute: WebPatternEditorRoute,
            submittedUrl: String,
        ) {
            when (result) {
                is WebPatternMutationResult.Created -> finishCreatedOrDuplicate(result.patternId, currentRoute)
                is WebPatternMutationResult.Duplicate -> {
                    if (currentRoute.origin == WebPatternEditorOrigin.Edit) {
                        setError(WebPatternEditorError.AlreadySaved)
                    } else {
                        finishCreatedOrDuplicate(result.existingPatternId, currentRoute)
                    }
                }
                is WebPatternMutationResult.Updated ->
                    complete(
                        WebPatternEditorCompletion.OpenDetail(nextEventId(), result.patternId),
                    )
                WebPatternMutationResult.RavelryOwnedUrl ->
                    complete(WebPatternEditorCompletion.OpenRavelry(nextEventId(), submittedUrl))

                WebPatternMutationResult.InvalidTitle,
                WebPatternMutationResult.InvalidDesigner,
                WebPatternMutationResult.InvalidUrl,
                WebPatternMutationResult.PersistenceFailure,
                -> setMutationError(currentRoute, WebPatternEditorError.SaveFailed, WebPatternEditorError.UpdateFailed)

                WebPatternMutationResult.PatternMissing -> setError(WebPatternEditorError.PatternUnavailable)
                WebPatternMutationResult.NotEditableAsWebPattern -> setError(WebPatternEditorError.NotEditable)
                WebPatternMutationResult.StaleAction -> setError(WebPatternEditorError.StaleAction)
            }
        }

        private suspend fun finishCreatedOrDuplicate(
            patternId: Long,
            currentRoute: WebPatternEditorRoute,
        ) {
            val projectId = currentRoute.projectId
            if (currentRoute.origin != WebPatternEditorOrigin.Project || projectId == null) {
                complete(WebPatternEditorCompletion.OpenDetail(nextEventId(), patternId))
                return
            }
            when (val attachResult = counterRepository.attachSavedPatternMetadata(projectId, patternId)) {
                is SavedPatternMetadataMutationResult.Attached,
                is SavedPatternMetadataMutationResult.AlreadyAttached,
                -> complete(WebPatternEditorCompletion.OpenProject(nextEventId(), projectId))

                is SavedPatternMetadataMutationResult.ReplacementRequired -> {
                    mutableUiState.update {
                        it.copy(
                            didPersist = true,
                            pendingReplacement =
                                PendingWebPatternReplacement(
                                    projectId = projectId,
                                    savedPatternId = patternId,
                                    expectedExistingSavedPatternId = attachResult.existingSavedPatternId,
                                ),
                        )
                    }
                    persistState()
                }

                SavedPatternMetadataMutationResult.ProjectMissing -> {
                    mutableUiState.update {
                        it.copy(didPersist = true, error = WebPatternEditorError.ProjectUnavailable)
                    }
                    persistState()
                }

                SavedPatternMetadataMutationResult.PatternMissing,
                SavedPatternMetadataMutationResult.NotWebPattern,
                SavedPatternMetadataMutationResult.StaleAction,
                SavedPatternMetadataMutationResult.PersistenceFailure,
                SavedPatternMetadataMutationResult.Unlinked,
                SavedPatternMetadataMutationResult.AlreadyUnlinked,
                -> {
                    mutableUiState.update { it.copy(didPersist = true, error = WebPatternEditorError.SaveFailed) }
                    persistState()
                }
            }
        }

        private fun setMutationError(
            route: WebPatternEditorRoute,
            createError: WebPatternEditorError,
            updateError: WebPatternEditorError,
        ) {
            setError(if (route.origin == WebPatternEditorOrigin.Edit) updateError else createError)
        }

        private fun setError(error: WebPatternEditorError) {
            mutableUiState.update { it.copy(error = error) }
            persistState()
        }

        private fun complete(completion: WebPatternEditorCompletion) {
            mutableUiState.update {
                it.copy(
                    didPersist = true,
                    error = null,
                    pendingReplacement = null,
                    completion = completion,
                )
            }
            persistState()
        }

        private fun updateDraft(transform: (WebPatternEditorUiState) -> WebPatternEditorUiState) {
            if (mutableUiState.value.isSaving || mutableUiState.value.didPersist) return
            mutableUiState.update(transform)
            persistState()
        }

        private fun hasCurrentDraft(): Boolean {
            val state = mutableUiState.value
            return state.route?.origin == WebPatternEditorOrigin.Edit ||
                state.title.isNotBlank() ||
                state.designer.isNotBlank() ||
                state.url.isNotBlank()
        }

        private fun applyIncomingShare(incoming: PendingIncomingWebPatternShare) {
            savedStateHandle[KEY_ACCEPTED_SHARE_REQUEST_ID] = incoming.requestId
            mutableUiState.update {
                it.copy(
                    title = incoming.titleSuggestion,
                    designer = "",
                    url = incoming.url,
                    error = null,
                    pendingIncomingShare = null,
                )
            }
        }

        private fun nextEventId(): Long {
            val next = (savedStateHandle.get<Long>(KEY_NEXT_EVENT_ID) ?: 0L) + 1L
            savedStateHandle[KEY_NEXT_EVENT_ID] = next
            return next
        }

        private fun persistState() {
            savedStateHandle.saveWebPatternEditorState(mutableUiState.value)
        }
    }

private fun PatternShareError.toEditorError(): WebPatternEditorError =
    when (this) {
        PatternShareError.Ambiguous -> WebPatternEditorError.SharedLinkAmbiguous
        PatternShareError.Empty,
        PatternShareError.Invalid,
        PatternShareError.TooLong,
        -> WebPatternEditorError.SharedLinkInvalid
    }

private const val KEY_INITIALIZED = "web_pattern_editor_initialized"
private const val KEY_TITLE = "web_pattern_editor_title"
private const val KEY_DESIGNER = "web_pattern_editor_designer"
private const val KEY_URL = "web_pattern_editor_url"
private const val KEY_IS_LOADING = "web_pattern_editor_loading"
private const val KEY_IS_SAVING = "web_pattern_editor_saving"
private const val KEY_DID_PERSIST = "web_pattern_editor_did_persist"
private const val KEY_ERROR = "web_pattern_editor_error"
private const val KEY_EXPECTED_UPDATED_AT = "web_pattern_editor_expected_updated_at"
private const val KEY_ACCEPTED_SHARE_REQUEST_ID = "web_pattern_editor_accepted_share_request_id"
private const val KEY_INCOMING_REQUEST_ID = "web_pattern_editor_incoming_request_id"
private const val KEY_INCOMING_URL = "web_pattern_editor_incoming_url"
private const val KEY_INCOMING_TITLE = "web_pattern_editor_incoming_title"
private const val KEY_REPLACEMENT_PROJECT_ID = "web_pattern_editor_replacement_project_id"
private const val KEY_REPLACEMENT_PATTERN_ID = "web_pattern_editor_replacement_pattern_id"
private const val KEY_REPLACEMENT_EXISTING_PATTERN_ID = "web_pattern_editor_replacement_existing_pattern_id"
private const val KEY_COMPLETION_KIND = "web_pattern_editor_completion_kind"
private const val KEY_COMPLETION_EVENT_ID = "web_pattern_editor_completion_event_id"
private const val KEY_COMPLETION_TARGET_ID = "web_pattern_editor_completion_target_id"
private const val KEY_COMPLETION_URL = "web_pattern_editor_completion_url"
private const val KEY_NEXT_EVENT_ID = "web_pattern_editor_next_event_id"
private const val COMPLETION_DETAIL = "detail"
private const val COMPLETION_PROJECT = "project"
private const val COMPLETION_RAVELRY = "ravelry"

private fun SavedStateHandle.saveWebPatternEditorState(state: WebPatternEditorUiState) {
    this[KEY_TITLE] = state.title
    this[KEY_DESIGNER] = state.designer
    this[KEY_URL] = state.url
    this[KEY_IS_LOADING] = state.isLoading
    this[KEY_IS_SAVING] = state.isSaving
    this[KEY_DID_PERSIST] = state.didPersist
    this[KEY_ERROR] = state.error?.name
    this[KEY_EXPECTED_UPDATED_AT] = state.expectedUpdatedAt
    this[KEY_INCOMING_REQUEST_ID] = state.pendingIncomingShare?.requestId
    this[KEY_INCOMING_URL] = state.pendingIncomingShare?.url
    this[KEY_INCOMING_TITLE] = state.pendingIncomingShare?.titleSuggestion
    this[KEY_REPLACEMENT_PROJECT_ID] = state.pendingReplacement?.projectId
    this[KEY_REPLACEMENT_PATTERN_ID] = state.pendingReplacement?.savedPatternId
    this[KEY_REPLACEMENT_EXISTING_PATTERN_ID] = state.pendingReplacement?.expectedExistingSavedPatternId
    this[KEY_COMPLETION_KIND] =
        when (state.completion) {
            is WebPatternEditorCompletion.OpenDetail -> COMPLETION_DETAIL
            is WebPatternEditorCompletion.OpenProject -> COMPLETION_PROJECT
            is WebPatternEditorCompletion.OpenRavelry -> COMPLETION_RAVELRY
            null -> null
        }
    this[KEY_COMPLETION_EVENT_ID] = state.completion?.eventId
    this[KEY_COMPLETION_TARGET_ID] =
        when (val completion = state.completion) {
            is WebPatternEditorCompletion.OpenDetail -> completion.patternId
            is WebPatternEditorCompletion.OpenProject -> completion.projectId
            is WebPatternEditorCompletion.OpenRavelry,
            null,
            -> null
        }
    this[KEY_COMPLETION_URL] = (state.completion as? WebPatternEditorCompletion.OpenRavelry)?.url
}

@Suppress("CyclomaticComplexMethod") // Tallennetun editoritilan kentät palautetaan eksplisiittisesti ja fail-closed.
private fun SavedStateHandle.restoreWebPatternEditorState(route: WebPatternEditorRoute?): WebPatternEditorUiState {
    val pendingRequestId = get<Long>(KEY_INCOMING_REQUEST_ID)
    val pendingUrl = get<String>(KEY_INCOMING_URL)
    val pending =
        if (pendingRequestId != null && pendingUrl != null) {
            PendingIncomingWebPatternShare(
                requestId = pendingRequestId,
                url = pendingUrl,
                titleSuggestion = get<String>(KEY_INCOMING_TITLE).orEmpty(),
            )
        } else {
            null
        }
    val eventId = get<Long>(KEY_COMPLETION_EVENT_ID)
    val completion =
        if (eventId == null) {
            null
        } else {
            when (get<String>(KEY_COMPLETION_KIND)) {
                COMPLETION_DETAIL ->
                    get<Long>(KEY_COMPLETION_TARGET_ID)?.let { WebPatternEditorCompletion.OpenDetail(eventId, it) }

                COMPLETION_PROJECT ->
                    get<Long>(KEY_COMPLETION_TARGET_ID)?.let { WebPatternEditorCompletion.OpenProject(eventId, it) }

                COMPLETION_RAVELRY ->
                    get<String>(KEY_COMPLETION_URL)?.let { WebPatternEditorCompletion.OpenRavelry(eventId, it) }

                else -> null
            }
        }
    val error =
        get<String>(KEY_ERROR)?.let { value ->
            runCatching { WebPatternEditorError.valueOf(value) }.getOrNull()
        }
    val replacementProjectId = get<Long>(KEY_REPLACEMENT_PROJECT_ID)
    val replacementPatternId = get<Long>(KEY_REPLACEMENT_PATTERN_ID)
    val replacementExistingPatternId = get<Long>(KEY_REPLACEMENT_EXISTING_PATTERN_ID)
    val pendingReplacement =
        if (replacementProjectId != null && replacementPatternId != null && replacementExistingPatternId != null) {
            PendingWebPatternReplacement(
                projectId = replacementProjectId,
                savedPatternId = replacementPatternId,
                expectedExistingSavedPatternId = replacementExistingPatternId,
            )
        } else {
            null
        }
    return WebPatternEditorUiState(
        route = route,
        title = get<String>(KEY_TITLE).orEmpty(),
        designer = get<String>(KEY_DESIGNER).orEmpty(),
        url = get<String>(KEY_URL).orEmpty(),
        isLoading = get<Boolean>(KEY_IS_LOADING) ?: (route?.origin == WebPatternEditorOrigin.Edit),
        isSaving = false,
        didPersist = get<Boolean>(KEY_DID_PERSIST) ?: false,
        error = error,
        pendingIncomingShare = pending,
        pendingReplacement = pendingReplacement,
        completion = completion,
        expectedUpdatedAt = get<Long>(KEY_EXPECTED_UPDATED_AT),
    )
}
