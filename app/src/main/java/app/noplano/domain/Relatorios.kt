package app.noplano.domain

/** Refeição planejada, só com o que os relatórios precisam. */
data class RefeicaoRef(val id: Long, val nome: String, val minutos: Int)

data class PendenciaRefeicao(val nome: String, val minutos: Int, val semRegistro: Int, val dias: Int)

data class Relatorio(
    val dias: Int,
    val diasComRegistro: Int,
    /** Refeições registradas ÷ planejadas, nos dias que têm plano fotografado. null se não há base. */
    val pctRefeicoes: Double?,
    /** Médias por dia, considerando só dias com registro. */
    val mediaConsumido: Macros?,
    val mediaPlano: Macros?,
    /** Média de água nos dias em que houve registro de água. */
    val mediaAguaMl: Int?,
    val diasComAgua: Int,
    val pendencias: List<PendenciaRefeicao>,
    val porDia: List<DiaHistorico>,
)

object Relatorios {
    /**
     * Dias sem nenhum registro ficam fora das médias: ausência de registro não prova
     * que a pessoa não comeu.
     */
    fun montar(historico: List<DiaHistorico>, planoPorDiaSemana: Map<Int, List<RefeicaoRef>> = emptyMap()): Relatorio {
        val comRegistro = historico.filter { it.acompanhado }
        val comPlano = comRegistro.filter { it.refeicoesPlano > 0 }
        val n = comRegistro.size
        val comMeta = comRegistro.filter { it.plano != null }
        val comAgua = historico.filter { it.aguaRegistrada }

        val pend = mutableMapOf<Long, IntArray>()
        val refs = mutableMapOf<Long, RefeicaoRef>()
        for (d in comRegistro) {
            val feitos = d.registros.mapNotNull { it.refeicaoId }.toSet()
            for (r in planoPorDiaSemana[diaDaSemana(d.data)].orEmpty()) {
                refs[r.id] = r
                val c = pend.getOrPut(r.id) { IntArray(2) }
                c[1]++
                if (r.id !in feitos) c[0]++
            }
        }

        return Relatorio(
            dias = historico.size,
            diasComRegistro = n,
            pctRefeicoes = if (comPlano.isEmpty()) null
            else comPlano.sumOf { it.refeicoesFeitas }.toDouble() / comPlano.sumOf { it.refeicoesPlano },
            mediaConsumido = if (n == 0) null else comRegistro.map { it.consumido }.somar() * (1.0 / n),
            mediaPlano = if (comMeta.isEmpty()) null else comMeta.map { it.plano!! }.somar() * (1.0 / comMeta.size),
            mediaAguaMl = if (comAgua.isEmpty()) null else comAgua.sumOf { it.aguaMl } / comAgua.size,
            diasComAgua = comAgua.size,
            pendencias = pend.map { (id, c) -> PendenciaRefeicao(refs.getValue(id).nome, refs.getValue(id).minutos, c[0], c[1]) }
                .filter { it.semRegistro > 0 }.sortedByDescending { it.semRegistro.toDouble() / it.dias },
            porDia = historico,
        )
    }
}

/**
 * Observações sobre padrões de uso. Estatística simples e regras visíveis;
 * só aparecem quando há dados suficientes e nunca inferem emoção ou diagnóstico.
 */
object Padroes {
    const val MIN_DIAS = 14
    const val MIN_AMOSTRAS = 5
    private val nomesDia = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    data class Resultado(val suficiente: Boolean, val diasFaltando: Int, val observacoes: List<String>)

    fun observar(historico: List<DiaHistorico>, planoPorDiaSemana: Map<Int, List<RefeicaoRef>>, hoje: Long): Resultado {
        val acompanhados = historico.filter { it.acompanhado }
        if (acompanhados.size < MIN_DIAS) return Resultado(false, MIN_DIAS - acompanhados.size, emptyList())
        val obs = mutableListOf<String>()

        // Refeições registradas bem depois do horário.
        val atrasos = acompanhados.flatMap { it.registros }
            .filter { it.refeicaoId != null && it.minutosPlanejado != null && it.registradoMin != null }
            .groupBy { it.nome }
            .mapValues { (_, l) -> l.count { it.registradoMin!! - it.minutosPlanejado!! > 60 } to l.size }
            .filter { it.value.second >= MIN_AMOSTRAS && it.value.first * 10 >= it.value.second * 4 }
            .maxByOrNull { it.value.first.toDouble() / it.value.second }
        atrasos?.let { (nome, c) ->
            obs += "$nome foi registrado mais de 1 h depois do horário em ${c.first} de ${c.second} vezes. Se o horário real mudou, ajuste no plano."
        }

        // Refeição que mais fica sem registro.
        Relatorios.montar(historico, planoPorDiaSemana).pendencias
            .firstOrNull { it.dias >= MIN_AMOSTRAS && it.semRegistro * 10 >= it.dias * 4 }
            ?.let { obs += "${it.nome} ficou sem registro em ${it.semRegistro} de ${it.dias} dias acompanhados. Um lembrete nesse horário pode ajudar." }

        // Dia da semana com menos acompanhamento (precisa de 4 semanas).
        val ate = historico.filter { it.data < hoje }
        if (ate.size >= 28) {
            val porDia = ate.groupBy { diaDaSemana(it.data) }.mapValues { (_, l) -> l.count { it.acompanhado }.toDouble() / l.size }
            val media = porDia.values.average()
            porDia.minByOrNull { it.value }?.takeIf { it.value <= media - 0.3 }?.let {
                obs += "Às ${plural(nomesDia[it.key - 1])} há menos registros do que nos outros dias."
            }
        }

        val retomadas = Consistencia.resumo(historico, hoje).retomadas
        if (retomadas > 0) obs += if (retomadas == 1) "Você já retomou o acompanhamento uma vez depois de uma pausa."
        else "Você já retomou o acompanhamento $retomadas vezes depois de pausas."

        return Resultado(true, 0, obs)
    }

    private fun plural(dia: String) = when (dia) {
        "sábado" -> "sábados"; "domingo" -> "domingos"; else -> "${dia}s"
    }
}
