package com.example.tecnoluna.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tecnoluna.model.Perfil
import kotlinx.coroutines.flow.Flow

// Operaciones sobre la tabla "usuarios"
@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY id")
    fun observarTodos(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios WHERE perfil = :perfil")
    suspend fun porPerfil(perfil: Perfil): List<Usuario>

    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) AND clave = :clave AND activo = 1 LIMIT 1")
    suspend fun login(correo: String, clave: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(lista: List<Usuario>)
}
