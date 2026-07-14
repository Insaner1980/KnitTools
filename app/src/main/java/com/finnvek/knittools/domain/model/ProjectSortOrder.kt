package com.finnvek.knittools.domain.model

enum class ProjectSortOrder(
    val persistedValue: String,
) {
    UPDATED("updated"),
    NAME("name"),
    CREATED("created"),
    ;

    companion object {
        val DEFAULT: ProjectSortOrder = UPDATED

        fun fromPersistedValue(value: String?): ProjectSortOrder =
            entries.firstOrNull { it.persistedValue == value } ?: DEFAULT
    }
}
