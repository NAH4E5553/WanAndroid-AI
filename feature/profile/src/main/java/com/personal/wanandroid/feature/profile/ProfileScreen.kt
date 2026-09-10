package com.personal.wanandroid.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.personal.wanandroid.core.designsystem.WanSpacing
import com.personal.wanandroid.core.ui.FeaturePlaceholder

@Composable
fun ProfileScreen(onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        FeaturePlaceholder(
            stringResource(R.string.profile),
            stringResource(R.string.account_pending)
        )
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
            Text(stringResource(R.string.login))
        }
        FeaturePlaceholder(
            stringResource(R.string.collections),
            stringResource(R.string.collections_pending)
        )
        FeaturePlaceholder(
            stringResource(R.string.history),
            stringResource(R.string.history_pending)
        )
        FeaturePlaceholder(
            stringResource(R.string.offline),
            stringResource(R.string.offline_pending)
        )
    }
}
