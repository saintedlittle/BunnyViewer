// Api.kt
package com.github.saintedlittle.bunnyviewer.data

import com.github.saintedlittle.bunnyviewer.KV
import com.github.saintedlittle.bunnyviewer.PlatformEnv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal expect fun platformEngine(): HttpClientEngineFactory<*>

private const val KEY_APP_URL_OVERRIDE = "app_url_override"

object Api {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true         // потерпим хвостовые запятые и т.п.
        explicitNulls = false
    }

    private fun baseUrl(): String {
        val override = KV.get<String>(KEY_APP_URL_OVERRIDE)?.trim().orEmpty()
        val root = override.ifEmpty { PlatformEnv.appUrl() }
        return root.trimEnd('/')
    }

    val client by lazy {
        HttpClient(platformEngine()) {
            install(ContentNegotiation) { json(json) }
            install(HttpCache)
            install(HttpRequestRetry) {
                maxRetries = 2
                retryIf { _, response -> response.status.value >= 500 }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        }
    }

    suspend fun getUpdates(): List<PostDto> {
        val url = baseUrl() + "/api/getUpdates"
        val resp = client.get(url)
        if (!resp.status.isSuccess()) error("HTTP ${resp.status.value}")

        // читаем сырой json и пытаемся распарсить 2 способами
        val text = resp.bodyAsText()

        // 1) чистый массив: [ {...}, {...} ]
        runCatching { return json.decodeFromString<List<PostDto>>(text) }

        // 2) объект-обёртка: { "items": [ ... ] }
        runCatching { return json.decodeFromString<UpdatesResponse>(text).items }

        // Если ни один не зашёл — отдадим нормальную ошибку
        throw SerializationException("Unexpected JSON shape (array or {items:[...]}) expected")
    }

    // Публичные утилиты для зеркала:
    fun getMirror(): String = KV.get<String>(KEY_APP_URL_OVERRIDE)?.trim().orEmpty()
    fun setMirror(url: String?) {
        val v = url?.trim().orEmpty()
        KV.put(KEY_APP_URL_OVERRIDE, if (v.isBlank()) null else v)
    }
}
