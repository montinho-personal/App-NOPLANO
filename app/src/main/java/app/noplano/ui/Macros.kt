package app.noplano.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.noplano.domain.Alvo
import app.noplano.domain.Comparacao
import app.noplano.domain.Fmt
import app.noplano.domain.Macros
import app.noplano.domain.Nutriente

private fun valor(n: Nutriente, v: Double) = if (n == Nutriente.KCAL) Fmt.kcal(v) else Fmt.gramas(v)

/**
 * Painel com calorias e macros do plano, comparados com o alvo (se houver).
 * A barra mostra o plano em relação ao alvo; passar do alvo não muda de cor.
 */
@Composable
fun PainelMacros(total: Macros, alvo: Alvo?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Comparacao.todas(total, alvo?.takeUnless { it.vazio }).forEach { c ->
            Column(Modifier.padding(vertical = 7.dp).semantics(mergeDescendants = true) {
                contentDescription = "${c.nutriente.rotulo}: ${valor(c.nutriente, c.plano)} ${c.nutriente.unidade}" +
                    (c.alvo?.let { ", alvo ${valor(c.nutriente, it)}, ${c.texto()}" } ?: "")
            }) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(c.nutriente.rotulo, style = Tipos.corpo, modifier = Modifier.weight(1f))
                    Text(valor(c.nutriente, c.plano), style = Tipos.numeroPequeno, color = if (c.nutriente == Nutriente.KCAL) Cores.destaque else Cores.texto)
                    Text(
                        (c.alvo?.let { " / ${valor(c.nutriente, it)}" } ?: "") + " ${c.nutriente.unidade}",
                        style = Tipos.unidade, modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                if (c.alvo != null) {
                    Espaco(5.dp)
                    BarraAlvo(c)
                    Espaco(3.dp)
                    Text(c.texto()!!.uppercase(), style = Tipos.rotulo, color = if (c.noAlvo) Cores.destaque else Cores.textoSuave)
                }
            }
        }
    }
}

/** Barra do plano em relação ao alvo. Até 100% enche; acima disso, uma marca mostra onde fica o alvo. */
@Composable
fun BarraAlvo(c: Comparacao, modifier: Modifier = Modifier) {
    val alvo = c.alvo ?: return
    val razao by animateFloatAsState(if (alvo > 0) (c.plano / alvo).toFloat() else 0f, tween(400), label = "alvo")
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        drawRect(Cores.trilho)
        if (razao <= 1f) {
            drawRect(Cores.destaque, size = Size(size.width * razao, size.height))
        } else {
            drawRect(Cores.destaque)
            val x = size.width / razao
            drawRect(Cores.fundo, Offset(x - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
        }
    }
}

/**
 * Linha compacta com os quatro valores e a diferença para o alvo.
 * Usada no topo do editor, sempre visível enquanto as quantidades mudam.
 */
@Composable
fun ResumoMacrosCompacto(titulo: String, total: Macros, alvo: Alvo?, modifier: Modifier = Modifier) {
    val comps = Comparacao.todas(total, alvo?.takeUnless { it.vazio })
    Column(modifier.fillMaxWidth()) {
        Rotulo(titulo, cor = Cores.destaque)
        Espaco(4.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            comps.forEach { c ->
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "${c.nutriente.rotulo} ${valor(c.nutriente, c.plano)} ${c.nutriente.unidade}" + (c.texto()?.let { ", $it" } ?: "")
                }) {
                    Rotulo(if (c.nutriente == Nutriente.KCAL) "Kcal" else c.nutriente.rotulo.take(4).let { if (it == "Carb") "Carbo" else it })
                    Text(
                        if (c.nutriente == Nutriente.KCAL) Fmt.kcal(c.plano) else Fmt.gramasInteiro(c.plano),
                        style = Tipos.numeroPequeno, maxLines = 1,
                    )
                    c.texto()?.let { Text(it, style = Tipos.pequeno.copy(fontSize = Tipos.rotulo.fontSize), color = if (c.noAlvo) Cores.destaque else Cores.textoSuave, maxLines = 1) }
                    c.alvo?.let { Espaco(3.dp); BarraAlvo(c) }
                }
            }
        }
    }
}

/** Linha "P 45 · C 38 · G 18" usada em listas. */
fun linhaMacros(m: Macros): String =
    "${Fmt.kcal(m.kcal)} kcal · P ${Fmt.gramas(m.prot)} · C ${Fmt.gramas(m.carb)} · G ${Fmt.gramas(m.gord)}"

/** Botões – e + em volta de um valor que também pode ser tocado para digitar. */
@Composable
fun Ajustador(
    texto: String,
    descricao: String,
    menos: () -> Unit,
    mais: () -> Unit,
    tocar: () -> Unit,
    modifier: Modifier = Modifier,
    podeDiminuir: Boolean = true,
) {
    Row(modifier.border(BorderStroke(1.dp, Cores.linha)), verticalAlignment = Alignment.CenterVertically) {
        BotaoPasso("–", "Diminuir $descricao", menos, podeDiminuir)
        Box(
            Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = tocar)
                .semantics { contentDescription = "$descricao: $texto. Toque para digitar." }
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) { Text(texto, style = Tipos.numeroPequeno, color = Cores.destaque, textAlign = TextAlign.Center, maxLines = 1) }
        BotaoPasso("+", "Aumentar $descricao", mais, true)
    }
}

@Composable
private fun BotaoPasso(simbolo: String, descricao: String, acao: () -> Unit, ativo: Boolean) {
    Box(
        Modifier.size(48.dp).background(Cores.superficieAlta).clickable(enabled = ativo, role = Role.Button, onClick = acao)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) { Text(simbolo, style = Tipos.numeroPequeno, color = if (ativo) Cores.texto else Cores.linha) }
}
