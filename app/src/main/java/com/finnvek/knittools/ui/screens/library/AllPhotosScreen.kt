package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.screens.counter.PhotoViewer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data-luokat AllPhotosScreen-parametrien ryhmittelyyn (S107)
data class AllPhotosState(
    val photos: List<ProgressPhotoEntity>,
    val projects: List<CounterProjectEntity>,
    val isSelectMode: Boolean,
    val selectedPhotoIds: Set<Long>,
)

data class AllPhotosActions(
    val onDeletePhoto: (ProgressPhotoEntity) -> Unit,
    val onEnterSelectMode: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onSelectAll: (List<Long>) -> Unit,
    val onDeleteSelected: () -> Unit,
    val onExitSelectMode: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPhotosScreen(
    state: AllPhotosState,
    actions: AllPhotosActions,
) {
    var selectedProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val filteredPhotos =
        if (selectedProjectId != null) {
            state.photos.filter { it.projectId == selectedProjectId }
        } else {
            state.photos
        }

    // Poistu valintamoodista back-painikkeella
    BackHandler(enabled = state.isSelectMode) {
        actions.onExitSelectMode()
    }

    // PhotoViewer (vain normaalimoodissa)
    if (!state.isSelectMode) {
        viewingPhoto?.let { photo ->
            PhotoViewer(
                photo = photo,
                onDismiss = { viewingPhoto = null },
                onDelete = {
                    actions.onDeletePhoto(it)
                    viewingPhoto = null
                },
            )
        }
    }

    // Vahvistusdialoogi batch-poistolle
    if (showDeleteConfirmDialog) {
        AllPhotosDeleteDialog(
            selectedCount = state.selectedPhotoIds.size,
            onConfirm = {
                actions.onDeleteSelected()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AllPhotosTopBar(
                state = state,
                filteredPhotos = filteredPhotos,
                onExitSelectMode = actions.onExitSelectMode,
                onSelectAll = actions.onSelectAll,
                onBack = actions.onBack,
            )
        },
        bottomBar = {
            SelectModeDeleteBar(
                visible = state.isSelectMode && state.selectedPhotoIds.isNotEmpty(),
                onDeleteClick = { showDeleteConfirmDialog = true },
            )
        },
    ) { padding ->
        if (state.photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_all_photos),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            AllPhotosContent(
                state = state,
                actions = actions,
                filteredPhotos = filteredPhotos,
                selectedProjectId = selectedProjectId,
                onProjectFilterClick = { selectedProjectId = it },
                onPhotoClick = { viewingPhoto = it },
                padding = padding,
            )
        }
    }
}

@Composable
private fun AllPhotosDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.delete_photo),
        message = stringResource(R.string.delete_photos_confirm, selectedCount),
        confirmText = stringResource(R.string.delete),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllPhotosTopBar(
    state: AllPhotosState,
    filteredPhotos: List<ProgressPhotoEntity>,
    onExitSelectMode: () -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onBack: () -> Unit,
) {
    if (state.isSelectMode) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.n_selected, state.selectedPhotoIds.size),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onExitSelectMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            },
            actions = {
                TextButton(onClick = { onSelectAll(filteredPhotos.map { it.id }) }) {
                    Text(stringResource(R.string.select_all))
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.all_photos_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
        )
    }
}

@Composable
private fun AllPhotosContent(
    state: AllPhotosState,
    actions: AllPhotosActions,
    filteredPhotos: List<ProgressPhotoEntity>,
    selectedProjectId: Long?,
    onProjectFilterClick: (Long?) -> Unit,
    onPhotoClick: (ProgressPhotoEntity) -> Unit,
    padding: PaddingValues,
) {
    val projectMap = state.projects.associateBy { it.id }
    val projectsWithPhotos = state.photos.map { it.projectId }.distinct()

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Suodatinsiput piilotetaan valintamoodissa
        if (!state.isSelectMode) {
            ProjectFilterChips(
                projectsWithPhotos = projectsWithPhotos,
                projectMap = projectMap,
                selectedProjectId = selectedProjectId,
                onProjectFilterClick = onProjectFilterClick,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        PhotoGrid(
            filteredPhotos = filteredPhotos,
            state = state,
            actions = actions,
            projectMap = projectMap,
            onPhotoClick = onPhotoClick,
        )
    }
}

@Composable
private fun ProjectFilterChips(
    projectsWithPhotos: List<Long>,
    projectMap: Map<Long, CounterProjectEntity>,
    selectedProjectId: Long?,
    onProjectFilterClick: (Long?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedProjectId == null,
                onClick = { onProjectFilterClick(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(projectsWithPhotos) { projectId ->
            val name = projectMap[projectId]?.name ?: "Project $projectId"
            FilterChip(
                selected = selectedProjectId == projectId,
                onClick = { onProjectFilterClick(projectId) },
                label = {
                    Text(
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    filteredPhotos: List<ProgressPhotoEntity>,
    state: AllPhotosState,
    actions: AllPhotosActions,
    projectMap: Map<Long, CounterProjectEntity>,
    onPhotoClick: (ProgressPhotoEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filteredPhotos, key = { it.id }) { photo ->
            PhotoGridItem(
                photo = photo,
                projectName = projectMap[photo.projectId]?.name,
                isSelectMode = state.isSelectMode,
                isSelected = photo.id in state.selectedPhotoIds,
                onClick = {
                    if (state.isSelectMode) {
                        actions.onToggleSelection(photo.id)
                    } else {
                        onPhotoClick(photo)
                    }
                },
                onLongClick = {
                    if (!state.isSelectMode) {
                        actions.onEnterSelectMode(photo.id)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: ProgressPhotoEntity,
    projectName: String?,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    // Valittu kortti saa kevyen primäärivärisen taustan
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    Surface(
        modifier =
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
    ) {
        Box {
            Column {
                AsyncImage(
                    model = photo.photoUri,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    if (projectName != null) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = photo.note ?: stringResource(R.string.row_label_format, photo.rowNumber),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dateFormat.format(Date(photo.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isSelectMode) {
                SelectionIndicator(
                    isSelected = isSelected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
    }
}
