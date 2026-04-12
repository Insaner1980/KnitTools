package com.finnvek.knittools.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPhotosScreen(
    photos: List<ProgressPhotoEntity>,
    projects: List<CounterProjectEntity>,
    isSelectMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onDeletePhoto: (ProgressPhotoEntity) -> Unit,
    onEnterSelectMode: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onDeleteSelected: () -> Unit,
    onExitSelectMode: () -> Unit,
    onBack: () -> Unit,
) {
    var selectedProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val projectMap = projects.associateBy { it.id }
    val projectsWithPhotos = photos.map { it.projectId }.distinct()
    val filteredPhotos =
        if (selectedProjectId != null) {
            photos.filter { it.projectId == selectedProjectId }
        } else {
            photos
        }

    // Poistu valintamoodista back-painikkeella
    BackHandler(enabled = isSelectMode) {
        onExitSelectMode()
    }

    // PhotoViewer (vain normaalimoodissa)
    if (!isSelectMode) {
        viewingPhoto?.let { photo ->
            PhotoViewer(
                photo = photo,
                onDismiss = { viewingPhoto = null },
                onDelete = {
                    onDeletePhoto(it)
                    viewingPhoto = null
                },
            )
        }
    }

    // Vahvistusdialoogi batch-poistolle
    if (showDeleteConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_photo),
            message = stringResource(R.string.delete_photos_confirm, selectedPhotoIds.size),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                onDeleteSelected()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSelectMode) {
                // Valintamoodi: sulje-ikoni, "N selected", "Select All" -painike
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.n_selected, selectedPhotoIds.size),
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
                // Normaalinäkymä: sama tyyli kuin ToolScreenScaffold
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
        },
        bottomBar = {
            // Poistopalkki: näkyy vain valintamoodissa, kun jotain on valittu
            AnimatedVisibility(
                visible = isSelectMode && selectedPhotoIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (photos.isEmpty()) {
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Suodatinsiput piilotetaan valintamoodissa
                if (!isSelectMode) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedProjectId == null,
                                onClick = { selectedProjectId = null },
                                label = { Text(stringResource(R.string.filter_all)) },
                            )
                        }
                        items(projectsWithPhotos) { projectId ->
                            val name = projectMap[projectId]?.name ?: "Project $projectId"
                            FilterChip(
                                selected = selectedProjectId == projectId,
                                onClick = { selectedProjectId = projectId },
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
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        val isSelected = photo.id in selectedPhotoIds
                        PhotoGridItem(
                            photo = photo,
                            projectName = projectMap[photo.projectId]?.name,
                            isSelectMode = isSelectMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectMode) {
                                    onToggleSelection(photo.id)
                                } else {
                                    viewingPhoto = photo
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) {
                                    onEnterSelectMode(photo.id)
                                }
                            },
                        )
                    }
                }
            }
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

            // Valintaindikaattori-ikoni kuvan päällä oikeassa yläkulmassa
            if (isSelectMode) {
                val iconTint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(2.dp),
                ) {
                    Icon(
                        imageVector =
                            if (isSelected) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.Circle
                            },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
