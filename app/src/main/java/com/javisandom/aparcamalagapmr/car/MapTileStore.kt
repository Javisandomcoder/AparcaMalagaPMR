package com.javisandom.aparcamalagapmr.car

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Only visible tiles. Seven day disk cache; no area downloads or prefetching. */
class MapTileStore(context: Context, private val scope: CoroutineScope, private val changed: () -> Unit) {
    private val directory = File(context.cacheDir, "car-map-tiles").apply { mkdirs() }
    private val memory = LruCache<String, Bitmap>(80)
    private val pending = mutableSetOf<String>()
    private val retryAt = mutableMapOf<String, Long>()
    private val semaphore = Semaphore(3)
    fun tile(zoom: Int, x: Int, y: Int): Bitmap? {
        if (x !in 0 until (1 shl zoom) || y !in 0 until (1 shl zoom)) return null
        val key = "$zoom-$x-$y"
        memory.get(key)?.let { return it }
        if (key in pending || System.currentTimeMillis() < (retryAt[key] ?: 0L)) return null
        pending.add(key)
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { semaphore.withPermit { read(key, zoom, x, y) } }
                if (bitmap != null) memory.put(key, bitmap)
                else retryAt[key] = System.currentTimeMillis() + 60_000
                changed()
            } finally { pending.remove(key) }
        }
        return null
    }
    private fun read(key: String, zoom: Int, x: Int, y: Int): Bitmap? {
        val file = File(directory, "$key.png")
        val cached = BitmapFactory.decodeFile(file.path)
        if (cached != null && System.currentTimeMillis() - file.lastModified() < 7 * 86_400_000L) return cached
        val connection = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "AparcaMalagaPMR/0.3 (Android; personal POI app)")
            if (file.exists()) connection.ifModifiedSince = file.lastModified()
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                file.setLastModified(System.currentTimeMillis())
                cached
            } else if (connection.responseCode == 200) {
                val bytes = connection.inputStream.use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return cached
                val temporary = File(directory, "$key.tmp")
                temporary.writeBytes(bytes)
                temporary.renameTo(file)
                // Evict only expired tiles; never defeat the seven-day cache policy.
                directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 7 * 86_400_000L }
                    ?.forEach { it.delete() }
                bitmap
            } else cached
        } catch (_: java.io.IOException) { cached }
        finally { connection.disconnect() }
    }
}
