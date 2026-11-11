package app.noplano.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.noplano.MainActivity
import app.noplano.R
import app.noplano.app
import app.noplano.domain.Ajustes
import app.noplano.domain.EstadoDia
import app.noplano.domain.Fmt
import app.noplano.domain.Situacao
import kotlinx.coroutines.flow.MutableStateFlow

/** Tudo o que o widget mostra, já em texto. Calculado a partir do estado do dia. */
data class DadosWidget(
    val kcal: String,
    val metaKcal: String,
    val proteinas: String,
    val agua: String,
    val progresso: String,
    val proximaTitulo: String?,
    val proximaHora: String?,
    val proximaId: Long?,
    /** Peso e estado de cada segmento da barra: 2 = feito, 1 = próximo, 0 = vazio. */
    val segmentos: List<Pair<Float, Int>>,
    val completo: Boolean,
    val temPlano: Boolean,
    val mostrar: Ajustes,
) {
    companion object {
        fun de(dia: EstadoDia, a: Ajustes): DadosWidget {
            val p = dia.proxima
            return DadosWidget(
                kcal = Fmt.kcal(dia.consumido.kcal),
                metaKcal = Fmt.kcal(dia.meta.kcal),
                proteinas = "${Fmt.gramasInteiro(dia.consumido.prot)}/${Fmt.gramasInteiro(dia.meta.prot)} g",
                agua = "${Fmt.litros(dia.aguaMl)}/${Fmt.litros(dia.metaAguaMl)} L",
                progresso = "${dia.concluidas} DE ${dia.total}",
                proximaTitulo = p?.nome,
                proximaHora = p?.let { Fmt.hora(it.minutos) },
                proximaId = p?.id,
                segmentos = dia.refeicoes.map {
                    it.plano.macros.kcal.toFloat().coerceAtLeast(1f) to when {
                        it.feita -> 2
                        it.situacao == Situacao.PROXIMA -> 1
                        else -> 0
                    }
                },
                completo = dia.completo,
                temPlano = dia.temPlano,
                mostrar = a,
            )
        }
    }
}

object Widgets {
    /** Muda a cada atualização para os widgets recarregarem os dados. */
    val versao = MutableStateFlow(0)

    suspend fun atualizar(context: Context) {
        versao.value++
        WidgetCompacto().updateAll(context)
        WidgetExpandido().updateAll(context)
    }

    suspend fun carregar(context: Context): DadosWidget {
        val repo = context.app.repo
        return DadosWidget.de(repo.estadoDia(), repo.ajustes.atual())
    }
}

private val amarelo = Color(0xFFFFD60A)
private val preto = Color(0xFF090909)
private val cinza = Color(0xFF9A9A9A)
private val trilho = Color(0xFF2A2A2A)
private val chaveRefeicao = ActionParameters.Key<Long>("refeicao")

class WidgetCompacto : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { Conteudo(expandido = false) }
}

class WidgetExpandido : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { Conteudo(expandido = true) }
}

class WidgetCompactoReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = WidgetCompacto() }
class WidgetExpandidoReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = WidgetExpandido() }

class ConcluirCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = context.app.repo
        parameters[chaveRefeicao]?.let { runCatching { repo.concluir(repo.relogio.hoje(), it) } }
        Widgets.atualizar(context)
    }
}

class AguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = context.app.repo
        repo.adicionarAgua(repo.relogio.hoje(), 250)
        Widgets.atualizar(context)
    }
}

