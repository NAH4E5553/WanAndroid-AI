package com.personal.wanandroid.core.model.auth

enum class AuthStatus { LOADING, GUEST, VERIFYING, AUTHENTICATED, UNVERIFIED }
enum class AuthNotice { NONE, EXPIRED, STORAGE_ERROR }
data class AuthSession(
    val status: AuthStatus = AuthStatus.LOADING,
    val user: AccountUser? = null,
    val generation: Long = 0,
    val notice: AuthNotice = AuthNotice.NONE
)
