package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Registro de hidratação diária isolado por conta (relacionamento 1:1 com [UsuarioEntity]).
 */
@Entity(
    tableName = "hidratacao",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HidratacaoEntity(
    @PrimaryKey
    val usuarioId: Int = UsuarioEntity.ID_DEMO_ALAN,
    val consumidoMl: Int,
    val metaBaseMl: Int,
)
