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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MainCounterCountSlot
import com.finnvek.knittools.domain.calculator.YarnUsageCalculator
import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.domain.model.ProjectYarnUsageItem
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.domain.model.YarnUsageAmounts
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.OverviewEmptyText
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewSectionHeader
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.YarnThumbnail
import com.finnvek.knittools.ui.components.durationText
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.counter.yarnUsageAmount
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens

@Composable
internal fun ProjectOverviewYarn(
    items: List<ProjectYarnUsageItem>?,
    unit: YarnUsageUnit,
    cards: List<YarnCard>,
    notes: List<ProjectYarnNote>,
    onEdit: () -> Unit,
) {
    // Osiolla on yksi toiminto: sen nimi kertoo mitä puuttuu, eikä riveillä ole omia linkkejä.
    val action =
        when {
            items.isNullOrEmpty() -> R.string.project_overview_add
            items.any { it.usage == null } -> R.string.project_overview_yarn_add_amounts
            else -> R.string.project_overview_edit
        }
    OverviewSectionHeader(R.string.project_content_yarn, action, onEdit)
    // Latauksen ajan osio jää tyhjäksi: iso latausindikaattori välähti joka avauksella.
    if (items == null) {
        return
    } else if (items.isEmpty()) {
        OverviewEmptyText(R.string.project_overview_yarn_empty)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.HeaderTopGap)) {
            items.forEach { item ->
                val note = notes.firstOrNull { it.id == item.source.projectYarnNoteId }
                val card = cards.firstOrNull { it.id == (item.source.yarnCardId ?: note?.savedYarnCardId) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = ProjectOverviewDimens.RowMinHeight,
                            ).clickable(onClick = onEdit),
                ) {
                    // Ilman kuvaa rivi on pelkkää tekstiä: neutraali laatta ei kertonut langasta mitään.
                    card?.photoUri?.takeIf(String::isNotBlank)?.let { photoUri ->
                        YarnThumbnail(photoUri = photoUri, size = ProjectOverviewDimens.YarnSwatchSize)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        (note?.description ?: card?.colorName)?.takeIf(String::isNotBlank)?.let { description ->
                            Text(
                                description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item.usage?.amounts?.let { ProjectOverviewYarnAmounts(it, unit) }
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
                                if (value != null &&
                                    value < 0 &&
                                    formatted != null
                                ) {
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
    val primary = state.primaryDocument
    if (primary != null) {
        val available = state.projectDocumentAvailability[primary.id] == true
        OverviewLinkRow(
            primary.label,
            if (available) {
                stringResource(
                    R.string.project_overview_pdf_page_format,
                    primary.currentPage + 1,
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
        // Dokumenttivalikko tarjoaa myös yksittäisen PDF:n nimeämisen, ensisijaisuuden ja poiston.
        OverviewLinkRow(
            stringResource(R.string.project_documents_title),
            null,
            CrossStitchGlyph.PATTERN,
            content.onDocuments,
        )
    }
    val metadata =
        state.linkedPattern?.takeIf { pattern ->
            state.projectDocuments.none {
                it.savedPatternId ==
                    pattern.id
            }
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
    if (primary == null && metadata == null) OverviewEmptyText(R.string.project_overview_pattern_empty)
}

@Composable
internal fun ProjectOverviewNotes(
    state: CounterUiState,
    onEdit: () -> Unit,
) {
    OverviewSectionHeader(
        R.string.notes,
        if (state.notes.isBlank()) R.string.project_overview_add else R.string.project_overview_edit,
        onEdit,
    )
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
    OverviewSectionHeader(
        R.string.project_content_photos,
        if (state.latestPhotos.size > 3) R.string.project_overview_see_all else R.string.project_overview_add,
        onOpen,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap)) {
        state.latestPhotos.take(3).forEach { photo ->
            AsyncImage(
                model = photo.photoUri.toUri(),
                contentDescription = stringResource(R.string.row_label_format, photo.rowNumber),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .weight(
                            1f,
                        ).aspectRatio(1f)
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
    OverviewSectionHeader(R.string.reminders, R.string.project_overview_add, actions.onAddReminder)
    val active = state.reminders.filterNot { it.isCompleted }
    val next =
        active.filter { it.targetRow >= state.counter.count }.minByOrNull { it.targetRow } ?: active.firstOrNull()
    if (next == null) {
        OverviewEmptyText(R.string.no_reminders)
    } else {
        OverviewLinkRow(
            next.message,
            mainCounterCountText(
                MainCounterCountSlot(next.targetRow, state.mainCounterLabelType, state.mainCounterCustomLabel),
            ),
            CrossStitchGlyph.REMINDER,
            actions.onReminders,
        )
    }
    if (state.reminders.size > 1 || (next == null && state.reminders.isNotEmpty())) {
        OverviewLinkRow(stringResource(R.string.reminders), null, CrossStitchGlyph.REMINDER, actions.onReminders)
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
