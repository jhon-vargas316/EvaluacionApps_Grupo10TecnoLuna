package com.example.tecnoluna.model.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Punto de entrada de Room: reúne las dos tablas (usuarios y solicitudes) y entrega los DAOs.
// Los datos se guardan en un archivo del celular y siguen ahí al cerrar la app.
@Database(
    entities = [Usuario::class, Solicitud::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun solicitudDao(): SolicitudDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        // Siempre se usa la misma base de datos en toda la app.
        // La versión 2 reemplaza las 7 tablas anteriores: si ya había una base antigua en el celular, se recrea.
        fun getDatabase(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "tecnoluna_database"
                ).fallbackToDestructiveMigration().build().also { instancia = it }
            }
    }
}
