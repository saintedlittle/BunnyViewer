package com.github.saintedlittle.bunnyviewer.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.saintedlittle.bunnyviewer.PlatformEnv
import com.github.saintedlittle.bunnyviewer.saveImageToGallery
import com.github.saintedlittle.bunnyviewer.shareText
import com.github.saintedlittle.bunnyviewer.data.Api
import com.github.saintedlittle.bunnyviewer.data.LocalCache
import com.github.saintedlittle.bunnyviewer.data.MediaDto
import com.github.saintedlittle.bunnyviewer.data.PostDto
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch

// FeedScreen.kt
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(onOpenPost: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    var posts by remember { mutableStateOf(LocalCache.readPosts()) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showMirrorDialog by remember { mutableStateOf(false) }

    fun load(force: Boolean = false) {
        scope.launch {
            if (refreshing) return@launch
            refreshing = true
            error = null
            try {
                val loaded = Api.getUpdates()
                posts = loaded
                LocalCache.savePosts(loaded)
            } catch (t: Throwable) {
                // не падаем — показываем ошибку
                if (posts.isEmpty()) {
                    error = "Не удалось загрузить данные: ${t.message ?: "ошибка сети"}"
                } else {
                    // есть кэш — не блокируем UI, просто покажем баннер/текст
                    error = "Обновить не удалось: ${t.message ?: "ошибка сети"}"
                }
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лента") },
                actions = {
                    IconButton(onClick = { load(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            // Ошибка (если была)
            if (error != null) {
                AssistChip(
                    onClick = { showMirrorDialog = true },
                    label = { Text(error!!) }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { load(force = true) }) { Text("Повторить") }
                    OutlinedButton(onClick = { showMirrorDialog = true }) { Text("Задать зеркало") }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Контент
            when {
                posts.isEmpty() && refreshing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                posts.isEmpty() && !refreshing -> {
                    // Пусто и ошибка — показываем дружелюбный пустой экран
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Данных нет")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { load(force = true) }) { Text("Обновить") }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showMirrorDialog = true }) { Text("Ввести зеркало") }
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post,
                                onShare = { scope.launch { shareText(postShareText(post)) } },
                                onSaveAll = {
                                    scope.launch {
                                        post.media.forEachIndexed { idx, m ->
                                            val url = mediaUrl(m)
                                            saveImageToGallery(url, "post_${post.id}_${idx}.jpg")
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }

        if (showMirrorDialog) {
            MirrorDialog(
                current = Api.getMirror().ifBlank { PlatformEnv.appUrl() },
                onDismiss = { showMirrorDialog = false },
                onApply = { newUrl ->
                    Api.setMirror(newUrl)
                    showMirrorDialog = false
                    // после смены зеркала — пробуем заново
                    posts = emptyList()
                    load(force = true)
                }
            )
        }
    }
}

@Composable
private fun MirrorDialog(
    current: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зеркало API") },
        text = {
            Column {
                Text("Укажи базовый URL сервера (например, https://example.com):")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(text) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun PostCard(post: PostDto, onShare: () -> Unit, onSaveAll: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(post.channel.title, style = MaterialTheme.typography.titleMedium)
            if (post.text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(post.text, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (post.media.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                MediaPager(post.media)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onShare, label = { Text("Поделиться") })
                AssistChip(onClick = onSaveAll, label = { Text("Сохранить фото") })
                Spacer(Modifier.weight(1f))
                Text("👁 ${post.views ?: 0}")
                Text("↗ ${post.forwards ?: 0}")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPager(media: List<MediaDto>) {
    val pager = rememberPagerState(pageCount = { media.size })
    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
        ) { page ->
            val m = media[page]
            val url = mediaUrl(m)
            val painterRes = asyncPainterResource(url)
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KamelImage(
                        resource = painterRes,
                        contentDescription = null,
                        onLoading = { CircularProgressIndicator() },
                        onFailure = { Text("Ошибка загрузки") }
                    )
                }
            }
        }
        if (media.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(media.size) { i ->
                    val active = pager.currentPage == i
                    Box(
                        Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

private fun mediaUrl(m: MediaDto): String =
    if (m.filePath.startsWith("http"))
        m.filePath
    else
        PlatformEnv.appUrl().trimEnd('/') + "/" + m.filePath.trimStart('/')

private fun postShareText(post: PostDto): String = buildString {
    appendLine(post.channel.title)
    if (post.text.isNotBlank()) appendLine(post.text)
    if (post.media.isNotEmpty()) {
        appendLine()
        post.media.take(3).forEach { appendLine(mediaUrl(it)) }
    }
}
