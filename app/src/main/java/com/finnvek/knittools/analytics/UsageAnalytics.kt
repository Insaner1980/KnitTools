package com.finnvek.knittools.analytics

fun interface UsageAnalytics {
    fun track(event: UsageEvent)

    companion object {
        val NONE = UsageAnalytics { }
    }
}

enum class UsageEvent(
    val eventName: String,
) {
    PROJECT_CREATION_STARTED("project creation started"),
    PROJECT_CREATION_SUBMITTED("project creation submitted"),
    PROJECT_CREATION_CANCELLED("project creation cancelled"),
    PROJECT_CREATED("project created"),
    PROJECT_CREATION_FAILED("project creation failed"),
    PDF_IMPORT_STARTED("pdf import started"),
    PDF_IMPORT_SUCCEEDED("pdf import succeeded"),
    PDF_IMPORT_FAILED("pdf import failed"),
    COUNTER_INCREMENTED("counter incremented"),
    COUNTER_DECREMENTED("counter decremented"),
    WORK_SESSION_START_REQUESTED("work session start requested"),
    WORK_SESSION_SAVED("work session saved"),
    WELCOME_DEMO_USED("welcome demo used"),
    WELCOME_TOOLS_CHOSEN("welcome tools chosen"),
    WELCOME_DISMISSED("welcome dismissed"),
    GUIDE_OPENED("guide opened"),
    TIP_ACTED("tip acted"),
    TIP_DISMISSED("tip dismissed"),
}
