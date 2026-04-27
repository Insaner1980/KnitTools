package com.finnvek.knittools.ui.screens.yarncard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.R
import com.finnvek.knittools.ai.AiQuotaManager
import com.finnvek.knittools.ai.ocr.ParsedYarnLabel
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.YarnLabelScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val status: String = "IN_STASH",
    val linkedProjectId: Long? = null,
)

@HiltViewModel
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

        val savedCards: StateFlow<List<YarnCardEntity>> =
            repository.getAllCards().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        val isPro: Boolean get() = proManager.hasFeature(ProFeature.OCR)

        val availableProjects: StateFlow<List<CounterProjectEntity>> =
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
                val project = counterRepository.getProject(projectId) ?: return@launch
                val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (cardId in currentIds) return@launch
                val newIds = (currentIds + cardId).joinToString(",")
                counterRepository.updateProjectYarnCardIds(projectId, newIds)
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
            viewModelScope.launch {
                repository.getCard(id)?.let { loadFromCard(it) }
            }
        }

        fun loadFromCard(card: YarnCardEntity) {
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
            viewModelScope.launch {
                _formState.update { it.copy(isScanning = true, scanError = null) }

                if (!aiQuotaManager.hasQuota()) {
                    _formState.update {
                        it.copy(
                            isScanning = false,
                            scanError = context.getString(R.string.ai_quota_exhausted),
                        )
                    }
                    return@launch
                }

                val parsed = scanRepository.scanLabel(photoUri)
                if (parsed != null) {
                    aiQuotaManager.recordCall()
                    loadFromScan(parsed, photoUri)
                    onSuccess()
                } else {
                    _formState.update {
                        it.copy(
                            isScanning = false,
                            scanError = context.getString(R.string.yarn_scan_failed),
                        )
                    }
                }
            }
        }

        fun createScanPhotoUri(): Uri = scanRepository.createScanPhotoUri()

        fun saveCard(onSaved: (Long) -> Unit) {
            if (!proManager.hasFeature(ProFeature.OCR)) return
            viewModelScope.launch {
                val form = _formState.value
                val id = repository.saveCard(form.toEntity())
                onSaved(id)
            }
        }

        fun saveCardEntity(card: YarnCardEntity) {
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
            _formState.update { it.copy(quantityInStash = newQty) }
            viewModelScope.launch { repository.updateQuantity(cardId, newQty) }
        }

        fun updateStatus(status: String) {
            val cardId = _formState.value.editingCardId ?: return
            _formState.update { it.copy(status = status) }
            viewModelScope.launch { repository.updateStatus(cardId, status) }
        }

        fun setLinkedProject(projectId: Long?) {
            val cardId = _formState.value.editingCardId ?: return
            val previousProjectId = _formState.value.linkedProjectId
            if (previousProjectId == projectId) return

            viewModelScope.launch {
                previousProjectId?.let { oldProjectId ->
                    counterRepository.getProject(oldProjectId)?.let { project ->
                        val remainingIds =
                            project.yarnCardIds
                                .split(",")
                                .mapNotNull { it.trim().toLongOrNull() }
                                .filter { it != cardId }
                                .joinToString(",")
                        counterRepository.updateProjectYarnCardIds(oldProjectId, remainingIds)
                    }
                }

                projectId?.let { newProjectId ->
                    counterRepository.getProject(newProjectId)?.let { project ->
                        val currentIds = project.yarnCardIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                        if (cardId !in currentIds) {
                            counterRepository.updateProjectYarnCardIds(
                                newProjectId,
                                (currentIds + cardId).joinToString(","),
                            )
                        }
                    }
                }

                repository.updateLinkedProjectId(cardId, projectId)
                _formState.update { it.copy(linkedProjectId = projectId) }
            }
        }

        private fun YarnCardFormState.toEntity() =
            YarnCardEntity(
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
    }
