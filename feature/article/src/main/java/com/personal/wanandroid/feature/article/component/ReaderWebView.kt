package com.personal.wanandroid.feature.article.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.personal.wanandroid.feature.article.policy.ReaderDestination
import com.personal.wanandroid.feature.article.policy.ReaderUrlPolicy
import com.personal.wanandroid.feature.article.state.ReaderEvent
import com.personal.wanandroid.feature.article.state.ReaderFailure
import java.io.ByteArrayInputStream

/** Adapted from CoolMall WebViewSecurity (cf5029b); no API Cookie/header or native JS bridge. */
@SuppressLint("SetJavaScriptEnabled")
internal fun WebView.configureReader(dark: Boolean, fontScale: Float) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(true) // New-window requests are denied by WebChromeClient.
        allowFileAccess = false
        allowContentAccess = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        mediaPlaybackRequiresUserGesture = true
        if (Build.VERSION.SDK_INT >= 26) safeBrowsingEnabled = true
        if (Build.VERSION.SDK_INT >= 33) isAlgorithmicDarkeningAllowed = dark
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = true
        displayZoomControls = false
        textZoom = (fontScale * 100).toInt().coerceIn(50, 300)
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
    // Keep WebView's cookie store independent from the API's HTTP client.
    WebView.setWebContentsDebuggingEnabled(false)
}

internal class ReaderWebView(context: Context) : WebView(context) {
    var rendererGone = false
    var released = false
        private set
    fun release() {
        if (released) return
        released = true
        if (!rendererGone) {
            stopLoading()
            onPause()
            webChromeClient = null
            webViewClient = WebViewClient()
            removeAllViews()
        }
        (parent as? ViewGroup)?.removeView(this)
        destroy()
    }
}

internal open class ReaderWebViewClient(private val emit: (ReaderEvent) -> Unit) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        navigate(request.url.toString(), request.isForMainFrame, request.hasGesture())

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        navigate(url, true, view.hitTestResult.type != WebView.HitTestResult.UNKNOWN_TYPE)

    private fun navigate(url: String, mainFrame: Boolean, gesture: Boolean): Boolean =
        when (ReaderUrlPolicy.classify(url)) {
            is ReaderDestination.InApp -> false

            is ReaderDestination.External -> {
                if (mainFrame && gesture) {
                    emit(ReaderEvent.External(url))
                } else if (mainFrame) {
                    emit(ReaderEvent.Blocked)
                }
                true
            }

            ReaderDestination.Blocked -> {
                if (mainFrame) emit(ReaderEvent.Blocked)
                true
            }
        }

    // This runs off Main. Only return a response; never mutate UI state here.
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? = if (ReaderUrlPolicy.inAppUrl(request.url.toString()) == null) {
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(byteArrayOf())
        )
    } else {
        null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (ReaderUrlPolicy.inAppUrl(url) == null) {
            view.stopLoading()
            emit(ReaderEvent.Unsafe)
        } else if (isCurrent(view, url)) {
            emit(ReaderEvent.Started(url))
        }
    }
    override fun onPageFinished(view: WebView, url: String) {
        if (isCurrent(view, url)) {
            emit(ReaderEvent.History(url, view.canGoBack()))
            emit(ReaderEvent.Finished(url, view.title))
        }
    }
    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        if (isCurrent(view, url)) emit(ReaderEvent.History(url, view.canGoBack()))
    }
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame && isCurrent(view, request.url.toString())) {
            emit(
                ReaderEvent.Failed(
                    request.url.toString(),
                    if (error.errorCode ==
                        ERROR_FAILED_SSL_HANDSHAKE
                    ) {
                        ReaderFailure.TLS
                    } else {
                        ReaderFailure.NETWORK
                    }
                )
            )
        }
    }
    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (request.isForMainFrame && isCurrent(view, request.url.toString())) {
            emit(ReaderEvent.Failed(request.url.toString(), ReaderFailure.HTTP))
        }
    }
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        if (isCurrent(view, error.url)) emit(ReaderEvent.Failed(error.url, ReaderFailure.TLS))
    }
    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse
    ) {
        if (Build.VERSION.SDK_INT >= 27) callback.backToSafety(false)
        if (request.isForMainFrame) {
            emit(ReaderEvent.Unsafe)
        }
    }
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        (view as ReaderWebView).rendererGone = true
        emit(ReaderEvent.RendererGone)
        view.release()
        return true
    }
    private fun isCurrent(view: WebView, url: String): Boolean =
        ReaderUrlPolicy.samePage(view.url ?: view.originalUrl.orEmpty(), url)
}

internal class ReaderChromeClient(private val emit: (ReaderEvent) -> Unit) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        emit(ReaderEvent.Progress(view.url.orEmpty(), newProgress))
    }
    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }
}
