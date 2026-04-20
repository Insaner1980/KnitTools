package com.finnvek.knittools.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnvek.knittools.ui.theme.knitToolsColors

@Composable
fun KnitToolsBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.parent?.route
            ?: navBackStackEntry?.destination?.route

    Surface(
        color = MaterialTheme.knitToolsColors.navBarContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        NavigationBar(
            modifier = Modifier.padding(horizontal = 6.dp),
            containerColor = MaterialTheme.knitToolsColors.navBarContainer,
        ) {
            TopLevelDestination.entries.forEach { destination ->
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
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
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
