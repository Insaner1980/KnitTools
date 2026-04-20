package com.finnvek.knittools.ui.screens.ravelry

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.theme.RavelryTeal

@Composable
fun RavelryDetailScreen(
    patternId: Int,
    onBack: () -> Unit,
    onStartProject: (Long) -> Unit,
    viewModel: RavelryViewModel = hiltViewModel(),
) {
    val detail by viewModel.patternDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isDetailLoading.collectAsStateWithLifecycle()
    val isSaved by viewModel.isPatternSaved.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(patternId) {
        viewModel.loadDetail(patternId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToProject.collect { projectId ->
            onStartProject(projectId)
        }
    }

    ToolScreenScaffold(
        title = detail?.name ?: stringResource(R.string.tool_ravelry),
        onBack = onBack,
    ) { _ ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (detail != null) {
            PatternDetailContent(
                pattern = detail!!,
                isSaved = isSaved,
                onStartProject = { viewModel.createProjectFromPattern() },
                onSave = {
                    viewModel.savePattern()
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.pattern_saved_to_library),
                            Toast.LENGTH_SHORT,
                        ).show()
                },
                onOpenInRavelry = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detail!!.ravelryUrl))
                    context.startActivity(intent)
                },
            )
        }
    }
}

@Composable
private fun PatternDetailContent(
    pattern: PatternDetail,
    isSaved: Boolean,
    onStartProject: () -> Unit,
    onSave: () -> Unit,
    onOpenInRavelry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        PatternHeroImage(photoUrl = pattern.mainPhotoUrl, name = pattern.name)
        PatternHeader(name = pattern.name, designerName = pattern.designer?.name)
        Spacer(modifier = Modifier.height(16.dp))
        PatternDetailRows(pattern = pattern)
        PatternNotes(notes = pattern.notes)
        Spacer(modifier = Modifier.height(24.dp))
        PatternActions(
            isSaved = isSaved,
            onStartProject = onStartProject,
            onSave = onSave,
            onOpenInRavelry = onOpenInRavelry,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PatternHeroImage(
    photoUrl: String?,
    name: String,
) {
    photoUrl?.let { url ->
        AsyncImage(
            model = url,
            contentDescription = name,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PatternHeader(
    name: String,
    designerName: String?,
) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
    )
    designerName?.let { designer ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = designer,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PatternDetailRows(pattern: PatternDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pattern.difficultyAverage?.let {
            DetailRow(
                label = stringResource(R.string.filter_difficulty),
                value = stringResource(R.string.difficulty_format, it),
            )
        }
        pattern.gauge?.let {
            DetailRow(label = stringResource(R.string.gauge_label), value = it)
        }
        pattern.needleSizeText?.let {
            DetailRow(label = stringResource(R.string.pattern_detail_needles), value = it)
        }
        pattern.yarnWeight?.name?.let {
            DetailRow(label = stringResource(R.string.filter_weight), value = it)
        }
        (pattern.yardage ?: pattern.yardageMax)?.let {
            DetailRow(
                label = stringResource(R.string.pattern_detail_yardage),
                value = stringResource(R.string.yardage_format, it),
            )
        }
        pattern.sizesAvailable?.let {
            DetailRow(label = stringResource(R.string.pattern_detail_sizes), value = it)
        }
    }
}

@Composable
private fun PatternNotes(notes: String?) {
    notes?.takeIf { it.isNotBlank() }?.let { text ->
        Spacer(modifier = Modifier.height(16.dp))
        var expanded by rememberSaveable { mutableStateOf(false) }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
        )
        if (text.length > 200) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.show_less else R.string.show_more))
            }
        }
    }
}

@Composable
private fun PatternActions(
    isSaved: Boolean,
    onStartProject: () -> Unit,
    onSave: () -> Unit,
    onOpenInRavelry: () -> Unit,
) {
    Button(
        onClick = onStartProject,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.start_project))
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaved,
    ) {
        Text(
            if (isSaved) {
                stringResource(R.string.pattern_saved)
            } else {
                stringResource(R.string.save_pattern)
            },
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(
        onClick = onOpenInRavelry,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.open_in_ravelry),
            color = RavelryTeal,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
