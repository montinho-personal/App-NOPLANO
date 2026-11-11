package app.noplano.domain

/** Um registro visto pelo histórico: de qual refeição, quando era e quando foi marcado. */
data class MarcaRegistro(
    val refeicaoId: Long?,
    val nome: String,
    val minutosPlanejado: Int?,
    /** Minuto do dia em que foi registrado; null se foi lançado em outro dia (correção). */
    val registradoMin: Int?,
)

data class DiaHistorico(
    val data: Long,
    val registros: List<MarcaRegistro> = emptyList(),
    val consumido: Macros = Macros.ZERO,
    /** Meta planejada naquele dia; null se o dia não tem fotografia do plano. */
    val plano: Macros? = null,
    val refeicoesPlano: Int = 0,
    val aguaMl: Int = 0,
    val metaAguaMl: Int = 0,
    val dificuldade: String? = null,
) {
    /** Adesão ao registro: o dia tem pelo menos uma refeição registrada. */
    val acompanhado: Boolean get() = registros.isNotEmpty()
    val refeicoesFeitas: Int get() = registros.count { it.refeicaoId != null }.coerceAtMost(maxOf(refeicoesPlano, 0)).let {
        if (refeicoesPlano == 0) registros.count { r -> r.refeicaoId != null } else it
    }
    /** Adesão ao plano: parte das refeições planejadas que foi registrada. */
    val adesaoPlano: Double? get() = if (refeicoesPlano > 0) refeicoesFeitas.toDouble() / refeicoesPlano else null
    val completo: Boolean get() = refeicoesPlano > 0 && refeicoesFeitas >= refeicoesPlano
    val aguaRegistrada: Boolean get() = aguaMl > 0
}

data class ResumoConsistencia(
    /** Total de dias acompanhados. Nunca diminui. */
    val diasAcompanhados: Int,
    val sequenciaAtual: Int,
    val melhorSequencia: Int,
    /** Vezes em que o acompanhamento voltou depois de uma pausa. */
    val retomadas: Int,
    val ultimos7: Int,
    val ultimos30: Int,
    val refeicoesRegistradas: Int,
)

object Consistencia {
    /**
     * [historico] deve estar em ordem de data, sem buracos, terminando em [hoje].
     * Um dia de hoje ainda sem registro não quebra a sequência.
     */
    fun resumo(historico: List<DiaHistorico>, hoje: Long): ResumoConsistencia {
        val dias = historico.filter { it.data <= hoje }
        var melhor = 0
        var corrente = 0
        var retomadas = 0
        var jaComecou = false
        var emPausa = false
        for (d in dias) {
            if (d.acompanhado) {
                if (jaComecou && emPausa) retomadas++
                jaComecou = true
                emPausa = false
                corrente++
                melhor = maxOf(melhor, corrente)
            } else {
                if (d.data != hoje) { corrente = 0; if (jaComecou) emPausa = true }
            }
        }
        return ResumoConsistencia(
            diasAcompanhados = dias.count { it.acompanhado },
            sequenciaAtual = corrente,
            melhorSequencia = melhor,
            retomadas = retomadas,
            ultimos7 = dias.count { it.acompanhado && it.data > hoje - 7 },
            ultimos30 = dias.count { it.acompanhado && it.data > hoje - 30 },
            refeicoesRegistradas = dias.sumOf { it.refeicoesFeitas },
        )
    }
}

data class InfoConquista(val chave: String, val titulo: String, val descricao: String)

/**
 * Conquistas. Todas reconhecem registrar, seguir o plano ou retomar.
 * Nenhuma premia comer menos do que a meta.
 */
object Conquistas {
    const val PRIMEIRO_DIA = "PRIMEIRO_DIA"
    const val DIA_COMPLETO = "DIA_COMPLETO"
    const val PRIMEIRA_SEMANA = "PRIMEIRA_SEMANA"
    const val DEZ_DIAS = "DEZ_DIAS"
    const val TRINTA_DIAS = "TRINTA_DIAS"
    const val RETOMADA = "RETOMADA"
    const val AGUA_7 = "AGUA_7"
    const val CEM_REFEICOES = "CEM_REFEICOES"

    val todas = listOf(
        InfoConquista(PRIMEIRO_DIA, "Primeiro dia", "Primeiro dia com refeição registrada."),
        InfoConquista(DIA_COMPLETO, "Dia inteiro", "Todas as refeições do plano registradas em um dia."),
        InfoConquista(PRIMEIRA_SEMANA, "Primeira semana", "Sete dias acompanhados."),
        InfoConquista(DEZ_DIAS, "Dez dias", "Dez dias de registros."),
        InfoConquista(TRINTA_DIAS, "Trinta dias", "Trinta dias de acompanhamento."),
        InfoConquista(RETOMADA, "De volta", "Primeira retomada depois de uma pausa."),
        InfoConquista(AGUA_7, "Água em dia", "Sete dias com água registrada."),
        InfoConquista(CEM_REFEICOES, "Cem refeições", "Cem refeições do plano registradas."),
    )

    fun info(chave: String): InfoConquista? = todas.firstOrNull { it.chave == chave }

    /** Chaves das conquistas já alcançadas com este histórico. */
    fun avaliar(historico: List<DiaHistorico>): List<String> {
        if (historico.isEmpty()) return emptyList()
        val r = Consistencia.resumo(historico, historico.last().data)
        return buildList {
            if (r.diasAcompanhados >= 1) add(PRIMEIRO_DIA)
            if (historico.any { it.completo }) add(DIA_COMPLETO)
            if (r.diasAcompanhados >= 7) add(PRIMEIRA_SEMANA)
            if (r.diasAcompanhados >= 10) add(DEZ_DIAS)
            if (r.diasAcompanhados >= 30) add(TRINTA_DIAS)
            if (r.retomadas >= 1) add(RETOMADA)
            if (historico.count { it.aguaRegistrada } >= 7) add(AGUA_7)
            if (r.refeicoesRegistradas >= 100) add(CEM_REFEICOES)
        }
    }
}
