package app.noplano.ui.plano

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.noplano.app
import app.noplano.data.Plano
import app.noplano.data.RefeicaoCompleta
import app.noplano.domain.Fmt
import app.noplano.domain.Macros
import app.noplano.domain.diaDaSemana
import app.noplano.domain.somar
import app.noplano.ui.BotaoContorno
import app.noplano.ui.BotaoPrincipal
import app.noplano.ui.Cartao
import app.noplano.ui.ChipEscolha
import app.noplano.ui.Cores
import app.noplano.ui.DialogoConfirmar
import app.noplano.ui.DialogoTexto
import app.noplano.ui.Espaco
import app.noplano.ui.EspacoH
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import app.noplano.ui.resumo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanoUi(
    val planos: List<Plano>,
    val atual: Plano,
    /** Dias da semana (1 = segunda) que usam o plano atual. */
    val dias: Set<Int>,
    val refeicoes: List<RefeicaoCompleta>,
) {
    val total: Macros get() = refeicoes.map { it.macros }.somar()
    val alvo: app.noplano.domain.Alvo get() = atual.alvo
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanoViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {
    private val repo = aplicacao.app.repo
    private val selecionado = MutableStateFlow<Long?>(null)

    val ui: StateFlow<PlanoUi?> = combine(repo.planos(), repo.diasSemana(), selecionado) { planos, dias, sel ->
        val hoje = dias.firstOrNull { it.dia == diaDaSemana(repo.relogio.hoje()) }?.planoId
        val atual = planos.firstOrNull { it.id == sel } ?: planos.firstOrNull { it.id == hoje } ?: planos.firstOrNull()
        Triple(planos, dias, atual)
    }.flatMapLatest { (planos, dias, atual) ->
        if (atual == null) flowOf(null) else combine(repo.refeicoes(atual.id), flowOf(Unit)) { rs, _ ->
            PlanoUi(planos, atual, dias.filter { it.planoId == atual.id }.map { it.dia }.toSet(), rs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selecionar(id: Long) { selecionado.value = id }
    fun criarPlano(nome: String) = viewModelScope.launch { selecionado.value = repo.criarPlano(nome) }
    fun renomear(id: Long, nome: String) = viewModelScope.launch { repo.renomearPlano(id, nome) }
    fun duplicar(id: Long) = viewModelScope.launch { selecionado.value = repo.duplicarPlano(id) }
    fun excluir(id: Long) = viewModelScope.launch { if (repo.excluirPlano(id)) selecionado.value = null }
    fun salvarAlvo(planoId: Long, alvo: app.noplano.domain.Alvo) = viewModelScope.launch { repo.salvarAlvo(planoId, alvo) }
    fun usarNoDia(dia: Int, planoId: Long) = viewModelScope.launch { repo.definirDia(dia, planoId) }
    fun novaRefeicao(planoId: Long, nome: String, aoCriar: (Long) -> Unit) = viewModelScope.launch {
        aoCriar(repo.criarRefeicao(planoId, nome, 12 * 60))
    }
}

class PlanoAcoes(
    val selecionar: (Long) -> Unit = {},
    val novoPlano: () -> Unit = {},
    val renomear: () -> Unit = {},
    val duplicar: () -> Unit = {},
    val excluir: () -> Unit = {},
    val usarNoDia: (Int) -> Unit = {},
    val abrirRefeicao: (Long) -> Unit = {},
    val novaRefeicao: () -> Unit = {},
    val editarAlvo: () -> Unit = {},
)

@Composable
fun PlanoTela(abrirRefeicao: (Long) -> Unit, vm: PlanoViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val estado = ui ?: return Box(Modifier.fillMaxSize())
    var dialogo by remember { mutableStateOf<String?>(null) }
    PlanoConteudo(
        estado,
        PlanoAcoes(
            selecionar = vm::selecionar, novoPlano = { dialogo = "novo" }, renomear = { dialogo = "renomear" },
            duplicar = { vm.duplicar(estado.atual.id) }, excluir = { dialogo = "excluir" },
            usarNoDia = { vm.usarNoDia(it, estado.atual.id) }, abrirRefeicao = abrirRefeicao, novaRefeicao = { dialogo = "refeicao" },
            editarAlvo = { dialogo = "alvo" },
        ),
    )
    when (dialogo) {
        "novo" -> DialogoTexto("Novo plano", "Nome (ex.: Dia de treino)", "", { dialogo = null }) { vm.criarPlano(it); dialogo = null }
        "renomear" -> DialogoTexto("Renomear plano", "Nome", estado.atual.nome, { dialogo = null }) { vm.renomear(estado.atual.id, it); dialogo = null }
        "refeicao" -> DialogoTexto("Nova refeição", "Nome (ex.: Almoço)", "", { dialogo = null }) {
            vm.novaRefeicao(estado.atual.id, it, abrirRefeicao); dialogo = null
        }
        "alvo" -> DialogoAlvo(estado.alvo, { dialogo = null }) { vm.salvarAlvo(estado.atual.id, it); dialogo = null }
        "excluir" -> DialogoConfirmar(
            "Excluir plano", "As refeições de \"${estado.atual.nome}\" serão apagadas. O que você já registrou continua no histórico.",
            "Excluir", { dialogo = null },
        ) { vm.excluir(estado.atual.id); dialogo = null }
    }
}

@Composable
fun PlanoConteudo(ui: PlanoUi, acoes: PlanoAcoes, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 18.dp, bottom = 28.dp)) {
        Rotulo("Cadastre uma vez, use todo dia", cor = Cores.destaque)
        Text("PLANO", style = Tipos.titulo, modifier = Modifier.semantics { heading() })
        Espaco(14.dp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.planos.forEach { ChipEscolha(it.nome, it.id == ui.atual.id, { acoes.selecionar(it.id) }) }
            BotaoContorno("+ Plano", acoes.novoPlano, cor = Cores.destaque, altura = 44.dp)
        }
        Espaco(16.dp)
        Rotulo("Dias em que este plano vale")
        Espaco(6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val nomes = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")
            "STQQSSD".forEachIndexed { i, letra ->
                val dia = i + 1
                ChipEscolha(
                    letra.toString(), dia in ui.dias, { acoes.usarNoDia(dia) },
                    Modifier.weight(1f).semantics { contentDescription = nomes[i] + if (dia in ui.dias) ", usa este plano" else "" },
                )
            }
        }
        if (ui.planos.size > 1) {
            Espaco(4.dp)
            Text("Toque em um dia para ele passar a usar este plano.", style = Tipos.pequeno)
        }
        Espaco(16.dp)
        TituloSecao("Macros do plano") {
            BotaoContorno(if (ui.alvo.vazio) "Definir alvo" else "Editar alvo", acoes.editarAlvo, cor = Cores.destaque, altura = 40.dp)
        }
        Cartao {
            Column {
                app.noplano.ui.PainelMacros(ui.total, ui.alvo)
                Espaco(4.dp)
                Text(
                    if (ui.alvo.vazio) "Soma de todas as refeições. Defina um alvo (por exemplo, o da sua nutricionista) para ver quanto falta ou sobra."
                    else "Soma das refeições comparada com o seu alvo. Ajuste as quantidades em cada refeição: os números mudam na hora.",
                    style = Tipos.pequeno,
                )
            }
        }
        TituloSecao("Refeições")
        if (ui.refeicoes.isEmpty()) Text("Nenhuma refeição ainda. Comece pela primeira do seu dia.", style = Tipos.corpoSuave, modifier = Modifier.padding(bottom = 12.dp))
        ui.refeicoes.forEach { r ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Cores.superficie).heightIn(min = 64.dp)
                    .clickable(role = Role.Button) { acoes.abrirRefeicao(r.refeicao.id) }.padding(14.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Fmt.hora(r.refeicao.minutos), style = Tipos.numeroPequeno, color = Cores.destaque)
                EspacoH(14.dp)
                Column(Modifier.weight(1f)) {
                    Text(r.refeicao.nome, style = Tipos.corpoForte)
                    Text(
                        r.principal?.itens?.joinToString(" · ") { it.resumo() }?.ifBlank { null } ?: "Sem alimentos. Toque para adicionar.",
                        style = Tipos.pequeno,
                    )
                    Text("P ${Fmt.gramasInteiro(r.macros.prot)} · C ${Fmt.gramasInteiro(r.macros.carb)} · G ${Fmt.gramasInteiro(r.macros.gord)}", style = Tipos.rotulo, color = Cores.textoSuave)
                    if (r.opcoes.size > 1) Rotulo("${r.opcoes.size - 1} alternativa" + if (r.opcoes.size > 2) "s" else "")
                }
                EspacoH(8.dp)
                Text(Fmt.kcal(r.macros.kcal), style = Tipos.numeroPequeno)
                Text(" kcal", style = Tipos.unidade)
            }
        }
        Espaco(6.dp)
        BotaoPrincipal("+ Nova refeição", acoes.novaRefeicao)
        TituloSecao("Este plano")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Renomear", acoes.renomear, Modifier.weight(1f))
            BotaoContorno("Duplicar", acoes.duplicar, Modifier.weight(1f))
            if (ui.planos.size > 1) BotaoContorno("Excluir", acoes.excluir, Modifier.weight(1f), cor = Cores.erro)
        }
        Espaco(8.dp)
        Text("Duplicar copia todas as refeições para um novo plano, pronto para ajustar (ex.: dia de descanso).", style = Tipos.pequeno)
    }
}