@Composable
private fun Conteudo(expandido: Boolean) {
    val context = LocalContext.current
    val versao by Widgets.versao.collectAsState()
    var dados by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<DadosWidget?>(null) }
    androidx.compose.runtime.LaunchedEffect(versao) { dados = runCatching { Widgets.carregar(context) }.getOrNull() }
    val tamanho = LocalSize.current
    val d = dados
    Box(
        GlanceModifier.fillMaxSize().background(preto).clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java))).padding(12.dp),
    ) {
        if (d == null) { Rotulo("NO PLANO", amarelo); return@Box }
        Column(GlanceModifier.fillMaxSize()) {
            val m = d.mostrar
            if (m.widgetCalorias) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val altura = if (expandido) 40 else 34
                    Image(
                        ImageProvider(textoImpacto(context, d.kcal, altura, Color.White)),
                        contentDescription = "${d.kcal} de ${d.metaKcal} quilocalorias",
                        modifier = GlanceModifier.height(altura.dp),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Rotulo("/ ${d.metaKcal} KCAL", cinza)
                }
            }
            if (m.widgetProgresso && d.temPlano) {
                Spacer(GlanceModifier.height(6.dp))
                val largura = (tamanho.width - 24.dp).coerceAtLeast(40.dp)
                Image(
                    ImageProvider(barra(context, d.segmentos, largura, 10.dp)),
                    contentDescription = "${d.progresso} refeições",
                    modifier = GlanceModifier.fillMaxWidth().height(10.dp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Rotulo("${d.progresso} REFEIÇÕES", cinza)
            }
            if (m.widgetProteinas || m.widgetAgua) {
                Spacer(GlanceModifier.height(4.dp))
                Row {
                    if (m.widgetProteinas) { Rotulo("PROT ", cinza); Rotulo(d.proteinas, Color.White) }
                    if (m.widgetProteinas && m.widgetAgua) Spacer(GlanceModifier.width(10.dp))
                    if (m.widgetAgua) { Rotulo("ÁGUA ", cinza); Rotulo(d.agua, Color.White) }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (m.widgetProxima) {
                when {
                    !d.temPlano -> Rotulo("TOQUE PARA MONTAR SEU PLANO", amarelo)
                    d.completo -> Rotulo("PLANO DO DIA CONCLUÍDO", amarelo)
                    d.proximaTitulo != null -> {
                        Rotulo("PRÓXIMA · ${d.proximaHora}", amarelo)
                        Text(
                            d.proximaTitulo.uppercase(), maxLines = 1,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    else -> Rotulo("ABRA PARA REGISTRAR", cinza)
                }
            }
            if (expandido) {
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    if (d.proximaId != null) {
                        Botao("CONCLUÍ", amarelo, preto, GlanceModifier.defaultWeight().clickable(
                            actionRunCallback<ConcluirCallback>(actionParametersOf(chaveRefeicao to d.proximaId))
                        ))
                        Spacer(GlanceModifier.width(8.dp))
                    }
                    Botao("+250 ML", Color(0xFF1E1E1E), Color.White, GlanceModifier.defaultWeight().clickable(actionRunCallback<AguaCallback>()))
                }
            }
        }
    }
}

@Composable
private fun Rotulo(texto: String, cor: Color) {
    Text(texto, maxLines = 1, style = TextStyle(color = ColorProvider(cor), fontSize = 11.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun Botao(texto: String, fundo: Color, cor: Color, modifier: GlanceModifier) {
    Box(modifier.height(48.dp).background(fundo), contentAlignment = Alignment.Center) {
        Text(texto, style = TextStyle(color = ColorProvider(cor), fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}

private fun Context.px(dp: Dp): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value, resources.displayMetrics)

/** Número na fonte do app (o Glance só oferece fontes do sistema), desenhado como imagem. */
private fun textoImpacto(context: Context, texto: String, alturaDp: Int, cor: Color): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.barlowc_extrabold_italic)
        textSize = context.px(alturaDp.dp) * 1.18f
        color = cor.toArgb()
    }
    val altura = context.px(alturaDp.dp).toInt().coerceAtLeast(1)
    val largura = (paint.measureText(texto) + context.px(6.dp)).toInt().coerceAtLeast(1)
    return Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawText(texto, 0f, altura - context.px(alturaDp.dp) * 0.12f, paint)
    }
}

/** Barra do dia em segmentos inclinados, igual à do app. */
private fun barra(context: Context, segmentos: List<Pair<Float, Int>>, largura: Dp, altura: Dp): Bitmap {
    val w = context.px(largura).toInt().coerceAtLeast(1)
    val h = context.px(altura).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val inclinacao = h * 0.28f
    val util = w - inclinacao
    val vao = context.px(3.dp)
    val total = segmentos.sumOf { it.first.toDouble() }.toFloat().takeIf { it > 0 } ?: 1f
    fun forma(x0: Float, x1: Float) = Path().apply {
        moveTo(x0 + inclinacao, 0f); lineTo(x1 + inclinacao, 0f); lineTo(x1, h.toFloat()); lineTo(x0, h.toFloat()); close()
    }
    var x = 0f
    for ((peso, estado) in segmentos) {
        val l = util * peso / total
        val x1 = (x + l - vao).coerceAtLeast(x + 1f)
        paint.color = (if (estado == 2) amarelo else trilho).toArgb()
        canvas.drawPath(forma(x, x1), paint)
        if (estado == 1) {
            paint.color = amarelo.toArgb()
            var hx = x
            while (hx < x1) { canvas.drawPath(forma(hx, minOf(hx + context.px(2.dp), x1)), paint); hx += context.px(5.dp) }
        }
        x += l
    }
    return bmp
}
