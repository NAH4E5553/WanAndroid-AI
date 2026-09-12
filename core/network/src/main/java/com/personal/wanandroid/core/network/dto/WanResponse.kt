package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WanResponse<T>(val errorCode: Int, val errorMsg: String = "", val data: T? = null)
