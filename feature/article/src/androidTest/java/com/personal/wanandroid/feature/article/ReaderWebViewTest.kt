package com.personal.wanandroid.feature.article

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderWebViewTest {
    @get:Rule val compose = createComposeRule()
    private val events = CopyOnWriteArrayList<ReaderEvent>()
    private lateinit var browser: ReaderWebView
    private lateinit var client: ReaderWebViewClient
    private val visible = mutableStateOf(true)
    private val one = "https://reader.invalid/one"
    private val two = "https://reader.invalid/two"

    private fun mount() {
        compose.setContent {
            if (visible.value) {
                AndroidView(factory = { context ->
                    ReaderWebView(context).also { view ->
                        browser = view
                        view.configureReader(dark = true, fontScale = 1.5f)
                        client = object : ReaderWebViewClient({ events.add(it) }) {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse {
                                super.shouldInterceptRequest(view, request)?.let { return it }
                                val missing = request.url.path in setOf("/missing", "/img-missing")
                                val html = "<html><head><title>Fixture</title></head>" +
                                    "<body>Offline fixture<img src='/img-missing'>" +
                                    "<a href='$two'>Next</a></body></html>"
                                return WebResourceResponse(
                                    "text/html",
                                    "UTF-8",
                                    if (missing) 404 else 200,
                                    if (missing) "Not Found" else "OK",
                                    emptyMap(),
                                    ByteArrayInputStream(html.toByteArray())
                                )
                            }
                        }
                        view.webViewClient = client
                        view.webChromeClient = ReaderChromeClient { events.add(it) }
                        view.loadUrl(one)
                    }
                }, onRelease = { it.release() }, modifier = Modifier.fillMaxSize())
            }
        }
        finished(one)
    }
    private fun finished(url: String) {
        compose.waitUntil(15_000) { events.any { it is ReaderEvent.Finished && it.url == url } }
    }

    @Test fun realWebViewLoadsFakeHtmlAndBackUsesWebHistory() {
        mount()
        assertTrue(events.any { it is ReaderEvent.Progress && it.percent > 0 })
        assertTrue(events.any { it is ReaderEvent.Finished && it.title == "Fixture" })
        assertFalse(events.any { it is ReaderEvent.Failed })
        compose.runOnIdle { browser.loadUrl(two) }
        finished(two)
        events.clear()
        compose.runOnIdle {
            assertTrue(browser.canGoBack())
            navigateReaderBack(browser) { error("Must return in webpage history") }
        }
        finished(one)
        compose.runOnIdle {
            assertFalse(browser.canGoBack())
            var exits = 0
            navigateReaderBack(browser) { exits++ }
            assertEquals(1, exits)
        }
    }

    @Test fun settingsRestrictFileMixedContentCookiesAndDisposalReleases() {
        mount()
        compose.runOnIdle {
            assertFalse(browser.settings.allowFileAccess)
            assertFalse(browser.settings.allowContentAccess)
            assertFalse(browser.settings.javaScriptCanOpenWindowsAutomatically)
            assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, browser.settings.mixedContentMode)
            assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(browser))
            assertEquals(150, browser.settings.textZoom)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                assertTrue(browser.settings.isAlgorithmicDarkeningAllowed)
            }
            visible.value = false
        }
        compose.waitUntil { browser.released }
        compose.runOnIdle { browser.release() }
    }

    @Test fun externalLinkRequiresMainFrameAndGesture() {
        mount()
        events.clear()
        compose.runOnIdle {
            assertTrue(
                client.shouldOverrideUrlLoading(
                    browser,
                    Request("tel:123", main = false, gesture = true)
                )
            )
            assertTrue(
                client.shouldOverrideUrlLoading(
                    browser,
                    Request("tel:123", main = true, gesture = false)
                )
            )
            assertFalse(events.any { it is ReaderEvent.External })
            assertTrue(
                client.shouldOverrideUrlLoading(
                    browser,
                    Request("tel:123", main = true, gesture = true)
                )
            )
            assertEquals(
                listOf(ReaderEvent.External("tel:123")),
                events.filterIsInstance<ReaderEvent.External>()
            )
            assertTrue(
                client.shouldOverrideUrlLoading(browser, Request("intent://evil", true, true))
            )
            assertFalse(client.shouldOverrideUrlLoading(browser, Request(two, true, true)))
        }
    }

    @Test fun mainHttpFailureIsReportedButSubresourceFailureIsIgnored() {
        mount()
        assertFalse(events.any { it is ReaderEvent.Failed })
        events.clear()
        compose.runOnIdle { browser.loadUrl("https://reader.invalid/missing") }
        compose.waitUntil(15_000) {
            events.any {
                it is ReaderEvent.Failed &&
                    it.reason == ReaderFailure.HTTP
            }
        }
        finished("https://reader.invalid/missing")
        assertTrue(
            events.filterIsInstance<ReaderEvent.Failed>().all {
                it.url == "https://reader.invalid/missing"
            }
        )
        assertFalse(
            events.any {
                it is ReaderEvent.Failed && it.url.endsWith("/img-missing")
            }
        )
    }

    @Test fun inMemoryRestorationRetainsNativeHistory() {
        mount()
        compose.runOnIdle { browser.loadUrl(two) }
        finished(two)
        compose.runOnIdle {
            val saved = android.os.Bundle()
            assertNotNull(browser.saveState(saved))
            val restored = ReaderWebView(browser.context)
            try {
                restored.configureReader(false, 1f)
                assertNotNull(restored.restoreState(saved))
                assertTrue(restored.canGoBack())
                assertEquals(two, restored.url)
            } finally {
                restored.release()
            }
        }
    }
}
private class Request(
    private val value: String,
    private val main: Boolean,
    private val gesture: Boolean
) : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(value)
    override fun isForMainFrame() = main
    override fun isRedirect() = false
    override fun hasGesture() = gesture
    override fun getMethod() = "GET"
    override fun getRequestHeaders(): Map<String, String> = emptyMap()
}
