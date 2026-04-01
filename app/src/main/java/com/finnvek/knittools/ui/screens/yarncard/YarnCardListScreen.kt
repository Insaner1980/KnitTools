package com.finnvek.knittools.ui.screens.yarncard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun YarnCardListScreen(
    viewModel: YarnCardViewModel,
    onCardClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val cards by viewModel.savedCards.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ToolScreenScaffold(title = stringResource(R.string.saved_yarns), onBack = onBack) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (cards.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.yarn_card_empty),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(200.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_saved_yarns),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cards, key = { it.id }) { card ->
                        SwipeToDeleteCard(
                            card = card,
                            onClick = { onCardClick(card.id) },
                            onDelete = {
                                viewModel.deleteCard(card.id) {
                                    scope.launch {
                                        val result =
                                            snackbarHostState.showSnackbar(
                                                message = "Yarn card deleted",
                                                actionLabel = "Undo",
                                            )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.saveCardEntity(card)
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun SwipeToDeleteCard(
    card: YarnCardEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = false,
    ) {
        YarnCardRow(card = card, onClick = onClick)
    }
}

@Composable
private fun YarnCardRow(
    card: YarnCardEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val title =
                listOfNotNull(
                    card.brand.takeIf { it.isNotBlank() },
                    card.yarnName.takeIf { it.isNotBlank() },
                ).joinToString(" — ").ifBlank { "Yarn card" }

            Text(text = title, style = MaterialTheme.typography.titleSmall)

            if (card.fiberContent.isNotBlank()) {
                Text(
                    text = card.fiberContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (card.weightCategory.isNotBlank()) {
                Text(
                    text = card.weightCategory,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
