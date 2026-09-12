package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WanPageDto<T>(val datas: List<T>, val curPage: Int, val over: Boolean, val total: Int)
