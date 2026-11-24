package com.example.trihb.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trihb.ui.navigation.Screen
import com.example.trihb.ui.theme.TriHubTheme

/** Descreve um botão ou controle explicado dentro de um passo do tutorial. */
data class AcaoBotaoTutorial(
    val botao: String,
    val explicacao: String,
)

/** Passo do tour guiado interativo do TriHub. */
data class PassoTutorial(
    val titulo: String,
    val subtitulo: String,
    val icone: ImageVector,
    val abaAlvo: Screen,
    val botoesExplicados: List<AcaoBotaoTutorial>,
)

val PASSOS_TUTORIAL_TRIHUB: List<PassoTutorial> = listOf(
    PassoTutorial(
        titulo = "Bem-vindo ao TriHub! 🌿",
        subtitulo = "Um tour rápido pelas 5 áreas do seu aplicativo 100% Offline-First. Você pode avançar passo a passo ou pular quando quiser.",
        icone = Icons.Rounded.Explore,
        abaAlvo = Screen.Dieta,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Barra Inferior (5 Abas)",
                explicacao = "Alterna em 1 toque entre Dieta, Progresso, Feed, Nutri (IA) e Perfil.",
            ),
            AcaoBotaoTutorial(
                botao = "100% Offline-First (Modo Avião)",
                explicacao = "Tudo o que você faz é salvo na hora no banco relacional Room (SQLite) do aparelho.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão (?) no topo da Dieta",
                explicacao = "Reabre este tutorial interativo sempre que você quiser demonstrar as funções.",
            ),
        ),
    ),
    PassoTutorial(
        titulo = "Aba 1: Dieta & Meta Diária",
        subtitulo = "Controle sua hidratação, calorias e refeições planejadas com apenas um toque.",
        icone = Icons.Rounded.Restaurant,
        abaAlvo = Screen.Dieta,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Botões +250 ml / -250 ml",
                explicacao = "Adicionam ou removem um copo de água na sua meta diária instantaneamente.",
            ),
            AcaoBotaoTutorial(
                botao = "Checkbox (caixinha à esquerda)",
                explicacao = "Marca a refeição como concluída e atualiza a barra de progresso calórico.",
            ),
            AcaoBotaoTutorial(
                botao = "Tocar no Card ou Ícone ✏️",
                explicacao = "Abre a tela de detalhes para editar horário, descrição, calorias ou excluir a refeição.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão flutuante + Nova Refeição",
                explicacao = "Cadastra uma nova refeição personalizada no cronograma da sua conta.",
            ),
        ),
    ),
    PassoTutorial(
        titulo = "Aba 2: Progresso & Acelerômetro",
        subtitulo = "Integração real com o sensor de hardware do celular e gráfico nativo em Compose Canvas.",
        icone = Icons.AutoMirrored.Rounded.ShowChart,
        abaAlvo = Screen.Progresso,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Sensor Acelerômetro (Ao Vivo)",
                explicacao = "Caminhe ou agite o celular: a cada 20 passos, sua meta na aba Dieta ganha +15 kcal e +25 ml de água.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão Simular +20 passos",
                explicacao = "Adiciona 20 passos manualmente para facilitar demonstrações em laboratório.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão Registrar Medição",
                explicacao = "Salva novo peso, cintura e quadril no Room e redesenha a curva do Gráfico Canvas na hora.",
            ),
            AcaoBotaoTutorial(
                botao = "Ícone 🗑️ no Histórico",
                explicacao = "Remove uma medição antiga com opção imediata de 'Desfazer'.",
            ),
        ),
    ),
    PassoTutorial(
        titulo = "Aba 3: Feed da Comunidade",
        subtitulo = "Mural estilo Strava para compartilhar constância e incentivar outros membros.",
        icone = Icons.Rounded.Groups,
        abaAlvo = Screen.Feed,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Botão + Compartilhar (topo)",
                explicacao = "Abre a janela para escolher um selo (💧 Água, 🥗 Refeição ou 🏃 Treino) e publicar seu relato.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão 👏 Incentivar",
                explicacao = "Soma ou retira seu aplauso (kudos) nas conquistas da comunidade.",
            ),
            AcaoBotaoTutorial(
                botao = "Ícone 🗑️ nas suas postagens",
                explicacao = "Exclui publicações criadas pela sua própria conta.",
            ),
        ),
    ),
    PassoTutorial(
        titulo = "Aba 4: Nutri — Dra. Camila (IA Híbrida)",
        subtitulo = "Sua nutricionista virtual conectada em tempo real aos dados da sua conta no Room.",
        icone = Icons.AutoMirrored.Rounded.Chat,
        abaAlvo = Screen.Chat,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Chips de Sugestão Rápida",
                explicacao = "Toque em 'O que falta comer hoje?' ou 'Como estão minhas calorias?' para perguntar em 1 clique.",
            ),
            AcaoBotaoTutorial(
                botao = "Campo de Texto + Botão Enviar (➤)",
                explicacao = "A Dra. Camila analisa suas refeições pendentes, água, peso, IMC, foco, restrições e passos para responder (offline ou via Gemini IA).",
            ),
        ),
    ),
    PassoTutorial(
        titulo = "Aba 5: Meu Perfil & Contas",
        subtitulo = "Personalize foto, bio, peso, altura, IMC, modalidades esportivas, foco e restrições.",
        icone = Icons.Rounded.Person,
        abaAlvo = Screen.Perfil,
        botoesExplicados = listOf(
            AcaoBotaoTutorial(
                botao = "Botão Adicionar / Trocar Foto",
                explicacao = "Seleciona uma imagem da galeria e salva no armazenamento interno para funcionar offline.",
            ),
            AcaoBotaoTutorial(
                botao = "Chips de Foco, Modalidades e Restrições",
                explicacao = "Ajustam suas metas e adaptam as recomendações da Dra. Camila (ex.: Sem Lactose, Vegetariano).",
            ),
            AcaoBotaoTutorial(
                botao = "Botão Salvar Alterações do Perfil",
                explicacao = "Persiste seus novos dados corporais, IMC e metas no banco Room.",
            ),
            AcaoBotaoTutorial(
                botao = "Botão Trocar Conta / Sair",
                explicacao = "Encerra a sessão atual e retorna à tela de Login para entrar com outro usuário.",
            ),
        ),
    ),
)

