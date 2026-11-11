package app.noplano

import app.noplano.data.Alimento
import app.noplano.data.FonteAjustes
import app.noplano.data.ItemComAlimento
import app.noplano.data.ItemPlano
import app.noplano.data.ItemRegistro
import app.noplano.data.Opcao
import app.noplano.data.OpcaoComItens
import app.noplano.data.Refeicao
import app.noplano.data.RefeicaoCompleta
import app.noplano.data.Registro
import app.noplano.data.RegistroComItens
import app.noplano.domain.Ajustes
import app.noplano.domain.Relogio
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class RelogioFalso(var data: LocalDate = LocalDate.of(2026, 11, 11), var minutos: Int = 12 * 60) : Relogio {
    override fun zona(): ZoneId = ZoneOffset.UTC
    override fun agoraMillis(): Long = data.atStartOfDay(ZoneOffset.UTC).plusMinutes(minutos.toLong()).toInstant().toEpochMilli()
}

class AjustesFalsos(inicial: Ajustes = Ajustes()) : FonteAjustes {
    override val fluxo = MutableStateFlow(inicial)
    override suspend fun alterar(f: (Ajustes) -> Ajustes) { fluxo.value = f(fluxo.value) }
}

/** Valores da TACO (por 100 g). */
object Comida {
    val arroz = Alimento(id = 1, nome = "Arroz, tipo 1, cozido", fonte = "TACO", tacoId = 3, kcal100 = 128.0, prot100 = 2.5, carb100 = 28.1, gord100 = 0.2)
    val frango = Alimento(id = 2, nome = "Frango, peito, sem pele, grelhado", fonte = "TACO", tacoId = 410, kcal100 = 159.0, prot100 = 32.0, carb100 = 0.0, gord100 = 2.5)
    val banana = Alimento(id = 3, nome = "Banana, prata, crua", fonte = "TACO", tacoId = 182, kcal100 = 98.0, prot100 = 1.3, carb100 = 26.0, gord100 = 0.1, unidadeNome = "unidade", unidadeGramas = 70.0)
}

fun refeicao(id: Long, nome: String, minutos: Int, vararg itens: Pair<Alimento, Double>): RefeicaoCompleta {
    val opcao = Opcao(id = id * 10, refeicaoId = id)
    return RefeicaoCompleta(
        Refeicao(id = id, planoId = 1, nome = nome, minutos = minutos),
        listOf(OpcaoComItens(opcao, itens.mapIndexed { i, (a, g) -> ItemComAlimento(ItemPlano(id * 100 + i, opcao.id, a.id, g), a) })),
    )
}

fun registroDe(r: RefeicaoCompleta, data: Long, fracao: Double = 1.0, id: Long = r.refeicao.id + 1000): RegistroComItens =
    RegistroComItens(
        Registro(id = id, data = data, refeicaoId = r.refeicao.id, nome = r.refeicao.nome, minutosPlanejado = r.refeicao.minutos, registradoEm = 0, fracao = fracao),
        r.principal!!.itens.map {
            val m = it.alimento.macros(it.item.gramas * fracao)
            ItemRegistro(registroId = id, alimentoId = it.alimento.id, nome = it.alimento.nome, gramas = it.item.gramas * fracao, kcal = m.kcal, prot = m.prot, carb = m.carb, gord = m.gord)
        },
    )
