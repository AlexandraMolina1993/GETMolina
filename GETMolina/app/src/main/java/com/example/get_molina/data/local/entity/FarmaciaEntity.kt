package com.example.get_molina.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.get_molina.Farmacia

/**
 * Entidad de Room que representa la tabla "farmacias" en la base de datos local SQLite.
 */
@Entity(tableName = "farmacias")
data class FarmaciaEntity(
    @PrimaryKey
    val id: String,

    /**
     * Posición u orden original devuelto por la API.
     * Permite mantener la secuencia cronológica exacta de fechas de turno al consultar Room.
     */
    val orden: Int,

    val nombre: String,

    val direccion: String,

    @ColumnInfo(name = "fecha_raw")
    val fechaRaw: String,

    @ColumnInfo(name = "fecha_iso")
    val fechaIso: String? = null,

    @ColumnInfo(name = "dia_semana")
    val diaSemana: String? = null,

    val telefono: String? = null,

    val imagen: String? = null,

    val fechaActualizacion: Long = System.currentTimeMillis()
)

// ============================================================================
// FUNCIONES DE MAPEO (MAPPERS)
// ============================================================================

/**
 * Mapea un DTO de Retrofit ([Farmacia]) a la entidad de Room ([FarmaciaEntity]).
 *
 * @param orden Índice de posición en la lista original de la API.
 * @param timestamp Momento de sincronización en milisegundos.
 */
fun Farmacia.toEntity(orden: Int, timestamp: Long = System.currentTimeMillis()): FarmaciaEntity {
    return FarmaciaEntity(
        id = "${this.nombre}_${this.direccion}",
        orden = orden,
        nombre = this.nombre,
        direccion = this.direccion,
        fechaRaw = this.fecha_raw,
        fechaIso = this.fecha_iso,
        diaSemana = this.dia_semana,
        telefono = this.telefono,
        imagen = this.imagen,
        fechaActualizacion = timestamp
    )
}

/**
 * Mapea una entidad de Room ([FarmaciaEntity]) al modelo de UI ([Farmacia]).
 */
fun FarmaciaEntity.toUiModel(): Farmacia {
    return Farmacia(
        nombre = this.nombre,
        direccion = this.direccion,
        fecha_raw = this.fechaRaw,
        fecha_iso = this.fechaIso,
        dia_semana = this.diaSemana,
        telefono = this.telefono,
        imagen = this.imagen
    )
}
