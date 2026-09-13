package com.personal.wanandroid.core.data.mapper

import java.net.URI

private val officialBlogPath = Regex("/?blog/show/[0-9]+/?")
private val articleOrigin = URI("https://wanandroid.com/")

/** The collections API can return a site-relative link for WanAndroid's own blog articles. */
internal fun normalizeArticleLink(raw: String): String {
    val value = raw.trim()
    if (value.length > 8192 || value.any { it.isISOControl() }) return raw
    val uri = try {
        URI(value)
    } catch (_: Exception) {
        return raw
    }
    // Never resolve protocol-relative hosts, arbitrary paths or external URLs against this origin.
    if (uri.isAbsolute || uri.rawAuthority != null ||
        !officialBlogPath.matches(uri.rawPath.orEmpty())
    ) {
        return raw
    }
    return articleOrigin.resolve(uri).toASCIIString()
}
