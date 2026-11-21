package com.example.trihb.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Empty state acolhedor: orienta a primeira ação em vez de exibir tela em branco.
 */
@Composable
fun EmptyState(
    icone: ImageVector,
    titulo: String,
    mensagem: String,
    modifier: Modifier = Modifier,
    acaoRotulo: String? = null,
    onAcao: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (acaoRotulo != null && onAcao != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAcao, shape = MaterialTheme.shapes.medium) { Text(acaoRotulo) }
        }
    }
}

/** Indicador discreto e tranquilizador exibido quando o aparelho está sem conexão. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Modo offline • suas alterações estão salvas no aparelho",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pílula/selo compacto (badges de meta, bônus e status). */
@Composable
fun Pilula(
    texto: String,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    corFundo: Color = MaterialTheme.colorScheme.primaryContainer,
    corConteudo: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = corFundo,
        contentColor = corConteudo,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icone != null) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Avatar circular com iniciais. */
@Composable
fun AvatarIniciais(
    nome: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 44.dp,
    corFundo: Color = MaterialTheme.colorScheme.primaryContainer,
    corTexto: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val iniciais = nome.split(" ", ".")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "U" }
    Box(
        modifier = modifier
            .size(tamanho)
            .background(corFundo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciais,
            style = if (tamanho >= 64.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            color = corTexto,
        )
    }
}

/**
 * Renderiza a foto de perfil salva localmente no armazenamento interno do app
 * (`filesDir/avatars/...`) ou, caso não exista, exibe o [AvatarIniciais].
 */
@Composable
fun AvatarFotoOuIniciais(
    nome: String,
    fotoUri: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 44.dp,
    corFundo: Color = MaterialTheme.colorScheme.primaryContainer,
    corTexto: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val bitmap = remember(fotoUri) {
        if (fotoUri.isNotBlank()) {
            runCatching {
                val arquivo = File(fotoUri)
                if (arquivo.exists()) BitmapFactory.decodeFile(arquivo.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Foto de perfil de $nome",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(tamanho)
                .clip(CircleShape),
        )
    } else {
        AvatarIniciais(
            nome = nome,
            tamanho = tamanho,
            corFundo = corFundo,
            corTexto = corTexto,
            modifier = modifier,
        )
    }
}
