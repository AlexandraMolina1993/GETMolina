package com.example.get_molina.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.get_molina.data.local.dao.FarmaciaDao
import com.example.get_molina.data.local.entity.FarmaciaEntity

/**
 * Clase principal de la base de datos de Room.
 */
@Database(
    entities = [FarmaciaEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Expone el DAO de farmacias.
     */
    abstract fun farmaciaDao(): FarmaciaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Retorna la instancia singleton de [AppDatabase].
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "get_molina_db"
                )
                    /*
                     * NOTA DIDÁCTICA: fallbackToDestructiveMigration(dropAllTables = true)
                     * recrea la base de datos al cambiar de versión (versión 2 con columna orden).
                     */
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
