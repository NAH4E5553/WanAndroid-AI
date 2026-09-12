package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TopicDto(val id: Long, val name: String, val children: List<TopicDto> = emptyList())
