package com.finnvek.knittools.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.finnvek.knittools.data.datastore.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hallitsee kontekstuaalisia tooltippejä. Jokainen tooltip näytetään kerran,
 * ja vain yksi kerrallaan.
 */
@Singleton
class TooltipManager
    @Inject
    constructor(
        private val preferencesManager: PreferencesManager,
    ) {
        val dismissedTooltips: Flow<Set<String>> = preferencesManager.dismissedTooltips

        private val _activeTooltipId = MutableStateFlow<String?>(null)
        val activeTooltipId: StateFlow<String?> = _activeTooltipId.asStateFlow()

        suspend fun shouldShow(id: String): Boolean {
            if (_activeTooltipId.value != null) return false
            val dismissed = preferencesManager.dismissedTooltips.first()
            return id !in dismissed
        }

        suspend fun show(id: String) {
            if (!shouldShow(id)) return
            _activeTooltipId.value = id
        }

        suspend fun dismiss(id: String) {
            preferencesManager.dismissTooltip(id)
            if (_activeTooltipId.value == id) {
                _activeTooltipId.value = null
            }
        }
    }

/**
 * Tooltip ID -vakiot.
 */
object TooltipIds {
    const val VOICE_COMMANDS = "voice_commands"
    const val LIBRARY_TAB = "library_tab"
    const val INSIGHTS_TAB = "insights_tab"
    const val SHAPING_COUNTER = "shaping_counter"
    const val SWIPE_PROJECT = "swipe_project"
    const val LONG_PRESS_PROJECT = "long_press_project"
    const val YARN_STASH = "yarn_stash"
    const val PHOTO_COUNTER = "photo_counter"
}

/**
 * Apukomponentti: näyttää tooltipin kerran, hävittää automaattisesti 5s jälkeen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnceTooltip(
    tooltipId: String,
    text: String,
    dismissed: Set<String>,
    onDismiss: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    if (tooltipId in dismissed) {
        content()
        return
    }

    val tooltipState = rememberTooltipState(isPersistent = false)

    LaunchedEffect(tooltipId) {
        tooltipState.show()
        delay(5000)
        tooltipState.dismiss()
        onDismiss(tooltipId)
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = tooltipState,
    ) {
        content()
    }
}
