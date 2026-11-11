package app.noplano.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noplano.domain.Fmt

val Margem = 22.dp
val FormaCartao = RoundedCornerShape(2.dp)

/** Vibração curta de confirmação. */
fun View.vibrarConfirmacao(ligado: Boolean) {
    if (!ligado) return
    performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
}

@Composable
fun Rotulo(texto: String, modifier: Modifier = Modifier, cor: Color = Cores.textoSuave, estilo: TextStyle = Tipos.rotulo) {
    Text(texto.uppercase(), modifier, style = estilo, color = cor, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Anima um valor numérico até o novo alvo (contagem). */
@Composable
fun animarValor(alvo: Double, duracao: Int = 600): Double {
    val anim = remember { Animatable(alvo.toFloat()) }
    LaunchedEffect(alvo) { anim.animateTo(alvo.toFloat(), tween(duracao, easing = FastOutSlowInEasing)) }
    return anim.value.toDouble()
}

data class Segmento(val peso: Float, val estado: EstadoSegmento)
enum class EstadoSegmento { FEITO, PROXIMO, VAZIO }

/**
 * Barra do dia: um segmento inclinado por refeição, com largura proporcional às calorias.
 * Feito = cheio; próximo = hachurado; restante = vazio.
 */
@Composable
fun BarraDoDia(segmentos: List<Segmento>, modifier: Modifier = Modifier, altura: Dp = 16.dp, descricao: String = "") {
    val total = segmentos.sumOf { it.peso.toDouble() }.toFloat().takeIf { it > 0 } ?: 1f
    val preenchido by animateFloatAsState(
        segmentos.filter { it.estado == EstadoSegmento.FEITO }.sumOf { it.peso.toDouble() }.toFloat() / total,
        tween(600, easing = FastOutSlowInEasing), label = "barra",
    )
    Canvas(modifier.fillMaxWidth().height(altura).semantics { contentDescription = descricao }) {
        val vao = 4.dp.toPx()
        val inclinacao = size.height * 0.28f
        val util = size.width - inclinacao
        var x = 0f
        fun paralelogramo(x0: Float, x1: Float) = Path().apply {
            moveTo(x0 + inclinacao, 0f); lineTo(x1 + inclinacao, 0f); lineTo(x1, size.height); lineTo(x0, size.height); close()
        }
        if (segmentos.isEmpty()) { drawPath(paralelogramo(0f, util), Cores.trilho); return@Canvas }
        segmentos.forEach { s ->
            val largura = util * s.peso / total
            val x0 = x
            val x1 = (x + largura - vao).coerceAtLeast(x0 + 1f)
            drawPath(paralelogramo(x0, x1), Cores.trilho)
            when (s.estado) {
                EstadoSegmento.FEITO -> {
                    // Só pinta até onde a animação chegou.
                    val limite = util * preenchido
                    if (limite > x0) drawPath(paralelogramo(x0, minOf(x1, limite).coerceAtLeast(x0 + 1f)), Cores.destaque)
                }
                EstadoSegmento.PROXIMO -> {
                    val passo = 7.dp.toPx()
                    var hx = x0
                    while (hx < x1) {
                        drawPath(paralelogramo(hx, minOf(hx + 3.dp.toPx(), x1)), Cores.destaque)
                        hx += passo
                    }
                }
                EstadoSegmento.VAZIO -> Unit
            }
            x += largura
        }
    }
}

/** Barra fina de progresso de um macro. */
@Composable
fun BarraFina(fracao: Float, modifier: Modifier = Modifier, cor: Color = Cores.destaque) {
    val f by animateFloatAsState(fracao.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "fina")
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        drawRect(Cores.trilho)
        drawRect(cor, size = Size(size.width * f, size.height))
    }
}

@Composable
fun CartaoMacro(rotulo: String, consumido: Double, meta: Double, modifier: Modifier = Modifier) {
    val v = animarValor(consumido)
    Column(
        modifier
            .background(Cores.superficie)
            .drawBehind { drawRect(Cores.destaque, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$rotulo: ${Fmt.gramasInteiro(consumido)} de ${Fmt.gramasInteiro(meta)} gramas"
            },
    ) {
        Rotulo(rotulo)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Fmt.gramasInteiro(v), style = Tipos.numero)
            Text(" /${Fmt.gramasInteiro(meta)}", style = Tipos.unidade, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(8.dp))
        BarraFina(if (meta > 0) (consumido / meta).toFloat() else 0f)
    }
}

/** Botão principal: preto sobre o cartão de destaque, ou destaque sobre o fundo. */
@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    invertido: Boolean = false,
    ativo: Boolean = true,
) {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pressionado) 0.97f else 1f, tween(120), label = "botao")
    val fundo = if (invertido) Cores.sobreDestaque else Cores.destaque
    val cor = if (invertido) Cores.destaque else Cores.sobreDestaque
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala; alpha = if (ativo) 1f else 0.4f }
            .heightIn(min = 56.dp)
            .background(fundo)
            .clickable(fonte, indication = null, enabled = ativo, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto.uppercase(), style = Tipos.botao, color = cor) }
}

@Composable
fun BotaoContorno(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, cor: Color = Cores.texto, altura: Dp = 48.dp) {
    Box(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = altura)
            .border(BorderStroke(1.dp, Cores.linha))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(texto.uppercase(), style = Tipos.rotuloGrande, color = cor, maxLines = 1) }
}

@Composable
fun ChipEscolha(texto: String, selecionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .background(if (selecionado) Cores.destaque else Color.Transparent)
            .border(BorderStroke(1.dp, if (selecionado) Cores.destaque else Cores.linha))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(texto.uppercase(), style = Tipos.rotuloGrande, color = if (selecionado) Cores.sobreDestaque else Cores.texto, maxLines = 1) }
}

@Composable
fun Cartao(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), conteudo: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().background(Cores.superficie).padding(padding)) { conteudo() }
}

/** Cartão de destaque com o canto inferior direito cortado. */
@Composable
fun CartaoDestaque(modifier: Modifier = Modifier, conteudo: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(CutCornerShape(bottomEnd = 18.dp)).background(Cores.destaque)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
    ) { conteudo() }
}

@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier, acao: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Rotulo(texto, Modifier.weight(1f), cor = Cores.destaque, estilo = Tipos.rotuloGrande)
        acao?.invoke(this)
    }
}

@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    numerico: Boolean = false,
    sufixo: String? = null,
    umaLinha: Boolean = true,
) {
    Column(modifier) {
        Rotulo(rotulo)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).background(Cores.superficieAlta).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = valor, onValueChange = aoMudar, singleLine = umaLinha,
                textStyle = Tipos.corpo.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified.let { Tipos.corpo.fontSize }),
                cursorBrush = SolidColor(Cores.destaque),
                keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Decimal else KeyboardType.Text),
                modifier = Modifier.weight(1f).semantics { contentDescription = rotulo },
            )
            if (sufixo != null) Text(sufixo, style = Tipos.unidade, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun LinhaDivisoria(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Cores.trilho))
}

@Composable
fun Espaco(altura: Dp) = Spacer(Modifier.height(altura))

@Composable
fun EspacoH(largura: Dp) = Spacer(Modifier.width(largura))

/** Linha com rótulo à esquerda e valor à direita. */
@Composable
fun LinhaValor(rotulo: String, valor: String, modifier: Modifier = Modifier, destaque: Boolean = false) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = Tipos.corpo, modifier = Modifier.weight(1f))
        Text(valor, style = Tipos.numeroPequeno, color = if (destaque) Cores.destaque else Cores.texto)
    }
}
