package com.example.trihb.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Escala tipográfica TriHub — prioriza escaneabilidade em listas de refeições,
 * métricas corporais, feed e chat. Fonte do sistema (SansSerif).
 */
private val TriFont = FontFamily.SansSerif

val Typography = Typography(
    // Saudação inicial e títulos de seção ("Olá, Alan!", "Meu Progresso")
    headlineMedium = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // Títulos de refeições, nomes no feed/chat, cabeçalhos de cards
    titleMedium = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Descrição de refeições, relatos do feed, mensagens do chat
    bodyLarge = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // Subtítulos, observações e textos auxiliares
    bodyMedium = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Rótulos da NavigationBar, botões, badges e horários
    labelLarge = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = TriFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/**
 * Estilo numérico com dígitos tabulares ("tnum") para contadores em tempo real
 * (água, calorias, passos, peso, horários) — evita trepidação visual ao atualizar.
 */
fun TextStyle.tabular(): TextStyle = this.copy(fontFeatureSettings = "tnum")
