package com.finnvek.knittools.domain.model

import com.finnvek.knittools.domain.calculator.MAX_PROJECT_START_COUNT
import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep { Welcome, Craft, Needs, Counter, Toolbox, Project, Ready, Price }

@Serializable
enum class OnboardingNeed { Counting, Pattern, Returning, Yarn, Projects }

@Serializable
enum class OnboardingCraft { Knitting, Crochet, Both }

/** Pro-sivun hyötyryhmät; järjestys personoidaan onboardingissa käyttäjän valintojen mukaan. */
enum class ProAddsGroup { Projects, Workflow, Insights }

/** Demon ohjeessa on kolmen rivin toisto; lukulinja osoittaa seuraavaksi tehtävää riviä. */
const val DEMO_PATTERN_ROWS = 3

/** Kuinka monen plus-napautuksen jälkeen demo näyttää paluukortin. */
const val DEMO_RETURN_AFTER_INCREMENTS = 2

/** Palauttaa nollapohjaisen ohjerivin, jota käyttäjä tekee seuraavaksi, kun [count] riviä on valmiina. */
fun demoPatternRow(count: Int): Int = Math.floorMod(count, DEMO_PATTERN_ROWS)

/** Nostaa käyttäjän valitsemia ongelmia vastaavan Pro-ryhmän ensimmäiseksi; Insights pysyy viimeisenä. */
fun proGroupOrder(needs: List<OnboardingNeed>): List<ProAddsGroup> {
    val workflowNeeds = setOf(OnboardingNeed.Counting, OnboardingNeed.Returning, OnboardingNeed.Pattern)
    val workflowFirst =
        needs.firstOrNull()?.let { it in workflowNeeds } ?: false
    return if (workflowFirst) {
        listOf(ProAddsGroup.Workflow, ProAddsGroup.Projects, ProAddsGroup.Insights)
    } else {
        listOf(ProAddsGroup.Projects, ProAddsGroup.Workflow, ProAddsGroup.Insights)
    }
}

@Serializable
data class OnboardingDraft(
    val id: String,
    val step: OnboardingStep = OnboardingStep.Welcome,
    val needs: List<OnboardingNeed> = emptyList(),
    val craft: OnboardingCraft = OnboardingCraft.Knitting,
    /** Valintamerkki näytetään vasta käyttäjän omasta valinnasta, ei oletuksesta. */
    val craftChosen: Boolean = false,
    val demoCount: Int = 0,
    val demoHistory: List<Int> = emptyList(),
    val demoIncrements: Int = 0,
    val name: String = "",
    val initialCount: String = "0",
    val targetRows: String = "",
    val showOptions: Boolean = false,
    val pdfUri: String? = null,
    val pdfName: String? = null,
    val projectId: Long? = null,
    val completed: Boolean = false,
    val exitPending: Boolean = false,
    val projectCraft: CraftType? = null,
) {
    val craftType: CraftType
        get() = if (craft == OnboardingCraft.Crochet) CraftType.CROCHET else CraftType.KNITTING

    val projectCraftType: CraftType
        get() = projectCraft ?: craftType

    /** Paluukortti näytetään, kun käyttäjä on itse laskenut vähintään kaksi riviä. */
    val demoReturnVisible: Boolean
        get() = demoIncrements >= DEMO_RETURN_AFTER_INCREMENTS

    fun changeDemo(delta: Int): OnboardingDraft {
        val next = (demoCount + delta).coerceIn(0, MAX_PROJECT_START_COUNT)
        return if (next == demoCount) {
            this
        } else {
            copy(
                demoCount = next,
                demoHistory = (demoHistory + demoCount).takeLast(100),
                demoIncrements = if (delta > 0) demoIncrements + 1 else demoIncrements,
            )
        }
    }

    fun undoDemo(): OnboardingDraft =
        demoHistory.lastOrNull()?.let { copy(demoCount = it, demoHistory = demoHistory.dropLast(1)) } ?: this

    fun toggleNeed(need: OnboardingNeed): OnboardingDraft =
        copy(needs = if (need in needs) needs - need else needs + need)

    fun next(): OnboardingDraft =
        when (step) {
            OnboardingStep.Welcome -> copy(step = OnboardingStep.Craft)
            OnboardingStep.Craft -> copy(step = OnboardingStep.Needs)
            OnboardingStep.Needs -> copy(step = OnboardingStep.Counter)
            OnboardingStep.Counter -> copy(step = OnboardingStep.Toolbox)
            OnboardingStep.Toolbox -> copy(step = OnboardingStep.Project)
            OnboardingStep.Ready -> copy(step = OnboardingStep.Price)
            else -> this
        }

    fun previous(): OnboardingDraft =
        if (step.canGoBack) copy(step = OnboardingStep.entries[step.ordinal - 1]) else this
}

/** Takaisin on sallittu vain ennen projektin luontia; Ready ja Price eivät palaa lomakkeeseen. */
val OnboardingStep.canGoBack: Boolean
    get() = this in OnboardingStep.Craft..OnboardingStep.Project
