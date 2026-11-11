package app.noplano.domain

/** Frase curta da tela principal, sempre derivada do estado real do dia. */
object Mensagens {
    fun doDia(d: EstadoDia): String {
        val p = d.proxima
        val agora = d.agoraMin
        return when {
            !d.temPlano -> "Cadastre seu plano para começar."
            d.completo -> if (d.aguaMl < d.metaAguaMl) "Plano do dia concluído. Falta a água."
            else "Plano do dia concluído."
            d.semRegistro.isNotEmpty() && p != null -> "Seu próximo passo é ${p.nome.minuscula()}, às ${Fmt.hora(p.minutos)}."
            d.semRegistro.isNotEmpty() -> "O dia ainda aceita registro. Marque o que você comeu."
            p == null -> "${d.concluidas} de ${d.total} refeições registradas."
            d.concluidas == 0 -> "O dia começa com ${p.nome.minuscula()}, às ${Fmt.hora(p.minutos)}."
            agora != null && p.minutos - agora in 1..30 -> "${p.nome} em ${p.minutos - agora} min."
            agora != null && agora >= p.minutos -> "Hora de ${p.nome.minuscula()}."
            else -> variantes(d, p)[((d.data + d.concluidas) % 3).toInt()]
        }
    }

    private fun variantes(d: EstadoDia, p: EstadoRefeicao): List<String> {
        val faltaProt = (d.meta.prot - d.consumido.prot).coerceAtLeast(0.0)
        return listOf(
            "${d.concluidas} de ${d.total} no plano. Próxima às ${Fmt.hora(p.minutos)}.",
            "Faltam ${Fmt.gramasInteiro(faltaProt)} g de proteína nas próximas refeições.",
            "${p.nome} às ${Fmt.hora(p.minutos)}. Segue o plano.",
        )
    }

    /** Mensagem logo depois de concluir uma refeição. */
    fun aoConcluir(nome: String, d: EstadoDia): String {
        val faltam = d.total - d.concluidas
        return when {
            d.completo -> "$nome registrado. Plano do dia concluído."
            faltam == 1 -> "$nome registrado. Falta 1 refeição."
            else -> "$nome registrado. Faltam $faltam refeições."
        }
    }

    private fun String.minuscula() = replaceFirstChar { it.lowercase() }
}
