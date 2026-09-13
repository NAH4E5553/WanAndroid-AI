package com.personal.wanandroid.core.network.interceptor

import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStore
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.Response

/** API-only transport; WebView never uses this client. Response effects are generation checked. */
class SessionInterceptor @Inject constructor(private val sessions: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!SessionStore.isApi(original.url)) throw IOException("Unsupported API origin")
        val context = original.tag(SessionRequest::class.java) ?: sessions.capture()
        val header = sessions.cookieHeader(context, original.url)
        val request = original.newBuilder().removeHeader("Cookie").apply {
            if (header.isNotEmpty()) header("Cookie", header)
        }.build()
        val response = chain.proceed(request)
        try {
            if (context.mode == SessionRequest.Mode.NORMAL) {
                if (!sessions.isCurrent(context)) throw IOException("Session changed")
                val expired = response.code == 401 || runCatching {
                    Json.parseToJsonElement(response.peekBody(1_048_576).string())
                        .jsonObject["errorCode"]?.jsonPrimitive?.intOrNull == -1001
                }.getOrDefault(false)
                if (expired) {
                    sessions.expire(context)
                } else {
                    sessions.updateCookies(context, Cookie.parseAll(original.url, response.headers))
                }
            }
            // Login cookies are only committed by the repository after validating the body.
            // Detached logout responses cannot delete a newer account's cookies.
            return response
        } catch (failure: Exception) {
            response.close()
            throw failure
        }
    }
}
