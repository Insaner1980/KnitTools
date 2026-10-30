package com.finnvek.knittools.pro

import com.finnvek.knittools.domain.model.CounterProject

enum class TrialJourneyAction {
    Widget,
    RepeatCounter,
    AnotherProject,
    WeeklyChart,
    PatternCamera,
}

data class TrialJourneyStep(
    val action: TrialJourneyAction,
    val done: Boolean,
)

data class TrialJourneyFacts(
    val trialStartTimestamp: Long,
    val activeProjectCount: Int,
    val latestActiveProject: CounterProject?,
    val projectsAdded: Int,
    val countersAdded: Int,
    val yarnCardsAdded: Int,
    val photosAdded: Int,
    val repeatCounterAdded: Boolean,
    val chartSeen: Boolean,
    val photoPdfCreated: Boolean,
) {
    fun steps(widgetInstalled: Boolean): List<TrialJourneyStep> =
        buildList {
            if (latestActiveProject != null) {
                add(TrialJourneyStep(TrialJourneyAction.Widget, widgetInstalled))
                add(TrialJourneyStep(TrialJourneyAction.RepeatCounter, repeatCounterAdded))
            }
            add(TrialJourneyStep(TrialJourneyAction.AnotherProject, projectsAdded > 0))
            add(TrialJourneyStep(TrialJourneyAction.WeeklyChart, chartSeen))
            if (latestActiveProject != null) {
                add(TrialJourneyStep(TrialJourneyAction.PatternCamera, photoPdfCreated))
            }
        }
}
