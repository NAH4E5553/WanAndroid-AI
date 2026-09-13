package com.personal.wanandroid.feature.home.view

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.feature.home.viewmodel.HomeViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeRouteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun returningToAnExistingHomeKeepsItsListAndDoesNotRequestAgain() {
        val requests = AtomicInteger()
        val article =
            Article(42, "Fixture article", "https://reader.invalid/", "", "", "", "", "", false)
        val repository = object : ArticleRepository {
            override suspend fun articles(
                page: Int,
                categoryId: Long?
            ): DataResult<PageResult<Article>> {
                requests.incrementAndGet()
                return DataResult.Success(PageResult(listOf(article), null))
            }
            override suspend fun questions() = DataResult.Success(emptyList<Article>())
            override suspend fun questionPage(page: Int) =
                DataResult.Success(PageResult<Article>(emptyList(), null))
            override suspend fun topics() = DataResult.Success(emptyList<Topic>())
            override suspend fun search(page: Int, keyword: String) =
                DataResult.Success(PageResult<Article>(emptyList(), null))
        }
        lateinit var vm: HomeViewModel
        compose.runOnUiThread { vm = HomeViewModel(repository) }
        val home = mutableStateOf(true)
        val opens = mutableListOf<Boolean>()
        compose.setContent {
            WanTheme {
                if (home.value) {
                    HomeRoute(
                        onSearch = {},
                        onQuestionsClick = {},
                        onArticleClick = {
                            opens.add(it.collected)
                            home.value = false
                        },
                        viewModel = vm
                    )
                } else {
                    TextButton(onClick = { home.value = true }) { Text("Back to fixture home") }
                }
            }
        }
        compose.waitUntil(5_000) { vm.uiState.value.articles.isNotEmpty() }
        repeat(2) {
            compose.onNodeWithText("Fixture article").performClick()
            compose.onNodeWithText("Back to fixture home").performClick()
        }
        compose.runOnIdle {
            assertEquals(1, requests.get())
            assertEquals(listOf(false, false), opens)
            vm.viewModelScope.cancel()
        }
    }
}
