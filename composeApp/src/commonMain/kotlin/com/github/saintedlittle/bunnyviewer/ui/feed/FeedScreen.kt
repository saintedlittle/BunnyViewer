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
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalTime::class)
@Composable
private fun PostCard(post: PostDto, onShare: () -> Unit, onSaveAll: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            // Header: avatar + title + date
            Row(verticalAlignment = Alignment.CenterVertically) {
                // простая заглушка-аватар с инициалом
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            post.channel.title.firstOrNull()?.uppercase() ?: "•",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        post.channel.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        formatPrettyDate(post.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (post.text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                ExpandableText(
                    text = post.text,
                    minLines = 3,
                    maxLines = 10
                )
            }

            if (post.media.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                MediaPager(post.media)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onShare) { Text("Поделиться") }
                OutlinedButton(onClick = onSaveAll) { Text("Сохранить фото") }
                Spacer(Modifier.weight(1f))
                // мелкая мета
                Text("👁 ${post.views ?: 0}")
                Spacer(Modifier.width(4.dp))
                Text("↗ ${post.forwards ?: 0}")
            }
        }
    }
}

@Composable
private fun ExpandableText(
    text: String,
    minLines: Int,
    maxLines: Int
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = text,
            maxLines = if (expanded) maxLines else minLines,
            overflow = TextOverflow.Ellipsis
        )
        if (text.length > 140) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Скрыть" else "Показать ещё")
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
                .aspectRatio(16f / 9f) // аккуратное окно под фото/видео
        ) { page ->
            val m = media[page]
            val url = mediaUrl(m)
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (m.type.lowercase()) {
                        "photo", "image" -> {
                            val res = asyncPainterResource(url)
                            KamelImage(
                                resource = res,
                                contentDescription = null,
                                onLoading = { CircularProgressIndicator() },
                                onFailure = { Text("Не удалось загрузить изображение") },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        "video" -> {
                            // пока заглушка для видео
                            Text("Видео (${m.mime ?: ""})", style = MaterialTheme.typography.labelLarge)
                        }
                        else -> Text("Медиа: ${m.type}")
                    }
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

private fun mediaUrl(m: MediaDto): String {
    val p = m.filePath
    return if (p.startsWith("http", ignoreCase = true)) p
    else {
        val baseUrl = PlatformEnv.appUrl()
        val protocol = if (baseUrl.startsWith("http")) "" else "https://"
        Api.run { protocol + baseUrl + "/" + p.trimStart('/') }
    }
}
private fun postShareText(post: PostDto): String = buildString {
    appendLine(post.channel.title)
    if (post.text.isNotBlank()) appendLine(post.text)
    if (post.media.isNotEmpty()) {
        appendLine()
        post.media.take(3).forEach { appendLine(mediaUrl(it)) }
    }
}
