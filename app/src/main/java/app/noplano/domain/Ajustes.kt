package app.noplano.domain

import kotlinx.serialization.Serializable

/** Preferências do usuário. Horários em minutos desde 00h00. */
@Serializable
data class Ajustes(
    val metaAguaMl: Int = 3000,
    val vibrar: Boolean = true,
    // Notificações
    val avisarAntes: Boolean = true,
    val minutosAntes: Int = 10,
    val avisarPendente: Boolean = true,
    val minutosPendente: Int = 45,
    val avisarAgua: Boolean = true,
    val intervaloAguaMin: Int = 120,
    val avisarResumo: Boolean = true,
    val resumoMin: Int = 21 * 60 + 30,
    val silencioInicioMin: Int = 22 * 60,
    val silencioFimMin: Int = 7 * 60,
    val tetoDiario: Int = 10,
    // Widget
    val widgetCalorias: Boolean = true,
    val widgetProteinas: Boolean = true,
    val widgetAgua: Boolean = true,
    val widgetProxima: Boolean = true,
    val widgetProgresso: Boolean = true,
    val boasVindasVista: Boolean = false,
    val pediuNotificacao: Boolean = false,
    /** Última versão de assets/plano.json já aplicada (ou recusada). */
    val planoArquivoVersao: Int = 0,
) {
    /** true se [min] cai no período de silêncio (que pode atravessar a meia-noite). */
    fun emSilencio(min: Int): Boolean = when {
        silencioInicioMin == silencioFimMin -> false
        silencioInicioMin < silencioFimMin -> min in silencioInicioMin until silencioFimMin
        else -> min >= silencioInicioMin || min < silencioFimMin
    }
}
