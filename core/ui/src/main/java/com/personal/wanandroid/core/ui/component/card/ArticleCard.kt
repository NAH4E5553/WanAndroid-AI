package com.personal.wanandroid.core.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.personal.wanandroid.core.designsystem.theme.WanSpacing
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.ui.R

@Composable
fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = WanSpacing.page,
            vertical = WanSpacing.small
        )
    ) {
        Column(
            modifier = Modifier.padding(WanSpacing.page),
            verticalArrangement = Arrangement.spacedBy(WanSpacing.small)
        ) {
            val metadata = article.displayMetadata(stringResource(R.string.metadata_unknown))
            Text(
                text = AnnotatedString.fromHtml(article.title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (metadata.usesAuthorLabel) {
                    stringResource(R.string.article_author, metadata.byline)
                } else {
                    stringResource(R.string.article_sharer, metadata.byline)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.article_category,
                    metadata.category
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.article_time, metadata.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ArticleDisplayMetadata(
    val usesAuthorLabel: Boolean,
    val byline: String,
    val category: String,
    val publishedAt: String
)

fun Article.displayMetadata(
    unknown: String,
    categorySeparator: String = "/"
): ArticleDisplayMetadata {
    val usesAuthorLabel = author.isNotBlank()
    return ArticleDisplayMetadata(
        usesAuthorLabel = usesAuthorLabel,
        byline = if (usesAuthorLabel) author else shareUser.ifBlank { unknown },
        category = listOf(superChapterName, chapter)
            .filter(String::isNotBlank)
            .joinToString(categorySeparator)
            .ifBlank { unknown },
        publishedAt = publishedAt.ifBlank { unknown }
    )
}
