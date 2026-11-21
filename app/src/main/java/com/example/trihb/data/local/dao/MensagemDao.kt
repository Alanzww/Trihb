package com.example.trihb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trihb.data.local.entity.MensagemEntity
import kotlinx.coroutines.flow.Flow

/** Leitura, inserção e limpeza de mensagens do chat por [usuarioId]. */
@Dao
interface MensagemDao {

    @Insert
    suspend fun inserirMensagem(mensagem: MensagemEntity): Long

    @Query("SELECT * FROM chat_mensagens WHERE usuarioId = :usuarioId ORDER BY id ASC")
    fun observarMensagensDoUsuario(usuarioId: Int): Flow<List<MensagemEntity>>

    @Query("SELECT * FROM chat_mensagens WHERE usuarioId = :usuarioId ORDER BY id DESC LIMIT :limite")
    suspend fun buscarUltimasMensagens(usuarioId: Int, limite: Int = 8): List<MensagemEntity>

    @Update
    suspend fun atualizarMensagem(mensagem: MensagemEntity)

    @Delete
    suspend fun deletarMensagem(mensagem: MensagemEntity)
}
