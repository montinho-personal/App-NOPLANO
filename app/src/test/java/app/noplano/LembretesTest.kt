package app.noplano

import app.noplano.domain.Ajustes
import app.noplano.domain.Lembretes
import app.noplano.domain.TipoLembrete
import app.noplano.domain.montarDia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LembretesTest {
    private val cafe = refeicao(1, "Café da manhã", 7 * 60 + 30, Comida.banana to 140.0)
    private val almoco = refeicao(2, "Almoço", 12 * 60 + 30, Comida.arroz to 150.0)
    private val jantar = refeicao(3, "Jantar", 19 * 60 + 30, Comida.frango to 120.0)
    private val plano = listOf(cafe, almoco, jantar)
    private val a = Ajustes()

    private fun dia(feitas: List<app.noplano.data.RefeicaoCompleta> = emptyList(), agua: Int = 0) =
        montarDia(100, "Plano", plano, feitas.map { registroDe(it, 100) }, agua, a.metaAguaMl, agoraMin = 6 * 60)

    @Test fun `cada refeicao pendente tem aviso antes e aviso de pendente`() {
        val l = Lembretes.doDia(dia(), a.copy(avisarAgua = false, avisarResumo = false))
        assertEquals(6, l.size)
        val almocoAntes = l.first { it.refeicaoId == 2L && it.tipo == TipoLembrete.ANTES }
        assertEquals(12 * 60 + 20, almocoAntes.quandoMin)
        val almocoPend = l.first { it.refeicaoId == 2L && it.tipo == TipoLembrete.PENDENTE }
        assertEquals(13 * 60 + 15, almocoPend.quandoMin)
    }

    @Test fun `refeicao registrada nao gera lembrete`() {
        val l = Lembretes.doDia(dia(feitas = listOf(almoco)), a)
        assertTrue(l.none { it.refeicaoId == 2L })
        assertTrue(l.any { it.refeicaoId == 3L })
    }

    @Test fun `lembrete agendado deixa de valer se a refeicao for registrada antes`() {
        val agendado = Lembretes.doDia(dia(), a).first { it.refeicaoId == 2L && it.tipo == TipoLembrete.PENDENTE }
        assertTrue(Lembretes.deveExibir(agendado, dia(), a, exibidosHoje = 0))
        assertFalse(Lembretes.deveExibir(agendado, dia(feitas = listOf(almoco)), a, exibidosHoje = 0))
    }

    @Test fun `nada no periodo de silencio`() {
        val cedo = a.copy(silencioFimMin = 8 * 60) // café 7h30 cai no silêncio
        val l = Lembretes.doDia(dia(), cedo)
        assertTrue(l.none { it.refeicaoId == 1L && it.tipo == TipoLembrete.ANTES })
        assertTrue(l.all { !cedo.emSilencio(it.quandoMin) })
    }

    @Test fun `silencio que atravessa a meia-noite`() {
        assertTrue(a.emSilencio(23 * 60))
        assertTrue(a.emSilencio(3 * 60))
        assertFalse(a.emSilencio(12 * 60))
        assertFalse(a.copy(silencioInicioMin = 0, silencioFimMin = 0).emSilencio(3 * 60))
    }

    @Test fun `teto diario bloqueia tudo menos o resumo`() {
        val todos = Lembretes.doDia(dia(), a)
        val refeicao = todos.first { it.tipo == TipoLembrete.ANTES }
        val resumo = todos.first { it.tipo == TipoLembrete.RESUMO }
        assertFalse(Lembretes.deveExibir(refeicao, dia(), a, exibidosHoje = a.tetoDiario))
        assertTrue(Lembretes.deveExibir(resumo, dia(), a, exibidosHoje = a.tetoDiario))
    }

    @Test fun `agua so lembra quando o consumo esta abaixo do ritmo`() {
        val semAgua = Lembretes.doDia(dia(agua = 0), a).filter { it.tipo == TipoLembrete.AGUA }
        assertTrue(semAgua.isNotEmpty())
        val emDia = Lembretes.doDia(dia(agua = 3000), a).filter { it.tipo == TipoLembrete.AGUA }
        assertTrue(emDia.isEmpty())
        // Com 1,5 L ao meio-dia a pessoa está adiantada: só sobram lembretes do fim do dia.
        val metade = Lembretes.doDia(dia(agua = 1500), a).filter { it.tipo == TipoLembrete.AGUA }
        assertTrue(metade.all { it.quandoMin > 14 * 60 })
    }

    @Test fun `agua esperada cresce ao longo do dia e nao passa da meta`() {
        assertEquals(0, Lembretes.aguaEsperada(7 * 60, a))
        assertTrue(Lembretes.aguaEsperada(15 * 60, a) in 1..a.metaAguaMl)
        assertTrue(Lembretes.aguaEsperada(15 * 60, a) < Lembretes.aguaEsperada(20 * 60, a))
        assertTrue(Lembretes.aguaEsperada(23 * 60 + 59, a) <= a.metaAguaMl)
    }

    @Test fun `proximo lembrete e o primeiro depois de agora`() {
        val so = a.copy(avisarAgua = false)
        assertEquals(7 * 60 + 20, Lembretes.proximo(dia(), so, 6 * 60)?.quandoMin)
        assertEquals(19 * 60 + 20, Lembretes.proximo(dia(feitas = listOf(cafe, almoco)), so, 14 * 60)?.quandoMin)
        assertNull(Lembretes.proximo(dia(feitas = plano), so.copy(avisarResumo = false), 14 * 60))
    }

    @Test fun `categorias desligadas nao geram lembretes`() {
        val off = a.copy(avisarAntes = false, avisarPendente = false, avisarAgua = false, avisarResumo = false)
        assertTrue(Lembretes.doDia(dia(), off).isEmpty())
    }
}
