package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Postagem de conquista na comunidade estilo Strava (aba Feed).
 * O feed é compartilhado entre as contas locais do dispositivo; [autorUsuarioId]
 * identifica o dono da publicação para habilitar exclusão apenas pelo próprio autor.
 */
@Entity(
    tableName = "feed_posts",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["autorUsuarioId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["autorUsuarioId"])],
)
data class PostFeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val autorUsuarioId: Int? = null,
    val autorNome: String,
    val autorFocoBadge: String = "",
    val tempoAtras: String,
    val badgeConquista: String,
    val descricao: String,
    val incentivosCount: Int,
    val incentivadoPorMim: Boolean = false,
    val isAutorUsuarioAtual: Boolean = false,
) {
    fun pertenceAoUsuario(usuarioAtivoId: Int): Boolean =
        if (autorUsuarioId != null) autorUsuarioId == usuarioAtivoId else isAutorUsuarioAtual
}
