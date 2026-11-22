package com.example.trihb.ui.screens.dieta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.data.local.entity.RefeicaoEntity
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.components.AvatarFotoOuIniciais
import com.example.trihb.ui.components.EmptyState
import com.example.trihb.ui.components.MetaDiariaCard
import com.example.trihb.ui.components.RefeicaoItemCard
import com.example.trihb.ui.theme.TriHubTheme

/** Rota Stateful da aba Dieta: coleta o StateFlow e delega para [DietaContent]. */
@Composable
fun DietaScreen(
    onNovaRefeicao: () -> Unit,
    onEditarRefeicao: (Int) -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onAbrirTutorial: () -> Unit = {},
    viewModel: DietaViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DietaContent(
        uiState = uiState,
        onToggleRefeicao = viewModel::toggleRefeicaoConcluida,
        onAdicionarAgua = { viewModel.ajustarAgua(+250) },
        onRemoverAgua = { viewModel.ajustarAgua(-250) },
        onNovaRefeicao = onNovaRefeicao,
        onEditarRefeicao = { onEditarRefeicao(it.id) },
        onAbrirPerfil = onAbrirPerfil,
        onAbrirTutorial = onAbrirTutorial,
    )
}

/** Conteúdo Stateless: recebe apenas estado imutável e lambdas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietaContent(
    uiState: DietaUiState,
    onToggleRefeicao: (RefeicaoEntity) -> Unit,
    onAdicionarAgua: () -> Unit,
    onRemoverAgua: () -> Unit,
    onNovaRefeicao: () -> Unit,
    onEditarRefeicao: (RefeicaoEntity) -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onAbrirTutorial: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Olá, ${uiState.nomeUsuario}! 👋", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${uiState.focoUsuario} • Constância vence perfeição 🌿",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAbrirTutorial) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "Abrir tutorial do aplicativo",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onAbrirPerfil,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        AvatarFotoOuIniciais(
                            nome = uiState.nomeUsuario,
                            fotoUri = uiState.fotoUriUsuario,
                            tamanho = 38.dp,
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNovaRefeicao,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nova Refeição", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { innerPadding ->
        if (uiState.carregando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "meta") {
                MetaDiariaCard(
                    consumidoMl = uiState.consumidoMl,
                    metaAguaTotalMl = uiState.metaAguaTotalMl,
                    bonusAguaMl = uiState.bonus.bonusAguaMl,
                    progressoAgua = uiState.progressoAgua,
                    caloriasConcluidas = uiState.caloriasConcluidas,
                    metaCaloriasTotal = uiState.metaCaloriasTotal,
                    bonusCalorias = uiState.bonus.bonusCalorias,
                    progressoCalorias = uiState.progressoCalorias,
                    passos = uiState.passos,
                    sensorAtivo = uiState.sensorAtivo,
                    sensorDisponivel = uiState.sensorDisponivel,
                    onAdicionarAgua = onAdicionarAgua,
                    onRemoverAgua = onRemoverAgua,
                )
            }

            item(key = "cabecalho") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Refeições de hoje",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.refeicoes.isNotEmpty()) {
                        Text(
                            text = "${uiState.refeicoesConcluidas}/${uiState.refeicoes.size} concluídas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (uiState.refeicoes.isEmpty()) {
                item(key = "vazio") {
                    EmptyState(
                        icone = Icons.Rounded.RestaurantMenu,
                        titulo = "Nenhuma refeição planejada",
                        mensagem = "Adicione sua primeira refeição do dia para começar a acompanhar o plano.",
                        acaoRotulo = "Adicionar refeição",
                        onAcao = onNovaRefeicao,
                    )
                }
            } else {
                items(uiState.refeicoes, key = { it.id }) { refeicao ->
                    RefeicaoItemCard(
                        refeicao = refeicao,
                        onConcluidaChange = { onToggleRefeicao(refeicao) },
                        onEditar = { onEditarRefeicao(refeicao) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DietaContentPreview() {
    TriHubTheme {
        DietaContent(
            uiState = DietaUiState(
                carregando = false,
                refeicoes = listOf(
                    RefeicaoEntity(1, 1, "07:30", "Café da Manhã", "3 ovos mexidos + pão integral", 420, true),
                    RefeicaoEntity(2, 1, "12:30", "Almoço", "150g frango + 100g arroz + salada", 580, false),
                ),
                consumidoMl = 1500,
                caloriasConcluidas = 420,
                passos = 40,
                sensorAtivo = true,
            ),
            onToggleRefeicao = {}, onAdicionarAgua = {}, onRemoverAgua = {},
            onNovaRefeicao = {}, onEditarRefeicao = {},
        )
    }
}
