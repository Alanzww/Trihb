package com.example.trihb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.trihb.data.local.entity.ProgressoEntity
import kotlinx.coroutines.flow.Flow

/** CRUD de medições corporais (peso, cintura, quadril) filtrado por [usuarioId]. */
@Dao
interface ProgressoDao {

    @Insert
    suspend fun inserirMedicao(medicao: ProgressoEntity): Long

    /** Ordem cronológica de inserção — usada pelo gráfico Canvas. */
    @Query("SELECT * FROM progresso_corporal WHERE usuarioId = :usuarioId ORDER BY id ASC")
    fun observarHistoricoDoUsuario(usuarioId: Int): Flow<List<ProgressoEntity>>

    @Delete
    suspend fun deletarMedicao(medicao: ProgressoEntity)
}
