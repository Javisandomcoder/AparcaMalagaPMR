package com.javisandom.aparcamalagapmr.car

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.javisandom.aparcamalagapmr.data.LocationStore

/** Owned by the visible car screen; requests stop when its lifecycle stops. */
class CarLocationUpdates(private val context: Context, private val changed: () -> Unit) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var registered = false
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            LocationStore(context).save(
                com.javisandom.aparcamalagapmr.domain.UserLocation(location.latitude, location.longitude), location.time)
            changed()
        }
        override fun onProviderDisabled(provider: String) { changed() }
        override fun onProviderEnabled(provider: String) { changed() }
    }
    fun start() {
        if (registered) return
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!coarse) return
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            if (provider == LocationManager.GPS_PROVIDER && !fine) continue
            try {
                if (manager.allProviders.contains(provider)) {
                    manager.requestLocationUpdates(provider, 2_000L, 5f, listener, Looper.getMainLooper())
                    registered = true
                }
            } catch (_: SecurityException) { /* Permission can be revoked while starting. */ }
        }
    }
    fun stop() {
        manager.removeUpdates(listener)
        registered = false
    }
}
