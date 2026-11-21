package com.example.trihb.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

/**
 * Entidade de usuário do banco local.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)],
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val email: String,
    val senhaHash: String,
    val fotoUri: String = "",
    val bio: String = "",
    val alturaCm: Float = 175f,
    val pesoAtualKg: Float = 74.2f,
    val metaPesoKg: Float = 72.0f,
    val metaCaloriasKcal: Int = 2100,
    val metaAguaMl: Int = 2500,
    val focoPrincipal: String = FocoPrincipal.EMAGRECIMENTO.codigo,
    val modalidades: String = "Musculação,Corrida",
    val nivelAtividade: String = NivelAtividade.MODERADO.codigo,
    val restricoesAlimentares: String = "",
    /** Campo JSON extensível para futuras configurações sem quebrar o schema SQLite. */
    val preferenciasJson: String = "{}",
    val criadoEm: Long = System.currentTimeMillis(),
) {
    val focoEnum: FocoPrincipal
        get() = FocoPrincipal.fromCodigo(focoPrincipal)

    val nivelEnum: NivelAtividade
        get() = NivelAtividade.fromCodigo(nivelAtividade)

    val listaModalidades: List<String>
        get() = modalidades.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val listaRestricoes: List<String>
        get() = restricoesAlimentares.split(",").map { it.trim() }.filter { it.isNotBlank() }

    /** Indica se o usuário já concluiu ou pulou o tutorial interativo inicial. */
    val tutorialConcluido: Boolean
        get() = preferenciasJson.contains("\"tutorialConcluido\":true")

    /** Cálculo automático do Índice de Massa Corporal (peso / altura²). */
    val imc: Float?
        get() = if (alturaCm in 50f..260f && pesoAtualKg in 20f..400f) {
            val alturaM = alturaCm / 100f
            pesoAtualKg / (alturaM * alturaM)
        } else null

    val classificacaoImc: String
        get() {
            val v = imc ?: return "Informe peso e altura"
            return when {
                v < 18.5f -> "Abaixo do peso"
                v < 25.0f -> "Faixa saudável"
                v < 30.0f -> "Sobrepeso"
                else -> "Obesidade"
            }
        }

    companion object {
        const val ID_DEMO_ALAN = 1
        const val ID_DEMO_MARINA = 2

        /** Gera hash SHA-256 determinístico com salt do e-mail normalizado. */
        fun gerarHashSenha(email: String, senha: String): String {
            val entrada = "${email.trim().lowercase()}::trihub_salt::${senha.trim()}"
            val bytes = MessageDigest.getInstance("SHA-256").digest(entrada.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

enum class FocoPrincipal(
    val codigo: String,
    val rotulo: String,
    val emoji: String,
    val metaCaloriasSugerida: Int,
) {
    EMAGRECIMENTO("EMAGRECIMENTO", "Emagrecimento", "🔥", 1950),
    HIPERTROFIA("HIPERTROFIA", "Hipertrofia", "💪", 2550),
    PERFORMANCE("PERFORMANCE", "Performance Esportiva", "⚡", 2400),
    REEDUCACAO("REEDUCACAO", "Reeducação Alimentar", "🥗", 2100),
    LONGEVIDADE("LONGEVIDADE", "Saúde & Longevidade", "🌿", 2050);

    val rotuloComEmoji: String get() = "$emoji $rotulo"

    companion object {
        fun fromCodigo(codigo: String): FocoPrincipal =
            entries.firstOrNull { it.codigo.equals(codigo, ignoreCase = true) } ?: EMAGRECIMENTO
    }
}

enum class ModalidadeEsportiva(val rotulo: String, val emoji: String) {
    MUSCULACAO("Musculação", "🏋️"),
    CORRIDA("Corrida", "🏃"),
    CICLISMO("Ciclismo", "🚴"),
    NATACAO("Natação", "🏊"),
    CROSSFIT("CrossFit", "🤸"),
    FUNCIONAL_YOGA("Yoga / Funcional", "🧘"),
    ARTES_MARCIAIS("Artes Marciais", "🥊"),
    CAMINHADA("Caminhada", "🚶");

    val rotuloComEmoji: String get() = "$emoji $rotulo"
}

enum class NivelAtividade(val codigo: String, val rotulo: String) {
    LEVE("LEVE", "Leve (1–2x/sem)"),
    MODERADO("MODERADO", "Moderado (3–4x/sem)"),
    INTENSO("INTENSO", "Intenso (5–6x/sem)"),
    ATLETA("ATLETA", "Atleta (Diário)");

    companion object {
        fun fromCodigo(codigo: String): NivelAtividade =
            entries.firstOrNull { it.codigo.equals(codigo, ignoreCase = true) } ?: MODERADO
    }
}

enum class RestricaoAlimentar(val rotulo: String, val emoji: String) {
    SEM_LACTOSE("Sem Lactose", "🥛"),
    SEM_GLUTEN("Sem Glúten", "🌾"),
    VEGETARIANO("Vegetariano", "🥦"),
    VEGANO("Vegano", "🌱"),
    LOW_CARB("Low Carb", "🥑");

    val rotuloComEmoji: String get() = "$emoji $rotulo"
}
