package com.example.trihb.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.ActivitySensorManager
import com.example.trihb.ui.components.OfflineBanner
import com.example.trihb.ui.components.TriHubBottomBar
import com.example.trihb.ui.components.TutorialOverlay
import com.example.trihb.ui.screens.auth.LoginScreen
import com.example.trihb.ui.screens.auth.RegistroScreen
import com.example.trihb.ui.screens.chat.ChatNutriScreen
import com.example.trihb.ui.screens.dieta.DetalheRefeicaoScreen
import com.example.trihb.ui.screens.dieta.DietaScreen
import com.example.trihb.ui.screens.feed.FeedScreen
import com.example.trihb.ui.screens.intro.IntroSplashScreen
import com.example.trihb.ui.screens.perfil.PerfilScreen
import com.example.trihb.ui.screens.progresso.ProgressoScreen
import kotlinx.coroutines.launch

/**
 * Grafo principal de navegação do app e controle do ciclo de vida do acelerômetro.
 */
@Composable
fun TriHubNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember(context) { ActivitySensorManager.getInstance(context) }
    val repository = remember(context) { TriHubRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val usuarioAtivo by repository.usuarioAtivo.collectAsStateWithLifecycle(initialValue = null)
    var tutorialAbertoManualmente by rememberSaveable { mutableStateOf(false) }

    // Liga o sensor quando a tela fica ativa e desliga em segundo plano
    DisposableEffect(lifecycleOwner, sensorManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensorManager.iniciar()
                Lifecycle.Event.ON_PAUSE -> sensorManager.parar()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.parar()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route
    val rotasComBottomBar = remember { BottomTab.entries.map { it.screen.route }.toSet() }
    val exibirBottomBar = rotaAtual in rotasComBottomBar
    val estaOnline by rememberConectividadeState(context)

    // Abre o tutorial automaticamente para novos usuários (ou contas que ainda não viram o tutorial)
    LaunchedEffect(usuarioAtivo?.id, usuarioAtivo?.tutorialConcluido, exibirBottomBar) {
        val u = usuarioAtivo
        if (exibirBottomBar && u != null && !u.tutorialConcluido) {
            tutorialAbertoManualmente = true
        }
    }

    fun navegarParaAba(screen: Screen) {
        if (rotaAtual != screen.route) {
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun fecharTutorial() {
        tutorialAbertoManualmente = false
        scope.launch {
            repository.marcarTutorialConcluido()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (exibirBottomBar) {
                    TriHubBottomBar(
                        rotaAtual = rotaAtual,
                        onTabSelecionada = { tab -> navegarParaAba(tab.screen) },
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                AnimatedVisibility(visible = exibirBottomBar && !estaOnline) {
                    OfflineBanner()
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.IntroSplash.route,
                    modifier = Modifier.weight(1f),
                ) {
                    // 1. IntroSplash -> verifica sessão ativa no Room
                    composable(route = Screen.IntroSplash.route) {
                        IntroSplashScreen(
                            onIntroConcluida = {
                                scope.launch {
                                    val sessao = repository.buscarSessao()
                                    val destino = if (sessao?.usuarioAtivoId != null && sessao.manterConectado) {
                                        Screen.Dieta.route
                                    } else {
                                        Screen.Login.route
                                    }
                                    navController.navigate(destino) {
                                        popUpTo(Screen.IntroSplash.route) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }

                    // 2. Login Padrão
                    composable(route = Screen.Login.route) {
                        LoginScreen(
                            onLoginSucesso = {
                                navController.navigate(Screen.Dieta.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onIrParaRegistro = {
                                navController.navigate(Screen.Registro.route)
                            },
                        )
                    }

                    // 3. Registro e Onboarding Personalizado (inicia o tutorial ao concluir)
                    composable(route = Screen.Registro.route) {
                        RegistroScreen(
                            onRegistroSucesso = {
                                tutorialAbertoManualmente = true
                                navController.navigate(Screen.Dieta.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onVoltarLogin = { navController.popBackStack() },
                        )
                    }

                    // 4. Aba 1 — Dieta
                    composable(route = Screen.Dieta.route) {
                        DietaScreen(
                            onNovaRefeicao = {
                                navController.navigate(
                                    Screen.DetalheRefeicao.createRoute(Screen.NOVA_REFEICAO_ID),
                                )
                            },
                            onEditarRefeicao = { id ->
                                navController.navigate(Screen.DetalheRefeicao.createRoute(id))
                            },
                            onAbrirPerfil = { navegarParaAba(Screen.Perfil) },
                            onAbrirTutorial = { tutorialAbertoManualmente = true },
                        )
                    }

                    // 5. Detalhe de Refeição (passagem tipada de refeicaoId: Int)
                    composable(
                        route = Screen.DetalheRefeicao.route,
                        arguments = listOf(
                            navArgument(Screen.ARG_REFEICAO_ID) {
                                type = NavType.IntType
                                defaultValue = Screen.NOVA_REFEICAO_ID
                            },
                        ),
                    ) { entry ->
                        val refeicaoId = entry.arguments?.getInt(Screen.ARG_REFEICAO_ID)
                            ?: Screen.NOVA_REFEICAO_ID
                        DetalheRefeicaoScreen(
                            refeicaoId = refeicaoId,
                            onVoltar = { navController.popBackStack() },
                        )
                    }

                    // 6. Aba 2 — Progresso
                    composable(route = Screen.Progresso.route) {
                        ProgressoScreen()
                    }

                    // 7. Aba 3 — Feed
                    composable(route = Screen.Feed.route) {
                        FeedScreen()
                    }

                    // 8. Aba 4 — Nutri (Chat Híbrido)
                    composable(route = Screen.Chat.route) {
                        ChatNutriScreen()
                    }

                    // 9. Aba 5 — Perfil Completo
                    composable(route = Screen.Perfil.route) {
                        PerfilScreen(
                            onLogout = {
                                tutorialAbertoManualmente = false
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }

        // Overlay do Tutorial Interativo (cobre a tela atual e troca a aba ao fundo)
        if (exibirBottomBar && tutorialAbertoManualmente) {
            TutorialOverlay(
                onMudarAbaAlvo = { screenAlvo -> navegarParaAba(screenAlvo) },
                onConcluirOuPular = { fecharTutorial() },
            )
        }
    }
}

/** Observa o estado de rede para exibir o banner discreto de Modo Offline. */
@Composable
private fun rememberConectividadeState(context: Context): State<Boolean> {
    return produceState(initialValue = verificarOnline(context), context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            value = true
            return@produceState
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }

            override fun onLost(network: Network) {
                value = verificarOnline(context)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        awaitDispose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
}

private fun verificarOnline(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val rede = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(rede) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(true)
