package app.noplano.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Dieta escrita em arquivo (assets/plano.json), para poder ser alterada fora do celular
 * e chegar junto com a atualização do app. Cada mudança sobe a [versao].
 */
@Serializable
data class PlanoArquivo(
    val versao: Int,
    val notas: String = "",
    val alimentos: List<AlimentoArquivo> = emptyList(),
    val planos: List<PlanoDoArquivo> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun ler(texto: String): PlanoArquivo = json.decodeFromString(serializer(), texto)

        fun doApp(context: Context): PlanoArquivo? = runCatching {
            ler(context.assets.open("plano.json").bufferedReader().use { it.readText() })
        }.getOrNull()
    }
}

/**
 * Alimento citado pelo arquivo. Com [taco], usa os valores da TACO (e pode acrescentar uma
 * medida caseira). Sem [taco], os valores vêm do rótulo, na [porcao] informada.
 */
@Serializable
data class AlimentoArquivo(
    val chave: String,
    val taco: Int? = null,
    val nome: String? = null,
    val marca: String = "",
    val fonte: String = "Rótulo",
    val porcao: Double = 100.0,
    val kcal: Double? = null,
    val prot: Double? = null,
    val carb: Double? = null,
    val gord: Double? = null,
    val emMl: Boolean = false,
    val unidadeNome: String? = null,
    val unidadeGramas: Double? = null,
)

@Serializable
data class PlanoDoArquivo(
    val nome: String,
    val dias: List<Int> = emptyList(),
    val alvo: AlvoArquivo? = null,
    val refeicoes: List<RefeicaoArquivo> = emptyList(),
)

@Serializable
data class AlvoArquivo(val kcal: Double? = null, val prot: Double? = null, val carb: Double? = null, val gord: Double? = null)

@Serializable
data class RefeicaoArquivo(
    val nome: String,
    /** "07:30" */
    val horario: String,
    val obs: String = "",
    val itens: List<ItemArquivo> = emptyList(),
    val alternativas: List<AlternativaArquivo> = emptyList(),
)

@Serializable
data class AlternativaArquivo(val nome: String, val itens: List<ItemArquivo> = emptyList())

/** Um alimento da refeição: pela TACO ([taco]) ou pela [alimento] (chave da lista de alimentos). */
@Serializable
data class ItemArquivo(
    val taco: Int? = null,
    val alimento: String? = null,
    val gramas: Double? = null,
    val unidades: Double? = null,
)

class PlanoArquivoInvalido(mensagem: String) : Exception(mensagem)

fun horarioEmMinutos(h: String): Int {
    val partes = h.trim().split(":", "h")
    val hora = partes.getOrNull(0)?.toIntOrNull()
    val min = partes.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
    if (hora == null || hora !in 0..23 || min !in 0..59) throw PlanoArquivoInvalido("Horário inválido: $h")
    return hora * 60 + min
}
