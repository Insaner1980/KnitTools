package com.finnvek.knittools.ui.navigation

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
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
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Toisesta sovelluksesta avattu tai jaettu PDF, joka on jo kopioitu sovelluksen omaan tallennustilaan. */
data class IncomingPdf(
    val localUri: String,
    val suggestedName: String,
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
    ) : ViewModel() {
        private val pendingMutex = Mutex()
        private val mutablePending = MutableStateFlow(restorePending())
        val pending: StateFlow<IncomingPdf?> = mutablePending.asStateFlow()

        private val mutableBusy = MutableStateFlow(false)
        val busy: StateFlow<Boolean> = mutableBusy.asStateFlow()

        val activeProjects: StateFlow<List<CounterProject>> =
            counterRepository
                .getActiveProjects()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

        val proStatus: StateFlow<ProStatus> =
            proManager.proState
                .map { it.status }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    proManager.proState.value.status,
                )

        /** Ilmaisversiossa uuden projektin voi aloittaa vain, kun aktiivisia projekteja ei ole. */
        val canStartProject: StateFlow<Boolean> =
            combine(activeProjects, proManager.proState) { projects, state ->
                state.hasFeature(ProFeature.UNLIMITED_PROJECTS) || projects.isEmpty()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

        private val outcomeChannel = Channel<IncomingPdfOutcome>(Channel.BUFFERED)
        val outcomes: Flow<IncomingPdfOutcome> = outcomeChannel.receiveAsFlow()

        fun receive(uri: Uri) {
            viewModelScope.launch {
                val fileName =
                    withContext(ioDispatcher) { context.contentResolver.displayNameOrNull(uri) }
                        ?: uri.lastPathSegment?.takeIf(String::isNotBlank)
                        ?: context.getString(R.string.pattern_pdf_fallback_name)
                receiveCopy(uri, fileName)
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
            viewModelScope.launch {
                if (!isPdf) {
                    outcomeChannel.send(IncomingPdfOutcome.NotPdf)
                    return@launch
                }
                outcomeChannel.send(IncomingPdfOutcome.Downloading)
                val file =
                    try {
                        webPdfDownloader.download(download)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    }
                if (file == null) {
                    outcomeChannel.send(IncomingPdfOutcome.OpenFailed)
                    return@launch
                }
                try {
                    receiveCopy(Uri.fromFile(file), fileName)
                } finally {
                    withContext(ioDispatcher) { file.delete() }
                }
            }
        }

        private suspend fun receiveCopy(
            uri: Uri,
            fileName: String,
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
                outcomeChannel.send(IncomingPdfOutcome.OpenFailed)
                return
            }
            val pdf = IncomingPdf(copiedUri, PatternDisplayNames.documentLabel(fileName))
            try {
                pendingMutex.withLock {
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
        ) {
            runBusy { pdf ->
                attach(pdf, projectId, name)
            }
        }

        fun startNewProject(name: String) {
            runBusy { pdf ->
                // Projekti ja PDF samassa transaktiossa: epäonnistunut liitos ei jätä tyhjää projektia.
                val result =
                    counterRepository.createProjectWithImportedPdf(
                        name = name.ifBlank { pdf.suggestedName },
                        canCreateAdditionalProjects = proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS),
                        patternUri = pdf.localUri,
                        patternName = documentLabel(pdf, name),
                    )
                if (result is ProjectCreationResult.Created) {
                    setPending(null)
                    outcomeChannel.send(IncomingPdfOutcome.OpenProject(result.projectId))
                } else {
                    outcomeChannel.send(IncomingPdfOutcome.SaveFailed)
                }
            }
        }

        fun saveToLibrary(name: String) {
            runBusy { pdf ->
                val savedPatternId =
                    savedPatternRepository.saveImportedPatternIfMissing(
                        patternUrl = pdf.localUri,
                        name = name.ifBlank { pdf.suggestedName },
                    )
                if (savedPatternId == null) {
                    outcomeChannel.send(IncomingPdfOutcome.SaveFailed)
                } else {
                    setPending(null)
                    outcomeChannel.send(IncomingPdfOutcome.OpenSavedPattern(savedPatternId))
                }
            }
        }

        fun discard() {
            if (mutableBusy.value) return
            viewModelScope.launch {
                pendingMutex.withLock {
                    val pdf = mutablePending.value ?: return@withLock
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
                    setPending(null)
                    outcomeChannel.send(IncomingPdfOutcome.OpenProject(projectId))
                }

                // Sama ohje on jo projektissa: kopiota ei tarvita, ja projekti avataan silti.
                ProjectDocumentMutationResult.AlreadyAttached,
                ProjectDocumentMutationResult.DuplicateUri,
                ProjectDocumentMutationResult.DuplicateDocumentKey,
                -> {
                    setPending(null)
                    deleteUnusedCopy(pdf)
                    outcomeChannel.send(IncomingPdfOutcome.OpenProject(projectId))
                }

                else -> outcomeChannel.send(IncomingPdfOutcome.SaveFailed)
            }
        }

        private fun runBusy(block: suspend (IncomingPdf) -> Unit) {
            if (mutableBusy.value) return
            mutableBusy.value = true
            viewModelScope.launch {
                try {
                    pendingMutex.withLock {
                        val pdf = mutablePending.value ?: return@withLock
                        block(pdf)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    outcomeChannel.send(IncomingPdfOutcome.SaveFailed)
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
                .take(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
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
            return IncomingPdf(localUri, name)
        }

        private companion object {
            // Sama projektiriippumaton hakemisto kuin varmuuskopiosta palautetuilla PDF:illä.
            const val UNASSIGNED_PDF_DIRECTORY_ID = 0L
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val PDF_MIME_TYPE = "application/pdf"
            const val KEY_LOCAL_URI = "incoming_pdf_local_uri"
            const val KEY_NAME = "incoming_pdf_name"
        }
    }
