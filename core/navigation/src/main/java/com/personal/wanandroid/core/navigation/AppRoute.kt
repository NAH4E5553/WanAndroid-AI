package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute : NavKey

@Serializable
data class ArticleRoute(val url: String, val title: String, val articleId: Long? = null) : NavKey

@Serializable
data object LoginRoute : NavKey

@Serializable
data object SearchRoute : NavKey
