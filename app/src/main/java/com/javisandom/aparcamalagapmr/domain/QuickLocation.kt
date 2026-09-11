package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

suspend fun firstFreshLocation(
    cached: LocationFix?,
    providers: List<suspend () -> LocationFix?>,
    nowMillis: () -> Long = System::currentTimeMillis,
    timeoutMillis: Long = 8_000,
): LocationFix? {
    selectFreshLocation(listOfNotNull(cached), true, nowMillis())?.let { return it }
    if (providers.isEmpty()) return null
    return withTimeoutOrNull(timeoutMillis) {
        coroutineScope {
            val results = Channel<LocationFix?>(providers.size)
            val jobs = providers.map { provider -> launch {
                val fix = try { provider() } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { null }
                results.send(fix)
            } }
            try {
                var result: LocationFix? = null
                repeat(providers.size) {
                    val candidate = results.receive()
                    result = selectFreshLocation(listOfNotNull(candidate), true, nowMillis())
                    if (result != null) return@coroutineScope result
                }
                result
            } finally {
                jobs.forEach { it.cancel() }
                results.close()
            }
        }
    }
}
