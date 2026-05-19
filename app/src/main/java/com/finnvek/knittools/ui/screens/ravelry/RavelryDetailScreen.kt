package com.finnvek.knittools.ui.screens.ravelry

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
    onUpgradeToPro: () -> Unit = {},
    viewModel: RavelryViewModel = hiltViewModel(),
) {
    val detail by viewModel.patternDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isDetailLoading.collectAsStateWithLifecycle()
    val isSaved by viewModel.isPatternSaved.collectAsStateWithLifecycle()
    val detailError by viewModel.detailError.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.pattern_saved_to_library)
    val saveFailedMessage = stringResource(R.string.ai_error_unknown)
    val openFailedMessage = stringResource(R.string.pattern_open_failed)
    val currentOnStartProject by rememberUpdatedState(onStartProject)
    val currentOnUpgradeToPro by rememberUpdatedState(onUpgradeToPro)

    LaunchedEffect(viewModel, patternId) {
        viewModel.loadDetail(patternId)
    }

    LaunchedEffect(viewModel) {
        viewModel.navigateToProject.collect { projectId ->
            currentOnStartProject(projectId)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.upgradeToPro.collect {
            currentOnUpgradeToPro()
        }
    }

    LaunchedEffect(viewModel, savedMessage, saveFailedMessage) {
        viewModel.patternSaveResults.collect { result ->
            val message =
                when (result) {
                    PatternSaveResult.Saved -> savedMessage
                    PatternSaveResult.Failed -> saveFailedMessage
                }
            Toast
                .makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    ToolScreenScaffold(
        title = detail?.name ?: stringResource(R.string.tool_ravelry),
        onBack = onBack,
    ) { _ ->
        val signInAction = {
            CustomTabsIntent
                .Builder()
                .build()
                .launchUrl(context, viewModel.createSignInUri())
        }
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isAuthenticated) {
                RavelrySignInPrompt(onSignIn = signInAction)
            }
            PatternDetailBody(
                detail = detail,
                detailError = detailError,
                isLoading = isLoading,
                isSaved = isSaved,
                modifier = Modifier.weight(1f),
                actions =
                    PatternDetailActions(
                        onRetry = { viewModel.loadDetail(patternId) },
                        onSignIn = signInAction,
                        onStartProject = { viewModel.createProjectFromPattern() },
                        onSave = { viewModel.savePattern() },
                        onOpenInRavelry = {
                            val url = detail?.ravelryUrlOrNull()
                            if (url == null) {
                                Toast
                                    .makeText(
                                        context,
                                        openFailedMessage,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            } else {
                                openRavelryUrl(
                                    context = context,
                                    url = url,
                                    failureMessage = openFailedMessage,
                                )
                            }
                        },
                    ),
            )
        }
    }
}

internal fun PatternDetail.ravelryUrlOrNull(): String? =
    if (permalink.isBlank()) {
        null
    } else {
        ravelryUrl
    }

private fun openRavelryUrl(
    context: Context,
    url: String,
    failureMessage: String,
) {
    val opened =
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }.fold(
            onSuccess = { true },
            onFailure = { error ->
                if (error is ActivityNotFoundException) {
                    false
                } else {
                    throw error
                }
            },
        )
    if (!opened) {
        Toast
            .makeText(
                context,
                failureMessage,
                Toast.LENGTH_SHORT,
            ).show()
    }
}

data class PatternDetailActions(
    val onRetry: () -> Unit,
    val onSignIn: () -> Unit,
    val onStartProject: () -> Unit,
    val onSave: () -> Unit,
    val onOpenInRavelry: () -> Unit,
)

@Composable
private fun PatternDetailBody(
    detail: PatternDetail?,
    detailError: RavelrySearchError?,
    isLoading: Boolean,
    isSaved: Boolean,
    modifier: Modifier = Modifier,
    actions: PatternDetailActions,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        detail != null -> {
            PatternDetailContent(
                pattern = detail,
                isSaved = isSaved,
                modifier = modifier,
                onStartProject = actions.onStartProject,
                onSave = actions.onSave,
                onOpenInRavelry = actions.onOpenInRavelry,
            )
        }

        detailError != null -> {
            val isAuthenticationError = detailError == RavelrySearchError.Authentication
            Box(
                modifier = modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                com.finnvek.knittools.ui.components.StatusMessage(
                    message = stringResource(detailError.detailMessageRes()),
                    type = com.finnvek.knittools.ui.components.StatusMessageType.Error,
                    actionLabel =
                        stringResource(
                            if (isAuthenticationError) {
                                R.string.ravelry_sign_in
                            } else {
                                R.string.retry
                            },
                        ),
                    onAction =
                        if (isAuthenticationError) {
                            actions.onSignIn
                        } else {
                            actions.onRetry
                        },
                )
            }
        }
    }
}

private fun RavelrySearchError.detailMessageRes(): Int =
    when (this) {
        RavelrySearchError.Authentication -> R.string.ravelry_search_auth_error
        RavelrySearchError.RateLimited -> R.string.ravelry_search_rate_limited
        RavelrySearchError.ServiceUnavailable -> R.string.ravelry_search_service_error
        RavelrySearchError.Network, RavelrySearchError.Unknown -> R.string.pattern_detail_error
    }

@Composable
private fun PatternDetailContent(
    pattern: PatternDetail,
    isSaved: Boolean,
    onStartProject: () -> Unit,
    onSave: () -> Unit,
    onOpenInRavelry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
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
            modifier = Modifier.width(120.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
