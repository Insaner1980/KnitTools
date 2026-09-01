package com.finnvek.knittools.domain.model

import java.net.IDN
import java.net.URI
import java.util.Locale

const val WEB_PATTERN_URL_MAX_LENGTH = 2_048

data class WebPatternUrl(
    val originalUrl: String,
    val canonicalUrl: String,
    val host: String,
    val isSecure: Boolean,
    val isRavelryPattern: Boolean,
)

sealed interface WebPatternUrlValidation {
    data class Valid(
        val value: WebPatternUrl,
    ) : WebPatternUrlValidation

    data object Invalid : WebPatternUrlValidation
}

// Sopimus hylkää virheellisen syötteen heti ilman korjaavaa heuristiikkaa.
@Suppress("CyclomaticComplexMethod", "ReturnCount")
fun validateWebPatternUrl(input: String): WebPatternUrlValidation {
    if (input.any(::isForbiddenWebPatternUrlCharacter)) return WebPatternUrlValidation.Invalid

    val originalUrl = input.trim()
    if (originalUrl.isEmpty() || originalUrl.length > WEB_PATTERN_URL_MAX_LENGTH) {
        return WebPatternUrlValidation.Invalid
    }
    if (originalUrl.any(Char::isWhitespace) || !hasValidPercentEncoding(originalUrl)) {
        return WebPatternUrlValidation.Invalid
    }

    val uri = runCatching { URI(originalUrl) }.getOrNull() ?: return WebPatternUrlValidation.Invalid
    if (uri.isOpaque || uri.rawAuthority.isNullOrBlank() || uri.rawUserInfo != null) {
        return WebPatternUrlValidation.Invalid
    }

    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return WebPatternUrlValidation.Invalid

    val authority = parseWebPatternAuthority(uri.rawAuthority) ?: return WebPatternUrlValidation.Invalid
    val host =
        normalizePublicHost(authority.host, authority.isIpv6Literal)
            ?: return WebPatternUrlValidation.Invalid
    val port = authority.port
    val canonicalPort =
        if ((scheme == "http" && port == 80) || (scheme == "https" && port == 443)) null else port
    val canonicalHost = if (authority.isIpv6Literal) "[$host]" else host
    val canonicalPath = uri.rawPath?.ifEmpty { "/" } ?: "/"
    val canonicalUrl =
        buildString {
            append(scheme)
            append("://")
            append(canonicalHost)
            canonicalPort?.let {
                append(':')
                append(it)
            }
            append(canonicalPath)
            uri.rawQuery?.let {
                append('?')
                append(it)
            }
            uri.rawFragment?.let {
                append('#')
                append(it)
            }
        }

    return WebPatternUrlValidation.Valid(
        WebPatternUrl(
            originalUrl = originalUrl,
            canonicalUrl = canonicalUrl,
            host = host,
            isSecure = scheme == "https",
            isRavelryPattern = isRavelryPatternUrl(scheme, host, uri.path),
        ),
    )
}

private data class WebPatternAuthority(
    val host: String,
    val port: Int?,
    val isIpv6Literal: Boolean,
)

// Authority-muodot validoidaan eksplisiittisesti ilman verkkotulkintaa.
@Suppress("CyclomaticComplexMethod", "ReturnCount", "kotlin:S3776")
private fun parseWebPatternAuthority(rawAuthority: String): WebPatternAuthority? {
    if ('@' in rawAuthority) return null

    if (rawAuthority.startsWith('[')) {
        val bracketEnd = rawAuthority.indexOf(']')
        if (bracketEnd <= 1) return null
        val host = rawAuthority.substring(1, bracketEnd)
        val suffix = rawAuthority.substring(bracketEnd + 1)
        val port =
            when {
                suffix.isEmpty() -> null
                suffix.startsWith(':') -> parseWebPatternPort(suffix.drop(1)) ?: return null
                else -> return null
            }
        if ('%' in host || ':' !in host) return null
        return WebPatternAuthority(host = host, port = port, isIpv6Literal = true)
    }

    if ('[' in rawAuthority || ']' in rawAuthority) return null
    val colonIndex = rawAuthority.lastIndexOf(':')
    val hasPort = colonIndex >= 0
    if (hasPort && rawAuthority.indexOf(':') != colonIndex) return null
    val host = if (hasPort) rawAuthority.substring(0, colonIndex) else rawAuthority
    if (host.isEmpty()) return null
    val port = if (hasPort) parseWebPatternPort(rawAuthority.substring(colonIndex + 1)) ?: return null else null
    return WebPatternAuthority(host = host, port = port, isIpv6Literal = false)
}

