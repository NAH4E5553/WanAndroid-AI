package com.personal.wanandroid.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.navigation.ThemeSettingsRoute as RouteKey

fun EntryProviderScope<NavKey>.profileGraph(
    dispatcher: NavigationDispatcher,
    host: NavigationHostToken
) {
    entry<RouteKey>(clazzContentKey = { it.entryId }) { route ->
        val source = NavigationSource(host, route.entryId)
        ThemeSettingsRoute(onBack = { dispatcher.back(source) })
    }
}
