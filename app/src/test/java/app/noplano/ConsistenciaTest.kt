package app.noplano

import app.noplano.domain.Conquistas
import app.noplano.domain.Consistencia
import app.noplano.domain.DiaHistorico
import app.noplano.domain.Macros
import app.noplano.domain.MarcaRegistro
import app.noplano.domain.Padroes
import app.noplano.domain.RefeicaoRef
import app.noplano.domain.Relatorios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsistenciaTest {
    private fun dia(data: Long, feitas: Int = 0, plano: Int = 3, agua: Int = 0, kcal: Double = 0.0, atrasoMin: Int = 0) = DiaHistorico(
        data = data,
        registros = (1..feitas).map { MarcaRegistro(it.toLong(), "Refeição $it", 420 + it * 180, 420 + it * 180 + atrasoMin) },
        consumido = Macros(kcal = kcal),
        plano = if (feitas > 0) Macros(kcal = 2000.0) else null,
        refeicoesPlano = if (feitas > 0) plano else 0,
        aguaMl = agua, metaAguaMl = 3000,
    )

    /** "x" = dia acompanhado, "." = dia sem registro. */
    private fun serie(s: String, inicio: Long = 1) = s.mapIndexed { i, c -> dia(inicio + i, feitas = if (c == 'x') 2 else 0) }

    @Test fun `um dia fora nao apaga o total de dias acompanhados`() {
        val h = serie("xxx.xx")
        val r = Consistencia.resumo(h, h.last().data)
        assertEquals(5, r.diasAcompanhados)
        assertEquals(2, r.sequenciaAtual)
        assertEquals(3, r.melhorSequencia)
        assertEquals(1, r.retomadas)
    }

    @Test fun `hoje ainda sem registro nao quebra a sequencia`() {
        val h = serie("xxx.")
        assertEquals(3, Consistencia.resumo(h, h.last().data).sequenciaAtual)
    }

    @Test fun `pausa longa conta como uma unica retomada`() {
        val h = serie("xx....x..x")
        assertEquals(2, Consistencia.resumo(h, h.last().data).retomadas)
    }

    @Test fun `dias antes do primeiro registro nao contam como pausa`() {
        val h = serie("...xx")
        assertEquals(0, Consistencia.resumo(h, h.last().data).retomadas)
    }

    @Test fun `conquistas reconhecem registro retomada e agua`() {
        assertEquals(listOf(Conquistas.PRIMEIRO_DIA), Conquistas.avaliar(serie("x")))
        assertTrue(Conquistas.RETOMADA in Conquistas.avaliar(serie("x.x")))
        assertTrue(Conquistas.PRIMEIRA_SEMANA in Conquistas.avaliar(serie("xxx.xxxx")))
        assertFalse(Conquistas.DEZ_DIAS in Conquistas.avaliar(serie("xxxxxxxxx")))
        assertTrue(Conquistas.DEZ_DIAS in Conquistas.avaliar(serie("xxxxxxxxxx")))
        val comAgua = (1L..7L).map { dia(it, feitas = 1, agua = 500) }
        assertTrue(Conquistas.AGUA_7 in Conquistas.avaliar(comAgua))
        assertTrue(Conquistas.DIA_COMPLETO in Conquistas.avaliar(listOf(dia(1, feitas = 3, plano = 3))))
    }

    @Test fun `comer menos que a meta nao rende conquista`() {
        val pouco = listOf(dia(1, feitas = 1, plano = 3, kcal = 400.0))
        val completo = listOf(dia(1, feitas = 1, plano = 3, kcal = 2000.0))
        assertEquals(Conquistas.avaliar(completo), Conquistas.avaliar(pouco))
        assertFalse(Conquistas.DIA_COMPLETO in Conquistas.avaliar(pouco))
    }

    @Test fun `relatorio ignora dias sem registro nas medias`() {
        val h = listOf(dia(1, feitas = 3, kcal = 2000.0, agua = 3000), dia(2), dia(3, feitas = 1, kcal = 1000.0))
        val r = Relatorios.montar(h)
        assertEquals(3, r.dias)
        assertEquals(2, r.diasComRegistro)
        assertEquals(1500.0, r.mediaConsumido!!.kcal, 1e-9)
        assertEquals(2000.0, r.mediaPlano!!.kcal, 1e-9)
        assertEquals(4.0 / 6.0, r.pctRefeicoes!!, 1e-9)
        assertEquals(3000, r.mediaAguaMl)
        assertEquals(1, r.diasComAgua)
    }

    @Test fun `relatorio vazio nao inventa numeros`() {
        val r = Relatorios.montar(listOf(dia(1), dia(2)))
        assertEquals(0, r.diasComRegistro)
        assertNull(r.pctRefeicoes)
        assertNull(r.mediaConsumido)
        assertNull(r.mediaAguaMl)
    }

    @Test fun `pendencias por refeicao contam so dias acompanhados`() {
        val plano = (1..7).associateWith { listOf(RefeicaoRef(1, "Café", 450), RefeicaoRef(2, "Almoço", 750), RefeicaoRef(3, "Jantar", 1170)) }
        val h = listOf(dia(1, feitas = 2), dia(2), dia(3, feitas = 3))
        val p = Relatorios.montar(h, plano).pendencias
        assertEquals(1, p.size)
        assertEquals("Jantar", p[0].nome)
        assertEquals(1, p[0].semRegistro)
        assertEquals(2, p[0].dias)
    }

    @Test fun `padroes so aparecem com dados suficientes`() {
        val pouco = Padroes.observar(serie("xxxxx"), emptyMap(), 5)
        assertFalse(pouco.suficiente)
        assertEquals(9, pouco.diasFaltando)
        assertTrue(pouco.observacoes.isEmpty())
    }

    @Test fun `padrao de registro atrasado e apontado com contagem`() {
        val h = (1L..14L).map { dia(it, feitas = 1, atrasoMin = 90) }
        val r = Padroes.observar(h, emptyMap(), 14)
        assertTrue(r.suficiente)
        assertTrue(r.observacoes.any { "14 de 14" in it })
    }
}
