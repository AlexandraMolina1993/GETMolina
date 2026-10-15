package com.example.get_molina.data.repository

import androidx.room.withTransaction
import com.example.get_molina.ApiService
import com.example.get_molina.Farmacia
import com.example.get_molina.data.local.AppDatabase
import com.example.get_molina.data.local.dao.FarmaciaDao
import com.example.get_molina.data.local.entity.FarmaciaEntity
import com.example.get_molina.data.local.entity.toEntity
import com.example.get_molina.data.local.entity.toUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio de Farmacias que implementa el patrón "Single Source of Truth" (Única Fuente de Verdad).
 *
 * La UI observa únicamente el [Flow] de Room, preservando el orden cronológico original de las fechas.
 */
class FarmaciaRepository(
    private val dao: FarmaciaDao,
    private val apiService: ApiService,
    private val database: AppDatabase
) {

    /**
     * Flow continuo de la lista de farmacias ordenadas cronológicamente.
     */
    val farmacias: Flow<List<Farmacia>> = dao.obtenerTodos().map { listaEntidades ->
        listaEntidades.map { entidad -> entidad.toUiModel() }
    }

    /**
     * Busca farmacias por nombre o dirección ejecutando la consulta con LIKE en Room
     * y manteniendo el orden posicional original.
     */
    fun buscarPorNombre(query: String): Flow<List<Farmacia>> {
        val queryLimpia = query.trim()
        return dao.buscarPorNombre(queryLimpia).map { listaEntidades ->
            listaEntidades.map { entidad -> entidad.toUiModel() }
        }
    }

    /**
     * Refresca los datos locales intentando sincronizarlos desde la API remota de Retrofit.
     * Preserva el orden posicional recibido del backend mediante mapIndexed.
     */
    suspend fun refrescar(forzar: Boolean = false): Result<Unit> {
        return try {
            val respuesta = apiService.getFarmacias()

            if (respuesta.isSuccessful) {
                val farmaciasDto = respuesta.body()?.farmacias ?: emptyList()
                val timestampActual = System.currentTimeMillis()

                // Mapeamos guardando el índice 'index' como 'orden' para preservar las fechas
                val entidades = farmaciasDto.mapIndexed { index, farmacia ->
                    farmacia.toEntity(orden = index, timestamp = timestampActual)
                }

                // Transacción atómica en Room: borrar registros e insertar con el nuevo orden
                database.withTransaction {
                    dao.borrarTodos()
                    dao.insertarTodos(entidades)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error en la respuesta del servidor: ${respuesta.code()}"))
            }
        } catch (e: Exception) {
            // Ante falta de conexión o fallo de red, se atrapa la excepción sin alterar Room.
            Result.failure(e)
        }
    }
}
