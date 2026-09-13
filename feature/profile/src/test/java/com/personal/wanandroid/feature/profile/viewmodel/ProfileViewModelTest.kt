package com.personal.wanandroid.feature.profile.viewmodel

import com.personal.wanandroid.core.data.model.ThemePreferences
import com.personal.wanandroid.core.data.model.ThemePreferencesState
import com.personal.wanandroid.core.data.model.ThemeUpdateResult
import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.data.repository.ThemePreferencesRepository
import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun after() {
        Dispatchers.resetMain()
    }
    private class Auth : AuthRepository {
        override val session =
            MutableStateFlow(AuthSession(AuthStatus.AUTHENTICATED, generation = 3))
        val result = CompletableDeferred<DataResult<Unit>>()
        var calls = 0
        override suspend fun logout(): DataResult<Unit> {
            calls++
            session.value = AuthSession(AuthStatus.GUEST, generation = 4)
            return result.await()
        }
        override suspend fun login(username: String, password: String) = DataResult.Success(Unit)
        override suspend fun restore() = DataResult.Success(Unit)
    }
    private class Themes : ThemePreferencesRepository {
        override val state = MutableStateFlow(ThemePreferencesState())
        override suspend fun update(preferences: ThemePreferences) = ThemeUpdateResult.SUCCESS
    }

    @Test fun duplicateLogoutIsIgnoredAndNetworkFailureStaysVisibleForThatSession() =
        runTest(dispatcher) {
            val auth = Auth()
            val vm = ProfileViewModel(Themes(), auth)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.accountState.collect {}
            }
            runCurrent()
            vm.logout()
            vm.logout()
            runCurrent()
            assertEquals(1, auth.calls)
            auth.result.complete(DataResult.Failure(DataError.NETWORK))
            runCurrent()
            assertEquals(DataError.NETWORK, vm.accountState.value.error)
            assertFalse(vm.accountState.value.busy)
        }

    @Test fun oldLogoutErrorIsNotShownOnNewSession() = runTest(dispatcher) {
        val auth = Auth()
        val vm = ProfileViewModel(Themes(), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.accountState.collect {}
        }
        runCurrent()
        vm.logout()
        runCurrent()
        auth.session.value = AuthSession(AuthStatus.AUTHENTICATED, generation = 5)
        auth.result.complete(DataResult.Failure(DataError.NETWORK))
        runCurrent()
        assertNull(vm.accountState.value.error)
    }
}
