package com.personal.wanandroid.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.personal.wanandroid.core.designsystem.WanSpacing

/** Explicit skeleton placeholder; never displays fabricated backend data. */
@Composable
fun FeaturePlaceholder(title: String, description: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Column(
            modifier = Modifier.padding(WanSpacing.section),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
