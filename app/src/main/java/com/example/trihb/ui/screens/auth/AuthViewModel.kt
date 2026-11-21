package com.example.trihb.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.NivelAtividade
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.repository.TriHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val carregando: Boolean = true,
    val processando: Boolean = false,
    val contasLocais: List<UsuarioEntity> = emptyList(),
    val usuarioAtivoId: Int? = null,
    val manterConectadoSessao: Boolean = true,
    val erroMensagem: String? = null,
)

class AuthViewModel(
    private val repository: TriHubRepository,
) : ViewModel() {

    private val processando = MutableStateFlow(false)
    private val erroMensagem = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AuthUiState> = combine(
        repository.todosUsuarios,
        repository.sessao,
        processando,
        erroMensagem,
    ) { usuarios, sessao, proc, erro ->
        AuthUiState(
            carregando = false,
            processando = proc,
            contasLocais = usuarios,
            usuarioAtivoId = sessao?.usuarioAtivoId,
            manterConectadoSessao = sessao?.manterConectado ?: true,
            erroMensagem = erro,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState())

    fun limparErro() {
        erroMensagem.value = null
    }

    fun entrar(
        email: String,
        senha: String,
        manterConectado: Boolean,
        aoSucesso: () -> Unit,
    ) {
        if (email.isBlank() || senha.isBlank()) {
            erroMensagem.value = "Informe seu e-mail e sua senha."
            return
        }
        viewModelScope.launch {
            processando.value = true
            erroMensagem.value = null
            val resultado = repository.autenticar(email, senha, manterConectado)
            processando.value = false
            resultado
                .onSuccess { aoSucesso() }
                .onFailure { erroMensagem.value = it.message ?: "Falha ao autenticar." }
        }
    }

    /** Login rápido em 1 toque para contas já salvas no dispositivo. */
    fun entrarRapido(
        usuarioId: Int,
        manterConectado: Boolean = true,
        aoSucesso: () -> Unit,
    ) {
        viewModelScope.launch {
            processando.value = true
            erroMensagem.value = null
            val resultado = repository.loginRapidoPorId(usuarioId, manterConectado)
            processando.value = false
            resultado
                .onSuccess { aoSucesso() }
                .onFailure { erroMensagem.value = it.message }
        }
    }

    fun registrar(
        nome: String,
        email: String,
        senha: String,
        bio: String,
        alturaCm: Float,
        pesoAtualKg: Float,
        metaPesoKg: Float,
        focoPrincipal: FocoPrincipal,
        modalidades: List<String>,
        nivelAtividade: NivelAtividade,
        restricoesAlimentares: List<String>,
        aoSucesso: () -> Unit,
    ) {
        viewModelScope.launch {
            processando.value = true
            erroMensagem.value = null
            val resultado = repository.registrarNovoUsuario(
                nome = nome,
                email = email,
                senha = senha,
                bio = bio,
                alturaCm = alturaCm,
                pesoAtualKg = pesoAtualKg,
                metaPesoKg = metaPesoKg,
                focoPrincipal = focoPrincipal,
                modalidades = modalidades,
                nivelAtividade = nivelAtividade,
                restricoesAlimentares = restricoesAlimentares,
            )
            processando.value = false
            resultado
                .onSuccess { aoSucesso() }
                .onFailure { erroMensagem.value = it.message ?: "Não foi possível criar a conta." }
        }
    }
}
