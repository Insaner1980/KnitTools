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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.ui.components.ConfirmationDialog
import com.finnvek.knittools.ui.components.CrossStitchEmptyState
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.ProjectFilterOption
import com.finnvek.knittools.ui.components.ProjectFilterPill
import com.finnvek.knittools.ui.components.cardContainerColor
import com.finnvek.knittools.ui.components.rememberLocaleDateFormat
import com.finnvek.knittools.ui.layout.PhotoGridCells
import com.finnvek.knittools.ui.screens.counter.PhotoViewer
import java.util.Date

// Data-luokat AllPhotosScreen-parametrien ryhmittelyyn (S107)
data class AllPhotosState(
    val photos: List<ProgressPhoto>,
    val projects: List<CounterProject>,
    val isSelectMode: Boolean,
    val selectedPhotoIds: Set<Long>,
    val deleteErrorId: Long = 0L,
    val isLoading: Boolean = false,
)

data class AllPhotosActions(
    // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
    val onDeletePhoto: (ProgressPhoto) -> Unit,
    val onEnterSelectMode: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onSelectAll: (List<Long>) -> Unit,
    val onDeleteSelected: () -> Unit,
    val onExitSelectMode: () -> Unit,
    val onBack: () -> Unit,
    val deliverDeleteError: LibraryErrorDelivery,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPhotosScreen(
// CPD-ON
    state: AllPhotosState,
    actions: AllPhotosActions,
) {
    var selectedProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val deleteFailedMessage = stringResource(R.string.generic_error_unknown)
    val viewingPhoto = remember(viewingPhotoId, state.photos) { state.photos.firstOrNull { it.id == viewingPhotoId } }

    LaunchedEffect(selectedProjectId, state.photos, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val selectedId = selectedProjectId ?: return@LaunchedEffect
        if (state.photos.none { it.projectId == selectedId }) selectedProjectId = null
    }

    val filteredPhotos =
        remember(selectedProjectId, state.photos) {
            if (selectedProjectId != null) {
                state.photos.filter { it.projectId == selectedProjectId }
            } else {
                state.photos
            }
        }

    // Poistu valintamoodista back-painikkeella
    BackHandler(enabled = state.isSelectMode) {
        actions.onExitSelectMode()
    }

    LibraryErrorSnackbarEffect(state.deleteErrorId, actions.deliverDeleteError, snackbarHostState, deleteFailedMessage)

    // PhotoViewer (vain normaalimoodissa)
    if (!state.isSelectMode) {
        viewingPhoto?.let { photo ->
            PhotoViewer(
                photo = photo,
                onDismiss = { viewingPhotoId = null },
                onDelete = {
                    actions.onDeletePhoto(it)
                    viewingPhotoId = null
                },
            )
        }
    }

    // Vahvistusdialoogi batch-poistolle
    if (showDeleteConfirmDialog && state.selectedPhotoIds.isNotEmpty()) {
        AllPhotosDeleteDialog(
            selectedCount = state.selectedPhotoIds.size,
            onConfirm = {
                actions.onDeleteSelected()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }
    LaunchedEffect(state.selectedPhotoIds.isEmpty()) {
        if (state.selectedPhotoIds.isEmpty()) showDeleteConfirmDialog = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LibraryTopBar(
                isSelectMode = state.isSelectMode,
                selectedCount = state.selectedPhotoIds.size,
                titleRes = R.string.all_photos_title,
                onExitSelectMode = actions.onExitSelectMode,
                onSelectAll = { actions.onSelectAll(filteredPhotos.map { it.id }) },
                onBack = actions.onBack,
            )
        },
        snackbarHost = { LibrarySnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            SelectModeDeleteBar(
                visible = state.isSelectMode && state.selectedPhotoIds.isNotEmpty(),
                onDeleteClick = { showDeleteConfirmDialog = true },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.photos.isEmpty()) {
            CrossStitchEmptyState(
                glyph = CrossStitchGlyph.PHOTOS,
                // Sama otsikko kuin projektin galleriassa; selitys kertoo, mistä kuvat tulevat.
                title = stringResource(R.string.no_photos),
                message = stringResource(R.string.empty_all_photos),
                modifier = Modifier.padding(padding),
            )
        } else {
            AllPhotosContent(
                state = state,
                actions = actions,
                filteredPhotos = filteredPhotos,
                selectedProjectId = selectedProjectId,
                onProjectFilterClick = { selectedProjectId = it },
                onPhotoClick = { viewingPhotoId = it.id },
                padding = padding,
                gridState = gridState,
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
        title = stringResource(if (selectedCount == 1) R.string.delete_photo_title else R.string.delete_photos_title),
        message =
            if (selectedCount == 1) {
                stringResource(R.string.delete_photo_confirm)
            } else {
                pluralStringResource(R.plurals.delete_photos_confirm, selectedCount, selectedCount)
            },
        confirmText = stringResource(R.string.delete),
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun AllPhotosContent(
    state: AllPhotosState,
    actions: AllPhotosActions,
    filteredPhotos: List<ProgressPhoto>,
    selectedProjectId: Long?,
    onProjectFilterClick: (Long?) -> Unit,
    onPhotoClick: (ProgressPhoto) -> Unit,
    padding: PaddingValues,
    gridState: LazyGridState,
) {
    val projectMap = remember(state.projects) { state.projects.associateBy { it.id } }
    val projectsWithPhotos = remember(state.photos) { state.photos.map { it.projectId }.distinct() }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Suodatin piilotetaan valintamoodissa
        if (!state.isSelectMode) {
            // Sama projektipilleri kuin Insightsissa: sirurivi ei skaalautunut projektimäärän kasvaessa.
            ProjectFilterPill(
                projects =
                    projectsWithPhotos.map { projectId ->
                        ProjectFilterOption(
                            id = projectId,
                            name =
                                projectMap[projectId]?.name
                                    ?: stringResource(R.string.new_project_name_format, projectId),
                        )
                    },
                selectedProjectId = selectedProjectId,
                onSelectProject = onProjectFilterClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        PhotoGrid(
            filteredPhotos = filteredPhotos,
            state = state,
            actions = actions,
            projectMap = projectMap,
            onPhotoClick = onPhotoClick,
            gridState = gridState,
        )
    }
}

@Composable
private fun PhotoGrid(
    filteredPhotos: List<ProgressPhoto>,
    state: AllPhotosState,
    actions: AllPhotosActions,
    projectMap: Map<Long, CounterProject>,
    onPhotoClick: (ProgressPhoto) -> Unit,
    gridState: LazyGridState,
) {
    LazyVerticalGrid(
        columns = PhotoGridCells,
        state = gridState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filteredPhotos, key = { it.id }) { photo ->
            val projectName =
                projectMap[photo.projectId]?.name ?: stringResource(R.string.new_project_name_format, photo.projectId)
            PhotoGridItem(
                photo = photo,
                projectName = projectName,
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
    photo: ProgressPhoto,
    projectName: String,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dateFormat = rememberLocaleDateFormat("MMMd")

    val backgroundColor = cardContainerColor(selected = isSelected)

    Surface(
        modifier =
            Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick.takeUnless { isSelectMode },
                    onLongClickLabel = if (isSelectMode) null else stringResource(R.string.select_photo),
                ).then(
                    if (isSelectMode) {
                        Modifier.semantics { selected = isSelected }
                    } else {
                        Modifier
                    },
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
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = photo.note ?: stringResource(R.string.photo_count_label_format, photo.rowNumber),
                        style = MaterialTheme.typography.bodySmall,
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