/** Alvo de macros digitado pelo usuário. Campo vazio = sem alvo para aquele valor. */
@Composable
fun DialogoAlvo(atual: app.noplano.domain.Alvo, fechar: () -> Unit, salvar: (app.noplano.domain.Alvo) -> Unit) {
    fun t(v: Double?) = v?.let { Fmt.quantidade(it) } ?: ""
    var kcal by remember { mutableStateOf(t(atual.kcal)) }
    var prot by remember { mutableStateOf(t(atual.prot)) }
    var carb by remember { mutableStateOf(t(atual.carb)) }
    var gord by remember { mutableStateOf(t(atual.gord)) }
    fun ler(s: String) = if (s.isBlank()) null else Fmt.lerNumero(s)
    val validos = listOf(kcal, prot, carb, gord).all { it.isBlank() || Fmt.lerNumero(it) != null }
    // Calorias que os macros digitados somam (4 kcal/g de proteína e carboidrato, 9 kcal/g de gordura).
    val pelosMacros = listOf(ler(prot), ler(carb), ler(gord)).takeIf { l -> l.all { it != null } }
        ?.let { (p, c, g) -> p!! * 4 + c!! * 4 + g!! * 9 }
    app.noplano.ui.DialogoBase(fechar) {
        Rotulo("Digite o que você quer atingir", cor = Cores.destaque)
        Text("ALVO DE MACROS", style = Tipos.tituloCartao)
        Espaco(6.dp)
        Text("O app não calcula alvos: use os números que você ou sua nutricionista definiram. Deixe em branco o que não quiser acompanhar.", style = Tipos.pequeno)
        Espaco(12.dp)
        app.noplano.ui.CampoTexto(kcal, { kcal = it }, "Calorias", numerico = true, sufixo = "kcal")
        Espaco(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.noplano.ui.CampoTexto(prot, { prot = it }, "Proteínas", Modifier.weight(1f), numerico = true, sufixo = "g")
            app.noplano.ui.CampoTexto(carb, { carb = it }, "Carbos", Modifier.weight(1f), numerico = true, sufixo = "g")
            app.noplano.ui.CampoTexto(gord, { gord = it }, "Gorduras", Modifier.weight(1f), numerico = true, sufixo = "g")
        }
        if (pelosMacros != null) {
            Espaco(8.dp)
            Text("Esses macros somam ${Fmt.kcal(pelosMacros)} kcal.", style = Tipos.pequeno)
            if (kcal.isBlank()) app.noplano.ui.BotaoContorno("Usar ${Fmt.kcal(pelosMacros)} kcal como alvo", { kcal = Fmt.quantidade(kotlin.math.round(pelosMacros)) }, Modifier.fillMaxWidth())
        }
        Espaco(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.noplano.ui.BotaoContorno("Limpar", { salvar(app.noplano.domain.Alvo()) }, Modifier.weight(1f))
            app.noplano.ui.BotaoPrincipal("Salvar", { salvar(app.noplano.domain.Alvo(ler(kcal), ler(prot), ler(carb), ler(gord))) }, Modifier.weight(1f), ativo = validos)
        }
    }
}
