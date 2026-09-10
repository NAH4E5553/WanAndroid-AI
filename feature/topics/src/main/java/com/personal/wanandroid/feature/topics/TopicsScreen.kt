package com.personal.wanandroid.feature.topics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.personal.wanandroid.core.ui.FeaturePlaceholder

@Composable
fun TopicsScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(stringResource(R.string.topics), stringResource(R.string.pending), modifier)
}
