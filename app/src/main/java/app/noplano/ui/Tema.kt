package app.noplano.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.noplano.R

/** Paleta "Pulso". Uma única cor de destaque; o resto é preto, branco e cinza. */
object Cores {
    val fundo = Color(0xFF090909)
    val superficie = Color(0xFF141414)
    val superficieAlta = Color(0xFF1E1E1E)
    val linha = Color(0xFF333333)
    val trilho = Color(0xFF242424)
    val texto = Color(0xFFFFFFFF)
    val textoSuave = Color(0xFF9A9A9A)
    val destaque = Color(0xFFFFD60A)
    val sobreDestaque = Color(0xFF0A0A0A)
    val erro = Color(0xFFFF7A6B)
}

private val condensada = FontFamily(
    Font(R.font.barlowc_semibold, FontWeight.SemiBold),
    Font(R.font.barlowc_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)
private val barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

object Tipos {
    private fun impacto(tamanho: Int) = TextStyle(
        fontFamily = condensada, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
        fontSize = tamanho.sp, lineHeight = (tamanho * 0.92).sp, color = Cores.texto, fontFeatureSettings = "tnum",
    )

    /** Número principal da tela. */
    val heroi = impacto(104).copy(
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
            androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
            androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
        ),
    )
    val numeroGrande = impacto(40)
    val numero = impacto(30)
    val numeroPequeno = impacto(22)
    val titulo = impacto(34)
    val tituloCartao = impacto(28)
    val botao = impacto(20).copy(letterSpacing = 0.06.em)

    /** Rótulo em caixa alta, espaçado. */
    val rotulo = TextStyle(
        fontFamily = condensada, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        letterSpacing = 0.14.em, color = Cores.textoSuave,
    )
    val rotuloGrande = rotulo.copy(fontSize = 15.sp, letterSpacing = 0.1.em)
    val unidade = TextStyle(
        fontFamily = condensada, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        color = Cores.textoSuave, fontFeatureSettings = "tnum",
    )
    val corpo = TextStyle(fontFamily = barlow, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = Cores.texto)
    val corpoForte = corpo.copy(fontWeight = FontWeight.SemiBold)
    val corpoSuave = corpo.copy(color = Cores.textoSuave)
    val pequeno = corpo.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Cores.textoSuave)
}

@Composable
fun TemaNoPlano(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cores.destaque, onPrimary = Cores.sobreDestaque,
            secondary = Cores.destaque, onSecondary = Cores.sobreDestaque,
            background = Cores.fundo, onBackground = Cores.texto,
            surface = Cores.superficie, onSurface = Cores.texto,
            surfaceVariant = Cores.superficieAlta, onSurfaceVariant = Cores.textoSuave,
            surfaceContainer = Cores.superficie, surfaceContainerHigh = Cores.superficieAlta,
            surfaceContainerHighest = Cores.superficieAlta, surfaceContainerLow = Cores.superficie,
            outline = Cores.linha, outlineVariant = Cores.linha, error = Cores.erro,
        ),
        typography = Typography(
            bodyLarge = Tipos.corpo, bodyMedium = Tipos.corpo, bodySmall = Tipos.pequeno,
            labelLarge = Tipos.rotuloGrande, labelMedium = Tipos.rotulo, labelSmall = Tipos.rotulo,
            titleLarge = Tipos.tituloCartao, titleMedium = Tipos.corpoForte, headlineSmall = Tipos.tituloCartao,
        ),
        content = conteudo,
    )
}
