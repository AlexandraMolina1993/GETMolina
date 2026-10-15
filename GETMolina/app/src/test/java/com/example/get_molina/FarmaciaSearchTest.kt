package com.example.get_molina

import com.example.get_molina.data.local.entity.FarmaciaEntity
import com.example.get_molina.data.local.entity.toUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class FarmaciaSearchTest {

    @Test
    fun `toUiModel mapea correctamente la entidad de Room al modelo de UI`() {
        val entidad = FarmaciaEntity(
            id = "Farmacia Central_Av Belgrano 123",
            orden = 0,
            nombre = "Farmacia Central",
            direccion = "Av Belgrano 123 – 3573-123456",
            fechaRaw = "Lunes 12 de Octubre"
        )

        val uiModel = entidad.toUiModel()

        assertEquals("Farmacia Central", uiModel.nombre)
        assertEquals("Av Belgrano 123 – 3573-123456", uiModel.direccion)
        assertEquals("Lunes 12 de Octubre", uiModel.fecha_raw)
        assertEquals("Av Belgrano 123", uiModel.getDireccionSolo())
        assertEquals("3573-123456", uiModel.getTelefonoExtraido())
    }

    @Test
    fun `filtro SQL simulado con LIKE busca en nombre y direccion`() {
        val farmacias = listOf(
            FarmaciaEntity(id = "1", orden = 0, nombre = "Farmacia San Martín", direccion = "San Martín 100", fechaRaw = "Hoy"),
            FarmaciaEntity(id = "2", orden = 1, nombre = "Farmacia Del Pueblo", direccion = "Belgrano 200", fechaRaw = "Mañana"),
            FarmaciaEntity(id = "3", orden = 2, nombre = "Farmacia Central", direccion = "San Martín 500", fechaRaw = "Ayer")
        )

        val query = "san martín"
        val resultados = farmacias.filter { entidad ->
            query.isEmpty() ||
                    entidad.nombre.contains(query, ignoreCase = true) ||
                    entidad.direccion.contains(query, ignoreCase = true)
        }

        assertEquals(2, resultados.size)
        assertEquals("Farmacia San Martín", resultados[0].nombre)
        assertEquals("Farmacia Central", resultados[1].nombre)
    }
}
