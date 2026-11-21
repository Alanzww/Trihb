package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Refeição planejada no cronograma diário do usuário (relacionamento 1:N com [UsuarioEntity]).
 * Possui índice composto (usuarioId, horario) para ordenação instantânea por conta.
 */
@Entity(
    tableName = "refeicoes",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["usuarioId", "horario"])],
)
data class RefeicaoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int = UsuarioEntity.ID_DEMO_ALAN,
    val horario: String,
    val titulo: String,
    val descricao: String,
    val calorias: Int,
    val concluida: Boolean = false,
    val observacao: String = "",
)
