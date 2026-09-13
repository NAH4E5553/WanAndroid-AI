package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.network.datasource.AuthNetworkDataSource
import com.personal.wanandroid.core.network.datasource.LoginResponse
import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.dto.UserInfoDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionPhase
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStorage
import com.personal.wanandroid.core.network.session.SessionStore
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import okhttp3.Cookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    private class Memory : SessionStorage {
        var payload: String? = null
        override fun read() = payload
        override fun write(payload: String?) {
            this.payload = payload
        }
    }
    private val memory = Memory()
    private val store = SessionStore(memory) { 1_000 }
    private val user = UserDto(7, "fixture-user")
    private val cookie = Cookie.Builder().name("session").value("fixture-token")
        .hostOnlyDomain("wanandroid.com").path("/").expiresAt(100_000).build()
    private val source = FakeSource()
    private val repository = DefaultAuthRepository(source, store)

    private inner class FakeSource : AuthNetworkDataSource {
        val starts = Channel<Unit>(Channel.UNLIMITED)
        var loginBody = WanResponse(0, data = user)
        var loginGate: CompletableDeferred<Unit>? = null
        var logoutGate: CompletableDeferred<Unit>? = null
        var verifyError: Exception? = null
        var verifyBody = WanResponse(0, data = UserInfoDto(user))
        var logoutBody: WanResponse<JsonElement> = WanResponse(0)
        var logoutError: Exception? = null
        var capturedUsername = ""
        var afterCommit: () -> Unit = {}
        override suspend fun login(
            username: String,
            password: String,
            session: SessionRequest
        ): LoginResponse {
            capturedUsername = username
            starts.send(Unit)
            loginGate?.await()
            return LoginResponse(loginBody) {
                store.commitLogin(session, it, listOf(cookie)).also { afterCommit() }
            }
        }
        override suspend fun userInfo(session: SessionRequest): WanResponse<UserInfoDto> {
            verifyError?.let { throw it }
            return verifyBody
        }
        override suspend fun logout(session: SessionRequest): WanResponse<JsonElement> {
            starts.send(Unit)
            logoutGate?.await()
            logoutError?.let { throw it }
            return logoutBody
        }
    }
    private suspend fun signIn() {
        assertEquals(
            DataResult.Success(Unit),
            repository.login(" fixture-user ", "fixture-password")
        )
    }

    @Test fun successCommitsValidatedAccountAndTrimsUsername() = runTest {
        signIn()
        assertEquals("fixture-user", source.capturedUsername)
        assertEquals(SessionPhase.AUTHENTICATED, store.state.value.phase)
        assertNotNull(memory.payload)
    }

    @Test fun rejectedAndEmptyResponsesCannotCommitCookies() = runTest {
        source.loginBody = WanResponse(-1, data = user)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            repository.login("fixture", "fixture-password")
        )
        assertNull(memory.payload)
        source.loginBody = WanResponse(0)
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            repository.login("fixture", "fixture-password")
        )
        assertNull(store.state.value.user)
    }

    @Test fun cancellingLoginClearsAttemptAndRethrowsCancellation() = runTest {
        source.loginGate = CompletableDeferred()
        val call = async { repository.login("fixture", "fixture-password") }
        source.starts.receive()
        call.cancel()
        call.join()
        assertTrue(call.isCancelled)
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertNull(memory.payload)
    }

    @Test fun logoutClearsLocallyBeforeNetworkAndStaysClearedOnFailure() = runTest {
        signIn()
        source.starts.receive()
        source.logoutGate = CompletableDeferred()
        source.logoutError = IOException("fixture")
        val logout = async { repository.logout() }
        source.starts.receive()
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertNull(memory.payload)
        val detachedGeneration = store.state.value.generation
        source.logoutGate!!.complete(Unit)
        val result = logout.await()
        assertEquals(detachedGeneration, result.generation)
        assertEquals(DataResult.Failure(DataError.NETWORK), result.result)
    }

    @Test fun noBodyLogoutSuccessIsAcceptedButBusinessFailureIsNot() = runTest {
        signIn()
        assertEquals(DataResult.Success(Unit), repository.logout().result)
        source.logoutBody = WanResponse(-1)
        assertEquals(DataResult.Failure(DataError.SERVICE), repository.logout().result)
    }

    @Test fun offlineRestoreRetainsCredentialsWithoutClaimingVerifiedLogin() = runTest {
        signIn()
        source.verifyError = IOException("fixture")
        val restored = SessionStore(memory) { 1_000 }
        val result = DefaultAuthRepository(source, restored).restore()
        assertEquals(DataResult.Failure(DataError.NETWORK), result)
        assertEquals(SessionPhase.UNVERIFIED, restored.state.value.phase)
        assertNotNull(memory.payload)
    }

    @Test fun expiredRestoreClearsStoredSession() = runTest {
        signIn()
        source.verifyBody = WanResponse(-1001)
        val restored = SessionStore(memory) { 1_000 }
        assertEquals(
            DataResult.Failure(DataError.SESSION_EXPIRED),
            DefaultAuthRepository(source, restored).restore()
        )
        assertEquals(SessionPhase.GUEST, restored.state.value.phase)
        assertNull(memory.payload)
    }

    @Test fun successfulRestoreValidatesMatchingIdentity() = runTest {
        signIn()
        val restored = SessionStore(memory) { 1_000 }
        assertEquals(DataResult.Success(Unit), DefaultAuthRepository(source, restored).restore())
        assertEquals(SessionPhase.AUTHENTICATED, restored.state.value.phase)
    }

    @Test fun loginResponseAfterLogoutCannotRestoreAccount() = runTest {
        source.loginGate = CompletableDeferred()
        val login = async { repository.login("fixture", "fixture-password") }
        source.starts.receive()
        repository.logout()
        source.loginGate!!.complete(Unit)
        assertEquals(DataResult.Failure(DataError.SESSION_CHANGED), login.await())
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
    }

    @Test fun invalidInputDoesNotStartRequest() = runTest {
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            repository.login(" ", "fixture-password")
        )
        assertTrue(source.starts.tryReceive().isFailure)
    }

    @Test fun cancelledDispatchAfterCommitRollsBackThatSession() = runTest {
        lateinit var call: Deferred<DataResult<Unit>>
        source.afterCommit = { call.cancel() }
        call =
            async(start = CoroutineStart.LAZY) { repository.login("fixture", "fixture-password") }
        call.start()
        call.join()
        assertTrue(call.isCancelled)
        assertEquals(SessionPhase.GUEST, store.state.value.phase)
        assertNull(memory.payload)
    }

    @Test fun mismatchedRestoredIdentityReportsSessionChange() = runTest {
        signIn()
        source.verifyBody = WanResponse(0, data = UserInfoDto(UserDto(8, "another-fixture")))
        val restored = SessionStore(memory) { 1_000 }
        assertEquals(
            DataResult.Failure(DataError.SESSION_CHANGED),
            DefaultAuthRepository(source, restored).restore()
        )
        assertEquals(SessionPhase.GUEST, restored.state.value.phase)
    }
}
