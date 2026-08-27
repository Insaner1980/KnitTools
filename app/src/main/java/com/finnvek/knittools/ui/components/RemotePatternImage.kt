package com.finnvek.knittools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import java.net.URI
import java.net.URISyntaxException

@Composable
fun RemotePatternImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageLoaderProvider: (() -> ImageLoader)? = null,
) {
    val normalizedUrl = remember(imageUrl) { normalizeRemotePatternImageUrl(imageUrl) }
    var failed by remember(normalizedUrl) { mutableStateOf(false) }

    if (normalizedUrl == null || failed) return

    val context = LocalPlatformContext.current
    val resolvedImageLoader = imageLoaderProvider?.invoke() ?: SingletonImageLoader.get(context)

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = normalizedUrl,
            contentDescription = null,
            imageLoader = resolvedImageLoader,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onError = { failed = true },
        )
    }
}

internal fun normalizeRemotePatternImageUrl(imageUrl: String?): String? {
    val trimmedUrl = imageUrl?.trim().orEmpty()
    if (trimmedUrl.isEmpty()) return null

    val uri =
        try {
            URI(trimmedUrl)
        } catch (_: URISyntaxException) {
            return null
        }

    if (
        !uri.scheme.equals("https", ignoreCase = true) ||
        uri.host.isNullOrBlank() ||
        uri.port > 65_535
    ) {
        return null
    }

    return trimmedUrl
}
