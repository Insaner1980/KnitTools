package com.finnvek.knittools.ui.screens.ravelry

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.htmlEncode
import com.finnvek.knittools.BuildConfig
import com.finnvek.knittools.R
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.ui.components.ToolScreenScaffold
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.layout.contentWidth
import com.finnvek.knittools.ui.platform.ExternalWebLinkOpenResult
import com.finnvek.knittools.ui.platform.openExternalWebLink
import com.finnvek.knittools.ui.theme.ComponentDimens
import java.io.ByteArrayInputStream

/**
 * Ravelry sovelluksen sisällä. Chromen välilehdessä ladattu PDF meni puhelimen latauskansioon;
 * täällä Download PDF ladataan suoraan KnitToolsiin ja avataan Tallenna ohje -sheetiin.
 * Kirjautuminen tapahtuu Ravelryn omalla sivulla, eikä salasana kulje sovelluksen kautta.
 * Jaettu tai tallennettu Ravelry-linkki avautuu suoraan ([startUrl]); muut osoitteet aloittavat kaavahausta.
 */
@Composable
fun RavelryBrowserScreen(
    startUrl: String?,
    requestId: String,
    onBack: () -> Unit,
    onPdfDownload: (WebPdfDownload) -> Unit,
) {
    val browserState =
        rememberSaveable(requestId, saver = RavelryBrowserState.saver(requestId)) {
            RavelryBrowserState(requestId)
        }
    val currentOnPdfDownload by rememberUpdatedState(onPdfDownload)
    val context = LocalContext.current
    val purchaseMessage = stringResource(R.string.ravelry_purchase_in_browser)
    val noBrowserMessage = stringResource(R.string.web_pattern_no_browser)
    val openOutside by rememberUpdatedState<(String, String?) -> Unit> { url, message ->
        val opened = openExternalWebLink(context, url) == ExternalWebLinkOpenResult.Opened
        (if (opened) message else noBrowserMessage)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // Järjestelmän takaisin-ele selaa ensin Ravelryn sivuhistoriaa.
    BackHandler(enabled = browserState.canGoBack) {
        if (!browserState.goBack()) onBack()
    }

    ToolScreenScaffold(
        title = stringResource(R.string.tool_ravelry),
        onBack = onBack,
        contentWidthPolicy = ContentWidthPolicy.Unrestricted,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = stringResource(R.string.ravelry_browser_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.align(Alignment.CenterHorizontally).contentWidth(ContentWidthPolicy.Form).padding(
                        horizontal = ComponentDimens.ContentPadding,
                        vertical = ComponentDimens.StandardSpacing,
                    ),
            )
            if (browserState.progress in 1 until FULL_PROGRESS) {
                LinearProgressIndicator(
                    progress = { browserState.progress / FULL_PROGRESS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (browserState.rendererLost) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .contentWidth(ContentWidthPolicy.Form)
                            .padding(ComponentDimens.ContentPadding),
                ) {
                    Text(
                        text = stringResource(R.string.generic_error_unknown),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    TextButton(onClick = browserState::retry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
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
                            configureRavelryBrowser(
                                browserState = browserState,
                                onPdfDownload = { currentOnPdfDownload(it) },
                                openOutside = { url, message -> openOutside(url, message) },
                                purchaseMessage = purchaseMessage,
                            )
                            browserState.restoreOrLoad(this, startUrl) { openOutside(it, purchaseMessage) }
                            browserState.updateBack(this)
                            browserState.updateProgress(this, progress)
                        }
                    },
                    onRelease = browserState::release,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled") // Ravelry pages and sign-in require JavaScript.
internal fun WebView.configureRavelryBrowser(
    browserState: RavelryBrowserState,
    onPdfDownload: (WebPdfDownload) -> Unit,
    openOutside: (String, String?) -> Unit,
    purchaseMessage: String,
) {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    CookieManager.getInstance().setAcceptCookie(true)
    webViewClient =
        object : WebViewClient() {
            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail,
            ): Boolean {
                browserState.rendererGone(view)
                return true
            }

            // Only the current instance may navigate, open an external app or update UI state.
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                if (browserState.webView !== view) return true
                val url = request.url
                val navigation =
                    ravelryNavigation(
                        scheme = url.scheme,
                        host = url.host,
                        path = url.path,
                        isMainFrame = request.isForMainFrame,
                        isRedirect = request.isRedirect,
                    )
                when (navigation) {
                    // Kassaosoite ei toimi selaimessa ilman sovelluksen kirjautumista (lahjaoston
                    // uudelleenohjaus näytti virhesivun), joten selain avaa sivun, jolta osto alkoi.
                    RavelryNavigation.Purchase -> openOutside(browserState.fallbackUrl, purchaseMessage)

                    RavelryNavigation.External -> openOutside(url.toString(), null)

                    RavelryNavigation.InApp, RavelryNavigation.Blocked -> Unit
                }
                return navigation != RavelryNavigation.InApp
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = interceptRavelryRequest(view, request, browserState, openOutside, purchaseMessage)

            override fun onPageStarted(
                view: WebView,
                url: String?,
                favicon: Bitmap?,
            ) {
                browserState.updateBack(view)
            }

            override fun doUpdateVisitedHistory(
                view: WebView,
                url: String?,
                isReload: Boolean,
            ) {
                browserState.visited(view, url)
            }
        }
    webChromeClient =
        object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView,
                newProgress: Int,
            ) {
                browserState.updateProgress(view, newProgress)
            }
        }
    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        if (browserState.webView === this) {
            onPdfDownload(
                WebPdfDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    cookieFor = { address -> CookieManager.getInstance().getCookie(address) },
                ),
            )
        }
    }
}

private fun interceptRavelryRequest(
    view: WebView,
    request: WebResourceRequest,
    browserState: RavelryBrowserState,
    openOutside: (String, String?) -> Unit,
    purchaseMessage: String,
): WebResourceResponse? {
    if (!request.isForMainFrame) return null
    val url = request.url
    val navigation =
        ravelryNavigation(
            scheme = url.scheme,
            host = url.host,
            path = url.path,
            isMainFrame = true,
            isRedirect = request.isRedirect,
        )
    if (navigation == RavelryNavigation.InApp) return null
    // POST requests bypass shouldOverrideUrlLoading. Block without replaying the body
    // as a GET or touching Compose/WebView state from the request worker thread.
    if (navigation == RavelryNavigation.Purchase) {
        // Lomakkeella avattu kassa ei saa jättää tyhjää sivua: kerrotaan, että osto jatkuu
        // selaimessa. Kassaosoite ei toimi ilman lomaketta eikä selaimessa ole sovelluksen
        // kirjautumista, joten selain avaa sivun, jolta ostoa yritettiin (laitetesti 2026-11-25).
        view.post {
            if (browserState.webView === view) openOutside(browserState.fallbackUrl, purchaseMessage)
        }
        return WebResourceResponse(
            "text/html",
            "UTF-8",
            403,
            "Forbidden",
            emptyMap(),
            ByteArrayInputStream(purchaseNoticePage(purchaseMessage).toByteArray(Charsets.UTF_8)),
        )
    }
    return WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Forbidden",
        emptyMap(),
        ByteArrayInputStream(byteArrayOf()),
    )
}

private const val FULL_PROGRESS = 100

internal fun purchaseNoticePage(message: String): String =
    "<!doctype html><html><head><meta charset=\"utf-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>" +
        "<body style=\"font-family:sans-serif;padding:24px;line-height:1.5\"><p>" +
        message.htmlEncode() +
        "</p></body></html>"
