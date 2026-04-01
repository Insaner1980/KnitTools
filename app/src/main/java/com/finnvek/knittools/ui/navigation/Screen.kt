package com.finnvek.knittools.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object Counter : Screen("counter")

    data object IncreaseDecrease : Screen("increase_decrease")

    data object Gauge : Screen("gauge")

    data object CastOn : Screen("cast_on")

    data object Yarn : Screen("yarn")

    data object Needles : Screen("needles")

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
}
