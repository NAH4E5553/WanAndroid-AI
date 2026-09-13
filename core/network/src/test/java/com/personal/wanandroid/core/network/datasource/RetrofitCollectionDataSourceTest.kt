package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.di.NetworkModule
import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.interceptor.SessionInterceptor
import com.personal.wanandroid.core.network.session.MemorySessionStorage
import com.personal.wanandroid.core.network.session.SessionStore
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrofitCollectionDataSourceTest {
    @Test fun fixedTransportDistinguishesAllIdsAndNeverUsesRealNetwork() = runTest {
        val store = SessionStore(MemorySessionStorage())
        val login = store.beginLogin()
        val cookie = Cookie.Builder().name(
            "session"
        ).value("fixture").hostOnlyDomain("wanandroid.com").build()
        assertTrue(store.commitLogin(login, UserDto(7, "fixture"), listOf(cookie)))
        val requests = mutableListOf<Request>()
        val client = NetworkModule.client(
            SessionInterceptor(store)
        ).newBuilder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            val body = if (request.method == "GET") {
                """{"errorCode":0,"data":{"datas":[{"id":900,"originId":42,"title":"Fixture","link":"https://example.invalid"}],"curPage":1,"over":true,"total":1}}"""
            } else {
                """{"errorCode":0,"data":null}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
                .message(
                    "fixture"
                ).body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val source =
            RetrofitCollectionDataSource(
                NetworkModule.collectionService(
                    NetworkModule.retrofit(client, NetworkModule.json())
                )
            )
        val tag = store.capture()
        val item = source.list(0, tag).data!!.datas.single()
        assertEquals(900L, item.id)
        assertEquals(42L, item.originId)
        assertNull(source.collect(42, tag).data)
        source.uncollectArticle(42, tag)
        source.uncollectRecord(900, 42, tag)
        source.uncollectRecord(901, -1, tag)
        assertEquals(
            listOf(
                "/lg/collect/list/0/json",
                "/lg/collect/42/json",
                "/lg/uncollect_originId/42/json",
                "/lg/uncollect/900/json",
                "/lg/uncollect/901/json"
            ),
            requests.map {
                it.url.encodedPath
            }
        )
        assertEquals("20", requests.first().url.queryParameter("page_size"))
        assertEquals(listOf("GET", "POST", "POST", "POST", "POST"), requests.map { it.method })
        assertEquals("42", (requests[3].body as FormBody).value(0))
        assertEquals("originId", (requests[4].body as FormBody).name(0))
        assertEquals("-1", (requests[4].body as FormBody).value(0))
        assertTrue(
            requests.all {
                it.url.host == "wanandroid.com" &&
                    it.header("Cookie") == "session=fixture"
            }
        )
    }
}
