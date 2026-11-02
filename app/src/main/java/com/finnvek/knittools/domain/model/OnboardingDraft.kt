package com.finnvek.knittools.domain.model

import com.finnvek.knittools.domain.calculator.MAX_PROJECT_START_COUNT
import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep { Welcome, Needs, Counter, Benefits, Project, Price }

@Serializable
enum class OnboardingNeed { Counting, Pattern, Returning, Yarn, Projects }

@Serializable
enum class OnboardingCraft { Knitting, Crochet, Both }

@Serializable
data class OnboardingDraft(
    val id: String,
    val step: OnboardingStep = OnboardingStep.Welcome,
    val needs: List<OnboardingNeed> = emptyList(),
    val craft: OnboardingCraft = OnboardingCraft.Knitting,
    val demoCount: Int = 47,
    val demoHistory: List<Int> = emptyList(),
    val name: String = "",
    val initialCount: String = "47",
    val targetRows: String = "",
    val showOptions: Boolean = false,
    val projectStarted: Boolean = false,
    val pdfUri: String? = null,
    val pdfName: String? = null,
    val projectId: Long? = null,
    val completed: Boolean = false,
    val exitPending: Boolean = false,
) {
    val craftType: CraftType
        get() = if (craft == OnboardingCraft.Crochet) CraftType.CROCHET else CraftType.KNITTING

    fun changeDemo(delta: Int): OnboardingDraft {
        val next = (demoCount + delta).coerceIn(0, MAX_PROJECT_START_COUNT)
        return if (next ==
            demoCount
        ) {
            this
        } else {
            copy(demoCount = next, demoHistory = (demoHistory + demoCount).takeLast(100))
        }
    }

    fun undoDemo(): OnboardingDraft =
        demoHistory.lastOrNull()?.let { copy(demoCount = it, demoHistory = demoHistory.dropLast(1)) } ?: this

    fun toggleNeed(need: OnboardingNeed): OnboardingDraft =
        copy(needs = if (need in needs) needs - need else needs + need)

    fun next(): OnboardingDraft =
        when (step) {
            OnboardingStep.Welcome -> copy(step = OnboardingStep.Needs)
            OnboardingStep.Needs -> copy(step = OnboardingStep.Counter)
            OnboardingStep.Counter -> copy(step = OnboardingStep.Benefits)
            OnboardingStep.Benefits ->
                copy(
                    step = OnboardingStep.Project,
                    initialCount = if (projectStarted) initialCount else demoCount.toString(),
                    projectStarted = true,
                )
            else -> this
        }

    fun previous(): OnboardingDraft =
        if (step.ordinal in 1..4) copy(step = OnboardingStep.entries[step.ordinal - 1]) else this
}
