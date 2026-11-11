package app.noplano.ui.progresso

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.noplano.app
import app.noplano.domain.Conquistas
import app.noplano.domain.Consistencia
import app.noplano.domain.DiaHistorico
import app.noplano.domain.Fmt
import app.noplano.domain.Padroes
import app.noplano.domain.Relatorio
import app.noplano.domain.Relatorios
import app.noplano.domain.ResumoConsistencia
import app.noplano.ui.BarraFina
import app.noplano.ui.Cartao
import app.noplano.ui.ChipEscolha
import app.noplano.ui.Cores
import app.noplano.ui.Espaco
import app.noplano.ui.EspacoH
import app.noplano.ui.LinhaDivisoria
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import app.noplano.ui.dataCurta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgressoUi(
    val hoje: Long,
    val resumo: ResumoConsistencia,
    /** Segunda a domingo da semana atual. */
    val semana: List<DiaHistorico>,
    val periodo: Int,
    val relatorio: Relatorio,
    val conquistas: Set<String>,
    val padroes: Padroes.Resultado,
)

class ProgressoViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {
    private val repo = aplicacao.app.repo
    private val periodo = MutableStateFlow(7)

    val ui: StateFlow<ProgressoUi?> = combine(repo.diasComRegistro(), repo.conquistas(), periodo) { _, conquistas, p ->
        val hoje = repo.relogio.hoje()
        val total = repo.historicoTotal()
        val plano = repo.planoPorDiaSemana()
        val segunda = hoje - (LocalDate.ofEpochDay(hoje).dayOfWeek.value - 1)
        ProgressoUi(
            hoje = hoje,
            resumo = Consistencia.resumo(total, hoje),
            semana = repo.historico(segunda, segunda + 6),
            periodo = p,
            relatorio = Relatorios.montar(repo.historico(hoje - p + 1, hoje), plano),
            conquistas = conquistas.map { it.chave }.toSet(),
            padroes = Padroes.observar(total, plano, hoje),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun periodo(dias: Int) { periodo.value = dias }
}

@Composable
fun ProgressoTela(vm: ProgressoViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ui?.let { ProgressoConteudo(it, vm::periodo) } ?: Box(Modifier.fillMaxSize())
}

@Composable
fun ProgressoConteudo(ui: ProgressoUi, mudarPeriodo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val r = ui.resumo
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 18.dp, bottom = 28.dp)) {
        Rotulo("Constância, não perfeição", cor = Cores.destaque)
        Text("PROGRESSO", style = Tipos.titulo, modifier = Modifier.semantics { heading() })
        Espaco(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Numero("No plano", r.diasAcompanhados.toString(), Modifier.weight(1f), destaque = true)
            Numero("Sequência", r.sequenciaAtual.toString(), Modifier.weight(1f))
            Numero("Retomadas", r.retomadas.toString(), Modifier.weight(1f))
        }
        Espaco(6.dp)
        Text(
            "No plano: total de dias com registro. Nunca diminui. Melhor sequência: ${r.melhorSequencia} " +
                if (r.melhorSequencia == 1) "dia." else "dias.",
            style = Tipos.pequeno,
        )

        TituloSecao("Semana em movimento")
        Semana(ui)
        Espaco(6.dp)
        Text("Altura da barra: refeições do plano registradas. Quadrado: água registrada.", style = Tipos.pequeno)

        TituloSecao("Relatório")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipEscolha("7 dias", ui.periodo == 7, { mudarPeriodo(7) })
            ChipEscolha("30 dias", ui.periodo == 30, { mudarPeriodo(30) })
        }
        Espaco(10.dp)
        Relatorio(ui.relatorio)

        TituloSecao("Padrões")
        if (!ui.padroes.suficiente) {
            Text(
                "Ainda não há dados suficientes. Faltam ${ui.padroes.diasFaltando} dias acompanhados para o app apontar padrões.",
                style = Tipos.corpoSuave,
            )
        } else if (ui.padroes.observacoes.isEmpty()) {
            Text("Nenhum padrão que mereça atenção por enquanto.", style = Tipos.corpoSuave)
        } else ui.padroes.observacoes.forEach {
            Row(Modifier.padding(vertical = 6.dp)) {
                Box(Modifier.padding(top = 7.dp).size(8.dp).background(Cores.destaque))
                EspacoH(12.dp)
                Text(it, style = Tipos.corpo)
            }
        }

