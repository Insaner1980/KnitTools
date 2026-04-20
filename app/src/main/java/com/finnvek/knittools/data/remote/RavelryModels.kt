package com.finnvek.knittools.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === Haku ===

@Serializable
data class PatternSearchResponse(
    val patterns: List<PatternSearchResult> = emptyList(),
    val paginator: Paginator? = null,
)

@Serializable
data class Paginator(
    val page: Int = 1,
    @SerialName("page_count")
    val pageCount: Int = 1,
    val results: Int = 0,
)

@Serializable
data class PatternSearchResult(
    val id: Int,
    val name: String = "",
    val designer: PatternDesigner? = null,
    @SerialName("first_photo")
    val firstPhoto: PatternPhoto? = null,
    @SerialName("difficulty_average")
    val difficultyAverage: Float? = null,
    val free: Boolean = true,
    val permalink: String = "",
)

@Serializable
data class PatternDesigner(
    val name: String = "",
)

@Serializable
data class PatternPhoto(
    @SerialName("small2_url")
    val small2Url: String? = null,
    @SerialName("medium_url")
    val mediumUrl: String? = null,
)

// === Yksityiskohdat ===

@Serializable
data class PatternDetailResponse(
    val pattern: PatternDetail,
)

@Serializable
data class PatternDetail(
    val id: Int,
    val name: String = "",
    val permalink: String = "",
    val designer: PatternDesigner? = null,
    val photos: List<PatternPhoto> = emptyList(),
    @SerialName("difficulty_average")
    val difficultyAverage: Float? = null,
    val free: Boolean = true,
    val gauge: String? = null,
    @SerialName("gauge_pattern")
    val gaugePattern: String? = null,
    @SerialName("gauge_divisor")
    val gaugeDivisor: Int? = null,
    @SerialName("row_gauge")
    val rowGauge: Float? = null,
    @SerialName("yardage_max")
    val yardageMax: Int? = null,
    @SerialName("yardage")
    val yardage: Int? = null,
    @SerialName("sizes_available")
    val sizesAvailable: String? = null,
    @SerialName("notes_html")
    val notesHtml: String? = null,
    val notes: String? = null,
    @SerialName("pattern_needle_sizes")
    val patternNeedleSizes: List<NeedleSize> = emptyList(),
    @SerialName("yarn_weight")
    val yarnWeight: YarnWeightInfo? = null,
    @SerialName("pattern_categories")
    val patternCategories: List<PatternCategory> = emptyList(),
) {
    val mainPhotoUrl: String?
        get() = photos.firstOrNull()?.mediumUrl ?: photos.firstOrNull()?.small2Url

    val needleSizeText: String?
        get() =
            patternNeedleSizes
                .mapNotNull { it.name }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")

    val ravelryUrl: String
        get() = "https://www.ravelry.com/patterns/library/$permalink"
}

@Serializable
data class NeedleSize(
    val name: String? = null,
    @SerialName("us")
    val us: String? = null,
    @SerialName("metric")
    val metric: Float? = null,
)

@Serializable
data class YarnWeightInfo(
    val name: String? = null,
)

@Serializable
data class PatternCategory(
    val name: String? = null,
)
