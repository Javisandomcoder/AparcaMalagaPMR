package com.javisandom.aparcamalagapmr.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.javisandom.aparcamalagapmr.domain.ResolvedAddress
import com.javisandom.aparcamalagapmr.domain.UserLocation
import kotlinx.coroutines.*
import java.util.Locale
import java.util.concurrent.*
import kotlin.coroutines.resume

/** Uses the device geocoding service; an address result is never a parking-space record. */
class AddressResolver(context: Context) {
    private val geocoder = Geocoder(context.applicationContext, Locale.forLanguageTag("es-ES"))
    private val cache = object : LinkedHashMap<String, ResolvedAddress>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ResolvedAddress>?): Boolean = size > 32
    }

    suspend fun resolve(query: String): ResolvedAddress? = withContext(Dispatchers.IO) { resolveOffMain(query) }

    private suspend fun resolveOffMain(query: String): ResolvedAddress? {
        val key = query.trim().lowercase(Locale.ROOT)
        synchronized(cache) { cache[key] }?.let { return it }
        if (!Geocoder.isPresent()) return null
        val found = withTimeoutOrNull(6_000) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
                fun complete(addresses: List<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }
                val requested = "$query, Málaga, España"
                if (Build.VERSION.SDK_INT >= 33) {
                    try {
                        geocoder.getFromLocationName(requested, 5, 36.60, -4.65, 36.95, -4.20,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) = complete(addresses)
                                override fun onError(errorMessage: String?) = complete(emptyList())
                            })
                    } catch (_: Exception) { complete(emptyList()) }
                } else {
                    // Legacy Geocoder blocks. Bound its worker and queue; do not block cancellation or UI.
                    val task = FutureTask<Unit> {
                        @Suppress("DEPRECATION")
                        val addresses = try { geocoder.getFromLocationName(requested, 5, 36.60, -4.65, 36.95, -4.20).orEmpty() }
                            catch (_: Exception) { emptyList() }
                        complete(addresses)
                    }
                    continuation.invokeOnCancellation { task.cancel(true); legacyWorker.remove(task) }
                    try { legacyWorker.execute(task) } catch (_: RejectedExecutionException) { complete(emptyList()) }
                }
            }
        }.orEmpty().firstOrNull { address ->
            address.hasLatitude() && address.hasLongitude() &&
                address.latitude in 36.60..36.95 && address.longitude in -4.65..-4.20 &&
                (address.countryCode == null || address.countryCode.equals("ES", true)) &&
                (!address.thoroughfare.isNullOrBlank() ||
                    (!address.featureName.isNullOrBlank() && address.featureName !in listOf(address.locality, address.adminArea, address.countryName)))
        } ?: return null
        currentCoroutineContext().ensureActive()
        return ResolvedAddress(found.getAddressLine(0) ?: query, UserLocation(found.latitude, found.longitude))
            .also { synchronized(cache) { cache[key] = it } }
    }

    companion object {
        private val legacyWorker = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(1), { runnable -> Thread(runnable, "address-geocoder").apply { isDaemon = true } })
            .apply { allowCoreThreadTimeOut(true) }
    }
}
