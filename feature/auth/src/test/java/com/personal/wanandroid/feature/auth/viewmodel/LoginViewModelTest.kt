package com.personal.wanandroid.feature.auth.viewmodel

import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun after() {
        Dispatchers.resetMain()
    }
    private class Fake : AuthRepository {
        override val session = MutableStateFlow(AuthSession(status = AuthStatus.GUEST))
        var calls = 0
        val result = CompletableDeferred<DataResult<Unit>>()
        override suspend fun login(username: String, password: String): DataResult<Unit> {
            calls++
            return result.await()
        }
        override suspend fun logout() = DataResult.Success(Unit)
        override suspend fun restore() = DataResult.Success(Unit)
    }
    private fun fill(vm: LoginViewModel) {
        vm.usernameChanged(" 13800000000 ")
        vm.passwordChanged("fixture-password")
    }

    @Test fun blankInputAndDoubleSubmitDoNotSendDuplicateRequests() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        vm.submit()
        runCurrent()
        assertEquals(0, fake.calls)
        fill(vm)
        vm.submit()
        vm.submit()
        runCurrent()
        assertEquals(1, fake.calls)
        fake.result.complete(DataResult.Success(Unit))
        runCurrent()
        assertTrue(vm.uiState.value.completed)
        assertEquals("", vm.uiState.value.password)
    }

    @Test fun failureStaysOnFormAndAllowsRetry() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        fill(vm)
        vm.submit()
        runCurrent()
        fake.result.complete(DataResult.Failure(DataError.SERVICE))
        runCurrent()
        assertFalse(vm.uiState.value.completed)
        assertTrue(vm.uiState.value.canSubmit)
        assertEquals(DataError.SERVICE, vm.uiState.value.error)
    }

    @Test fun backCancelsSubmissionAndClearsPassword() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        fill(vm)
        vm.submit()
        runCurrent()
        vm.cancel()
        runCurrent()
        fake.result.complete(DataResult.Success(Unit))
        runCurrent()
        assertFalse(vm.uiState.value.completed)
        assertEquals("", vm.uiState.value.password)
    }

    @Test fun restoredVerifiedSessionCompletesButUnverifiedSessionDoesNot() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        fake.session.value = AuthSession(status = AuthStatus.UNVERIFIED)
        runCurrent()
        assertFalse(vm.uiState.value.completed)
        fake.session.value = AuthSession(status = AuthStatus.AUTHENTICATED)
        runCurrent()
        assertTrue(vm.uiState.value.completed)
    }

    @Test fun ownSessionEmissionDoesNotNavigateBeforeRepositoryFinishes() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        fill(vm)
        vm.submit()
        runCurrent()
        fake.session.value = AuthSession(status = AuthStatus.AUTHENTICATED)
        runCurrent()
        assertFalse(vm.uiState.value.completed)
        fake.result.complete(DataResult.Success(Unit))
        runCurrent()
        assertTrue(vm.uiState.value.completed)
    }

    @Test fun credentialsAreBoundedAndAbsentFromStateString() = runTest(dispatcher) {
        val vm = LoginViewModel(Fake())
        vm.usernameChanged("u".repeat(201))
        vm.passwordChanged("p".repeat(201))
        assertEquals(200, vm.uiState.value.username.length)
        assertEquals(200, vm.uiState.value.password.length)
        assertFalse(vm.uiState.value.toString().contains("pppp"))
    }

    @Test fun passwordFocusValidatesPhoneAndEditingClearsMessage() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        assertFalse(vm.uiState.value.phoneError)
        vm.passwordFocused()
        assertTrue(vm.uiState.value.phoneError)
        vm.usernameChanged("123")
        assertFalse(vm.uiState.value.phoneError)
        vm.passwordFocused()
        assertTrue(vm.uiState.value.phoneError)
        vm.usernameChanged("13800000000")
        vm.passwordFocused()
        assertFalse(vm.uiState.value.phoneError)
        runCurrent()
        assertEquals(0, fake.calls)
    }

    @Test fun invalidPhoneCannotSubmitEvenWithPassword() = runTest(dispatcher) {
        val fake = Fake()
        val vm = LoginViewModel(fake)
        vm.usernameChanged("12800000000")
        vm.passwordChanged("fixture-password")
        assertFalse(vm.uiState.value.canSubmit)
        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.phoneError)
        assertEquals(0, fake.calls)
        vm.usernameChanged("13800000000")
        assertTrue(vm.uiState.value.canSubmit)
        vm.submit()
        runCurrent()
        assertEquals(1, fake.calls)
        vm.cancel()
    }
}
