package com.example.trihb.data.ai

import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.MensagemEntity
import com.example.trihb.data.local.entity.RefeicaoEntity
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.local.entity.UsuarioResumoRelation
import com.example.trihb.sensor.BonusAtividade
import com.example.trihb.util.DataHora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Snapshot em tempo real do usuário logado extraído do Room + Acelerômetro,
 * compartilhado entre o motor Gemini Online e o motor Especialista Offline.
 */
data class NutriUserContext(
    val nome: String,
    val foco: FocoPrincipal,
    val modalidades: List<String>,
    val restricoes: List<String>,
    val pesoAtualKg: Float,
    val metaPesoKg: Float,
    val alturaCm: Float,
    val imc: Float?,
    val classificacaoImc: String,
    val variacaoPesoKg: Float?,
    val refeicoesConcluidas: List<RefeicaoEntity>,
    val refeicoesPendentes: List<RefeicaoEntity>,
    val caloriasConsumidas: Int,
    val metaCaloriasTotal: Int,
    val bonusCaloriasPassos: Int,
    val aguaConsumidaMl: Int,
    val metaAguaTotalMl: Int,
    val bonusAguaPassosMl: Int,
    val passosHoje: Int,
) {
    val saldoCalorias: Int get() = (metaCaloriasTotal - caloriasConsumidas).coerceAtLeast(0)
    val faltaAguaMl: Int get() = (metaAguaTotalMl - aguaConsumidaMl).coerceAtLeast(0)
    val coposAguaRestantes: Int get() = ceil(faltaAguaMl / 250.0).toInt()
    val semLactose: Boolean get() = restricoes.any { it.contains("Lactose", ignoreCase = true) || it.contains("Vegano", ignoreCase = true) }
    val semGluten: Boolean get() = restricoes.any { it.contains("Glúten", ignoreCase = true) || it.contains("Gluten", ignoreCase = true) }
    val vegetarianoOuVegano: Boolean get() = restricoes.any { it.contains("Veg", ignoreCase = true) }

    companion object {
        fun fromResumo(resumo: UsuarioResumoRelation?, passos: Int): NutriUserContext {
            val u = resumo?.usuario ?: UsuarioEntity(
                id = UsuarioEntity.ID_DEMO_ALAN,
                nome = "Alan",
                email = "alan@trihub.com",
                senhaHash = "",
            )
            val bonus = BonusAtividade(passos)
            val refeicoes = resumo?.refeicoes.orEmpty().sortedBy { it.horario }
            val concluidas = refeicoes.filter { it.concluida }
            val pendentes = refeicoes.filterNot { it.concluida }
            val calConsumidas = concluidas.sumOf { it.calorias }
            val metaAguaBase = resumo?.hidratacao?.metaBaseMl ?: u.metaAguaMl
            val aguaConsumida = resumo?.hidratacao?.consumidoMl ?: 0
            val progressos = resumo?.progressos.orEmpty()
            val pesoAtual = progressos.lastOrNull()?.pesoKg ?: u.pesoAtualKg
            val variacao = if (progressos.size >= 2) {
                progressos.last().pesoKg - progressos.first().pesoKg
            } else null

            return NutriUserContext(
                nome = u.nome,
                foco = u.focoEnum,
                modalidades = u.listaModalidades,
                restricoes = u.listaRestricoes,
                pesoAtualKg = pesoAtual,
                metaPesoKg = u.metaPesoKg,
                alturaCm = u.alturaCm,
                imc = u.copy(pesoAtualKg = pesoAtual).imc,
                classificacaoImc = u.copy(pesoAtualKg = pesoAtual).classificacaoImc,
                variacaoPesoKg = variacao,
                refeicoesConcluidas = concluidas,
                refeicoesPendentes = pendentes,
                caloriasConsumidas = calConsumidas,
                metaCaloriasTotal = u.metaCaloriasKcal + bonus.bonusCalorias,
                bonusCaloriasPassos = bonus.bonusCalorias,
                aguaConsumidaMl = aguaConsumida,
                metaAguaTotalMl = metaAguaBase + bonus.bonusAguaMl,
                bonusAguaPassosMl = bonus.bonusAguaMl,
                passosHoje = passos,
            )
        }
    }
}

data class NutriResposta(
    val texto: String,
    val fonte: String,
)

/**
 * Serviço de respostas da Dra. Camila.
 * Tenta usar a API do Gemini se houver chave configurada e conexão;
 * caso contrário, monta a resposta localmente com base nos dados do usuário no Room.
 */
object NutriAiService {

