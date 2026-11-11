package app.noplano

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.noplano.data.Backup
import app.noplano.data.BackupInvalido
import app.noplano.data.Banco
import app.noplano.data.ItemConsumo
import app.noplano.data.Repositorio
import app.noplano.data.ResultadoRegistro
import app.noplano.data.Taco
import app.noplano.domain.Ajustes
import app.noplano.domain.Conquistas
import app.noplano.domain.Situacao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class RepositorioTest {
    private lateinit var banco: Banco
    private lateinit var repo: Repositorio
    private val relogio = RelogioFalso()
    private val ajustes = AjustesFalsos()
    private var mudancas = 0
    private var planoId = 0L
    private var cafe = 0L
    private var almoco = 0L
    private var arrozId = 0L
    private var frangoId = 0L
    private val d = 1e-9

    @Before fun preparar() = runTest {
        banco = Banco.emMemoria(ApplicationProvider.getApplicationContext())
        repo = Repositorio(banco, ajustes, relogio)
        repo.aoMudar = { mudancas++ }
        repo.garantirInicial()
        planoId = repo.planos().first().first().id
        arrozId = repo.salvarAlimento(Comida.arroz.copy(id = 0))
        frangoId = repo.salvarAlimento(Comida.frango.copy(id = 0))
        cafe = repo.criarRefeicao(planoId, "Café da manhã", 7 * 60 + 30)
        almoco = repo.criarRefeicao(planoId, "Almoço", 12 * 60 + 30)
        val opAlmoco = repo.refeicaoAgora(almoco)!!.principal!!.opcao.id
        repo.adicionarItem(opAlmoco, arrozId, 150.0)
        repo.adicionarItem(opAlmoco, frangoId, 150.0)
        val opCafe = repo.refeicaoAgora(cafe)!!.principal!!.opcao.id
        repo.adicionarItem(opCafe, arrozId, 100.0)
        mudancas = 0
    }

    @After fun fechar() = banco.close()

    private val hoje get() = relogio.hoje()

    @Test fun `um toque registra a refeicao com os valores do plano`() = runTest {
        val r = repo.concluir(hoje, almoco)
        assertTrue(r is ResultadoRegistro.Novo)
        val dia = repo.estadoDia()
        assertEquals(192.0 + 238.5, dia.consumido.kcal, d)
        assertEquals(3.75 + 48.0, dia.consumido.prot, d)
        assertEquals(1, dia.concluidas)
        assertEquals(1, mudancas) // widget e lembretes foram avisados
    }

    @Test fun `tocar duas vezes nao duplica o registro`() = runTest {
        val a = repo.concluir(hoje, almoco)
        val b = repo.concluir(hoje, almoco)
        assertTrue(b is ResultadoRegistro.JaExistia)
        assertEquals(a.id, b.id)
        assertEquals(192.0 + 238.5, repo.estadoDia().consumido.kcal, d)
        assertEquals(1, mudancas)
    }

    @Test fun `desfazer remove o registro e os itens`() = runTest {
        val r = repo.concluir(hoje, almoco)
        repo.desfazer(r.id)
        val dia = repo.estadoDia()
        assertEquals(0.0, dia.consumido.kcal, d)
        assertEquals(0, dia.concluidas)
        assertNull(repo.registro(r.id))
        assertTrue(repo.concluir(hoje, almoco) is ResultadoRegistro.Novo)
    }

    @Test fun `consumo parcial guarda a fracao`() = runTest {
        repo.concluir(hoje, almoco, fracao = 0.5)
        val dia = repo.estadoDia()
        assertEquals((192.0 + 238.5) / 2, dia.consumido.kcal, d)
        assertEquals(Situacao.AJUSTADA, dia.refeicoes.first { it.id == almoco }.situacao)
    }

    @Test fun `corrigir quantidades de um registro recalcula o dia`() = runTest {
        val r = repo.concluir(hoje, almoco)
        val itens = repo.registro(r.id)!!.itens.map { ItemConsumo.de(it) }
        repo.corrigir(r.id, itens.map { if (it.nome.startsWith("Frango")) it.comGramas(100.0) else it })
        assertEquals(192.0 + 159.0, repo.estadoDia().consumido.kcal, d)
    }

    @Test fun `alternativa registra os itens dela e nao muda a meta`() = runTest {
        val alt = repo.criarOpcao(almoco, "Sem arroz")
        repo.adicionarItem(alt, frangoId, 200.0)
        val metaAntes = repo.estadoDia().meta
        repo.concluir(hoje, almoco, opcaoId = alt)
        val dia = repo.estadoDia()
        assertEquals(318.0, dia.consumido.kcal, d)
        assertEquals(metaAntes, dia.meta)
        assertEquals(Situacao.AJUSTADA, dia.refeicoes.first { it.id == almoco }.situacao)
    }

    @Test fun `registro fora do plano entra no total e nao conta como refeicao do plano`() = runTest {
        repo.registrarFora(hoje, "Lanche", listOf(ItemConsumo.de(Comida.banana, 70.0, 1.0)))
        val dia = repo.estadoDia()
        assertEquals(68.6, dia.consumido.kcal, d)
        assertEquals(0, dia.concluidas)
        assertEquals(1, dia.extras.size)
    }

    @Test fun `mudanca de data separa os registros de cada dia`() = runTest {
        repo.concluir(hoje, almoco)
        repo.adicionarAgua(hoje, 500)
        val ontem = hoje
        relogio.data = relogio.data.plusDays(1)
        val novo = repo.estadoDia()
        assertEquals(0.0, novo.consumido.kcal, d)
        assertEquals(0, novo.aguaMl)
        assertEquals(0, novo.concluidas)
        assertTrue(repo.concluir(hoje, almoco) is ResultadoRegistro.Novo)
        val anterior = repo.estadoDia(ontem)
        assertEquals(1, anterior.concluidas)
        assertEquals(500, anterior.aguaMl)
        assertEquals(1, anterior.semRegistro.size) // café do dia anterior
    }

    @Test fun `registro de dia anterior pode ser lancado depois`() = runTest {
        val ontem = hoje - 1
        repo.concluir(ontem, cafe)
        assertEquals(1, repo.estadoDia(ontem).concluidas)
        assertEquals(0, repo.estadoDia(hoje).concluidas)
    }

    @Test fun `agua soma e desfaz`() = runTest {
        repo.adicionarAgua(hoje, 250)
        repo.adicionarAgua(hoje, 500)
        assertEquals(750, repo.estadoDia().aguaMl)
        repo.removerUltimaAgua(hoje)
        assertEquals(250, repo.estadoDia().aguaMl)
        assertEquals(-1, repo.adicionarAgua(hoje, 0))
    }

    @Test fun `o fluxo do dia reage a um novo registro`() = runTest {
        val fluxo = repo.observarDia(hoje, flowOf(0L))
        assertEquals(0, fluxo.first().concluidas)
        repo.concluir(hoje, almoco)
        assertEquals(1, fluxo.first().concluidas)
    }

    @Test fun `editar o plano depois nao altera o que ja foi registrado`() = runTest {
        repo.concluir(hoje, almoco)
        val item = repo.refeicaoAgora(almoco)!!.principal!!.itens.first { it.alimento.id == frangoId }.item
        repo.atualizarItem(item.copy(gramas = 300.0))
        val dia = repo.estadoDia()
        assertEquals(192.0 + 238.5, dia.consumido.kcal, d)
        assertEquals(192.0 + 477.0 + 128.0, dia.meta.kcal, d)
        // O relatório guarda a meta do momento do registro.
        assertEquals(192.0 + 238.5 + 128.0, repo.historico(hoje, hoje).first().plano!!.kcal, d)
    }

    @Test fun `duplicar refeicao e plano copia itens e alternativas`() = runTest {
        val copia = repo.duplicarRefeicao(almoco)
        assertEquals(repo.refeicaoAgora(almoco)!!.macros, repo.refeicaoAgora(copia)!!.macros)
        val outro = repo.duplicarPlano(planoId)
        val refeicoes = repo.refeicoes(outro).first()
        assertEquals(3, refeicoes.size)
        assertEquals(repo.refeicoes(planoId).first().map { it.macros }, refeicoes.map { it.macros })
    }

    @Test fun `plano por dia da semana`() = runTest {
        val leve = repo.criarPlano("Descanso")
        val r = repo.criarRefeicao(leve, "Brunch", 10 * 60)
        repo.adicionarItem(repo.refeicaoAgora(r)!!.principal!!.opcao.id, frangoId, 100.0)
        val dow = relogio.data.dayOfWeek.value
        repo.definirDia(dow, leve)
        assertEquals("Descanso", repo.estadoDia().planoNome)
        assertEquals(159.0, repo.estadoDia().meta.kcal, d)
        assertEquals("Meu plano", repo.estadoDia(hoje + 1).planoNome)
        assertTrue(repo.excluirPlano(leve))
        assertEquals("Meu plano", repo.estadoDia().planoNome)
        assertFalse(repo.excluirPlano(planoId)) // o último plano não pode ser excluído
    }

    @Test fun `conquistas sao concedidas uma unica vez`() = runTest {
        repo.concluir(hoje, almoco)
        val primeira = repo.conquistas().first().map { it.chave }
        assertTrue(Conquistas.PRIMEIRO_DIA in primeira)
        repo.concluir(hoje, cafe)
        val depois = repo.conquistas().first().map { it.chave }
        assertEquals(1, depois.count { it == Conquistas.PRIMEIRO_DIA })
        assertTrue(Conquistas.DIA_COMPLETO in depois)
    }

    @Test fun `backup e restauracao devolvem exatamente os mesmos dados`() = runTest {
        repo.concluir(hoje, almoco, fracao = 0.5)
        repo.registrarFora(hoje, "Lanche", listOf(ItemConsumo.de(Comida.banana, 70.0, 1.0)))
        repo.adicionarAgua(hoje, 750)
        repo.anotarDificuldade(hoje, "Rotina corrida")
        ajustes.alterar { it.copy(metaAguaMl = 2500, vibrar = false) }
        val backup = Backup(banco, ajustes)
        val texto = backup.exportar(relogio.agoraMillis())
        val antes = repo.estadoDia()
        val conquistasAntes = repo.conquistas().first()

        // Apaga tudo e muda os ajustes, como em um celular novo.
        banco.clearAllTables()
        ajustes.alterar { Ajustes() }
        assertEquals(0, repo.estadoDia().total)

        backup.restaurar(texto)
        val depois = repo.estadoDia()
        assertEquals(antes.consumido, depois.consumido)
        assertEquals(antes.meta, depois.meta)
        assertEquals(antes.aguaMl, depois.aguaMl)
        assertEquals(antes.refeicoes.map { it.nome to it.situacao }, depois.refeicoes.map { it.nome to it.situacao })
        assertEquals(antes.extras.map { it.registro.nome }, depois.extras.map { it.registro.nome })
        assertEquals(conquistasAntes, repo.conquistas().first())
        assertEquals(2500, ajustes.atual().metaAguaMl)
        assertFalse(ajustes.atual().vibrar)
        assertEquals("Rotina corrida", repo.historico(hoje, hoje).first().dificuldade)
        assertEquals(texto, backup.exportar(relogio.agoraMillis()))
    }

    @Test fun `arquivo invalido nao apaga os dados`() = runTest {
        repo.concluir(hoje, almoco)
        val backup = Backup(banco, ajustes)
        for (ruim in listOf("isto não é json", "{\"formato\":\"outro\"}", "{\"formato\":\"noplano-backup\",\"versao\":99}")) {
            try { backup.restaurar(ruim); fail("deveria recusar") } catch (_: BackupInvalido) {}
        }
        assertEquals(1, repo.estadoDia().concluidas)
    }

    @Test fun `tabela TACO carrega com valores conferidos e sem duplicar`() = runTest {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        Taco.garantir(contexto, banco)
        val total = repo.totalTaco()
        assertTrue(total > 550)
        Taco.garantir(contexto, banco)
        assertEquals(total, repo.totalTaco())
        val arroz = repo.alimentoTaco(3)!!
        assertEquals("Arroz, tipo 1, cozido", arroz.nome)
        assertEquals(128.0, arroz.kcal100, d)
        assertEquals(2.5, arroz.prot100, d)
        assertEquals(28.1, arroz.carb100, d)
        val frango = repo.alimentoTaco(410)!!
        assertEquals(159.0, frango.kcal100, d)
        assertEquals(32.0, frango.prot100, d)
        assertNotNull(repo.buscarAlimentos("feijão").first().firstOrNull())
    }
}