        TituloSecao("Conquistas")
        Conquistas.todas.forEach { c ->
            val tem = c.chave in ui.conquistas
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                contentDescription = "${c.titulo}. ${c.descricao} " + if (tem) "Alcançada." else "Ainda não alcançada."
            }, verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(18.dp)) {
                    if (tem) drawRect(Cores.destaque) else drawRect(Cores.linha, style = Stroke(2.dp.toPx()))
                }
                EspacoH(14.dp)
                Column {
                    Text(c.titulo.uppercase(), style = Tipos.rotuloGrande, color = if (tem) Cores.texto else Cores.textoSuave)
                    Text(c.descricao, style = Tipos.pequeno)
                }
            }
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, modifier: Modifier = Modifier, destaque: Boolean = false) {
    Column(
        modifier.background(Cores.superficie)
            .drawBehind { drawRect(if (destaque) Cores.destaque else Cores.linha, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$rotulo: $valor" },
    ) {
        Rotulo(rotulo)
        Text(valor, style = Tipos.numeroGrande, color = if (destaque) Cores.destaque else Cores.texto)
    }
}

@Composable
private fun Semana(ui: ProgressoUi) {
    Row(Modifier.fillMaxWidth().height(132.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.semana.forEachIndexed { i, d ->
            val futuro = d.data > ui.hoje
            val fracao = (d.adesaoPlano ?: if (d.acompanhado) 1.0 else 0.0).toFloat().coerceIn(0f, 1f)
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = dataCurta(d.data) + when {
                        futuro -> ", ainda não chegou"
                        d.acompanhado -> ", ${d.refeicoesFeitas} de ${d.refeicoesPlano} refeições" + if (d.aguaRegistrada) ", água registrada" else ""
                        else -> ", sem registro"
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.weight(1f).fillMaxWidth()) {
                    drawRect(Cores.trilho)
                    if (d.acompanhado) {
                        val h = size.height * fracao.coerceAtLeast(0.08f)
                        drawRect(Cores.destaque, Offset(0f, size.height - h), Size(size.width, h))
                    } else if (!futuro && d.data != ui.hoje) {
                        // Sem registro: contorno tracejado, sem julgamento.
                        drawRect(Cores.linha, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                    }
                }
                Espaco(6.dp)
                Box(Modifier.size(8.dp).background(if (d.aguaRegistrada) Cores.texto else Cores.trilho))
                Espaco(4.dp)
                Text("STQQSSD"[i].toString(), style = Tipos.rotuloGrande, color = if (d.data == ui.hoje) Cores.destaque else Cores.textoSuave)
            }
        }
    }
}

@Composable
private fun Relatorio(r: Relatorio) {
    Cartao {
        Column {
            Linha("Dias com registro", "${r.diasComRegistro} de ${r.dias}")
            LinhaDivisoria()
            Linha("Refeições do plano registradas", r.pctRefeicoes?.let { "${Fmt.kcal(it * 100)}%" } ?: "—")
            LinhaDivisoria()
            Linha("Água (média nos dias com registro)", r.mediaAguaMl?.let { "${Fmt.litros(it)} L" } ?: "—")
        }
    }
    val c = r.mediaConsumido
    val p = r.mediaPlano
    if (c != null) {
        Espaco(8.dp)
        Cartao {
            Column {
                Rotulo("Média por dia · feito / planejado")
                Espaco(8.dp)
                Comparacao("Calorias", c.kcal, p?.kcal, "kcal")
                Comparacao("Proteínas", c.prot, p?.prot, "g")
                Comparacao("Carboidratos", c.carb, p?.carb, "g")
                Comparacao("Gorduras", c.gord, p?.gord, "g")
            }
        }
        Espaco(8.dp)
        Cartao {
            Column {
                Rotulo("Calorias por dia")
                Espaco(10.dp)
                Grafico(r)
                Espaco(6.dp)
                Text("Linha: planejado. Espaços vazios são dias sem registro, não dias sem comer.", style = Tipos.pequeno)
            }
        }
    } else {
        Espaco(8.dp)
        Text("Sem registros neste período.", style = Tipos.corpoSuave)
    }
    if (r.pendencias.isNotEmpty()) {
        Espaco(8.dp)
        Cartao {
            Column {
                Rotulo("Refeições que mais ficam sem registro")
                r.pendencias.take(4).forEach {
                    Linha("${it.nome} · ${Fmt.hora(it.minutos)}", "${it.semRegistro} de ${it.dias} dias")
                }
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = Tipos.corpo, modifier = Modifier.weight(1f))
        Text(valor, style = Tipos.numeroPequeno)
    }
}

@Composable
private fun Comparacao(nome: String, consumido: Double, plano: Double?, unidade: String) {
    Column(Modifier.padding(vertical = 6.dp).semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(nome, style = Tipos.corpo, modifier = Modifier.weight(1f))
            Text(Fmt.kcal(consumido), style = Tipos.numeroPequeno)
            Text(" / ${plano?.let { Fmt.kcal(it) } ?: "—"} $unidade", style = Tipos.unidade, modifier = Modifier.padding(bottom = 2.dp))
        }
        Espaco(4.dp)
        BarraFina(if (plano != null && plano > 0) (consumido / plano).toFloat() else 0f)
    }
}

@Composable
private fun Grafico(r: Relatorio) {
    val dias = r.porDia
    val maximo = (dias.maxOfOrNull { maxOf(it.consumido.kcal, it.plano?.kcal ?: 0.0) } ?: 0.0).coerceAtLeast(1.0) * 1.1
    Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "Gráfico de calorias por dia no período" }) {
        val vao = if (dias.size > 10) 2.dp.toPx() else 6.dp.toPx()
        val largura = (size.width - vao * (dias.size - 1)) / dias.size
        dias.forEachIndexed { i, d ->
            val x = i * (largura + vao)
            if (d.acompanhado) {
                val h = (size.height * d.consumido.kcal / maximo).toFloat()
                drawRect(Cores.destaque, Offset(x, size.height - h), Size(largura, h))
                d.plano?.let { p ->
                    val y = size.height - (size.height * p.kcal / maximo).toFloat()
                    drawLine(Cores.texto, Offset(x, y), Offset(x + largura, y), 2.dp.toPx())
                }
            } else {
                drawRect(Cores.trilho, Offset(x, size.height - 3.dp.toPx()), Size(largura, 3.dp.toPx()))
            }
        }
    }
}
