package com.example.trihb.data.repository

import android.content.Context
import com.example.trihb.data.local.AppDatabase
import com.example.trihb.data.local.dao.MensagemDao
import com.example.trihb.data.local.dao.PostFeedDao
import com.example.trihb.data.local.dao.ProgressoDao
import com.example.trihb.data.local.dao.RefeicaoDao
import com.example.trihb.data.local.dao.UsuarioDao
import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.HidratacaoEntity
import com.example.trihb.data.local.entity.MensagemEntity
import com.example.trihb.data.local.entity.NivelAtividade
import com.example.trihb.data.local.entity.PostFeedEntity
import com.example.trihb.data.local.entity.ProgressoEntity
import com.example.trihb.data.local.entity.RefeicaoEntity
import com.example.trihb.data.local.entity.SessaoEntity
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.local.entity.UsuarioResumoRelation
import com.example.trihb.util.DataHora
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * Fachada única de acesso aos DAOs (Repository Pattern).
 * Todos os fluxos de Dieta, Hidratação, Progresso, Chat e Perfil reagem
 * automaticamente à troca de conta na tabela [SessaoEntity].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TriHubRepository(
    private val usuarioDao: UsuarioDao,
    private val refeicaoDao: RefeicaoDao,
    private val progressoDao: ProgressoDao,
    private val postFeedDao: PostFeedDao,
    private val mensagemDao: MensagemDao,
) {

    // Sessão e Usuários ---------------------------------------------------------
    val sessao: Flow<SessaoEntity?> = usuarioDao.observarSessao()
    val todosUsuarios: Flow<List<UsuarioEntity>> = usuarioDao.observarTodosUsuarios()

    /** ID da conta ativa (fallback seguro para a conta Demo Alan). */
    val usuarioAtivoIdFlow: Flow<Int> = sessao
        .map { it?.usuarioAtivoId ?: UsuarioEntity.ID_DEMO_ALAN }
        .distinctUntilChanged()

    val usuarioAtivo: Flow<UsuarioEntity?> = usuarioAtivoIdFlow
        .flatMapLatest { id -> usuarioDao.observarUsuarioPorId(id) }

    val resumoUsuarioAtivo: Flow<UsuarioResumoRelation?> = usuarioAtivoIdFlow
        .flatMapLatest { id -> usuarioDao.observarResumoUsuario(id) }

    suspend fun obterUsuarioAtivoId(): Int =
        usuarioDao.buscarSessao()?.usuarioAtivoId ?: UsuarioEntity.ID_DEMO_ALAN

    suspend fun buscarSessao(): SessaoEntity? = usuarioDao.buscarSessao()

    suspend fun buscarUsuarioAtivo(): UsuarioEntity? =
        usuarioDao.buscarUsuarioPorId(obterUsuarioAtivoId())

    suspend fun buscarResumoUsuarioAtivo(): UsuarioResumoRelation? =
        usuarioDao.buscarResumoUsuario(obterUsuarioAtivoId())

    // Autenticação & Contas -----------------------------------------------------
    suspend fun autenticar(
        email: String,
        senha: String,
        manterConectado: Boolean = true,
    ): Result<UsuarioEntity> {
        val emailLimpo = email.trim().lowercase()
        val usuario = usuarioDao.buscarUsuarioPorEmail(emailLimpo)
            ?: return Result.failure(IllegalArgumentException("Nenhuma conta encontrada para este e-mail."))

        val hashInformado = UsuarioEntity.gerarHashSenha(emailLimpo, senha)
        if (usuario.senhaHash != hashInformado) {
            return Result.failure(IllegalArgumentException("Senha incorreta. Tente novamente."))
        }
        ativarSessao(usuario.id, manterConectado)
        return Result.success(usuario)
    }

    /** Login rápido em 1 toque para alternar entre contas locais durante a apresentação. */
    suspend fun loginRapidoPorId(
        usuarioId: Int,
        manterConectado: Boolean = true,
    ): Result<UsuarioEntity> {
        val usuario = usuarioDao.buscarUsuarioPorId(usuarioId)
            ?: return Result.failure(IllegalArgumentException("Conta não encontrada."))
        ativarSessao(usuario.id, manterConectado)
        return Result.success(usuario)
    }

    /**
     * Registra uma nova conta local, cria o plano inicial de 4 refeições adaptado ao
     * [focoPrincipal] e às [restricoesAlimentares], define a meta hídrica (35 ml/kg),
     * registra o peso inicial no gráfico e envia mensagem de boas-vindas da Dra. Camila.
     */
    suspend fun registrarNovoUsuario(
        nome: String,
        email: String,
        senha: String,
        bio: String,
        alturaCm: Float,
        pesoAtualKg: Float,
        metaPesoKg: Float,
        focoPrincipal: FocoPrincipal,
        modalidades: List<String>,
        nivelAtividade: NivelAtividade,
        restricoesAlimentares: List<String>,
    ): Result<UsuarioEntity> {
        val emailLimpo = email.trim().lowercase()
        if (usuarioDao.buscarUsuarioPorEmail(emailLimpo) != null) {
            return Result.failure(IllegalArgumentException("Já existe uma conta cadastrada com este e-mail."))
        }

        val metaAguaCalculada = ((pesoAtualKg * 35f) / 50f).roundToInt().coerceIn(40, 80) * 50
        val novoUsuario = UsuarioEntity(
            nome = nome.trim(),
            email = emailLimpo,
            senhaHash = UsuarioEntity.gerarHashSenha(emailLimpo, senha),
            bio = bio.trim().ifBlank { "Buscando constância em ${focoPrincipal.rotulo.lowercase()} no TriHub." },
            alturaCm = alturaCm,
            pesoAtualKg = pesoAtualKg,
            metaPesoKg = metaPesoKg,
            metaCaloriasKcal = focoPrincipal.metaCaloriasSugerida,
            metaAguaMl = metaAguaCalculada,
            focoPrincipal = focoPrincipal.codigo,
            modalidades = modalidades.joinToString(","),
            nivelAtividade = nivelAtividade.codigo,
            restricoesAlimentares = restricoesAlimentares.joinToString(","),
        )

        val novoId = usuarioDao.inserirUsuario(novoUsuario).toInt()
        val usuarioSalvo = novoUsuario.copy(id = novoId)

        // 1. Plano inicial de refeições personalizado
        refeicaoDao.inserirRefeicoes(
            gerarPlanoInicialRefeicoes(
                usuarioId = novoId,
                foco = focoPrincipal,
                restricoes = restricoesAlimentares,
            ),
        )

        // 2. Meta de hidratação personalizada
        refeicaoDao.atualizarHidratacao(
            HidratacaoEntity(
                usuarioId = novoId,
                consumidoMl = 0,
                metaBaseMl = metaAguaCalculada,
            ),
        )

        // 3. Primeiro registro no histórico corporal
        progressoDao.inserirMedicao(
            ProgressoEntity(
                usuarioId = novoId,
                dataRegistro = DataHora.dataAtual(),
                pesoKg = pesoAtualKg,
                metaPesoKg = metaPesoKg,
                cinturaCm = 80f,
                quadrilCm = 96f,
            ),
        )

        // 4. Mensagem inicial personalizada da Dra. Camila
        val esportesTxt = modalidades.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "sua rotina ativa"
        mensagemDao.inserirMensagem(
            MensagemEntity(
                usuarioId = novoId,
                remetente = MensagemEntity.REMETENTE_NUTRI,
                texto = "Olá, ${usuarioSalvo.nome}! Sou a Dra. Camila Nutrition 🌿. Preparei seu plano inicial focado em ${focoPrincipal.rotulo} (${focoPrincipal.metaCaloriasSugerida} kcal e $metaAguaCalculada ml de água) alinhado com $esportesTxt. Pode me chamar aqui sempre que precisar substituir algum alimento!",
                horario = DataHora.horaAtual(),
                fonteIa = MensagemEntity.FONTE_LOCAL_ROOM,
            ),
        )

        ativarSessao(novoId, manterConectado = true)
        return Result.success(usuarioSalvo)
    }

    suspend fun atualizarPerfilUsuario(
        usuario: UsuarioEntity,
        registrarNovoPesoNoHistorico: Boolean = false,
    ) {
        usuarioDao.atualizarUsuario(usuario)
        // Sincroniza a meta base de hidratação da conta
        val hidratacaoAtual = usuarioDao.buscarResumoUsuario(usuario.id)?.hidratacao
        refeicaoDao.atualizarHidratacao(
            HidratacaoEntity(
                usuarioId = usuario.id,
                consumidoMl = hidratacaoAtual?.consumidoMl ?: 0,
                metaBaseMl = usuario.metaAguaMl,
            ),
        )
        if (registrarNovoPesoNoHistorico) {
            val ultimo = usuarioDao.buscarResumoUsuario(usuario.id)?.progressos?.lastOrNull()
            progressoDao.inserirMedicao(
                ProgressoEntity(
                    usuarioId = usuario.id,
                    dataRegistro = DataHora.dataAtual(),
                    pesoKg = usuario.pesoAtualKg,
                    metaPesoKg = usuario.metaPesoKg,
                    cinturaCm = ultimo?.cinturaCm ?: 80f,
                    quadrilCm = ultimo?.quadrilCm ?: 96f,
                ),
            )
        }
    }

    suspend fun salvarChaveGemini(apiKey: String) {
        val sessaoAtual = usuarioDao.buscarSessao() ?: SessaoEntity()
        usuarioDao.salvarSessao(sessaoAtual.copy(geminiApiKey = apiKey.trim()))
    }

    /** Marca o tutorial inicial como concluído (ou pulado) para a conta ativa no Room. */
    suspend fun marcarTutorialConcluido() {
        val usuario = buscarUsuarioAtivo() ?: return
        if (usuario.tutorialConcluido) return
        usuarioDao.atualizarUsuario(
            usuario.copy(
                preferenciasJson = "{\"notificacoesAgua\":true,\"tutorialConcluido\":true}",
            ),
        )
    }

    suspend fun encerrarSessao() {
        val sessaoAtual = usuarioDao.buscarSessao() ?: SessaoEntity()
        usuarioDao.salvarSessao(
            sessaoAtual.copy(
                usuarioAtivoId = null,
                manterConectado = false,
            ),
        )
    }

    private suspend fun ativarSessao(usuarioId: Int, manterConectado: Boolean) {
        val sessaoAtual = usuarioDao.buscarSessao() ?: SessaoEntity()
        usuarioDao.salvarSessao(
            sessaoAtual.copy(
                usuarioAtivoId = usuarioId,
                manterConectado = manterConectado,
                ultimoLoginEm = System.currentTimeMillis(),
            ),
        )
    }

    // Dieta / Refeições (isoladas pela conta ativa) -----------------------------
    val refeicoes: Flow<List<RefeicaoEntity>> = usuarioAtivoIdFlow
        .flatMapLatest { id -> refeicaoDao.observarRefeicoesDoUsuario(id) }

    val hidratacao: Flow<HidratacaoEntity?> = usuarioAtivoIdFlow
        .flatMapLatest { id -> refeicaoDao.observarHidratacaoDoUsuario(id) }

    suspend fun buscarRefeicao(id: Int): RefeicaoEntity? = refeicaoDao.buscarRefeicaoPorId(id)

    suspend fun inserirRefeicao(refeicao: RefeicaoEntity): Long {
        val uid = obterUsuarioAtivoId()
        return refeicaoDao.inserirRefeicao(refeicao.copy(usuarioId = uid))
    }

    suspend fun atualizarRefeicao(refeicao: RefeicaoEntity) =
        refeicaoDao.atualizarRefeicao(refeicao)

    suspend fun deletarRefeicao(refeicao: RefeicaoEntity) =
        refeicaoDao.deletarRefeicao(refeicao)

    suspend fun atualizarHidratacao(consumidoMl: Int, metaBaseMl: Int) {
        val uid = obterUsuarioAtivoId()
        refeicaoDao.atualizarHidratacao(
            HidratacaoEntity(
                usuarioId = uid,
                consumidoMl = consumidoMl,
                metaBaseMl = metaBaseMl,
            ),
        )
    }

    // Progresso (isolado pela conta ativa) --------------------------------------
    val historicoProgresso: Flow<List<ProgressoEntity>> = usuarioAtivoIdFlow
        .flatMapLatest { id -> progressoDao.observarHistoricoDoUsuario(id) }

    suspend fun inserirMedicao(medicao: ProgressoEntity): Long {
        val uid = obterUsuarioAtivoId()
        val idInserido = progressoDao.inserirMedicao(medicao.copy(usuarioId = uid))
        // Mantém o pesoAtualKg do perfil sincronizado com a medição mais recente
        usuarioDao.buscarUsuarioPorId(uid)?.let { u ->
            usuarioDao.atualizarUsuario(u.copy(pesoAtualKg = medicao.pesoKg))
        }
        return idInserido
    }

    suspend fun deletarMedicao(medicao: ProgressoEntity) =
        progressoDao.deletarMedicao(medicao)

    // Feed comunitário ----------------------------------------------------------
    val posts: Flow<List<PostFeedEntity>> = postFeedDao.observarPosts()

    suspend fun inserirPost(post: PostFeedEntity): Long = postFeedDao.inserirPost(post)
    suspend fun atualizarPost(post: PostFeedEntity) = postFeedDao.atualizarPost(post)
    suspend fun deletarPost(post: PostFeedEntity) = postFeedDao.deletarPost(post)

    // Chat (isolado pela conta ativa) -------------------------------------------
    val mensagens: Flow<List<MensagemEntity>> = usuarioAtivoIdFlow
        .flatMapLatest { id -> mensagemDao.observarMensagensDoUsuario(id) }

    suspend fun inserirMensagem(mensagem: MensagemEntity): Long {
        val uid = obterUsuarioAtivoId()
        return mensagemDao.inserirMensagem(mensagem.copy(usuarioId = uid))
    }

    suspend fun buscarUltimasMensagens(limite: Int = 8): List<MensagemEntity> {
        val uid = obterUsuarioAtivoId()
        return mensagemDao.buscarUltimasMensagens(uid, limite).asReversed()
    }

    companion object {
        @Volatile
        private var INSTANCE: TriHubRepository? = null

        fun getInstance(context: Context): TriHubRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase.getInstance(context).let { db ->
                    TriHubRepository(
                        usuarioDao = db.usuarioDao(),
                        refeicaoDao = db.refeicaoDao(),
                        progressoDao = db.progressoDao(),
                        postFeedDao = db.postFeedDao(),
                        mensagemDao = db.mensagemDao(),
                    )
                }.also { INSTANCE = it }
            }

        /** Gera um cronograma inicial de 4 refeições adequado ao foco e restrições do usuário. */
        internal fun gerarPlanoInicialRefeicoes(
            usuarioId: Int,
            foco: FocoPrincipal,
            restricoes: List<String>,
        ): List<RefeicaoEntity> {
            val semCarne = restricoes.any { it.contains("Veg", ignoreCase = true) }
            val semLactose = restricoes.any { it.contains("Lactose", ignoreCase = true) || it.contains("Vegano", ignoreCase = true) }

            val proteinaPrincipal = if (semCarne) "160g tofu grelhado com grão-de-bico" else "150g frango grelhado"
            val proteinaJantar = if (semCarne) "150g lentilha + cogumelos salteados" else "150g patinho moído"
            val lancheBase = if (semLactose) "Shake de proteína vegetal + 30g aveia + banana" else "Iogurte natural + 30g aveia + banana"

            return when (foco) {
                FocoPrincipal.HIPERTROFIA -> listOf(
                    RefeicaoEntity(usuarioId = usuarioId, horario = "07:30", titulo = "Café da Manhã Proteico", descricao = "4 ovos mexidos + 2 fatias de pão integral + fruta", calorias = 540),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "12:30", titulo = "Almoço Construtor", descricao = "$proteinaPrincipal + 160g arroz + feijão + salada", calorias = 720),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "16:30", titulo = "Pré-Treino Força", descricao = "$lancheBase + 15g pasta de amendoim", calorias = 430),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "20:30", titulo = "Jantar Recuperação", descricao = "$proteinaJantar + 180g batata doce + brócolis", calorias = 640),
                )
                FocoPrincipal.PERFORMANCE -> listOf(
                    RefeicaoEntity(usuarioId = usuarioId, horario = "06:30", titulo = "Pré-Treino Energia", descricao = "Tapioca com banana e mel + café", calorias = 410),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "12:30", titulo = "Almoço Endurance", descricao = "$proteinaPrincipal + 150g macarrão integral + legumes", calorias = 660),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "16:30", titulo = "Lanche Repositor", descricao = lancheBase, calorias = 360),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "20:00", titulo = "Jantar Antioxidante", descricao = "$proteinaJantar + 150g mandioca + azeite", calorias = 580),
                )
                else -> listOf(
                    RefeicaoEntity(usuarioId = usuarioId, horario = "07:30", titulo = "Café da Manhã", descricao = "3 ovos mexidos + 1 fatia de pão integral + café", calorias = 420),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "12:30", titulo = "Almoço", descricao = "$proteinaPrincipal + 100g arroz + salada à vontade", calorias = 580),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "16:30", titulo = "Lanche da Tarde", descricao = lancheBase, calorias = 290),
                    RefeicaoEntity(usuarioId = usuarioId, horario = "20:00", titulo = "Jantar", descricao = "$proteinaJantar + 120g batata doce + brócolis", calorias = 510),
                )
            }
        }
    }
}
