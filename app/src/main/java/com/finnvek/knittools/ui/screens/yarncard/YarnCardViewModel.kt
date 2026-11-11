package com.finnvek.knittools.ui.screens.yarncard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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

        private val mutationErrorsChannel = Channel<Unit>(Channel.BUFFERED)
        val mutationErrors = mutationErrorsChannel.receiveAsFlow()

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

        fun observeCardForDetail(id: Long): Flow<YarnCard?> = repository.observeCard(id)

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
            onFailure: () -> Unit = {},
            onDeleted: () -> Unit,
        ) {
            launchCardMutation(onFailure) {
                repository.deleteCard(id).also { deleted -> if (deleted) onDeleted() }
            }
        }

        fun clearFormState() {
            _formState.value = YarnCardFormState()
        }

        fun updateQuantity(delta: Int) {
            val cardId = _formState.value.editingCardId ?: return
            launchCardMutation { repository.changeQuantity(cardId, delta) }
        }

        fun updateStatus(status: String) {
            if (!YarnCardStatus.isSupported(status)) return
            val cardId = _formState.value.editingCardId ?: return
            launchCardMutation { repository.updateStatus(cardId, status) }
        }

        fun updatePhotoUri(
            uri: Uri,
            onFailure: () -> Unit = {},
        ) {
            val cardId = _formState.value.editingCardId ?: return
            viewModelScope.launch {
                try {
                    if (!repository.updatePhotoUri(cardId, uri)) onFailure()
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    onFailure()
                }
            }
        }

        fun setLinkedProject(projectId: Long?) {
            val cardId = _formState.value.editingCardId ?: return
            val previousProjectId = _formState.value.linkedProjectId
            if (previousProjectId == projectId) return

            launchCardMutation {
                repository.updateLinkedProjectId(
                    cardId,
                    projectId,
                    expectedLinkedProjectId = previousProjectId.takeIf { projectId == null },
                )
            }
        }

        fun updateManualDetails(input: ManualYarnCardInput) {
            val form = _formState.value
            val cardId = form.editingCardId ?: return
            val yarnName = input.yarnName.trim()
            if (yarnName.isBlank()) return

            launchCardMutation {
                repository.saveCard(
                    YarnCard(
                        id = cardId,
                        brand = input.brand.trim(),
                        yarnName = yarnName,
                        fiberContent = form.fiberContent,
                        weightGrams = form.weightGrams,
                        lengthMeters = form.lengthMeters,
                        needleSize = form.needleSize,
                        gaugeInfo = form.gaugeInfo,
                        colorName = input.colorName.trim(),
                        colorNumber = input.colorNumber.trim(),
                        dyeLot = input.dyeLot.trim(),
                        weightCategory = input.weightCategory.trim(),
                        careSymbols = form.careSymbols,
                        photoUri = form.photoUri,
                        quantityInStash = form.quantityInStash,
                        status = form.status,
                        linkedProjectId = form.linkedProjectId,
                    ),
                ) != null
            }
        }

        private fun launchCardMutation(
            onFailure: () -> Unit = {},
            operation: suspend () -> Boolean,
        ) {
            viewModelScope.launch {
                val succeeded =
                    try {
                        operation()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        false
                    }
                if (!succeeded) {
                    onFailure()
                    mutationErrorsChannel.send(Unit)
                }
            }
        }
    }
