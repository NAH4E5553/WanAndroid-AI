package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.di.NetworkModule
import com.personal.wanandroid.core.network.interceptor.SessionInterceptor
import com.personal.wanandroid.core.network.session.MemorySessionStorage
import com.personal.wanandroid.core.network.session.SessionPhase
import com.personal.wanandroid.core.network.session.SessionStore
import kotlinx.coroutines.test.runTest
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrofitAuthDataSourceTest {
    @Test fun formLoginThenVerificationAndEmptyLogoutUseFixedResponses() = runTest {
        val store = SessionStore(MemorySessionStorage())
        val requests = mutableListOf<Request>()
        val user = """{"id":7,"username":"fixture","nickname":null,"password":"ignored"}"""
        val client = NetworkModule.client(SessionInterceptor(store)).newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                requests.add(request)
                val payload = when (request.url.encodedPath) {
                    "/user/login" -> """{"errorCode":0,"data":$user}"""
                    "/user/lg/userinfo/json" -> """{"errorCode":0,"data":{"userInfo":$user}}"""
                    "/user/logout/json" -> """{"errorCode":0,"data":null}"""
                    else -> error("Unexpected fixture route")
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
                    .message(
                        "fixture"
                    ).body(payload.toResponseBody("application/json".toMediaType()))
                    .apply {
                        if (request.url.encodedPath == "/user/login") {
                            addHeader(
                                "Set-Cookie",
                                "session=fixture-token; Path=/; Max-Age=3600; Secure; HttpOnly"
                            )
                        }
                    }.build()
            }.build()
        val source = RetrofitAuthDataSource(
            NetworkModule.authService(NetworkModule.retrofit(client, NetworkModule.json())),
            store
        )
        val attempt = store.beginLogin()
        val response = source.login("fixture", "fixture-password", attempt)
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        val form = requests.single().body as FormBody
        assertEquals("username", form.name(0))
        assertEquals("fixture", form.value(0))
        assertEquals("password", form.name(1))
        assertEquals("fixture-password", form.value(1))
        assertNull(requests.single().header("Cookie"))
        assertTrue(response.commit(requireNotNull(response.body.data)))
        assertNotNull(source.userInfo(store.capture()).data)
        assertEquals("session=fixture-token", requests.last().header("Cookie"))
        assertNull(source.logout(store.detach()).data)
        assertEquals("session=fixture-token", requests.last().header("Cookie"))
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
    }
}
