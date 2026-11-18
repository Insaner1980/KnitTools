package com.finnvek.knittools.ui.screens.pro

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProAddsGroup
import com.finnvek.knittools.domain.model.proGroupOrder
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.rememberStaggeredEntrance
import com.finnvek.knittools.ui.components.staggeredEntrance
import com.finnvek.knittools.ui.theme.ComponentDimens

/**
 * Pro-sivun ja onboardingin yhteinen kuvaus siitä, mikä pysyy ilmaisena ja mitä Pro lisää.
 * Yksi lähde, jotta hintavaihe ja Pro-sivu eivät lupaa eri asioita.
 */
@Composable
internal fun ProFreeForeverSection(modifier: Modifier = Modifier) {
    // Tiivis yhteenveto riittää useimmille; koko lista avautuu pyydettäessä, jottei sivu veny sekavaksi.
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
    ) {
        SectionLabel(stringResource(R.string.onboarding_pro_free_title))
        if (!expanded) {
            Text(stringResource(R.string.onboarding_pro_free_summary), style = MaterialTheme.typography.bodyMedium)
        } else {
            ProFreeFeatureList()
        }
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = ComponentDimens.FlushTextButtonPadding,
        ) {
            Text(
                stringResource(
                    if (expanded) R.string.onboarding_pro_free_show_less else R.string.onboarding_pro_free_show_all,
                ),
            )
        }
    }
}

@Composable
private fun ProFreeFeatureList() {
    Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
        listOf(
            R.string.onboarding_pro_free_project,
            R.string.onboarding_pro_free_counter,
            R.string.onboarding_pro_free_pdf,
            R.string.onboarding_pro_free_web,
            R.string.onboarding_pro_free_yarn,
            R.string.onboarding_pro_free_time,
            R.string.onboarding_pro_free_tools,
            R.string.onboarding_pro_free_backup,
        ).forEach { resource ->
            Text(stringResource(resource), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Pro Adds -osio; [order] nostaa onboardingissa käyttäjän valintoja vastaavan ryhmän ensimmäiseksi. */
@Composable
internal fun ProAddsSection(
    modifier: Modifier = Modifier,
    order: List<ProAddsGroup> = proGroupOrder(emptyList()),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentPadding)) {
        SectionLabel(stringResource(R.string.onboarding_pro_adds_title))
        order.forEachIndexed { index, group ->
            val (title, body) = group.texts()
            Column(
                Modifier.staggeredEntrance(rememberStaggeredEntrance(index)),
                verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ProAddsGroup.texts(): Pair<Int, Int> =
    when (this) {
        ProAddsGroup.Projects ->
            R.string.onboarding_pro_group_projects_title to R.string.onboarding_pro_group_projects_body
        ProAddsGroup.Workflow ->
            R.string.onboarding_pro_group_workflow_title to R.string.onboarding_pro_group_workflow_body
        ProAddsGroup.Insights ->
            R.string.onboarding_pro_group_insights_title to R.string.onboarding_pro_group_insights_body
    }
