package com.example.trihb.ui.screens.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.NivelAtividade
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.repository.TriHubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.min

data class PerfilUiState(
    val carregando: Boolean = true,
    val usuario: UsuarioEntity? = null,
    val refeicoesConcluidasHoje: Int = 0,
    val totalRefeicoesHoje: Int = 0,
    val aguaConsumidaHojeMl: Int = 0,
    val totalMedicoesHistorico: Int = 0,
    val geminiApiKey: String = "",
)

class PerfilViewModel(
    private val repository: TriHubRepository,
) : ViewModel() {

    val uiState: StateFlow<PerfilUiState> = combine(
        repository.resumoUsuarioAtivo,
        repository.sessao,
    ) { resumo, sessao ->
        val u = resumo?.usuario
        val refeicoes = resumo?.refeicoes.orEmpty()
        PerfilUiState(
            carregando = false,
            usuario = u,
            refeicoesConcluidasHoje = refeicoes.count { it.concluida },
            totalRefeicoesHoje = refeicoes.size,
            aguaConsumidaHojeMl = resumo?.hidratacao?.consumidoMl ?: 0,
            totalMedicoesHistorico = resumo?.progressos?.size ?: 0,
            geminiApiKey = sessao?.geminiApiKey.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    /**
     * Copia e redimensiona a foto escolhida no Photo Picker para o diretório interno
     * privado do app (`filesDir/avatars/`), garantindo persistência 100% offline.
     */
    fun salvarFotoPerfil(context: Context, uri: Uri, aoConcluir: (Boolean) -> Unit) {
        val usuarioAtual = uiState.value.usuario ?: return
        viewModelScope.launch {
            val caminhoSalvo = withContext(Dispatchers.IO) {
                runCatching {
                    val pasta = File(context.filesDir, "avatars").apply { mkdirs() }
                    val destino = File(pasta, "avatar_user_${usuarioAtual.id}_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val original = BitmapFactory.decodeStream(input) ?: return@runCatching null
                        val reduzido = redimensionarQuadrado(original, 480)
                        FileOutputStream(destino).use { out ->
                            reduzido.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        }
                    }
                    // Remove foto anterior se existir
                    if (usuarioAtual.fotoUri.isNotBlank()) {
                        runCatching { File(usuarioAtual.fotoUri).delete() }
                    }
                    destino.absolutePath
                }.getOrNull()
            }
            if (!caminhoSalvo.isNullOrBlank()) {
                repository.atualizarPerfilUsuario(usuarioAtual.copy(fotoUri = caminhoSalvo))
                aoConcluir(true)
            } else {
                aoConcluir(false)
            }
        }
    }

    fun removerFotoPerfil() {
        val usuarioAtual = uiState.value.usuario ?: return
        viewModelScope.launch {
            if (usuarioAtual.fotoUri.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching { File(usuarioAtual.fotoUri).delete() }
                }
            }
            repository.atualizarPerfilUsuario(usuarioAtual.copy(fotoUri = ""))
        }
    }

    fun salvarAlteracoesPerfil(
        nome: String,
        bio: String,
        pesoAtualKg: Float,
        metaPesoKg: Float,
        alturaCm: Float,
        metaCaloriasKcal: Int,
        metaAguaMl: Int,
        focoPrincipal: FocoPrincipal,
        modalidades: List<String>,
        nivelAtividade: NivelAtividade,
        restricoesAlimentares: List<String>,
        geminiApiKey: String,
        aoConcluir: () -> Unit,
    ) {
        val usuarioAtual = uiState.value.usuario ?: return
        val pesoMudou = abs(usuarioAtual.pesoAtualKg - pesoAtualKg) >= 0.05f
        viewModelScope.launch {
            repository.atualizarPerfilUsuario(
                usuario = usuarioAtual.copy(
                    nome = nome.trim().ifBlank { usuarioAtual.nome },
                    bio = bio.trim(),
                    pesoAtualKg = pesoAtualKg,
                    metaPesoKg = metaPesoKg,
                    alturaCm = alturaCm,
                    metaCaloriasKcal = metaCaloriasKcal.coerceIn(1000, 6000),
                    metaAguaMl = metaAguaMl.coerceIn(1000, 8000),
                    focoPrincipal = focoPrincipal.codigo,
                    modalidades = modalidades.joinToString(","),
                    nivelAtividade = nivelAtividade.codigo,
                    restricoesAlimentares = restricoesAlimentares.joinToString(","),
                ),
                registrarNovoPesoNoHistorico = pesoMudou,
            )
            repository.salvarChaveGemini(geminiApiKey)
            aoConcluir()
        }
    }

    fun sairDaConta(aoSair: () -> Unit) {
        viewModelScope.launch {
            repository.encerrarSessao()
            aoSair()
        }
    }

    private fun redimensionarQuadrado(origem: Bitmap, tamanhoMax: Int): Bitmap {
        val lado = min(origem.width, origem.height)
        val x = (origem.width - lado) / 2
        val y = (origem.height - lado) / 2
        val recorte = Bitmap.createBitmap(origem, x, y, lado, lado)
        return if (lado > tamanhoMax) {
            Bitmap.createScaledBitmap(recorte, tamanhoMax, tamanhoMax, true)
        } else {
            recorte
        }
    }
}
