package com.personal.wanandroid.core.ui.component.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.ui.R

// Source roles: CoolMallKotlin PageLoading / EmptyNetwork / EmptyData / LoadMore.
// WanAndroid layouts and strings are retained; no Toast, protocol, or request ownership.
@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(WanSpacing.section),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.medium)
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun MessageCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(WanSpacing.section)
        )
    }
}

@Composable
fun LoadMoreContent(
    isLoading: Boolean,
    paused: Boolean,
    onContinue: () -> Unit,
    error: DataError?,
    canLoadMore: Boolean,
    onRetry: () -> Unit,
    endMessage: String,
    endTextAlign: TextAlign = TextAlign.Start
) {
    when {
        isLoading -> LoadingContent()

        error != null -> ErrorContent(message = errorMessage(error), onRetry = onRetry)

        paused -> Button(onClick = onContinue, modifier = Modifier.padding(WanSpacing.page)) {
            Text(stringResource(R.string.continue_loading))
        }

        !canLoadMore -> Text(
            text = endMessage,
            textAlign = endTextAlign,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(WanSpacing.page)
        )
    }
}

@Composable
fun errorMessage(error: DataError): String = stringResource(
    when (error) {
        DataError.NETWORK -> R.string.error_network
        DataError.SERVICE -> R.string.error_service
        DataError.SESSION_EXPIRED -> R.string.error_session_expired
        DataError.INVALID_RESPONSE -> R.string.error_invalid_response
    }
)
