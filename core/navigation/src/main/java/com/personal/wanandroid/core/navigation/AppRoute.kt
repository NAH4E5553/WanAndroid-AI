package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A logical destination has no entry identity. Only Dispatcher creates stack entries. */
sealed interface Destination {
    data object Search : Destination
    data object Questions : Destination
    data object Login : Destination
    data object ThemeSettings : Destination
    data class Article(val url: String, val title: String, val articleId: Long?) : Destination
}

@Serializable
sealed interface AppRoute : NavKey {
    val entryId: String
}

@Serializable
data class MainRoute(override val entryId: String) : AppRoute

@Serializable
data class ArticleRoute(
    val url: String,
    val title: String,
    val articleId: Long?,
    override val entryId: String
) : AppRoute

@Serializable
data class LoginRoute(override val entryId: String) : AppRoute

@Serializable
data class SearchRoute(override val entryId: String) : AppRoute

@Serializable
data class ThemeSettingsRoute(override val entryId: String) : AppRoute

@Serializable
data class DailyQuestionsRoute(override val entryId: String) : AppRoute
