package com.github.saintedlittle.bunnyviewer.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter

@Composable
actual fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { CircularProgressIndicator() },
        error = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ошибка загрузки")
                Text(url, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}