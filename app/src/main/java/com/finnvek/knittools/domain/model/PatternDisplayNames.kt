package com.finnvek.knittools.domain.model

import java.util.Locale

/**
 * Ohjeen luettava nimi tiedostonimestä. Ladatut PDF:t ovat usein muotoa "STEP_BY_STEP_SWEATER_V3.pdf",
 * joka näkyi sellaisenaan lukijan otsikossa ja projektinäkymässä.
 */
object PatternDisplayNames {
    private const val PDF_EXTENSION = ".pdf"
    private val whitespace = Regex("\\s+")
    private val legacyPhotoPdf = Regex("pattern-images-[0-9]+(?:-[0-9]+)?(?:\\.pdf)?", RegexOption.IGNORE_CASE)

    /** "STEP_BY_STEP_SWEATER_V3.pdf" → "Step By Step Sweater V3"; sekakirjaiminen nimi säilyttää muotonsa. */
    fun fromFileName(fileName: String): String {
        val base =
            fileName
                .trim()
                .let { if (it.endsWith(PDF_EXTENSION, ignoreCase = true)) it.dropLast(PDF_EXTENSION.length) else it }
                .replace('_', ' ')
                .replace(whitespace, " ")
                .trim()
                .trimEnd('.', ' ')
        if (base.isEmpty()) return fileName.trim()
        val mixedCase = base.any(Char::isLowerCase) && base.any(Char::isUpperCase)
        return if (mixedCase) {
            base
        } else {
            base.split(' ').joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }
    }

    /**
     * Projektin dokumentin nimike tiedostonimestä. Pitkä tiedostonimi lyhennetään nimikkeen
     * enimmäispituuteen, ettei liitos kaadu pituustarkistukseen.
     */
    fun documentLabel(fileName: String): String =
        fromFileName(fileName)
            .takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
            .trimEnd()

    fun uniqueDocumentLabel(
        label: String,
        existingLabels: Set<String>,
    ): String {
        if (label !in existingLabels) return label
        return generateSequence(2) { it + 1 }
            .map { number ->
                val suffix = " $number"
                label
                    .takeWithoutSplittingSurrogatePair(PROJECT_DOCUMENT_LABEL_MAX_LENGTH - suffix.length)
                    .trimEnd() + suffix
            }.first { it !in existingLabels }
    }

    /** Näytettävä nimi: tiedostonimeltä näyttävä nimike siistitään, käyttäjän antama nimi pysyy ennallaan. */
    fun forDisplay(
        label: String,
        photoFallback: String? = null,
    ): String =
        when {
            photoFallback != null && legacyPhotoPdf.matches(label.trim()) -> photoFallback
            label.trim().endsWith(PDF_EXTENSION, ignoreCase = true) -> fromFileName(label)
            else -> label
        }
}
