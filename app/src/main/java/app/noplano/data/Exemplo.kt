package app.noplano.data

import kotlinx.coroutines.flow.first

/**
 * Plano de exemplo, montado só com alimentos da tabela TACO, para a pessoa ver o app
 * funcionando antes de cadastrar a própria dieta. Não é uma recomendação alimentar.
 */
object Exemplo {
    private data class R(val nome: String, val minutos: Int, val itens: List<Pair<Int, Double>>)

    private val refeicoes = listOf(
        R("Café da manhã", 7 * 60 + 30, listOf(488 to 100.0, 53 to 50.0, 182 to 70.0)),
        R("Lanche", 10 * 60, listOf(448 to 170.0, 7 to 30.0)),
        R("Almoço", 12 * 60 + 30, listOf(3 to 150.0, 561 to 100.0, 410 to 150.0, 100 to 80.0)),
        R("Lanche da tarde", 16 * 60, listOf(182 to 70.0, 589 to 15.0)),
        R("Jantar", 19 * 60 + 30, listOf(88 to 150.0, 377 to 120.0, 100 to 80.0)),
    )

    /** Preenche o primeiro plano se ele estiver vazio. Retorna false se já havia refeições. */
    suspend fun criar(repo: Repositorio): Boolean {
        repo.garantirInicial()
        val plano = repo.planos().first().first()
        if (repo.refeicoes(plano.id).first().isNotEmpty()) return false
        for (r in refeicoes) {
            val id = repo.criarRefeicao(plano.id, r.nome, r.minutos)
            val opcao = repo.refeicaoAgora(id)!!.principal!!.opcao.id
            for ((taco, gramas) in r.itens) {
                val a = repo.alimentoTaco(taco) ?: continue
                repo.adicionarItem(opcao, a.id, gramas)
            }
        }
        return true
    }
}
