package com.example.trihb.ui.screens.progresso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trihb.data.local.entity.ProgressoEntity
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.ActivitySensorManager
import com.example.trihb.sensor.BonusAtividade
import com.example.trihb.util.DataHora
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressoUiState(
    val carregando: Boolean = true,
    val historico: List<ProgressoEntity> = emptyList(),
    val passos: Int = 0,
    val sensorAtivo: Boolean = false,
    val sensorDisponivel: Boolean = true,
) {
    val bonus: BonusAtividade get() = BonusAtividade(passos)
    val ultimo: ProgressoEntity? get() = historico.lastOrNull()
    val pesoAtual: Float? get() = ultimo?.pesoKg
    val metaPeso: Float get() = ultimo?.metaPesoKg ?: META_PADRAO_KG
    /** Variação total: último registro − primeiro registro (negativo = perdeu peso). */
    val variacaoTotal: Float?
        get() = if (historico.size >= 2) historico.last().pesoKg - historico.first().pesoKg else null
    val faltaParaMeta: Float? get() = pesoAtual?.let { it - metaPeso }

    companion object {
        const val META_PADRAO_KG = 72.0f
    }
}

class ProgressoViewModel(
    private val repository: TriHubRepository,
    private val sensor: ActivitySensorManager,
) : ViewModel() {

    val uiState: StateFlow<ProgressoUiState> = combine(
        repository.historicoProgresso,
        sensor.passosDetectados,
        sensor.sensorAtivo,
    ) { historico, passos, ativo ->
        ProgressoUiState(
            carregando = false,
            historico = historico,
            passos = passos,
            sensorAtivo = ativo,
            sensorDisponivel = sensor.sensorDisponivel,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressoUiState())

    fun simularPassos() = sensor.simularPassos(BonusAtividade.PASSOS_POR_BLOCO)

    // CREATE
    fun registrarMedicao(pesoKg: Float, cinturaCm: Float, quadrilCm: Float) {
        val meta = uiState.value.metaPeso
        viewModelScope.launch {
            repository.inserirMedicao(
                ProgressoEntity(
                    dataRegistro = DataHora.dataAtual(),
                    pesoKg = pesoKg,
                    metaPesoKg = meta,
                    cinturaCm = cinturaCm,
                    quadrilCm = quadrilCm,
                ),
            )
        }
    }

    // DELETE (+ desfazer)
    fun excluirMedicao(medicao: ProgressoEntity) {
        viewModelScope.launch { repository.deletarMedicao(medicao) }
    }

    fun restaurarMedicao(medicao: ProgressoEntity) {
        viewModelScope.launch { repository.inserirMedicao(medicao) }
    }
}
