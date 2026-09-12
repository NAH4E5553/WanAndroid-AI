package com.personal.wanandroid.feature.article.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.personal.wanandroid.core.navigation.ArticleRoute as RouteKey
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.feature.article.view.ArticleRoute

fun EntryProviderScope<NavKey>.articleGraph(
    dispatcher: NavigationDispatcher,
    host: NavigationHostToken
) {
    entry<RouteKey>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        ArticleRoute(article = route, onBack = { dispatcher.back(source) })
    }
}
