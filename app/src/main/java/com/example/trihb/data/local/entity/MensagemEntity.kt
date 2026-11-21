package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mensagem do chat entre paciente e nutricionista (isolada por [usuarioId]).
 * [fonteIa] indica se a resposta da Dra. Camila veio da IA em nuvem ("GEMINI")
 * ou do motor especialista contextual offline ("LOCAL_ROOM").
 */
@Entity(
    tableName = "chat_mensagens",
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
data class MensagemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int = UsuarioEntity.ID_DEMO_ALAN,
    /** [REMETENTE_NUTRI] ou [REMETENTE_PACIENTE]. */
    val remetente: String,
    val texto: String,
    val horario: String,
    val fonteIa: String = FONTE_LOCAL_ROOM,
) {
    val isDaNutri: Boolean get() = remetente == REMETENTE_NUTRI

    companion object {
        const val REMETENTE_NUTRI = "NUTRI"
        const val REMETENTE_PACIENTE = "PACIENTE"
        const val FONTE_GEMINI = "GEMINI"
        const val FONTE_LOCAL_ROOM = "LOCAL_ROOM"
    }
}
