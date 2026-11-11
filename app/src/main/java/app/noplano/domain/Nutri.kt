package app.noplano.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Energia e macronutrientes de uma quantidade de alimento. Valores exatos; arredonda-se só ao exibir. */
data class Macros(val kcal: Double = 0.0, val prot: Double = 0.0, val carb: Double = 0.0, val gord: Double = 0.0) {
    operator fun plus(o: Macros) = Macros(kcal + o.kcal, prot + o.prot, carb + o.carb, gord + o.gord)
    operator fun times(f: Double) = Macros(kcal * f, prot * f, carb * f, gord * f)

    companion object {
        val ZERO = Macros()

        /** Valores por 100 g (ou 100 ml) aplicados a uma quantidade. */
        fun de(kcal100: Double, prot100: Double, carb100: Double, gord100: Double, quantidade: Double): Macros {
            val f = quantidade / 100.0
            return Macros(kcal100 * f, prot100 * f, carb100 * f, gord100 * f)
        }
    }
}

fun Iterable<Macros>.somar(): Macros = fold(Macros.ZERO) { a, b -> a + b }

/**
 * Regras de arredondamento do app (meio para cima, sempre na exibição):
 * calorias em inteiro, macros com até uma casa, água em litros com uma casa.
 */
object Fmt {
    private val br = DecimalFormatSymbols(Locale("pt", "BR"))
    private val inteiro = DecimalFormat("#,##0", br)
    private val umaCasa = DecimalFormat("#,##0.#", br)
    private val umaCasaFixa = DecimalFormat("#,##0.0", br)
    // Sem separador de milhar: é o formato que volta para os campos de digitação.
    private val editavel = DecimalFormat("0.#", br)

    fun arredondar(v: Double, casas: Int): BigDecimal =
        BigDecimal.valueOf(v).setScale(casas, RoundingMode.HALF_UP)

    fun kcal(v: Double): String = inteiro.format(arredondar(v, 0))
    fun gramasInteiro(v: Double): String = inteiro.format(arredondar(v, 0))
    fun gramas(v: Double): String = umaCasa.format(arredondar(v, 1))
    fun litros(ml: Int): String = umaCasaFixa.format(arredondar(ml / 1000.0, 1))
    /** Quantidade para mostrar e para pôr em campo editável: "1500", "12,5". Nunca usa ponto de milhar. */
    fun quantidade(v: Double): String = editavel.format(arredondar(v, 1))

    fun hora(minutos: Int): String = "%02dh%02d".format(minutos / 60, minutos % 60)

    /** Aceita "12,5" ou "12.5". Retorna null se não for número válido e não negativo. */
    fun lerNumero(texto: String): Double? {
        var t = texto.trim().replace(" ", "")
        // "1.800" ou "1.800,5": ponto como separador de milhar (padrão brasileiro).
        if (Regex("""^\d{1,3}(\.\d{3})+(,\d+)?$""").matches(t)) t = t.replace(".", "")
        return t.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
    }
}
