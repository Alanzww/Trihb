package com.example.trihb.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utilidades de data/hora compatíveis com minSdk 24 (sem desugaring). */
object DataHora {
    private val localeBr: Locale = Locale.forLanguageTag("pt-BR")

    fun horaAtual(): String = SimpleDateFormat("HH:mm", localeBr).format(Date())

    fun dataAtual(): String = SimpleDateFormat("dd/MM/yyyy", localeBr).format(Date())

    /** Valida horários no formato HH:mm (00:00–23:59). */
    fun horarioValido(valor: String): Boolean =
        Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(valor.trim())

    /** Converte "74,2" ou "74.2" em Float. */
    fun parseDecimal(valor: String): Float? = valor.trim().replace(',', '.').toFloatOrNull()

    fun formatarDecimal(valor: Float, casas: Int = 1): String =
        String.format(localeBr, "%.${casas}f", valor)

    /** 1500 → "1.500" */
    fun formatarInteiro(valor: Int): String = String.format(localeBr, "%,d", valor)
}
