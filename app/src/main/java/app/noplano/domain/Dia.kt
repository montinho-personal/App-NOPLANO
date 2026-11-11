package app.noplano.domain

import app.noplano.data.RefeicaoCompleta
import app.noplano.data.RegistroComItens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Fonte de data e hora; substituível nos testes. */
interface Relogio {
    fun agoraMillis(): Long
    fun zona(): ZoneId = ZoneId.systemDefault()
    fun hoje(): Long = Instant.ofEpochMilli(agoraMillis()).atZone(zona()).toLocalDate().toEpochDay()
    fun agoraMin(): Int = Instant.ofEpochMilli(agoraMillis()).atZone(zona()).toLocalTime().let { it.hour * 60 + it.minute }
    fun millisDe(data: Long, minutos: Int): Long =
        LocalDate.ofEpochDay(data).atTime(LocalTime.MIN).plusMinutes(minutos.toLong()).atZone(zona()).toInstant().toEpochMilli()

    object Sistema : Relogio {
        override fun agoraMillis() = System.currentTimeMillis()
    }
}

/** 1 = segunda … 7 = domingo. */
fun diaDaSemana(data: Long): Int = LocalDate.ofEpochDay(data).dayOfWeek.value

enum class Situacao {
    /** Registrada como planejado. */
    CONCLUIDA,
    /** Registrada com quantidade ou itens diferentes do plano. */
    AJUSTADA,
    /** A próxima a fazer. */
    PROXIMA,
    /** Ainda vai chegar. */
    FUTURA,
    /** O horário passou e não há registro. Não significa que a pessoa não comeu. */
    SEM_REGISTRO,
}

data class EstadoRefeicao(
    val plano: RefeicaoCompleta,
    val registro: RegistroComItens?,
    val situacao: Situacao,
) {
    val id: Long get() = plano.refeicao.id
    val nome: String get() = plano.refeicao.nome
    val minutos: Int get() = plano.refeicao.minutos
    val feita: Boolean get() = registro != null
}

data class EstadoDia(
    val data: Long,
    val planoNome: String?,
    val meta: Macros,
    val consumido: Macros,
    val refeicoes: List<EstadoRefeicao>,
    /** Registros fora do plano (ou de refeições que não existem mais no plano). */
    val extras: List<RegistroComItens>,
    val aguaMl: Int,
    val metaAguaMl: Int,
    val agoraMin: Int?,
) {
    val concluidas: Int get() = refeicoes.count { it.feita }
    val total: Int get() = refeicoes.size
    val proxima: EstadoRefeicao? get() = refeicoes.firstOrNull { it.situacao == Situacao.PROXIMA }
    val semRegistro: List<EstadoRefeicao> get() = refeicoes.filter { it.situacao == Situacao.SEM_REGISTRO }
    val faltamKcal: Double get() = (meta.kcal - consumido.kcal).coerceAtLeast(0.0)
    val temPlano: Boolean get() = refeicoes.isNotEmpty()
    val completo: Boolean get() = total > 0 && concluidas == total
}

/** Tempo, depois do horário, em que uma refeição ainda é tratada como "a próxima". */
const val TOLERANCIA_MIN = 90

/**
 * Monta o estado de um dia a partir do plano e dos registros.
 * [agoraMin] é a hora atual quando o dia é hoje; null para outros dias
 * ([passado] diz se o dia já acabou).
 */
fun montarDia(
    data: Long,
    planoNome: String?,
    refeicoes: List<RefeicaoCompleta>,
    registros: List<RegistroComItens>,
    aguaMl: Int,
    metaAguaMl: Int,
    agoraMin: Int?,
    passado: Boolean = false,
): EstadoDia {
    val porRefeicao = registros.filter { it.registro.refeicaoId != null }.associateBy { it.registro.refeicaoId }
    val idsPlano = refeicoes.map { it.refeicao.id }.toSet()
    var proximaDefinida = false
    val estados = refeicoes.sortedWith(compareBy({ it.refeicao.minutos }, { it.refeicao.ordem }, { it.refeicao.id })).map { r ->
        val reg = porRefeicao[r.refeicao.id]
        val situacao = when {
            reg != null -> if (reg.registro.ajustado || reg.registro.fracao != 1.0) Situacao.AJUSTADA else Situacao.CONCLUIDA
            passado -> Situacao.SEM_REGISTRO
            agoraMin != null && r.refeicao.minutos < agoraMin - TOLERANCIA_MIN -> Situacao.SEM_REGISTRO
            !proximaDefinida -> { proximaDefinida = true; Situacao.PROXIMA }
            else -> Situacao.FUTURA
        }
        EstadoRefeicao(r, reg, situacao)
    }
    val extras = registros.filter { it.registro.refeicaoId == null || it.registro.refeicaoId !in idsPlano }
    return EstadoDia(
        data = data,
        planoNome = planoNome,
        meta = refeicoes.map { it.macros }.somar(),
        consumido = registros.map { it.macros }.somar(),
        refeicoes = estados,
        extras = extras,
        aguaMl = aguaMl,
        metaAguaMl = metaAguaMl,
        agoraMin = agoraMin,
    )
}
