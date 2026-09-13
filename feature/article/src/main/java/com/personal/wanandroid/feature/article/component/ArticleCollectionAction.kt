package com.personal.wanandroid.feature.article.component

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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
    val label = when {
        status.busy -> R.string.collection_busy
        !authenticated -> R.string.collection_add
        status.collected == true -> R.string.collection_remove
        !canAdd && available -> R.string.collection_removed
        else -> R.string.collection_add
    }
    val text = stringResource(label)
    val trace = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    LaunchedEffect(status, authenticated, available, text, trace) {
        if (trace) {
            Log.d(
                "CollectionTrace",
                "reader.menu authenticated=$authenticated inputCollect=${status.collected} " +
                    "busy=${status.busy} available=$available label=$text"
            )
        }
    }
    DropdownMenuItem(
        text = { Text(text) },
        enabled =
            available && !status.busy && !(authenticated && !canAdd && status.collected == false),
        onClick = onClick
    )
}
