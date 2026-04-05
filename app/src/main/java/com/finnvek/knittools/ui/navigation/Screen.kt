package com.finnvek.knittools.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.ui.graphics.vector.ImageVector
import com.finnvek.knittools.R

sealed class Screen(
    val route: String,
) {
    data object Tools : Screen("tools")

    data object Counter : Screen("counter")

    data object IncreaseDecrease : Screen("increase_decrease")

    data object Gauge : Screen("gauge")

    data object CastOn : Screen("cast_on")

    data object Yarn : Screen("yarn")

    data object Needles : Screen("needles")

    data object Reference : Screen("reference")

    data object SizeCharts : Screen("size_charts")

    data object Abbreviations : Screen("abbreviations")

    data object ChartSymbols : Screen("chart_symbols")

    data object Settings : Screen("settings")

    data object ProUpgrade : Screen("pro_upgrade")

    data object YarnCardReview : Screen("yarn_card_review")

    data object YarnCardList : Screen("yarn_card_list")

    data class YarnCardDetail(
        val cardId: Long,
    ) : Screen("yarn_card_detail/$cardId") {
        companion object {
            const val ROUTE = "yarn_card_detail/{cardId}"
        }
    }

    data object ProjectList : Screen("project_list")

    data class SessionHistory(
        val projectId: Long,
    ) : Screen("session_history/$projectId") {
        companion object {
            const val ROUTE = "session_history/{projectId}"
        }
    }
}

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val startRoute: String,
) {
    Projects("projects_tab", R.string.tab_projects, Icons.Outlined.FolderOpen, "counter"),
    Tools("tools_tab", R.string.tab_tools, Icons.Outlined.Build, "tools"),
    Reference("reference_tab", R.string.tab_reference, Icons.Filled.LibraryBooks, "reference"),
}
