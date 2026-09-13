package com.personal.wanandroid.core.network.session

import com.personal.wanandroid.core.network.dto.UserDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** All memory/disk transitions share one lock. Invoke on IO, never from a Composable. */
@Singleton
class SessionStore(private val storage: SessionStorage, private val now: () -> Long) {
    @Inject constructor(storage: SessionStorage) : this(storage, System::currentTimeMillis)
    private val mutableState = MutableStateFlow(SessionSnapshot())
    val state = mutableState.asStateFlow()
    private var initialized = false
    private var cookies: List<Cookie> = emptyList()
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun initialize(): SessionSnapshot {
        if (initialized) return mutableState.value
        initialized = true
        try {
            val payload = storage.read()
            if (payload == null) {
                mutableState.value = SessionSnapshot(phase = SessionPhase.GUEST)
            } else {
                require(payload.length <= MAX_PAYLOAD)
                val saved = json.decodeFromString<StoredSession>(payload)
                require(validUser(saved.user))
                cookies = saved.cookies.mapNotNull { Cookie.parse(API, it) }
                    .filter { acceptable(it) && it.persistent && it.expiresAt > now() }
                if (cookies.isEmpty()) {
                    clearLocked(SessionNotice.EXPIRED)
                } else {
                    require(cookies.size <= MAX_COOKIES)
                    mutableState.value = SessionSnapshot(SessionPhase.VERIFYING, saved.user)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            clearLocked(SessionNotice.STORAGE_ERROR)
        }
        return mutableState.value
    }

    @Synchronized
    fun capture(): SessionRequest {
        initialize()
        if (mutableState.value.user != null && cookies.none { it.expiresAt > now() }) {
            clearLocked(SessionNotice.EXPIRED)
        }
        return SessionRequest(mutableState.value.generation)
    }

    @Synchronized
    fun beginLogin(): SessionRequest {
        initialize()
        if (!clearLocked(SessionNotice.NONE)) throw SessionStorageException()
        return SessionRequest(mutableState.value.generation, SessionRequest.Mode.LOGIN)
    }

    @Synchronized
    fun detach(): SessionRequest {
        initialize()
        val old = cookies
        if (!clearLocked(SessionNotice.NONE)) throw SessionStorageException()
        // This identity belongs to the state created by detach, not the removed account.
        return SessionRequest(mutableState.value.generation, SessionRequest.Mode.LOGOUT, old)
    }

    @Synchronized
    fun isCurrent(request: SessionRequest): Boolean =
        request.generation == mutableState.value.generation

    @Synchronized
    fun cookieHeader(request: SessionRequest, url: HttpUrl): String {
        if (!isApi(url)) return ""
        val selected = when (request.mode) {
            SessionRequest.Mode.LOGIN -> emptyList()

            SessionRequest.Mode.LOGOUT -> request.logoutCookies

            SessionRequest.Mode.NORMAL -> {
                if (!isCurrent(request)) throw SessionChangedException()
                cookies
            }
        }
        return selected.filter { acceptable(it) && it.expiresAt > now() && it.matches(url) }
            .sortedByDescending { it.path.length }.joinToString("; ") { "${it.name}=${it.value}" }
    }

    @Synchronized
    fun commitLogin(request: SessionRequest, user: UserDto, received: List<Cookie>): Boolean {
        if (!isCurrent(request)) return false
        require(validUser(user))
        val next = received.filter { acceptable(it) && it.expiresAt > now() }
            .distinctBy { Triple(it.name, it.domain, it.path) }
        require(next.isNotEmpty() && next.size <= MAX_COOKIES)
        persistLocked(user, next)
        cookies = next
        mutableState.value = SessionSnapshot(SessionPhase.AUTHENTICATED, user, request.generation)
        return true
    }

    @Synchronized
    fun verified(request: SessionRequest, user: UserDto): Boolean {
        if (!isCurrent(request) || mutableState.value.user == null) return false
        // A different account must never be attached to a restored account's cookies.
        if (!validUser(user) || user.id != mutableState.value.user?.id) {
            clearLocked(SessionNotice.EXPIRED)
            return false
        }
        persistLocked(user, cookies)
        mutableState.value = SessionSnapshot(SessionPhase.AUTHENTICATED, user, request.generation)
        return true
    }

    @Synchronized
    fun verificationFailed(request: SessionRequest) {
        if (isCurrent(request) && mutableState.value.user != null) {
            mutableState.value = mutableState.value.copy(phase = SessionPhase.UNVERIFIED)
        }
    }

    @Synchronized
    fun expire(request: SessionRequest) {
        if (isCurrent(request) &&
            mutableState.value.user != null
        ) {
            clearLocked(SessionNotice.EXPIRED)
        }
    }

    @Synchronized
    fun abortLogin(request: SessionRequest) {
        if (isCurrent(request)) clearLocked(SessionNotice.NONE)
    }

    @Synchronized
    fun updateCookies(request: SessionRequest, received: List<Cookie>) {
        if (!isCurrent(request) || mutableState.value.user == null || received.isEmpty()) return
        val next = cookies.associateBy { Triple(it.name, it.domain, it.path) }.toMutableMap()
        received.filter(::acceptable).forEach { cookie ->
            val key = Triple(cookie.name, cookie.domain, cookie.path)
            if (cookie.expiresAt <= now()) next.remove(key) else next[key] = cookie
        }
        val valid = next.values.filter { it.expiresAt > now() }
        if (valid.isEmpty()) {
            clearLocked(SessionNotice.EXPIRED)
        } else {
            if (valid.size > MAX_COOKIES) throw IOException("Too many session cookies")
            persistLocked(requireNotNull(mutableState.value.user), valid)
            cookies = valid
        }
    }

    private fun persistLocked(user: UserDto, next: List<Cookie>) {
        try {
            val persistent = next.filter { it.persistent && it.expiresAt > now() }
            // Session-only cookies stay in memory and never survive a process restart.
            val payload = if (persistent.isEmpty()) {
                null
            } else {
                json.encodeToString(
                    StoredSession(user, persistent.map(Cookie::toString))
                )
            }
            require(payload == null || payload.length <= MAX_PAYLOAD)
            storage.write(payload)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            clearLocked(SessionNotice.STORAGE_ERROR)
            throw SessionStorageException()
        }
    }

    private fun clearLocked(notice: SessionNotice): Boolean {
        cookies = emptyList()
        var success = true
        try {
            storage.write(null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            success = false
        }
        mutableState.value = SessionSnapshot(
            phase = SessionPhase.GUEST,
            generation = mutableState.value.generation + 1,
            notice = if (success) notice else SessionNotice.STORAGE_ERROR
        )
        return success
    }

    private fun acceptable(cookie: Cookie) = cookie.domain == API.host &&
        cookie.toString().length <= 4096
    private fun validUser(user: UserDto) = user.id > 0 && user.username.isNotBlank() &&
        user.username.length <= 200 && user.nickname.orEmpty().length <= 200

    @Serializable
    private class StoredSession(val user: UserDto, val cookies: List<String>)

    companion object {
        private const val MAX_PAYLOAD = 65536
        private const val MAX_COOKIES = 32
        val API = "https://wanandroid.com/".toHttpUrl()
        fun isApi(url: HttpUrl) = url.isHttps && url.host == API.host && url.port == 443
    }
}
