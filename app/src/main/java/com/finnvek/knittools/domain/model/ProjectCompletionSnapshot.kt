package com.finnvek.knittools.domain.model

/** Uuden valmistumistapahtuman tiedot säilyvät, vaikka käyttöliittymä sulkee valitun projektin. */
data class ProjectCompletionSnapshot(
    val eventId: Long,
    val project: CounterProject,
    val totalSessionMinutes: Int,
)
