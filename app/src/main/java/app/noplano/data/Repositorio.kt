package app.noplano.data

import androidx.room.withTransaction
import app.noplano.domain.Conquistas
import app.noplano.domain.DiaHistorico
import app.noplano.domain.EstadoDia
import app.noplano.domain.Macros
import app.noplano.domain.MarcaRegistro
import app.noplano.domain.Relogio
import app.noplano.domain.diaDaSemana
import app.noplano.domain.montarDia
import app.noplano.domain.somar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/** Um alimento e a quantidade consumida, pronto para virar item de registro. */
data class ItemConsumo(
    val alimentoId: Long?,
    val nome: String,
    val gramas: Double,
    val medida: String = "g",
    val unidades: Double? = null,
    val unidadeNome: String? = null,
    val macros: Macros,
) {
    companion object {
        fun de(a: Alimento, gramas: Double, unidades: Double? = null) = ItemConsumo(
            a.id, a.nome, gramas, a.medida, unidades, a.unidadeNome.takeIf { unidades != null }, a.macros(gramas),
        )

        fun de(i: ItemComAlimento, fracao: Double = 1.0) =
            de(i.alimento, i.item.gramas * fracao, i.item.unidades?.times(fracao))

        fun de(i: ItemRegistro) =
            ItemConsumo(i.alimentoId, i.nome, i.gramas, i.medida, i.unidades, i.unidadeNome, i.macros)
    }

    /** Mesma comida, outra quantidade (proporcional). */
    fun comGramas(novo: Double): ItemConsumo {
        val f = if (gramas > 0) novo / gramas else 0.0
        return copy(gramas = novo, unidades = unidades?.times(f), macros = macros * f)
    }
}

sealed interface ResultadoRegistro {
    val id: Long
    data class Novo(override val id: Long) : ResultadoRegistro
    /** A refeição já tinha registro neste dia; nada foi duplicado. */
    data class JaExistia(override val id: Long) : ResultadoRegistro
}

