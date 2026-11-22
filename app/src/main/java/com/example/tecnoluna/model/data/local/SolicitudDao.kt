package com.example.tecnoluna.model.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Operaciones CRUD sobre la tabla "solicitudes"
@Dao
interface SolicitudDao {

    // ---------- READ (leer) ----------
    // Flow: avisa a las pantallas cada vez que la tabla cambia
    @Query("SELECT * FROM solicitudes ORDER BY actualizada DESC")
    fun observarTodas(): Flow<List<Solicitud>>

    @Query("SELECT * FROM solicitudes WHERE codigo = :codigo")
    fun observar(codigo: String): Flow<Solicitud?>

    @Query("SELECT * FROM solicitudes WHERE codigo = :codigo")
    suspend fun obtener(codigo: String): Solicitud?

    // Último número usado (SOL-1007 -> 1007). Si no hay solicitudes, parte en 1000.
    @Query("SELECT COALESCE(MAX(CAST(SUBSTR(codigo, 5) AS INTEGER)), 1000) FROM solicitudes")
    suspend fun ultimoNumero(): Int

    // ---------- CREATE (crear) ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(solicitud: Solicitud)

    // ---------- UPDATE (actualizar) ----------
    @Update
    suspend fun actualizar(solicitud: Solicitud)

    // ---------- DELETE (eliminar) ----------
    @Delete
    suspend fun eliminar(solicitud: Solicitud)
}
