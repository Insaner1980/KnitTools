package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.ui.components.BadgePill
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.components.FabricKind
import com.finnvek.knittools.ui.components.FabricPhotoPlaceholder
import com.finnvek.knittools.ui.components.OverviewHeroPhoto
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.craftTypeLabel
import com.finnvek.knittools.ui.components.workSessionStatusText
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.counter.ProjectYarnUsageViewModel
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId

data class ProjectOverviewContentActions(
    val onEditDetails: () -> Unit,
    val onMoveToFolder: () -> Unit,
    val onComplete: () -> Unit,
    val onReactivate: () -> Unit,
    val onDelete: () -> Unit,
    val onYarn: () -> Unit,
    val onDocuments: () -> Unit,
    val onAddPattern: () -> Unit,
    val onReminders: () -> Unit,
    val onAddReminder: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectOverviewRouteContent(
    state: CounterUiState,
    yarnCards: List<YarnCard>,
    actions: CounterScreenActions,
    contentActions: ProjectOverviewContentActions,
    snackbarHostState: SnackbarHostState,
) {
    val projectId = state.projectId ?: return
    val yarnModel: ProjectYarnUsageViewModel = hiltViewModel(key = "overview-yarn")
    val yarnItems by yarnModel.items.collectAsStateWithLifecycle()
    val yarnUnit by yarnModel.displayUnit.collectAsStateWithLifecycle()
    LaunchedEffect(projectId) { yarnModel.observe(projectId) }
    DisposableEffect(yarnModel) { onDispose { yarnModel.stopObserving() } }
    ProjectOverviewScreen(state, actions, contentActions, snackbarHostState, yarnItems, yarnUnit, yarnCards)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectOverviewScreen(
    state: CounterUiState,
    actions: CounterScreenActions,
    contentActions: ProjectOverviewContentActions,
    snackbarHostState: SnackbarHostState,
    yarnItems: List<ProjectYarnUsageItem>?,
    yarnUnit: YarnUsageUnit,
    yarnCards: List<YarnCard> = emptyList(),
) {
    val projectId = state.projectId ?: return
    val listState = rememberLazyListState()
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }
    var menuOpen by rememberSaveable(projectId) { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { if (showTitle) Text(state.projectName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.project_actions_title))
                    }
                    ProjectOverviewMenu(state, menuOpen, { menuOpen = false }, contentActions)
                },
                // Läpinäkyvä kuten laskurin yläpalkki: oletuspinta piirsi taustasta eroavan kaistaleen,
                // jonka terävä reuna sai vieritetyn sisällön näyttämään renderöintivirheeltä.
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding =
                PaddingValues(
                    start = ProjectOverviewDimens.ScreenHorizontalPadding,
                    top = ProjectOverviewDimens.TopContentGap,
                    end = ProjectOverviewDimens.ScreenHorizontalPadding,
                    bottom = ProjectOverviewDimens.ContentBottomPadding,
                ),
        ) {
            item(key = "photo") { ProjectOverviewPhoto(state, projectId, actions.onPhotoGallery) }
            item(key = "name") { ProjectOverviewName(state) }
            item(key = "progress") { ProjectOverviewProgress(state, actions.onOpenCounter) }
            // Valmistuneella projektilla ei ole jatka-korttia, joten laskurin historia avataan täältä.
            if (state.isCompleted) {
                item(key = "counter-history") {
                    OverviewLinkRow(
                        title = stringResource(R.string.counter_history_title),
                        subtitle = null,
                        icon = null,
                        onClick = { actions.onCounterHistory(projectId) },
                    )
                }
            }
            item(key = "yarn") {
                ProjectOverviewYarn(yarnItems, yarnUnit, yarnCards, state.projectYarnNotes, contentActions.onYarn)
            }
            item(key = "pattern") { ProjectOverviewPattern(state, actions, contentActions) }
            item(key = "notes") { ProjectOverviewNotes(state) { actions.onNotesEditor(projectId) } }
            if (state.latestPhotos.isNotEmpty()) {
                item(key = "photos") { ProjectOverviewPhotos(state, actions.onPhotoGallery) }
            }
            item(key = "reminders") { ProjectOverviewReminders(state, contentActions) }
            // Pelkkiä rivejä sisältävät istunnot ovat nollakestoisia, joten näkyvyys seuraa olemassaoloa.
            if (state.hasSessions) {
                item(key = "sessions") { ProjectOverviewSessions(state) { actions.onSessionHistory(projectId) } }
            }
        }
    }
}