/**
 * Única porta de entrada para ler e gravar dados. Toda gravação chama [aoMudar],
 * que atualiza widget e lembretes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Repositorio(
    private val banco: Banco,
    val ajustes: FonteAjustes,
    val relogio: Relogio = Relogio.Sistema,
) {
    private val planos = banco.planoDao()
    private val alimentos = banco.alimentoDao()
    private val registros = banco.registroDao()

    var aoMudar: suspend () -> Unit = {}
    private val _novasConquistas = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val novasConquistas: SharedFlow<String> = _novasConquistas

    private suspend fun mudou(avaliar: Boolean = true) {
        if (avaliar) avaliarConquistas()
        aoMudar()
    }

    // ---------- Dia ----------

    fun observarDia(data: Long, agora: Flow<Long>): Flow<EstadoDia> {
        val refeicoes = planos.planoDoDiaFluxo(diaDaSemana(data)).flatMapLatest { id ->
            if (id == null) flowOf(null to emptyList()) else combine(planos.planos(), planos.refeicoes(id)) { ps, rs ->
                ps.firstOrNull { it.id == id }?.nome to rs
            }
        }
        return combine(refeicoes, registros.doDia(data), registros.agua(data), ajustes.fluxo, agora) { (nome, rs), regs, aguas, aj, _ ->
            val hoje = relogio.hoje()
            montarDia(
                data, nome, rs, regs, aguas.sumOf { it.ml }, aj.metaAguaMl,
                agoraMin = if (data == hoje) relogio.agoraMin() else null,
                passado = data < hoje,
            )
        }
    }

    suspend fun estadoDia(data: Long = relogio.hoje()): EstadoDia {
        val planoId = planos.planoDoDia(diaDaSemana(data))
        val rs = planoId?.let { planos.refeicoesAgora(it) } ?: emptyList()
        val hoje = relogio.hoje()
        return montarDia(
            data, planoId?.let { planos.plano(it)?.nome }, rs, registros.doDiaAgora(data),
            registros.aguaTotal(data), ajustes.atual().metaAguaMl,
            agoraMin = if (data == hoje) relogio.agoraMin() else null,
            passado = data < hoje,
        )
    }

    private suspend fun fotografarDia(data: Long) {
        val planoId = planos.planoDoDia(diaDaSemana(data))
        val rs = planoId?.let { planos.refeicoesAgora(it) } ?: emptyList()
        val meta = rs.map { it.macros }.somar()
        val anterior = registros.resumo(data)
        registros.salvarResumo(
            DiaResumo(
                data, planoId?.let { planos.plano(it)?.nome } ?: "", meta.kcal, meta.prot, meta.carb, meta.gord,
                rs.size, ajustes.atual().metaAguaMl, anterior?.dificuldade,
            )
        )
    }

    // ---------- Registro ----------

    /**
     * Registra uma refeição do plano. Sem [itens], usa a opção indicada (ou a principal)
     * multiplicada por [fracao]. Nunca cria dois registros da mesma refeição no mesmo dia.
     */
    suspend fun concluir(
        data: Long,
        refeicaoId: Long,
        opcaoId: Long? = null,
        fracao: Double = 1.0,
        itens: List<ItemConsumo>? = null,
    ): ResultadoRegistro {
        val resultado = banco.withTransaction {
            registros.idDoPlano(data, refeicaoId)?.let { return@withTransaction ResultadoRegistro.JaExistia(it) }
            val r = planos.refeicaoAgora(refeicaoId) ?: error("Refeição não encontrada")
            val opcao = r.opcoesOrdenadas.firstOrNull { it.opcao.id == opcaoId } ?: r.principal
            val consumo = itens ?: opcao?.itens?.map { ItemConsumo.de(it, fracao) } ?: emptyList()
            val alternativa = opcao != null && opcao.opcao.id != r.principal?.opcao?.id
            val id = registros.inserir(
                Registro(
                    data = data, refeicaoId = refeicaoId, nome = r.refeicao.nome,
                    minutosPlanejado = r.refeicao.minutos, registradoEm = relogio.agoraMillis(),
                    fracao = if (itens == null) fracao else 1.0, ajustado = itens != null || alternativa,
                )
            )
            registros.inserirItens(consumo.map { it.paraItem(id) })
            fotografarDia(data)
            ResultadoRegistro.Novo(id)
        }
        if (resultado is ResultadoRegistro.Novo) mudou()
        return resultado
    }

    suspend fun registrarFora(data: Long, nome: String, itens: List<ItemConsumo>): Long {
        val id = banco.withTransaction {
            val id = registros.inserir(
                Registro(data = data, refeicaoId = null, nome = nome.ifBlank { "Fora do plano" },
                    minutosPlanejado = null, registradoEm = relogio.agoraMillis(), ajustado = true)
            )
            registros.inserirItens(itens.map { it.paraItem(id) })
            fotografarDia(data)
            id
        }
        mudou()
        return id
    }

    /** Corrige um registro existente, trocando os itens consumidos. */
    suspend fun corrigir(registroId: Long, itens: List<ItemConsumo>) {
        banco.withTransaction {
            val atual = registros.porId(registroId) ?: return@withTransaction
            registros.limparItens(registroId)
            registros.inserirItens(itens.map { it.paraItem(registroId) })
            registros.atualizar(atual.registro.copy(ajustado = true, fracao = 1.0))
        }
        mudou(avaliar = false)
    }

    suspend fun desfazer(registroId: Long) {
        registros.excluir(registroId)
        mudou(avaliar = false)
    }

    suspend fun registro(id: Long): RegistroComItens? = registros.porId(id)

    private fun ItemConsumo.paraItem(registroId: Long) = ItemRegistro(
        registroId = registroId, alimentoId = alimentoId, nome = nome, gramas = gramas, medida = medida,
        unidades = unidades, unidadeNome = unidadeNome,
        kcal = macros.kcal, prot = macros.prot, carb = macros.carb, gord = macros.gord,
    )

    // ---------- Água ----------

    suspend fun adicionarAgua(data: Long, ml: Int): Long {
        if (ml <= 0) return -1
        val id = banco.withTransaction {
            val id = registros.inserir(Agua(data = data, ml = ml, em = relogio.agoraMillis()))
            if (registros.resumo(data) == null) fotografarDia(data)
            id
        }
        mudou()
        return id
    }

    suspend fun removerAgua(id: Long) { registros.excluirAgua(id); mudou(avaliar = false) }

    suspend fun removerUltimaAgua(data: Long) {
        registros.ultimaAgua(data)?.let { removerAgua(it.id) }
    }

    suspend fun anotarDificuldade(data: Long, texto: String?) {
        if (registros.resumo(data) == null) fotografarDia(data)
        registros.resumo(data)?.let { registros.salvarResumo(it.copy(dificuldade = texto)) }
    }

    // ---------- Plano ----------

    fun planos(): Flow<List<Plano>> = planos.planos()
    fun diasSemana(): Flow<List<DiaSemana>> = planos.diasSemana()
    fun refeicoes(planoId: Long): Flow<List<RefeicaoCompleta>> = planos.refeicoes(planoId)
    fun refeicao(id: Long): Flow<RefeicaoCompleta?> = planos.refeicao(id)
    suspend fun refeicaoAgora(id: Long): RefeicaoCompleta? = planos.refeicaoAgora(id)

    /** Garante que existe ao menos um plano e que todo dia da semana aponta para um plano. */
    suspend fun garantirInicial() {
        banco.withTransaction {
            val lista = planos.planosAgora()
            val padrao = lista.firstOrNull()?.id ?: planos.inserir(Plano(nome = "Meu plano"))
            val dias = planos.diasSemanaAgora().associateBy { it.dia }
            val validos = (lista.map { it.id } + padrao).toSet()
            for (d in 1..7) if (dias[d]?.planoId !in validos) planos.definirDia(DiaSemana(d, padrao))
        }
    }

    suspend fun criarPlano(nome: String): Long {
        val id = planos.inserir(Plano(nome = nome.trim().ifBlank { "Novo plano" }, ordem = planos.planosAgora().size))
        mudou(avaliar = false)
        return id
    }

    suspend fun renomearPlano(id: Long, nome: String) {
        planos.plano(id)?.let { planos.atualizar(it.copy(nome = nome.trim().ifBlank { it.nome })) }
        mudou(avaliar = false)
    }

    suspend fun duplicarPlano(id: Long): Long {
        val novo = banco.withTransaction {
            val origem = planos.plano(id) ?: error("Plano não encontrado")
            val novoId = planos.inserir(origem.copy(id = 0, nome = "${origem.nome} (cópia)", ordem = planos.planosAgora().size))
            planos.refeicoesAgora(id).forEach { copiarRefeicao(it, novoId, it.refeicao.nome) }
            novoId
        }
        mudou(avaliar = false)
        return novo
    }

    /** Exclui o plano. Os dias que o usavam passam para outro plano. Não exclui o último. */
    suspend fun excluirPlano(id: Long): Boolean {
        val ok = banco.withTransaction {
            val outros = planos.planosAgora().filter { it.id != id }
            val alvo = planos.plano(id)
            if (outros.isEmpty() || alvo == null) return@withTransaction false
            planos.diasSemanaAgora().filter { it.planoId == id }.forEach { planos.definirDia(it.copy(planoId = outros.first().id)) }
            planos.excluir(alvo)
            true
        }
        if (ok) mudou(avaliar = false)
        return ok
    }

    fun plano(id: Long): Flow<Plano?> = planos.planoFluxo(id)

    /** Salva o alvo de macros do plano. Campos nulos ficam sem alvo. */
    suspend fun salvarAlvo(planoId: Long, alvo: app.noplano.domain.Alvo) {
        val limpo = app.noplano.domain.Alvo(
            alvo.kcal?.takeIf { it > 0 }, alvo.prot?.takeIf { it > 0 }, alvo.carb?.takeIf { it > 0 }, alvo.gord?.takeIf { it > 0 },
        )
        planos.plano(planoId)?.let { planos.atualizar(it.comAlvo(limpo)) }
        mudou(avaliar = false)
    }

    suspend fun definirDia(dia: Int, planoId: Long) { planos.definirDia(DiaSemana(dia, planoId)); mudou(avaliar = false) }

    suspend fun criarRefeicao(planoId: Long, nome: String, minutos: Int): Long {
        val id = banco.withTransaction {
            val id = planos.inserir(Refeicao(planoId = planoId, nome = nome.trim().ifBlank { "Refeição" }, minutos = minutos))
            planos.inserir(Opcao(refeicaoId = id))
            id
        }
        mudou(avaliar = false)
        return id
    }

    suspend fun salvarRefeicao(r: Refeicao) { planos.atualizar(r); mudou(avaliar = false) }

    suspend fun duplicarRefeicao(id: Long): Long {
        val novo = banco.withTransaction {
            val r = planos.refeicaoAgora(id) ?: error("Refeição não encontrada")
            copiarRefeicao(r, r.refeicao.planoId, "${r.refeicao.nome} (cópia)")
        }
        mudou(avaliar = false)
        return novo
    }

    private suspend fun copiarRefeicao(r: RefeicaoCompleta, planoId: Long, nome: String): Long {
        val id = planos.inserir(r.refeicao.copy(id = 0, planoId = planoId, nome = nome))
        r.opcoesOrdenadas.forEach { o ->
            val oid = planos.inserir(o.opcao.copy(id = 0, refeicaoId = id))
            o.itens.forEach { planos.inserir(it.item.copy(id = 0, opcaoId = oid)) }
        }
        return id
    }

    suspend fun excluirRefeicao(id: Long) { planos.excluirRefeicao(id); mudou(avaliar = false) }

    /** Cria uma alternativa; se [copiarDe] for informado, começa com os mesmos itens. */
    suspend fun criarOpcao(refeicaoId: Long, nome: String, copiarDe: Long? = null): Long {
        val id = banco.withTransaction {
            val r = planos.refeicaoAgora(refeicaoId) ?: error("Refeição não encontrada")
            val oid = planos.inserir(Opcao(refeicaoId = refeicaoId, nome = nome.trim().ifBlank { "Alternativa" },
                ordem = (r.opcoes.maxOfOrNull { it.opcao.ordem } ?: 0) + 1))
            r.opcoes.firstOrNull { it.opcao.id == copiarDe }?.itens?.forEach { planos.inserir(it.item.copy(id = 0, opcaoId = oid)) }
            oid
        }
        mudou(avaliar = false)
        return id
    }

    suspend fun renomearOpcao(o: Opcao, nome: String) { planos.atualizar(o.copy(nome = nome.trim().ifBlank { o.nome })); mudou(avaliar = false) }
    suspend fun excluirOpcao(id: Long) { planos.excluirOpcao(id); mudou(avaliar = false) }

    suspend fun adicionarItem(opcaoId: Long, alimentoId: Long, gramas: Double, unidades: Double? = null): Long {
        val id = planos.inserir(ItemPlano(opcaoId = opcaoId, alimentoId = alimentoId, gramas = gramas, unidades = unidades))
        mudou(avaliar = false)
        return id
    }

    suspend fun atualizarItem(i: ItemPlano) { planos.atualizar(i); mudou(avaliar = false) }
    suspend fun excluirItem(id: Long) { planos.excluirItem(id); mudou(avaliar = false) }

    // ---------- Alimentos ----------

    fun buscarAlimentos(q: String): Flow<List<Alimento>> = alimentos.buscar(q.trim())
    suspend fun alimento(id: Long): Alimento? = alimentos.porId(id)
    suspend fun alimentoTaco(tacoId: Int): Alimento? = alimentos.porTaco(tacoId)
    suspend fun totalTaco(): Int = alimentos.totalTaco()
    suspend fun importarAlimentos(lista: List<Alimento>) = alimentos.inserirTodos(lista)

    suspend fun salvarAlimento(a: Alimento): Long =
        if (a.id == 0L) alimentos.inserir(a) else { alimentos.atualizar(a); mudou(avaliar = false); a.id }

    /** Remove da busca. Se o alimento estiver em algum plano, apenas arquiva. */
    suspend fun removerAlimento(a: Alimento) {
        if (planos.usosDoAlimento(a.id) > 0) alimentos.atualizar(a.copy(arquivado = true)) else alimentos.excluir(a.id)
    }

    // ---------- Histórico ----------

    /** Refeições planejadas para cada dia da semana (1 = segunda), segundo o plano atual. */
    suspend fun planoPorDiaSemana(): Map<Int, List<app.noplano.domain.RefeicaoRef>> {
        val cache = mutableMapOf<Long, List<app.noplano.domain.RefeicaoRef>>()
        return planos.diasSemanaAgora().associate { d ->
            d.dia to cache.getOrPut(d.planoId) {
                planos.refeicoesAgora(d.planoId).map { app.noplano.domain.RefeicaoRef(it.refeicao.id, it.refeicao.nome, it.refeicao.minutos) }
            }
        }
    }

    fun diasComRegistro(): Flow<List<Long>> = registros.diasComRegistroFluxo()
    fun conquistas(): Flow<List<Conquista>> = registros.conquistas()

    suspend fun historico(de: Long, ate: Long): List<DiaHistorico> {
        val regs = registros.entre(de, ate).groupBy { it.registro.data }
        val aguas = registros.aguaEntre(de, ate).associate { it.data to it.ml }
        val resumos = registros.resumosEntre(de, ate).associateBy { it.data }
        val zona = relogio.zona()
        return (de..ate).map { d ->
            val lista = regs[d].orEmpty()
            val r = resumos[d]
            DiaHistorico(
                data = d,
                registros = lista.map {
                    val hora = Instant.ofEpochMilli(it.registro.registradoEm).atZone(zona)
                    val mesmoDia = hora.toLocalDate().toEpochDay() == d
                    MarcaRegistro(it.registro.refeicaoId, it.registro.nome, it.registro.minutosPlanejado,
                        if (mesmoDia) hora.hour * 60 + hora.minute else null)
                },
                consumido = lista.map { it.macros }.somar(),
                plano = r?.let { Macros(it.kcalPlano, it.protPlano, it.carbPlano, it.gordPlano) },
                refeicoesPlano = r?.refeicoesPlano ?: 0,
                aguaMl = aguas[d] ?: 0,
                metaAguaMl = r?.metaAguaMl ?: 0,
                dificuldade = r?.dificuldade,
            )
        }
    }

    /** Histórico completo, do primeiro dia com dados até hoje. */
    suspend fun historicoTotal(): List<DiaHistorico> {
        val primeiro = listOfNotNull(registros.diasComRegistro().firstOrNull(), registros.aguaPorDia().minOfOrNull { it.data }).minOrNull()
            ?: return emptyList()
        return historico(primeiro, relogio.hoje())
    }

    suspend fun avaliarConquistas(): List<String> {
        val tem = registros.conquistasAgora().map { it.chave }.toSet()
        val novas = Conquistas.avaliar(historicoTotal()).filter { it !in tem }
        novas.forEach {
            if (registros.inserir(Conquista(it, relogio.agoraMillis())) != -1L) _novasConquistas.tryEmit(it)
        }
        return novas
    }
}
