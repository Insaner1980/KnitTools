package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/*
 * Projektinäkymän ja langan sivun yhteinen rakenne: hiusviivalla erotetut osiot, joiden otsikko
 * on SectionLabel ja toiminto oikeassa reunassa. Kortteja ei käytetä osioiden ympärillä.
 */

@Composable
internal fun OverviewSectionHeader(
    title: Int,
    action: Int? = null,
    onAction: () -> Unit = {},
) {
    Column(Modifier.padding(top = ProjectOverviewDimens.SectionTopGap)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = ProjectOverviewDimens.RowMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(text = stringResource(title), modifier = Modifier.weight(1f))
            action?.let { OverviewTextAction(stringResource(it), onAction) }
        }
    }
}

/**
 * Osion tekstitoiminto, jonka teksti päättyy samaan oikeaan reunaan kuin jakoviivat ja nuolet.
 * TextButtonin sisäpehmuste siirsi tekstin noin 12 dp sisemmäs. Kosketusalue pysyy 48 dp:nä.
 */
@Composable
internal fun OverviewTextAction(
    label: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .heightIn(min = ProjectOverviewDimens.ActionTouchSize)
                .widthIn(min = ProjectOverviewDimens.ActionTouchSize)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(start = ProjectOverviewDimens.ContentGap),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.knitToolsColors.primaryReadable,
        )
        trailingIcon?.let { Icon(it, null, tint = MaterialTheme.knitToolsColors.primaryReadable) }
    }
}

@Composable
internal fun OverviewEmptyText(resource: Int) {
    Text(
        stringResource(resource),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.knitToolsColors.emptyStateText,
    )
}

@Composable
internal fun OverviewLinkRow(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(
                min = ProjectOverviewDimens.RowMinHeight,
            ).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
    ) {
        icon?.let { Icon(it, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        Column(Modifier.weight(1f).padding(vertical = ProjectOverviewDimens.ContentGap)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Näkymän yläreunan kuva; napautus avaa kuvat tai vaihtaa kuvan ([onClickLabel] kertoo kumpi). */
@Composable
internal fun OverviewHeroPhoto(
    model: String,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ProjectOverviewDimens.HeroPhotoHeight)
                .clip(RoundedCornerShape(ProjectOverviewDimens.HeroPhotoCornerRadius))
                .background(MaterialTheme.knitToolsColors.cardContainer)
                .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick),
    )
}

/**
 * Kuvan paikka ennen ensimmäistä kuvaa: haalea neulepintatilkku ja sen päällä lisäyspilleri.
 * Sama paikkamerkki projektissa ja langassa. Yleistä kameraikonia ei käytetä, ja täytetty
 * khakilaatikko luki raskaana, kun se ei kertonut tyhjästä paikasta mitään.
 */
@Composable
internal fun FabricPhotoPlaceholder(
    kind: FabricKind,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ProjectOverviewDimens.NoPhotoHeight)
                .clip(RoundedCornerShape(ProjectOverviewDimens.HeroPhotoCornerRadius))
                .background(MaterialTheme.knitToolsColors.cardContainer)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FabricSwatch(
            kind = kind,
            color = color,
            modifier = Modifier.matchParentSize().alpha(ProjectOverviewDimens.PLACEHOLDER_FABRIC_ALPHA),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.knitToolsColors.primaryReadable,
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(
                        horizontal = ProjectOverviewDimens.PillHorizontalPadding,
                        vertical = ProjectOverviewDimens.ContentGap,
                    ),
        )
    }
}
