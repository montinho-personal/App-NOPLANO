package app.noplano.ui.hoje

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.noplano.app
import app.noplano.data.ItemConsumo
import app.noplano.data.ResultadoRegistro
import app.noplano.domain.Ajustes
import app.noplano.domain.Consistencia
import app.noplano.domain.EstadoDia
import app.noplano.domain.Fmt
import app.noplano.domain.Mensagens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DiaDaSemanaUi(val data: Long, val letra: String, val acompanhado: Boolean, val hoje: Boolean, val futuro: Boolean)

data class HojeUi(
    val dia: EstadoDia,
    val ehHoje: Boolean,
    val semana: List<DiaDaSemanaUi>,
    val diasAcompanhados: Int,
    val mensagem: String,
    val vibrar: Boolean = true,
    val boasVindas: Boolean = false,
)

/** Aviso de rodapé com opção de desfazer. */
data class Aviso(val texto: String, val desfazer: (suspend () -> Unit)? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class HojeViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {
    private val repo = aplicacao.app.repo
    private val relogio = repo.relogio

    private val data = MutableStateFlow(relogio.hoje())
    private var seguindoHoje = true
    val avisos = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)
    val conquistas = repo.novasConquistas

    private val tique = flow {
        while (true) {
            emit(relogio.agoraMillis())
            delay(20_000)
            if (seguindoHoje && data.value != relogio.hoje()) data.value = relogio.hoje()
        }
    }

    private val resumo = combine(repo.diasComRegistro(), tique) { dias, _ ->
        val hoje = relogio.hoje()
        val conjunto = dias.toSet()
        val segunda = hoje - (LocalDate.ofEpochDay(hoje).dayOfWeek.value - 1)
        val semana = (0..6).map {
            val d = segunda + it
            DiaDaSemanaUi(d, "STQQSSD"[it].toString(), d in conjunto, d == hoje, d > hoje)
        }
        semana to Consistencia.resumo(repo.historicoTotal(), hoje).diasAcompanhados
    }

    val ui: StateFlow<HojeUi?> = combine(
        data.flatMapLatest { repo.observarDia(it, tique) }, resumo, repo.ajustes.fluxo,
    ) { dia, (semana, total), aj: Ajustes ->
        HojeUi(dia, dia.data == relogio.hoje(), semana, total, Mensagens.doDia(dia), aj.vibrar, !aj.boasVindasVista)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun irPara(d: Long) {
        val hoje = relogio.hoje()
        if (d > hoje) return
        seguindoHoje = d == hoje
        data.value = d
    }

    fun mudarDia(delta: Int) = irPara(data.value + delta)

    fun concluir(refeicaoId: Long, nome: String, opcaoId: Long? = null, fracao: Double = 1.0, itens: List<ItemConsumo>? = null) = viewModelScope.launch {
        val d = data.value
        when (val r = repo.concluir(d, refeicaoId, opcaoId, fracao, itens)) {
            is ResultadoRegistro.Novo -> avisos.emit(Aviso(Mensagens.aoConcluir(nome, repo.estadoDia(d))) { repo.desfazer(r.id) })
            is ResultadoRegistro.JaExistia -> avisos.emit(Aviso("$nome já estava registrado."))
        }
    }

    fun registrarFora(nome: String, itens: List<ItemConsumo>) = viewModelScope.launch {
        val id = repo.registrarFora(data.value, nome, itens)
        avisos.emit(Aviso("Registrado. Seu próximo passo é a próxima refeição.") { repo.desfazer(id) })
    }

    fun corrigir(registroId: Long, itens: List<ItemConsumo>) = viewModelScope.launch {
        repo.corrigir(registroId, itens)
        avisos.emit(Aviso("Registro corrigido."))
    }

    fun desfazer(registroId: Long) = viewModelScope.launch {
        repo.desfazer(registroId)
        avisos.emit(Aviso("Registro removido."))
    }

    fun agua(ml: Int) = viewModelScope.launch {
        val id = repo.adicionarAgua(data.value, ml)
        if (id > 0) avisos.emit(Aviso("+$ml ml · ${Fmt.litros(repo.estadoDia(data.value).aguaMl)} L") { repo.removerAgua(id) })
    }

    fun dificuldade(texto: String?) = viewModelScope.launch {
        repo.anotarDificuldade(data.value, texto)
        if (texto != null) avisos.emit(Aviso("Anotado. Seguimos."))
    }

    fun boasVindasVista() = viewModelScope.launch { repo.ajustes.alterar { it.copy(boasVindasVista = true) } }

    fun criarExemplo() = viewModelScope.launch {
        app.noplano.data.Exemplo.criar(repo)
        boasVindasVista()
    }

    fun executar(acao: suspend () -> Unit) = viewModelScope.launch { acao() }
}
