package com.finnvek.knittools.ui.screens.counter

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.components.rememberLocaleDateFormat
import java.util.Date

private enum class PendingPhotoProAction {
    Capture,
}

data class PhotoGalleryActions(
    val authorizePhotoCreation: () -> Unit,
    val cancelPhotoCreation: () -> Unit,
    val createPhotoCaptureTarget: (Long, (PhotoCaptureTarget?) -> Unit) -> Unit,
    val deletePendingPhotoFile: (String?) -> Unit,
    val savePhoto: (Uri) -> Unit,
    val deletePhoto: (ProgressPhoto) -> Unit,
    val updateNote: (Long, String?) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    photos: List<ProgressPhoto>,
    projectId: Long?,
    canCreatePhoto: Boolean,
    proStatus: ProStatus,
    onBack: () -> Unit,
    onSeePro: () -> Unit,
    actions: PhotoGalleryActions,
) {
    val context = LocalContext.current
    var pendingPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingPhotoUri = pendingPhotoUriString?.toUri()
    var renamingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    val renamingPhoto = remember(renamingPhotoId, photos) { photos.firstOrNull { it.id == renamingPhotoId } }
    val viewingPhoto = remember(viewingPhotoId, photos) { photos.firstOrNull { it.id == viewingPhotoId } }
    val cameraPermissionDeniedMessage = stringResource(R.string.camera_permission_denied)
    val cameraPermissionDeniedPermanentMessage = stringResource(R.string.camera_permission_denied_permanent)
    var pendingProAction by rememberSaveable { mutableStateOf<PendingPhotoProAction?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            handlePhotoCaptureResult(
                success = success,
                pendingPhotoUri = pendingPhotoUri,
                pendingPhotoFilePath = pendingPhotoFilePath,
                actions = actions,
            )
            pendingPhotoUriString = null
            pendingPhotoFilePath = null
            if (!success) actions.cancelPhotoCreation()
        }

    fun startCameraCapture() {
        requestCameraCaptureTarget(projectId, actions) { captureTarget ->
            pendingPhotoUriString = captureTarget.uri.toString()
            pendingPhotoFilePath = captureTarget.filePath
            try {
                cameraLauncher.launch(captureTarget.uri)
            } catch (_: ActivityNotFoundException) {
                actions.deletePendingPhotoFile(captureTarget.filePath)
                actions.cancelPhotoCreation()
                pendingPhotoUriString = null
                pendingPhotoFilePath = null
            } catch (_: IllegalStateException) {
                actions.deletePendingPhotoFile(captureTarget.filePath)
                actions.cancelPhotoCreation()
                pendingPhotoUriString = null
                pendingPhotoFilePath = null
            } catch (_: SecurityException) {
                actions.deletePendingPhotoFile(captureTarget.filePath)
                actions.cancelPhotoCreation()
                pendingPhotoUriString = null
                pendingPhotoFilePath = null
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            handleCameraPermissionResult(
                granted = granted,
                context = context,
                deniedMessage = cameraPermissionDeniedMessage,
                permanentlyDeniedMessage = cameraPermissionDeniedPermanentMessage,
                onGranted = ::startCameraCapture,
            )
            if (!granted) actions.cancelPhotoCreation()
        }

    fun launchCamera() {
        requestPhotoCapturePermission(
            canCreatePhoto = canCreatePhoto,
            actions = actions,
            onAuthorized = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onProRequired = { pendingProAction = PendingPhotoProAction.Capture },
        )
    }

    pendingProAction?.let {
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.ProgressPhotos,
                ),
            onDismiss = { pendingProAction = null },
            onTrialStarted = {
                pendingProAction = null
                actions.authorizePhotoCreation()
                permissionLauncher.launch(Manifest.permission.CAMERA)
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

    Scaffold(
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
                        containerColor = Color.Transparent,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launchCamera() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = stringResource(R.string.take_photo),
                    )
                    ProBadge(status = proStatus)
                }
            }
        },
    ) { padding ->
        PhotoGalleryContent(
            photos = photos,
            padding = padding,
            onPhotoClick = { viewingPhotoId = it.id },
            onPhotoLongClick = { renamingPhotoId = it.id },
        )
    }
}

private fun requestPhotoCapturePermission(
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
    actions: PhotoGalleryActions,
) {
    if (success) {
        pendingPhotoUri?.let(actions.savePhoto)
    } else {
        actions.deletePendingPhotoFile(pendingPhotoFilePath)
    }
}

private fun requestCameraCaptureTarget(
    projectId: Long?,
    actions: PhotoGalleryActions,
    onCaptureTargetReady: (PhotoCaptureTarget) -> Unit,
) {
    val id = projectId ?: return
    actions.createPhotoCaptureTarget(id) { captureTarget ->
        captureTarget?.let(onCaptureTargetReady)
    }
}

private fun handleCameraPermissionResult(
    granted: Boolean,
    context: Context,
    deniedMessage: String,
    permanentlyDeniedMessage: String,
    onGranted: () -> Unit,
) {
    if (granted) {
        onGranted()
    } else {
        showCameraPermissionDeniedToast(context, deniedMessage, permanentlyDeniedMessage)
    }
}

private fun showCameraPermissionDeniedToast(
    context: Context,
    deniedMessage: String,
    permanentlyDeniedMessage: String,
) {
    val activity = context as? Activity
    val permanentlyDenied =
        activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    val message =
        if (permanentlyDenied) {
            permanentlyDeniedMessage
        } else {
            deniedMessage
        }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun PhotoGalleryContent(
    photos: List<ProgressPhoto>,
    padding: PaddingValues,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onPhotoLongClick: (ProgressPhoto) -> Unit,
) {
    if (photos.isEmpty()) {
        EmptyPhotoGallery(padding)
    } else {
        PhotoGalleryGrid(
            photos = photos,
            padding = padding,
            onPhotoClick = onPhotoClick,
            onPhotoLongClick = onPhotoLongClick,
        )
    }
}

@Composable
private fun EmptyPhotoGallery(padding: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.camera_icon),
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.no_photos),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.take_photo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoGalleryGrid(
    photos: List<ProgressPhoto>,
    padding: PaddingValues,
    onPhotoClick: (ProgressPhoto) -> Unit,
    onPhotoLongClick: (ProgressPhoto) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
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
    val displayName = photo.note ?: stringResource(R.string.row_label_format, photo.rowNumber)

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
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_photo)) },
        text = {
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { text = it.take(100) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.photo_name_hint)) },
                shape = MaterialTheme.shapes.medium,
                colors =
                    androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
