package com.example.trihb.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.local.entity.PostFeedEntity
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.repository.TriHubRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tags rápidas de conquista oferecidas no diálogo de nova postagem. */
enum class TagConquista(val rotulo: String) {
    AGUA("💧 Meta de Água"),
    REFEICAO("🥗 Refeição no Plano"),
    TREINO("🏃 Treino Concluído"),
}

data class FeedUiState(
    val carregando: Boolean = true,
    val usuarioAtivoId: Int = UsuarioEntity.ID_DEMO_ALAN,
    val nomeUsuarioAtivo: String = "Alan",
    val focoUsuarioAtivo: String = "🔥 Emagrecimento",
    val posts: List<PostFeedEntity> = emptyList(),
)

class FeedViewModel(private val repository: TriHubRepository) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = combine(
        repository.posts,
        repository.usuarioAtivo,
    ) { posts, usuario ->
        FeedUiState(
            carregando = false,
            usuarioAtivoId = usuario?.id ?: UsuarioEntity.ID_DEMO_ALAN,
            nomeUsuarioAtivo = usuario?.nome ?: "Alan",
            focoUsuarioAtivo = usuario?.focoEnum?.rotuloComEmoji ?: "🔥 Emagrecimento",
            posts = posts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    // CREATE
    fun publicarConquista(tag: TagConquista, texto: String) {
        val estado = uiState.value
        viewModelScope.launch {
            repository.inserirPost(
                PostFeedEntity(
                    autorUsuarioId = estado.usuarioAtivoId,
                    autorNome = estado.nomeUsuarioAtivo,
                    autorFocoBadge = estado.focoUsuarioAtivo,
                    tempoAtras = "Agora",
                    badgeConquista = tag.rotulo,
                    descricao = texto.trim(),
                    incentivosCount = 0,
                    incentivadoPorMim = false,
                    isAutorUsuarioAtual = true,
                ),
            )
        }
    }

    // UPDATE — alterna o aplauso
    fun alternarIncentivo(post: PostFeedEntity) {
        val incentivar = !post.incentivadoPorMim
        viewModelScope.launch {
            repository.atualizarPost(
                post.copy(
                    incentivadoPorMim = incentivar,
                    incentivosCount = (post.incentivosCount + if (incentivar) 1 else -1).coerceAtLeast(0),
                ),
            )
        }
    }

    // DELETE (apenas posts criados pelo usuário logado atualmente)
    fun excluirPost(post: PostFeedEntity) {
        if (!post.pertenceAoUsuario(uiState.value.usuarioAtivoId)) return
        viewModelScope.launch { repository.deletarPost(post) }
    }
}
