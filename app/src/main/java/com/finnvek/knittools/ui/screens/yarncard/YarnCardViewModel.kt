package com.finnvek.knittools.ui.screens.yarncard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.ParsedYarnLabel
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.YarnLabelScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YarnCardFormState(
    val editingCardId: Long? = null,
    val brand: String = "",
    val yarnName: String = "",
    val fiberContent: String = "",
    val weightGrams: String = "",
    val lengthMeters: String = "",
    val needleSize: String = "",
    val gaugeInfo: String = "",
    val colorName: String = "",
    val colorNumber: String = "",
    val dyeLot: String = "",
    val weightCategory: String = "",
    val careSymbols: Long = 0L,
    val photoUri: String = "",
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val quantityInStash: Int = 1,
    val status: String = YarnCardStatus.IN_STASH,
    val linkedProjectId: Long? = null,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class YarnCardViewModel
    @Inject
    constructor(
        private val repository: YarnCardRepository,
        private val counterRepository: CounterRepository,
        private val proManager: ProManager,
        private val scanRepository: YarnLabelScanRepository,
        private val aiQuotaManager: AiQuotaManager,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _formState = MutableStateFlow(YarnCardFormState())
        val formState: StateFlow<YarnCardFormState> = _formState.asStateFlow()

        val savedCards: StateFlow<List<YarnCard>> =
            repository.getAllCards().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.OCR)

        val availableProjects: StateFlow<List<CounterProject>> =
            counterRepository
                .getActiveProjects()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        val linkedProjectName: StateFlow<String?> =
            combine(availableProjects, formState) { projects, form ->
                form.linkedProjectId?.let { id -> projects.firstOrNull { it.id == id }?.name }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

        // Skannatut arvot estimaattorille (Save and Use / Use in Calculator)
        private val _pendingCalcValues = MutableStateFlow<Triple<String, String, String>?>(null)
        val pendingCalcValues: StateFlow<Triple<String, String, String>?> = _pendingCalcValues.asStateFlow()

        private var activeScanRequestId = 0L

        fun setPendingCalcValues(
            weightGrams: String,
            lengthMeters: String,
            needleSize: String,
        ) {
            _pendingCalcValues.value = Triple(weightGrams, lengthMeters, needleSize)
        }

        fun clearPendingCalcValues() {
            _pendingCalcValues.value = null
        }

        fun linkCardToProject(
            cardId: Long,
            projectId: Long,
        ) {
            viewModelScope.launch {
                repository.updateLinkedProjectId(cardId, projectId)
            }
        }

        fun loadFromScan(
            parsed: ParsedYarnLabel,
            photoUri: Uri?,
        ) {
            _formState.value =
                YarnCardFormState(
                    brand = parsed.brand,
                    yarnName = parsed.yarnName,
                    fiberContent = parsed.fiberContent,
                    weightGrams = parsed.weightGrams,
                    lengthMeters = parsed.lengthMeters,
                    needleSize = parsed.needleSize,
                    gaugeInfo = parsed.gaugeInfo,
                    colorName = parsed.colorName,
                    colorNumber = parsed.colorNumber,
                    dyeLot = parsed.dyeLot,
                    weightCategory = parsed.weightCategory,
                    careSymbols = parsed.careSymbols,
                    photoUri = photoUri?.toString() ?: "",
                )
        }

        fun loadCardById(id: Long) {
            loadCardForDetail(id)
        }

        fun observeCardForDetail(id: Long): Flow<YarnCard?> = repository.observeCard(id)

        fun loadCardForDetail(
            id: Long,
            onLoaded: (Boolean) -> Unit = {},
        ) {
            viewModelScope.launch {
                onLoaded(loadCardForDetailInternal(id))
            }
        }

        private suspend fun loadCardForDetailInternal(id: Long): Boolean {
            val card = repository.getCard(id)
            if (card == null) {
                clearFormState()
                return false
            }
            loadFromCard(card)
            return true
        }

        fun loadFromCard(card: YarnCard) {
            invalidateActiveScan()
            _formState.value =
                YarnCardFormState(
                    editingCardId = card.id,
                    brand = card.brand,
                    yarnName = card.yarnName,
                    fiberContent = card.fiberContent,
                    weightGrams = card.weightGrams,
                    lengthMeters = card.lengthMeters,
                    needleSize = card.needleSize,
                    gaugeInfo = card.gaugeInfo,
                    colorName = card.colorName,
                    colorNumber = card.colorNumber,
                    dyeLot = card.dyeLot,
                    weightCategory = card.weightCategory,
                    careSymbols = card.careSymbols,
                    photoUri = card.photoUri,
                    quantityInStash = card.quantityInStash,
                    status = card.status,
                    linkedProjectId = card.linkedProjectId,
                )
        }

        fun updateField(updater: YarnCardFormState.() -> YarnCardFormState) {
            _formState.update { it.updater() }
        }

        fun setScanning(scanning: Boolean) {
            _formState.update { it.copy(isScanning = scanning) }
        }

        /**
         * Skannaa lankaetikettikuva Geminillä ja täyttää lomakkeen.
         * Kutsuu [onSuccess]-callbackia onnistumisen jälkeen (navigointi review-näytölle).
         */
        fun scanWithGemini(
            photoUri: Uri,
            onSuccess: () -> Unit,
        ) {
            if (!isPro) {
                rejectScanWithoutPro()
                return
            }
            val scanRequestId = startScanRequest()
            viewModelScope.launch {
                if (!isActiveScanOrDeletePhoto(scanRequestId, photoUri)) return@launch
                _formState.update { it.copy(isScanning = true, scanError = null) }

                if (!hasQuotaForActiveScan(scanRequestId)) {
                    deletePhotoFile(photoUri.toString())
                    return@launch
                }
                if (!isActiveScanOrDeletePhoto(scanRequestId, photoUri)) return@launch

                val parsed = scanRepository.scanLabel(photoUri)
                if (!isActiveScanOrDeletePhoto(scanRequestId, photoUri)) return@launch
                finishActiveScan(scanRequestId, parsed, photoUri, onSuccess)
            }
        }

        private fun rejectScanWithoutPro() {
            invalidateActiveScan()
            _formState.update { it.copy(isScanning = false, scanError = null) }
        }

        private suspend fun hasQuotaForActiveScan(scanRequestId: Long): Boolean {
            val hasQuota = aiQuotaManager.hasQuota()
            if (!isActiveScan(scanRequestId)) return false
            if (hasQuota) return true
            _formState.update {
                it.copy(
                    isScanning = false,
                    scanError = context.getString(R.string.ai_quota_exhausted),
                )
            }
            return false
        }

        private suspend fun finishActiveScan(
            scanRequestId: Long,
            parsed: ParsedYarnLabel?,
            photoUri: Uri,
            onSuccess: () -> Unit,
        ) {
            if (parsed == null) {
                deletePhotoFile(photoUri.toString())
                _formState.update {
                    it.copy(
                        isScanning = false,
                        scanError = context.getString(R.string.yarn_scan_failed),
                    )
                }
                return
            }

            aiQuotaManager.recordCall()
            if (!isActiveScanOrDeletePhoto(scanRequestId, photoUri)) return
            loadFromScan(parsed, photoUri)
            onSuccess()
        }

        fun createScanPhotoUri(): Uri = scanRepository.createScanPhotoUri()

        fun saveCard(onSaved: (Long) -> Unit) {
            if (!proManager.hasFeature(ProFeature.OCR)) return
            viewModelScope.launch {
                val form = _formState.value.normalizedForPersistence()
                if (!form.canPersistYarnCard()) return@launch
                _formState.value = form
                val id = repository.saveCard(form.toDomain())
                onSaved(id)
            }
        }

        fun saveCardDomain(card: YarnCard) {
            viewModelScope.launch { repository.saveCard(card) }
        }

        fun deleteCard(
            id: Long,
            onDeleted: () -> Unit,
        ) {
            viewModelScope.launch {
                repository.deleteCard(id)
                onDeleted()
            }
        }

        fun discardScan() {
            deletePhotoFile(_formState.value.photoUri)
            clearFormState()
        }

        fun clearFormState() {
            invalidateActiveScan()
            _formState.value = YarnCardFormState()
        }

        fun deletePhotoFile(uriString: String) {
            scanRepository.deleteScanPhoto(uriString)
        }

        fun getCalculatorValues(): Triple<String, String, String> {
            val form = _formState.value
            return Triple(form.weightGrams, form.lengthMeters, form.needleSize)
        }

        fun updateQuantity(delta: Int) {
            val cardId = _formState.value.editingCardId ?: return
            val newQty = (_formState.value.quantityInStash + delta).coerceAtLeast(0)
            viewModelScope.launch { repository.updateQuantity(cardId, newQty) }
        }

        fun updateStatus(status: String) {
            if (!YarnCardStatus.isSupported(status)) return
            val cardId = _formState.value.editingCardId ?: return
            viewModelScope.launch { repository.updateStatus(cardId, status) }
        }

        fun setLinkedProject(projectId: Long?) {
            val cardId = _formState.value.editingCardId ?: return
            val previousProjectId = _formState.value.linkedProjectId
            if (previousProjectId == projectId) return

            viewModelScope.launch {
                repository.updateLinkedProjectId(cardId, projectId)
            }
        }

        private fun YarnCardFormState.toDomain() =
            YarnCard(
                id = editingCardId ?: 0,
                brand = brand,
                yarnName = yarnName,
                fiberContent = fiberContent,
                weightGrams = weightGrams,
                lengthMeters = lengthMeters,
                needleSize = needleSize,
                gaugeInfo = gaugeInfo,
                colorName = colorName,
                colorNumber = colorNumber,
                dyeLot = dyeLot,
                weightCategory = weightCategory,
                careSymbols = careSymbols,
                photoUri = photoUri,
                quantityInStash = quantityInStash,
                status = status,
                linkedProjectId = linkedProjectId,
            )

        private fun startScanRequest(): Long {
            activeScanRequestId += 1
            return activeScanRequestId
        }

        private fun invalidateActiveScan() {
            activeScanRequestId += 1
        }

        private fun isActiveScan(scanRequestId: Long): Boolean = activeScanRequestId == scanRequestId

        private fun isActiveScanOrDeletePhoto(
            scanRequestId: Long,
            photoUri: Uri,
        ): Boolean {
            if (isActiveScan(scanRequestId)) return true
            deletePhotoFile(photoUri.toString())
            return false
        }
    }

internal fun YarnCardFormState.canPersistYarnCard(): Boolean =
    hasYarnIdentity() &&
        weightGrams.isBlankOrPositiveInt() &&
        lengthMeters.isBlankOrPositiveInt()

internal fun YarnCardFormState.normalizedForPersistence(): YarnCardFormState =
    copy(
        brand = brand.trim(),
        yarnName = yarnName.trim(),
        fiberContent = fiberContent.trim(),
        weightGrams = weightGrams.trim(),
        lengthMeters = lengthMeters.trim(),
        needleSize = needleSize.trim(),
        gaugeInfo = gaugeInfo.trim(),
        colorName = colorName.trim(),
        colorNumber = colorNumber.trim(),
        dyeLot = dyeLot.trim(),
        weightCategory = weightCategory.trim(),
        photoUri = photoUri.trim(),
        quantityInStash = quantityInStash.coerceAtLeast(0),
        status = YarnCardStatus.normalize(status),
    )

private fun YarnCardFormState.hasYarnIdentity(): Boolean = brand.isNotBlank() || yarnName.isNotBlank()

private fun String.isBlankOrPositiveInt(): Boolean = isBlank() || toIntOrNull()?.let { it > 0 } == true
