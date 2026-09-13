package com.personal.wanandroid.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.model.auth.AuthStatus
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.auth.policy.isMainlandMobileNumber
import com.personal.wanandroid.feature.auth.state.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0L

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                // A restored, verified account needs no second login. Own requests finish below.
                if (session.status == AuthStatus.AUTHENTICATED && !uiState.value.isSubmitting) {
                    mutableState.update { it.copy(completed = true, password = "") }
                }
            }
        }
    }

    fun usernameChanged(value: String) {
        if (!uiState.value.isSubmitting) {
            mutableState.update {
                it.copy(username = value.take(200), error = null, phoneError = false)
            }
        }
    }
    fun passwordChanged(value: String) {
        if (!uiState.value.isSubmitting) {
            mutableState.update {
                it.copy(password = value.take(200), error = null)
            }
        }
    }
    fun passwordFocused() {
        val current = uiState.value
        if (current.isSubmitting || current.completed) return
        mutableState.update { it.copy(phoneError = !isMainlandMobileNumber(it.username)) }
    }
    fun submit() {
        val current = uiState.value
        if (current.isSubmitting || current.completed) return
        if (!isMainlandMobileNumber(current.username)) {
            mutableState.update { it.copy(phoneError = true) }
            return
        }
        if (!current.canSubmit) return
        val id = ++generation
        mutableState.update { it.copy(isSubmitting = true, error = null) }
        request = viewModelScope.launch {
            try {
                val result = repository.login(current.username.trim(), current.password)
                if (id != generation) return@launch
                mutableState.update {
                    when (result) {
                        is DataResult.Success -> it.copy(
                            isSubmitting = false,
                            password = "",
                            completed = true
                        )

                        is DataResult.Failure -> it.copy(
                            isSubmitting = false,
                            error = result.reason
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                if (id ==
                    generation
                ) {
                    mutableState.update { it.copy(isSubmitting = false, password = "") }
                }
                throw cancelled
            }
        }
    }
    fun cancel() {
        generation++
        request?.cancel()
        request = null
        mutableState.update { it.copy(isSubmitting = false, password = "", completed = false) }
    }
    override fun onCleared() {
        cancel()
    }
}
