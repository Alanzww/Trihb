package com.example.trihb.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * Banco SQLite relacional local (Offline-First) do TriHub — Versão 2.
 * 7 entidades relacionais (chaves estrangeiras CASCADE/SET_NULL + índices compostos),
 * 5 DAOs e modo Journal WRITE_AHEAD_LOGGING (WAL) para leituras/escritas concorrentes.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        SessaoEntity::class,
        RefeicaoEntity::class,
        HidratacaoEntity::class,
        ProgressoEntity::class,
        PostFeedEntity::class,
        MensagemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun refeicaoDao(): RefeicaoDao
    abstract fun progressoDao(): ProgressoDao
    abstract fun postFeedDao(): PostFeedDao
    abstract fun mensagemDao(): MensagemDao

    companion object {
        private const val NOME_BANCO = "trihub.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOME_BANCO,
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
    }

    /**
     * Popula duas contas completas de demonstração (Alan e Marina S.) no banco local,
     * permitindo testar Login/Registro, alternância entre contas separadas e dados iniciais.
     */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            popularSeVazio(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Garante a população inicial mesmo após destructive migration (v1 -> v2),
            // pois o Room executa onDestructiveMigration antes de recriar as novas tabelas.
            popularSeVazio(db)
        }

        private fun popularSeVazio(db: SupportSQLiteDatabase) {
            val jaPopulado = runCatching {
                db.query("SELECT COUNT(*) FROM usuarios").use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) > 0
                }
            }.getOrDefault(false)
            if (jaPopulado) return

            db.beginTransaction()
            try {
                popularBancoInicial(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        private fun popularBancoInicial(db: SupportSQLiteDatabase) {
            val agora = System.currentTimeMillis()

            // 1. Conta 1 — Alan (Emagrecimento & Definição, Musculação + Corrida)
            db.inserir("usuarios") {
                put("id", UsuarioEntity.ID_DEMO_ALAN)
                put("nome", "Alan")
                put("email", "alan@trihub.com")
                put("senhaHash", UsuarioEntity.gerarHashSenha("alan@trihub.com", "123456"))
                put("fotoUri", "")
                put("bio", "Focado em baixar para 72 kg mantendo energia na corrida e musculação.")
                put("alturaCm", 176f)
                put("pesoAtualKg", 74.2f)
                put("metaPesoKg", 72.0f)
                put("metaCaloriasKcal", 2100)
                put("metaAguaMl", 2500)
                put("focoPrincipal", FocoPrincipal.EMAGRECIMENTO.codigo)
                put("modalidades", "Musculação,Corrida")
                put("nivelAtividade", NivelAtividade.MODERADO.codigo)
                put("restricoesAlimentares", "")
                put("preferenciasJson", "{\"notificacoesAgua\":true}")
                put("criadoEm", agora)
            }

            // 2. Conta 2 — Marina S. (Performance Esportiva, Corrida + Ciclismo + Natação)
            db.inserir("usuarios") {
                put("id", UsuarioEntity.ID_DEMO_MARINA)
                put("nome", "Marina S.")
                put("email", "marina@trihub.com")
                put("senhaHash", UsuarioEntity.gerarHashSenha("marina@trihub.com", "123456"))
                put("fotoUri", "")
                put("bio", "Triatleta amadora • Hidratação e periodização de carboidratos em dia!")
                put("alturaCm", 166f)
                put("pesoAtualKg", 59.4f)
                put("metaPesoKg", 58.5f)
                put("metaCaloriasKcal", 2350)
                put("metaAguaMl", 2800)
                put("focoPrincipal", FocoPrincipal.PERFORMANCE.codigo)
                put("modalidades", "Corrida,Ciclismo,Natação")
                put("nivelAtividade", NivelAtividade.INTENSO.codigo)
                put("restricoesAlimentares", "Sem Lactose")
                put("preferenciasJson", "{\"notificacoesAgua\":true}")
                put("criadoEm", agora)
            }

            // Sessão inicial: inicia na tela de Login (usuarioAtivoId = null) para permitir
            // demonstrar a tela de Login/Registro logo após a animação da logo
            db.inserir("sessao_ativa") {
                put("id", SessaoEntity.ID_SESSAO_UNICA)
                putNull("usuarioAtivoId")
                put("manterConectado", 1)
                put("geminiApiKey", "")
                put("ultimoLoginEm", agora)
            }

            // Refeições do Alan (id = 1) -----------------------------------------
            listOf(
                RefeicaoSeed("07:30", "Café da Manhã", "3 ovos mexidos + 1 fatia de pão integral + café", 420, true),
                RefeicaoSeed("12:30", "Almoço", "150g frango grelhado + 100g arroz + salada à vontade", 580, false),
                RefeicaoSeed("16:30", "Lanche da Tarde", "Iogurte natural + 30g aveia + banana", 290, false),
                RefeicaoSeed("20:00", "Jantar", "150g patinho moído + 120g batata doce + brócolis", 510, false),
            ).forEach { r ->
                db.inserir("refeicoes") {
                    put("usuarioId", UsuarioEntity.ID_DEMO_ALAN)
                    put("horario", r.horario)
                    put("titulo", r.titulo)
                    put("descricao", r.descricao)
                    put("calorias", r.calorias)
                    put("concluida", if (r.concluida) 1 else 0)
                    put("observacao", "")
                }
            }

            // Refeições da Marina (id = 2) — demonstra isolamento entre contas ----
            listOf(
                RefeicaoSeed("06:30", "Pré-Treino Endurance", "Tapioca com geleia 100% fruta + café preto", 340, true),
                RefeicaoSeed("09:00", "Pós-Treino", "Shake proteína vegetal + banana + 40g aveia", 410, true),
                RefeicaoSeed("13:00", "Almoço Energético", "150g tilápia + 140g macarrão integral + legumes", 640, false),
                RefeicaoSeed("19:30", "Jantar Recuperação", "140g frango desfiado + 150g mandioca + azeite", 560, false),
            ).forEach { r ->
                db.inserir("refeicoes") {
                    put("usuarioId", UsuarioEntity.ID_DEMO_MARINA)
                    put("horario", r.horario)
                    put("titulo", r.titulo)
                    put("descricao", r.descricao)
                    put("calorias", r.calorias)
                    put("concluida", if (r.concluida) 1 else 0)
                    put("observacao", "Plano sem lactose")
                }
            }

            // Hidratação por conta -----------------------------------------------
            db.inserir("hidratacao") {
                put("usuarioId", UsuarioEntity.ID_DEMO_ALAN)
                put("consumidoMl", 1500)
                put("metaBaseMl", 2500)
            }
            db.inserir("hidratacao") {
                put("usuarioId", UsuarioEntity.ID_DEMO_MARINA)
                put("consumidoMl", 2250)
                put("metaBaseMl", 2800)
            }

            // Progresso corporal do Alan (3 registros para o gráfico) ------------
            listOf(
                floatArrayOf(76.0f, 83.0f, 99.0f) to "01/10/2026",
                floatArrayOf(75.1f, 82.0f, 98.5f) to "01/11/2026",
                floatArrayOf(74.2f, 81.0f, 98.0f) to "23/11/2026",
            ).forEach { (m, data) ->
                db.inserir("progresso_corporal") {
                    put("usuarioId", UsuarioEntity.ID_DEMO_ALAN)
                    put("dataRegistro", data)
                    put("pesoKg", m[0])
                    put("metaPesoKg", 72.0f)
                    put("cinturaCm", m[1])
                    put("quadrilCm", m[2])
                }
            }

            // Progresso corporal da Marina ---------------------------------------
            listOf(
                floatArrayOf(60.5f, 68.0f, 94.0f) to "01/10/2026",
                floatArrayOf(59.9f, 67.5f, 93.5f) to "01/11/2026",
                floatArrayOf(59.4f, 67.0f, 93.0f) to "23/11/2026",
            ).forEach { (m, data) ->
                db.inserir("progresso_corporal") {
                    put("usuarioId", UsuarioEntity.ID_DEMO_MARINA)
                    put("dataRegistro", data)
                    put("pesoKg", m[0])
                    put("metaPesoKg", 58.5f)
                    put("cinturaCm", m[1])
                    put("quadrilCm", m[2])
                }
            }

            // Feed comunitário ---------------------------------------------------
            db.inserir("feed_posts") {
                putNull("autorUsuarioId")
                put("autorNome", "Carlos E.")
                put("autorFocoBadge", "💪 Hipertrofia")
                put("tempoAtras", "há 2h")
                put("badgeConquista", "🥗 Pré-treino turbinado: mingau de aveia + whey")
                put("descricao", "Treino de pernas pesado hoje e a energia veio do mingau que a nutri sugeriu. Bora manter o ritmo!")
                put("incentivosCount", 28)
                put("incentivadoPorMim", 0)
                put("isAutorUsuarioAtual", 0)
            }
            db.inserir("feed_posts") {
                put("autorUsuarioId", UsuarioEntity.ID_DEMO_MARINA)
                put("autorNome", "Marina S.")
                put("autorFocoBadge", "⚡ Performance Esportiva")
                put("tempoAtras", "há 25 min")
                put("badgeConquista", "🔥 7º dia consecutivo batendo a meta de hidratação!")
                put("descricao", "Garrafinha sempre por perto e lembrete no celular. Uma semana inteira sem falhar!")
                put("incentivosCount", 14)
                put("incentivadoPorMim", 0)
                put("isAutorUsuarioAtual", 0)
            }

            // Chat do Alan -------------------------------------------------------
            listOf(
                Triple(MensagemEntity.REMETENTE_NUTRI, "10:15", "Olá Alan! Ajustei os carboidratos do seu pré-treino para hoje!"),
                Triple(MensagemEntity.REMETENTE_PACIENTE, "10:20", "Perfeito, já visualizei no plano. Posso trocar arroz por batata?"),
                Triple(MensagemEntity.REMETENTE_NUTRI, "10:22", "Pode sim! Mesma proporção: cerca de 120g de batata doce."),
            ).forEach { (remetente, horario, texto) ->
                db.inserir("chat_mensagens") {
                    put("usuarioId", UsuarioEntity.ID_DEMO_ALAN)
                    put("remetente", remetente)
                    put("texto", texto)
                    put("horario", horario)
                    put("fonteIa", MensagemEntity.FONTE_LOCAL_ROOM)
                }
            }

            // Chat da Marina -----------------------------------------------------
            db.inserir("chat_mensagens") {
                put("usuarioId", UsuarioEntity.ID_DEMO_MARINA)
                put("remetente", MensagemEntity.REMETENTE_NUTRI)
                put("texto", "Olá Marina! Como seu perfil tem restrição a lactose, mantive o pós-treino com proteína vegetal e aveia para sua prova de domingo. ⚡")
                put("horario", "08:40")
                put("fonteIa", MensagemEntity.FONTE_LOCAL_ROOM)
            }
        }

        private data class RefeicaoSeed(
            val horario: String,
            val titulo: String,
            val descricao: String,
            val calorias: Int,
            val concluida: Boolean,
        )

        private inline fun SupportSQLiteDatabase.inserir(tabela: String, valores: ContentValues.() -> Unit) {
            insert(tabela, SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply(valores))
        }
    }
}
