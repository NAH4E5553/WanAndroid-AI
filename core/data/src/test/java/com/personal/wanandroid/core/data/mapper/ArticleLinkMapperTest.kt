package com.personal.wanandroid.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleLinkMapperTest {
    @Test fun relativeOfficialBlogPathsResolveToHttpsAndPreserveQueryAndFragment() {
        listOf("/blog/show/123", "blog/show/123", " /blog/show/123 ").forEach {
            assertEquals("https://wanandroid.com/blog/show/123", normalizeArticleLink(it))
        }
        assertEquals(
            "https://wanandroid.com/blog/show/123/?q=a%2Fb#section",
            normalizeArticleLink("/blog/show/123/?q=a%2Fb#section")
        )
    }

    @Test fun externalAmbiguousAndUnsafeLinksAreNotRewrittenIntoTrustedOnes() {
        listOf(
            "https://reader.invalid/blog/show/123",
            "http://wanandroid.com/blog/show/123",
            "//reader.invalid/blog/show/123",
            "//wanandroid.com/blog/show/123",
            "/blog/show/../123",
            "/blog/show/123/../../user/login",
            "/blog/show/%2e%2e",
            "/user/logout/json",
            "/other/path",
            "javascript:alert(1)",
            "https://user:password@wanandroid.com/blog/show/123",
            "/blog/show/123\n?x=1",
            "/blog/show/123?bad=%",
            "/blog/show/123?x=" + "a".repeat(8192)
        ).forEach { assertEquals(it, it, normalizeArticleLink(it)) }
    }
}
