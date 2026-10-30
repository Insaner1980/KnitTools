package com.finnvek.knittools.ui.screens.guide

import androidx.annotation.StringRes
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.ui.components.CrossStitchGlyph

data class GuideItem(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val proFeature: ProFeature? = null,
    val action: GuideAction? = null,
)

enum class GuideAction {
    OpenCounterForLatestProject,
    OpenLatestProjectPage,
    OpenTools,
    OpenLibrary,
    OpenMyYarn,
    OpenSavedPatterns,
    OpenInsights,
    OpenBackup,
    OpenRavelry,
}

enum class GuideTopic(
    val key: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val glyph: CrossStitchGlyph,
    val items: List<GuideItem>,
) {
    CONTINUE(
        key = "continue",
        title = R.string.guide_topic_continue_title,
        subtitle = R.string.guide_topic_continue_subtitle,
        glyph = CrossStitchGlyph.COUNTER,
        items =
            listOf(
                GuideItem(
                    title = R.string.guide_continue_count_title,
                    body = R.string.guide_continue_count_body,
                    action = GuideAction.OpenCounterForLatestProject,
                ),
                GuideItem(
                    title = R.string.guide_continue_undo_title,
                    body = R.string.guide_continue_undo_body,
                ),
                GuideItem(
                    title = R.string.guide_continue_step_title,
                    body = R.string.guide_continue_step_body,
                ),
                GuideItem(
                    title = R.string.guide_continue_project_page_title,
                    body = R.string.guide_continue_project_page_body,
                    action = GuideAction.OpenLatestProjectPage,
                ),
                GuideItem(
                    title = R.string.guide_continue_find_title,
                    body = R.string.guide_continue_find_body,
                ),
                GuideItem(
                    title = R.string.guide_continue_widget_title,
                    body = R.string.guide_continue_widget_body,
                    proFeature = ProFeature.WIDGET,
                ),
            ),
    ),
    PATTERN(
        key = "pattern",
        title = R.string.guide_topic_pattern_title,
        subtitle = R.string.guide_topic_pattern_subtitle,
        glyph = CrossStitchGlyph.PATTERN,
        items =
            listOf(
                GuideItem(
                    title = R.string.guide_pattern_attach_title,
                    body = R.string.guide_pattern_attach_body,
                    action = GuideAction.OpenSavedPatterns,
                ),
                GuideItem(
                    title = R.string.guide_pattern_documents_title,
                    body = R.string.guide_pattern_documents_body,
                    action = GuideAction.OpenLatestProjectPage,
                ),
                GuideItem(
                    title = R.string.guide_pattern_markup_title,
                    body = R.string.guide_pattern_markup_body,
                ),
                GuideItem(
                    title = R.string.guide_pattern_reading_line_title,
                    body = R.string.guide_pattern_reading_line_body,
                ),
                GuideItem(
                    title = R.string.guide_pattern_bookmarks_title,
                    body = R.string.guide_pattern_bookmarks_body,
                ),
                GuideItem(
                    title = R.string.guide_pattern_ravelry_title,
                    body = R.string.guide_pattern_ravelry_body,
                    action = GuideAction.OpenRavelry,
                ),
                GuideItem(
                    title = R.string.guide_pattern_camera_title,
                    body = R.string.guide_pattern_camera_body,
                    proFeature = ProFeature.PATTERN_CAMERA_SCAN,
                    action = GuideAction.OpenLatestProjectPage,
                ),
            ),
    ),
    YARN(
        key = "yarn",
        title = R.string.guide_topic_yarn_title,
        subtitle = R.string.guide_topic_yarn_subtitle,
        glyph = CrossStitchGlyph.YARN,
        items =
            listOf(
                GuideItem(
                    title = R.string.guide_yarn_project_title,
                    body = R.string.guide_yarn_project_body,
                    action = GuideAction.OpenLatestProjectPage,
                ),
                GuideItem(
                    title = R.string.guide_yarn_remaining_title,
                    body = R.string.guide_yarn_remaining_body,
                ),
                GuideItem(
                    title = R.string.guide_yarn_my_yarn_title,
                    body = R.string.guide_yarn_my_yarn_body,
                    proFeature = ProFeature.UNLIMITED_YARN,
                    action = GuideAction.OpenMyYarn,
                ),
            ),
    ),
    DETAILS(
        key = "details",
        title = R.string.guide_topic_details_title,
        subtitle = R.string.guide_topic_details_subtitle,
        glyph = CrossStitchGlyph.NOTES,
        items =
            listOf(
                GuideItem(
                    title = R.string.guide_details_notes_title,
                    body = R.string.guide_details_notes_body,
                    proFeature = ProFeature.NOTES,
                    action = GuideAction.OpenLatestProjectPage,
                ),
                GuideItem(
                    title = R.string.guide_details_counters_title,
                    body = R.string.guide_details_counters_body,
                    proFeature = ProFeature.MULTIPLE_COUNTERS,
                    action = GuideAction.OpenCounterForLatestProject,
                ),
                GuideItem(
                    title = R.string.guide_details_reminders_title,
                    body = R.string.guide_details_reminders_body,
                    proFeature = ProFeature.ROW_REMINDERS,
                    action = GuideAction.OpenLatestProjectPage,
                ),
                GuideItem(
                    title = R.string.guide_details_photos_title,
                    body = R.string.guide_details_photos_body,
                    proFeature = ProFeature.PROGRESS_PHOTOS,
                    action = GuideAction.OpenLatestProjectPage,
                ),
            ),
    ),
    TOOLS(
        key = "tools",
        title = R.string.guide_topic_tools_title,
        subtitle = R.string.guide_topic_tools_subtitle,
        glyph = CrossStitchGlyph.CLOCK,
        items =
            listOf(
                GuideItem(
                    title = R.string.guide_tools_calculators_title,
                    body = R.string.guide_tools_calculators_body,
                    action = GuideAction.OpenTools,
                ),
                GuideItem(
                    title = R.string.guide_tools_reference_title,
                    body = R.string.guide_tools_reference_body,
                    action = GuideAction.OpenLibrary,
                ),
                GuideItem(
                    title = R.string.guide_tools_work_time_title,
                    body = R.string.guide_tools_work_time_body,
                    action = GuideAction.OpenCounterForLatestProject,
                ),
                GuideItem(
                    title = R.string.guide_tools_insights_title,
                    body = R.string.guide_tools_insights_body,
                    proFeature = ProFeature.INSIGHTS_CHARTS,
                    action = GuideAction.OpenInsights,
                ),
                GuideItem(
                    title = R.string.guide_tools_folders_title,
                    body = R.string.guide_tools_folders_body,
                ),
                GuideItem(
                    title = R.string.guide_tools_backup_title,
                    body = R.string.guide_tools_backup_body,
                    action = GuideAction.OpenBackup,
                ),
            ),
    ),
    ;

    companion object {
        fun fromKey(key: String?): GuideTopic? = entries.firstOrNull { it.key == key }
    }
}
