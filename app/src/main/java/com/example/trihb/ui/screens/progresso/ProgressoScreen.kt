package com.example.trihb.ui.screens.progresso

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.data.local.entity.ProgressoEntity
import com.example.trihb.sensor.BonusAtividade
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.components.EmptyState
import com.example.trihb.ui.components.SensorStatusPill
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.ui.theme.tabular
import com.example.trihb.util.DataHora
import kotlinx.coroutines.launch

/** Campos do formulário de nova medição (texto cru digitado). */
data class MedicaoFormState(
    val peso: String = "",
    val cintura: String = "",
    val quadril: String = "",
) {
    val pesoKg: Float? get() = DataHora.parseDecimal(peso)?.takeIf { it in 20f..400f }
    val cinturaCm: Float? get() = DataHora.parseDecimal(cintura)?.takeIf { it in 30f..300f }
    val quadrilCm: Float? get() = DataHora.parseDecimal(quadril)?.takeIf { it in 30f..300f }
    val valido: Boolean get() = pesoKg != null && cinturaCm != null && quadrilCm != null
}

/** Rota Stateful da aba Progresso. */
@Composable
fun ProgressoScreen(
    viewModel: ProgressoViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var peso by rememberSaveable { mutableStateOf("") }
    var cintura by rememberSaveable { mutableStateOf("") }
    var quadril by rememberSaveable { mutableStateOf("") }
    var tentouRegistrar by rememberSaveable { mutableStateOf(false) }

    val form = MedicaoFormState(peso, cintura, quadril)

    ProgressoContent(
        uiState = uiState,
        form = form,
        mostrarErros = tentouRegistrar,
        snackbarHostState = snackbarHostState,
        onPesoChange = { peso = it },
        onCinturaChange = { cintura = it },
        onQuadrilChange = { quadril = it },
        onSimularPassos = viewModel::simularPassos,
        onRegistrar = {
            tentouRegistrar = true
            if (form.valido) {
                viewModel.registrarMedicao(form.pesoKg!!, form.cinturaCm!!, form.quadrilCm!!)
                peso = ""; cintura = ""; quadril = ""
                tentouRegistrar = false
                focusManager.clearFocus()
                scope.launch { snackbarHostState.showSnackbar("Medição registrada com sucesso ✅") }
            }
        },
        onExcluir = { medicao ->
            viewModel.excluirMedicao(medicao)
            scope.launch {
                val resultado = snackbarHostState.showSnackbar(
                    message = "Medição de ${medicao.dataRegistro} removida",
                    actionLabel = "Desfazer",
                    duration = SnackbarDuration.Short,
                )
                if (resultado == SnackbarResult.ActionPerformed) viewModel.restaurarMedicao(medicao)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressoContent(
    uiState: ProgressoUiState,
    form: MedicaoFormState,
    mostrarErros: Boolean,
    snackbarHostState: SnackbarHostState,
    onPesoChange: (String) -> Unit,
    onCinturaChange: (String) -> Unit,
    onQuadrilChange: (String) -> Unit,
    onSimularPassos: () -> Unit,
    onRegistrar: () -> Unit,
    onExcluir: (ProgressoEntity) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Meu Progresso", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Corpo, medidas e movimento em tempo real",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (uiState.carregando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "sensor") {
                SensorAtividadeCard(
                    passos = uiState.passos,
                    bonus = uiState.bonus,
                    sensorAtivo = uiState.sensorAtivo,
                    sensorDisponivel = uiState.sensorDisponivel,
                    onSimularPassos = onSimularPassos,
                )
            }

            item(key = "resumo") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResumoCard(
                        titulo = "Peso atual",
                        valor = uiState.pesoAtual?.let { "${DataHora.formatarDecimal(it)} kg" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    ResumoCard(
                        titulo = "Meta",
                        valor = "${DataHora.formatarDecimal(uiState.metaPeso)} kg",
                        modifier = Modifier.weight(1f),
                        corValor = MaterialTheme.colorScheme.primary,
                    )
                    val variacao = uiState.variacaoTotal
                    ResumoCard(
                        titulo = "Variação",
                        valor = variacao?.let { (if (it > 0) "+" else "") + DataHora.formatarDecimal(it) + " kg" } ?: "—",
                        modifier = Modifier.weight(1f),
                        corValor = when {
                            variacao == null -> MaterialTheme.colorScheme.onSurface
                            variacao <= 0f -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
            }

            item(key = "grafico") {
                GraficoEvolucaoCard(historico = uiState.historico, metaPeso = uiState.metaPeso, faltaParaMeta = uiState.faltaParaMeta)
            }

            item(key = "form") {
                NovaMedicaoCard(
                    form = form,
                    mostrarErros = mostrarErros,
                    onPesoChange = onPesoChange,
                    onCinturaChange = onCinturaChange,
                    onQuadrilChange = onQuadrilChange,
                    onRegistrar = onRegistrar,
                )
            }

            item(key = "histTitulo") {
                Text(
                    "Histórico de medições",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (uiState.historico.isEmpty()) {
                item(key = "vazio") {
                    EmptyState(
                        icone = Icons.Rounded.MonitorWeight,
                        titulo = "Nenhuma medição ainda",
                        mensagem = "Registre seu peso e medidas acima para acompanhar sua evolução.",
                    )
                }
            } else {
                items(uiState.historico.asReversed(), key = { it.id }) { medicao ->
                    MedicaoItem(
                        medicao = medicao,
                        onExcluir = { onExcluir(medicao) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Card de Monitoramento de Hardware (acelerômetro)
// ---------------------------------------------------------------------------
@Composable
private fun SensorAtividadeCard(
    passos: Int,
    bonus: BonusAtividade,
    sensorAtivo: Boolean,
    sensorDisponivel: Boolean,
    onSimularPassos: () -> Unit,
) {
    val progressoBloco by animateFloatAsState(
        targetValue = (passos % BonusAtividade.PASSOS_POR_BLOCO) / BonusAtividade.PASSOS_POR_BLOCO.toFloat(),
        label = "bloco",
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Atividade em tempo real", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                SensorStatusPill(passos = passos, ativo = sensorAtivo, disponivel = sensorDisponivel)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) { drawCircle(color = Color(0xFFE8F3EC)) }
                    Icon(
                        Icons.AutoMirrored.Rounded.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = DataHora.formatarInteiro(passos),
                        style = MaterialTheme.typography.headlineMedium.tabular(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { contentDescription = "$passos passos detectados" },
                    )
                    Text("passos detectados pelo acelerômetro", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetrica("${bonus.caloriasAtivasEstimadas}", "kcal ativas", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                MiniMetrica("+${bonus.bonusCalorias}", "kcal na meta", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MiniMetrica("+${bonus.bonusAguaMl}", "ml de água", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
            LinearProgressIndicator(
                progress = { progressoBloco },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                text = "Faltam ${bonus.passosParaProximoBonus} passos para +${BonusAtividade.KCAL_POR_BLOCO} kcal e +${BonusAtividade.AGUA_ML_POR_BLOCO} ml na sua meta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onSimularPassos,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simular +${BonusAtividade.PASSOS_POR_BLOCO} passos")
            }
        }
    }
}

@Composable
private fun MiniMetrica(valor: String, rotulo: String, cor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = MaterialTheme.typography.titleMedium.tabular(), color = cor)
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ResumoCard(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    corValor: Color = MaterialTheme.colorScheme.onSurface,
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleMedium.tabular(), color = corValor)
        }
    }
}

// ---------------------------------------------------------------------------
// Gráfico de Evolução — desenhado nativamente com Compose Canvas
// ---------------------------------------------------------------------------
@Composable
private fun GraficoEvolucaoCard(historico: List<ProgressoEntity>, metaPeso: Float, faltaParaMeta: Float?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Evolução do peso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (faltaParaMeta != null) {
                    Text(
                        text = if (faltaParaMeta > 0f) "Faltam ${DataHora.formatarDecimal(faltaParaMeta)} kg" else "Meta atingida! 🎉",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (historico.size < 2) {
                Text(
                    "Registre ao menos 2 medições para visualizar a linha de tendência.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                GraficoPesoCanvas(
                    historico = historico,
                    metaPeso = metaPeso,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .semantics {
                            contentDescription = "Gráfico de evolução do peso com ${historico.size} registros. " +
                                "Último: ${DataHora.formatarDecimal(historico.last().pesoKg)} kg"
                        },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendaItem(MaterialTheme.colorScheme.primary, "Peso (kg)")
                    LegendaItem(MaterialTheme.colorScheme.tertiary, "Meta ${DataHora.formatarDecimal(metaPeso)} kg")
                }
            }
        }
    }
}

@Composable
private fun LegendaItem(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(cor) }
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GraficoPesoCanvas(historico: List<ProgressoEntity>, metaPeso: Float, modifier: Modifier = Modifier) {
    val corLinha = MaterialTheme.colorScheme.primary
    val corMeta = MaterialTheme.colorScheme.tertiary
    val corGrade = MaterialTheme.colorScheme.outlineVariant
    val corSuperficie = MaterialTheme.colorScheme.surface
    val estiloRotulo = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val estiloValor = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface).tabular()
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val pesos = historico.map { it.pesoKg }
        val minimo = minOf(pesos.min(), metaPeso) - 1f
        val maximo = maxOf(pesos.max(), metaPeso) + 1f
        val faixa = (maximo - minimo).takeIf { it > 0f } ?: 1f

        val margemTopo = 24.dp.toPx()
        val margemBase = 24.dp.toPx()
        val margemLateral = 20.dp.toPx()
        val altura = size.height - margemTopo - margemBase
        val largura = size.width - margemLateral * 2

        fun y(peso: Float) = margemTopo + altura * (1f - (peso - minimo) / faixa)
        fun x(indice: Int) = margemLateral + if (pesos.size == 1) largura / 2 else largura * indice / (pesos.size - 1)

        // Grade horizontal suave
        repeat(4) { i ->
            val gy = margemTopo + altura * i / 3f
            drawLine(corGrade, Offset(margemLateral, gy), Offset(size.width - margemLateral, gy), strokeWidth = 1.dp.toPx())
        }

        // Linha tracejada da meta
        val yMeta = y(metaPeso)
        drawLine(
            color = corMeta,
            start = Offset(margemLateral, yMeta),
            end = Offset(size.width - margemLateral, yMeta),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        // Área sob a curva (gradiente) + linha de tendência
        val pontos = pesos.mapIndexed { i, p -> Offset(x(i), y(p)) }
        val linha = Path().apply {
            moveTo(pontos.first().x, pontos.first().y)
            pontos.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(linha)
            lineTo(pontos.last().x, margemTopo + altura)
            lineTo(pontos.first().x, margemTopo + altura)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(corLinha.copy(alpha = 0.18f), Color.Transparent)))
        drawPath(linha, corLinha, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Pontos, valores e datas
        pontos.forEachIndexed { i, ponto ->
            drawCircle(corSuperficie, radius = 7.dp.toPx(), center = ponto)
            drawCircle(corLinha, radius = 5.dp.toPx(), center = ponto)

            val valor = textMeasurer.measure(DataHora.formatarDecimal(pesos[i]), estiloValor)
            drawText(
                valor,
                topLeft = Offset(
                    (ponto.x - valor.size.width / 2f).coerceIn(0f, size.width - valor.size.width),
                    (ponto.y - valor.size.height - 8.dp.toPx()).coerceAtLeast(0f),
                ),
            )
            val data = textMeasurer.measure(historico[i].dataRegistro.take(5), estiloRotulo)
            drawText(
                data,
                topLeft = Offset(
                    (ponto.x - data.size.width / 2f).coerceIn(0f, size.width - data.size.width),
                    size.height - data.size.height,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Formulário de Nova Medição + Histórico
// ---------------------------------------------------------------------------
@Composable
private fun NovaMedicaoCard(
    form: MedicaoFormState,
    mostrarErros: Boolean,
    onPesoChange: (String) -> Unit,
    onCinturaChange: (String) -> Unit,
    onQuadrilChange: (String) -> Unit,
    onRegistrar: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nova medição", style = MaterialTheme.typography.titleMedium)
            CampoNumerico(
                valor = form.peso,
                onChange = onPesoChange,
                rotulo = "Peso (kg)",
                erro = if (mostrarErros && form.pesoKg == null) "Informe um peso válido" else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumerico(
                    valor = form.cintura,
                    onChange = onCinturaChange,
                    rotulo = "Cintura (cm)",
                    erro = if (mostrarErros && form.cinturaCm == null) "Valor inválido" else null,
                    modifier = Modifier.weight(1f),
                )
                CampoNumerico(
                    valor = form.quadril,
                    onChange = onQuadrilChange,
                    rotulo = "Quadril (cm)",
                    erro = if (mostrarErros && form.quadrilCm == null) "Valor inválido" else null,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = onRegistrar,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar Medição")
            }
        }
    }
}

@Composable
private fun CampoNumerico(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    erro: String?,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo -> onChange(novo.filter { it.isDigit() || it == ',' || it == '.' }.take(6)) },
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        textStyle = MaterialTheme.typography.bodyLarge.tabular(),
        modifier = modifier,
    )
}

@Composable
private fun MedicaoItem(medicao: ProgressoEntity, onExcluir: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(medicao.dataRegistro, style = MaterialTheme.typography.labelLarge.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${DataHora.formatarDecimal(medicao.pesoKg)} kg", style = MaterialTheme.typography.titleMedium.tabular())
                Text(
                    "Cintura ${DataHora.formatarDecimal(medicao.cinturaCm)} cm  •  Quadril ${DataHora.formatarDecimal(medicao.quadrilCm)} cm",
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExcluir) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Excluir medição de ${medicao.dataRegistro}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressoPreview() {
    TriHubTheme {
        ProgressoContent(
            uiState = ProgressoUiState(
                carregando = false,
                historico = listOf(
                    ProgressoEntity(1, 1, "01/10/2026", 76.0f, 72f, 83f, 99f),
                    ProgressoEntity(2, 1, "01/11/2026", 75.1f, 72f, 82f, 98.5f),
                    ProgressoEntity(3, 1, "23/11/2026", 74.2f, 72f, 81f, 98f),
                ),
                passos = 47,
                sensorAtivo = true,
            ),
            form = MedicaoFormState(),
            mostrarErros = false,
            snackbarHostState = remember { SnackbarHostState() },
            onPesoChange = {}, onCinturaChange = {}, onQuadrilChange = {},
            onSimularPassos = {}, onRegistrar = {}, onExcluir = {},
        )
    }
}
