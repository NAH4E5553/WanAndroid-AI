package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionDto(
    val id: Long,
    val originId: Long,
    val title: String,
    val link: String,
    val author: String? = null,
    val chapterName: String? = null,
    val niceDate: String? = null
)
