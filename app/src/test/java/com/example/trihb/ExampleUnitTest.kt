package com.example.trihb

import com.example.trihb.data.ai.NutriAiService
import com.example.trihb.data.ai.NutriUserContext
import com.example.trihb.data.local.entity.FocoPrincipal
import com.example.trihb.data.local.entity.RefeicaoEntity
import com.example.trihb.data.local.entity.UsuarioEntity
import com.example.trihb.data.repository.TriHubRepository
import com.example.trihb.sensor.BonusAtividade
import com.example.trihb.ui.screens.auth.RegistroFormState
import com.example.trihb.ui.screens.chat.ChatViewModel
import com.example.trihb.ui.screens.dieta.RefeicaoFormState
import com.example.trihb.ui.screens.progresso.MedicaoFormState
import com.example.trihb.util.DataHora
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários das regras de negócio do TriHub v2:
 * - Bônus de calorias e hidratação gerado pelo acelerômetro (a cada 20 passos)
 * - Validação de formulários (refeição, medição corporal e registro de conta)
 * - Segurança de hash de senha (SHA-256 + salt) e cálculo de IMC no Perfil
 * - Geração automática do plano inicial personalizado por Foco e Restrições
 * - Motor Especialista Contextual Offline da Dra. Camila (lendo dados reais do usuário)
 */
class ExampleUnitTest {

    @Test
    fun bonusAtividade_calculaCaloriasEAguaACada20Passos() {
        val zero = BonusAtividade(0)
        assertEquals(0, zero.bonusCalorias)
        assertEquals(0, zero.bonusAguaMl)
        assertEquals(20, zero.passosParaProximoBonus)

        val quarentaECinco = BonusAtividade(45)
        assertEquals(2, quarentaECinco.blocos)
        assertEquals(30, quarentaECinco.bonusCalorias)   // 2 * 15 kcal
        assertEquals(50, quarentaECinco.bonusAguaMl)     // 2 * 25 ml
        assertEquals(15, quarentaECinco.passosParaProximoBonus)
    }

    @Test
    fun usuarioEntity_calculaHashSenhaEImcCorretamente() {
        val hash1 = UsuarioEntity.gerarHashSenha("alan@trihub.com", "123456")
        val hash2 = UsuarioEntity.gerarHashSenha("ALAN@trihub.com ", "123456")
        val hashOutro = UsuarioEntity.gerarHashSenha("marina@trihub.com", "123456")
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashOutro)

        val usuario = UsuarioEntity(
            nome = "Alan",
            email = "alan@trihub.com",
            senhaHash = hash1,
            alturaCm = 176f,
            pesoAtualKg = 74.2f,
        )
        val imc = usuario.imc
        assertNotNull(imc)
        assertEquals(23.95f, imc ?: 0f, 0.05f)
        assertEquals("Faixa saudável", usuario.classificacaoImc)
    }

    @Test
    fun planoInicial_adaptaRefeicoesAoFocoERestricoes() {
        val planoVegSemLactose = TriHubRepository.gerarPlanoInicialRefeicoes(
            usuarioId = 10,
            foco = FocoPrincipal.HIPERTROFIA,
            restricoes = listOf("Vegetariano", "Sem Lactose"),
        )
        assertEquals(4, planoVegSemLactose.size)
        assertTrue(planoVegSemLactose.any { it.descricao.contains("tofu", ignoreCase = true) })
        assertTrue(planoVegSemLactose.any { it.descricao.contains("proteína vegetal", ignoreCase = true) })
    }

    @Test
    fun validacaoFormularios_refeicaoMedicaoERegistro() {
        val refeicaoInvalida = RefeicaoFormState(horario = "25:99", titulo = "", calorias = "0")
        assertFalse(refeicaoInvalida.valido)

        val refeicaoValida = RefeicaoFormState(
            horario = "12:30",
            titulo = "Almoço",
            descricao = "150g frango + 100g arroz",
            calorias = "580",
        )
        assertTrue(refeicaoValida.valido)
        assertNull(refeicaoValida.erroHorario)

        val medicao = MedicaoFormState(peso = "74,2", cintura = "81.0", quadril = "98")
        assertTrue(medicao.valido)

        val registroValido = RegistroFormState(
            nome = "Marina",
            email = "marina@trihub.com",
            senha = "123456",
            peso = "59,4",
            altura = "166",
            metaPeso = "58,5",
        )
        assertTrue(registroValido.valido)
    }

    @Test
    fun nutriAiService_respondeComContextoRealDoUsuarioNoRoom() {
        val ctx = NutriUserContext(
            nome = "Marina",
            foco = FocoPrincipal.PERFORMANCE,
            modalidades = listOf("Corrida", "Ciclismo"),
            restricoes = listOf("Sem Lactose"),
            pesoAtualKg = 59.4f,
            metaPesoKg = 58.5f,
            alturaCm = 166f,
            imc = 21.5f,
            classificacaoImc = "Faixa saudável",
            variacaoPesoKg = -1.1f,
            refeicoesConcluidas = listOf(
                RefeicaoEntity(1, 2, "06:30", "Pré-Treino", "Tapioca", 340, true),
            ),
            refeicoesPendentes = listOf(
                RefeicaoEntity(2, 2, "13:00", "Almoço Energético", "Tilápia + macarrão", 640, false),
            ),
            caloriasConsumidas = 340,
            metaCaloriasTotal = 2380,
            bonusCaloriasPassos = 30,
            aguaConsumidaMl = 2000,
            metaAguaTotalMl = 2850,
            bonusAguaPassosMl = 50,
            passosHoje = 40,
        )

        val respPlano = NutriAiService.gerarRespostaContextualOffline("O que falta comer hoje?", ctx)
        assertTrue(respPlano.contains("Almoço Energético"))
        assertTrue(respPlano.contains("13:00"))

        val respProt = NutriAiService.gerarRespostaContextualOffline("Posso substituir frango por ovo?", ctx)
        assertTrue(respProt.contains("zero lactose"))

        val respCarbo = ChatViewModel.gerarRespostaNutri("Posso trocar arroz por batata?")
        assertTrue(respCarbo.contains("120g de batata"))
    }
}
