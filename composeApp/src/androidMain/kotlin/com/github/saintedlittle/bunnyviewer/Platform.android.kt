package com.github.saintedlittle.bunnyviewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

// === Глобальная инициализация Android-контекста ===
internal lateinit var appContext: Context
    private set

internal fun initAndroid(context: Context) {
    if (!::appContext.isInitialized) {
        appContext = context.applicationContext
    }
}

// === Платформенная реализация ===
class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun getAppUrl(): String {
        return "https://docs.saintedlittle.jp" // TODO: замените на реальный сервер
    }

    override fun getKeyValue(): KeyValue = AndroidKeyValue(context)

    override fun writeText(name: String, text: String) {
        val file = File(context.filesDir, name)
        file.writeText(text)
    }

    override fun readText(name: String): String? {
        val file = File(context.filesDir, name)
        return if (file.exists()) file.readText() else null
    }

    override suspend fun saveImageToGallery(url: String, filenameHint: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val filename = filenameHint ?: "image_${System.currentTimeMillis()}.jpg"
                val connection = URL(url).openConnection()
                val inputStream = connection.getInputStream()

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BunnyViewer")
                    }
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override suspend fun shareText(text: String) = withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

class AndroidKeyValue(ctx: Context) : KeyValue {
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? {
        val s = prefs.getString(key, null)
        return s as T?
    }

    override fun <T : Any> put(key: String, value: T?) {
        with(prefs.edit()) {
            if (value == null) remove(key) else putString(key, value.toString())
            apply()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: String): Flow<T?> = callbackFlow {
        // стартовое значение
        trySend(prefs.getString(key, null) as T?)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, changed ->
            if (changed == key) {
                trySend(p.getString(key, null) as T?)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

// === (Де)сериализация для common expect ===
val _json = Json { ignoreUnknownKeys = true; isLenient = true }

actual inline fun <reified T : Any> serialize(obj: T): String = 
    _json.encodeToString(kotlinx.serialization.serializer(), obj)

actual inline fun <reified T : Any> deserialize(text: String): T = 
    _json.decodeFromString(kotlinx.serialization.serializer(), text)

// === Доступ к Platform через глобальный appContext ===
actual fun getPlatform(): Platform = AndroidPlatform(appContext)
