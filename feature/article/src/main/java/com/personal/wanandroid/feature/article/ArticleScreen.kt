package com.personal.wanandroid.feature.article

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.personal.wanandroid.core.ui.FeaturePlaceholder

@Composable
fun ArticleScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.safeDrawingPadding()) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        FeaturePlaceholder(title, stringResource(R.string.pending))
    }
}
