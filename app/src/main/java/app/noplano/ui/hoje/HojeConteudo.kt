package app.noplano.ui.hoje

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.noplano.data.RegistroComItens
import app.noplano.domain.EstadoDia
import app.noplano.domain.EstadoRefeicao
import app.noplano.domain.Fmt
import app.noplano.domain.Situacao
import app.noplano.ui.BarraDoDia
import app.noplano.ui.BarraFina
import app.noplano.ui.BotaoContorno
import app.noplano.ui.BotaoPrincipal
import app.noplano.ui.Cartao
import app.noplano.ui.CartaoDestaque
import app.noplano.ui.CartaoMacro
import app.noplano.ui.Cores
import app.noplano.ui.Espaco
import app.noplano.ui.EspacoH
import app.noplano.ui.EstadoSegmento
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.Segmento
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import app.noplano.ui.animarValor
import app.noplano.ui.dataCurta
import app.noplano.ui.resumo

class HojeAcoes(
    val concluir: (EstadoRefeicao, opcaoId: Long?) -> Unit = { _, _ -> },
    val abrirRefeicao: (EstadoRefeicao, opcaoId: Long?) -> Unit = { _, _ -> },
    val abrirExtra: (RegistroComItens) -> Unit = {},
    val agua: (Int) -> Unit = {},
    val aguaOutro: () -> Unit = {},
    val foraDoPlano: () -> Unit = {},
    val irPara: (Long) -> Unit = {},
    val abrirAjustes: () -> Unit = {},
    val abrirProgresso: () -> Unit = {},
    val abrirPlano: () -> Unit = {},
    val dificuldade: () -> Unit = {},
    val criarExemplo: () -> Unit = {},
)

@Composable
fun HojeConteudo(ui: HojeUi, acoes: HojeAcoes, modifier: Modifier = Modifier) {
    val dia = ui.dia
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 18.dp, bottom = 28.dp)) {
        Cabecalho(ui, acoes)
        Espaco(14.dp)
        SemanaFaixa(ui, acoes)
        Espaco(18.dp)
        Calorias(dia)
        Espaco(20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CartaoMacro("Proteínas", dia.consumido.prot, dia.meta.prot, Modifier.weight(1f))
            CartaoMacro("Carbos", dia.consumido.carb, dia.meta.carb, Modifier.weight(1f))
            CartaoMacro("Gorduras", dia.consumido.gord, dia.meta.gord, Modifier.weight(1f))
        }
        Espaco(16.dp)
        when {
            !dia.temPlano -> SemPlano(ui, acoes)
            dia.completo -> DiaConcluido(dia)
            else -> {
                if (dia.semRegistro.isNotEmpty()) { Retomada(dia, acoes); Espaco(14.dp) }
                dia.proxima?.let { Proxima(it, acoes) }
            }
        }
        Espaco(14.dp)
        Agua(dia, acoes)
        Espaco(14.dp)
        Text(ui.mensagem.uppercase(), style = Tipos.rotuloGrande, color = Cores.textoSuave)
        if (dia.temPlano) {
            TituloSecao("Refeições do dia")
            dia.refeicoes.forEach { LinhaRefeicao(it, acoes) }
            dia.extras.forEach { LinhaExtra(it, acoes) }
        }
        Espaco(14.dp)
        BotaoContorno("+ Registrar fora do plano", acoes.foraDoPlano, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Cabecalho(ui: HojeUi, acoes: HojeAcoes) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Rotulo(dataCurta(ui.dia.data), cor = Cores.destaque)
            Text(
                if (ui.ehHoje) "MEU DIA" else "DIA ANTERIOR", style = Tipos.titulo,
                modifier = Modifier.semantics { heading() },
            )
        }
        BotaoContorno(
            if (ui.diasAcompanhados == 1) "1 dia no plano" else "${ui.diasAcompanhados} dias no plano",
            acoes.abrirProgresso, altura = 40.dp,
        )
        EspacoH(8.dp)
        BotaoContorno("Ajustes", acoes.abrirAjustes, altura = 40.dp)
    }
}

