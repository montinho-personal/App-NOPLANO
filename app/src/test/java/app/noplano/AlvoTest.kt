package app.noplano

import app.noplano.domain.Alvo
import app.noplano.domain.Comparacao
import app.noplano.domain.Macros
import app.noplano.domain.Nutriente
import app.noplano.domain.Passo
import app.noplano.ui.plano.passo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlvoTest {
    private val plano = Macros(kcal = 1864.4, prot = 201.3, carb = 162.0, gord = 42.6)

    @Test fun `compara cada nutriente com o alvo`() {
        val c = Comparacao.todas(plano, Alvo(kcal = 2000.0, prot = 180.0, carb = 162.2, gord = null)).associateBy { it.nutriente }
        assertEquals("faltam 136 kcal", c.getValue(Nutriente.KCAL).texto())
        assertEquals("+21,3 g", c.getValue(Nutriente.PROT).texto())
        assertEquals("no alvo", c.getValue(Nutriente.CARB).texto())
        assertTrue(c.getValue(Nutriente.CARB).noAlvo)
        assertNull(c.getValue(Nutriente.GORD).texto())
        assertNull(c.getValue(Nutriente.GORD).diferenca)
    }

    @Test fun `sem alvo nao ha diferenca`() {
        assertTrue(Comparacao.todas(plano, null).all { it.diferenca == null && !it.noAlvo })
        assertTrue(Alvo().vazio)
        assertFalse(Alvo(prot = 1.0).vazio)
    }

    @Test fun `passo em gramas acompanha o tamanho da quantidade`() {
        assertEquals(50.0, Passo.aplicar(45.0, 1, false), 1e-9)
        assertEquals(40.0, Passo.aplicar(45.0, -1, false), 1e-9)
        assertEquals(160.0, Passo.aplicar(150.0, 1, false), 1e-9)
        assertEquals(140.0, Passo.aplicar(150.0, -1, false), 1e-9)
        assertEquals(25.0, Passo.aplicar(20.0, 1, false), 1e-9)
        assertEquals(19.0, Passo.aplicar(20.0, -1, false), 1e-9)
        assertEquals(0.0, Passo.aplicar(1.0, -1, false), 1e-9)
        assertEquals(0.0, Passo.aplicar(0.0, -1, false), 1e-9)
    }

    @Test fun `quantidade quebrada se alinha ao passo`() {
        assertEquals(130.0, Passo.aplicar(123.0, 1, false), 1e-9)
        assertEquals(120.0, Passo.aplicar(123.0, -1, false), 1e-9)
    }

    @Test fun `passo por unidade vai de um em um e usa meia unidade no fim`() {
        assertEquals(3.0, Passo.aplicar(2.0, 1, true), 1e-9)
        assertEquals(1.0, Passo.aplicar(2.0, -1, true), 1e-9)
        assertEquals(0.5, Passo.aplicar(1.0, -1, true), 1e-9)
        assertEquals(1.0, Passo.aplicar(0.5, 1, true), 1e-9)
        assertEquals(0.0, Passo.aplicar(0.5, -1, true), 1e-9)
    }

    @Test fun `item em unidades muda gramas junto`() {
        val ovo = Comida.banana.copy(nome = "Ovo", unidadeNome = "ovo", unidadeGramas = 50.0)
        val r = refeicao(1, "R", 600, ovo to 100.0)
        val item = r.principal!!.itens.first().let { it.copy(item = it.item.copy(unidades = 2.0)) }
        val mais = item.passo(1)
        assertEquals(3.0, mais.unidades!!, 1e-9)
        assertEquals(150.0, mais.gramas, 1e-9)
        val emGramas = r.principal!!.itens.first().passo(1)
        assertNull(emGramas.unidades)
        assertEquals(110.0, emGramas.gramas, 1e-9)
    }
}
