package com.javisandom.aparcamalagapmr.data

import android.content.Context
import com.javisandom.aparcamalagapmr.domain.ParkingSpot

class AssetParkingRepository(
    private val context: Context,
    private val parser: MalagaParkingGeoJsonParser = MalagaParkingGeoJsonParser(),
) {
    fun load(): List<ParkingSpot> = context.assets
        .open(ASSET_NAME)
        .bufferedReader()
        .use { parser.parse(it.readText()) }

    companion object {
        const val ASSET_NAME = "aparcamientos_pmr_malaga_4326.geojson"
        const val SOURCE_NAME = "Ayuntamiento de Málaga — Datos abiertos"
        const val SOURCE_URL =
            "https://datosabiertos.malaga.eu/dataset/aparcamientos-movilidad-reducida"
        const val SNAPSHOT_DATE = "30/08/2026"
        const val SOURCE_LAST_MODIFIED = "12/05/2025"
    }
}