private fun parseWebPatternPort(value: String): Int? {
    if (value.isEmpty() || value.any { !it.isDigit() }) return null
    return value.toIntOrNull()?.takeIf { it in 1..65_535 }
}

private fun normalizePublicHost(
    rawHost: String,
    isIpv6Literal: Boolean,
): String? {
    if (isIpv6Literal) {
        val address = parseIpv6Literal(rawHost) ?: return null
        return address.takeUnless(::isNonPublicIpv6Address)?.toCanonicalIpv6Host()
    }

    val asciiHost =
        runCatching { IDN.toASCII(rawHost, IDN.USE_STD3_ASCII_RULES) }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
            ?: return null
    if (asciiHost.isEmpty() || asciiHost.length > 253 || asciiHost.endsWith('.')) return null
    if (asciiHost == "localhost" || asciiHost.endsWith(".localhost") || asciiHost.endsWith(".local")) {
        return null
    }

    val ipv4 = parseIpv4Literal(asciiHost)
    if (ipv4 != null) {
        if (isNonPublicIpv4Address(ipv4)) return null
        return ipv4.joinToString(".")
    }
    if (asciiHost.all { it.isDigit() || it == '.' } || asciiHost.looksLikeNonCanonicalIpv4Literal()) return null
    return asciiHost
}

private fun parseIpv4Literal(host: String): IntArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val values =
        parts.map { part ->
            if (part.isEmpty() || part.any { !it.isDigit() }) return null
            if (part.length > 1 && part.startsWith('0')) return null
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
    return values.toIntArray()
}

private fun String.looksLikeNonCanonicalIpv4Literal(): Boolean =
    split('.').all { part ->
        part.isNotEmpty() &&
            (
                part.all(Char::isDigit) ||
                    (
                        part.startsWith("0x", ignoreCase = true) &&
                            part.drop(2).isNotEmpty() &&
                            part.drop(2).all(Char::isHexDigit)
                    )
            )
    }

@Suppress("CyclomaticComplexMethod") // Julkisuusrajat pidetään näkyvinä ja auditoitavina.
private fun isNonPublicIpv4Address(address: IntArray): Boolean {
    val first = address[0]
    val second = address[1]
    val third = address[2]
    return first == 0 ||
        first == 10 ||
        first == 127 ||
        first >= 224 ||
        (first == 100 && second in 64..127) ||
        (first == 169 && second == 254) ||
        (first == 172 && second in 16..31) ||
        (first == 192 && second == 168) ||
        (first == 192 && second == 0 && third == 0) ||
        (first == 192 && second == 0 && third == 2) ||
        (first == 192 && second == 88 && third == 99) ||
        (first == 198 && second in 18..19) ||
        (first == 198 && second == 51 && third == 100) ||
        (first == 203 && second == 0 && third == 113)
}

@Suppress("CyclomaticComplexMethod", "ReturnCount") // IPv6-litteraali jäsennetään paikallisesti ilman DNS:ää.
private fun parseIpv6Literal(value: String): ByteArray? {
    if (value.isEmpty() || '%' in value || value.count { it == ':' } < 2) return null
    val compressionIndex = value.indexOf("::")
    if (compressionIndex >= 0 && value.indexOf("::", compressionIndex + 2) >= 0) return null

    val left = if (compressionIndex >= 0) value.substring(0, compressionIndex) else value
    val right = if (compressionIndex >= 0) value.substring(compressionIndex + 2) else ""
    val leftGroups =
        parseIpv6Groups(left, allowIpv4Suffix = compressionIndex < 0 && right.isEmpty()) ?: return null
    val rightGroups = parseIpv6Groups(right, allowIpv4Suffix = true) ?: return null
    val explicitGroupCount = leftGroups.size + rightGroups.size
    val zeroGroupCount = if (compressionIndex >= 0) 8 - explicitGroupCount else 0
    if ((compressionIndex >= 0 && zeroGroupCount < 1) || (compressionIndex < 0 && explicitGroupCount != 8)) {
        return null
    }

    val groups = leftGroups + List(zeroGroupCount) { 0 } + rightGroups
    if (groups.size != 8) return null
    return ByteArray(16).also { bytes ->
        groups.forEachIndexed { index, group ->
            bytes[index * 2] = (group ushr 8).toByte()
            bytes[index * 2 + 1] = group.toByte()
        }
    }
}

