package com.finnvek.knittools.domain.model

val SavedPattern.webPatternUrlOrNull: WebPatternUrl?
    get() {
        if (!localPdfUri.isNullOrBlank()) return null
        if (source != SavedPatternSource.WebLink && source != SavedPatternSource.Other) return null
        val storedUrl = originalUrl.ifBlank { canonicalUrl }
        val validation = validateWebPatternUrl(storedUrl) as? WebPatternUrlValidation.Valid ?: return null
        return validation.value.takeUnless(WebPatternUrl::isRavelryPattern)
    }

val SavedPattern.isWebPatternCompatible: Boolean
    get() = webPatternUrlOrNull != null
