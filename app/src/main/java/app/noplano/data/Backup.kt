package app.noplano.data

import androidx.room.withTransaction
import app.noplano.domain.Ajustes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ArquivoBackup(
    val formato: String = FORMATO,
    val versao: Int = 1,
    val criadoEm: Long = 0,
    val ajustes: Ajustes = Ajustes(),
    val alimentos: List<Alimento> = emptyList(),
    val planos: List<Plano> = emptyList(),
    val dias: List<DiaSemana> = emptyList(),
    val refeicoes: List<Refeicao> = emptyList(),
    val opcoes: List<Opcao> = emptyList(),
    val itens: List<ItemPlano> = emptyList(),
    val registros: List<Registro> = emptyList(),
    val itensRegistro: List<ItemRegistro> = emptyList(),
    val aguas: List<Agua> = emptyList(),
    val resumos: List<DiaResumo> = emptyList(),
    val conquistas: List<Conquista> = emptyList(),
) {
    companion object { const val FORMATO = "noplano-backup" }
}

class BackupInvalido(mensagem: String) : Exception(mensagem)

/** Exporta e restaura todos os dados em um único arquivo JSON. */
class Backup(private val banco: Banco, private val ajustes: FonteAjustes) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    suspend fun exportar(agora: Long): String {
        val d = banco.backupDao()
        val arquivo = banco.withTransaction {
            ArquivoBackup(
                criadoEm = agora, ajustes = ajustes.atual(),
                alimentos = d.alimentos(), planos = d.planos(), dias = d.dias(), refeicoes = d.refeicoes(),
                opcoes = d.opcoes(), itens = d.itens(), registros = d.registros(), itensRegistro = d.itensRegistro(),
                aguas = d.aguas(), resumos = d.resumos(), conquistas = d.conquistas(),
            )
        }
        return json.encodeToString(ArquivoBackup.serializer(), arquivo)
    }

    fun ler(texto: String): ArquivoBackup {
        val arquivo = runCatching { json.decodeFromString(ArquivoBackup.serializer(), texto) }
            .getOrElse { throw BackupInvalido("Este arquivo não é um backup do No Plano.") }
        if (arquivo.formato != ArquivoBackup.FORMATO) throw BackupInvalido("Este arquivo não é um backup do No Plano.")
        if (arquivo.versao > 1) throw BackupInvalido("Backup feito por uma versão mais nova do app.")
        return arquivo
    }

    /** Substitui todos os dados atuais pelos do backup. Tudo ou nada. */
    suspend fun restaurar(texto: String): ArquivoBackup {
        val a = ler(texto)
        val d = banco.backupDao()
        banco.withTransaction {
            d.apagarItensRegistro(); d.apagarRegistros(); d.apagarAguas(); d.apagarResumos(); d.apagarConquistas()
            d.apagarDias(); d.apagarItens(); d.apagarOpcoes(); d.apagarRefeicoes(); d.apagarPlanos(); d.apagarAlimentos()
            d.alimentos(a.alimentos); d.planos(a.planos); d.dias(a.dias); d.refeicoes(a.refeicoes)
            d.opcoes(a.opcoes); d.itens(a.itens); d.registros(a.registros); d.itensRegistro(a.itensRegistro)
            d.aguas(a.aguas); d.resumos(a.resumos); d.conquistas(a.conquistas)
        }
        ajustes.alterar { a.ajustes }
        return a
    }
}
