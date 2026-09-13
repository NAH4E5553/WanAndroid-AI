package com.personal.wanandroid.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.model.ThemePreferencesState
import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.data.repository.ThemePreferencesRepository
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.profile.state.AccountUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    repository: ThemePreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val themeState = repository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreferencesState()
    )

    private val operation = MutableStateFlow(AccountOperation())
    val accountState = combine(authRepository.session, operation) { session, status ->
        AccountUiState(
            session,
            status.busy,
            status.error.takeIf { status.generation == session.generation }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun logout() = runAccountOperation(accountState.value.session.generation + 1) {
        authRepository.logout()
    }
    fun retrySession() = runAccountOperation(accountState.value.session.generation) {
        authRepository.restore()
    }

    private fun runAccountOperation(generation: Long, call: suspend () -> DataResult<Unit>) {
        if (operation.value.busy) return
        operation.value = AccountOperation(busy = true)
        viewModelScope.launch {
            try {
                val result = call()
                operation.value =
                    AccountOperation(
                        generation = generation,
                        error = (result as? DataResult.Failure)?.reason
                    )
            } finally {
                operation.value = operation.value.copy(busy = false)
            }
        }
    }

    private data class AccountOperation(
        val busy: Boolean = false,
        val error: DataError? = null,
        val generation: Long? = null
    )
}
