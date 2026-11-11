package app.noplano.domain

import kotlin.math.abs

/**
 * Alvo de macros digitado pelo usuário (por exemplo, o que a nutricionista passou).
 * O app nunca calcula alvo: só compara o plano com ele. Cada campo é opcional.
 */
data class Alvo(
    val kcal: Double? = null,
    val prot: Double? = null,
    val carb: Double? = null,
    val gord: Double? = null,
) {
    val vazio: Boolean get() = kcal == null && prot == null && carb == null && gord == null
}

enum class Nutriente(val rotulo: String, val unidade: String) {
    KCAL("Calorias", "kcal"), PROT("Proteínas", "g"), CARB("Carboidratos", "g"), GORD("Gorduras", "g");

    fun de(m: Macros): Double = when (this) { KCAL -> m.kcal; PROT -> m.prot; CARB -> m.carb; GORD -> m.gord }
    fun de(a: Alvo): Double? = when (this) { KCAL -> a.kcal; PROT -> a.prot; CARB -> a.carb; GORD -> a.gord }
}

/** Comparação de um nutriente do plano com o alvo. */
data class Comparacao(val nutriente: Nutriente, val plano: Double, val alvo: Double?) {
    /** Positivo: o plano passa do alvo. Negativo: falta. */
    val diferenca: Double? get() = alvo?.let { plano - it }

    /** Diferença pequena demais para importar: até 1 kcal ou 0,5 g (o que o arredondamento esconde). */
    val noAlvo: Boolean get() = diferenca?.let { abs(it) < if (nutriente == Nutriente.KCAL) 1.0 else 0.5 } ?: false

    /** Texto neutro da diferença: "faltam 12 g", "+30 kcal", "no alvo". */
    fun texto(): String? {
        val d = diferenca ?: return null
        if (noAlvo) return "no alvo"
        val v = if (nutriente == Nutriente.KCAL) Fmt.kcal(abs(d)) else Fmt.gramas(abs(d))
        return if (d < 0) "faltam $v ${nutriente.unidade}" else "+$v ${nutriente.unidade}"
    }

    companion object {
        fun todas(plano: Macros, alvo: Alvo?): List<Comparacao> =
            Nutriente.entries.map { Comparacao(it, it.de(plano), alvo?.let(it::de)) }
    }
}

/** Passo dos botões – e + ao ajustar uma quantidade. */
object Passo {
    fun gramas(atual: Double): Double = when {
        atual < 20 -> 1.0
        atual < 60 -> 5.0
        else -> 10.0
    }

    /** Novo valor depois de um toque em – ou +, sem passar de zero e alinhado ao passo. */
    fun aplicar(atual: Double, sentido: Int, porUnidade: Boolean): Double {
        if (porUnidade) {
            val passo = if (atual <= 1.0 && sentido < 0 || atual < 1.0) 0.5 else 1.0
            return (atual + sentido * passo).coerceAtLeast(0.0)
        }
        val passo = gramas(if (sentido < 0) (atual - 0.001).coerceAtLeast(0.0) else atual)
        val alinhado = if (sentido > 0) (kotlin.math.floor(atual / passo + 1e-9) + 1) * passo
        else (kotlin.math.ceil(atual / passo - 1e-9) - 1) * passo
        return alinhado.coerceAtLeast(0.0)
    }
}
