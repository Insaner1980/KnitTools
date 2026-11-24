package com.finnvek.knittools.ui.navigation

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.data.remote.WebPdfDownloader
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.displayNameOrNull
import com.finnvek.knittools.di.ApplicationScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.PROJECT_DOCUMENT_LABEL_MAX_LENGTH
import com.finnvek.knittools.domain.model.PatternDisplayNames
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class IncomingPdfChoices(
    val projects: List<CounterProject>,
    val canStartProject: Boolean,
    val proState: ProState,
)

/** Toisesta sovelluksesta avattu tai jaettu PDF, joka on jo kopioitu sovelluksen omaan tallennustilaan. */
data class IncomingPdf(
    val localUri: String,
    val suggestedName: String,
    val source: UsageDetail = UsageDetail.SOURCE_SHARED,
)

sealed interface IncomingPdfOutcome {
    data class OpenProject(
        val projectId: Long,
    ) : IncomingPdfOutcome

    data class OpenSavedPattern(
        val savedPatternId: Long,
    ) : IncomingPdfOutcome

    data object OpenFailed : IncomingPdfOutcome

    data object SaveFailed : IncomingPdfOutcome

    /** Sovelluksen Ravelry-selaimen lataus alkoi. */
    data object Downloading : IncomingPdfOutcome

    /** Selaimessa napautettu lataus ei ollut PDF. */
    data object NotPdf : IncomingPdfOutcome
}

/**
 * Ottaa vastaan PDF-ohjeen muista sovelluksista (esim. Ravelryn lataus) ja tallentaa sen
 * projektiin, uuteen projektiin tai Kirjastoon. Tiedosto kopioidaan heti sovelluksen omaan
 * tallennustilaan, koska toisen sovelluksen antama lukuoikeus ei säily.
 */
