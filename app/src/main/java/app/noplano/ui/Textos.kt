package app.noplano.ui

import app.noplano.data.ItemComAlimento
import app.noplano.data.ItemRegistro
import app.noplano.domain.Fmt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBR = Locale("pt", "BR")
private val fmtCurto = DateTimeFormatter.ofPattern("EEE · d MMM", ptBR)
private val fmtLongo = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)

fun dataCurta(data: Long): String = LocalDate.ofEpochDay(data).format(fmtCurto).replace(".", "")
fun dataLonga(data: Long): String = LocalDate.ofEpochDay(data).format(fmtLongo)

fun descreverQuantidade(gramas: Double, medida: String, unidades: Double?, unidadeNome: String?): String =
    if (unidades != null && unidadeNome != null) {
        val nome = if (unidades > 1 && !unidadeNome.endsWith("s")) "${unidadeNome}s" else unidadeNome
        "${Fmt.quantidade(unidades)} $nome"
    } else "${Fmt.quantidade(gramas)} $medida"

fun ItemComAlimento.descricao(): String =
    "${descreverQuantidade(item.gramas, alimento.medida, item.unidades, alimento.unidadeNome)} · ${alimento.nome}"

fun ItemRegistro.descricao(): String = "${descreverQuantidade(gramas, medida, unidades, unidadeNome)} · $nome"

/** Nome curto para listas resumidas: "Arroz, tipo 1, cozido" vira "Arroz". */
fun nomeCurto(nome: String): String = nome.substringBefore(",").trim()

fun ItemComAlimento.resumo(): String =
    "${descreverQuantidade(item.gramas, alimento.medida, item.unidades, alimento.unidadeNome)} ${nomeCurto(alimento.nome).lowercase()}"