/**
 * Overlay interativo de tutorial (Coach-Mark) que navega pelas abas reais ao fundo
 * explicando cada botão e oferece a opção "Pular tutorial" em todos os passos.
 */
@Composable
fun TutorialOverlay(
    onMudarAbaAlvo: (Screen) -> Unit,
    onConcluirOuPular: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var indiceAtual by rememberSaveable { mutableIntStateOf(0) }
    val passo = PASSOS_TUTORIAL_TRIHUB[indiceAtual]
    val ehUltimoPasso = indiceAtual == PASSOS_TUTORIAL_TRIHUB.lastIndex

    // Navega automaticamente para a aba explicada pelo passo atual
    LaunchedEffect(indiceAtual) {
        onMudarAbaAlvo(PASSOS_TUTORIAL_TRIHUB[indiceAtual].abaAlvo)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Topo: Indicador do passo + Botão Pular Tutorial
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Pilula(
                        texto = "Tutorial • Passo ${indiceAtual + 1} de ${PASSOS_TUTORIAL_TRIHUB.size}",
                        icone = passo.icone,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onConcluirOuPular) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Pular tutorial")
                    }
                }

                // Conteúdo animado do passo
                AnimatedContent(
                    targetState = passo,
                    label = "passoTutorial",
                ) { p ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = p.titulo,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = p.subtitulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "O que cada botão desta tela faz:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        p.botoesExplicados.forEach { item ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = "• ${item.botao}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = item.explicacao,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Indicador de páginas (bolinhas) + Botões Anterior / Próximo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PASSOS_TUTORIAL_TRIHUB.indices.forEach { i ->
                            val ativo = i == indiceAtual
                            Box(
                                modifier = Modifier
                                    .size(if (ativo) 10.dp else 7.dp)
                                    .background(
                                        color = if (ativo) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (indiceAtual > 0) {
                            OutlinedButton(
                                onClick = { indiceAtual-- },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Anterior")
                            }
                        }

                        Button(
                            onClick = {
                                if (ehUltimoPasso) {
                                    onConcluirOuPular()
                                } else {
                                    indiceAtual++
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.heightIn(min = 44.dp),
                        ) {
                            Text(if (ehUltimoPasso) "Começar a usar" else "Próximo")
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (ehUltimoPasso) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TutorialOverlayPreview() {
    TriHubTheme {
        TutorialOverlay(
            onMudarAbaAlvo = {},
            onConcluirOuPular = {},
        )
    }
}
