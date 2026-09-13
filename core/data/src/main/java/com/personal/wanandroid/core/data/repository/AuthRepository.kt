package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.data.mapper.requestWithData
import com.personal.wanandroid.core.data.mapper.requestWithoutData
import com.personal.wanandroid.core.model.auth.AccountUser
import com.personal.wanandroid.core.model.auth.AuthNotice
import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.core.network.datasource.AuthNetworkDataSource
import com.personal.wanandroid.core.network.session.SessionChangedException
import com.personal.wanandroid.core.network.session.SessionNotice
import com.personal.wanandroid.core.network.session.SessionPhase
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStorageException
import com.personal.wanandroid.core.network.session.SessionStore
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** generation is captured by detach; null means cleanup failed, reported through session.notice. */
data class LogoutResult(val generation: Long?, val result: DataResult<Unit>)

interface AuthRepository {
    fun authenticatedAccountId(): Long? = null
    fun knownAccountId(): Long? = authenticatedAccountId()
    val session: Flow<AuthSession>
    suspend fun restore(): DataResult<Unit>
    suspend fun login(username: String, password: String): DataResult<Unit>
    suspend fun logout(): LogoutResult
}

internal class DefaultAuthRepository @Inject constructor(
    private val source: AuthNetworkDataSource,
    private val sessions: SessionStore
) : AuthRepository {
    override fun authenticatedAccountId(): Long? = sessions.state.value.let {
        it.user?.id.takeIf { _ -> it.phase == SessionPhase.AUTHENTICATED }
    }

    override fun knownAccountId(): Long? = sessions.state.value.user?.id

    private val restoreMutex = Mutex()
    override val session: Flow<AuthSession> = sessions.state.map { state ->
        AuthSession(
            status = when (state.phase) {
                SessionPhase.LOADING -> AuthStatus.LOADING
                SessionPhase.GUEST -> AuthStatus.GUEST
                SessionPhase.VERIFYING -> AuthStatus.VERIFYING
                SessionPhase.AUTHENTICATED -> AuthStatus.AUTHENTICATED
                SessionPhase.UNVERIFIED -> AuthStatus.UNVERIFIED
            },
            user = state.user?.let {
                AccountUser(it.id, it.username, it.nickname.orEmpty().ifBlank { it.username })
            },
            generation = state.generation,
            notice = when (state.notice) {
                SessionNotice.NONE -> AuthNotice.NONE
                SessionNotice.EXPIRED -> AuthNotice.EXPIRED
                SessionNotice.STORAGE_ERROR -> AuthNotice.STORAGE_ERROR
            }
        )
    }

    override suspend fun restore(): DataResult<Unit> = withContext(Dispatchers.IO) {
        restoreMutex.withLock {
            val state = sessions.initialize()
            if (state.phase !in setOf(SessionPhase.VERIFYING, SessionPhase.UNVERIFIED)) {
                return@withLock DataResult.Success(Unit)
            }
            val request = sessions.capture()
            try {
                val coroutineContext = currentCoroutineContext()
                val result = requestWithData({ source.userInfo(request) }) { body ->
                    coroutineContext.ensureActive()
                    if (!sessions.verified(request, body.userInfo)) throw SessionChangedException()
                }
                when (result) {
                    is DataResult.Success -> DataResult.Success(Unit)

                    is DataResult.Failure -> {
                        if (result.reason == DataError.SESSION_EXPIRED) {
                            sessions.expire(request)
                        } else {
                            sessions.verificationFailed(request)
                        }
                        result
                    }
                }
            } catch (cancelled: CancellationException) {
                sessions.verificationFailed(request)
                throw cancelled
            }
        }
    }

    override suspend fun login(username: String, password: String): DataResult<Unit> {
        var request: SessionRequest? = null
        return try {
            withContext(Dispatchers.IO) {
                if (username.isBlank() || username.length > 200 || password.isEmpty() ||
                    password.length > 200
                ) {
                    return@withContext DataResult.Failure(DataError.INVALID_RESPONSE)
                }
                try {
                    val context = sessions.beginLogin().also { request = it }
                    val response = source.login(username.trim(), password, context)
                    currentCoroutineContext().ensureActive()
                    val result = requestWithData({ response.body }) { user ->
                        if (!response.commit(user)) throw SessionChangedException()
                        Unit
                    }
                    if (result is DataResult.Failure) sessions.abortLogin(context)
                    result
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SessionStorageException) {
                    DataResult.Failure(DataError.STORAGE)
                } catch (_: SessionChangedException) {
                    request?.let(sessions::abortLogin)
                    DataResult.Failure(DataError.SESSION_CHANGED)
                } catch (_: java.io.IOException) {
                    request?.let(sessions::abortLogin)
                    DataResult.Failure(DataError.NETWORK)
                } catch (_: Exception) {
                    request?.let(sessions::abortLogin)
                    DataResult.Failure(DataError.INVALID_RESPONSE)
                }
            }
        } catch (cancelled: CancellationException) {
            // Also catches cancellation while dispatching an IO commit back to the caller.
            withContext(NonCancellable + Dispatchers.IO) { request?.let(sessions::abortLogin) }
            throw cancelled
        }
    }

    override suspend fun logout(): LogoutResult = withContext(Dispatchers.IO) {
        val detached = try {
            sessions.detach()
        } catch (_: SessionStorageException) {
            return@withContext LogoutResult(null, DataResult.Failure(DataError.STORAGE))
        }
        // No automatic retries: this request carries only the detached account's cookies.
        LogoutResult(detached.generation, requestWithoutData { source.logout(detached) })
    }
}
