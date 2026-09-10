package com.personal.wanandroid.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.personal.wanandroid.core.designsystem.WanSpacing
import com.personal.wanandroid.core.ui.FeaturePlaceholder

@Composable
fun HomeScreen(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        OutlinedButton(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)
        ) { Text(stringResource(R.string.search_hint)) }
        FeaturePlaceholder(
            stringResource(R.string.daily_question),
            stringResource(R.string.question_pending)
        )
        FeaturePlaceholder(
            stringResource(R.string.latest_articles),
            stringResource(R.string.article_pending)
        )
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit) {
    Column(Modifier.safeDrawingPadding()) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        FeaturePlaceholder(
            stringResource(R.string.search_hint),
            stringResource(R.string.search_pending)
        )
    }
}
