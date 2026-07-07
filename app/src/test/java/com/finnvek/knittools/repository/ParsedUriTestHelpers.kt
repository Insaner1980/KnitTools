package com.finnvek.knittools.repository

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

internal suspend inline fun withParsedAppUri(
    uriString: String,
    pathSegments: List<String>,
    block: suspend () -> Unit,
) = withParsedUri(
    uriString = uriString,
    configure = { uri ->
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.finnvek.knittools.fileprovider"
        every { uri.pathSegments } returns pathSegments
    },
    block = block,
)

internal suspend inline fun withParsedFileUri(
    uriString: String,
    path: String,
    block: suspend () -> Unit,
) = withParsedUri(
    uriString = uriString,
    configure = { uri ->
        every { uri.scheme } returns "file"
        every { uri.path } returns path
    },
    block = block,
)

private suspend inline fun withParsedUri(
    uriString: String,
    configure: (Uri) -> Unit,
    block: suspend () -> Unit,
) {
    mockkStatic(Uri::class)
    try {
        val uri = mockk<Uri>(relaxed = true)
        every { Uri.parse(uriString) } returns uri
        configure(uri)
        block()
    } finally {
        unmockkStatic(Uri::class)
    }
}
