package app.noplano

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.noplano.data.Banco
import app.noplano.data.PlanoArquivo
import app.noplano.data.PlanoArquivoInvalido
import app.noplano.data.Repositorio
import app.noplano.data.Taco
import app.noplano.data.horarioEmMinutos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanoArquivoTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var banco: Banco
    private lateinit var repo: Repositorio
    private val relogio = RelogioFalso()
    private val d = 1e-9

    @Before fun preparar() = runTest {
        banco = Banco.emMemoria(ctx)
        repo = Repositorio(banco, AjustesFalsos(), relogio)
        Taco.garantir(ctx, banco)
        repo.garantirInicial()
    }

    @After fun fechar() = banco.close()

    private fun arquivo(versao: Int, gramasArroz: Int = 150) = PlanoArquivo.ler("""
        {"versao": $versao, "notas": "teste",
         "alimentos": [{"chave": "whey", "nome": "Whey X", "marca": "M", "porcao": 30, "kcal": 120, "prot": 24, "carb": 3, "gord": 1.5},
                       {"chave": "ovo", "taco": 488, "unidadeNome": "ovo", "unidadeGramas": 50}],
         "planos": [{"nome": "Meu plano", "dias": [1,2,3,4,5,6,7], "alvo": {"prot": 180},
           "refeicoes": [
             {"nome": "Almoço", "horario": "12:30", "itens": [{"taco": 3, "gramas": $gramasArroz}, {"alimento": "whey", "gramas": 30}],
              "alternativas": [{"nome": "Ovos", "itens": [{"alimento": "ovo", "unidades": 3}]}]},
             {"nome": "Café", "horario": "7h00", "itens": [{"alimento": "ovo", "unidades": 2}]}
           ]}]}
    """)

    @Test fun `o arquivo que vai no app e valido e entra no primeiro uso`() = runTest {
        val a = PlanoArquivo.doApp(ctx)
        assertNotNull(a)
        assertTrue(repo.aplicarArquivoSeVazio(a))
        val dia = repo.estadoDia()
        assertEquals(4, dia.total)
        // Conferido à mão com a TACO e o rótulo da Growth:
        // R1 177,3 + 183 + 121,2 = 481,5; R2 e R3 192 + 244,5 + 15 = 451,5; R4 146 + 150 + 183 = 479.
        assertEquals(1863.5, dia.meta.kcal, 1e-6)
        assertEquals(201.255, dia.meta.prot, 1e-6)
        assertNull(repo.planoArquivoPendente(a))
    }

    @Test fun `aplica refeicoes, horarios, alternativas, medidas caseiras e alvo`() = runTest {
        repo.aplicarArquivo(arquivo(1))
        val plano = repo.planos().first().single()
        assertEquals(180.0, plano.alvo.prot!!, d)
        val rs = repo.refeicoes(plano.id).first()
        assertEquals(listOf("Café", "Almoço"), rs.map { it.refeicao.nome })
        assertEquals(7 * 60, rs[0].refeicao.minutos)
        val ovos = rs[0].principal!!.itens.single()
        assertEquals(100.0, ovos.item.gramas, d)
        assertEquals(2.0, ovos.item.unidades!!, d)
        val almoco = rs[1]
        assertEquals(listOf("Principal", "Ovos"), almoco.opcoesOrdenadas.map { it.opcao.nome })
        assertEquals(128.0 * 1.5 + 120.0, almoco.macros.kcal, d)
    }

    @Test fun `aplicar de novo substitui sem duplicar e mantem o historico`() = runTest {
        repo.aplicarArquivo(arquivo(1))
        val almocoId = repo.refeicoes(repo.planos().first().single().id).first().first { it.refeicao.nome == "Almoço" }.refeicao.id
        repo.concluir(relogio.hoje(), almocoId)
        val registrado = repo.estadoDia().consumido.kcal

        repo.aplicarArquivo(arquivo(2, gramasArroz = 100))
        assertEquals(1, repo.planos().first().size)
        val rs = repo.refeicoes(repo.planos().first().single().id).first()
        assertEquals(2, rs.size)
        assertEquals(128.0 + 120.0, rs.first { it.refeicao.nome == "Almoço" }.macros.kcal, d)
        // O que foi registrado continua igual, mesmo com o plano mudando.
        assertEquals(registrado, repo.estadoDia().consumido.kcal, d)
        assertEquals(1, repo.alimentoTacoContagemWhey())
    }

    @Test fun `versao nova so e oferecida uma vez`() = runTest {
        repo.aplicarArquivo(arquivo(1))
        assertNull(repo.planoArquivoPendente(arquivo(1)))
        assertNotNull(repo.planoArquivoPendente(arquivo(2)))
        repo.recusarArquivo(arquivo(2))
        assertNull(repo.planoArquivoPendente(arquivo(2)))
        assertNotNull(repo.planoArquivoPendente(arquivo(3)))
    }

    @Test fun `com plano ja cadastrado nao aplica sozinho`() = runTest {
        val p = repo.planos().first().single().id
        repo.criarRefeicao(p, "Minha refeição", 600)
        assertFalse(repo.aplicarArquivoSeVazio(arquivo(1)))
        assertEquals("Minha refeição", repo.refeicoes(p).first().single().refeicao.nome)
    }

    @Test fun `arquivo com erro nao muda nada`() = runTest {
        repo.aplicarArquivo(arquivo(1))
        val ruim = PlanoArquivo.ler("""{"versao": 2, "planos": [{"nome": "Meu plano", "refeicoes": [{"nome": "X", "horario": "10:00", "itens": [{"taco": 999999, "gramas": 10}]}]}]}""")
        try { repo.aplicarArquivo(ruim); fail("deveria recusar") } catch (_: PlanoArquivoInvalido) {}
        assertEquals(2, repo.refeicoes(repo.planos().first().single().id).first().size)
    }

    @Test fun `horarios aceitos`() {
        assertEquals(450, horarioEmMinutos("07:30"))
        assertEquals(420, horarioEmMinutos("7h"))
        assertEquals(1320, horarioEmMinutos("22h00"))
        try { horarioEmMinutos("25:00"); fail() } catch (_: PlanoArquivoInvalido) {}
    }

    private suspend fun Repositorio.alimentoTacoContagemWhey() = banco.alimentoDao().todos().count { it.nome == "Whey X" }
}
