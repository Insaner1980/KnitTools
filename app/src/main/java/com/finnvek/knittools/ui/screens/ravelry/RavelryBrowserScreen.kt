package com.finnvek.knittools.ui.screens.ravelry

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.theme.ComponentDimens

/**
 * Ravelry sovelluksen sisällä. Chromen välilehdessä ladattu PDF meni puhelimen latauskansioon;
 * täällä Download PDF ladataan suoraan KnitToolsiin ja avataan Tallenna ohje -sheetiin.
 * Kirjautuminen tapahtuu Ravelryn omalla sivulla, eikä salasana kulje sovelluksen kautta.
 * Jaettu tai tallennettu Ravelry-linkki avautuu suoraan ([startUrl]); muut osoitteet aloittavat kaavahausta.
 */
@SuppressLint("SetJavaScriptEnabled") // Ravelryn sivut ja kirjautuminen vaativat JavaScriptin.
@Composable
fun RavelryBrowserScreen(
    startUrl: String?,
    onBack: () -> Unit,
    onPdfDownload: (WebPdfDownload) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    val currentOnPdfDownload by rememberUpdatedState(onPdfDownload)

    // Järjestelmän takaisin-ele selaa ensin Ravelryn sivuhistoriaa.
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    ToolScreenScaffold(
        title = stringResource(R.string.tool_ravelry),
        onBack = onBack,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = stringResource(R.string.ravelry_browser_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = ComponentDimens.ContentPadding,
                        vertical = ComponentDimens.StandardSpacing,
                    ),
            )
            if (progress in 1 until FULL_PROGRESS) {
                LinearProgressIndicator(
                    progress = { progress / FULL_PROGRESS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // Täysi koko myös näkymälle itselleen: ilman tätä 100vh-korkuiset paneelit
                        // (esim. Ravelryn suodattimet) jäivät nollan korkuisiksi.
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        CookieManager.getInstance().setAcceptCookie(true)
                        webViewClient =
                            object : WebViewClient() {
                                // Vain HTTPS-sivut; muut osoitteet (http, intent, tiedostot) estetään.
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean = request.url.scheme != HTTPS

                                override fun onPageStarted(
                                    view: WebView,
                                    url: String?,
                                    favicon: Bitmap?,
                                ) {
                                    canGoBack = view.canGoBack()
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    canGoBack = view.canGoBack()
                                }
                            }
                        webChromeClient =
                            object : WebChromeClient() {
                                override fun onProgressChanged(
                                    view: WebView,
                                    newProgress: Int,
                                ) {
                                    progress = newProgress
                                }
                            }
                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            currentOnPdfDownload(
                                WebPdfDownload(
                                    url = url,
                                    userAgent = userAgent,
                                    contentDisposition = contentDisposition,
                                    mimeType = mimeType,
                                    cookieFor = { address -> CookieManager.getInstance().getCookie(address) },
                                ),
                            )
                        }
                        loadUrl(startUrl?.let(::ravelryPageUrlOrNull) ?: RAVELRY_PATTERN_SEARCH_URL)
                        webView = this
                    }
                },
                onRelease = { view ->
                    CookieManager.getInstance().flush()
                    view.destroy()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val HTTPS = "https"
private const val FULL_PROGRESS = 100
private const val RAVELRY_PATTERN_SEARCH_URL = "https://www.ravelry.com/patterns/search"
