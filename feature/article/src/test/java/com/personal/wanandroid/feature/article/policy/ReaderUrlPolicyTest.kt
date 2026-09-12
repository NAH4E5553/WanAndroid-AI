package com.personal.wanandroid.feature.article.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderUrlPolicyTest {
    @Test fun normalizesHttpsWithoutChangingEncodedPathOrQuery() {
        assertEquals(
            "https://reader.invalid/a%2Fb?q=x%20y#part",
            ReaderUrlPolicy.inAppUrl(" HTTPS://READER.INVALID:443/a%2Fb?q=x%20y#part ")
        )
        assertEquals("https://reader.invalid/", ReaderUrlPolicy.inAppUrl("https://reader.invalid"))
    }

    @Test fun rejectsUnsafeMalformedAndCredentialUrls() {
        listOf(
            "",
            "https://",
            "https://user:secret@reader.invalid/a",
            "https://reader.invalid:444/a",
            "https://reader.invalid/a\nb",
            "javascript:alert(1)",
            "file:///etc/passwd",
            "content://reader/a",
            "intent://reader/#Intent;end",
            "data:text/html,test",
            "unknown://host",
            "https://reader.invalid/" + "a".repeat(8192)
        ).forEach {
            assertEquals(it, ReaderDestination.Blocked, ReaderUrlPolicy.classify(it))
        }
    }

    @Test fun externalSchemesRequireExplicitDispatchAndRejectEncodedNewlines() {
        listOf(
            "http://reader.invalid/a",
            "tel:123456",
            "mailto:test@example.invalid",
            "geo:0,0"
        ).forEach {
            assertTrue(ReaderUrlPolicy.classify(it) is ReaderDestination.External)
            assertNull(ReaderUrlPolicy.inAppUrl(it))
        }
        assertNull(
            ReaderUrlPolicy.externalUrl("mailto:test@example.invalid?subject=%0D%0Ainjection")
        )
        assertNull(ReaderUrlPolicy.externalUrl("tel:"))
    }

    @Test fun matchesFragmentsButNotDifferentPagesOrUnsafeUrls() {
        assertTrue(
            ReaderUrlPolicy.samePage(
                "https://reader.invalid/a#one",
                "https://READER.invalid:443/a#two"
            )
        )
        assertFalse(
            ReaderUrlPolicy.samePage(
                "https://reader.invalid/a",
                "https://reader.invalid/b"
            )
        )
        assertFalse(
            ReaderUrlPolicy.samePage(
                "file:///a",
                "file:///a"
            )
        )
    }
}
