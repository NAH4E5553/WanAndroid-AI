package com.personal.wanandroid.feature.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.personal.wanandroid.core.navigation.CollectionsRoute as CollectionsKey
import com.personal.wanandroid.core.navigation.Destination
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.navigation.ThemeSettingsRoute as RouteKey
import com.personal.wanandroid.feature.profile.view.CollectionsRoute
import com.personal.wanandroid.feature.profile.view.ThemeSettingsRoute

fun EntryProviderScope<NavKey>.profileGraph(
    dispatcher: NavigationDispatcher,
    host: NavigationHostToken
) {
    entry<com.personal.wanandroid.core.navigation.HistoryRoute>(clazzContentKey = {
        it.entryId
    }) { route ->
        val source = NavigationSource(host, route.entryId)
        com.personal.wanandroid.feature.profile.view.HistoryRoute(
            onBack = { dispatcher.back(source) },
            onArticle = { item ->
                dispatcher.navigateFrom(
                    source,
                    Destination.Article(item.url, item.title, item.articleId)
                )
            }
        )
    }
    entry<CollectionsKey>(clazzContentKey = {
        it.entryId
    }) { route ->
        val source = NavigationSource(host, route.entryId)
        CollectionsRoute(
            onBack = { dispatcher.back(source) },
            onLogin = {
                dispatcher.navigateFrom(
                    source,
                    Destination.Login
                )
            },
            onArticle = { item ->
                dispatcher.navigateFrom(
                    source,
                    Destination.Article(
                        item.article.url,
                        item.article.title,
                        item.target.articleId,
                        item.target.recordId,
                        collected = item.article.collected,
                        collectionSession = item.article.collectionSession
                    )
                )
            }
        )
    }
    entry<RouteKey>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        ThemeSettingsRoute(onBack = { dispatcher.back(source) })
    }
}
