package com.finnvek.knittools.ui.screens.yarncard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YarnCardFormState(
    // CPD-OFF: Lomaketila peilaa lankakortin pysyvat kentat ja lisaa vain UI:n tilat.
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
    val quantityInStash: Int = 1,
    val status: String = YarnCardStatus.IN_STASH,
    val linkedProjectId: Long? = null,
    // CPD-ON
)

@HiltViewModel
@Suppress("TooManyFunctions")
class YarnCardViewModel
    @Inject
    constructor(
        private val repository: YarnCardRepository,
        private val counterRepository: CounterRepository,
    ) : ViewModel() {
        private val _formState = MutableStateFlow(YarnCardFormState())
        val formState: StateFlow<YarnCardFormState> = _formState.asStateFlow()

        val savedCards: StateFlow<List<YarnCard>> =
            repository.getAllCards().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

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
        fun linkCardToProject(
            cardId: Long,
            projectId: Long,
        ) {
            viewModelScope.launch {
                repository.updateLinkedProjectId(cardId, projectId)
            }
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

        fun deleteCard(
            id: Long,
            onDeleted: () -> Unit,
        ) {
            viewModelScope.launch {
                repository.deleteCard(id)
                onDeleted()
            }
        }

        fun clearFormState() {
            _formState.value = YarnCardFormState()
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
    }
