package com.personal.wanandroid.core.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.wanandroid.core.model.SearchHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.searchHistoryStore by preferencesDataStore(name = "search_history")
internal val searchHistoryKeys = (0 until 20).map { stringPreferencesKey("recent_$it") }
internal fun recentQueries(values: List<String>): List<String> = values.map { it.trim().take(200) }
    .filter { it.isNotEmpty() }.distinct().take(20)

internal interface SearchHistoryDataSource {
    val history: Flow<SearchHistory>
    suspend fun record(keyword: String): Boolean
    suspend fun clear(): Boolean
}
internal class PreferencesSearchHistoryDataSource(private val store: DataStore<Preferences>) :
    SearchHistoryDataSource {
    @Inject constructor(@ApplicationContext context: Context) : this(context.searchHistoryStore)
    override val history: Flow<SearchHistory> = store.data.map { values ->
        SearchHistory(recentQueries(searchHistoryKeys.mapNotNull { values[it] }), ready = true)
    }.catch { failure ->
        if (failure is CancellationException) throw failure
        emit(SearchHistory(ready = true, readFailed = true))
    }

    override suspend fun record(keyword: String): Boolean {
        val query = keyword.trim().take(200)
        if (query.isEmpty()) return true
        return update { old -> recentQueries(listOf(query) + old) }
    }
    override suspend fun clear(): Boolean = update { emptyList() }
    private suspend fun update(transform: (List<String>) -> List<String>): Boolean = try {
        store.edit { values ->
            val next = transform(searchHistoryKeys.mapNotNull { values[it] })
            searchHistoryKeys.forEach { values.remove(it) }
            next.forEachIndexed { index, value -> values[searchHistoryKeys[index]] = value }
        }
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
}
