package com.finnvek.knittools.ui.screens.counter

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.takeWithoutSplittingSurrogatePair
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.CrossStitchEmptyState
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.LabeledCounterImageButton
import com.finnvek.knittools.ui.components.LabeledTextField
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.ScrollableFormDialog
import com.finnvek.knittools.ui.components.proMarkerDescription
import com.finnvek.knittools.ui.components.rememberLocaleDateFormat
import com.finnvek.knittools.ui.components.withExtraBottom
import com.finnvek.knittools.ui.layout.PhotoGridCells
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Date

data class PhotoGalleryActions(
    val authorizePhotoCreation: () -> Unit,
    val cancelPhotoCreation: () -> Unit,
    val createPhotoCaptureTarget: (Long, (PhotoCaptureTarget?) -> Unit) -> Unit,
    val deletePendingPhotoFile: (String?) -> Unit,
    val savePhoto: (Uri, Long?) -> Boolean,
    val deletePhoto: (ProgressPhoto) -> Unit,
    val updateNote: (Long, String?) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    photos: List<ProgressPhoto>,
    projectId: Long?,
    canCreatePhoto: Boolean,
    proState: ProState,
    proAccessReady: Boolean,
    onBack: () -> Unit,
    onSeePro: () -> Unit,
    actions: PhotoGalleryActions,
    errorsProvider: () -> Flow<Unit> = { emptyFlow() },
) {
    val photoAccess = proActionAccess(proState, proAccessReady, ProFeature.PROGRESS_PHOTOS)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val errorMessage = stringResource(R.string.generic_error_unknown)
    CollectWithLifecycleEffect({ errorsProvider() }) {
        snackbarHostState.showSnackbar(errorMessage)
    }
    var creatingCapture by remember { mutableStateOf(false) }
    var captureHostActive by remember { mutableStateOf(true) }
    val currentProjectId by rememberUpdatedState(projectId)
    DisposableEffect(Unit) { onDispose { captureHostActive = false } }
    var pendingPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoProjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renamingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    val renamingPhoto = remember(renamingPhotoId, photos) { photos.firstOrNull { it.id == renamingPhotoId } }
    val viewingPhoto = remember(viewingPhotoId, photos) { photos.firstOrNull { it.id == viewingPhotoId } }
    var pendingProProjectId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun clearPendingCapture() {
        pendingPhotoUriString = null
        pendingPhotoFilePath = null
        pendingPhotoProjectId = null
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            handlePhotoCaptureResult(
                success = success,
                pendingPhotoUri = pendingPhotoUriString?.toUri(),
                pendingPhotoFilePath = pendingPhotoFilePath,
                pendingPhotoProjectId = pendingPhotoProjectId,
                actions = actions,
            )
            clearPendingCapture()
        }

    fun startCameraCapture(ownerProjectId: Long?) {
        if (creatingCapture || pendingPhotoUriString != null) return
        creatingCapture = true
        requestCameraCaptureTarget(ownerProjectId, actions) { captureTarget ->
            creatingCapture = false
            if (captureTarget == null) return@requestCameraCaptureTarget
            if (!captureHostActive || currentProjectId != ownerProjectId) {
                actions.deletePendingPhotoFile(captureTarget.filePath)
                actions.cancelPhotoCreation()
                return@requestCameraCaptureTarget
            }

            fun abandonCapture() {
                actions.deletePendingPhotoFile(captureTarget.filePath)
                actions.cancelPhotoCreation()
                clearPendingCapture()
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }

            pendingPhotoUriString = captureTarget.uri.toString()
            pendingPhotoFilePath = captureTarget.filePath
            pendingPhotoProjectId = ownerProjectId
            try {
                cameraLauncher.launch(captureTarget.uri)
            } catch (_: ActivityNotFoundException) {
                abandonCapture()
            } catch (_: IllegalStateException) {
                abandonCapture()
            } catch (_: SecurityException) {
                abandonCapture()
            }
        }
    }

    fun launchCamera() {
        if (creatingCapture || pendingPhotoUriString != null) return
        val ownerProjectId = projectId ?: return
        requestPhotoCapture(
            canCreatePhoto = canCreatePhoto,
            actions = actions,
            onAuthorized = {
                startCameraCapture(ownerProjectId)
            },
            onProRequired = { pendingProProjectId = ownerProjectId },
        )
    }

    pendingProProjectId?.takeIf { it == projectId }?.let { ownerProjectId ->
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.ProgressPhotos,
                ),
            onDismiss = { pendingProProjectId = null },
            onTrialStarted = {
                pendingProProjectId = null
                if (currentProjectId == ownerProjectId) {
                    actions.authorizePhotoCreation()
                    startCameraCapture(ownerProjectId)
                }
            },
            onSeePro = onSeePro,
        )
    }

    // Koko näytön kuvankatselija
    viewingPhoto?.let { photo ->
        PhotoViewer(
            photo = photo,
            onDismiss = { viewingPhotoId = null },
            onDelete = { actions.deletePhoto(it) },
        )
    }

    // Nimeämisdialogi
    renamingPhoto?.let { photo ->
        RenamePhotoDialog(
            currentNote = photo.note ?: "",
            onConfirm = { newNote ->
                actions.updateNote(photo.id, newNote.ifBlank { null })
                renamingPhotoId = null
            },
            onDismiss = { renamingPhotoId = null },
        )
    }

    val density = LocalDensity.current
    var actionHeight by remember { mutableStateOf(0.dp) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // CPD-OFF: Ruudun paikallinen Compose-rakenne pidetaan vastuun yhteydessa.
                        text = stringResource(R.string.progress_photos),
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
                // CPD-ON
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.knitToolsColors.transparentIndicator,
                    ),
            )
        },
        floatingActionButton = {
            if (projectId == null) return@Scaffold
            // Sama lisäyspilleri kuin New Project, Add Yarn ja Add web pattern.
            val photoLabel = stringResource(R.string.take_photo)
            val proDescription = proMarkerDescription(photoAccess)
            LabeledCounterImageButton(
                imageRes = R.drawable.counter_plus_button,
                label = photoLabel,
                visualSize = ProjectListDimens.CreateButtonVisualSize,
                onClick = { launchCamera() },
                modifier = Modifier.onSizeChanged { actionHeight = with(density) { it.height.toDp() } },
                badgeContentDescription = proDescription,
                badge = proDescription?.let { { ProMarker(photoAccess) } },
            )
        },
    ) { padding ->
        PhotoGalleryContent(
            photos = photos,
            padding =
                padding.withExtraBottom(actionHeight + 32.dp, LocalLayoutDirection.current),
            onPhotoClick = { viewingPhotoId = it.id },
            onPhotoLongClick = { renamingPhotoId = it.id },
            gridState = gridState,
        )
    }
}

