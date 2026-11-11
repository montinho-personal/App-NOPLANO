package app.noplano.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import app.noplano.domain.Macros
import app.noplano.domain.somar
import kotlinx.serialization.Serializable

/** Alimento com valores por 100 g (ou por 100 ml quando [emMl]). */
@Serializable
@Entity(indices = [Index("nome")])
data class Alimento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val marca: String = "",
    /** De onde vieram os números: "TACO", "Rótulo" ou "Manual". */
    val fonte: String = "Manual",
    val tacoId: Int? = null,
    val kcal100: Double,
    val prot100: Double,
    val carb100: Double,
    val gord100: Double,
    val emMl: Boolean = false,
    /** Medida caseira opcional, ex.: "unidade", "fatia", "colher". */
    val unidadeNome: String? = null,
    val unidadeGramas: Double? = null,
    val arquivado: Boolean = false,
) {
    val medida: String get() = if (emMl) "ml" else "g"
    fun macros(quantidade: Double): Macros = Macros.de(kcal100, prot100, carb100, gord100, quantidade)
}

@Serializable
@Entity
data class Plano(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val ordem: Int = 0,
)

/** Qual plano vale em cada dia da semana (1 = segunda … 7 = domingo). */
@Serializable
@Entity
data class DiaSemana(@PrimaryKey val dia: Int, val planoId: Long)

@Serializable
@Entity(
    foreignKeys = [ForeignKey(Plano::class, ["id"], ["planoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planoId")],
)
data class Refeicao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planoId: Long,
    val nome: String,
    /** Horário planejado, em minutos desde 00h00. */
    val minutos: Int,
    val obs: String = "",
    val ordem: Int = 0,
)

/** Uma alternativa da refeição. A primeira (menor ordem) é a principal e define a meta do dia. */
@Serializable
@Entity(
    foreignKeys = [ForeignKey(Refeicao::class, ["id"], ["refeicaoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("refeicaoId")],
)
data class Opcao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val refeicaoId: Long,
    val nome: String = "Principal",
    val ordem: Int = 0,
)

@Serializable
@Entity(
    foreignKeys = [
        ForeignKey(Opcao::class, ["id"], ["opcaoId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Alimento::class, ["id"], ["alimentoId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("opcaoId"), Index("alimentoId")],
)
data class ItemPlano(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opcaoId: Long,
    val alimentoId: Long,
    /** Quantidade em gramas (ou ml). */
    val gramas: Double,
    /** Se preenchido, a quantidade é exibida em medidas caseiras (ex.: 2 unidades). */
    val unidades: Double? = null,
)

/** Refeição efetivamente consumida em um dia. */
@Serializable
@Entity(indices = [Index(value = ["data", "refeicaoId"], unique = true), Index("data")])
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Dia local, em dias desde 1970-01-01. */
    val data: Long,
    /** Refeição do plano; null quando é um consumo fora do plano. */
    val refeicaoId: Long?,
    val nome: String,
    val minutosPlanejado: Int?,
    val registradoEm: Long,
    /** Parte da refeição planejada que foi consumida (1.0 = inteira). */
    val fracao: Double = 1.0,
    /** true quando os itens foram alterados em relação ao plano. */
    val ajustado: Boolean = false,
)

/** Alimento consumido, com os valores já calculados no momento do registro. */
@Serializable
@Entity(
    foreignKeys = [ForeignKey(Registro::class, ["id"], ["registroId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("registroId")],
)
data class ItemRegistro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val registroId: Long,
    val alimentoId: Long?,
    val nome: String,
    val gramas: Double,
    val medida: String = "g",
    val unidades: Double? = null,
    val unidadeNome: String? = null,
    val kcal: Double,
    val prot: Double,
    val carb: Double,
    val gord: Double,
) {
    val macros: Macros get() = Macros(kcal, prot, carb, gord)
}

@Serializable
@Entity(indices = [Index("data")])
data class Agua(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: Long,
    val ml: Int,
    val em: Long,
)

/** Fotografia do que estava planejado no dia, para os relatórios não mudarem quando o plano mudar. */
@Serializable
@Entity
data class DiaResumo(
    @PrimaryKey val data: Long,
    val planoNome: String,
    val kcalPlano: Double,
    val protPlano: Double,
    val carbPlano: Double,
    val gordPlano: Double,
    val refeicoesPlano: Int,
    val metaAguaMl: Int,
    /** Resposta opcional a "o que dificultou hoje?". */
    val dificuldade: String? = null,
)

@Serializable
@Entity
data class Conquista(@PrimaryKey val chave: String, val em: Long)

// ---- Relações ----

data class ItemComAlimento(
    @Embedded val item: ItemPlano,
    @Relation(parentColumn = "alimentoId", entityColumn = "id") val alimento: Alimento,
) {
    val macros: Macros get() = alimento.macros(item.gramas)
}

data class OpcaoComItens(
    @Embedded val opcao: Opcao,
    @Relation(entity = ItemPlano::class, parentColumn = "id", entityColumn = "opcaoId") val itens: List<ItemComAlimento>,
) {
    val macros: Macros get() = itens.map { it.macros }.somar()
}

data class RefeicaoCompleta(
    @Embedded val refeicao: Refeicao,
    @Relation(entity = Opcao::class, parentColumn = "id", entityColumn = "refeicaoId") val opcoes: List<OpcaoComItens>,
) {
    val opcoesOrdenadas: List<OpcaoComItens> get() = opcoes.sortedWith(compareBy({ it.opcao.ordem }, { it.opcao.id }))
    val principal: OpcaoComItens? get() = opcoesOrdenadas.firstOrNull()
    val macros: Macros get() = principal?.macros ?: Macros.ZERO
}

data class RegistroComItens(
    @Embedded val registro: Registro,
    @Relation(parentColumn = "id", entityColumn = "registroId") val itens: List<ItemRegistro>,
) {
    val macros: Macros get() = itens.map { it.macros }.somar()
}

data class AguaDoDia(val data: Long, val ml: Int)
