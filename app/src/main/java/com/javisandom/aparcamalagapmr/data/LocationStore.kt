package com.javisandom.aparcamalagapmr.data

import android.content.Context
import androidx.core.content.edit
import com.javisandom.aparcamalagapmr.domain.LocationFix
import com.javisandom.aparcamalagapmr.domain.UserLocation
import com.javisandom.aparcamalagapmr.domain.selectFreshLocation

class LocationStore(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(location: UserLocation, capturedAtMillis: Long = nowMillis()) {
        preferences.edit {
            putBoolean(KEY_HAS_LOCATION, true)
            putLong(KEY_LATITUDE, location.latitude.toBits())
            putLong(KEY_LONGITUDE, location.longitude.toBits())
            putLong(KEY_CAPTURED_AT, capturedAtMillis)
        }
    }

    fun load(): UserLocation? = selectFreshLocation(listOfNotNull(loadFix()), true, nowMillis())?.location

    internal fun loadFix(): LocationFix? {
        if (!preferences.getBoolean(KEY_HAS_LOCATION, false)) return null
        return LocationFix(
            location = UserLocation(
                latitude = Double.fromBits(preferences.getLong(KEY_LATITUDE, 0L)),
                longitude = Double.fromBits(preferences.getLong(KEY_LONGITUDE, 0L)),
            ),
            capturedAtMillis = preferences.getLong(KEY_CAPTURED_AT, 0L).takeIf { it > 0 },
        )
    }

    fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFERENCES_NAME = "last_user_location"
        const val KEY_HAS_LOCATION = "has_location"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_CAPTURED_AT = "captured_at_millis"
    }
}
