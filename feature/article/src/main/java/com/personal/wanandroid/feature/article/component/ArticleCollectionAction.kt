package com.personal.wanandroid.feature.article.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.feature.article.R

@Composable
internal fun ArticleCollectionAction(
    status: CollectionStatus,
    authenticated: Boolean,
    canAdd: Boolean,
    available: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    when {
                        status.busy -> R.string.collection_busy
                        !authenticated -> R.string.collection_add
                        status.collected == null -> R.string.collection_verify
                        status.collected == true -> R.string.collection_remove
                        !canAdd && available -> R.string.collection_removed
                        else -> R.string.collection_add
                    }
                )
            )
        },
        enabled =
            available && !status.busy && !(authenticated && !canAdd && status.collected == false),
        onClick = onClick
    )
}
