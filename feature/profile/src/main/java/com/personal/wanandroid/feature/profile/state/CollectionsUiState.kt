package com.personal.wanandroid.feature.profile.state

import com.personal.wanandroid.core.common.base.state.PagedUiState
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.CollectionSnapshot
import com.personal.wanandroid.core.result.DataError

data class CollectionsUiState(
    val collections: CollectionSnapshot = CollectionSnapshot(),
    val page: PagedUiState<CollectionItem> = PagedUiState(),
    val error: DataError? = null
)
