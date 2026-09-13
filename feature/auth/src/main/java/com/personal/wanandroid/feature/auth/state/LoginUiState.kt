package com.personal.wanandroid.feature.auth.state

import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.feature.auth.policy.isMainlandMobileNumber

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: DataError? = null,
    val completed: Boolean = false,
    val phoneError: Boolean = false
) {
    val canSubmit get() = isMainlandMobileNumber(
        username
    ) && password.isNotEmpty() && !isSubmitting &&
        !completed
    override fun toString() =
        "LoginUiState(isSubmitting=$isSubmitting, error=$error, completed=$completed)"
}
