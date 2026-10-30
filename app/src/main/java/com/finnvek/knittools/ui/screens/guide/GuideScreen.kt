package com.finnvek.knittools.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewTextAction
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens

@Composable
fun GuideScreen(
    topic: GuideTopic?,
    proState: ProState?,
    onBack: () -> Unit,
    onTopicSelected: (GuideTopic) -> Unit,
    onAction: (GuideAction) -> Unit,
    onResetTips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolScreenScaffold(
        title = stringResource(topic?.title ?: R.string.guide_screen_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        key(topic) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ProjectOverviewDimens.ScreenHorizontalPadding)
                    .padding(
                        top = ProjectOverviewDimens.TopContentGap,
                        bottom = ProjectOverviewDimens.ContentBottomPadding,
                    ),
            ) {
                if (topic == null) {
                    GuideRootContent(onTopicSelected, onResetTips)
                } else {
                    GuideTopicContent(topic, proState, onAction)
                }
            }
        }
    }
}

@Composable
private fun GuideRootContent(
    onTopicSelected: (GuideTopic) -> Unit,
    onResetTips: () -> Unit,
) {
    Text(
        stringResource(R.string.guide_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = ProjectOverviewDimens.SectionTopGap),
    )
    GuideTopic.entries.forEach { topic ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OverviewLinkRow(
            title = stringResource(topic.title),
            subtitle = stringResource(topic.subtitle),
            glyph = topic.glyph,
            onClick = { onTopicSelected(topic) },
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().padding(top = ProjectOverviewDimens.ContentGap),
        horizontalArrangement = Arrangement.End,
    ) {
        OverviewTextAction(stringResource(R.string.settings_show_tips_again), onResetTips)
    }
}

@Composable
private fun GuideTopicContent(
    topic: GuideTopic,
    proState: ProState?,
    onAction: (GuideAction) -> Unit,
) {
    Text(
        stringResource(topic.subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = ProjectOverviewDimens.SectionTopGap),
    )
    topic.items.forEach { item ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        GuideItemSection(item, proState, onAction)
    }
}

@Composable
private fun GuideItemSection(
    item: GuideItem,
    proState: ProState?,
    onAction: (GuideAction) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = ProjectOverviewDimens.DetailRowGap),
        verticalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.ContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(item.title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (item.proFeature != null) {
                val guideAccess = proActionAccess(proState ?: ProState(), proState != null, item.proFeature)
                ProMarker(guideAccess)
            }
        }
        Text(
            stringResource(item.body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.action?.let { action ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OverviewTextAction(stringResource(R.string.guide_try_it), onClick = { onAction(action) })
            }
        }
    }
}
