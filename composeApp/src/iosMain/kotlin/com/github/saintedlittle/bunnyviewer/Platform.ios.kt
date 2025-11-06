package com.github.saintedlittle.bunnyviewer

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import platform.Photos.*
import platform.posix.memcpy
import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun getAppUrl(): String {
        return "https://docs.saintedlittle.jp" // Замени на свой URL
    }

    override fun getKeyValue(): KeyValue = IOSKeyValue()

    override fun writeText(name: String, text: String) {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: return

        val filePath = "$documentsPath/$name"
        val data = text.encodeToByteArray()

        memScoped {
            val nsData = data.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = data.size.toULong()
                )
            }
            nsData.writeToFile(filePath, atomically = true)
        }
    }

    override fun readText(name: String): String? {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: return null

        val filePath = "$documentsPath/$name"
        val nsData = NSData.dataWithContentsOfFile(filePath) ?: return null

        return nsData.toByteArray().decodeToString()
    }

    override suspend fun saveImageToGallery(url: String, filenameHint: String?): Boolean =
        suspendCoroutine { continuation ->
            val status = PHPhotoLibrary.authorizationStatus()

            when (status) {
                PHAuthorizationStatusAuthorized -> {
                    saveImageImpl(url, continuation)
                }
                PHAuthorizationStatusNotDetermined -> {
                    PHPhotoLibrary.requestAuthorization { newStatus ->
                        if (newStatus == PHAuthorizationStatusAuthorized) {
                            saveImageImpl(url, continuation)
                        } else {
                            continuation.resume(false)
                        }
                    }
                }
                else -> {
                    continuation.resume(false)
                }
            }
        }

    private fun saveImageImpl(url: String, continuation: kotlin.coroutines.Continuation<Boolean>) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            continuation.resume(false)
            return
        }

        val data = NSData.dataWithContentsOfURL(nsUrl)
        if (data == null) {
            continuation.resume(false)
            return
        }

        val image = UIImage.imageWithData(data)
        if (image == null) {
            continuation.resume(false)
            return
        }

        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        continuation.resume(true)
    }

    override suspend fun shareText(text: String): Unit = withContext(Dispatchers.Main) {
        val activityViewController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }

    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(this.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
            }
        }
    }
}

class IOSKeyValue : KeyValue {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val flows = mutableMapOf<String, MutableStateFlow<String?>>()

    // Всегда читаем как String (или null)
    override fun <T : Any> get(key: String): T? {
        val s: String? = defaults.stringForKey(key)
        @Suppress("UNCHECKED_CAST")
        return s as T?
    }

    // Всегда пишем только String; НИКАКОЙ serialize(value) здесь!
    override fun <T : Any> put(key: String, value: T?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
            flows[key]?.value = null
        } else {
            val stored = value as? String
                ?: value.toString() // на случай, если кто-то всё же передаст не-String
            defaults.setObject(stored, forKey = key)
            flows.getOrPut(key) { MutableStateFlow(null) }.value = stored
        }
        defaults.synchronize()
    }

    // Поток тоже строковый
    override fun <T : Any> observe(key: String): Flow<T?> {
        val flow = flows.getOrPut(key) {
            MutableStateFlow(defaults.stringForKey(key))
        }
        @Suppress("UNCHECKED_CAST")
        return flow as Flow<T?>
    }
}

val _json = Json { ignoreUnknownKeys = true; isLenient = true }

actual inline fun <reified T: Any> serialize(obj: T): String = _json.encodeToString(obj)
actual inline fun <reified T: Any> deserialize(text: String): T = _json.decodeFromString(text)

actual fun getPlatform(): Platform = IOSPlatform()