package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro único (id = 1) que controla qual conta está autenticada no dispositivo
 * e armazena configurações globais como a chave opcional da API Gemini.
 */
@Entity(
    tableName = "sessao_ativa",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioAtivoId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["usuarioAtivoId"])],
)
data class SessaoEntity(
    @PrimaryKey
    val id: Int = ID_SESSAO_UNICA,
    val usuarioAtivoId: Int? = null,
    val manterConectado: Boolean = true,
    val geminiApiKey: String = "",
    val ultimoLoginEm: Long = System.currentTimeMillis(),
) {
    companion object {
        const val ID_SESSAO_UNICA = 1
    }
}
