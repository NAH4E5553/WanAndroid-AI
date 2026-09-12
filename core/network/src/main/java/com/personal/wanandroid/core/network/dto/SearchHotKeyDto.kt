package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchHotKeyDto(val id: Long, val name: String, val order: Int = 0)
