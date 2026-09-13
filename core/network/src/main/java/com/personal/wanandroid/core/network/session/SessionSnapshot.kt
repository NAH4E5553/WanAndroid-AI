package com.personal.wanandroid.core.network.session

import com.personal.wanandroid.core.network.dto.UserDto

enum class SessionPhase { LOADING, GUEST, VERIFYING, AUTHENTICATED, UNVERIFIED }
enum class SessionNotice { NONE, EXPIRED, STORAGE_ERROR }
data class SessionSnapshot(
    val phase: SessionPhase = SessionPhase.LOADING,
    val user: UserDto? = null,
    val generation: Long = 0,
    val notice: SessionNotice = SessionNotice.NONE
)
