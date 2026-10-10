package com.finnvek.knittools.data.remote

import android.content.Context
import com.finnvek.knittools.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/** Sovelluksen oman Ravelry-selaimen lataus: osoite, selaimen tunniste ja evästeet osoitekohtaisesti. */
data class WebPdfDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String?,
    val mimeType: String?,
    /** Evästeet juuri tälle osoitteelle; uudelleenohjauksessa toinen palvelin ei saa Ravelryn evästeitä. */
    val cookieFor: (String) -> String?,
)

/**
 * Lataa selaimesta napautetun PDF:n väliaikaiseen tiedostoon. Vain HTTPS, uudelleenohjaukset
 * seurataan itse (evästeet osoitteen mukaan), ja koko on rajattu.
 */
@Singleton
class WebPdfDownloader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun download(download: WebPdfDownload): File? =
            withContext(ioDispatcher) {
                var url = download.url
                repeat(MAX_REDIRECTS + 1) {
                    if (!url.startsWith("https://", ignoreCase = true)) return@withContext null
                    val connection = open(url, download)
                    try {
                        when (connection.responseCode) {
                            in HttpURLConnection.HTTP_MULT_CHOICE..HTTP_LAST_REDIRECT -> {
                                val location = connection.getHeaderField("Location") ?: return@withContext null
                                url = URL(URL(url), location).toString()
                            }

                            in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE ->
                                return@withContext writeToTempFile(connection)

                            else -> return@withContext null
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
                null
            }

        private fun open(
            url: String,
            download: WebPdfDownload,
        ): HttpURLConnection =
            (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("User-Agent", download.userAgent)
                download.cookieFor(url)?.takeIf(String::isNotBlank)?.let { setRequestProperty("Cookie", it) }
            }

        private suspend fun writeToTempFile(connection: HttpURLConnection): File {
            val directory = File(context.cacheDir, TEMP_DIRECTORY).apply { mkdirs() }
            val target = File.createTempFile("pattern-", ".pdf", directory)
            var completed = false
            try {
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_BYTES)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_DOWNLOAD_BYTES) throw IOException("PDF is too large")
                            output.write(buffer, 0, read)
                        }
                    }
                }
                completed = true
                return target
            } finally {
                // Keskeytynyt tai liian suuri lataus ei jätä puolikasta tiedostoa välimuistiin.
                if (!completed && target.exists() && !target.delete()) target.deleteOnExit()
            }
        }

        private companion object {
            const val MAX_REDIRECTS = 5
            const val HTTP_LAST_REDIRECT = 399
            const val TIMEOUT_MILLIS = 30_000
            const val BUFFER_BYTES = 64 * 1024

            // Neuleohjeet ovat muutamia megatavuja; raja estää selaimen kautta tulevan jättitiedoston.
            const val MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024
            const val TEMP_DIRECTORY = "web_pdf_downloads"
        }
    }
