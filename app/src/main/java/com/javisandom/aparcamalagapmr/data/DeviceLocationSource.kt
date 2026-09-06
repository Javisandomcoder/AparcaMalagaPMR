package com.javisandom.aparcamalagapmr.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.javisandom.aparcamalagapmr.domain.LocationFix
import com.javisandom.aparcamalagapmr.domain.UserLocation
import com.javisandom.aparcamalagapmr.domain.selectFreshLocation

/** Shared by phone and car; never requests permission or starts background tracking. */
class DeviceLocationSource(private val context: Context) {
    private val store = LocationStore(context)

    fun latest(): UserLocation? = latestFix()?.location

    fun latestFix(): LocationFix? {
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (!LocationManagerCompat.isLocationEnabled(manager)) return null
        val providers = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) add(LocationManager.GPS_PROVIDER)
        }
        val candidates = providers.mapNotNull { provider ->
            try {
                if (!LocationManagerCompat.hasProvider(manager, provider) || !manager.isProviderEnabled(provider)) {
                    null
                } else {
                    manager.getLastKnownLocation(provider)?.let {
                        LocationFix(UserLocation(it.latitude, it.longitude), it.time)
                    }
                }
            } catch (_: SecurityException) {
                null
            }
        } + listOfNotNull(store.loadFix())
        return selectFreshLocation(
            candidates,
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
            System.currentTimeMillis(),
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