private fun requestPhotoCapture(
    canCreatePhoto: Boolean,
    actions: PhotoGalleryActions,
    onAuthorized: () -> Unit,
    onProRequired: () -> Unit,
) {
    if (canCreatePhoto) {
        actions.authorizePhotoCreation()
        onAuthorized()
    } else {
        onProRequired()
    }
}

private fun handlePhotoCaptureResult(
    success: Boolean,
    pendingPhotoUri: Uri?,
    pendingPhotoFilePath: String?,
    pendingPhotoProjectId: Long?,
    actions: PhotoGalleryActions,
) {
    val admitted = success && pendingPhotoUri?.let { actions.savePhoto(it, pendingPhotoProjectId) } == true
    if (!admitted) {
        actions.deletePendingPhotoFile(pendingPhotoFilePath)
        actions.cancelPhotoCreation()
    }
}

private fun requestCameraCaptureTarget(
    projectId: Long?,
    actions: PhotoGalleryActions,
    onCaptureTargetReady: (PhotoCaptureTarget?) -> Unit,
) {
    val id =
        projectId ?: run {
            actions.cancelPhotoCreation()
            onCaptureTargetReady(null)
            return
        }
    actions.createPhotoCaptureTarget(id) { captureTarget ->
        if (captureTarget == null) {
            actions.cancelPhotoCreation()
        }
        onCaptureTargetReady(captureTarget)
    }
}

@Composable
private fun PhotoGalleryContent(
    photos: List<ProgressPhoto>,
    padding: PaddingValues,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onPhotoLongClick: (ProgressPhoto) -> Unit,
    gridState: LazyGridState,
) {
    if (photos.isEmpty()) {
        EmptyPhotoGallery(padding)
    } else {
        PhotoGalleryGrid(
            photos = photos,
            padding = padding,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
            gridState = gridState,
        )
    }
}

// Sama Photos-ristipisto kuin laskurin laatassa; lisäys on alareunan Take photo -pillerissä.
@Composable
private fun EmptyPhotoGallery(padding: PaddingValues) {
    CrossStitchEmptyState(
        glyph = CrossStitchGlyph.PHOTOS,
        title = stringResource(R.string.no_photos),
        // Selitys eikä "Take photo": sama teksti oli jo alareunan pillerissä.
        message = stringResource(R.string.photo_gallery_empty_body),
        modifier = Modifier.padding(padding),
    )
}

@Composable
private fun PhotoGalleryGrid(
    photos: List<ProgressPhoto>,
    padding: PaddingValues,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onPhotoLongClick: (ProgressPhoto) -> Unit,
    gridState: LazyGridState,
) {
    LazyVerticalGrid(
        columns = PhotoGridCells,
        state = gridState,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoGridItem(
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onLongClick = { onPhotoLongClick(photo) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: ProgressPhoto,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val dateFormat = rememberLocaleDateFormat("MMMd")
    val displayName = photo.note ?: stringResource(R.string.photo_count_label_format, photo.rowNumber)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.knitToolsColors.cardContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(3.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium),
            ) {
                AsyncImage(
                    model = photo.photoUri.toUri(),
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text(
                text = dateFormat.format(Date(photo.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RenamePhotoDialog(
    currentNote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(currentNote) { mutableStateOf(currentNote) }

    ScrollableFormDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_photo)) },
        text = {
            LabeledTextField(
                value = text,
                onValueChange = { text = it.takeWithoutSplittingSurrogatePair(100) },
                label = stringResource(R.string.photo_name_label),
                placeholder = stringResource(R.string.photo_name_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            CancelButton(onClick = onDismiss)
        },
    )
}
