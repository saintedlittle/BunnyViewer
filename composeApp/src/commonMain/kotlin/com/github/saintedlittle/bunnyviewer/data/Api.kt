package com.github.saintedlittle.bunnyviewer.data


import com.github.saintedlittle.bunnyviewer.PlatformEnv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


internal expect fun platformEngine(): HttpClientEngineFactory<*>


object Api {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }


    val client by lazy {
        HttpClient(platformEngine()) {
            install(ContentNegotiation) { json(json) }
            install(HttpCache)
            install(HttpRequestRetry) {
                maxRetries = 2
                retryIf { _, response -> response.status.value >= 500 }
            }
        }
    }


    suspend fun getUpdates(): List<PostDto> {
        val url = PlatformEnv.appUrl().trimEnd('/') + "/api/getUpdates"
        return client.get(url).body<UpdatesResponse>().items
    }
}