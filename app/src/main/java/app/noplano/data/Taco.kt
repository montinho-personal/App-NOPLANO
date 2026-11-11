package app.noplano.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tabela Brasileira de Composição de Alimentos (TACO), 4ª edição, NEPA/UNICAMP, 2011.
 * Valores por 100 g de parte comestível. O arquivo fica em assets/taco.json.
 */
object Taco {
    const val FONTE = "TACO"
    const val REFERENCIA = "TACO — Tabela Brasileira de Composição de Alimentos, 4ª edição revisada e ampliada (NEPA/UNICAMP, 2011)"

    @Serializable
    private data class Arquivo(val fonte: String, val base: String, val alimentos: List<Linha>)

    @Serializable
    private data class Linha(
        val id: Int,
        @SerialName("n") val nome: String,
        @SerialName("c") val categoria: String,
        @SerialName("k") val kcal: Double,
        @SerialName("p") val prot: Double,
        @SerialName("h") val carb: Double,
        @SerialName("g") val gord: Double,
    )

    fun ler(json: String): List<Alimento> =
        Json { ignoreUnknownKeys = true }.decodeFromString(Arquivo.serializer(), json).alimentos.map {
            Alimento(nome = it.nome, fonte = FONTE, tacoId = it.id, kcal100 = it.kcal, prot100 = it.prot, carb100 = it.carb, gord100 = it.gord)
        }

    /** Insere os alimentos da TACO que ainda não estão no banco. */
    suspend fun garantir(context: Context, banco: Banco) {
        val dao = banco.alimentoDao()
        val todos = ler(context.assets.open("taco.json").bufferedReader().use { it.readText() })
        if (dao.totalTaco() >= todos.size) return
        val existentes = dao.todos().mapNotNull { it.tacoId }.toSet()
        dao.inserirTodos(todos.filter { it.tacoId !in existentes })
    }
}
