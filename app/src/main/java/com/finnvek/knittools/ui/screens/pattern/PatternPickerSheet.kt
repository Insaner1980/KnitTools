package com.finnvek.knittools.ui.screens.pattern

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PatternPickerActions(
    val openDeviceFiles: () -> Unit,
    val startCameraScan: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternPickerSheet(
    projectId: Long?,
    savedPatterns: List<SavedPatternEntity>,
    isPro: Boolean,
    onSavedPatternSelected: (SavedPatternEntity) -> Unit,
    onDocumentSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val attachablePatterns = remember(savedPatterns) { savedPatterns.filter { it.patternUrl.isLocalPatternUri() } }
    val actions =
        rememberPatternPickerActions(
            projectId = projectId,
            onDocumentSelected = onDocumentSelected,
            onDismiss = onDismiss,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PatternPickerSheetContent(
            attachablePatterns = attachablePatterns,
            isPro = isPro,
            projectId = projectId,
            actions = actions,
            onSavedPatternSelected = { pattern ->
                onSavedPatternSelected(pattern)
                onDismiss()
            },
        )
    }
}

@Composable
private fun rememberPatternPickerActions(
    projectId: Long?,
    onDocumentSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
): PatternPickerActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val patternStorage = remember { PatternDocumentStorage() }
    var pendingCaptureImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCaptureFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            onDocumentSelected(uri.toString(), resolvePatternName(context, uri))
            onDismiss()
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            scope.launch {
                handleCaptureResult(
                    success = success,
                    context = context,
                    projectId = projectId,
                    patternStorage = patternStorage,
                    pendingImageUriString = pendingCaptureImageUriString,
                    onDocumentSelected = onDocumentSelected,
                    onDismiss = onDismiss,
                )
                pendingCaptureFilePath?.let { path -> java.io.File(path).delete() }
                pendingCaptureImageUriString = null
                pendingCaptureFilePath = null
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showCameraPermissionDeniedToast(context)
                return@rememberLauncherForActivityResult
            }
            val pendingProjectId = projectId ?: return@rememberLauncherForActivityResult
            val (file, uri) = patternStorage.createCaptureImageFile(context, pendingProjectId)
            pendingCaptureImageUriString = uri.toString()
            pendingCaptureFilePath = file.absolutePath
            cameraLauncher.launch(uri)
        }

    return remember(openDocumentLauncher, permissionLauncher) {
        PatternPickerActions(
            openDeviceFiles = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
            startCameraScan = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
    }
}

@Composable
private fun PatternPickerSheetContent(
    attachablePatterns: List<SavedPatternEntity>,
    isPro: Boolean,
    projectId: Long?,
    actions: PatternPickerActions,
    onSavedPatternSelected: (SavedPatternEntity) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.attach_pattern),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedButton(
            onClick = actions.openDeviceFiles,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pattern_picker_device_files))
        }

        Button(
            onClick = actions.startCameraScan,
            enabled = isPro && projectId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pattern_picker_camera_scan))
        }

        if (attachablePatterns.isNotEmpty()) {
            Text(
                text = stringResource(R.string.pattern_picker_saved_patterns),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            PatternPickerSavedPatterns(
                attachablePatterns = attachablePatterns,
                onSavedPatternSelected = onSavedPatternSelected,
            )
        }
    }
}

@Composable
private fun PatternPickerSavedPatterns(
    attachablePatterns: List<SavedPatternEntity>,
    onSavedPatternSelected: (SavedPatternEntity) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(attachablePatterns, key = { it.id }) { pattern ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp),
                        ).clickable { onSavedPatternSelected(pattern) }
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = pattern.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (pattern.designerName.isNotBlank()) {
                    Text(
                        text = pattern.designerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private suspend fun handleCaptureResult(
    success: Boolean,
    context: android.content.Context,
    projectId: Long?,
    patternStorage: PatternDocumentStorage,
    pendingImageUriString: String?,
    onDocumentSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val pendingUri = pendingImageUriString?.let(Uri::parse)
    if (!success || pendingUri == null || projectId == null) return
    val fileName = "pattern-scan-${System.currentTimeMillis()}.pdf"
    val converted =
        withContext(Dispatchers.IO) {
            patternStorage.convertImageToPdf(context, projectId, pendingUri, fileName)
        }
    if (converted != null) {
        onDocumentSelected(converted.first, converted.second)
        onDismiss()
    } else {
        Toast.makeText(context, context.getString(R.string.pattern_scan_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun showCameraPermissionDeniedToast(context: android.content.Context) {
    val activity = context as? Activity
    val permanentlyDenied =
        activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    val messageRes =
        if (permanentlyDenied) {
            R.string.camera_permission_denied_permanent
        } else {
            R.string.camera_permission_denied
        }
    Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
}

private fun resolvePatternName(
    context: android.content.Context,
    uri: Uri,
): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex >= 0 && cursor.moveToFirst()) {
            val fileName = cursor.getString(columnIndex)
            if (!fileName.isNullOrBlank()) return fileName
        }
    }
    return uri.lastPathSegment ?: context.getString(R.string.pattern_pdf_fallback_name)
}

private fun String.isLocalPatternUri(): Boolean = startsWith("content://") || startsWith("file://")
