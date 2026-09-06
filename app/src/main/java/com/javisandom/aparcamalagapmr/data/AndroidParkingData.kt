package com.javisandom.aparcamalagapmr.data

import android.content.Context
import androidx.core.content.edit
import com.javisandom.aparcamalagapmr.data.local.ParkingDatabase
import com.javisandom.aparcamalagapmr.data.local.RoomParkingStorage
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val REMOTE_GEOJSON_URL =
    "https://datosabiertos.malaga.eu/recursos/transporte/trafico/da_aparcamientosMovilidadReducida-4326.geojson"

class AssetParkingJsonSource(private val context: Context) : ParkingJsonSource {
    override suspend fun read(): String = context.assets
        .open(AssetParkingRepository.ASSET_NAME)
        .bufferedReader()
        .use { it.readText() }
}

class HttpParkingJsonSource(
    private val url: String = REMOTE_GEOJSON_URL,
) : ParkingJsonSource {
    override suspend fun read(): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/geo+json, application/json")
            connection.setRequestProperty("User-Agent", "AparcaMalagaPMR/0.1")
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("El Ayuntamiento respondió con HTTP $responseCode")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

class SharedPreferencesParkingRefreshState(context: Context) : ParkingRefreshState {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun lastSuccessMillis(): Long? = preferences
        .getLong(KEY_LAST_SUCCESS, NO_SUCCESS)
        .takeUnless { it == NO_SUCCESS }

    override fun markSuccess(millis: Long) {
        preferences.edit { putLong(KEY_LAST_SUCCESS, millis) }
    }

    private companion object {
        const val PREFERENCES_NAME = "parking_refresh"
        const val KEY_LAST_SUCCESS = "last_success_millis"
        const val NO_SUCCESS = -1L
    }
}

fun createParkingRepository(context: Context): ParkingRepository {
    val applicationContext = context.applicationContext
    return ParkingRepository(
        storage = RoomParkingStorage(
            ParkingDatabase.getInstance(applicationContext).parkingSpotDao(),
        ),
        bundledSource = AssetParkingJsonSource(applicationContext),
        remoteSource = HttpParkingJsonSource(),
        refreshState = SharedPreferencesParkingRefreshState(applicationContext),
    )
}