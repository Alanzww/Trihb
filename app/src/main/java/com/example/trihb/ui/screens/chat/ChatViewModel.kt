package com.example.trihb.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.ai.NutriAiService
import com.example.trihb.data.ai.NutriUserContext
import com.example.trihb.data.local.entity.MensagemEntity
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.ActivitySensorManager
import com.example.trihb.util.DataHora
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val carregando: Boolean = true,
    val nomePaciente: String = "Alan",
    val focoPaciente: String = "Emagrecimento",
    val geminiConfigurado: Boolean = false,
    val mensagens: List<MensagemEntity> = emptyList(),
    val nutriDigitando: Boolean = false,
)

class ChatViewModel(
    private val repository: TriHubRepository,
    private val sensor: ActivitySensorManager,
) : ViewModel() {

    private val nutriDigitando = MutableStateFlow(false)

    val uiState: StateFlow<ChatUiState> = combine(
        repository.mensagens,
        repository.usuarioAtivo,
        repository.sessao,
        nutriDigitando,
    ) { msgs, usuario, sessao, digitando ->
        ChatUiState(
            carregando = false,
            nomePaciente = usuario?.nome ?: "Alan",
            focoPaciente = usuario?.focoEnum?.rotuloComEmoji ?: "🔥 Emagrecimento",
            geminiConfigurado = !sessao?.geminiApiKey.isNullOrBlank(),
            mensagens = msgs,
            nutriDigitando = digitando,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    /**
     * Insere a mensagem do paciente no Room e aciona o Motor Híbrido da Dra. Camila:
     * - Se online + chave Gemini configurada: responde via Gemini 2.5 Flash com o contexto do Room.
     * - Se em Modo Avião / sem chave: responde via Motor Especialista Contextual Offline lendo
     *   as refeições, água, peso, foco, restrições e passos reais do usuário no Room.
     */
    fun enviarMensagem(texto: String) {
        val conteudo = texto.trim()
        if (conteudo.isEmpty()) return
        viewModelScope.launch {
            repository.inserirMensagem(
                MensagemEntity(
                    remetente = MensagemEntity.REMETENTE_PACIENTE,
                    texto = conteudo,
                    horario = DataHora.horaAtual(),
                ),
            )
            nutriDigitando.value = true

            val resumo = repository.buscarResumoUsuarioAtivo()
            val passos = sensor.passosDetectados.value
            val contexto = NutriUserContext.fromResumo(resumo, passos)
            val historico = repository.buscarUltimasMensagens(limite = 6)
            val chaveGemini = repository.buscarSessao()?.geminiApiKey.orEmpty()

            delay(ATRASO_MINIMO_DIGITACAO_MS)
            val resposta = NutriAiService.responder(
                pergunta = conteudo,
                contexto = contexto,
                historicoRecente = historico,
                geminiApiKey = chaveGemini,
            )

            repository.inserirMensagem(
                MensagemEntity(
                    remetente = MensagemEntity.REMETENTE_NUTRI,
                    texto = resposta.texto,
                    horario = DataHora.horaAtual(),
                    fonteIa = resposta.fonte,
                ),
            )
            nutriDigitando.value = false
        }
    }

    companion object {
        private const val ATRASO_MINIMO_DIGITACAO_MS = 850L

        /** Atalho para testes unitários diretos do motor offline. */
        internal fun gerarRespostaNutri(
            pergunta: String,
            contexto: NutriUserContext = NutriUserContext.fromResumo(null, passos = 40),
        ): String = NutriAiService.gerarRespostaContextualOffline(pergunta, contexto)
    }
}
