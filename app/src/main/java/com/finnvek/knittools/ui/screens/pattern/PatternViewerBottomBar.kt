package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.ui.components.ReadingLineNotice
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.layout.LocalAdaptiveContentSize
import com.finnvek.knittools.ui.navigation.LocalEntryActionsBlocked
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.theme.PatternViewerDimens

@Composable
internal fun ProjectReadingLineNotice(
    document: ProjectDocument?,
    blocked: Boolean,
    counterViewModel: CounterViewModel,
) {
    val onboarding by counterViewModel.onboardingState.collectAsStateWithLifecycle(initialValue = null)
    ReadingLineNotice(
        hasMainCounter = document != null,
        following = document?.readingLineFollowCurrentRow == true,
        dismissed = onboarding?.readingLineNoticeDismissed != false,
        blocked = blocked || LocalEntryActionsBlocked.current,
        onDismiss = counterViewModel::dismissReadingLineNotice,
        onEnable = {
            document?.let {
                counterViewModel.setReadingLineFollowCurrentRow(true, it.id, fromNotice = true)
            }
        },
    )
}

internal data class PatternViewerBottomBarState(
    val countSlot: MainCounterCountSlot,
    val currentPage: Int,
    val totalPages: Int,
)

internal data class PatternViewerBottomBarActions(
    val onPreviousRow: () -> Unit,
    val onNextRow: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
    val onOpenBookmarks: () -> Unit,
)

@Composable
internal fun PatternViewerBottomBar(
    state: PatternViewerBottomBarState,
    actions: PatternViewerBottomBarActions,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = PatternViewerDimens.BottomBarHorizontalPadding,
                        vertical =
                            PatternViewerDimens.bottomBarVerticalPadding(
                                LocalAdaptiveContentSize.current.isCompactHeight,
                                library = false,
                            ),
                    ),
        ) {
            val textMeasurer = rememberTextMeasurer()
            val style = MaterialTheme.typography.bodyMedium
            val countSample = mainCounterCountText(state.countSlot.copy(count = 99_999))
            val pageSample = stringResource(R.string.pattern_page_indicator_compact, 999, 999)
            val labelWidth =
                with(LocalDensity.current) {
                    (
                        textMeasurer.measure(countSample, style, maxLines = 1).size.width +
                            textMeasurer.measure(pageSample, style, maxLines = 1).size.width
                    ).toDp()
                }
            val minimumWidth =
                labelWidth + PatternViewerDimens.BottomBarButtonSize * 5 + PatternViewerDimens.BottomBarCountSpacing
            if (maxWidth >= minimumWidth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BottomBarCountControls(state = state, actions = actions, modifier = Modifier.weight(1f))
                    BottomBarPageControls(state = state, actions = actions)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(PatternViewerDimens.BottomBarRowSpacing)) {
                    BottomBarCountControls(state = state, actions = actions)
                    BottomBarPageControls(state = state, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun BottomBarCountControls(
    state: PatternViewerBottomBarState,
    actions: PatternViewerBottomBarActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mainCounterCountText(state.countSlot),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(PatternViewerDimens.BottomBarCountSpacing))
        IconButton(
            onClick = actions.onPreviousRow,
            modifier = Modifier.size(PatternViewerDimens.BottomBarButtonSize),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.counter_decrease),
            )
        }
        IconButton(
            onClick = actions.onNextRow,
            modifier = Modifier.size(PatternViewerDimens.BottomBarButtonSize),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.counter_increase),
            )
        }
    }
}

@Composable
private fun BottomBarPageControls(
    state: PatternViewerBottomBarState,
    actions: PatternViewerBottomBarActions,
) {
    val page = state.currentPage + 1
    val total = state.totalPages.coerceAtLeast(1)
    val pageDescription = stringResource(R.string.pattern_page_indicator, page, total)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = actions.onPreviousPage,
            modifier = Modifier.size(PatternViewerDimens.BottomBarButtonSize),
            enabled = state.currentPage > 0,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.pattern_previous_page),
            )
        }
        Text(
            text = stringResource(R.string.pattern_page_indicator_compact, page, total),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics { contentDescription = pageDescription },
        )
        IconButton(
            onClick = actions.onNextPage,
            modifier = Modifier.size(PatternViewerDimens.BottomBarButtonSize),
            enabled = state.currentPage < state.totalPages - 1,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.pattern_next_page),
            )
        }
        IconButton(
            onClick = actions.onOpenBookmarks,
            modifier = Modifier.size(PatternViewerDimens.BottomBarButtonSize),
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.pattern_bookmarks),
            )
        }
    }
}
