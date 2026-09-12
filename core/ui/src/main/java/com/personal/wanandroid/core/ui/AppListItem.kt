package com.personal.wanandroid.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.wanandroid.core.designsystem.WanSpacing

/** CoolMall AppListItem slots, stripped of icons/util dependencies and empty click handlers. */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    minHeight: Dp = 72.dp,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
    ) {
        Row(
            modifier = Modifier.padding(WanSpacing.page),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let {
                it()
                Spacer(Modifier.width(WanSpacing.medium))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
}
