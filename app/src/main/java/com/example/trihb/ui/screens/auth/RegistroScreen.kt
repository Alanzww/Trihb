package com.example.trihb.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.util.DataHora

/** Estado imutável de validação do formulário de registro. */
data class RegistroFormState(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val bio: String = "",
    val peso: String = "74,0",
    val altura: String = "175",
    val metaPeso: String = "70,0",
    val foco: FocoPrincipal = FocoPrincipal.EMAGRECIMENTO,
    val modalidades: Set<String> = setOf(ModalidadeEsportiva.MUSCULACAO.rotulo, ModalidadeEsportiva.CORRIDA.rotulo),
    val nivel: NivelAtividade = NivelAtividade.MODERADO,
    val restricoes: Set<String> = emptySet(),
) {
    val erroNome: String? get() = if (nome.trim().length < 2) "Informe seu nome" else null
    val erroEmail: String?
        get() = if (!email.contains("@") || !email.contains(".")) "Informe um e-mail válido" else null
    val erroSenha: String?
        get() = if (senha.length < 4) "Mínimo de 4 caracteres" else null
    val pesoKg: Float? get() = DataHora.parseDecimal(peso)?.takeIf { it in 25f..350f }
    val alturaCm: Float? get() = DataHora.parseDecimal(altura)?.takeIf { it in 100f..250f }
    val metaPesoKg: Float? get() = DataHora.parseDecimal(metaPeso)?.takeIf { it in 25f..350f }
    val erroMedidas: String?
        get() = if (pesoKg == null || alturaCm == null || metaPesoKg == null) {
            "Verifique peso, altura (cm) e meta de peso"
        } else null
    val valido: Boolean
        get() = erroNome == null && erroEmail == null && erroSenha == null && erroMedidas == null
}

