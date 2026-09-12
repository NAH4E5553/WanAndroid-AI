package com.personal.wanandroid.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.personal.wanandroid.core.navigation.DailyQuestionsRoute as QuestionsKey
import com.personal.wanandroid.core.navigation.Destination
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.navigation.SearchRoute as SearchKey
import com.personal.wanandroid.feature.home.view.DailyQuestionsRoute
import com.personal.wanandroid.feature.home.view.SearchRoute

fun EntryProviderScope<NavKey>.homeGraph(
    dispatcher: NavigationDispatcher,
    host: NavigationHostToken
) {
    entry<SearchKey>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        SearchRoute(
            onBack = { dispatcher.back(source) },
            onArticleClick = { article ->
                dispatcher.navigateFrom(
                    source,
                    Destination.Article(article.url, article.title, article.id)
                )
            }
        )
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
