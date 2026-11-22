package com.example.trihb.ui.screens.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.local.entity.RefeicaoEntity
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.ActivitySensorManager
import com.example.trihb.sensor.BonusAtividade
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado imutável da aba Dieta (Usuário Ativo + Room + Sensor combinados). */
data class DietaUiState(
    val carregando: Boolean = true,
    val nomeUsuario: String = "Alan",
    val fotoUriUsuario: String = "",
    val focoUsuario: String = "🔥 Emagrecimento",
    val refeicoes: List<RefeicaoEntity> = emptyList(),
    val consumidoMl: Int = 0,
    val metaBaseAguaMl: Int = META_AGUA_PADRAO_ML,
    val caloriasConcluidas: Int = 0,
    val metaBaseCalorias: Int = META_CALORIAS_BASE,
    val passos: Int = 0,
    val sensorAtivo: Boolean = false,
    val sensorDisponivel: Boolean = true,
) {
    val bonus: BonusAtividade get() = BonusAtividade(passos)
    val metaAguaTotalMl: Int get() = metaBaseAguaMl + bonus.bonusAguaMl
    val metaCaloriasTotal: Int get() = metaBaseCalorias + bonus.bonusCalorias
    val progressoAgua: Float get() = (consumidoMl.toFloat() / metaAguaTotalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progressoCalorias: Float get() = (caloriasConcluidas.toFloat() / metaCaloriasTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val refeicoesConcluidas: Int get() = refeicoes.count { it.concluida }

    companion object {
        const val META_CALORIAS_BASE = 2100
        const val META_AGUA_PADRAO_ML = 2500
    }
}

class DietaViewModel(
    private val repository: TriHubRepository,
    private val sensor: ActivitySensorManager,
) : ViewModel() {

    val uiState: StateFlow<DietaUiState> = combine(
        repository.usuarioAtivo,
        repository.refeicoes,
        repository.hidratacao,
        sensor.passosDetectados,
        sensor.sensorAtivo,
    ) { usuario, refeicoes, hidratacao, passos, ativo ->
        DietaUiState(
            carregando = false,
            nomeUsuario = usuario?.nome ?: "Alan",
            fotoUriUsuario = usuario?.fotoUri.orEmpty(),
            focoUsuario = usuario?.focoEnum?.rotuloComEmoji ?: "🔥 Emagrecimento",
            refeicoes = refeicoes,
            consumidoMl = hidratacao?.consumidoMl ?: 0,
            metaBaseAguaMl = hidratacao?.metaBaseMl ?: usuario?.metaAguaMl ?: DietaUiState.META_AGUA_PADRAO_ML,
            caloriasConcluidas = refeicoes.filter { it.concluida }.sumOf { it.calorias },
            metaBaseCalorias = usuario?.metaCaloriasKcal ?: DietaUiState.META_CALORIAS_BASE,
            passos = passos,
            sensorAtivo = ativo,
            sensorDisponivel = sensor.sensorDisponivel,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DietaUiState(),
    )

    // UPDATE — Checkbox da lista
    fun toggleRefeicaoConcluida(refeicao: RefeicaoEntity) {
        viewModelScope.launch {
            repository.atualizarRefeicao(refeicao.copy(concluida = !refeicao.concluida))
        }
    }

    // UPDATE — Hidratação (+250 / −250 ml)
    fun ajustarAgua(deltaMl: Int) {
        val estado = uiState.value
        val novoValor = (estado.consumidoMl + deltaMl).coerceIn(0, LIMITE_AGUA_ML)
        viewModelScope.launch {
            repository.atualizarHidratacao(
                consumidoMl = novoValor,
                metaBaseMl = estado.metaBaseAguaMl,
            )
        }
    }

    // ---- Tela de Detalhe (Create / Read / Update / Delete) --------------------

    suspend fun carregarRefeicao(id: Int): RefeicaoEntity? = repository.buscarRefeicao(id)

    /** CREATE se id == -1, UPDATE se id > 0. */
    fun salvarRefeicao(
        id: Int,
        horario: String,
        titulo: String,
        descricao: String,
        calorias: Int,
        observacao: String,
        concluida: Boolean,
        aoConcluir: () -> Unit,
    ) {
        viewModelScope.launch {
            val uid = repository.obterUsuarioAtivoId()
            val refeicao = RefeicaoEntity(
                id = if (id > 0) id else 0,
                usuarioId = uid,
                horario = horario.trim(),
                titulo = titulo.trim(),
                descricao = descricao.trim(),
                calorias = calorias,
                concluida = concluida,
                observacao = observacao.trim(),
            )
            if (id > 0) repository.atualizarRefeicao(refeicao) else repository.inserirRefeicao(refeicao)
            aoConcluir()
        }
    }

    // DELETE na tela de detalhe
    fun excluirRefeicaoPorId(id: Int, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repository.buscarRefeicao(id)?.let { repository.deletarRefeicao(it) }
            aoConcluir()
        }
    }

    companion object {
        private const val LIMITE_AGUA_ML = 10_000
    }
}
