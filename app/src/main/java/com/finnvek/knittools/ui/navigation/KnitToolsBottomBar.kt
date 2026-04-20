package com.finnvek.knittools.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnvek.knittools.ui.theme.knitToolsColors

private val NavBarHorizontalPadding = 6.dp

// NavigationBarItem varaa labelille käytännössä koko sarakkeen leveyden;
// pieni turvamargiinia jää ripplea ja reunuksia varten.
private val LabelHorizontalSafetyPadding = 8.dp

@Composable
fun KnitToolsBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.parent?.route
            ?: navBackStackEntry?.destination?.route

    val destinations = TopLevelDestination.entries
    val labels = destinations.map { stringResource(it.labelRes) }
    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.sp,
        )

    Surface(
        color = MaterialTheme.knitToolsColors.navBarContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availablePerTab =
                (maxWidth - NavBarHorizontalPadding * 2) / destinations.size -
                    LabelHorizontalSafetyPadding
            val sharedLabelFontSize =
                rememberSharedLabelFontSize(
                    labels = labels,
                    style = labelStyle,
                    maxWidth = availablePerTab,
                    maxFontSize = labelStyle.fontSize,
                    minFontSize = 8.sp,
                )

            NavigationBar(
                modifier = Modifier.padding(horizontal = NavBarHorizontalPadding),
                containerColor = MaterialTheme.knitToolsColors.navBarContainer,
            ) {
                destinations.forEach { destination ->
                    val selected =
                        currentRoute == destination.route ||
                            currentRoute == destination.startRoute

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelRes),
                                style = labelStyle.copy(color = LocalContentColor.current),
                                fontSize = sharedLabelFontSize,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.knitToolsColors.navBarContainer,
                                unselectedIconColor = MaterialTheme.knitToolsColors.inactiveContent,
                                unselectedTextColor = MaterialTheme.knitToolsColors.inactiveContent,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSharedLabelFontSize(
    labels: List<String>,
    style: TextStyle,
    maxWidth: Dp,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
): TextUnit {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    return remember(labels, maxWidthPx, style, maxFontSize, minFontSize) {
        val max = maxFontSize.value
        val min = minFontSize.value
        var candidate = max
        val step = 0.5f
        while (candidate >= min) {
            val testStyle = style.copy(fontSize = candidate.sp)
            val allFit =
                labels.all { label ->
                    val result = textMeasurer.measure(label, testStyle, maxLines = 1)
                    result.size.width <= maxWidthPx
                }
            if (allFit) return@remember candidate.sp
            candidate -= step
        }
        minFontSize
    }
}
