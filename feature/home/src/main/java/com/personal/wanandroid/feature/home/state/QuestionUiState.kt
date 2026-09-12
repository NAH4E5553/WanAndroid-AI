package com.personal.wanandroid.feature.home.state

import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.result.DataError

data class QuestionUiState(
    val items: List<Article> = emptyList(),
    val loading: Boolean = true,
    val error: DataError? = null
)
