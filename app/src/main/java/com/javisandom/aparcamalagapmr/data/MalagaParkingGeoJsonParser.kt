package com.javisandom.aparcamalagapmr.data

import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import org.json.JSONObject

class MalagaParkingGeoJsonParser {
    private companion object {
        val MALAGA_LONGITUDE_RANGE = -5.0..-3.5
        val MALAGA_LATITUDE_RANGE = 36.0..37.0
    }

    fun parse(json: String): List<ParkingSpot> {
        val features = JSONObject(json).getJSONArray("features")
        return buildList(features.length()) {
            for (index in 0 until features.length()) {
                val feature = features.getJSONObject(index)
                val properties = feature.getJSONObject("properties")
                val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val longitude = coordinates.getDouble(0)
                val latitude = coordinates.getDouble(1)
                if (longitude !in MALAGA_LONGITUDE_RANGE || latitude !in MALAGA_LATITUDE_RANGE) {
                    continue
                }
                add(
                    ParkingSpot(
                        id = properties.get("ID").toString(),
                        address = properties.optString("DIRECCION").trim(),
                        longitude = longitude,
                        latitude = latitude,
                        description = properties.optString("DESCRIPCION").takeUnless { it == "null" }.orEmpty().trim(),
                        spaceCount = properties.optString("NROPLAZAS").toIntOrNull() ?: 1,
                    ),
                )
            }
        }
    }
}