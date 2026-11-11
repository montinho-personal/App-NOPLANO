package app.noplano

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.noplano.data.Alimento
import app.noplano.data.Plano
import app.noplano.domain.Conquistas
import app.noplano.domain.Consistencia
import app.noplano.domain.DiaHistorico
import app.noplano.domain.Macros
import app.noplano.domain.MarcaRegistro
import app.noplano.domain.Mensagens
import app.noplano.domain.Padroes
import app.noplano.domain.RefeicaoRef
import app.noplano.domain.Relatorios
import app.noplano.domain.montarDia
import app.noplano.ui.Cores
import app.noplano.ui.TemaNoPlano
import app.noplano.ui.hoje.DiaDaSemanaUi
import app.noplano.ui.hoje.HojeAcoes
import app.noplano.ui.hoje.HojeConteudo
import app.noplano.ui.hoje.HojeUi
import app.noplano.ui.plano.PlanoAcoes
import app.noplano.ui.plano.PlanoConteudo
import app.noplano.ui.plano.PlanoUi
import app.noplano.ui.progresso.ProgressoConteudo
import app.noplano.ui.progresso.ProgressoUi
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Gera imagens das telas reais (as mesmas funções de interface usadas no app),
 * com dados de exemplo. As imagens vão para docs/telas.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TelasTest {
    private fun a(id: Long, nome: String, k: Double, p: Double, c: Double, g: Double, un: String? = null, ug: Double? = null) =
        Alimento(id = id, nome = nome, fonte = "TACO", kcal100 = k, prot100 = p, carb100 = c, gord100 = g, unidadeNome = un, unidadeGramas = ug)

    private val ovo = a(10, "Ovo, de galinha, inteiro, cozido", 146.0, 13.3, 0.6, 9.5)
    private val pao = a(11, "Pão, trigo, francês", 300.0, 8.0, 58.6, 3.1)
    private val iogurte = a(12, "Iogurte, natural", 51.0, 4.1, 1.9, 3.0)
    private val aveia = a(13, "Aveia, flocos, crua", 394.0, 13.9, 66.6, 8.5)
    private val feijao = a(14, "Feijão, carioca, cozido", 76.0, 4.8, 13.6, 0.5)
    private val batata = a(15, "Batata, doce, cozida", 77.0, 0.6, 18.4, 0.1)
    private val patinho = a(16, "Carne, bovina, patinho, grelhado", 219.0, 35.9, 0.0, 7.3)

    private val plano = listOf(
        refeicao(1, "Café da manhã", 7 * 60 + 30, ovo to 100.0, pao to 50.0, Comida.banana to 70.0),
        refeicao(2, "Lanche", 10 * 60, iogurte to 170.0, aveia to 30.0),
        refeicao(3, "Almoço", 12 * 60 + 30, Comida.arroz to 150.0, feijao to 100.0, Comida.frango to 150.0),
        refeicao(4, "Lanche da tarde", 16 * 60, iogurte to 170.0, Comida.banana to 70.0),
        refeicao(5, "Jantar", 19 * 60 + 30, batata to 150.0, patinho to 120.0),
    )
    private val hoje = LocalDate.of(2026, 11, 11).toEpochDay()
    private val segunda = hoje - 2

    private fun semana(acompanhados: Set<Int>) = (0..6).map {
        DiaDaSemanaUi(segunda + it, "STQQSSD"[it].toString(), it in acompanhados, segunda + it == hoje, segunda + it > hoje)
    }

    private fun tela(conteudo: @Composable () -> Unit): @Composable () -> Unit = {
        TemaNoPlano { Box(Modifier.fillMaxSize().background(Cores.fundo)) { conteudo() } }
    }

    private fun hojeUi(feitas: Int, agoraMin: Int, agua: Int) = montarDia(
        hoje, "Meu plano", plano, plano.take(feitas).map { registroDe(it, hoje) }, agua, 3000, agoraMin,
    ).let { HojeUi(it, true, semana(setOf(0, 1) + if (feitas > 0) setOf(2) else emptySet()), 12, Mensagens.doDia(it)) }

    @Test @Config(qualifiers = "w393dp-h1250dp-420dpi")
    fun hoje() = captureRoboImage("../docs/telas/01-meu-dia.png", content = tela { HojeConteudo(hojeUi(3, 14 * 60, 2100), HojeAcoes()) })

    @Test @Config(qualifiers = "w393dp-h1300dp-420dpi")
    fun retomada() {
        val dia = montarDia(hoje, "Meu plano", plano, listOf(registroDe(plano[0], hoje)), 750, 3000, 15 * 60)
        captureRoboImage("../docs/telas/02-retomada.png", content = tela {
            HojeConteudo(HojeUi(dia, true, semana(setOf(0, 2)), 9, Mensagens.doDia(dia)), HojeAcoes())
        })
    }

    @Test @Config(qualifiers = "w393dp-h900dp-420dpi")
    fun primeiroUso() {
        val dia = montarDia(hoje, "Meu plano", emptyList(), emptyList(), 0, 3000, 9 * 60)
        captureRoboImage("../docs/telas/03-primeiro-uso.png", content = tela {
            HojeConteudo(HojeUi(dia, true, semana(emptySet()), 0, Mensagens.doDia(dia), boasVindas = true), HojeAcoes())
        })
    }

    private val alvoPlano = Plano(1, "Meu plano", alvoKcal = 1800.0, alvoProt = 150.0, alvoCarb = 180.0, alvoGord = 55.0)

    @Test @Config(qualifiers = "w393dp-h1300dp-420dpi")
    fun editor() {
        val whey = Alimento(id = 20, nome = "Whey Protein Concentrado (TOP)", marca = "Growth", fonte = "Rótulo",
            kcal100 = 122 * 10 / 3.0, prot100 = 23 * 10 / 3.0, carb100 = 2.7 * 10 / 3.0, gord100 = 2.1 * 10 / 3.0)
        val amendoim = a(21, "Amendoim, torrado, salgado", 606.0, 22.5, 18.7, 54.0)
        val r = refeicao(1, "Refeição 1", 7 * 60 + 30, aveia to 45.0, whey to 45.0, amendoim to 20.0)
        val outras = plano.drop(1).map { it.macros }.fold(Macros.ZERO) { x, y -> x + y }
        captureRoboImage("../docs/telas/06-editar-refeicao.png", content = tela {
            app.noplano.ui.plano.EditorConteudo(r, r.principal, outras + r.macros, alvoPlano.alvo, app.noplano.ui.plano.EditorAcoes())
        })
    }

    @Test @Config(qualifiers = "w393dp-h900dp-420dpi")
    fun alvo() = captureRoboImage("../docs/telas/07-alvo.png", content = tela {
        app.noplano.ui.plano.DialogoAlvo(alvoPlano.alvo, {}, {})
    })

    @Test @Config(qualifiers = "w393dp-h1500dp-420dpi")
    fun plano() = captureRoboImage("../docs/telas/04-plano.png", content = tela {
        PlanoConteudo(PlanoUi(listOf(Plano(1, "Meu plano"), Plano(2, "Descanso")), alvoPlano, setOf(1, 2, 3, 4, 5), plano), PlanoAcoes())
    })

    @Test @Config(qualifiers = "w393dp-h2300dp-420dpi")
    fun progresso() {
        val padrao = "xxx.xxxxx.xxxx..xxxxxxxxx.xxxx"
        val historico = padrao.mapIndexed { i, c ->
            val d = hoje - padrao.length + 1 + i
            if (c == '.') DiaHistorico(d) else {
                val feitas = if (i % 4 == 0) 3 else if (i % 3 == 0) 4 else 5
                DiaHistorico(
                    d, (1..feitas).map { MarcaRegistro(it.toLong(), plano[it - 1].refeicao.nome, plano[it - 1].refeicao.minutos, plano[it - 1].refeicao.minutos + if (it == 5) 80 else 10) },
                    Macros(1950.0 * feitas / 5, 150.0 * feitas / 5, 190.0 * feitas / 5, 55.0 * feitas / 5),
                    Macros(1987.0, 152.0, 196.0, 58.0), 5, if (i % 5 == 0) 0 else 2400 + (i % 3) * 300, 3000,
                )
            }
        }
        val refs = (1..7).associateWith { plano.map { RefeicaoRef(it.refeicao.id, it.refeicao.nome, it.refeicao.minutos) } }
        captureRoboImage("../docs/telas/05-progresso.png", content = tela {
            ProgressoConteudo(
                ProgressoUi(
                    hoje, Consistencia.resumo(historico, hoje), historico.filter { it.data >= segunda } + (1..4).map { DiaHistorico(hoje + it) },
                    30, Relatorios.montar(historico, refs),
                    setOf(Conquistas.PRIMEIRO_DIA, Conquistas.DIA_COMPLETO, Conquistas.PRIMEIRA_SEMANA, Conquistas.DEZ_DIAS, Conquistas.RETOMADA, Conquistas.AGUA_7),
                    Padroes.observar(historico, refs, hoje),
                ), {},
            )
        })
    }
}
