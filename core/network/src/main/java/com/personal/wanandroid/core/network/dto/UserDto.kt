package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

// Deliberately omit server password/token fields from the decoded user model.
@Serializable
data class UserDto(val id: Long, val username: String, val nickname: String? = null)
