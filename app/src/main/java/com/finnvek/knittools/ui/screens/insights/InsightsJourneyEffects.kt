package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

// Konteksti, hero, tilastot, trendi ja kaavion otsikko edeltävät kaaviota.
private const val WEEKLY_CHART_ITEM_INDEX = 5

@Composable
internal fun InsightsJourneyEffects(
    state: InsightsUiState,
    listState: LazyListState,
    requestId: String?,
    onPrepareWeeklyChart: () -> Unit,
    onRequestHandled: (String) -> Unit,
    onChartVisible: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentState by rememberUpdatedState(state)
    val currentPrepare by rememberUpdatedState(onPrepareWeeklyChart)
    val currentHandled by rememberUpdatedState(onRequestHandled)
    val currentVisible by rememberUpdatedState(onChartVisible)
    LaunchedEffect(requestId, lifecycleOwner) {
        val id = requestId ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            currentPrepare()
            val ready =
                snapshotFlow { currentState }.first {
                    !it.isLoading && it.timeRange == TimeRange.THIS_WEEK && it.selectedProjectId == null
                }
            if (ready.totalMinutes > 0 && ready.hasMeaningfulChartData) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > WEEKLY_CHART_ITEM_INDEX }
                listState.scrollToItem(WEEKLY_CHART_ITEM_INDEX)
            } else {
                listState.scrollToItem(0)
            }
            currentHandled(id)
        }
    }
    LaunchedEffect(state.isLoading, state.timeRange, state.isPro, state.hasMeaningfulChartData, lifecycleOwner) {
        if (state.isLoading) return@LaunchedEffect
        if (!state.isPro || !state.hasMeaningfulChartData) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow {
                val layout = listState.layoutInfo
                layout.visibleItemsInfo.any {
                    it.key == "chart" &&
                        it.offset < layout.viewportEndOffset &&
                        it.offset + it.size > layout.viewportStartOffset
                }
            }.filter { it }.collect { currentVisible() }
        }
    }
}
