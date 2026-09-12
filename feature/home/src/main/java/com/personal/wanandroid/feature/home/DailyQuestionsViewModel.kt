package com.personal.wanandroid.feature.home

import com.personal.wanandroid.core.common.BaseNetworkListViewModel
import com.personal.wanandroid.core.common.PagedUiState
import com.personal.wanandroid.core.data.ArticleRepository
import com.personal.wanandroid.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

typealias DailyQuestionsUiState = PagedUiState<Article>

@HiltViewModel
class DailyQuestionsViewModel @Inject constructor(repository: ArticleRepository) :
    BaseNetworkListViewModel<Article>(1, Article::id, { repository.questionPage(it) }) {
    init {
        startInitialLoad()
    }
}
