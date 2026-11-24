package com.finnvek.knittools.ui.screens.sizecharts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.SizeChartData
import com.finnvek.knittools.ui.components.SegmentedToggle
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.screens.home.HomeViewModel
import com.finnvek.knittools.ui.theme.SizeChartDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SizeChartScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SizeChartData.Category.WOMEN) }
    val useImperial by homeViewModel.useImperial.collectAsStateWithLifecycle()

    val headers = remember(selectedCategory) { SizeChartData.headers(selectedCategory) }
    val entries = remember(selectedCategory) { SizeChartData.entries(selectedCategory) }

    val categoryLabels =
        mapOf(
            SizeChartData.Category.BABY to stringResource(R.string.category_baby),
            SizeChartData.Category.CHILD_YOUTH to stringResource(R.string.category_child_youth),
            SizeChartData.Category.WOMEN to stringResource(R.string.category_women),
            SizeChartData.Category.MEN to stringResource(R.string.category_men),
            SizeChartData.Category.HEAD to stringResource(R.string.category_head),
            SizeChartData.Category.HAND to stringResource(R.string.category_hand),
        )

    ToolScreenScaffold(
        contentWidthPolicy = ContentWidthPolicy.Chart,
        title = stringResource(R.string.size_charts_title),
        onBack = onBack,
    ) { padding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = SizeChartDimens.ScreenPadding),
        ) {
            val density = LocalDensity.current
            val viewportWidth = maxWidth
            val tableScroll = rememberScrollState()
            val unit = stringResource(if (useImperial) R.string.measurement_unit_inch_short else R.string.unit_cm)
            val context = LocalContext.current
            val locale = rememberCurrentLocale()
            val headerCells =
                headers.mapIndexed { index, label ->
                    val visibleLabel = if (label == R.string.size_col_arm) R.string.size_col_sleeve else label
                    val name = stringResource(visibleLabel)
                    if (index == 0) name else stringResource(R.string.size_chart_header_with_unit, name, unit)
                }
            val sleeveDescription =
                stringResource(R.string.size_chart_header_with_unit, stringResource(R.string.size_col_arm), unit)
            val bodyCells =
                remember(entries, context, locale, useImperial) {
                    entries.map { entry ->
                        listOf(entry.sizeLabel.resolve(context)) +
                            entry.measurements.map {
                                SizeChartData.formatMeasurementValue(it, useImperial, locale)
                            }
                    }
                }
            val headerStyle = MaterialTheme.typography.labelMedium
            val bodyStyle = MaterialTheme.typography.bodyMedium
            val measurer = rememberTextMeasurer()
            val columnWidths =
                remember(headerCells, bodyCells, headerStyle, bodyStyle, measurer, density, viewportWidth) {
                    with(density) {
                        val minimums =
                            headerCells
                                .mapIndexed { index, header ->
                                    val headerWidth =
                                        measurer
                                            .measure(
                                                header,
                                                headerStyle,
                                                softWrap = false,
                                                maxLines = 1,
                                            ).size.width
                                    val bodyWidth =
                                        bodyCells.maxOf { row ->
                                            measurer
                                                .measure(
                                                    row[index],
                                                    bodyStyle,
                                                    softWrap = false,
                                                    maxLines = 1,
                                                ).size.width
                                        }
                                    maxOf(headerWidth, bodyWidth) + 2 * SizeChartDimens.CellPadding.roundToPx()
                                }.toMutableList()
                        val viewportPixels = viewportWidth.roundToPx()
                        // Keep measurement columns reachable when a large size label fills the viewport.
                        minimums[0] = minimums[0].coerceAtMost(viewportPixels / 2)
                        val extraPixels = (viewportPixels - minimums.sum()).coerceAtLeast(0)
                        minimums.mapIndexed { index, width ->
                            (
                                width + extraPixels / minimums.size +
                                    if (index <
                                        extraPixels % minimums.size
                                    ) {
                                        1
                                    } else {
                                        0
                                    }
                            ).toDp()
                        }
                    }
                }
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(SizeChartDimens.RowSpacing),
            ) {
                item(key = "categories") {
                    val categories = SizeChartData.Category.entries
                    SegmentedToggle(
                        options = categories.map { categoryLabels.getValue(it) },
                        selectedIndex = categories.indexOf(selectedCategory),
                        onSelect = { selectedCategory = categories[it] },
                        modifier = Modifier.padding(vertical = SizeChartDimens.RowSpacing),
                    )
                }
                stickyHeader(key = "size-table-header") {
                    SizeChartRow(
                        cells = headerCells,
                        columnWidths = columnWidths,
                        scroll = tableScroll,
                        header = true,
                        sleeveDescription =
                            sleeveDescription.takeIf { selectedCategory == SizeChartData.Category.CHILD_YOUTH },
                    )
                }
                items(bodyCells) { cells ->
                    SizeChartRow(cells = cells, columnWidths = columnWidths, scroll = tableScroll)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (selectedCategory == SizeChartData.Category.CHILD_YOUTH) {
                    item(key = "sleeve-note") {
                        Text(
                            text = stringResource(R.string.size_chart_sleeve_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeChartRow(
    cells: List<String>,
    columnWidths: List<Dp>,
    scroll: ScrollState,
    header: Boolean = false,
    sleeveDescription: String? = null,
) {
    val background =
        if (header) Modifier.background(MaterialTheme.knitToolsColors.tableHeaderContainer) else Modifier
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().then(background).padding(vertical = SizeChartDimens.RowSpacing),
    ) {
        Text(
            text = cells.first(),
            modifier = Modifier.width(columnWidths.first()).padding(horizontal = SizeChartDimens.CellPadding),
            style = style,
            textAlign = TextAlign.Center,
            maxLines = if (header) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        Row(modifier = Modifier.weight(1f).horizontalScroll(scroll)) {
            for (index in 1..cells.lastIndex) {
                val description = sleeveDescription.takeIf { index == cells.lastIndex }
                Text(
                    text = cells[index],
                    modifier =
                        Modifier.width(columnWidths[index]).padding(horizontal = SizeChartDimens.CellPadding).then(
                            if (description ==
                                null
                            ) {
                                Modifier
                            } else {
                                Modifier.clearAndSetSemantics { contentDescription = description }
                            },
                        ),
                    style = style,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
