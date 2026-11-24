package com.example.trihb.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

/** Rotas do NavHost (9 destinos). */
sealed class Screen(val route: String) {
    data object IntroSplash : Screen("intro_splash")
    data object Login : Screen("login")
    data object Registro : Screen("registro")
    data object Dieta : Screen("dieta")
    data object DetalheRefeicao : Screen("detalhe_refeicao/{$ARG_REFEICAO_ID}") {
        /** -1 = Nova Refeição (Create) · >0 = Editar/Excluir (Read, Update, Delete). */
        fun createRoute(refeicaoId: Int) = "detalhe_refeicao/$refeicaoId"
    }
    data object Progresso : Screen("progresso")
    data object Feed : Screen("feed")
    data object Chat : Screen("chat")
    data object Perfil : Screen("perfil")

    companion object {
        const val ARG_REFEICAO_ID = "refeicaoId"
        const val NOVA_REFEICAO_ID = -1
    }
}

/** As 5 abas fixas da barra inferior. */
enum class BottomTab(
    val screen: Screen,
    val rotulo: String,
    val iconeSelecionado: ImageVector,
    val iconeNaoSelecionado: ImageVector,
) {
    DIETA(Screen.Dieta, "Dieta", Icons.Rounded.Restaurant, Icons.Outlined.Restaurant),
    PROGRESSO(Screen.Progresso, "Progresso", Icons.AutoMirrored.Rounded.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
    FEED(Screen.Feed, "Feed", Icons.Rounded.Groups, Icons.Outlined.Groups),
    NUTRI(Screen.Chat, "Nutri", Icons.AutoMirrored.Rounded.Chat, Icons.AutoMirrored.Outlined.Chat),
    PERFIL(Screen.Perfil, "Perfil", Icons.Rounded.Person, Icons.Outlined.Person),
}
