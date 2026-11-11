package app.noplano

import app.noplano.data.ItemConsumo
import app.noplano.domain.Fmt
import app.noplano.domain.Macros
import app.noplano.domain.Situacao
import app.noplano.domain.montarDia
import app.noplano.domain.somar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculoTest {
    private val d = 1e-9

    @Test fun `calorias e macros de uma quantidade seguem a regra de tres por 100 g`() {
        val m = Comida.arroz.macros(150.0)
        assertEquals(192.0, m.kcal, d)
        assertEquals(3.75, m.prot, d)
        assertEquals(42.15, m.carb, d)
        assertEquals(0.3, m.gord, d)
    }

    @Test fun `refeicao soma os itens`() {
        val r = refeicao(1, "Almoço", 750, Comida.arroz to 150.0, Comida.frango to 150.0)
        assertEquals(192.0 + 238.5, r.macros.kcal, d)
        assertEquals(3.75 + 48.0, r.macros.prot, d)
    }

    @Test fun `porcao parcial e proporcional`() {
        val r = refeicao(1, "Almoço", 750, Comida.arroz to 150.0, Comida.frango to 150.0)
        val metade = r.principal!!.itens.map { ItemConsumo.de(it, 0.5).macros }.somar()
        assertEquals(r.macros.kcal / 2, metade.kcal, d)
        assertEquals(r.macros.prot / 2, metade.prot, d)
    }

    @Test fun `alterar a quantidade recalcula proporcionalmente`() {
        val item = ItemConsumo.de(Comida.frango, 150.0)
        val novo = item.comGramas(100.0)
        assertEquals(159.0, novo.macros.kcal, d)
        assertEquals(32.0, novo.macros.prot, d)
        assertEquals(100.0, novo.gramas, d)
    }

    @Test fun `medida caseira acompanha a quantidade`() {
        val item = ItemConsumo.de(Comida.banana, 140.0, unidades = 2.0)
        assertEquals("unidade", item.unidadeNome)
        assertEquals(1.0, item.comGramas(70.0).unidades!!, d)
    }

    @Test fun `arredonda so na exibicao e meio para cima`() {
        assertEquals("193", Fmt.kcal(192.5))
        assertEquals("192", Fmt.kcal(192.49))
        assertEquals("1.450", Fmt.kcal(1450.0))
        assertEquals("3,8", Fmt.gramas(3.75))
        assertEquals("42", Fmt.gramas(42.04))
        assertEquals("2,1", Fmt.litros(2100))
        assertEquals("3,0", Fmt.litros(3000))
        assertEquals("0,2", Fmt.litros(150))
    }

    @Test fun `a soma exata nao acumula erro de arredondamento`() {
        // Dez itens de 0,04 g: arredondados um a um dariam 0; somados dão 0,4.
        val total = List(10) { Macros(prot = 0.04) }.somar()
        assertEquals("0,4", Fmt.gramas(total.prot))
    }

    @Test fun `leitura de numero aceita virgula e rejeita invalido`() {
        assertEquals(12.5, Fmt.lerNumero("12,5")!!, d)
        assertEquals(12.5, Fmt.lerNumero(" 12.5 ")!!, d)
        assertEquals(1800.0, Fmt.lerNumero("1.800")!!, d)
        assertEquals(1800.5, Fmt.lerNumero("1.800,5")!!, d)
        assertEquals(1.8, Fmt.lerNumero("1.8")!!, d)
        assertNull(Fmt.lerNumero("abc"))
        assertNull(Fmt.lerNumero("-3"))
    }

    @Test fun `numero que vai para um campo volta igual ao ser lido`() {
        for (v in listOf(1800.0, 1500.0, 45.0, 12.5, 0.5, 2500.0)) assertEquals(v, Fmt.lerNumero(Fmt.quantidade(v))!!, d)
        assertEquals("1800", Fmt.quantidade(1800.0))
    }

    @Test fun `hora formatada`() {
        assertEquals("07h05", Fmt.hora(425))
        assertEquals("12h30", Fmt.hora(750))
    }

    // ---- Estado do dia ----

    private val cafe = refeicao(1, "Café da manhã", 7 * 60 + 30, Comida.banana to 140.0)
    private val almoco = refeicao(2, "Almoço", 12 * 60 + 30, Comida.arroz to 150.0, Comida.frango to 150.0)
    private val jantar = refeicao(3, "Jantar", 19 * 60 + 30, Comida.arroz to 100.0, Comida.frango to 120.0)
    private val plano = listOf(cafe, almoco, jantar)

    @Test fun `meta do dia e a soma do plano`() {
        val dia = montarDia(100, "Plano", plano, emptyList(), 0, 3000, agoraMin = 6 * 60)
        assertEquals(plano.map { it.macros }.somar(), dia.meta)
        assertEquals(0.0, dia.consumido.kcal, d)
        assertEquals(3, dia.total)
    }

    @Test fun `proxima refeicao e a primeira sem registro`() {
        val dia = montarDia(100, "Plano", plano, listOf(registroDe(cafe, 100)), 0, 3000, agoraMin = 9 * 60)
        assertEquals("Almoço", dia.proxima?.nome)
        assertEquals(1, dia.concluidas)
        assertEquals(Situacao.CONCLUIDA, dia.refeicoes[0].situacao)
        assertEquals(Situacao.FUTURA, dia.refeicoes[2].situacao)
        assertEquals(cafe.macros.kcal, dia.consumido.kcal, d)
    }

    @Test fun `refeicao com horario passado fica sem registro e a proxima segue em frente`() {
        // 15h: café (7h30) e almoço (12h30) passaram da tolerância de 90 min.
        val dia = montarDia(100, "Plano", plano, emptyList(), 0, 3000, agoraMin = 15 * 60)
        assertEquals(listOf("Café da manhã", "Almoço"), dia.semRegistro.map { it.nome })
        assertEquals("Jantar", dia.proxima?.nome)
    }

    @Test fun `dentro da tolerancia a refeicao continua sendo a proxima`() {
        val dia = montarDia(100, "Plano", plano, listOf(registroDe(cafe, 100)), 0, 3000, agoraMin = 13 * 60 + 30)
        assertEquals("Almoço", dia.proxima?.nome)
        assertTrue(dia.semRegistro.isEmpty())
    }

    @Test fun `registro parcial conta como feito e aparece como ajustado`() {
        val dia = montarDia(100, "Plano", plano, listOf(registroDe(almoco, 100, fracao = 0.5)), 0, 3000, agoraMin = 13 * 60)
        assertEquals(Situacao.AJUSTADA, dia.refeicoes[1].situacao)
        assertEquals(almoco.macros.kcal / 2, dia.consumido.kcal, d)
        assertEquals(dia.meta.kcal - almoco.macros.kcal / 2, dia.faltamKcal, d)
    }

    @Test fun `passar da meta nao gera valor negativo em faltam`() {
        val regs = plano.map { registroDe(it, 100) } + registroDe(almoco, 100, id = 9999).let {
            it.copy(registro = it.registro.copy(refeicaoId = null, nome = "Fora do plano"))
        }
        val dia = montarDia(100, "Plano", plano, regs, 0, 3000, agoraMin = 22 * 60)
        assertEquals(0.0, dia.faltamKcal, d)
        assertEquals(1, dia.extras.size)
        assertTrue(dia.completo)
    }

    @Test fun `dia passado mostra pendentes como sem registro`() {
        val dia = montarDia(99, "Plano", plano, listOf(registroDe(cafe, 99)), 0, 3000, agoraMin = null, passado = true)
        assertNull(dia.proxima)
        assertEquals(2, dia.semRegistro.size)
    }
}
