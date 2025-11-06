package com.github.saintedlittle.bunnyviewer

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface Platform {
    val name: String
    fun getAppUrl(): String
    fun getKeyValue(): KeyValue
    fun writeText(name: String, text: String)
    fun readText(name: String): String?
    suspend fun saveImageToGallery(url: String, filenameHint: String? = null): Boolean
    suspend fun shareText(text: String)
}

expect fun getPlatform(): Platform

// Key-Value API
interface KeyValue {
    fun <T: Any> get(key: String): T?
    fun <T: Any> put(key: String, value: T?)
    fun <T: Any> observe(key: String): Flow<T?>
}

// Composable provider
@Composable
fun ProvidePlatformDependencies(content: @Composable () -> Unit) {
    content()
}

// Global accessors
object PlatformEnv {
    private val platform: Platform by lazy { getPlatform() }

    fun appUrl(): String = platform.getAppUrl()
}

object KV : KeyValue {
    private val platform: Platform by lazy { getPlatform() }
    private val impl: KeyValue by lazy { platform.getKeyValue() }

    override fun <T : Any> get(key: String): T? = impl.get(key)
    override fun <T : Any> put(key: String, value: T?) = impl.put(key, value)
    override fun <T : Any> observe(key: String): Flow<T?> = impl.observe(key)
}

object Files {
    private val platform: Platform by lazy { getPlatform() }

    fun writeText(name: String, text: String) = platform.writeText(name, text)
    fun readText(name: String): String? = platform.readText(name)

    inline fun <reified T: Any> writeJson(name: String, obj: T) = writeText(name, serialize(obj))
    inline fun <reified T: Any> readJson(name: String): T? = readText(name)?.let { deserialize(it) }
}

// Media helpers
suspend fun saveImageToGallery(url: String, filenameHint: String? = null): Boolean =
    getPlatform().saveImageToGallery(url, filenameHint)

suspend fun shareText(text: String) =
    getPlatform().shareText(text)

// Serialization
expect inline fun <reified T: Any> serialize(obj: T): String
expect inline fun <reified T: Any> deserialize(text: String): T