@HiltViewModel
class IncomingPdfViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val patternDocumentStorage: PatternDocumentStorage,
        private val counterRepository: CounterRepository,
        private val savedPatternRepository: SavedPatternRepository,
        private val proManager: ProManager,
        private val webPdfDownloader: WebPdfDownloader,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
        private val analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) : ViewModel() {
        internal var pdfIntentConsumed = false
        private val pendingMutex = Mutex()
        private var incomingRequestVersion = 0L
        private val mutablePending = MutableStateFlow(restorePending())
        val pending: StateFlow<IncomingPdf?> = mutablePending.asStateFlow()

        private val mutableBusy = MutableStateFlow(false)
        val busy: StateFlow<Boolean> = mutableBusy.asStateFlow()
        private val mutableReceiving = MutableStateFlow(false)
        val receiving: StateFlow<Boolean> = mutableReceiving.asStateFlow()
        internal val interruptedSourceUri: Uri?
            get() = savedStateHandle[KEY_SOURCE_URI]
        val proAccessReady get() = proManager.initialStateReady

        val choices: StateFlow<IncomingPdfChoices?> =
            combine(counterRepository.getActiveProjects(), proManager.proState, proManager.initialControlsReady) {
                projects,
                state,
                ready,
                ->
                if (ready) {
                    IncomingPdfChoices(
                        projects,
                        state.hasFeature(ProFeature.UNLIMITED_PROJECTS) || projects.isEmpty(),
                        state,
                    )
                } else {
                    null
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        private val pendingOutcomes = MutableStateFlow<List<IncomingPdfOutcome>>(emptyList())

        // The Activity host is the only collector. Acknowledge each result after its callback returns.
        val outcomes: Flow<IncomingPdfOutcome> =
            flow {
                pendingOutcomes.collect {
                    while (true) {
                        val outcome = pendingOutcomes.value.firstOrNull() ?: break
                        emit(outcome)
                        pendingOutcomes.update { it.drop(1) }
                    }
                }
            }

        private fun postOutcome(outcome: IncomingPdfOutcome) {
            pendingOutcomes.update { it + outcome }
        }

        fun receive(uri: Uri) {
            analytics.track(UsageEvent.PDF_IMPORT_STARTED, UsageDetail.SOURCE_SHARED)
            val requestVersion = ++incomingRequestVersion
            savedStateHandle[KEY_SOURCE_URI] = uri
            mutableReceiving.value = true
            viewModelScope.launch {
                try {
                    val fileName =
                        withContext(ioDispatcher) { context.contentResolver.displayNameOrNull(uri) }
                            ?: uri.lastPathSegment?.takeIf(String::isNotBlank)
                            ?: context.getString(R.string.pattern_pdf_fallback_name)
                    receiveCopy(uri, fileName, requestVersion)
                } finally {
                    if (requestVersion == incomingRequestVersion) finishReceiving()
                }
            }
        }

        /**
         * Sovelluksen oman Ravelry-selaimen Download PDF: tiedosto ladataan suoraan KnitToolsiin
         * ilman puhelimen latauskansiota ja avataan samaan Tallenna ohje -sheetiin.
         */
        fun receiveDownload(download: WebPdfDownload) {
            val fileName = URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType)
            val isPdf =
                download.mimeType.equals(PDF_MIME_TYPE, ignoreCase = true) ||
                    fileName.endsWith(".pdf", ignoreCase = true)
            val requestVersion =
                if (isPdf) {
                    finishReceiving()
                    ++incomingRequestVersion
                } else {
                    incomingRequestVersion
                }
            viewModelScope.launch {
                if (!isPdf) {
                    postOutcome(IncomingPdfOutcome.NotPdf)
                    return@launch
                }
                analytics.track(UsageEvent.PDF_IMPORT_STARTED, UsageDetail.SOURCE_RAVELRY)
                postOutcome(IncomingPdfOutcome.Downloading)
                val file =
                    try {
                        webPdfDownloader.download(download)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    }
                if (file == null) {
                    analytics.track(UsageEvent.PDF_IMPORT_FAILED, UsageDetail.SOURCE_RAVELRY, UsageDetail.ERROR_READ)
                    if (requestVersion == incomingRequestVersion) postOutcome(IncomingPdfOutcome.OpenFailed)
                    return@launch
                }
                try {
                    receiveCopy(Uri.fromFile(file), fileName, requestVersion, UsageDetail.SOURCE_RAVELRY)
                } finally {
                    withContext(NonCancellable + ioDispatcher) { file.delete() }
                }
            }
        }

        private fun finishReceiving() {
            savedStateHandle.remove<Uri>(KEY_SOURCE_URI)
            mutableReceiving.value = false
        }

        private suspend fun receiveCopy(
            uri: Uri,
            fileName: String,
            requestVersion: Long,
            source: UsageDetail = UsageDetail.SOURCE_SHARED,
        ) {
            val copiedUri =
                try {
                    patternDocumentStorage.copyPdfToInternal(
                        context = context,
                        projectId = UNASSIGNED_PDF_DIRECTORY_ID,
                        sourceUri = uri,
                        fileName = fileName,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
            if (copiedUri == null) {
                analytics.track(UsageEvent.PDF_IMPORT_FAILED, source, UsageDetail.ERROR_READ)
                if (requestVersion == incomingRequestVersion) postOutcome(IncomingPdfOutcome.OpenFailed)
                return
            }
            val pdf = IncomingPdf(copiedUri, PatternDisplayNames.documentLabel(fileName), source)
            try {
                pendingMutex.withLock {
                    if (requestVersion != incomingRequestVersion) {
                        deleteUnusedCopy(pdf)
                        return@withLock
                    }
                    deleteUnusedCopy(mutablePending.value)
                    setPending(pdf)
                }
            } catch (cancellation: CancellationException) {
                deleteUnusedCopy(pdf)
                throw cancellation
            }
        }

        fun attachToProject(
            projectId: Long,
            name: String,
            expectedPdfUri: String? = mutablePending.value?.localUri,
        ) {
            runBusy(expectedPdfUri) { pdf ->
                attach(pdf, projectId, name)
            }
        }

        fun startNewProject(
            name: String,
            expectedPdfUri: String? = mutablePending.value?.localUri,
        ) {
            runBusy(expectedPdfUri) { pdf ->
                // Projekti ja PDF samassa transaktiossa: epäonnistunut liitos ei jätä tyhjää projektia.
                val result =
                    counterRepository.createProjectWithImportedPdf(
                        name = name.ifBlank { pdf.suggestedName },
                        // Jaettu PDF voi käynnistää sovelluksen kylmänä: odota Pro-tilan alkulatausta.
                        canCreateAdditionalProjects =
                            proManager.hasFeatureAfterInitialLoad(ProFeature.UNLIMITED_PROJECTS),
                        patternUri = pdf.localUri,
                        patternName = documentLabel(pdf, name),
                    )
                if (result is ProjectCreationResult.Created) {
                    analytics.track(UsageEvent.PROJECT_CREATED, pdf.source)
                    analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, pdf.source)
                    setPending(null)
                    postOutcome(IncomingPdfOutcome.OpenProject(result.projectId))
                } else {
                    analytics.track(UsageEvent.PDF_IMPORT_FAILED, pdf.source, UsageDetail.ERROR_WRITE)
                    postOutcome(IncomingPdfOutcome.SaveFailed)
                }
            }
        }

        fun saveToLibrary(
            name: String,
            expectedPdfUri: String? = mutablePending.value?.localUri,
        ) {
            runBusy(expectedPdfUri) { pdf ->
                val savedPatternId =
                    savedPatternRepository.saveImportedPatternIfMissing(
                        patternUrl = pdf.localUri,
                        name = name.ifBlank { pdf.suggestedName },
                    )
                if (savedPatternId == null) {
                    analytics.track(UsageEvent.PDF_IMPORT_FAILED, pdf.source, UsageDetail.ERROR_WRITE)
                    postOutcome(IncomingPdfOutcome.SaveFailed)
                } else {
                    setPending(null)
                    analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, pdf.source)
                    postOutcome(IncomingPdfOutcome.OpenSavedPattern(savedPatternId))
                }
            }
        }

        fun discard(expectedPdfUri: String? = mutablePending.value?.localUri) {
            if (mutableBusy.value) return
            viewModelScope.launch {
                pendingMutex.withLock {
                    val pdf = mutablePending.value ?: return@withLock
                    if (pdf.localUri != expectedPdfUri) return@withLock
                    analytics.track(UsageEvent.PDF_IMPORT_CANCELLED, pdf.source)
                    setPending(null)
                    deleteUnusedCopy(pdf)
                }
            }
        }

        private suspend fun attach(
            pdf: IncomingPdf,
            projectId: Long,
            name: String,
        ) {
            val result =
                counterRepository.attachPattern(
                    id = projectId,
                    patternUri = pdf.localUri,
                    patternName = documentLabel(pdf, name),
                    currentPatternPage = 0,
                    patternRowMapping = null,
                )
            when (result) {
                is ProjectDocumentMutationResult.Added -> {
                    analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, pdf.source)
                    setPending(null)
                    postOutcome(IncomingPdfOutcome.OpenProject(projectId))
                }

                // Sama ohje on jo projektissa: kopiota ei tarvita, ja projekti avataan silti.
                ProjectDocumentMutationResult.AlreadyAttached,
                ProjectDocumentMutationResult.DuplicateUri,
                ProjectDocumentMutationResult.DuplicateDocumentKey,
                -> {
                    analytics.track(UsageEvent.PDF_IMPORT_SUCCEEDED, pdf.source)
                    setPending(null)
                    deleteUnusedCopy(pdf)
                    postOutcome(IncomingPdfOutcome.OpenProject(projectId))
                }

                else -> {
                    analytics.track(UsageEvent.PDF_IMPORT_FAILED, pdf.source, UsageDetail.ERROR_WRITE)
                    postOutcome(IncomingPdfOutcome.SaveFailed)
                }
            }
        }

        private fun runBusy(
            expectedPdfUri: String?,
            block: suspend (IncomingPdf) -> Unit,
        ) {
            if (mutableBusy.value) return
            mutableBusy.value = true
            viewModelScope.launch {
                try {
                    pendingMutex.withLock {
                        val pdf = mutablePending.value ?: return@withLock
                        if (pdf.localUri != expectedPdfUri) return@withLock
                        block(pdf)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutablePending.value?.let {
                        analytics.track(
                            UsageEvent.PDF_IMPORT_FAILED,
                            it.source,
                            UsageDetail.ERROR_WRITE,
                        )
                    }
                    postOutcome(IncomingPdfOutcome.SaveFailed)
                } finally {
                    mutableBusy.value = false
                }
            }
        }

        private fun documentLabel(
            pdf: IncomingPdf,
            name: String,
        ): String =
            name
                .trim()
                .ifBlank { pdf.suggestedName }
                .takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
                .trimEnd()

        /**
         * Siivous sovelluksen elinkaaren scopessa: ViewModelin poistuminen ei saa keskeyttää sitä,
         * koska pattern_pdfs/0-hakemiston orpoja ei siivota myöhemmin.
         */
        private fun deleteUnusedCopy(pdf: IncomingPdf?) {
            pdf ?: return
            applicationScope.launch {
                try {
                    savedPatternRepository.deleteLocalPatternFileIfUnused(pdf.localUri)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Failed cleanup can leave an orphan but must not undo a completed save.
                }
            }
        }

        private fun setPending(pdf: IncomingPdf?) {
            mutablePending.value = pdf
            savedStateHandle[KEY_LOCAL_URI] = pdf?.localUri
            savedStateHandle[KEY_NAME] = pdf?.suggestedName
            savedStateHandle[KEY_ANALYTICS_SOURCE] = pdf?.source?.name
        }

        private fun restorePending(): IncomingPdf? {
            val localUri = savedStateHandle.get<Any?>(KEY_LOCAL_URI) as? String ?: return null
            val name = savedStateHandle.get<Any?>(KEY_NAME) as? String ?: return null
            val isStagedPdf =
                runCatching {
                    val file = AppFileStorage.resolveAppOwnedFile(context, localUri.toUri()) ?: return@runCatching false
                    file.isFile &&
                        file.canonicalFile.parentFile ==
                        File(context.filesDir, "pattern_pdfs/$UNASSIGNED_PDF_DIRECTORY_ID").canonicalFile
                }.getOrDefault(false)
            if (!isStagedPdf) return null
            val source =
                if (savedStateHandle.get<String>(KEY_ANALYTICS_SOURCE) ==
                    UsageDetail.SOURCE_RAVELRY.name
                ) {
                    UsageDetail.SOURCE_RAVELRY
                } else {
                    UsageDetail.SOURCE_SHARED
                }
            return IncomingPdf(localUri, name, source)
        }

        private companion object {
            // Sama projektiriippumaton hakemisto kuin varmuuskopiosta palautetuilla PDF:illä.
            const val UNASSIGNED_PDF_DIRECTORY_ID = 0L
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val PDF_MIME_TYPE = "application/pdf"
            const val KEY_LOCAL_URI = "incoming_pdf_local_uri"
            const val KEY_NAME = "incoming_pdf_name"
            const val KEY_ANALYTICS_SOURCE = "incoming_pdf_analytics_source"
            const val KEY_SOURCE_URI = "incoming_pdf_source_uri"
        }
    }
