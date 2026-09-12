package com.personal.wanandroid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.personal.wanandroid.core.navigation.AppRoute
import com.personal.wanandroid.core.navigation.Destination
import com.personal.wanandroid.core.navigation.MainRoute
import com.personal.wanandroid.core.navigation.NavigationDispatcher
import com.personal.wanandroid.core.navigation.NavigationHostToken
import com.personal.wanandroid.core.navigation.NavigationSource
import com.personal.wanandroid.core.ui.AppScaffold
import com.personal.wanandroid.feature.article.articleGraph
import com.personal.wanandroid.feature.auth.authGraph
import com.personal.wanandroid.feature.home.HomeRoute
import com.personal.wanandroid.feature.home.homeGraph
import com.personal.wanandroid.feature.profile.ProfileRoute
import com.personal.wanandroid.feature.profile.profileGraph
import com.personal.wanandroid.feature.topics.TopicsScreen

@Composable
fun AppNavigation(dispatcher: NavigationDispatcher) {
    val root = remember(dispatcher) { dispatcher.createRoot() }
    val stack = rememberNavBackStack(root)
    var host by remember(dispatcher, stack) { mutableStateOf<NavigationHostToken?>(null) }
    DisposableEffect(dispatcher, stack) {
        val token = dispatcher.attach(stack)
        host = token
        onDispose { dispatcher.detach(token) }
    }
    // Show entries only once callbacks can capture the current host identity.
    val token = host ?: return
    val top = stack.last() as AppRoute
    val backSource = NavigationSource(token, top.entryId)
    NavDisplay(
        backStack = stack,
        onBack = { dispatcher.back(backSource) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider<NavKey> {
            entry<MainRoute>(clazzContentKey = { it.entryId }) { route ->
                val source = NavigationSource(token, route.entryId)
                MainTabs(
                    onSearch = { dispatcher.navigateFrom(source, Destination.Search) },
                    onQuestionsClick = { dispatcher.navigateFrom(source, Destination.Questions) },
                    onArticleClick = { url, title, id ->
                        dispatcher.navigateFrom(source, Destination.Article(url, title, id))
                    },
                    onLogin = { dispatcher.navigateFrom(source, Destination.Login) },
                    onThemeSettings = { dispatcher.navigateFrom(source, Destination.ThemeSettings) }
                )
            }
            homeGraph(dispatcher, token)
            profileGraph(dispatcher, token)
            authGraph(dispatcher, token)
            articleGraph(dispatcher, token)
        }
    )
}

@Composable
private fun MainTabs(
    onSearch: () -> Unit,
    onQuestionsClick: () -> Unit,
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    onLogin: () -> Unit,
    onThemeSettings: () -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()
    val labels = listOf(R.string.home, R.string.topics, R.string.profile)
    AppScaffold(
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { TabIcon(index) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    ) { padding ->
        stateHolder.SaveableStateProvider(selected) {
            when (selected) {
                0 -> HomeRoute(
                    onSearch = onSearch,
                    onQuestionsClick = onQuestionsClick,
                    onArticleClick = onArticleClick,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )

                1 -> TopicsScreen(Modifier.fillMaxSize().padding(padding))

                2 -> ProfileRoute(
                    onLogin = onLogin,
                    onThemeSettings = onThemeSettings,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}
