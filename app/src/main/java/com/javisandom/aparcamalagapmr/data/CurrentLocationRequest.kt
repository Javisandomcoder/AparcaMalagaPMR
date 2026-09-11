package com.javisandom.aparcamalagapmr.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.javisandom.aparcamalagapmr.domain.LocationFix
import com.javisandom.aparcamalagapmr.domain.UserLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** One cancellable request. Permissions are checked by the caller and revocations are handled. */
@SuppressLint("MissingPermission")
suspend fun requestCurrentFix(context: Context, manager: LocationManager, provider: String): LocationFix? =
    suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation { cancellation.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, cancellation, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location?.let {
                    LocationFix(UserLocation(it.latitude, it.longitude), it.time)
                })
            }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
