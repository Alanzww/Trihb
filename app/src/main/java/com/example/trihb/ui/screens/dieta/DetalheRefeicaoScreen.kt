package com.example.trihb.ui.screens.dieta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.util.DataHora

/** Estado do formulário de refeição (imutável, passado ao conteúdo stateless). */
data class RefeicaoFormState(
    val horario: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val calorias: String = "",
    val observacao: String = "",
    val concluida: Boolean = false,
) {
    val erroHorario: String? get() = if (DataHora.horarioValido(horario)) null else "Use o formato HH:mm (ex.: 07:30)"
    val erroTitulo: String? get() = if (titulo.isBlank()) "Informe um título" else null
    val caloriasInt: Int? get() = calorias.trim().toIntOrNull()
    val erroCalorias: String?
        get() = when (val c = caloriasInt) {
            null -> "Informe um número inteiro de kcal"
            !in 1..5000 -> "Valor entre 1 e 5000 kcal"
            else -> null
        }
    val valido: Boolean get() = erroHorario == null && erroTitulo == null && erroCalorias == null
}

/**
 * Rota Stateful da tela de Criação/Edição/Exclusão.
 * refeicaoId == -1 → Nova Refeição (Create) · refeicaoId > 0 → Editar/Excluir.
 */
@Composable
fun DetalheRefeicaoScreen(
    refeicaoId: Int,
    onVoltar: () -> Unit,
    viewModel: DietaViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val modoEdicao = refeicaoId > 0

    var horario by rememberSaveable { mutableStateOf(if (modoEdicao) "" else DataHora.horaAtual()) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var calorias by rememberSaveable { mutableStateOf("") }
    var observacao by rememberSaveable { mutableStateOf("") }
    var concluida by rememberSaveable { mutableStateOf(false) }
    var carregado by rememberSaveable { mutableStateOf(!modoEdicao) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }
    var salvando by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    // READ: carrega a refeição uma única vez (sobrevive à rotação via rememberSaveable)
    LaunchedEffect(refeicaoId) {
        if (modoEdicao && !carregado) {
            val refeicao = viewModel.carregarRefeicao(refeicaoId)
            if (refeicao == null) {
                onVoltar()
                return@LaunchedEffect
            }
            horario = refeicao.horario
            titulo = refeicao.titulo
            descricao = refeicao.descricao
            calorias = refeicao.calorias.toString()
            observacao = refeicao.observacao
            concluida = refeicao.concluida
            carregado = true
        }
    }

    val form = RefeicaoFormState(horario, titulo, descricao, calorias, observacao, concluida)

    DetalheRefeicaoContent(
        modoEdicao = modoEdicao,
        carregado = carregado,
        form = form,
        mostrarErros = tentouSalvar,
        salvando = salvando,
        onHorarioChange = { horario = it.take(5) },
        onTituloChange = { titulo = it },
        onDescricaoChange = { descricao = it },
        onCaloriasChange = { novo -> calorias = novo.filter(Char::isDigit).take(4) },
        onObservacaoChange = { observacao = it },
        onConcluidaChange = { concluida = it },
        onVoltar = onVoltar,
        onSalvar = {
            tentouSalvar = true
            if (form.valido && !salvando) {
                salvando = true
                viewModel.salvarRefeicao(
                    id = refeicaoId,
                    horario = form.horario,
                    titulo = form.titulo,
                    descricao = form.descricao,
                    calorias = form.caloriasInt ?: 0,
                    observacao = form.observacao,
                    concluida = form.concluida,
                    aoConcluir = onVoltar,
                )
            }
        },
        onExcluir = { confirmarExclusao = true },
    )

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Excluir refeição?") },
            text = { Text("\"${form.titulo}\" será removida do seu cronograma. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        salvando = true
                        viewModel.excluirRefeicaoPorId(refeicaoId, aoConcluir = onVoltar)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheRefeicaoContent(
    modoEdicao: Boolean,
    carregado: Boolean,
    form: RefeicaoFormState,
    mostrarErros: Boolean,
    salvando: Boolean,
    onHorarioChange: (String) -> Unit,
    onTituloChange: (String) -> Unit,
    onDescricaoChange: (String) -> Unit,
    onCaloriasChange: (String) -> Unit,
    onObservacaoChange: (String) -> Unit,
    onConcluidaChange: (Boolean) -> Unit,
    onVoltar: () -> Unit,
    onSalvar: () -> Unit,
    onExcluir: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (modoEdicao) "Editar Refeição" else "Nova Refeição",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        if (!carregado) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoTexto(
                            valor = form.horario,
                            onChange = onHorarioChange,
                            rotulo = "Horário",
                            placeholder = "07:30",
                            erro = form.erroHorario.takeIf { mostrarErros },
                            teclado = KeyboardType.Text,
                            modifier = Modifier.weight(1f),
                        )
                        CampoTexto(
                            valor = form.calorias,
                            onChange = onCaloriasChange,
                            rotulo = "Calorias (kcal)",
                            placeholder = "420",
                            erro = form.erroCalorias.takeIf { mostrarErros },
                            teclado = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CampoTexto(
                        valor = form.titulo,
                        onChange = onTituloChange,
                        rotulo = "Título",
                        placeholder = "Café da Manhã",
                        erro = form.erroTitulo.takeIf { mostrarErros },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CampoTexto(
                        valor = form.descricao,
                        onChange = onDescricaoChange,
                        rotulo = "Descrição",
                        placeholder = "3 ovos mexidos + 1 fatia de pão integral + café",
                        linhasMin = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CampoTexto(
                        valor = form.observacao,
                        onChange = onObservacaoChange,
                        rotulo = "Observação (opcional)",
                        placeholder = "Substituição aprovada pela nutri",
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = form.concluida, onCheckedChange = onConcluidaChange)
                        Text("Refeição concluída", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Button(
                onClick = onSalvar,
                enabled = !salvando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salvar", style = MaterialTheme.typography.labelLarge)
            }

            if (modoEdicao) {
                OutlinedButton(
                    onClick = onExcluir,
                    enabled = !salvando,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Excluir Refeição", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    erro: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    linhasMin: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = linhasMin == 1,
        minLines = linhasMin,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            imeAction = imeAction,
            capitalization = if (teclado == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun DetalheRefeicaoPreview() {
    TriHubTheme {
        DetalheRefeicaoContent(
            modoEdicao = true, carregado = true,
            form = RefeicaoFormState("12:30", "Almoço", "150g frango grelhado + 100g arroz", "580"),
            mostrarErros = false, salvando = false,
            onHorarioChange = {}, onTituloChange = {}, onDescricaoChange = {}, onCaloriasChange = {},
            onObservacaoChange = {}, onConcluidaChange = {}, onVoltar = {}, onSalvar = {}, onExcluir = {},
        )
    }
}
