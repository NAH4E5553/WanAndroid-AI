package com.personal.wanandroid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.personal.wanandroid.core.navigation.ArticleRoute
import com.personal.wanandroid.core.navigation.LoginRoute
import com.personal.wanandroid.core.navigation.MainRoute
import com.personal.wanandroid.core.navigation.SearchRoute
import com.personal.wanandroid.core.navigation.popIfCurrent
import com.personal.wanandroid.feature.article.ArticleScreen
import com.personal.wanandroid.feature.auth.LoginScreen
import com.personal.wanandroid.feature.home.HomeRoute
import com.personal.wanandroid.feature.home.SearchScreen
import com.personal.wanandroid.feature.profile.ProfileScreen
import com.personal.wanandroid.feature.topics.TopicsScreen

@Composable
fun AppNavigation() {
    val stack = rememberNavBackStack(MainRoute)
    NavDisplay(
        backStack = stack,
        onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<MainRoute> {
                MainTabs(
                    onSearch = { if (stack.lastOrNull() == MainRoute) stack.add(SearchRoute) },
                    onArticleClick = { url, title, articleId ->
                        if (stack.lastOrNull() == MainRoute) {
                            stack.add(ArticleRoute(url, title, articleId))
                        }
                    },
                    onLogin = { if (stack.lastOrNull() == MainRoute) stack.add(LoginRoute) }
                )
            }
            entry<SearchRoute> { SearchScreen(onBack = { stack.popIfCurrent(SearchRoute) }) }
            entry<LoginRoute> { LoginScreen(onBack = { stack.popIfCurrent(LoginRoute) }) }
            entry<ArticleRoute> { route ->
                ArticleScreen(title = route.title, onBack = { stack.popIfCurrent(route) })
            }
        }
    )
}

@Composable
private fun MainTabs(
    onSearch: () -> Unit,
    onArticleClick: (url: String, title: String, articleId: Long) -> Unit,
    onLogin: () -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()
    val labels = listOf(R.string.home, R.string.topics, R.string.profile)
    Scaffold(
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
                    onArticleClick = onArticleClick,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )

                1 -> TopicsScreen(Modifier.fillMaxSize().padding(padding))

                2 -> ProfileScreen(onLogin, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}
