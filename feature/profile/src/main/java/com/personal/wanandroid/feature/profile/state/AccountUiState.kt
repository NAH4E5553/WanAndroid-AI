package com.personal.wanandroid.feature.profile.state

import com.personal.wanandroid.core.model.auth.AuthSession
import com.personal.wanandroid.core.result.DataError

data class AccountUiState(
    val session: AuthSession = AuthSession(),
    val busy: Boolean = false,
    val error: DataError? = null
)
