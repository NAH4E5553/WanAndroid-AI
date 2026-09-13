package com.personal.wanandroid.feature.article.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class ArticleCollectionViewModel @Inject constructor(
    private val repository: CollectionRepository
) : ViewModel() {
    private val errors = MutableStateFlow<Pair<Long, DataError>?>(null)
    val collections = repository.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.current()
    )
    val error = combine(repository.state, errors) { snapshot, failure ->
        failure?.second.takeIf {
            failure?.first ==
                snapshot.generation
        }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun load(target: CollectionTarget, generation: Long) {
        val snapshot = repository.current()
        if (snapshot.generation != generation ||
            snapshot.status(target).collected != null
        ) {
            return
        }
        viewModelScope.launch {
            report(generation, repository.reconcile(generation, target))
        }
    }
    fun toggle(target: CollectionTarget, generation: Long, collected: Boolean?) {
        if (repository.current().generation != generation) return
        errors.value = null
        viewModelScope.launch {
            val result = if (collected ==
                null
            ) {
                repository.reconcile(generation, target)
            } else {
                repository.setCollected(generation, target, !collected)
            }
            report(generation, result)
        }
    }
    private fun report(generation: Long, result: DataResult<Unit>) {
        if (repository.current().generation ==
            generation
        ) {
            errors.value =
                (result as? DataResult.Failure)?.let { generation to it.reason }
        }
    }
}
