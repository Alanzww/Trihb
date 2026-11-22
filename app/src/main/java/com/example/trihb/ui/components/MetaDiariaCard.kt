package com.example.trihb.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trihb.ui.theme.TriHubTheme
import com.example.trihb.ui.theme.tabular
import com.example.trihb.util.DataHora

/**
 * Card stateless de Meta Diária: hidratação (+/−250 ml), progresso calórico e
 * bônus gerado em tempo real pelo acelerômetro.
 */
@Composable
fun MetaDiariaCard(
    consumidoMl: Int,
    metaAguaTotalMl: Int,
    bonusAguaMl: Int,
    progressoAgua: Float,
    caloriasConcluidas: Int,
    metaCaloriasTotal: Int,
    bonusCalorias: Int,
    progressoCalorias: Float,
    passos: Int,
    sensorAtivo: Boolean,
    sensorDisponivel: Boolean,
    onAdicionarAgua: () -> Unit,
    onRemoverAgua: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aguaAnimada by animateFloatAsState(progressoAgua, tween(600), label = "agua")
    val caloriasAnimadas by animateFloatAsState(progressoCalorias, tween(600), label = "calorias")

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabeçalho + status do sensor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Meta Diária",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                SensorStatusPill(passos = passos, ativo = sensorAtivo, disponivel = sensorDisponivel)
            }

            // Hidratação -------------------------------------------------------
            MetricaLinha(
                icone = Icons.Rounded.WaterDrop,
                corIcone = MaterialTheme.colorScheme.secondary,
                fundoIcone = MaterialTheme.colorScheme.secondaryContainer,
                titulo = "Hidratação",
                valor = "${DataHora.formatarInteiro(consumidoMl)} / ${DataHora.formatarInteiro(metaAguaTotalMl)} ml",
            )
            LinearProgressIndicator(
                progress = { aguaAnimada },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 8.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Progresso de hidratação ${(progressoAgua * 100).toInt()}%" },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            AnimatedVisibility(visible = bonusAguaMl > 0) {
                Pilula(
                    texto = "+$bonusAguaMl ml de bônus pela atividade",
                    icone = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    corFundo = MaterialTheme.colorScheme.secondaryContainer,
                    corConteudo = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRemoverAgua,
                    enabled = consumidoMl > 0,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("250 ml")
                }
                Button(
                    onClick = onAdicionarAgua,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("250 ml")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Calorias ---------------------------------------------------------
            MetricaLinha(
                icone = Icons.Rounded.LocalFireDepartment,
                corIcone = MaterialTheme.colorScheme.tertiary,
                fundoIcone = MaterialTheme.colorScheme.tertiaryContainer,
                titulo = "Calorias",
                valor = "${DataHora.formatarInteiro(caloriasConcluidas)} / ${DataHora.formatarInteiro(metaCaloriasTotal)} kcal",
            )
            LinearProgressIndicator(
                progress = { caloriasAnimadas },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 8.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Progresso calórico ${(progressoCalorias * 100).toInt()}%" },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            AnimatedVisibility(visible = bonusCalorias > 0) {
                Pilula(
                    texto = "+$bonusCalorias kcal liberadas pelos seus passos",
                    icone = Icons.Rounded.LocalFireDepartment,
                    corFundo = MaterialTheme.colorScheme.tertiaryContainer,
                    corConteudo = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MetricaLinha(
    icone: ImageVector,
    corIcone: Color,
    fundoIcone: Color,
    titulo: String,
    valor: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(fundoIcone, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleSmall.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Indicador do sensor de atividade (passos ao vivo). */
@Composable
fun SensorStatusPill(passos: Int, ativo: Boolean, disponivel: Boolean, modifier: Modifier = Modifier) {
    val texto = when {
        !disponivel -> "Sem sensor • $passos passos"
        ativo -> "Sensor ativo • $passos passos"
        else -> "Sensor pausado • $passos passos"
    }
    Pilula(
        texto = texto,
        icone = Icons.AutoMirrored.Rounded.DirectionsWalk,
        corFundo = if (ativo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        corConteudo = if (ativo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MetaDiariaCardPreview() {
    TriHubTheme {
        MetaDiariaCard(
            consumidoMl = 1500, metaAguaTotalMl = 2550, bonusAguaMl = 50, progressoAgua = 0.59f,
            caloriasConcluidas = 420, metaCaloriasTotal = 2130, bonusCalorias = 30, progressoCalorias = 0.2f,
            passos = 45, sensorAtivo = true, sensorDisponivel = true,
            onAdicionarAgua = {}, onRemoverAgua = {},
        )
    }
}
