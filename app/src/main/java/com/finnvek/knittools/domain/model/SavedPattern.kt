package com.finnvek.knittools.domain.model

enum class SavedPatternSource(
    val persistedValue: String,
) {
    Ravelry("RAVELRY"),
    LocalFile("LOCAL_FILE"),
    WebLink("WEB_LINK"),
    Other("OTHER"),
    Unknown("UNKNOWN"),
    ;

    companion object {
        fun fromPersistedValue(value: String): SavedPatternSource =
            entries.firstOrNull { it.persistedValue == value } ?: Unknown
    }
}

data class SavedPattern(
    val id: Long = 0,
    val source: SavedPatternSource,
    val ravelryPatternId: Int? = null,
    val name: String,
    val designerName: String,
    val thumbnailUrl: String? = null,
    val difficulty: Float? = null,
    val gaugeStitches: Float? = null,
    val gaugeRows: Float? = null,
    val needleSize: String? = null,
    val yarnWeight: String? = null,
    val yardage: Int? = null,
    val availability: PatternAvailability = PatternAvailability.Unknown,
    val originalUrl: String = "",
    val canonicalUrl: String = "",
    val localPdfUri: String? = null,
    val isAvailableOffline: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = savedAt,
    val lastSyncedAt: Long? = null,
) {
    val isFree: Boolean
        get() = availability.isFree

    val ravelryId: Int
        get() = ravelryPatternId ?: 0

    val patternUrl: String
        get() = localPdfUri?.takeIf { it.isNotBlank() } ?: canonicalUrl.ifBlank { originalUrl }
}
