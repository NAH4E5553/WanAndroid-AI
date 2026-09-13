package com.personal.wanandroid.core.network.interceptor

import java.io.IOException
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** Debug-only installation. Logs allowlisted endpoints, never headers, bodies or query values. */
internal class CollectionTraceInterceptor(private val log: (String) -> Unit) : Interceptor {
    private val requests = AtomicLong()
    private val endpoints = Regex(
        "/(article/list/[0-9]+/json|lg/collect/list/[0-9]+/json|" +
            "lg/(collect|uncollect|uncollect_originId)/[0-9]+/json|user/logout/json)"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (!endpoints.matches(path)) return chain.proceed(request)
        val id = requests.incrementAndGet()
        val category = if (request.url.queryParameter("cid") == null) "none" else "present"
        log("http.start request=$id method=${request.method} endpoint=$path category=$category")
        return try {
            chain.proceed(request).also {
                log("http.end request=$id status=${it.code}")
                if (path.startsWith("/lg/collect/list/")) traceBlogLinks(it, id)
            }
        } catch (failure: IOException) {
            log("http.failed request=$id")
            throw failure
        }
    }

    private fun traceBlogLinks(response: Response, requestId: Long) {
        // Only URL shape and numeric IDs for official blog paths; never log raw payloads/URLs.
        runCatching {
            val rows = Json.parseToJsonElement(response.peekBody(1_048_576).string())
                .jsonObject["data"]?.jsonObject?.get("datas")?.jsonArray.orEmpty()
            rows.forEach { row ->
                val item = row.jsonObject
                val uri = URI(item["link"]?.jsonPrimitive?.contentOrNull ?: return@forEach)
                if (!Regex("/?blog/show/[0-9]+/?").matches(uri.rawPath.orEmpty())) return@forEach
                val origin = item["originId"]?.jsonPrimitive?.longOrNull
                val scheme = when (uri.scheme?.lowercase()) {
                    null -> "none"
                    "http" -> "http"
                    "https" -> "https"
                    else -> "other"
                }
                val officialHost =
                    uri.host?.lowercase() in setOf("wanandroid.com", "www.wanandroid.com")
                log(
                    "collection.link request=$requestId article=$origin scheme=$scheme " +
                        "absolute=${uri.isAbsolute} officialHost=$officialHost"
                )
            }
        }
    }
}
