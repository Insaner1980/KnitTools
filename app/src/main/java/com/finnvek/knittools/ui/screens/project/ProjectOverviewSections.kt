package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MainCounterLabelSlot
import com.finnvek.knittools.domain.calculator.YarnUsageCalculator
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.OverviewEmptyText
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewSectionHeader
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.YarnThumbnail
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.projectDocumentDisplayName
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.counter.LinkedYarnSummary
import com.finnvek.knittools.ui.screens.counter.reminderDetailText
import com.finnvek.knittools.ui.screens.counter.yarnUsageAmount
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens

@Composable
internal fun ProjectOverviewYarn(
    items: List<ProjectYarnUsageItem>?,
    unit: YarnUsageUnit,
    cards: List<YarnCard>,
    notes: List<ProjectYarnNote>,
    linkedYarns: List<LinkedYarnSummary>,
    onEdit: () -> Unit,
) {
    val displayedCardIds =
        items
            .orEmpty()
            .mapNotNull { item ->
                item.source.yarnCardId.takeIf { item.source.projectYarnNoteId == null }
            }.toSet()
    // A note and its saved card share one usage record, while both entries stay visible.
    val additionalLinkedYarns = linkedYarns.filterNot { it.id in displayedCardIds }
    // Osiolla on yksi toiminto: sen nimi kertoo mitä puuttuu, eikä riveillä ole omia linkkejä.
    val action =
        when {
            items.isNullOrEmpty() && additionalLinkedYarns.isEmpty() -> R.string.project_overview_add
            items?.any { it.usage == null } == true -> R.string.project_overview_yarn_add_amounts
            else -> R.string.project_overview_edit
        }
    OverviewSectionHeader(R.string.project_content_yarn, action, onEdit)
    // Latauksen ajan osio jää tyhjäksi: iso latausindikaattori välähti joka avauksella.
    if (items == null) {
        return
    } else if (items.isEmpty() && additionalLinkedYarns.isEmpty()) {
        OverviewEmptyText(R.string.project_overview_yarn_empty)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.HeaderTopGap)) {
            items.forEach { item ->
                val note = notes.firstOrNull { it.id == item.source.projectYarnNoteId }
                val card = cards.firstOrNull { it.id == (item.source.yarnCardId ?: note?.savedYarnCardId) }
                val linkedYarn = linkedYarns.firstOrNull { it.id == item.source.yarnCardId }
                ProjectOverviewYarnRow(
                    name = if (note == null) linkedYarn?.name ?: item.name else item.name,
                    description = note?.description ?: linkedYarn?.colorLine ?: card?.colorName,
                    photoUri = card?.photoUri,
                    onEdit = onEdit,
                )
                item.usage?.amounts?.let { ProjectOverviewYarnAmounts(it, unit) }
            }
            additionalLinkedYarns.forEach { yarn ->
                ProjectOverviewYarnRow(
                    yarn.name,
                    yarn.colorLine,
                    cards.firstOrNull { it.id == yarn.id }?.photoUri,
                    onEdit,
                )
            }
        }
    }
}

