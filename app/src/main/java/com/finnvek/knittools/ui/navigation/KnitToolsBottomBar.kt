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
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.theme.knitToolsColors

private val NavBarHorizontalPadding = 6.dp

// NavigationBarItem varaa labelille käytännössä koko sarakkeen leveyden;
// pieni turvamargiinia jää ripplea ja reunuksia varten.
private val LabelHorizontalSafetyPadding = 8.dp

// Matalan palkin kuvake (24 dp), kuvakkeen ja tekstin väli sekä indikaattorin vaakapehmuste molemmin puolin.
private val ShortBarIconAndPadding = 60.dp

/**
 * Alanavigointi. [short] valitaan matalassa ja leveässä ikkunassa (vaakapuhelin, jaettu näyttö):
 * 64 dp:n palkki, jossa kuvake on tekstin vieressä. Täyskorkea 80 dp:n palkki jätti 360 dp:n
 * korkuiseen ikkunaan sisällölle alle 200 dp.
 */
@Composable
fun KnitToolsBottomBar(
    navControllerProvider: @Composable () -> NavController,
    short: Boolean = false,
) {
    val navController = navControllerProvider()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.parent?.route
            ?: navBackStackEntry?.destination?.route

    val destinations = TopLevelDestination.entries
    val labels = destinations.map { stringResource(it.labelRes).localizedUppercase() }
    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.sp,
        )

    Surface(
        color = MaterialTheme.knitToolsColors.navBarContainer,
        modifier = Modifier.fillMaxWidth().testTag("navigation-bottom-bar"),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Matalassa palkissa kuvake on tekstin vieressä, joten sama leveys jakautuu myös kuvakkeelle.
            val itemChrome = if (short) ShortBarIconAndPadding else 0.dp
            val availablePerTab =
                (
                    (maxWidth - NavBarHorizontalPadding * 2) / destinations.size -
                        LabelHorizontalSafetyPadding - itemChrome
                ).coerceAtLeast(0.dp)
            val item =
                NavigationBarItemContent(
                    labelStyle = labelStyle,
                    labelFontSize =
                        rememberSharedLabelFontSize(
                            labels = labels,
                            style = labelStyle,
                            maxWidth = availablePerTab,
                            maxFontSize = labelStyle.fontSize,
                        ),
                    currentRoute = currentRoute,
                    onNavigate = navController::navigateToTopLevel,
                )
            if (short) {
                KnitToolsShortNavigationBar(destinations, item)
            } else {
                KnitToolsTallNavigationBar(destinations, item)
            }
        }
    }
}

private class NavigationBarItemContent(
    val labelStyle: TextStyle,
    val labelFontSize: TextUnit,
    val currentRoute: String?,
    val onNavigate: (TopLevelDestination) -> Unit,
) {
    fun isSelected(destination: TopLevelDestination): Boolean =
        currentRoute == destination.route || currentRoute == destination.startRoute

    @Composable
    fun ItemIcon(destination: TopLevelDestination) {
        Icon(
            imageVector = destination.icon,
            contentDescription = stringResource(destination.labelRes),
        )
    }

    @Composable
    fun ItemLabel(
        destination: TopLevelDestination,
        selected: Boolean,
    ) {
        // Ajonaikainen yhteinen koko sovittaa kaikki lokalisoidut välilehtinimet samaan leveyteen.
        Text(
            text = stringResource(destination.labelRes).localizedUppercase(),
            style = labelStyle.copy(color = LocalContentColor.current),
            fontSize = labelFontSize,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KnitToolsTallNavigationBar(
    destinations: List<TopLevelDestination>,
    item: NavigationBarItemContent,
) {
    NavigationBar(
        modifier = Modifier.padding(horizontal = NavBarHorizontalPadding),
        containerColor = MaterialTheme.knitToolsColors.navBarContainer,
    ) {
        destinations.forEach { destination ->
            val selected = item.isSelected(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) item.onNavigate(destination) },
                icon = { item.ItemIcon(destination) },
                label = { item.ItemLabel(destination, selected) },
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

@Composable
private fun KnitToolsShortNavigationBar(
    destinations: List<TopLevelDestination>,
    item: NavigationBarItemContent,
) {
    ShortNavigationBar(
        modifier = Modifier.padding(horizontal = NavBarHorizontalPadding),
        containerColor = MaterialTheme.knitToolsColors.navBarContainer,
    ) {
        destinations.forEach { destination ->
            val selected = item.isSelected(destination)
            ShortNavigationBarItem(
                selected = selected,
                onClick = { if (!selected) item.onNavigate(destination) },
                icon = { item.ItemIcon(destination) },
                label = { item.ItemLabel(destination, selected) },
                iconPosition = NavigationItemIconPosition.Start,
                colors =
                    ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIndicatorColor = MaterialTheme.knitToolsColors.navBarContainer,
                        unselectedIconColor = MaterialTheme.knitToolsColors.inactiveContent,
                        unselectedTextColor = MaterialTheme.knitToolsColors.inactiveContent,
                    ),
            )
        }
    }
}

@Composable
internal fun rememberSharedLabelFontSize(
    labels: List<String>,
    style: TextStyle,
    maxWidth: Dp,
    maxFontSize: TextUnit,
): TextUnit {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    return remember(labels, maxWidthPx, style, maxFontSize, textMeasurer) {
        sharedBottomNavigationLabelFontSize(
            maxFontSize = maxFontSize,
            step = 0.5.sp,
            allLabelsFit = { candidate ->
                val testStyle = style.copy(fontSize = candidate, fontWeight = FontWeight.SemiBold)
                labels.all { label ->
                    val result = textMeasurer.measure(label, testStyle, maxLines = 1)
                    result.size.width <= maxWidthPx
                }
            },
        )
    }
}

internal fun sharedBottomNavigationLabelFontSize(
    maxFontSize: TextUnit,
    step: TextUnit,
    allLabelsFit: (TextUnit) -> Boolean,
): TextUnit {
    var candidate = maxFontSize.value
    while (candidate > step.value) {
        val candidateFontSize = candidate.sp
        if (allLabelsFit(candidateFontSize)) return candidateFontSize
        candidate -= step.value
    }
    return step
}
