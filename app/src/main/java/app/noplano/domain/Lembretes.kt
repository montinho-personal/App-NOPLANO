package app.noplano.domain

enum class TipoLembrete { ANTES, PENDENTE, AGUA, RESUMO }

data class Lembrete(
    val tipo: TipoLembrete,
    /** Minuto do dia em que deve aparecer. */
    val quandoMin: Int,
    val refeicaoId: Long? = null,
    val titulo: String,
    val texto: String,
) {
    /** Identificador estável da notificação no dia. */
    val chave: String get() = "${tipo.name}:${refeicaoId ?: quandoMin}"
}

/**
 * Regras locais de notificação. Função pura: recebe o estado do dia e devolve os lembretes
 * que ainda fazem sentido, em ordem de horário.
 *
 * - Refeição registrada não gera lembrete.
 * - Nada dentro do período de silêncio.
 * - Água só aparece se o consumo estiver abaixo do ritmo esperado para a hora.
 */
object Lembretes {

    /** Todos os lembretes possíveis do dia, dado o estado atual (sem olhar a hora). */
    fun doDia(dia: EstadoDia, a: Ajustes): List<Lembrete> {
        val lista = mutableListOf<Lembrete>()
        for (r in dia.refeicoes) {
            if (r.feita) continue
            if (a.avisarAntes) {
                val q = r.minutos - a.minutosAntes
                if (q >= 0) lista += Lembrete(
                    TipoLembrete.ANTES, q, r.id,
                    titulo = "${r.nome} às ${Fmt.hora(r.minutos)}",
                    texto = "Sua refeição está chegando. Tudo preparado?",
                )
            }
            if (a.avisarPendente) {
                val q = r.minutos + a.minutosPendente
                if (q < 24 * 60) lista += Lembrete(
                    TipoLembrete.PENDENTE, q, r.id,
                    titulo = "${r.nome} ainda sem registro",
                    texto = "Já comeu? Um toque registra.",
                )
            }
        }
        if (a.avisarAgua && a.metaAguaMl > 0 && a.intervaloAguaMin > 0) {
            for (q in horariosAgua(a)) {
                if (dia.aguaMl < aguaEsperada(q, a) && dia.aguaMl < a.metaAguaMl) lista += Lembrete(
                    TipoLembrete.AGUA, q,
                    titulo = "Água: ${Fmt.litros(dia.aguaMl)} de ${Fmt.litros(a.metaAguaMl)} L",
                    texto = "Que tal conferir sua água de hoje?",
                )
            }
        }
        if (a.avisarResumo && dia.temPlano) lista += Lembrete(
            TipoLembrete.RESUMO, a.resumoMin,
            titulo = "Seu dia: ${dia.concluidas} de ${dia.total} refeições",
            texto = "Veja como foi seu planejamento de hoje.",
        )
        return lista.filterNot { a.emSilencio(it.quandoMin) }.sortedBy { it.quandoMin }
    }

    /** Lembretes do dia que ainda não passaram. */
    fun futuros(dia: EstadoDia, a: Ajustes, agoraMin: Int): List<Lembrete> =
        doDia(dia, a).filter { it.quandoMin > agoraMin }

    /** O próximo lembrete a agendar, ou null se não há mais nenhum hoje. */
    fun proximo(dia: EstadoDia, a: Ajustes, agoraMin: Int): Lembrete? = futuros(dia, a, agoraMin).firstOrNull()

    /**
     * Decide, na hora de exibir, se o lembrete ainda vale. [exibidosHoje] é quantas notificações
     * já foram mostradas no dia (teto diário).
     */
    fun deveExibir(l: Lembrete, dia: EstadoDia, a: Ajustes, exibidosHoje: Int): Boolean {
        if (a.emSilencio(l.quandoMin)) return false
        if (exibidosHoje >= a.tetoDiario && l.tipo != TipoLembrete.RESUMO) return false
        return doDia(dia, a).any { it.chave == l.chave }
    }

    /** Janela "acordada": do fim do silêncio ao início do silêncio. */
    private fun janela(a: Ajustes): Pair<Int, Int> {
        val inicio = a.silencioFimMin
        val fim = if (a.silencioInicioMin > a.silencioFimMin) a.silencioInicioMin else 24 * 60
        return inicio to fim
    }

    fun horariosAgua(a: Ajustes): List<Int> {
        val (inicio, fim) = janela(a)
        val lista = mutableListOf<Int>()
        var q = inicio + a.intervaloAguaMin
        while (q < fim) { lista += q; q += a.intervaloAguaMin }
        return lista
    }

    /**
     * Quanto de água seria esperado até [min], distribuindo a meta pela janela acordada,
     * com um intervalo de folga para não cobrar quem está só um pouco atrás.
     */
    fun aguaEsperada(min: Int, a: Ajustes): Int {
        val (inicio, fim) = janela(a)
        if (fim <= inicio) return 0
        val decorrido = (min - a.intervaloAguaMin - inicio).coerceIn(0, fim - inicio)
        return (a.metaAguaMl.toLong() * decorrido / (fim - inicio)).toInt()
    }
}
