package com.example.trihb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.trihb.data.local.entity.SessaoEntity
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.local.entity.UsuarioResumoRelation
import kotlinx.coroutines.flow.Flow

/**
 * DAO responsável por autenticação, gestão de contas locais, sessão ativa
 * e consultas relacionais (@Transaction) do perfil completo do usuário.
 */
@Dao
interface UsuarioDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirUsuario(usuario: UsuarioEntity): Long

    // READ — listagem de contas locais para login rápido em 1 toque
    @Query("SELECT * FROM usuarios ORDER BY id ASC")
    fun observarTodosUsuarios(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    fun observarUsuarioPorId(id: Int): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarUsuarioPorId(id: Int): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun buscarUsuarioPorEmail(email: String): UsuarioEntity?

    // READ — Projeção relacional completa (@Transaction)
    @Transaction
    @Query("SELECT * FROM usuarios WHERE id = :usuarioId LIMIT 1")
    fun observarResumoUsuario(usuarioId: Int): Flow<UsuarioResumoRelation?>

    @Transaction
    @Query("SELECT * FROM usuarios WHERE id = :usuarioId LIMIT 1")
    suspend fun buscarResumoUsuario(usuarioId: Int): UsuarioResumoRelation?

    // UPDATE
    @Update
    suspend fun atualizarUsuario(usuario: UsuarioEntity)

    // DELETE
    @Delete
    suspend fun deletarUsuario(usuario: UsuarioEntity)

    // Sessão ativa -----------------------------------------------------------
    @Query("SELECT * FROM sessao_ativa WHERE id = ${SessaoEntity.ID_SESSAO_UNICA} LIMIT 1")
    fun observarSessao(): Flow<SessaoEntity?>

    @Query("SELECT * FROM sessao_ativa WHERE id = ${SessaoEntity.ID_SESSAO_UNICA} LIMIT 1")
    suspend fun buscarSessao(): SessaoEntity?

    @Upsert
    suspend fun salvarSessao(sessao: SessaoEntity)
}
