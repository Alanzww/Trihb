package com.example.trihb.ui.screens.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.ModalidadeEsportiva
import com.example.trihb.data.local.entity.NivelAtividade
import com.example.trihb.data.local.entity.RestricaoAlimentar
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.components.AvatarFotoOuIniciais
import com.example.trihb.ui.components.Pilula
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.ui.theme.tabular
import com.example.trihb.util.DataHora
import kotlinx.coroutines.launch

/** Rota Stateful da Tela de Perfil (Foto, Bio, Peso, Modalidades, Foco e Conta). */
@Composable
fun PerfilScreen(
    onLogout: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var idCarregado by rememberSaveable { mutableIntStateOf(-1) }
    var nome by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var peso by rememberSaveable { mutableStateOf("") }
    var metaPeso by rememberSaveable { mutableStateOf("") }
    var altura by rememberSaveable { mutableStateOf("") }
    var metaCalorias by rememberSaveable { mutableStateOf("") }
    var metaAgua by rememberSaveable { mutableStateOf("") }
    var focoCodigo by rememberSaveable { mutableStateOf(FocoPrincipal.EMAGRECIMENTO.codigo) }
    var modalidadesCsv by rememberSaveable { mutableStateOf("") }
    var nivelCodigo by rememberSaveable { mutableStateOf(NivelAtividade.MODERADO.codigo) }
    var restricoesCsv by rememberSaveable { mutableStateOf("") }
    var geminiKey by rememberSaveable { mutableStateOf("") }

    // Sincroniza os campos sempre que o usuário logado mudar
    LaunchedEffect(uiState.usuario?.id, uiState.geminiApiKey) {
        val u = uiState.usuario ?: return@LaunchedEffect
        if (idCarregado != u.id) {
            idCarregado = u.id
            nome = u.nome
            bio = u.bio
            peso = DataHora.formatarDecimal(u.pesoAtualKg)
            metaPeso = DataHora.formatarDecimal(u.metaPesoKg)
            altura = DataHora.formatarDecimal(u.alturaCm, casas = 0)
            metaCalorias = u.metaCaloriasKcal.toString()
            metaAgua = u.metaAguaMl.toString()
            focoCodigo = u.focoPrincipal
            modalidadesCsv = u.modalidades
            nivelCodigo = u.nivelAtividade
            restricoesCsv = u.restricoesAlimentares
            geminiKey = uiState.geminiApiKey
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.salvarFotoPerfil(context, uri) { sucesso ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (sucesso) "Foto de perfil atualizada ✅" else "Não foi possível carregar a imagem",
                    )
                }
            }
        }
    }

    val modalidadesSet = remember(modalidadesCsv) {
        modalidadesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
    val restricoesSet = remember(restricoesCsv) {
        restricoesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    PerfilContent(
        uiState = uiState,
        nome = nome,
        bio = bio,
        peso = peso,
        metaPeso = metaPeso,
        altura = altura,
        metaCalorias = metaCalorias,
        metaAgua = metaAgua,
        foco = FocoPrincipal.fromCodigo(focoCodigo),
        modalidades = modalidadesSet,
        nivel = NivelAtividade.fromCodigo(nivelCodigo),
        restricoes = restricoesSet,
        geminiKey = geminiKey,
        snackbarHostState = snackbarHostState,
        onEscolherFoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemoverFoto = {
            viewModel.removerFotoPerfil()
            scope.launch { snackbarHostState.showSnackbar("Foto removida") }
        },
        onNomeChange = { nome = it },
        onBioChange = { bio = it },
        onPesoChange = { peso = it },
        onMetaPesoChange = { metaPeso = it },
        onAlturaChange = { altura = it },
        onMetaCaloriasChange = { metaCalorias = it.filter(Char::isDigit).take(4) },
        onMetaAguaChange = { metaAgua = it.filter(Char::isDigit).take(4) },
        onFocoChange = {
            focoCodigo = it.codigo
            metaCalorias = it.metaCaloriasSugerida.toString()
        },
        onToggleModalidade = { mod ->
            val novo = modalidadesSet.toMutableSet()
            if (!novo.add(mod)) novo.remove(mod)
            modalidadesCsv = novo.joinToString(",")
        },
        onNivelChange = { nivelCodigo = it.codigo },
        onToggleRestricao = { rest ->
            val novo = restricoesSet.toMutableSet()
            if (!novo.add(rest)) novo.remove(rest)
            restricoesCsv = novo.joinToString(",")
        },
        onGeminiKeyChange = { geminiKey = it },
        onSalvar = {
            val u = uiState.usuario ?: return@PerfilContent
            val pesoF = DataHora.parseDecimal(peso) ?: u.pesoAtualKg
            val metaPesoF = DataHora.parseDecimal(metaPeso) ?: u.metaPesoKg
            val alturaF = DataHora.parseDecimal(altura) ?: u.alturaCm
            val calInt = metaCalorias.toIntOrNull() ?: u.metaCaloriasKcal
            val aguaInt = metaAgua.toIntOrNull() ?: u.metaAguaMl

            focusManager.clearFocus()
            viewModel.salvarAlteracoesPerfil(
                nome = nome,
                bio = bio,
                pesoAtualKg = pesoF,
                metaPesoKg = metaPesoF,
                alturaCm = alturaF,
                metaCaloriasKcal = calInt,
                metaAguaMl = aguaInt,
                focoPrincipal = FocoPrincipal.fromCodigo(focoCodigo),
                modalidades = modalidadesSet.toList(),
                nivelAtividade = NivelAtividade.fromCodigo(nivelCodigo),
                restricoesAlimentares = restricoesSet.toList(),
                geminiApiKey = geminiKey,
            ) {
                scope.launch {
                    snackbarHostState.showSnackbar("Perfil e metas atualizados no Room ✅")
                }
            }
        },
        onSair = { viewModel.sairDaConta(onLogout) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerfilContent(
    uiState: PerfilUiState,
    nome: String,
    bio: String,
    peso: String,
    metaPeso: String,
    altura: String,
    metaCalorias: String,
    metaAgua: String,
    foco: FocoPrincipal,
    modalidades: Set<String>,
    nivel: NivelAtividade,
    restricoes: Set<String>,
    geminiKey: String,
    snackbarHostState: SnackbarHostState,
    onEscolherFoto: () -> Unit,
    onRemoverFoto: () -> Unit,
    onNomeChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onMetaPesoChange: (String) -> Unit,
    onAlturaChange: (String) -> Unit,
    onMetaCaloriasChange: (String) -> Unit,
    onMetaAguaChange: (String) -> Unit,
    onFocoChange: (FocoPrincipal) -> Unit,
    onToggleModalidade: (String) -> Unit,
    onNivelChange: (NivelAtividade) -> Unit,
    onToggleRestricao: (String) -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onSalvar: () -> Unit,
    onSair: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val usuario = uiState.usuario

    // IMC calculado ao vivo conforme o usuário digita peso e altura
    val pesoVal = DataHora.parseDecimal(peso) ?: usuario?.pesoAtualKg ?: 74f
    val alturaVal = DataHora.parseDecimal(altura) ?: usuario?.alturaCm ?: 175f
    val usuarioPreviewImc = (usuario ?: UsuarioEntity(nome = nome, email = "", senhaHash = ""))
        .copy(pesoAtualKg = pesoVal, alturaCm = alturaVal)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Meu Perfil", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = usuario?.email ?: "Conta local • Offline-First",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSair,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Trocar Conta")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Card de Cabeçalho: Foto + Resumo + IMC
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AvatarFotoOuIniciais(
                        nome = nome.ifBlank { usuario?.nome ?: "Alan" },
                        fotoUri = usuario?.fotoUri.orEmpty(),
                        tamanho = 88.dp,
                    )

                    Pilula(texto = foco.rotuloComEmoji)

                    if (bio.isNotBlank()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onEscolherFoto,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (usuario?.fotoUri.isNullOrBlank()) "Adicionar foto" else "Trocar foto")
                        }
                        if (!usuario?.fotoUri.isNullOrBlank()) {
                            TextButton(
                                onClick = onRemoverFoto,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Remover")
                            }
                        }
                    }

                    // Mini indicadores: IMC, Refeições hoje e Água hoje
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        IndicadorPerfil(
                            valor = usuarioPreviewImc.imc?.let { DataHora.formatarDecimal(it) } ?: "—",
                            rotulo = "IMC (${usuarioPreviewImc.classificacaoImc})",
                        )
                        IndicadorPerfil(
                            valor = "${uiState.refeicoesConcluidasHoje}/${uiState.totalRefeicoesHoje}",
                            rotulo = "Refeições hoje",
                        )
                        IndicadorPerfil(
                            valor = "${uiState.aguaConsumidaHojeMl} ml",
                            rotulo = "Água hoje",
                        )
                    }
                }
            }

            // 2. Identidade e Descrição (Bio)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sobre você", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = nome,
                        onValueChange = onNomeChange,
                        label = { Text("Nome de exibição") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = onBioChange,
                        label = { Text("Descrição / Bio") },
                        placeholder = { Text("Conte seu objetivo, rotina de treino e motivação…") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 3. Peso, Altura e Metas Diárias
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Peso, altura e metas diárias", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Alterar o peso atual também adiciona um novo registro no gráfico da aba Progresso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = onPesoChange,
                            label = { Text("Peso (kg)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = metaPeso,
                            onValueChange = onMetaPesoChange,
                            label = { Text("Meta (kg)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = altura,
                            onValueChange = onAlturaChange,
                            label = { Text("Altura (cm)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = metaCalorias,
                            onValueChange = onMetaCaloriasChange,
                            label = { Text("Meta Calorias (kcal)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = metaAgua,
                            onValueChange = onMetaAguaChange,
                            label = { Text("Meta Água (ml)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 4. Foco Principal e Modalidades
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Foco principal", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FocoPrincipal.entries.forEach { item ->
                            val selecionado = foco == item
                            FilterChip(
                                selected = selecionado,
                                onClick = { onFocoChange(item) },
                                label = { Text(item.rotuloComEmoji) },
                                leadingIcon = if (selecionado) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null,
                            )
                        }
                    }

                    Text("Modalidades praticadas", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ModalidadeEsportiva.entries.forEach { mod ->
                            val selecionada = mod.rotulo in modalidades
                            FilterChip(
                                selected = selecionada,
                                onClick = { onToggleModalidade(mod.rotulo) },
                                label = { Text(mod.rotuloComEmoji) },
                                leadingIcon = if (selecionada) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null,
                            )
                        }
                    }

                    Text("Nível de atividade", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NivelAtividade.entries.forEach { niv ->
                            FilterChip(
                                selected = nivel == niv,
                                onClick = { onNivelChange(niv) },
                                label = { Text(niv.rotulo) },
                            )
                        }
                    }

                    Text("Restrições e preferências alimentares", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RestricaoAlimentar.entries.forEach { rest ->
                            val selecionada = rest.rotulo in restricoes
                            FilterChip(
                                selected = selecionada,
                                onClick = { onToggleRestricao(rest.rotulo) },
                                label = { Text(rest.rotuloComEmoji) },
                                leadingIcon = if (selecionada) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // 5. Configuração opcional da IA Híbrida (Gemini API + Room Offline)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("IA Híbrida — Dra. Camila Nutrition", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Em Modo Avião (ou sem chave), a Dra. Camila usa o Motor Especialista Offline lendo seu plano real no Room. Para ativar também o Gemini 2.5 Flash quando houver internet, cole uma chave opcional abaixo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = onGeminiKeyChange,
                        label = { Text("Gemini API Key (opcional)") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = onSalvar,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salvar Alterações do Perfil", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onSair,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sair / Trocar de Conta", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IndicadorPerfil(valor: String, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilPreview() {
    TriHubTheme {
        PerfilContent(
            uiState = PerfilUiState(
                carregando = false,
                usuario = UsuarioEntity(id = 1, nome = "Alan", email = "alan@trihub.com", senhaHash = ""),
                refeicoesConcluidasHoje = 2,
                totalRefeicoesHoje = 4,
                aguaConsumidaHojeMl = 1500,
            ),
            nome = "Alan",
            bio = "Focado em baixar para 72 kg mantendo energia na corrida e musculação.",
            peso = "74,2",
            metaPeso = "72,0",
            altura = "176",
            metaCalorias = "2100",
            metaAgua = "2500",
            foco = FocoPrincipal.EMAGRECIMENTO,
            modalidades = setOf("Musculação", "Corrida"),
            nivel = NivelAtividade.MODERADO,
            restricoes = emptySet(),
            geminiKey = "",
            snackbarHostState = remember { SnackbarHostState() },
            onEscolherFoto = {}, onRemoverFoto = {},
            onNomeChange = {}, onBioChange = {}, onPesoChange = {}, onMetaPesoChange = {},
            onAlturaChange = {}, onMetaCaloriasChange = {}, onMetaAguaChange = {},
            onFocoChange = {}, onToggleModalidade = {}, onNivelChange = {}, onToggleRestricao = {},
            onGeminiKeyChange = {}, onSalvar = {}, onSair = {},
        )
    }
}
