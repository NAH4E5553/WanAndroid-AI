package com.personal.wanandroid.feature.home.viewmodel

import com.personal.wanandroid.core.common.base.viewmodel.BaseNetworkListViewModel
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DailyQuestionsViewModel @Inject constructor(repository: ArticleRepository) :
    BaseNetworkListViewModel<Article>(1, Article::id, { repository.questionPage(it) }) {
    init {
        startInitialLoad()
    }
}
