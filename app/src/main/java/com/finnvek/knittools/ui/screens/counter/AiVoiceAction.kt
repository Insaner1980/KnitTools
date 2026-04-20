package com.finnvek.knittools.ui.screens.counter

/**
 * AI-tulkittujen äänikomentojen toimintomallit.
 * Keyword-komennot käyttävät edelleen VoiceCommand-enumia;
 * tämä sealed class kattaa Gemini-tulkinnan laajemman tulosalueen.
 */
sealed class AiVoiceAction {
    data class Increment(
        val count: Int = 1,
    ) : AiVoiceAction()

    data class Decrement(
        val count: Int = 1,
    ) : AiVoiceAction()

    data object Undo : AiVoiceAction()

    data object Reset : AiVoiceAction()

    data class AddNote(
        val text: String,
    ) : AiVoiceAction()

    data object QueryProgress : AiVoiceAction()

    data object QueryRemaining : AiVoiceAction()

    data object QuerySessionTime : AiVoiceAction()

    data object QueryYarn : AiVoiceAction()

    data object QueryInstruction : AiVoiceAction()

    data object QueryShaping : AiVoiceAction()

    data object QueryStitches : AiVoiceAction()

    data object QueryReminders : AiVoiceAction()

    data object QueryCounters : AiVoiceAction()

    data object QueryNotes : AiVoiceAction()

    data object QueryTotalTime : AiVoiceAction()

    data object QuerySummary : AiVoiceAction()

    data object QueryProject : AiVoiceAction()

    data object QuerySection : AiVoiceAction()

    data object StitchIncrement : AiVoiceAction()

    data object StitchDecrement : AiVoiceAction()

    data object Help : AiVoiceAction()

    data object DismissReminder : AiVoiceAction()

    data class IncrementCounter(
        val name: String,
    ) : AiVoiceAction()

    data class DecrementCounter(
        val name: String,
    ) : AiVoiceAction()

    data class SetSection(
        val name: String,
    ) : AiVoiceAction()

    data class SetStepSize(
        val size: Int,
    ) : AiVoiceAction()

    data object NextPage : AiVoiceAction()

    data object PreviousPage : AiVoiceAction()

    data class GoToPage(
        val page: Int,
    ) : AiVoiceAction()

    data object CompleteProject : AiVoiceAction()

    data object GenerateSummary : AiVoiceAction()

    data class ResetCounter(
        val name: String,
    ) : AiVoiceAction()

    data class AddReminder(
        val row: Int,
        val message: String,
    ) : AiVoiceAction()

    data class SetStitchCount(
        val count: Int,
    ) : AiVoiceAction()

    data class ToggleStitchTracking(
        val enabled: Boolean,
    ) : AiVoiceAction()

    data object Unknown : AiVoiceAction()
}
