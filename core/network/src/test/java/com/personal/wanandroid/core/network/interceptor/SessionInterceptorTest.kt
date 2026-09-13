package com.personal.wanandroid.core.network.interceptor

import com.personal.wanandroid.core.network.di.NetworkModule
import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.session.MemorySessionStorage
import com.personal.wanandroid.core.network.session.SessionNotice
import com.personal.wanandroid.core.network.session.SessionPhase
import com.personal.wanandroid.core.network.session.SessionStore
import java.io.IOException
import java.lang.reflect.Proxy
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SessionInterceptorTest {
    private val store = SessionStore(MemorySessionStorage()) { 1_000 }
    private val interceptor = SessionInterceptor(store)
    private val user = UserDto(7, "fixture")
    private var sent: Request? = null
    private fun login(value: String = "fixture-token") {
        store.commitLogin(
            store.beginLogin(),
            user,
            listOf(
                Cookie.Builder().name("session").value(value).hostOnlyDomain("wanandroid.com")
                    .path("/").expiresAt(100_000).build()
            )
        )
    }
    private fun call(
        request: Request = Request.Builder().url(
            "https://wanandroid.com/user/lg/userinfo/json"
        ).build(),
        json: String = "{\"errorCode\":0,\"data\":{}}",
        code: Int = 200,
        cookie: String? = null,
        duringRequest: () -> Unit = {}
    ): Response {
        val chain = Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)
        ) { _, method, args ->
            when (method.name) {
                "request" -> request

                "proceed" -> {
                    sent = args!![0] as Request
                    duringRequest()
                    Response.Builder().request(sent!!).protocol(Protocol.HTTP_1_1).code(code)
                        .message("fixture").body(json.toResponseBody()).apply {
                            if (cookie != null) addHeader("Set-Cookie", cookie)
                        }.build()
                }

                else -> error("Unexpected Chain method: ${method.name}")
            }
        } as Interceptor.Chain
        return interceptor.intercept(chain)
    }

    @Test fun sendsOnlyMatchedSessionAndReplacesCallerCookie() {
        login()
        call(
            Request.Builder().url(
                "https://wanandroid.com/user/lg/userinfo/json"
            ).header("Cookie", "spoof=fixture").build()
        ).close()
        assertEquals("session=fixture-token", sent?.header("Cookie"))
    }

    @Test fun thirdPartyAndCleartextRequestsNeverReachTransport() {
        login()
        for (url in listOf(
            "https://example.org/",
            "http://wanandroid.com/",
            "https://wanandroid.com:8443/"
        )) {
            try {
                call(Request.Builder().url(url).build())
                fail("Expected origin rejection")
            } catch (
                _: IOException
            ) { }
        }
        assertNull(sent)
    }

    @Test fun sessionExpiredBusinessResponseIsStillReadableAndClearsSession() {
        login()
        call(json = "{\"errorCode\":-1001,\"data\":null}").use {
            assertTrue(it.body.string().contains("-1001"))
        }
        assertEquals(SessionNotice.EXPIRED, store.state.value.notice)
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
    }

    @Test fun unauthorizedHttpAlsoExpiresCurrentSession() {
        login()
        call(code = 401).close()
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
    }

    @Test fun oldResponseCannotExpireNewLogin() {
        login()
        try {
            call(json = "{\"errorCode\":-1001}", duringRequest = {
                store.detach()
                login("new-fixture")
            }).close()
            fail("Stale response must be rejected")
        } catch (_: IOException) { }
        assertEquals(SessionPhase.AUTHENTICATED, store.state.value.phase)
        assertEquals("session=new-fixture", store.cookieHeader(store.capture(), SessionStore.API))
    }

    @Test fun loginResponseCookiesAreNotInstalledAutomatically() {
        val attempt = store.beginLogin()
        val request = Request.Builder().url("https://wanandroid.com/user/login")
            .tag(
                com.personal.wanandroid.core.network.session.SessionRequest::class.java,
                attempt
            ).build()
        call(request, cookie = "session=uncommitted; Path=/; Max-Age=100").close()
        assertNull(sent?.header("Cookie"))
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
    }

    @Test fun redirectsAndTransportRetriesAreDisabled() {
        val client = NetworkModule.client(interceptor)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
    }
}
