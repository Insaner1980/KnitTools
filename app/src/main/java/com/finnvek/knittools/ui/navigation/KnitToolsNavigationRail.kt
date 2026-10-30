package com.finnvek.knittools.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
internal fun KnitToolsNavigationRail(navControllerProvider: @Composable () -> NavController) {
    val navController = navControllerProvider()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.parent?.route ?: entry?.destination?.route
    val colors = MaterialTheme.knitToolsColors
    NavigationRail(
        modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).testTag("navigation-rail"),
        containerColor = colors.navBarContainer,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route || currentRoute == destination.startRoute
            NavigationRailItem(
                selected = selected,
                onClick = { if (!selected) navController.navigateToTopLevel(destination) },
                icon = {
                    Icon(destination.icon, contentDescription = stringResource(destination.labelRes))
                },
                label = {
                    Text(
                        stringResource(destination.labelRes).localizedUppercase(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors =
                    NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = colors.navBarContainer,
                        unselectedIconColor = colors.inactiveContent,
                        unselectedTextColor = colors.inactiveContent,
                    ),
            )
        }
    }
}
