package com.finnvek.knittools.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
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

    data object SizeCharts : Screen("size_charts")

    data object Abbreviations : Screen("abbreviations")

    data object ChartSymbols : Screen("chart_symbols")

    data object Settings : Screen("settings")

    data object ProUpgrade : Screen("pro_upgrade")

    data object YarnCardReview : Screen("yarn_card_review")

    data class YarnCardDetail(
        val cardId: Long,
    ) : Screen("yarn_card_detail/$cardId") {
        companion object {
            const val ROUTE = "yarn_card_detail/{cardId}"
        }
    }

    data class LibraryRavelryDetail(
        val patternId: Int,
    ) : Screen("library_ravelry_detail/$patternId") {
        companion object {
            const val ROUTE = "library_ravelry_detail/{patternId}"
        }
    }

    data object Ravelry : Screen("ravelry")

    data class RavelryDetail(
        val patternId: Int,
    ) : Screen("ravelry_detail/$patternId") {
        companion object {
            const val ROUTE = "ravelry_detail/{patternId}"
        }
    }

    data object ProjectList : Screen("project_list")

    data object PhotoGallery : Screen("photo_gallery")

    data class PatternViewer(
        val projectId: Long,
    ) : Screen("pattern_viewer/$projectId") {
        companion object {
            const val ROUTE = "pattern_viewer/{projectId}"
        }
    }

    data class SessionHistory(
        val projectId: Long,
    ) : Screen("session_history/$projectId") {
        companion object {
            const val ROUTE = "session_history/{projectId}"
        }
    }

    data class NotesEditor(
        val projectId: Long,
    ) : Screen("notes_editor/$projectId") {
        companion object {
            const val ROUTE = "notes_editor/{projectId}"
        }
    }

    // Library-tab
    data object Library : Screen("library")

    data object SavedPatterns : Screen("saved_patterns")

    data class LibraryPatternViewer(
        val savedPatternId: Long,
    ) : Screen("library_pattern_viewer/$savedPatternId") {
        companion object {
            const val ROUTE = "library_pattern_viewer/{savedPatternId}"
        }
    }

    data object MyYarn : Screen("my_yarn")

    data object AllPhotos : Screen("all_photos")

    // Insights-tab
    data object Insights : Screen("insights")
}

enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val startRoute: String,
) {
    Projects("projects_tab", R.string.tab_projects, Icons.Outlined.FolderOpen, "project_list"),
    Library("library_tab", R.string.tab_library, Icons.Filled.AutoStories, "library"),
    Tools("tools_tab", R.string.tab_tools, Icons.Outlined.Build, "tools"),
    Insights("insights_tab", R.string.tab_insights, Icons.Outlined.BarChart, "insights"),
    Settings("settings_tab", R.string.tab_settings, Icons.Outlined.Settings, "settings"),
}