@Composable
private fun ProjectOverviewYarnRow(
    name: String,
    description: String?,
    photoUri: String?,
    onEdit: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ProjectOverviewDimens.RowMinHeight)
                .clickable(onClick = onEdit),
    ) {
        photoUri
            ?.takeIf(
                String::isNotBlank,
            )?.let { YarnThumbnail(photoUri = it, size = ProjectOverviewDimens.YarnSwatchSize) }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            description?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProjectOverviewYarnAmounts(
    amounts: YarnUsageAmounts,
    unit: YarnUsageUnit,
) {
    val remaining = YarnUsageCalculator.remaining(amounts.allocatedMeters, amounts.usedMeters)
    val values =
        listOf(
            R.string.project_overview_yarn_planned to amounts.plannedMeters,
            R.string.project_overview_yarn_allocated to amounts.allocatedMeters,
            R.string.project_overview_yarn_used to amounts.usedMeters,
            R.string.project_overview_yarn_remaining to remaining,
        )
    BoxWithConstraints {
        val columns =
            if (maxWidth >= ProjectOverviewDimens.YarnGridMinWidthForFourColumns &&
                LocalDensity.current.fontScale <= 1.15f
            ) {
                4
            } else {
                2
            }
        Column(verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
            values.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
                    row.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val formatted = value?.let { yarnUsageAmount(it, unit, amounts) }
                            Text(
                                if (value != null && value < 0 && formatted != null) {
                                    stringResource(R.string.yarn_usage_over_format, formatted)
                                } else {
                                    formatted ?: stringResource(R.string.project_overview_amount_unknown)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProjectOverviewPattern(
    state: CounterUiState,
    actions: CounterScreenActions,
    content: ProjectOverviewContentActions,
) {
    OverviewSectionHeader(
        R.string.project_overview_section_pattern,
        R.string.project_overview_add,
        content.onAddPattern,
    )
    val metadata =
        state.linkedPattern?.takeIf { pattern ->
            state.projectDocuments.none { it.savedPatternId == pattern.id }
        }
    metadata?.let {
        OverviewLinkRow(
            it.name,
            stringResource(
                if (it.isWebPatternCompatible) {
                    R.string.web_pattern_label
                } else {
                    R.string.project_documents_pattern_information
                },
            ),
            CrossStitchGlyph.PATTERN,
            content.onDocuments,
        )
    }
    val primary = state.primaryDocument
    if (primary != null) {
        val available = state.projectDocumentAvailability[primary.id] == true
        OverviewLinkRow(
            projectDocumentDisplayName(primary.label),
            if (available) {
                stringResource(
                    R.string.project_overview_pdf_page_format,
                    primary.currentPage.toLong() + 1L,
                )
            } else {
                stringResource(R.string.project_documents_unavailable)
            },
            CrossStitchGlyph.PATTERN,
            onClick = {
                if (available) {
                    state.projectId?.let { actions.onPatternViewer(it, primary.id) }
                } else {
                    content.onDocuments()
                }
            },
        )
        val documentCount = state.projectDocuments.size
        OverviewLinkRow(
            stringResource(R.string.project_documents_manage),
            pluralStringResource(R.plurals.project_documents_count, documentCount, documentCount),
            CrossStitchGlyph.PATTERN,
            content.onDocuments,
        )
    }
    if (primary == null && metadata == null) OverviewEmptyText(R.string.project_overview_pattern_empty)
}

@Composable
internal fun ProjectOverviewNotes(
    state: CounterUiState,
    onEdit: () -> Unit,
) {
    val notesAccess =
        proActionAccess(state.proState, state.proAccessReady, ProFeature.NOTES, alreadyUnlocked = state.notesCreated)
    OverviewSectionHeader(R.string.notes, trailingContent = {
        OverviewTextAction(
            stringResource(
                if (state.notes.isBlank()) R.string.project_overview_add else R.string.project_overview_edit,
            ),
            onEdit,
            enabled = notesAccess != ProActionAccess.Unknown || state.canUseNotes,
            trailingContent = { if (state.notes.isBlank()) ProMarker(notesAccess) },
        )
    })
    if (state.notes.isBlank()) {
        OverviewEmptyText(R.string.project_overview_notes_empty)
    } else {
        // bodyMedium: muistiinpano on leipätekstiä eikä saa olla rivien otsikoita suurempaa.
        Text(
            state.notes.trim(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            // Lyhyt muistiinpano ei venytä osiota rivin korkuiseksi: kosketusalue on 48 dp, teksti keskellä.
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .heightIn(min = ProjectOverviewDimens.ActionTouchSize)
                    .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}

@Composable
internal fun ProjectOverviewPhotos(
    state: CounterUiState,
    onOpen: () -> Unit,
) {
    val photoAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.PROGRESS_PHOTOS)
    val showAll = state.latestPhotos.size > 3
    OverviewSectionHeader(R.string.project_content_photos, trailingContent = {
        OverviewTextAction(
            stringResource(if (showAll) R.string.project_overview_see_all else R.string.project_overview_add),
            onOpen,
            trailingContent = { if (!showAll) ProMarker(photoAccess) },
        )
    })
    Row(horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
        state.latestPhotos.take(3).forEach { photo ->
            AsyncImage(
                model = photo.photoUri.toUri(),
                contentDescription = stringResource(R.string.photo_count_label_format, photo.rowNumber),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ProjectListDimens.ThumbnailCornerRadius))
                        .clickable(role = Role.Button, onClick = onOpen),
            )
        }
        repeat((3 - state.latestPhotos.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
internal fun ProjectOverviewReminders(
    state: CounterUiState,
    actions: ProjectOverviewContentActions,
) {
    val active = state.reminders.filterNot { it.isCompleted }
    val next =
        active.filter { it.targetRow >= state.counter.count }.minByOrNull { it.targetRow } ?: active.firstOrNull()
    // Piilossa olevat muistutukset avataan otsikkorivin See all -toiminnolla kuten Photosissa. Erillinen
    // "Reminders ›" -rivi luki toisena muistutuksena; sheetissä on oma Add-toiminto.
    val hasHidden = state.reminders.size > if (next == null) 0 else 1
    if (hasHidden) {
        OverviewSectionHeader(R.string.reminders, R.string.project_overview_see_all, actions.onReminders)
    } else {
        val reminderAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.ROW_REMINDERS)
        OverviewSectionHeader(R.string.reminders, trailingContent = {
            OverviewTextAction(
                stringResource(R.string.project_overview_add),
                actions.onAddReminder,
                enabled = reminderAccess != ProActionAccess.Unknown || state.canUseRowReminders,
                trailingContent = { ProMarker(reminderAccess) },
            )
        })
    }
    if (next == null) {
        OverviewEmptyText(R.string.no_reminders)
    } else {
        OverviewLinkRow(
            next.message,
            reminderDetailText(
                next,
                state.counter.count,
                MainCounterLabelSlot(state.mainCounterLabelType, state.mainCounterCustomLabel),
            ),
            CrossStitchGlyph.REMINDER,
            actions.onReminders,
        )
    }
}

@Composable
internal fun ProjectOverviewSessions(
    state: CounterUiState,
    onHistory: () -> Unit,
) {
    // Historia-linkki on arvon rinnalla eikä otsikkorivillä: pitkä linkki painoi otsikkoa enemmän.
    // Kokonaisaika on titleLarge, jottei se kilpaile laskurin lukeman kanssa.
    OverviewSectionHeader(R.string.project_overview_section_sessions)
    Row(verticalAlignment = Alignment.Bottom) {
        // Nollakestoisilla istunnoilla ei näytetä "0 min" -kokonaisaikaa, vain historian linkki.
        Column(Modifier.weight(1f)) {
            if (state.totalSessionMinutes > 0) {
                Text(
                    stringResource(R.string.project_overview_total_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    durationText(DurationDisplayFormatter.fromMinutes(state.totalSessionMinutes)),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        OverviewTextAction(
            stringResource(R.string.session_history_title),
            onHistory,
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
        )
    }
}
