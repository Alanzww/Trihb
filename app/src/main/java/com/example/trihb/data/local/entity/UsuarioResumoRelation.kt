package com.example.trihb.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Projeção relacional (@Transaction) que une o usuário às suas refeições,
 * hidratação e histórico de progresso corporal em uma única leitura atômica do Room.
 */
data class UsuarioResumoRelation(
    @Embedded
    val usuario: UsuarioEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "usuarioId",
    )
    val refeicoes: List<RefeicaoEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "usuarioId",
    )
    val hidratacao: HidratacaoEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "usuarioId",
    )
    val progressos: List<ProgressoEntity>,
)
