package com.personal.wanandroid.feature.article

import java.net.URI
import java.util.Locale

internal sealed interface ReaderDestination {
    data class InApp(val url: String) : ReaderDestination
    data class External(val url: String) : ReaderDestination
    data object Blocked : ReaderDestination
}

/** Adapted from CoolMall WebUrlPolicy (cf5029b): article hosts vary; schemes remain restricted. */
internal object ReaderUrlPolicy {
    fun classify(raw: String): ReaderDestination {
        val value = raw.trim()
        if (value.isEmpty() || value.length > 8192 || value.any { it.isISOControl() }) {
            return ReaderDestination.Blocked
        }
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return ReaderDestination.Blocked
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return ReaderDestination.Blocked
        if (scheme == "https" || scheme == "http") {
            val host = uri.host?.lowercase(Locale.ROOT) ?: return ReaderDestination.Blocked
            val port = if (scheme == "https") 443 else 80
            if (uri.rawUserInfo != null ||
                uri.port !in setOf(-1, port)
            ) {
                return ReaderDestination.Blocked
            }
            val url = buildString {
                append("$scheme://$host")
                append(uri.rawPath.orEmpty().ifEmpty { "/" })
                uri.rawQuery?.let { append("?$it") }
                uri.rawFragment?.let { append("#$it") }
            }
            return if (scheme ==
                "https"
            ) {
                ReaderDestination.InApp(url)
            } else {
                ReaderDestination.External(url)
            }
        }
        // Never parse intent:// payloads, javascript:, file:, content: or arbitrary app schemes.
        if (scheme in setOf("tel", "mailto", "geo") && !uri.rawSchemeSpecificPart.isNullOrBlank() &&
            !Regex("%0[ad]", RegexOption.IGNORE_CASE).containsMatchIn(value)
        ) {
            return ReaderDestination.External(value)
        }
        return ReaderDestination.Blocked
    }

    fun inAppUrl(raw: String): String? = (classify(raw) as? ReaderDestination.InApp)?.url
    fun externalUrl(raw: String): String? = when (val destination = classify(raw)) {
        is ReaderDestination.InApp -> destination.url
        is ReaderDestination.External -> destination.url
        ReaderDestination.Blocked -> null
    }
    fun samePage(first: String, second: String): Boolean =
        inAppUrl(first)?.substringBefore('#')?.let {
            it == inAppUrl(second)?.substringBefore('#')
        } ==
            true
}