/** Rota Stateful da Tela de Registro e Onboarding Personalizado. */
@Composable
fun RegistroScreen(
    onRegistroSucesso: () -> Unit,
    onVoltarLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var peso by rememberSaveable { mutableStateOf("74,0") }
    var altura by rememberSaveable { mutableStateOf("175") }
    var metaPeso by rememberSaveable { mutableStateOf("70,0") }
    var focoCodigo by rememberSaveable { mutableStateOf(FocoPrincipal.EMAGRECIMENTO.codigo) }
    var modalidadesCsv by rememberSaveable { mutableStateOf("Musculação,Corrida") }
    var nivelCodigo by rememberSaveable { mutableStateOf(NivelAtividade.MODERADO.codigo) }
    var restricoesCsv by rememberSaveable { mutableStateOf("") }
    var tentouRegistrar by rememberSaveable { mutableStateOf(false) }

    val form = RegistroFormState(
        nome = nome,
        email = email,
        senha = senha,
        bio = bio,
        peso = peso,
        altura = altura,
        metaPeso = metaPeso,
        foco = FocoPrincipal.fromCodigo(focoCodigo),
        modalidades = modalidadesCsv.split(",").filter { it.isNotBlank() }.toSet(),
        nivel = NivelAtividade.fromCodigo(nivelCodigo),
        restricoes = restricoesCsv.split(",").filter { it.isNotBlank() }.toSet(),
    )

    RegistroContent(
        uiState = uiState,
        form = form,
        mostrarErros = tentouRegistrar,
        onNomeChange = { nome = it; viewModel.limparErro() },
        onEmailChange = { email = it; viewModel.limparErro() },
        onSenhaChange = { senha = it; viewModel.limparErro() },
        onBioChange = { bio = it },
        onPesoChange = { peso = it },
        onAlturaChange = { altura = it },
        onMetaPesoChange = { metaPeso = it },
        onFocoChange = { focoCodigo = it.codigo },
        onToggleModalidade = { mod ->
            val atual = form.modalidades.toMutableSet()
            if (!atual.add(mod)) atual.remove(mod)
            modalidadesCsv = atual.joinToString(",")
        },
        onNivelChange = { nivelCodigo = it.codigo },
        onToggleRestricao = { rest ->
            val atual = form.restricoes.toMutableSet()
            if (!atual.add(rest)) atual.remove(rest)
            restricoesCsv = atual.joinToString(",")
        },
        onVoltar = onVoltarLogin,
        onConcluirRegistro = {
            tentouRegistrar = true
            if (form.valido) {
                viewModel.registrar(
                    nome = form.nome,
                    email = form.email,
                    senha = form.senha,
                    bio = form.bio,
                    alturaCm = form.alturaCm!!,
                    pesoAtualKg = form.pesoKg!!,
                    metaPesoKg = form.metaPesoKg!!,
                    focoPrincipal = form.foco,
                    modalidades = form.modalidades.toList(),
                    nivelAtividade = form.nivel,
                    restricoesAlimentares = form.restricoes.toList(),
                    aoSucesso = onRegistroSucesso,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistroContent(
    uiState: AuthUiState,
    form: RegistroFormState,
    mostrarErros: Boolean,
    onNomeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSenhaChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onAlturaChange: (String) -> Unit,
    onMetaPesoChange: (String) -> Unit,
    onFocoChange: (FocoPrincipal) -> Unit,
    onToggleModalidade: (String) -> Unit,
    onNivelChange: (NivelAtividade) -> Unit,
    onToggleRestricao: (String) -> Unit,
    onVoltar: () -> Unit,
    onConcluirRegistro: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Criar Conta no TriHub", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar ao Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Credenciais e Bio
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Dados da conta", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = form.nome,
                        onValueChange = onNomeChange,
                        label = { Text("Nome") },
                        placeholder = { Text("Ex.: Alan") },
                        isError = mostrarErros && form.erroNome != null,
                        supportingText = form.erroNome.takeIf { mostrarErros }?.let { { Text(it) } },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = form.email,
                        onValueChange = onEmailChange,
                        label = { Text("E-mail") },
                        placeholder = { Text("voce@trihub.com") },
                        isError = mostrarErros && form.erroEmail != null,
                        supportingText = form.erroEmail.takeIf { mostrarErros }?.let { { Text(it) } },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = form.senha,
                        onValueChange = onSenhaChange,
                        label = { Text("Senha") },
                        isError = mostrarErros && form.erroSenha != null,
                        supportingText = form.erroSenha.takeIf { mostrarErros }?.let { { Text(it) } },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = form.bio,
                        onValueChange = onBioChange,
                        label = { Text("Descrição / Bio (opcional)") },
                        placeholder = { Text("Ex.: Corredor e praticante de musculação focado em constância") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 2. Medidas Corporais
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Medidas e meta corporal", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.peso,
                            onValueChange = onPesoChange,
                            label = { Text("Peso (kg)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.altura,
                            onValueChange = onAlturaChange,
                            label = { Text("Altura (cm)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.metaPeso,
                            onValueChange = onMetaPesoChange,
                            label = { Text("Meta (kg)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (mostrarErros && form.erroMedidas != null) {
                        Text(
                            text = form.erroMedidas.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // 3. Foco Principal + Modalidades + Restrições
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("3. Foco principal e esportes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "A Dra. Camila usará seu foco para montar suas 4 refeições iniciais:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FocoPrincipal.entries.forEach { item ->
                            val selecionado = form.foco == item
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

                    Text("Modalidades praticadas:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ModalidadeEsportiva.entries.forEach { mod ->
                            val selecionada = mod.rotulo in form.modalidades
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

                    Text("Nível de atividade:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NivelAtividade.entries.forEach { niv ->
                            val selecionado = form.nivel == niv
                            FilterChip(
                                selected = selecionado,
                                onClick = { onNivelChange(niv) },
                                label = { Text(niv.rotulo) },
                            )
                        }
                    }

                    Text("Restrições ou preferências alimentares (opcional):", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RestricaoAlimentar.entries.forEach { rest ->
                            val selecionada = rest.rotulo in form.restricoes
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

            AnimatedVisibility(visible = uiState.erroMensagem != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.erroMensagem.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Button(
                onClick = onConcluirRegistro,
                enabled = !uiState.processando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Icon(Icons.Rounded.HowToReg, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Criar Conta e Gerar Meu Plano", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistroPreview() {
    TriHubTheme {
        RegistroContent(
            uiState = AuthUiState(carregando = false),
            form = RegistroFormState(),
            mostrarErros = false,
            onNomeChange = {}, onEmailChange = {}, onSenhaChange = {}, onBioChange = {},
            onPesoChange = {}, onAlturaChange = {}, onMetaPesoChange = {},
            onFocoChange = {}, onToggleModalidade = {}, onNivelChange = {}, onToggleRestricao = {},
            onVoltar = {}, onConcluirRegistro = {},
        )
    }
}
