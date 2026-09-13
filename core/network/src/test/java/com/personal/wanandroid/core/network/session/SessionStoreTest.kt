package com.personal.wanandroid.core.network.session

import com.personal.wanandroid.core.network.dto.UserDto
import java.io.IOException
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SessionStoreTest {
    private var time = 1_000L
    private val storage = MemorySessionStorage()
    private val store = SessionStore(storage) { time }
    private val user = UserDto(7, "fixture-user")
    private fun cookie(
        value: String = "fixture-token",
        path: String = "/",
        expires: Long = 100_000
    ) = Cookie.Builder().name("session").value(value).hostOnlyDomain("wanandroid.com")
        .path(path).secure().httpOnly().expiresAt(expires).build()
    private fun login(value: String = "fixture-token"): SessionRequest {
        val request = store.beginLogin()
        assertTrue(store.commitLogin(request, user, listOf(cookie(value))))
        return request
    }

    @Test fun responseHintVersionChangesWithSessionAndProcess() {
        assertNull(store.authenticatedVersionKey())
        login()
        val key = store.authenticatedVersionKey()
        assertNotNull(key)
        val restored = SessionStore(storage) { time }
        restored.initialize()
        assertNull(restored.authenticatedVersionKey())
        restored.verified(restored.capture(), user)
        assertFalse(key == restored.authenticatedVersionKey())
        login()
        assertFalse(key == store.authenticatedVersionKey())
    }

    @Test fun emptyStorageIsGuestAndHasNoCookie() {
        assertEquals(SessionPhase.GUEST, store.initialize().phase)
        assertEquals("", store.cookieHeader(store.capture(), SessionStore.API))
    }

    @Test fun persistentCookiesRestoreAsUnverifiedUntilServerValidation() {
        login()
        val restored = SessionStore(storage) { time }
        assertEquals(SessionPhase.VERIFYING, restored.initialize().phase)
        assertEquals(
            "session=fixture-token",
            restored.cookieHeader(restored.capture(), SessionStore.API)
        )
        assertTrue(restored.verified(restored.capture(), user))
        assertEquals(SessionPhase.AUTHENTICATED, restored.state.value.phase)
    }

    @Test fun sessionOnlyCookieIsNeverPersisted() {
        val request = store.beginLogin()
        val sessionCookie = Cookie.Builder().name(
            "session"
        ).value("fixture").hostOnlyDomain("wanandroid.com").build()
        assertTrue(store.commitLogin(request, user, listOf(sessionCookie)))
        assertNull(storage.payload)
        assertEquals(SessionPhase.GUEST, SessionStore(storage) { time }.initialize().phase)
    }

    @Test fun domainPathProtocolAndExpiryAreMatched() {
        val request = store.beginLogin()
        store.commitLogin(request, user, listOf(cookie(path = "/user")))
        val current = store.capture()
        assertEquals(
            "session=fixture-token",
            store.cookieHeader(current, "https://wanandroid.com/user/lg/userinfo/json".toHttpUrl())
        )
        for (url in listOf(
            "https://wanandroid.com/article/list/0/json",
            "http://wanandroid.com/user",
            "https://evil.example/user",
            "https://wanandroid.com.evil.example/user",
            "https://sub.wanandroid.com/user",
            "https://wanandroid.com:8443/user"
        )) {
            assertEquals("", store.cookieHeader(current, url.toHttpUrl()))
        }
        time = 100_001
        store.capture()
        assertEquals(SessionNotice.EXPIRED, store.state.value.notice)
        assertNull(storage.payload)
    }

    @Test fun hostOnlyAndPathAttributesSurvivePersistence() {
        val request = store.beginLogin()
        store.commitLogin(request, user, listOf(cookie(path = "/user")))
        val restored = SessionStore(storage) { time }
        val current = restored.capture()
        assertEquals("", restored.cookieHeader(current, SessionStore.API))
        assertEquals(
            "session=fixture-token",
            restored.cookieHeader(current, "https://wanandroid.com/user/login".toHttpUrl())
        )
    }

    @Test fun logoutDetachesOldCookiesAndLateLoginCannotRestoreThem() {
        val old = login()
        val detached = store.detach()
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertNull(storage.payload)
        assertFalse(store.commitLogin(old, user, listOf(cookie())))
        login("new-token")
        assertEquals("session=fixture-token", store.cookieHeader(detached, SessionStore.API))
        assertEquals("session=new-token", store.cookieHeader(store.capture(), SessionStore.API))
    }

    @Test fun oldExpiryAndSetCookieCannotOverwriteNewAccount() {
        val old = login()
        store.detach()
        login("new-token")
        store.expire(old)
        store.updateCookies(old, listOf(cookie(expires = 1)))
        store.abortLogin(old)
        assertEquals(SessionPhase.AUTHENTICATED, store.state.value.phase)
        assertEquals("session=new-token", store.cookieHeader(store.capture(), SessionStore.API))
    }

    @Test fun deletingCurrentLastCookieEndsSession() {
        val current = login()
        store.updateCookies(current, listOf(cookie(expires = 1)))
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertNull(storage.payload)
    }

    @Test fun restoredDifferentAccountFailsClosed() {
        login()
        assertFalse(store.verified(store.capture(), UserDto(9, "other-fixture")))
        assertEquals(SessionNotice.EXPIRED, store.state.value.notice)
    }

    @Test fun corruptPersistenceIsClearedWithoutPublishingAnAccount() {
        storage.payload = "not-json"
        assertEquals(SessionNotice.STORAGE_ERROR, store.initialize().notice)
        assertNull(store.state.value.user)
        assertNull(storage.payload)
    }

    @Test fun writeFailureDoesNotPublishLoginSuccess() {
        val request = store.beginLogin()
        storage.failWrites = true
        try {
            store.commitLogin(request, user, listOf(cookie()))
            fail("Expected storage error")
        } catch (_: SessionStorageException) { }
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertEquals(SessionNotice.STORAGE_ERROR, store.state.value.notice)
        assertEquals("", store.cookieHeader(store.capture(), SessionStore.API))
    }

    @Test fun cancelledLoginAndVerificationFailurePreserveCorrectState() {
        val request = store.beginLogin()
        store.abortLogin(request)
        assertFalse(store.commitLogin(request, user, listOf(cookie())))
        login()
        store.verificationFailed(store.capture())
        assertEquals(SessionPhase.UNVERIFIED, store.state.value.phase)
        assertNotNull(storage.payload)
    }
}

internal class MemorySessionStorage : SessionStorage {
    var payload: String? = null
    var failWrites = false
    override fun read() = payload
    override fun write(payload: String?) {
        if (failWrites) throw IOException("fixture")
        this.payload = payload
    }
}
