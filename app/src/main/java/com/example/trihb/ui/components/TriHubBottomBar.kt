package com.example.trihb.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trihb.ui.navigation.BottomTab
import com.example.trihb.ui.theme.TriHubTheme

/** Barra inferior fixa com as 4 abas principais (stateless). */
@Composable
fun TriHubBottomBar(
    rotaAtual: String?,
    onTabSelecionada: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        BottomTab.entries.forEach { tab ->
            val selecionada = rotaAtual == tab.screen.route
            NavigationBarItem(
                selected = selecionada,
                onClick = { onTabSelecionada(tab) },
                icon = {
                    Icon(
                        imageVector = if (selecionada) tab.iconeSelecionado else tab.iconeNaoSelecionado,
                        contentDescription = "Aba ${tab.rotulo}",
                    )
                },
                label = { Text(tab.rotulo, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun TriHubBottomBarPreview() {
    TriHubTheme { TriHubBottomBar(rotaAtual = "dieta", onTabSelecionada = {}) }
}
