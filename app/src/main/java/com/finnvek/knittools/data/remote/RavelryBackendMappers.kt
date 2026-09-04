package com.finnvek.knittools.data.remote

import com.finnvek.knittools.domain.model.PatternAvailability
import java.net.URI

internal object RavelryBackendMappers {
    fun searchResponseFrom(data: Any?): PatternSearchResponse {
        val root = data.asMap()
        val patterns =
            root["patterns"]
                .asList()
                .mapNotNull { value -> value.asMap().toSearchResultOrNull() }
        val pagination = root["pagination"].asMap()

        return PatternSearchResponse(
            patterns = patterns,
            paginator =
                Paginator(
                    page = pagination.int("page") ?: 1,
                    pageCount = pagination.int("pageCount") ?: 1,
                    results = pagination.int("resultCount") ?: 0,
                ),
        )
    }

    fun patternDetailFrom(data: Any?): PatternDetail {
        val pattern = data.asMap()
        val ravelryPatternId =
            requireNotNull(pattern.int("ravelryPatternId")?.takeIf { it > 0 }) {
                "Missing ravelryPatternId in Ravelry pattern detail response"
            }
        val title = pattern.string("title")
        val designerName = pattern.string("designerName")
        val thumbnailUrl = pattern.optionalString("thumbnailUrl")
        val canonicalUrl = pattern.optionalString("canonicalUrl")
        val availability = PatternAvailability.fromBackendValue(pattern.optionalString("availability"))
        val permalink = permalinkFrom(canonicalUrl, ravelryPatternId)

        return PatternDetail(
            id = ravelryPatternId,
            name = title,
            permalink = permalink,
            designer = PatternDesigner(name = designerName),
            photos = thumbnailUrl?.let { listOf(PatternPhoto(mediumUrl = it, small2Url = it)) } ?: emptyList(),
            availability = availability,
            canonicalUrl = canonicalUrl.orEmpty(),
            originalUrl = pattern.optionalString("originalUrl").orEmpty(),
        )
    }

    private fun Map<*, *>.toSearchResultOrNull(): PatternSearchResult? {
        val ravelryPatternId = int("ravelryPatternId")?.takeIf { it > 0 } ?: return null
        val thumbnailUrl = optionalString("thumbnailUrl")
        val canonicalUrl = optionalString("canonicalUrl")
        val availability = PatternAvailability.fromBackendValue(optionalString("availability"))

        return PatternSearchResult(
            id = ravelryPatternId,
            name = string("title"),
            designer = PatternDesigner(name = string("designerName")),
            firstPhoto = thumbnailUrl?.let { PatternPhoto(small2Url = it, mediumUrl = it) },
            availability = availability,
            permalink = permalinkFrom(canonicalUrl, ravelryPatternId),
        )
    }

    private fun permalinkFrom(
        canonicalUrl: String?,
        fallbackId: Int,
    ): String =
        canonicalUrl
            ?.let { url ->
                runCatching {
                    URI(url).path.split("/").last { it.isNotBlank() }
                }.getOrNull()
            }?.takeIf { it.isNotBlank() }
            ?: fallbackId.toString()

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: emptyMap<Any?, Any?>()

    private fun Any?.asList(): List<*> = this as? List<*> ?: emptyList<Any?>()

    private fun Map<*, *>.optionalString(key: String): String? = this[key]?.toString()?.takeIf { it.isNotBlank() }

    private fun Map<*, *>.string(key: String): String = optionalString(key) ?: ""

    private fun Map<*, *>.int(key: String): Int? =
        when (val value = this[key]) {
            is Number -> value.toExactIntOrNull()
            is String -> value.toIntOrNull()
            else -> null
        }

    private fun Number.toExactIntOrNull(): Int? {
        val number = toDouble()
        if (!number.isFinite() || number % 1.0 != 0.0 || number < Int.MIN_VALUE || number > Int.MAX_VALUE) {
            return null
        }
        return number.toInt()
    }
}
