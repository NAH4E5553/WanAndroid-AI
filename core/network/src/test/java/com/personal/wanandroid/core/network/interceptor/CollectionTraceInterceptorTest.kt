package com.personal.wanandroid.core.network.interceptor

import java.io.IOException
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class CollectionTraceInterceptorTest {
    @Test fun malformedCollectionBodyDoesNotBreakTracingOrConsumeTheResponse() {
        val logs = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(CollectionTraceInterceptor(logs::add))
            .addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("fixture").body("not JSON".toResponseBody()).build()
            }.build()
        client.newCall(
            Request.Builder().url("https://wanandroid.com/lg/collect/list/0/json").build()
        )
            .execute().use { assertEquals("not JSON", it.body.string()) }
        assertEquals(2, logs.size)
    }

    @Test fun collectionDiagnosticsReportRelativeBlogShapeWithoutPrivateUrlContents() {
        val logs = mutableListOf<String>()
        val body = """
            {"data":{"datas":[
              {"originId":40,"link":"/bad URI"},
              null,
              {"link":{"invalid":"type"}},
              {
              "originId":42,
              "title":"private title",
              "link":"/blog/show/123?secret=private-query#private-fragment"
            }]}}
        """.trimIndent()
        val client = OkHttpClient.Builder()
            .addInterceptor(CollectionTraceInterceptor(logs::add))
            .addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("fixture").body(body.toResponseBody()).build()
            }.build()
        client.newCall(
            Request.Builder().url("https://wanandroid.com/lg/collect/list/0/json").build()
        )
            .execute().use { assertEquals(body, it.body.string()) }
        assertEquals(
            listOf(
                "http.start request=1 method=GET endpoint=/lg/collect/list/0/json category=none",
                "http.end request=1 status=200",
                "collection.link request=1 article=42 scheme=none absolute=false officialHost=false"
            ),
            logs
        )
    }

    @Test fun traceContainsOnlyAllowlistedMetadataAndDoesNotConsumeTheResponse() {
        val logs = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(CollectionTraceInterceptor(logs::add))
            .addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("fixture").header("Set-Cookie", "secret=response")
                    .body("private response".toResponseBody()).build()
            }.build()
        val request = Request.Builder()
            .url("https://wanandroid.com/article/list/0/json?cid=private-query")
            .header("Cookie", "secret=request")
            .post(FormBody.Builder().add("password", "private-body").build()).build()
        client.newCall(request).execute().use {
            assertEquals("private response", it.body.string())
        }
        assertEquals(
            listOf(
                "http.start request=1 method=POST endpoint=/article/list/0/json category=present",
                "http.end request=1 status=200"
            ),
            logs
        )
        logs.clear()
        client.newCall(request.newBuilder().url("https://wanandroid.com/user/login").build())
            .execute().close()
        assertEquals(emptyList<String>(), logs)
    }

    @Test fun failureIsPropagatedWithoutLoggingExceptionDetailsOrRetrying() {
        val logs = mutableListOf<String>()
        val failure = IOException("private exception details")
        var calls = 0
        val client = OkHttpClient.Builder()
            .addInterceptor(CollectionTraceInterceptor(logs::add))
            .addInterceptor {
                calls++
                throw failure
            }.build()
        try {
            client.newCall(Request.Builder().url("https://wanandroid.com/user/logout/json").build())
                .execute().close()
            fail("Expected failure")
        } catch (actual: IOException) {
            assertSame(failure, actual)
        }
        assertEquals(1, calls)
        assertEquals(
            listOf(
                "http.start request=1 method=GET endpoint=/user/logout/json category=none",
                "http.failed request=1"
            ),
            logs
        )
    }
}
