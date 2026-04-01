package com.finnvek.knittools.ui.screens.yarncard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.ai.ocr.ParsedYarnLabel
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.YarnCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YarnCardFormState(
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
)

@HiltViewModel
class YarnCardViewModel
    @Inject
    constructor(
        private val repository: YarnCardRepository,
        private val proManager: ProManager,
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

        fun loadFromScan(parsed: ParsedYarnLabel, photoUri: Uri?) {
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
                    photoUri = photoUri?.toString() ?: "",
                )
        }

        fun loadFromCard(card: YarnCardEntity) {
            _formState.value =
                YarnCardFormState(
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
                )
        }

        fun updateField(updater: YarnCardFormState.() -> YarnCardFormState) {
            _formState.update { it.updater() }
        }

        fun setScanning(scanning: Boolean) {
            _formState.update { it.copy(isScanning = scanning) }
        }

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

        fun deleteCard(id: Long, onDeleted: () -> Unit) {
            viewModelScope.launch {
                repository.deleteCard(id)
                onDeleted()
            }
        }

        fun getCalculatorValues(): Triple<String, String, String> {
            val form = _formState.value
            return Triple(form.weightGrams, form.lengthMeters, form.needleSize)
        }

        private fun YarnCardFormState.toEntity() =
            YarnCardEntity(
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
            )
    }
