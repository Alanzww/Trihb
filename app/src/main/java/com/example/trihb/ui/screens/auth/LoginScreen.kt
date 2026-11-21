package com.example.trihb.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trihb.R
import com.example.trihb.ui.AppViewModelProvider
import com.example.trihb.ui.theme.TriHubTheme

/** Rota Stateful da Tela de Login Padrão (E-mail e Senha + Criação de Conta). */
@Composable
fun LoginScreen(
    onLoginSucesso: () -> Unit,
    onIrParaRegistro: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    var manterConectado by rememberSaveable { mutableStateOf(true) }

    LoginContent(
        uiState = uiState,
        email = email,
        senha = senha,
        senhaVisivel = senhaVisivel,
        manterConectado = manterConectado,
        onEmailChange = {
            email = it
            viewModel.limparErro()
        },
        onSenhaChange = {
            senha = it
            viewModel.limparErro()
        },
        onToggleSenhaVisivel = { senhaVisivel = !senhaVisivel },
        onManterConectadoChange = { manterConectado = it },
        onEntrar = {
            viewModel.entrar(email, senha, manterConectado, aoSucesso = onLoginSucesso)
        },
        onIrParaRegistro = onIrParaRegistro,
    )
}

/** Conteúdo Stateless da Tela de Login. */
@Composable
fun LoginContent(
    uiState: AuthUiState,
    email: String,
    senha: String,
    senhaVisivel: Boolean,
    manterConectado: Boolean,
    onEmailChange: (String) -> Unit,
    onSenhaChange: (String) -> Unit,
    onToggleSenhaVisivel: () -> Unit,
    onManterConectadoChange: (Boolean) -> Unit,
    onEntrar: () -> Unit,
    onIrParaRegistro: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.trihb_logo),
                contentDescription = "Logotipo TriHub",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(160.dp),
            )

            Text(
                text = "Sua rotina alimentar, evolução física e comunidade em um só lugar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Card de Login com E-mail e Senha
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Acessar sua conta", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("E-mail") },
                        placeholder = { Text("exemplo@trihub.com") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = senha,
                        onValueChange = onSenhaChange,
                        label = { Text("Senha") },
                        placeholder = { Text("Digite sua senha") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = onToggleSenhaVisivel) {
                                Icon(
                                    imageVector = if (senhaVisivel) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                                )
                            }
                        },
                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = manterConectado,
                            onCheckedChange = onManterConectadoChange,
                        )
                        Text(
                            text = "Manter conectado neste aparelho",
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                        onClick = onEntrar,
                        enabled = !uiState.processando,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Entrar", style = MaterialTheme.typography.labelLarge)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedButton(
                        onClick = onIrParaRegistro,
                        enabled = !uiState.processando,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Criar nova conta", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    TriHubTheme {
        LoginContent(
            uiState = AuthUiState(carregando = false),
            email = "",
            senha = "",
            senhaVisivel = false,
            manterConectado = true,
            onEmailChange = {},
            onSenhaChange = {},
            onToggleSenhaVisivel = {},
            onManterConectadoChange = {},
            onEntrar = {},
            onIrParaRegistro = {},
        )
    }
}
