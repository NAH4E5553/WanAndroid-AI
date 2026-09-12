package com.personal.wanandroid.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.personal.wanandroid.core.navigation.DailyQuestionsRoute as QuestionsKey
import com.personal.wanandroid.core.navigation.Destination
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.navigation.SearchRoute

fun EntryProviderScope<NavKey>.homeGraph(
    dispatcher: NavigationDispatcher,
    host: NavigationHostToken
) {
    entry<SearchRoute>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        SearchScreen(onBack = { dispatcher.back(source) })
    }
    entry<QuestionsKey>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        DailyQuestionsRoute(
            onBack = { dispatcher.back(source) },
            onArticleClick = { url, title, id ->
                dispatcher.navigateFrom(source, Destination.Article(url, title, id))
            }
        )
    }
}