/** Sete dias da semana: mostra quais foram acompanhados e serve para ir a um dia anterior. */
@Composable
private fun SemanaFaixa(ui: HojeUi, acoes: HojeAcoes) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ui.semana.forEach { d ->
            val selecionado = d.data == ui.dia.data
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clickable(enabled = !d.futuro, role = Role.Tab) { acoes.irPara(d.data) }
                    .semantics { contentDescription = dataCurta(d.data) + if (d.acompanhado) ", acompanhado" else "" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(6.dp).background(
                        when {
                            d.acompanhado -> Cores.destaque
                            else -> Cores.trilho
                        }
                    )
                )
                Espaco(6.dp)
                Text(
                    d.letra, style = Tipos.rotuloGrande,
                    color = when { selecionado -> Cores.destaque; d.futuro -> Cores.linha; else -> Cores.textoSuave },
                )
                if (selecionado) Box(Modifier.padding(top = 3.dp).size(width = 14.dp, height = 2.dp).background(Cores.destaque))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Calorias(dia: EstadoDia) {
    val kcal = animarValor(dia.consumido.kcal)
    Column(Modifier.semantics(mergeDescendants = true) {
        contentDescription = "${Fmt.kcal(dia.consumido.kcal)} de ${Fmt.kcal(dia.meta.kcal)} quilocalorias. " +
            "${dia.concluidas} de ${dia.total} refeições. Faltam ${Fmt.kcal(dia.faltamKcal)}."
    }) {
        FlowRow(verticalArrangement = Arrangement.Bottom) {
            Text(Fmt.kcal(kcal), style = Tipos.heroi, modifier = Modifier.alignByBaseline())
            Text(" / ${Fmt.kcal(dia.meta.kcal)} KCAL", style = Tipos.unidade.copy(fontSize = Tipos.numeroPequeno.fontSize), modifier = Modifier.alignByBaseline())
        }
        Espaco(10.dp)
        BarraDoDia(dia.refeicoes.map {
            Segmento(
                it.plano.macros.kcal.toFloat().coerceAtLeast(1f),
                when { it.feita -> EstadoSegmento.FEITO; it.situacao == Situacao.PROXIMA -> EstadoSegmento.PROXIMO; else -> EstadoSegmento.VAZIO },
            )
        })
        Espaco(8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Rotulo("${dia.concluidas} de ${dia.total} refeições")
            if (dia.temPlano) Rotulo("Faltam ${Fmt.kcal(dia.faltamKcal)}", cor = Cores.destaque)
        }
    }
}

@Composable
private fun Proxima(r: EstadoRefeicao, acoes: HojeAcoes) {
    val opcoes = r.plano.opcoesOrdenadas
    var opcaoId by remember(r.id) { mutableStateOf(opcoes.firstOrNull()?.opcao?.id) }
    val opcao = opcoes.firstOrNull { it.opcao.id == opcaoId } ?: opcoes.firstOrNull()
    CartaoDestaque {
        Column {
            Rotulo("Próxima · ${Fmt.hora(r.minutos)}", cor = Cores.sobreDestaque)
            Text(r.nome.uppercase(), style = Tipos.tituloCartao, color = Cores.sobreDestaque)
            Espaco(4.dp)
            Text(
                opcao?.itens?.joinToString(" · ") { it.resumo() }?.ifBlank { null } ?: "Sem alimentos cadastrados",
                style = Tipos.corpoForte, color = Cores.sobreDestaque,
            )
            if (r.plano.refeicao.obs.isNotBlank()) Text(r.plano.refeicao.obs, style = Tipos.pequeno, color = Cores.sobreDestaque)
            if (opcoes.size > 1) {
                Espaco(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    opcoes.forEach { o ->
                        val sel = o.opcao.id == opcao?.opcao?.id
                        Box(
                            Modifier.defaultMinSize(minHeight = 40.dp)
                                .background(if (sel) Cores.sobreDestaque else Color.Transparent)
                                .drawBehind { if (!sel) drawRect(Cores.sobreDestaque, style = Stroke(1.dp.toPx())) }
                                .clickable(role = Role.Tab) { opcaoId = o.opcao.id }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(o.opcao.nome.uppercase(), style = Tipos.rotulo, color = if (sel) Cores.destaque else Cores.sobreDestaque) }
                    }
                }
            }
            Espaco(12.dp)
            BotaoPrincipal("Concluí refeição", { acoes.concluir(r, opcao?.opcao?.id) }, invertido = true)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { acoes.abrirRefeicao(r, opcao?.opcao?.id) },
                contentAlignment = Alignment.Center,
            ) {
                Text("COMI DIFERENTE · AJUSTAR", style = Tipos.rotulo, color = Cores.sobreDestaque, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

@Composable
private fun Retomada(dia: EstadoDia, acoes: HojeAcoes) {
    Column(
        Modifier.fillMaxWidth().background(Cores.superficie)
            .drawBehind { drawRect(Cores.destaque, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 8.dp)
    ) {
        Rotulo("Retomada", cor = Cores.destaque)
        Espaco(4.dp)
        Text(
            dia.proxima?.let { "Seu próximo passo é ${it.nome.replaceFirstChar { c -> c.lowercase() }}." }
                ?: "Seu próximo passo é a próxima refeição.",
            style = Tipos.corpoForte,
        )
        Text("Se você comeu, registre. Se não, tudo bem: o plano continua.", style = Tipos.pequeno)
        Espaco(8.dp)
        dia.semRegistro.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.nome, style = Tipos.corpo)
                    Rotulo("${Fmt.hora(r.minutos)} · sem registro")
                }
                BotaoContorno("Comi", { acoes.concluir(r, null) }, cor = Cores.destaque)
                EspacoH(6.dp)
                BotaoContorno("Ajustar", { acoes.abrirRefeicao(r, null) })
            }
        }
        Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = acoes.dificuldade), contentAlignment = Alignment.CenterStart) {
            Text("O QUE DIFICULTOU? (OPCIONAL)", style = Tipos.rotulo, textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
private fun DiaConcluido(dia: EstadoDia) {
    CartaoDestaque {
        Column {
            Rotulo("${dia.concluidas} de ${dia.total} refeições", cor = Cores.sobreDestaque)
            Text("PLANO DO DIA CONCLUÍDO", style = Tipos.tituloCartao, color = Cores.sobreDestaque)
            Espaco(8.dp)
        }
    }
}

@Composable
private fun SemPlano(ui: HojeUi, acoes: HojeAcoes) {
    Cartao {
        Column {
            Text("COMECE PELO PLANO", style = Tipos.tituloCartao)
            Espaco(6.dp)
            Text(
                "Cadastre suas refeições uma vez. Depois, cada dia é só tocar em CONCLUÍ REFEIÇÃO.",
                style = Tipos.corpoSuave,
            )
            Espaco(14.dp)
            BotaoPrincipal("Montar meu plano", acoes.abrirPlano)
            if (ui.boasVindas) {
                Espaco(8.dp)
                BotaoContorno("Ver com um plano de exemplo", acoes.criarExemplo, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Agua(dia: EstadoDia, acoes: HojeAcoes) {
    Column(Modifier.fillMaxWidth().background(Cores.superficie)) {
        Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                contentDescription = "Água: ${Fmt.litros(dia.aguaMl)} de ${Fmt.litros(dia.metaAguaMl)} litros"
            }) {
                Rotulo("Água")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Fmt.litros(dia.aguaMl), style = Tipos.numero)
                    Text(" / ${Fmt.litros(dia.metaAguaMl)} L", style = Tipos.unidade, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(150, 250, 500).forEach { ml -> BotaoContorno("+$ml", { acoes.agua(ml) }) }
                BotaoContorno("+ …", acoes.aguaOutro)
            }
        }
        BarraFina(if (dia.metaAguaMl > 0) dia.aguaMl.toFloat() / dia.metaAguaMl else 0f)
    }
}

@Composable
private fun Marca(situacao: Situacao?) {
    Canvas(Modifier.size(18.dp)) {
        val t = 2.dp.toPx()
        when (situacao) {
            Situacao.CONCLUIDA, Situacao.AJUSTADA -> drawRect(Cores.destaque)
            Situacao.PROXIMA -> {
                drawRect(Cores.destaque, style = Stroke(t))
                drawRect(Cores.destaque, Offset(size.width * 0.3f, size.height * 0.3f), Size(size.width * 0.4f, size.height * 0.4f))
            }
            Situacao.SEM_REGISTRO -> drawRect(Cores.textoSuave, style = Stroke(t, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
            else -> drawRect(Cores.linha, style = Stroke(t))
        }
    }
}

@Composable
private fun LinhaRefeicao(r: EstadoRefeicao, acoes: HojeAcoes) {
    val estado = when (r.situacao) {
        Situacao.CONCLUIDA -> "registrado"
        Situacao.AJUSTADA -> "registrado · ajustado"
        Situacao.PROXIMA -> "próxima"
        Situacao.FUTURA -> "planejado"
        Situacao.SEM_REGISTRO -> "sem registro"
    }
    val kcal = r.registro?.macros?.kcal ?: r.plano.macros.kcal
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { acoes.abrirRefeicao(r, null) }
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Marca(r.situacao)
        EspacoH(14.dp)
        Column(Modifier.weight(1f)) {
            Text(r.nome, style = Tipos.corpoForte, color = if (r.feita) Cores.texto else Cores.textoSuave)
            Rotulo("${Fmt.hora(r.minutos)} · $estado", cor = if (r.situacao == Situacao.PROXIMA) Cores.destaque else Cores.textoSuave)
        }
        Text(Fmt.kcal(kcal), style = Tipos.numeroPequeno, color = if (r.feita) Cores.texto else Cores.textoSuave)
        Text(" kcal", style = Tipos.unidade)
    }
}

@Composable
private fun LinhaExtra(reg: RegistroComItens, acoes: HojeAcoes) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { acoes.abrirExtra(reg) }.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Marca(Situacao.CONCLUIDA)
        EspacoH(14.dp)
        Column(Modifier.weight(1f)) {
            Text(reg.registro.nome, style = Tipos.corpoForte)
            Rotulo("fora do plano")
        }
        Text(Fmt.kcal(reg.macros.kcal), style = Tipos.numeroPequeno)
        Text(" kcal", style = Tipos.unidade)
    }
}