    private const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun responder(
        pergunta: String,
        contexto: NutriUserContext,
        historicoRecente: List<MensagemEntity> = emptyList(),
        geminiApiKey: String = "",
    ): NutriResposta {
        val chave = geminiApiKey.trim()
        if (chave.isNotEmpty()) {
            val respostaNuvem = runCatching {
                chamarGeminiOnline(pergunta, contexto, historicoRecente, chave)
            }.getOrNull()
            if (!respostaNuvem.isNullOrBlank()) {
                return NutriResposta(
                    texto = respostaNuvem.trim(),
                    fonte = MensagemEntity.FONTE_GEMINI,
                )
            }
        }
        return NutriResposta(
            texto = gerarRespostaContextualOffline(pergunta, contexto),
            fonte = MensagemEntity.FONTE_LOCAL_ROOM,
        )
    }

    private suspend fun chamarGeminiOnline(
        pergunta: String,
        ctx: NutriUserContext,
        historico: List<MensagemEntity>,
        apiKey: String,
    ): String? = withContext(Dispatchers.IO) {
        val url = URL("$GEMINI_ENDPOINT?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7_000
            readTimeout = 9_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        try {
            val payload = JSONObject().apply {
                put(
                    "system_instruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", construirSystemPrompt(ctx))),
                    ),
                )
                val contents = JSONArray()
                historico.takeLast(6).forEach { msg ->
                    contents.put(
                        JSONObject().apply {
                            put("role", if (msg.isDaNutri) "model" else "user")
                            put("parts", JSONArray().put(JSONObject().put("text", msg.texto)))
                        },
                    )
                }
                contents.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", pergunta)))
                    },
                )
                put("contents", contents)
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.5)
                        put("maxOutputTokens", 320)
                    },
                )
            }

            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        } finally {
            conn.disconnect()
        }
    }

    private fun construirSystemPrompt(ctx: NutriUserContext): String {
        val pendentes = ctx.refeicoesPendentes.joinToString("; ") {
            "${it.horario} ${it.titulo} (${it.calorias} kcal: ${it.descricao})"
        }.ifBlank { "Todas as refeições de hoje foram concluídas" }
        val concluidas = ctx.refeicoesConcluidas.joinToString("; ") {
            "${it.titulo} (${it.calorias} kcal)"
        }.ifBlank { "Nenhuma concluída ainda" }

        return """
            Você é a Dra. Camila Nutrition, nutricionista esportiva e clínica do aplicativo TriHub.
            Responda em português brasileiro, de forma acolhedora, prática e concisa (máximo de 3 a 4 frases).
            Use sempre os dados reais do paciente abaixo:
            - Paciente: ${ctx.nome} | Foco: ${ctx.foco.rotulo}
            - Modalidades: ${ctx.modalidades.joinToString(", ").ifBlank { "Não informadas" }}
            - Restrições alimentares: ${ctx.restricoes.joinToString(", ").ifBlank { "Nenhuma" }}
            - Peso atual: ${DataHora.formatarDecimal(ctx.pesoAtualKg)} kg | Meta: ${DataHora.formatarDecimal(ctx.metaPesoKg)} kg | IMC: ${ctx.imc?.let { DataHora.formatarDecimal(it) } ?: "-"} (${ctx.classificacaoImc})
            - Calorias hoje: ${ctx.caloriasConsumidas} / ${ctx.metaCaloriasTotal} kcal (bônus dos passos: +${ctx.bonusCaloriasPassos} kcal, passos hoje: ${ctx.passosHoje})
            - Água hoje: ${ctx.aguaConsumidaMl} / ${ctx.metaAguaTotalMl} ml (bônus dos passos: +${ctx.bonusAguaPassosMl} ml)
            - Refeições concluídas hoje: $concluidas
            - Refeições pendentes hoje: $pendentes
        """.trimIndent()
    }

    /**
     * Motor Especialista Contextual 100% Offline: analisa a intenção nutricional da pergunta
     * e formula uma resposta personalizada com os dados reais do Room e do sensor de passos.
     */
    internal fun gerarRespostaContextualOffline(
        pergunta: String,
        ctx: NutriUserContext,
    ): String {
        val p = Normalizer.normalize(pergunta.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        val palavras = p.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        fun contem(vararg chaves: String) = chaves.any { chave ->
            if (chave.length >= 4) palavras.any { it.startsWith(chave) } else chave in palavras
        }

        val avisoRestricao = when {
            ctx.vegetarianoOuVegano -> " (respeitando sua preferência vegetal)"
            ctx.semLactose -> " (em versão zero lactose como no seu perfil)"
            ctx.semGluten -> " (naturalmente sem glúten)"
            else -> ""
        }

        return when {
            // 1. O que falta comer hoje / Próxima refeição / Plano do dia
            contem("falta", "proxim", "restam", "cardapio", "planejad") ||
                (contem("comer", "refeic", "plano") && contem("hoje", "agora", "qual", "quais")) -> {
                if (ctx.refeicoesPendentes.isEmpty()) {
                    "Parabéns, ${ctx.nome}! Você já concluiu todas as ${ctx.refeicoesConcluidas.size} refeições planejadas hoje (${ctx.caloriasConsumidas} kcal). Agora é só manter a hidratação (${ctx.aguaConsumidaMl}/${ctx.metaAguaTotalMl} ml)! 🎉"
                } else {
                    val proxima = ctx.refeicoesPendentes.first()
                    val lista = ctx.refeicoesPendentes.joinToString(" • ") { "${it.horario} ${it.titulo} (${it.calorias} kcal)" }
                    "Sua próxima refeição é **${proxima.titulo}** às ${proxima.horario} (${proxima.descricao} — ${proxima.calorias} kcal). Hoje ainda restam ${ctx.refeicoesPendentes.size}: $lista."
                }
            }

            // 2. Substituição de Carboidratos
            contem("arroz", "batata", "macarrao", "pao", "tapioca", "aveia", "mandioca", "carbo") -> {
                val dicaFoco = when (ctx.foco) {
                    FocoPrincipal.HIPERTROFIA, FocoPrincipal.PERFORMANCE ->
                        "Como seu foco é ${ctx.foco.rotulo}, mantenha a carga energética completa"
                    else ->
                        "Para favorecer a saciedade no seu foco de ${ctx.foco.rotulo}, priorize raízes ou integrais"
                }
                val opcoes = if (ctx.semGluten) {
                    "100g de arroz ≈ 120g de batata-doce ≈ 110g de mandioca cozida ≈ 45g de tapioca"
                } else {
                    "100g de arroz ≈ 120g de batata-doce ≈ 80g de macarrão cozido ≈ 2 fatias de pão integral"
                }
                "Boa pergunta, ${ctx.nome}! Equivalência de carboidratos$avisoRestricao: $opcoes. $dicaFoco (você ainda tem ${ctx.saldoCalorias} kcal disponíveis hoje). 😉"
            }

            // 3. Substituição de Proteínas
            contem("frango", "carne", "patinho", "peixe", "tilapia", "ovo", "ovos", "whey", "tofu", "protein") -> {
                val opcoes = when {
                    ctx.vegetarianoOuVegano ->
                        "160g de tofu grelhado ≈ 150g de grão-de-bico/lentilha cozidos + 30g de proteína vegetal"
                    ctx.semLactose ->
                        "150g de frango grelhado ≈ 150g de patinho ≈ 160g de tilápia ≈ 3 a 4 ovos (ou shake de proteína isolada/vegetal zero lactose)"
                    else ->
                        "150g de frango ≈ 150g de patinho ≈ 160g de tilápia ≈ 4 ovos ≈ 30g de whey + iogurte natural"
                }
                "Pode alternar as proteínas tranquilamente, ${ctx.nome}$avisoRestricao: $opcoes. Prefira preparos grelhados ou assados para preservar sua meta de ${ctx.foco.rotulo}!"
            }

            // 4. Calorias / Saldo energético
            contem("caloria", "kcal", "saldo", "deficit", "consumi") -> {
                val bonusTxt = if (ctx.bonusCaloriasPassos > 0) {
                    " (já incluindo +${ctx.bonusCaloriasPassos} kcal liberadas pelos seus ${ctx.passosHoje} passos!)"
                } else ""
                "Hoje você consumiu **${ctx.caloriasConsumidas} de ${ctx.metaCaloriasTotal} kcal**$bonusTxt. Restam **${ctx.saldoCalorias} kcal** distribuídas em ${ctx.refeicoesPendentes.size} refeição(ões) pendente(s)."
            }

            // 5. Água e Hidratação
            contem("agua", "hidrat", "sede", "beber", "litro", "copo") -> {
                if (ctx.faltaAguaMl == 0) {
                    "Meta hídrica batida, ${ctx.nome}! Você já atingiu ${ctx.aguaConsumidaMl} ml hoje. Excelente constância! 💧"
                } else {
                    val bonusAguaTxt = if (ctx.bonusAguaPassosMl > 0) " (com +${ctx.bonusAguaPassosMl} ml de bônus pelos seus ${ctx.passosHoje} passos)" else ""
                    "Você já registrou **${ctx.aguaConsumidaMl} de ${ctx.metaAguaTotalMl} ml** hoje$bonusAguaTxt. Faltam **${ctx.faltaAguaMl} ml** (cerca de ${ctx.coposAguaRestantes} cliques de +250 ml na aba Dieta). 💧"
                }
            }

            // 6. Peso, IMC, Medidas e Progresso Corporal
            contem("peso", "imc", "medida", "cintura", "quadril", "evoluc", "emagrec", "meta") -> {
                val imcTxt = ctx.imc?.let { "Seu IMC atual é **${DataHora.formatarDecimal(it)}** (${ctx.classificacaoImc})." } ?: ""
                val varTxt = ctx.variacaoPesoKg?.let { v ->
                    val sinal = if (v > 0) "+" else ""
                    " Desde o primeiro registro, sua variação é de **$sinal${DataHora.formatarDecimal(v)} kg**."
                } ?: ""
                val diffMeta = abs(ctx.pesoAtualKg - ctx.metaPesoKg)
                "Seu peso atual no TriHub é **${DataHora.formatarDecimal(ctx.pesoAtualKg)} kg** e sua meta é **${DataHora.formatarDecimal(ctx.metaPesoKg)} kg** (diferença de ${DataHora.formatarDecimal(diffMeta)} kg). $imcTxt$varTxt"
            }

            // 7. Treino, Modalidades e Passos
            contem("treino", "academia", "corrida", "ciclismo", "natacao", "crossfit", "passo", "caminhada", "energia") -> {
                val esportes = ctx.modalidades.takeIf { it.isNotEmpty() }?.joinToString(" e ") ?: "seu treino"
                val refeicaoSugerida = ctx.refeicoesPendentes.firstOrNull()?.let {
                    "Sua próxima refeição programada (${it.titulo} às ${it.horario}) encaixa muito bem como suporte energético."
                } ?: "Mantenha a hidratação pós-exercício!"
                "Para $esportes no foco de ${ctx.foco.rotulo}: consuma carboidrato de fácil digestão 45–60 min antes. Hoje o sensor já contou **${ctx.passosHoje} passos** (+${ctx.bonusCaloriasPassos} kcal e +${ctx.bonusAguaPassosMl} ml na sua meta). $refeicaoSugerida"
            }

            // 8. Doces / Chocolate / Sobremesa
            contem("doce", "chocolate", "sobremesa", "acucar", "vontade") -> {
                val opcaoDoce = if (ctx.semLactose) "20g de chocolate 70% cacau (sem leite) ou banana aquecida com canela" else "20g de chocolate 70% cacau ou iogurte com cacau e morangos"
                "Pode encaixar sem culpa, ${ctx.nome}! Com ${ctx.saldoCalorias} kcal restantes hoje, uma ótima opção$avisoRestricao é $opcaoDoce logo após o almoço para reduzir o pico glicêmico. 🍫"
            }

            // 9. Fome fora de hora / Lanche
            contem("fome", "lanche", "beliscar", "saciedade") -> {
                val sugestaoLanche = when {
                    ctx.vegetarianoOuVegano -> "uma fruta com 15g de castanhas ou mix de sementes"
                    ctx.semLactose -> "uma fruta com aveia ou 2 ovos cozidos"
                    else -> "um iogurte natural com chia ou uma fruta com aveia"
                }
                "Se bater fome fora de hora, ${ctx.nome}, priorize fibras e proteínas: experimente $sugestaoLanche. Também vale beber 250 ml de água antes, pois faltam ${ctx.faltaAguaMl} ml para sua meta de hoje!"
            }

            // 10. Agradecimento / Confirmação
            contem("obrigad", "valeu", "perfeito", "ok", "certo", "show") ->
                "Por nada, ${ctx.nome}! Continue firme no seu foco de ${ctx.foco.rotulo} — você já concluiu ${ctx.refeicoesConcluidas.size} refeição(ões) hoje. Estou acompanhando! 💚"

            // 11. Saudação inicial
            contem("ola", "oi", "bom", "boa") ->
                "Olá, ${ctx.nome}! Hoje você está com ${ctx.caloriasConsumidas}/${ctx.metaCaloriasTotal} kcal e ${ctx.aguaConsumidaMl}/${ctx.metaAguaTotalMl} ml de água no seu plano de ${ctx.foco.rotulo}. Quer revisar a próxima refeição ou fazer alguma substituição?"

            // 12. Resposta contextual padrão com resumo do dia do usuário
            else -> {
                val prox = ctx.refeicoesPendentes.firstOrNull()?.let { "Sua próxima refeição é ${it.titulo} às ${it.horario}." } ?: "Todas as refeições de hoje estão concluídas!"
                "Anotado, ${ctx.nome}! No seu plano de **${ctx.foco.rotulo}** hoje você está com **${ctx.caloriasConsumidas}/${ctx.metaCaloriasTotal} kcal** e **${ctx.aguaConsumidaMl}/${ctx.metaAguaTotalMl} ml** de água. $prox Se quiser, pergunte sobre substituições de carboidratos, proteínas, pré-treino ou seu IMC! 🌿"
            }
        }
    }
}
