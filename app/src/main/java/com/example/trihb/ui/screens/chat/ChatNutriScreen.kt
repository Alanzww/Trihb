package com.example.trihb.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.data.local.entity.MensagemEntity
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.components.AvatarIniciais
import com.example.trihb.ui.components.EmptyState
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.ui.theme.tabular

/** Rota Stateful da aba Nutri (Chat Híbrido com Dra. Camila Nutrition). */
@Composable
fun ChatNutriScreen(
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textoDigitado by rememberSaveable { mutableStateOf("") }

    ChatNutriContent(
        uiState = uiState,
        textoDigitado = textoDigitado,
        onTextoChange = { textoDigitado = it },
        onEnviar = {
            if (textoDigitado.isNotBlank()) {
                viewModel.enviarMensagem(textoDigitado)
                textoDigitado = ""
            }
        },
        onSugestaoClick = viewModel::enviarMensagem,
    )
}

/** Conteúdo Stateless da aba Nutri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatNutriContent(
    uiState: ChatUiState,
    textoDigitado: String,
    onTextoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugestaoClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.mensagens.size, uiState.nutriDigitando) {
        val total = uiState.mensagens.size + if (uiState.nutriDigitando) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarIniciais(
                            nome = "Camila Nutrition",
                            corFundo = MaterialTheme.colorScheme.primaryContainer,
                            corTexto = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Dra. Camila Nutrition", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.geminiConfigurado) {
                                        "IA Híbrida Gemini + Room • ${uiState.focoPaciente}"
                                    } else {
                                        "IA Local Conectada ao Plano • ${uiState.focoPaciente}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            BarraEnvioMensagem(
                texto = textoDigitado,
                onTextoChange = onTextoChange,
                onEnviar = onEnviar,
                onSugestaoClick = onSugestaoClick,
            )
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

        if (uiState.mensagens.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icone = Icons.AutoMirrored.Rounded.Chat,
                    titulo = "Tire suas dúvidas com a Dra. Camila",
                    mensagem = "Ela lê suas refeições, água, peso, foco e passos em tempo real para orientar substituições!",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.mensagens, key = { it.id }) { mensagem ->
                BalaoMensagem(
                    mensagem = mensagem,
                    modifier = Modifier.animateItem(),
                )
            }
            if (uiState.nutriDigitando) {
                item(key = "digitando") {
                    IndicadorDigitando()
                }
            }
        }
    }
}

/**
 * Balão de conversa com geometria de 18.dp e canto inferior assimétrico mais contido (4.dp)
 * indicando a direção do remetente (NUTRI à esquerda, PACIENTE à direita).
 */
@Composable
private fun BalaoMensagem(
    mensagem: MensagemEntity,
    modifier: Modifier = Modifier,
) {
    val daNutri = mensagem.isDaNutri
    val formato = if (daNutri) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    }
    val corFundo = if (daNutri) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val corTexto = if (daNutri) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    val corHorario = if (daNutri) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (daNutri) Alignment.Start else Alignment.End,
    ) {
        Surface(
            shape = formato,
            color = corFundo,
            shadowElevation = if (daNutri) 1.dp else 0.dp,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = mensagem.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = corTexto,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (daNutri) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = if (mensagem.fonteIa == MensagemEntity.FONTE_GEMINI) {
                                "Gemini IA •"
                            } else {
                                "Contexto do Plano •"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = mensagem.horario,
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = corHorario,
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicadorDigitando() {
    Surface(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "Dra. Camila está analisando seu plano…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun BarraEnvioMensagem(
    texto: String,
    onTextoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugestaoClick: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Sugestões rápidas conectadas ao plano real do usuário no Room
            AnimatedVisibility(visible = texto.isBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                ) {
                    listOf(
                        "O que falta comer hoje?",
                        "Como estão minhas calorias?",
                        "Quanto falta de água?",
                        "Trocar frango por ovo?",
                        "Dica para meu treino",
                        "Como está meu IMC?",
                    ).forEach { sugestao ->
                        SuggestionChip(
                            onClick = { onSugestaoClick(sugestao) },
                            label = { Text(sugestao, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = onTextoChange,
                    placeholder = { Text("Pergunte sobre seu plano ou substituição…") },
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { onEnviar() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = onEnviar,
                    enabled = texto.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Enviar mensagem",
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatNutriPreview() {
    TriHubTheme {
        ChatNutriContent(
            uiState = ChatUiState(
                carregando = false,
                mensagens = listOf(
                    MensagemEntity(1, 1, "NUTRI", "Olá Alan! Ajustei os carboidratos do seu pré-treino para hoje!", "10:15"),
                    MensagemEntity(2, 1, "PACIENTE", "Perfeito, já visualizei no plano. Posso trocar arroz por batata?", "10:20"),
                    MensagemEntity(3, 1, "NUTRI", "Pode sim! Mesma proporção: cerca de 120g de batata doce.", "10:22"),
                ),
            ),
            textoDigitado = "",
            onTextoChange = {},
            onEnviar = {},
            onSugestaoClick = {},
        )
    }
}
