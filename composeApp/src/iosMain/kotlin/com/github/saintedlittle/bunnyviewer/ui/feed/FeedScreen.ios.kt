package com.github.saintedlittle.bunnyviewer.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
actual fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    KamelImage(
        resource = { asyncPainterResource(url) },
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onLoading = { CircularProgressIndicator() },
        onFailure = { error ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ошибка загрузки")
                Text(url, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}