package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.InsightsDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId

data class ProjectFilterOption(
    val id: Long,
    val name: String,
)

/**
 * Projektisuodatin täytettynä pillerinä ja valikkona (Insights ja Libraryn valokuvat).
 * Sirurivi ei skaalautunut projektimäärän kasvaessa. Suodatin ei ole valinta samaan tapaan
 * kuin segmentti, joten se ei koskaan saa täytettyä primary-tyyliä: valittu projekti
 * tunnistetaan omasta lankaväripisteestään.
 */
@Composable
fun ProjectFilterPill(
    projects: List<ProjectFilterOption>,
    selectedProjectId: Long?,
    onSelectProject: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProjectPicker by remember { mutableStateOf(false) }
    val allProjectsLabel = stringResource(R.string.all_projects)
    val selectedName = projects.firstOrNull { it.id == selectedProjectId }?.name

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    // Kosketuskohde tulee Composen omasta laajennuksesta, joten pilleri saa
                    // olla visuaalisesti matalampi kuin 48 dp minimi.
                    .minimumInteractiveComponentSize()
                    .heightIn(min = InsightsDimens.FilterPillHeight)
                    .clip(InsightsDimens.FilterChipShape)
                    .background(MaterialTheme.knitToolsColors.cardContainer)
                    .clickable(role = Role.DropdownList) { showProjectPicker = !showProjectPicker }
                    .semantics {
                        if (showProjectPicker) {
                            collapse {
                                showProjectPicker = false
                                true
                            }
                        } else {
                            expand {
                                showProjectPicker = true
                                true
                            }
                        }
                    }.padding(
                        horizontal = InsightsDimens.FilterChipHorizontalPadding,
                        vertical = InsightsDimens.FilterChipVerticalPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedName != null && selectedProjectId != null) {
                Box(
                    modifier =
                        Modifier
                            .size(InsightsDimens.FilterChipDotSize)
                            .background(
                                yarnColorForId(selectedProjectId, MaterialTheme.knitToolsColors.yarnPalette),
                                CircleShape,
                            ),
                )
                Spacer(modifier = Modifier.width(InsightsDimens.FilterChipDotSpacing))
            }
            Text(
                text = selectedName ?: allProjectsLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            DropdownIndicator(
                modifier =
                    Modifier
                        .padding(start = InsightsDimens.FilterChipIndicatorSpacing)
                        .size(InsightsDimens.FilterChipIndicatorSize),
            )
        }
        DropdownMenu(
            expanded = showProjectPicker,
            onDismissRequest = { showProjectPicker = false },
            // Vakio-Material-pinta oli näytön ainoa tyylittelemätön kohta.
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FilterMenuItem(
                label = allProjectsLabel,
                selected = selectedName == null,
                onClick = {
                    onSelectProject(null)
                    showProjectPicker = false
                },
                dotColor = null,
                showsDot = true,
            )
            projects.forEach { project ->
                FilterMenuItem(
                    label = project.name,
                    selected = selectedProjectId == project.id,
                    onClick = {
                        onSelectProject(project.id)
                        showProjectPicker = false
                    },
                    dotColor = yarnColorForId(project.id, MaterialTheme.knitToolsColors.yarnPalette),
                    showsDot = true,
                )
            }
        }
    }
}

/**
 * Suodatinvalikon rivi. Valittu merkitään lihavoinnilla ja checkillä — ilman merkintää
 * valikosta ei nähnyt mikä on päällä.
 *
 * Teksti on `bodyMedium` eikä `bodyLarge`: isommalla koolla check söi leveyttä juuri
 * valitulta riviltä, jolloin ainoa katkeava nimi oli se jota eniten halusi lukea.
 */
@Composable
fun FilterMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    showsDot: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingIcon =
            if (showsDot) {
                {
                    Box(
                        modifier = Modifier.size(InsightsDimens.FilterChipDotSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        // "Kaikki projektit" saa vaimean pienemmän pisteen: täysi
                        // onSurfaceMuted luki kermalla yhtenä lankaväreistä.
                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        if (dotColor == null) {
                                            InsightsDimens.MenuNeutralDotSize
                                        } else {
                                            InsightsDimens.FilterChipDotSize
                                        },
                                    ).background(
                                        dotColor ?: MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape,
                                    ),
                        )
                    }
                }
            } else {
                null
            },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(InsightsDimens.FilterChipIndicatorSize),
                )
            }
        },
        onClick = onClick,
    )
}
