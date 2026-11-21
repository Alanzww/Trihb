package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Medição corporal histórica por conta (relacionamento 1:N com [UsuarioEntity]).
 */
@Entity(
    tableName = "progresso_corporal",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["usuarioId", "id"])],
)
data class ProgressoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int = UsuarioEntity.ID_DEMO_ALAN,
    val dataRegistro: String,
    val pesoKg: Float,
    val metaPesoKg: Float,
    val cinturaCm: Float,
    val quadrilCm: Float,
)