@Composable
private fun ProjectOverviewMenu(
    state: CounterUiState,
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: ProjectOverviewContentActions,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(
            R.string.project_overview_edit_details to actions.onEditDetails,
            R.string.folder_move_to to actions.onMoveToFolder,
            if (state.isCompleted) {
                R.string.reactivate_project to actions.onReactivate
            } else {
                R.string.complete_project to actions.onComplete
            },
            R.string.delete_project to actions.onDelete,
        ).forEach { (label, action) ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(label),
                        color =
                            if (label == R.string.delete_project) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                },
                onClick = {
                    onDismiss()
                    action()
                },
            )
        }
    }
}

@Composable
private fun ProjectOverviewPhoto(
    state: CounterUiState,
    projectId: Long,
    onClick: () -> Unit,
) {
    val photo = state.latestPhotos.firstOrNull()
    if (photo == null) {
        // Sama tilkku ja väri kuin projektikortin pikkukuvassa, joten kortti ja näkymä tunnistaa samaksi.
        FabricPhotoPlaceholder(
            kind = if (state.craftType == CraftType.CROCHET) FabricKind.CROCHET else FabricKind.KNIT,
            color = yarnColorForId(projectId, MaterialTheme.knitToolsColors.yarnPalette),
            label = stringResource(R.string.project_overview_add_photo),
            onClick = onClick,
        )
        return
    }
    OverviewHeroPhoto(
        model = photo.photoUri,
        onClickLabel = stringResource(R.string.project_overview_open_photos),
        onClick = onClick,
    )
}

@Composable
private fun ProjectOverviewName(state: CounterUiState) {
    Column(
        Modifier.padding(top = ProjectOverviewDimens.HeaderTopGap),
        verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
    ) {
        Text(
            state.projectName,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
            verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
        ) {
            Text(
                craftTypeLabel(state.craftType),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Aktiivisuus on projektin oletustila, joten merkki kertoo vain valmistumisesta.
            if (state.isCompleted) {
                BadgePill(text = stringResource(R.string.section_completed))
            }
        }
    }
}

@Composable
private fun ProjectOverviewProgress(
    state: CounterUiState,
    onOpenCounter: () -> Unit,
) {
    // Sama jatka-kortti kuin listan herossa. Projektin nimi on jo sivun otsikkona, joten korttiin ei
    // toisteta sitä. Valmistuneella projektilla ei ole jatka-nappia: laskemaan pääsee uudelleenaktivoimalla.
    val hasSession = state.activeSession?.projectId == state.projectId
    ContinueProjectCard(
        project =
            CounterProject(
                id = state.projectId ?: 0L,
                name = state.projectName,
                count = if (state.isCompleted) state.totalRows ?: state.counter.count else state.counter.count,
                sectionName = state.sectionName,
                targetRows = state.targetRows,
                craftType = state.craftType,
                mainCounterLabelType = state.mainCounterLabelType,
                mainCounterCustomLabel = state.mainCounterCustomLabel,
                isCompleted = state.isCompleted,
            ),
        sessionStatus = workSessionStatusText(hasSession, state.activeSession?.needsRecoveryReview == true),
        onClick = onOpenCounter.takeUnless { state.isCompleted },
        onClickLabel = stringResource(R.string.project_continue_content_description, state.projectName),
        onOpenCounter = onOpenCounter.takeUnless { state.isCompleted },
        showName = false,
        modifier = Modifier.padding(top = ProjectOverviewDimens.HeaderTopGap),
    )
}
