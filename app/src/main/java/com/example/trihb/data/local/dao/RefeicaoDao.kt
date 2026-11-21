package com.example.trihb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.trihb.data.local.entity.HidratacaoEntity
import com.example.trihb.data.local.entity.RefeicaoEntity
import kotlinx.coroutines.flow.Flow

/** CRUD de refeições + controle da hidratação diária filtrados por [usuarioId]. */
@Dao
interface RefeicaoDao {

    // CREATE
    @Insert
    suspend fun inserirRefeicao(refeicao: RefeicaoEntity): Long

    @Insert
    suspend fun inserirRefeicoes(refeicoes: List<RefeicaoEntity>)

    // READ (reativo por conta)
    @Query("SELECT * FROM refeicoes WHERE usuarioId = :usuarioId ORDER BY horario ASC, id ASC")
    fun observarRefeicoesDoUsuario(usuarioId: Int): Flow<List<RefeicaoEntity>>

    // READ (pontual)
    @Query("SELECT * FROM refeicoes WHERE id = :id LIMIT 1")
    suspend fun buscarRefeicaoPorId(id: Int): RefeicaoEntity?

    // UPDATE (checkbox da lista e edição na tela de detalhes)
    @Update
    suspend fun atualizarRefeicao(refeicao: RefeicaoEntity)

    // DELETE
    @Delete
    suspend fun deletarRefeicao(refeicao: RefeicaoEntity)

    // Hidratação por conta --------------------------------------------------
    @Query("SELECT * FROM hidratacao WHERE usuarioId = :usuarioId LIMIT 1")
    fun observarHidratacaoDoUsuario(usuarioId: Int): Flow<HidratacaoEntity?>

    @Upsert
    suspend fun atualizarHidratacao(hidratacao: HidratacaoEntity)
}
