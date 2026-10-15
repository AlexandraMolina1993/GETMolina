package com.example.get_molina.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.get_molina.data.local.entity.FarmaciaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la entidad [FarmaciaEntity].
 * Define las consultas y operaciones con la base de datos local SQLite.
 */
@Dao
interface FarmaciaDao {

    /**
     * Obtiene la lista completa de farmacias preservando el orden secuencial/cronológico original ([orden]).
     */
    @Query("SELECT * FROM farmacias ORDER BY orden ASC")
    fun obtenerTodos(): Flow<List<FarmaciaEntity>>

    /**
     * Inserta la lista de farmacias. Si existe colisión, reemplaza el registro ([OnConflictStrategy.REPLACE]).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(farmacias: List<FarmaciaEntity>)

    /**
     * Actualiza una farmacia en la base de datos local.
     */
    @Update
    suspend fun actualizar(farmacia: FarmaciaEntity)

    /**
     * Elimina una farmacia de la base de datos local.
     */
    @Delete
    suspend fun eliminar(farmacia: FarmaciaEntity)

    /**
     * Busca farmacias por nombre o dirección mediante una consulta SQL con LIKE,
     * manteniendo la ordenación cronológica ([orden]).
     * Si [query] está vacío, retorna la lista completa de farmacias.
     */
    @Query("SELECT * FROM farmacias WHERE :query = '' OR nombre LIKE '%' || :query || '%' OR direccion LIKE '%' || :query || '%' ORDER BY orden ASC")
    fun buscarPorNombre(query: String): Flow<List<FarmaciaEntity>>

    /**
     * Cuenta la cantidad de farmacias en la base de datos local.
     */
    @Query("SELECT COUNT(*) FROM farmacias")
    suspend fun contar(): Int

    /**
     * Borra todas las farmacias registradas en la base local.
     */
    @Query("DELETE FROM farmacias")
    suspend fun borrarTodos()
}
