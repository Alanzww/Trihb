package com.example.trihb.ui.screens.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.data.local.entity.PostFeedEntity
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.components.AvatarIniciais
import com.example.trihb.ui.components.EmptyState
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.ui.theme.tabular

/** Rota Stateful da aba Feed (Comunidade estilo Strava). */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogAberto by rememberSaveable { mutableStateOf(false) }

    FeedContent(
        uiState = uiState,
        onAbrirDialog = { dialogAberto = true },
        onIncentivar = viewModel::alternarIncentivo,
        onExcluir = viewModel::excluirPost,
    )

    if (dialogAberto) {
        NovaConquistaDialog(
            onDismiss = { dialogAberto = false },
            onPublicar = { tag, texto ->
                viewModel.publicarConquista(tag, texto)
                dialogAberto = false
            },
        )
    }
}

/** Conteúdo Stateless da aba Feed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    uiState: FeedUiState,
    onAbrirDialog: () -> Unit,
    onIncentivar: (PostFeedEntity) -> Unit,
    onExcluir: (PostFeedEntity) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comunidade", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Conquistas e consistência do grupo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onAbrirDialog,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .heightIn(min = 40.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartilhar", style = MaterialTheme.typography.labelLarge)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.posts.isEmpty()) {
                item(key = "vazio") {
                    EmptyState(
                        icone = Icons.Rounded.Groups,
                        titulo = "Nenhuma conquista publicada ainda",
                        mensagem = "Compartilhe sua primeira meta cumprida para inspirar a comunidade!",
                        acaoRotulo = "Compartilhar conquista",
                        onAcao = onAbrirDialog,
                    )
                }
            } else {
                items(uiState.posts, key = { it.id }) { post ->
                    PostConquistaCard(
                        post = post,
                        ehAutorAtual = post.pertenceAoUsuario(uiState.usuarioAtivoId),
                        onIncentivar = { onIncentivar(post) },
                        onExcluir = { onExcluir(post) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** Card social estilo Strava: autor, selo de conquista, relato, aplausos e exclusão. */
@Composable
fun PostConquistaCard(
    post: PostFeedEntity,
    onIncentivar: () -> Unit,
    onExcluir: () -> Unit,
    modifier: Modifier = Modifier,
    ehAutorAtual: Boolean = post.isAutorUsuarioAtual,
) {
    val corBotao by animateColorAsState(
        targetValue = if (post.incentivadoPorMim) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "corIncentivo",
    )
    val corTextoBotao by animateColorAsState(
        targetValue = if (post.incentivadoPorMim) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        label = "corTextoIncentivo",
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabeçalho do autor
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarIniciais(
                    nome = post.autorNome,
                    corFundo = if (ehAutorAtual) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                    corTexto = if (ehAutorAtual) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.autorNome, style = MaterialTheme.typography.titleMedium)
                        if (ehAutorAtual) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• Você",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = if (post.autorFocoBadge.isNotBlank()) "${post.tempoAtras} • ${post.autorFocoBadge}" else post.tempoAtras,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ehAutorAtual) {
                    IconButton(onClick = onExcluir) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Excluir publicação",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Selo de conquista (badge)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = post.badgeConquista,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            // Relato
            if (post.descricao.isNotBlank()) {
                Text(
                    text = post.descricao,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Rodapé de incentivos (aplausos / kudos)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.incentivosCount} ${if (post.incentivosCount == 1) "incentivo" else "incentivos"}",
                        style = MaterialTheme.typography.labelLarge.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onIncentivar,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = corBotao,
                        contentColor = corTextoBotao,
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = if (post.incentivadoPorMim) "👏 Incentivado" else "👏 Incentivar",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** AlertDialog para publicar uma nova conquista na comunidade. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovaConquistaDialog(
    onDismiss: () -> Unit,
    onPublicar: (TagConquista, String) -> Unit,
) {
    var tagSelecionada by rememberSaveable { mutableStateOf(TagConquista.AGUA) }
    var texto by rememberSaveable { mutableStateOf("") }
    var tentouPublicar by rememberSaveable { mutableStateOf(false) }
    val erroTexto = tentouPublicar && texto.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartilhar Conquista", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Escolha o selo da sua conquista:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TagConquista.entries.forEach { tag ->
                        val selecionada = tag == tagSelecionada
                        FilterChip(
                            selected = selecionada,
                            onClick = { tagSelecionada = tag },
                            label = { Text(tag.rotulo) },
                            leadingIcon = if (selecionada) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else null,
                        )
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Como foi essa conquista?") },
                    placeholder = { Text("Ex.: Bati 2,5 L de água antes das 18h!") },
                    isError = erroTexto,
                    supportingText = if (erroTexto) {
                        { Text("Escreva um breve relato para publicar") }
                    } else null,
                    minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    tentouPublicar = true
                    if (texto.isNotBlank()) onPublicar(tagSelecionada, texto)
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedContentPreview() {
    TriHubTheme {
        FeedContent(
            uiState = FeedUiState(
                carregando = false,
                posts = listOf(
                    PostFeedEntity(
                        id = 1,
                        autorNome = "Marina S.",
                        tempoAtras = "há 25 min",
                        badgeConquista = "🔥 7º dia consecutivo batendo a meta de hidratação!",
                        descricao = "Garrafinha sempre por perto e lembrete no celular.",
                        incentivosCount = 14,
                        incentivadoPorMim = false,
                        isAutorUsuarioAtual = false,
                    ),
                    PostFeedEntity(
                        id = 2,
                        autorNome = "Alan",
                        tempoAtras = "Agora",
                        badgeConquista = "🥗 Refeição no Plano",
                        descricao = "Almoço completo dentro das calorias planejadas!",
                        incentivosCount = 3,
                        incentivadoPorMim = true,
                        isAutorUsuarioAtual = true,
                    ),
                ),
            ),
            onAbrirDialog = {},
            onIncentivar = {},
            onExcluir = {},
        )
    }
}
