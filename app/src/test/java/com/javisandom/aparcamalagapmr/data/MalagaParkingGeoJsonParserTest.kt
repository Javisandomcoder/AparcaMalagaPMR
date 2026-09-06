package com.javisandom.aparcamalagapmr.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MalagaParkingGeoJsonParserTest {
    @Test
    fun `parses an official EPSG 4326 feature`() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [{
                "type": "Feature",
                "id": "official-feature-id",
                "geometry": {"type": "Point", "coordinates": [-4.38156389, 36.72615397, 0]},
                "properties": {
                  "ID": 77785,
                  "DIRECCION": "PASEO CERRADO DE CALDERÓN, 18 ",
                  "DESCRIPCION": "Paseo Cerrado de Calderón, nº 18",
                  "NROPLAZAS": "2"
                }
              }]
            }
        """.trimIndent()

        val spots = MalagaParkingGeoJsonParser().parse(json)

        assertEquals(1, spots.size)
        assertEquals("77785", spots.single().id)
        assertEquals("PASEO CERRADO DE CALDERÓN, 18", spots.single().address)
        assertEquals("Paseo Cerrado de Calderón, nº 18", spots.single().description)
        assertEquals(2, spots.single().spaceCount)
        assertEquals(36.72615397, spots.single().latitude, 0.00000001)
        assertEquals(-4.38156389, spots.single().longitude, 0.00000001)
    }

    @Test
    fun `discards coordinates outside Malaga`() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "geometry": {"type": "Point", "coordinates": [-4.4524995, -4.4524995, 0]},
                  "properties": {"ID": 66296, "DIRECCION": "CALLE HUNGRÍA, 52B", "NROPLAZAS": "1"}
                },
                {
                  "geometry": {"type": "Point", "coordinates": [-4.4217, 36.7213, 0]},
                  "properties": {"ID": 1, "DIRECCION": "MÁLAGA", "NROPLAZAS": "1"}
                }
              ]
            }
        """.trimIndent()

        val spots = MalagaParkingGeoJsonParser().parse(json)

        assertEquals(listOf("1"), spots.map { it.id })
    }
}