package com.finnvek.knittools.data.remote

import com.finnvek.knittools.domain.model.normalizeRemotePatternImageUrl
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import java.io.IOException

internal val PatternThumbnailNetwork =
    createClientPlugin("PatternThumbnailNetwork") {
        // SendingRequest also runs before each request issued by Ktor's redirect handler.
        on(SendingRequest) { request, _ ->
            if (normalizeRemotePatternImageUrl(request.url.buildString()) == null) {
                throw IOException("Untrusted pattern thumbnail destination")
            }
        }
    }
