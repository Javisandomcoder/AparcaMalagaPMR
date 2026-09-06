package com.javisandom.aparcamalagapmr.data

private const val REFRESH_INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L

fun isParkingRefreshDue(lastSuccessMillis: Long?, nowMillis: Long): Boolean =
    lastSuccessMillis == null || nowMillis - lastSuccessMillis >= REFRESH_INTERVAL_MILLIS