@Suppress("kotlin:S3776") // IPv6-ryhmät validoidaan eksplisiittisesti ilman DNS- tai verkkotulkintaa.
private fun parseIpv6Groups(
    value: String,
    allowIpv4Suffix: Boolean,
): List<Int>? {
    if (value.isEmpty()) return emptyList()
    val parts = value.split(':')
    if (parts.any(String::isEmpty)) return null
    return buildList {
        parts.forEachIndexed { index, part ->
            if ('.' in part) {
                if (!allowIpv4Suffix || index != parts.lastIndex) return null
                val ipv4 = parseIpv4Literal(part) ?: return null
                add((ipv4[0] shl 8) or ipv4[1])
                add((ipv4[2] shl 8) or ipv4[3])
            } else {
                if (part.length !in 1..4 || part.any { !it.isHexDigit() }) return null
                add(part.toInt(16))
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod") // Julkisuusrajat pidetään näkyvinä ja auditoitavina.
private fun isNonPublicIpv6Address(address: ByteArray): Boolean {
    val first = address.unsigned(0)
    val second = address.unsigned(1)
    val isGlobalUnicast = (first and 0xe0) == 0x20
    val isUnspecified = address.all { it == 0.toByte() }
    val isLoopback = address.take(15).all { it == 0.toByte() } && address.unsigned(15) == 1
    val isIpv4Mapped =
        address.take(10).all { it == 0.toByte() } &&
            address.unsigned(10) == 0xff &&
            address.unsigned(11) == 0xff
    val isIpv4Compatible = address.take(12).all { it == 0.toByte() }
    val embeddedIpv4 =
        if (isIpv4Mapped || isIpv4Compatible) {
            intArrayOf(address.unsigned(12), address.unsigned(13), address.unsigned(14), address.unsigned(15))
        } else {
            null
        }
    return !isGlobalUnicast ||
        isUnspecified ||
        isLoopback ||
        first == 0xff ||
        (first == 0xfe && second and 0xc0 == 0x80) ||
        (first == 0xfe && second and 0xc0 == 0xc0) ||
        (first and 0xfe) == 0xfc ||
        address.hasIpv6Prefix(byteArrayOf(0x20, 0x01, 0x00), 23) ||
        address.hasIpv6Prefix(byteArrayOf(0x20, 0x01, 0x0d, 0xb8.toByte()), 32) ||
        address.hasIpv6Prefix(byteArrayOf(0x20, 0x02), 16) ||
        address.hasIpv6Prefix(byteArrayOf(0x3f, 0xff.toByte(), 0x00), 20) ||
        address.hasIpv6Prefix(byteArrayOf(0x5f, 0x00), 16) ||
        (embeddedIpv4 != null && isNonPublicIpv4Address(embeddedIpv4))
}

private fun ByteArray.hasIpv6Prefix(
    prefix: ByteArray,
    bitCount: Int,
): Boolean {
    val completeBytes = bitCount / 8
    val remainingBits = bitCount % 8
    if ((0 until completeBytes).any { index -> this[index] != prefix[index] }) return false
    if (remainingBits == 0) return true
    val mask = 0xff shl (8 - remainingBits) and 0xff
    return (unsigned(completeBytes) and mask) == (prefix[completeBytes].toInt() and mask)
}

private fun ByteArray.toCanonicalIpv6Host(): String =
    (0 until 8).joinToString(":") { groupIndex ->
        ((unsigned(groupIndex * 2) shl 8) or unsigned(groupIndex * 2 + 1)).toString(16)
    }

private fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xff

private fun hasValidPercentEncoding(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        if (value[index] != '%') {
            index += 1
            continue
        }
        if (index + 2 >= value.length || !value[index + 1].isHexDigit() || !value[index + 2].isHexDigit()) {
            return false
        }
        index += 3
    }
    return true
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun isForbiddenWebPatternUrlCharacter(character: Char): Boolean = character.isUnsafeWebPatternTextCharacter()

private fun isRavelryPatternUrl(
    scheme: String,
    host: String,
    path: String?,
): Boolean =
    scheme == "https" &&
        (host == "ravelry.com" || host == "www.ravelry.com") &&
        path?.startsWith("/patterns/library/") == true &&
        path.removePrefix("/patterns/library/").isNotBlank()
