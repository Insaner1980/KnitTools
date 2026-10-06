package com.finnvek.knittools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.projectHeroCount

/**
 * Jatka-kortti: sama komponentti listan Continue-herossa ja projektinäkymässä, jotta laskuriin
 * vievä toiminto näyttää kaikkialla samalta. Kolmiulotteinen jatka-nappi avaa laskurin.
 *
 * @param project laskurin lukema ja tavoite. Nimi näytetään vain kun [showName] on tosi:
 *   projektinäkymässä nimi on jo sivun otsikkona.
 * @param onClick kortin rungon napautus (listalla projektinäkymä, projektinäkymässä laskuri).
 * @param onOpenCounter jatka-nappi; null valmistuneelle projektille, jolla ei lasketa.
 */
@Composable
fun ContinueProjectCard(
    project: CounterProject,
    sessionStatus: String?,
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    onOpenCounter: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
) {
    val display = CounterValueFormatter.forMainCounter(project)
    val targetStatus = mainCounterTargetStatus(display.targetLine)
    val progressFraction = mainCounterTargetFraction(display.targetLine)
    val countText =
        display.targetLine?.let { mainCounterTargetText(it) } ?: mainCounterCountText(display.projectCardCount)
    Card(
        modifier =
            modifier.fillMaxWidth().then(
                if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier,
            ),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.knitToolsColors.actionContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(ProjectListDimens.HeroPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(continueKickerRes(project)).localizedUppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.knitToolsColors.primaryReadable,
                )
                if (showName) {
                    Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                    Text(text = project.name, style = MaterialTheme.typography.titleLarge)
                }
                sessionStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                normalizedContinueKnittingSectionName(project.sectionName)?.let {
                    Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(ProjectListDimens.ProgressGroupTopGap))
                Row(horizontalArrangement = Arrangement.spacedBy(ProjectListDimens.HeroContentGap)) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.projectHeroCount,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).alignByBaseline(),
                    )
                    targetStatus?.let {
                        Text(
                            text = projectListTargetStatusText(it),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                if (progressFraction != null) {
                    Spacer(modifier = Modifier.height(ProjectListDimens.ItemLineGap))
                    ProjectProgressBar(progressFraction)
                }
            }
            onOpenCounter?.let {
                Spacer(modifier = Modifier.width(ProjectListDimens.HeroContentGap))
                CounterImageButton(
                    imageRes = R.drawable.counter_continue_button,
                    contentDescription = stringResource(R.string.project_continue_content_description, project.name),
                    visualSize = ProjectListDimens.HeroActionVisualSize,
                    onClick = it,
                    modifier = Modifier.size(ProjectListDimens.HeroActionTouchSize),
                )
            }
        }
    }
}

/** Kortin ylärivi kertoo, mitä jatketaan: neulomista tai virkkaamista. Valmistunut projekti ei jatku. */
private fun continueKickerRes(project: CounterProject): Int =
    when {
        project.isCompleted -> R.string.section_completed
        project.craftType == CraftType.CROCHET -> R.string.continue_crocheting
        else -> R.string.continue_knitting
    }

internal fun normalizedContinueKnittingSectionName(sectionName: String?): String? =
    sectionName?.trim()?.takeIf(String::isNotEmpty)
