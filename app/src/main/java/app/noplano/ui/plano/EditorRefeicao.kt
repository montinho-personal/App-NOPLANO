package app.noplano.ui.plano

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.noplano.app
import app.noplano.data.ItemComAlimento
import app.noplano.data.OpcaoComItens
import app.noplano.data.RefeicaoCompleta
import app.noplano.domain.Alvo
import app.noplano.domain.Fmt
import app.noplano.domain.Macros
import app.noplano.domain.Passo
import app.noplano.domain.somar
import app.noplano.ui.Ajustador
import app.noplano.ui.BotaoContorno
import app.noplano.ui.BotaoPrincipal
import app.noplano.ui.CampoTexto
import app.noplano.ui.Cartao
import app.noplano.ui.ChipEscolha
import app.noplano.ui.Cores
import app.noplano.ui.DialogoBase
import app.noplano.ui.DialogoConfirmar
import app.noplano.ui.DialogoHora
import app.noplano.ui.DialogoTexto
import app.noplano.ui.Espaco
import app.noplano.ui.LinhaDivisoria
import app.noplano.ui.Margem
import app.noplano.ui.ResumoMacrosCompacto
import app.noplano.ui.Rotulo
import app.noplano.ui.SeletorAlimento
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import app.noplano.ui.descreverQuantidade
import app.noplano.ui.linhaMacros
import kotlinx.coroutines.launch

/** Item com quantidade em medida caseira (ex.: 2 ovos) é ajustado de unidade em unidade. */
private val ItemComAlimento.porUnidade: Boolean
    get() = item.unidades != null && (alimento.unidadeGramas ?: 0.0) > 0

private fun ItemComAlimento.textoQuantidade() =
    descreverQuantidade(item.gramas, alimento.medida, item.unidades, alimento.unidadeNome)

/** Calcula a nova quantidade depois de um toque em – ou +. */
fun ItemComAlimento.passo(sentido: Int): app.noplano.data.ItemPlano =
    if (porUnidade) {
        val u = Passo.aplicar(item.unidades!!, sentido, porUnidade = true)
        item.copy(unidades = u, gramas = u * alimento.unidadeGramas!!)
    } else item.copy(gramas = Passo.aplicar(item.gramas, sentido, porUnidade = false))

class EditorAcoes(
    val voltar: () -> Unit = {},
    val nome: () -> Unit = {},
    val hora: () -> Unit = {},
    val obs: () -> Unit = {},
    val selecionarOpcao: (Long) -> Unit = {},
    val novaOpcao: () -> Unit = {},
    val renomearOpcao: () -> Unit = {},
    val excluirOpcao: () -> Unit = {},
    val passo: (ItemComAlimento, Int) -> Unit = { _, _ -> },
    val quantidade: (ItemComAlimento) -> Unit = {},
    val remover: (ItemComAlimento) -> Unit = {},
    val adicionar: () -> Unit = {},
    val duplicar: () -> Unit = {},
    val excluir: () -> Unit = {},
    val irParaAlvo: () -> Unit = {},
)

/**
 * Tela de edição de uma refeição. O topo fica fixo e mostra o dia inteiro em relação ao alvo,
 * para cada toque em – ou + mostrar na hora o efeito nas macros.
 */
@Composable
fun EditorConteudo(
    r: RefeicaoCompleta,
    opcao: OpcaoComItens?,
    /** Total do dia considerando a opção selecionada nesta refeição. */
    dia: Macros,
    alvo: Alvo?,
    acoes: EditorAcoes,
    modifier: Modifier = Modifier,
) {
    val opcoes = r.opcoesOrdenadas
    val principal = opcao?.opcao?.id == opcoes.firstOrNull()?.opcao?.id
    Column(modifier.fillMaxSize()) {
        Column(Modifier.background(Cores.fundo).padding(horizontal = Margem).padding(top = 18.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Rotulo("${Fmt.hora(r.refeicao.minutos)} · refeição do plano", cor = Cores.destaque)
                    Text(r.refeicao.nome.uppercase(), style = Tipos.titulo, modifier = Modifier.semantics { heading() })
                }
                BotaoContorno("Voltar", acoes.voltar)
            }
            Espaco(12.dp)
            Column(Modifier.fillMaxWidth().background(Cores.superficie).padding(14.dp)) {
                ResumoMacrosCompacto(
                    when {
                        alvo == null || alvo.vazio -> "Dia inteiro"
                        principal -> "Dia inteiro × alvo"
                        else -> "Dia com esta alternativa × alvo"
                    },
                    dia, alvo,
                )
                Espaco(10.dp)
                LinhaDivisoria()
                Espaco(8.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Rotulo("Esta refeição", Modifier.weight(1f))
                    Text(linhaMacros(opcao?.macros ?: Macros.ZERO), style = Tipos.pequeno, color = Cores.texto)
                }
            }
            if (alvo == null || alvo.vazio) {
                Box(Modifier.heightIn(min = 40.dp).clickable(role = Role.Button, onClick = acoes.irParaAlvo), contentAlignment = Alignment.CenterStart) {
                    Text("DEFINIR UM ALVO DE MACROS NA TELA PLANO →", style = Tipos.rotulo, color = Cores.destaque)
                }
            }
        }
        LinhaDivisoria()
        Column(Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 6.dp, bottom = 28.dp)) {
            TituloSecao("Alimentos")
            run {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    opcoes.forEach { o -> ChipEscolha(o.opcao.nome, o.opcao.id == opcao?.opcao?.id, { acoes.selecionarOpcao(o.opcao.id) }) }
                    BotaoContorno("+ Alternativa", acoes.novaOpcao, cor = Cores.destaque, altura = 44.dp)
                }
                Espaco(4.dp)
                Text(
                    if (principal) "Use – e + para ajustar. O topo mostra na hora como fica o dia."
                    else "Alternativa: você escolhe na hora de registrar. A meta do dia continua sendo a da opção principal.",
                    style = Tipos.pequeno,
                )
            }
            Espaco(6.dp)
            opcao?.itens?.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(item.alimento.nome + if (item.alimento.marca.isNotBlank()) " · ${item.alimento.marca}" else "", style = Tipos.corpoForte)
                            Text(linhaMacros(item.macros), style = Tipos.pequeno)
                            Rotulo("Fonte: ${item.alimento.fonte}")
                        }
                        Box(
                            Modifier.heightIn(min = 44.dp).width(44.dp).clickable(role = Role.Button) { acoes.remover(item) },
                            contentAlignment = Alignment.Center,
                        ) { Text("×", style = Tipos.numeroPequeno, color = Cores.textoSuave) }
                    }
                    Espaco(8.dp)
                    Ajustador(
                        item.textoQuantidade(), item.alimento.nome,
                        menos = { acoes.passo(item, -1) }, mais = { acoes.passo(item, 1) }, tocar = { acoes.quantidade(item) },
                        podeDiminuir = (if (item.porUnidade) item.item.unidades!! else item.item.gramas) > 0.5,
                    )
                }
                LinhaDivisoria()
            }
            if (opcao?.itens.isNullOrEmpty()) Text("Nenhum alimento nesta opção.", style = Tipos.corpoSuave, modifier = Modifier.padding(vertical = 10.dp))
            Espaco(12.dp)
            BotaoPrincipal("+ Adicionar alimento", acoes.adicionar)
            if (!principal && opcao != null) {
                Espaco(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoContorno("Renomear alternativa", acoes.renomearOpcao, Modifier.weight(1f))
                    BotaoContorno("Excluir alternativa", acoes.excluirOpcao, Modifier.weight(1f), cor = Cores.erro)
                }
            }

            TituloSecao("Dados da refeição")
            Cartao {
                Column {
                    LinhaEditavel("Nome", r.refeicao.nome, acoes.nome)
                    LinhaDivisoria()
                    LinhaEditavel("Horário", Fmt.hora(r.refeicao.minutos), acoes.hora)
                    LinhaDivisoria()
                    LinhaEditavel("Observação", r.refeicao.obs.ifBlank { "Adicionar" }, acoes.obs)
                }
            }
            Espaco(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotaoContorno("Duplicar refeição", acoes.duplicar, Modifier.weight(1f))
                BotaoContorno("Excluir refeição", acoes.excluir, Modifier.weight(1f), cor = Cores.erro)
            }
        }
    }
}

private sealed interface DialogoEditor {
    data object Nome : DialogoEditor
    data object Hora : DialogoEditor
    data object Obs : DialogoEditor
    data object NovaOpcao : DialogoEditor
    data object RenomearOpcao : DialogoEditor
    data object ExcluirOpcao : DialogoEditor
    data object Excluir : DialogoEditor
    data object Alimento : DialogoEditor
    data class Quantidade(val item: ItemComAlimento) : DialogoEditor
}

@Composable
fun EditorRefeicaoTela(refeicaoId: Long, voltar: () -> Unit, abrir: (Long) -> Unit, irParaPlano: () -> Unit = voltar) {
    val repo = LocalContext.current.app.repo
    val escopo = rememberCoroutineScope()
    val refeicao by remember(refeicaoId) { repo.refeicao(refeicaoId) }.collectAsState(null)
    var carregou by remember { mutableStateOf(false) }
    var opcaoId by remember { mutableStateOf<Long?>(null) }
    var dialogo by remember { mutableStateOf<DialogoEditor?>(null) }
    val r = refeicao
    if (r == null) {
        if (carregou) LaunchedEffect(Unit) { voltar() }
        Box(Modifier.fillMaxSize())
        return
    }
    carregou = true
    val planoId = r.refeicao.planoId
    val plano by remember(planoId) { repo.plano(planoId) }.collectAsState(null)
    val refeicoesDoPlano by remember(planoId) { repo.refeicoes(planoId) }.collectAsState(emptyList())

    val opcoes = r.opcoesOrdenadas
    val opcao = opcoes.firstOrNull { it.opcao.id == opcaoId } ?: opcoes.firstOrNull()
    // Dia com a opção selecionada no lugar da principal desta refeição.
    val outras = refeicoesDoPlano.filter { it.refeicao.id != r.refeicao.id }.map { it.macros }.somar()
    fun diaCom(m: Macros) = outras + m
    val dia = diaCom(opcao?.macros ?: Macros.ZERO)
    val alvo = plano?.alvo
    fun fechar() { dialogo = null }

    EditorConteudo(
        r, opcao, dia, alvo,
        EditorAcoes(
            voltar = voltar,
            nome = { dialogo = DialogoEditor.Nome }, hora = { dialogo = DialogoEditor.Hora }, obs = { dialogo = DialogoEditor.Obs },
            selecionarOpcao = { opcaoId = it }, novaOpcao = { dialogo = DialogoEditor.NovaOpcao },
            renomearOpcao = { dialogo = DialogoEditor.RenomearOpcao }, excluirOpcao = { dialogo = DialogoEditor.ExcluirOpcao },
            passo = { item, s -> escopo.launch { repo.atualizarItem(item.passo(s)) } },
            quantidade = { dialogo = DialogoEditor.Quantidade(it) },
            remover = { escopo.launch { repo.excluirItem(it.item.id) } },
            adicionar = { dialogo = DialogoEditor.Alimento },
            duplicar = { escopo.launch { abrir(repo.duplicarRefeicao(r.refeicao.id)) } },
            excluir = { dialogo = DialogoEditor.Excluir },
            irParaAlvo = irParaPlano,
        ),
    )

    when (val d = dialogo) {
        DialogoEditor.Nome -> DialogoTexto("Nome da refeição", "Nome", r.refeicao.nome, ::fechar) {
            escopo.launch { repo.salvarRefeicao(r.refeicao.copy(nome = it.trim())) }; fechar()
        }
        DialogoEditor.Obs -> DialogoTexto("Observação", "Ex.: levar marmita", r.refeicao.obs, ::fechar, validar = { true }) {
            escopo.launch { repo.salvarRefeicao(r.refeicao.copy(obs = it.trim())) }; fechar()
        }
        DialogoEditor.Hora -> DialogoHora("Horário planejado", r.refeicao.minutos, ::fechar) {
            escopo.launch { repo.salvarRefeicao(r.refeicao.copy(minutos = it)) }; fechar()
        }
        DialogoEditor.NovaOpcao -> DialogoTexto("Nova alternativa", "Nome (ex.: Sem arroz)", "", ::fechar) {
            escopo.launch { opcaoId = repo.criarOpcao(r.refeicao.id, it, copiarDe = opcao?.opcao?.id) }; fechar()
        }
        DialogoEditor.RenomearOpcao -> opcao?.let { o ->
            DialogoTexto("Renomear alternativa", "Nome", o.opcao.nome, ::fechar) { escopo.launch { repo.renomearOpcao(o.opcao, it) }; fechar() }
        }
        DialogoEditor.ExcluirOpcao -> opcao?.let { o ->
            DialogoConfirmar("Excluir alternativa", "\"${o.opcao.nome}\" será removida desta refeição.", "Excluir", ::fechar) {
                escopo.launch { repo.excluirOpcao(o.opcao.id) }; opcaoId = null; fechar()
            }
        }
        DialogoEditor.Excluir -> DialogoConfirmar(
            "Excluir refeição", "\"${r.refeicao.nome}\" sai do plano. O que você já registrou continua no histórico.", "Excluir", ::fechar,
        ) { escopo.launch { repo.excluirRefeicao(r.refeicao.id) }; fechar() }
        DialogoEditor.Alimento -> SeletorAlimento(
            { e -> opcao?.let { o -> escopo.launch { repo.adicionarItem(o.opcao.id, e.alimento.id, e.gramas, e.unidades) } }; fechar() },
            ::fechar, contexto = ContextoMacros("Dia com este alimento", dia, alvo),
        )
        is DialogoEditor.Quantidade -> {
            // Usa a versão mais nova do item (os botões – e + podem ter mudado a quantidade).
            val atual = opcao?.itens?.firstOrNull { it.item.id == d.item.item.id } ?: d.item
            DialogoQuantidade(atual, diaSemItem = dia + atual.macros * -1.0, alvo = alvo, fechar = ::fechar) { novo ->
                escopo.launch { repo.atualizarItem(novo) }; fechar()
            }
        }
        null -> Unit
    }
}

/** Contexto para mostrar o efeito de um alimento no dia, enquanto a quantidade é escolhida. */
data class ContextoMacros(val titulo: String, val base: Macros, val alvo: Alvo?)

/** Digitar a quantidade de um item, vendo o efeito no dia enquanto digita. */
@Composable
fun DialogoQuantidade(
    item: ItemComAlimento,
    diaSemItem: Macros,
    alvo: Alvo?,
    fechar: () -> Unit,
    salvar: (app.noplano.data.ItemPlano) -> Unit,
) {
    val a = item.alimento
    val porUnidade = item.item.unidades != null && (a.unidadeGramas ?: 0.0) > 0
    var texto by remember { mutableStateOf(Fmt.quantidade(if (porUnidade) item.item.unidades!! else item.item.gramas)) }
    val n = Fmt.lerNumero(texto)
    val gramas = n?.let { if (porUnidade) it * a.unidadeGramas!! else it }
    val m = gramas?.let { a.macros(it) }
    DialogoBase(fechar) {
        Rotulo("Quantidade", cor = Cores.destaque)
        Text(a.nome, style = Tipos.corpoForte)
        Espaco(12.dp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto(texto, { texto = it }, if (porUnidade) "Em ${a.unidadeNome}" else "Em ${a.medida}", Modifier.weight(1f), numerico = true,
                sufixo = if (porUnidade) a.unidadeNome else a.medida)
        }
        Espaco(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-1, 1).forEach { s ->
                BotaoContorno(if (s < 0) "–" else "+", {
                    val atual = n ?: 0.0
                    texto = Fmt.quantidade(Passo.aplicar(atual, s, porUnidade))
                }, Modifier.weight(1f))
            }
        }
        Espaco(10.dp)
        Text(m?.let { linhaMacros(it) } ?: "Quantidade inválida", style = Tipos.pequeno, color = if (m == null) Cores.erro else Cores.texto)
        if (porUnidade && gramas != null) Text("${Fmt.quantidade(gramas)} ${a.medida}", style = Tipos.pequeno)
        Espaco(12.dp)
        Column(Modifier.fillMaxWidth().background(Cores.superficieAlta).padding(12.dp)) {
            ResumoMacrosCompacto("Dia com esta quantidade", diaSemItem + (m ?: Macros.ZERO), alvo)
        }
        Espaco(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Cancelar", fechar, Modifier.weight(1f))
            BotaoPrincipal("Salvar", {
                salvar(if (porUnidade) item.item.copy(unidades = n, gramas = gramas!!) else item.item.copy(gramas = gramas!!))
            }, Modifier.weight(1f), ativo = gramas != null && gramas > 0)
        }
    }
}

@Composable
private fun LinhaEditavel(rotulo: String, valor: String, aoTocar: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = aoTocar), verticalAlignment = Alignment.CenterVertically) {
        Rotulo(rotulo, Modifier.width(110.dp))
        Text(valor, style = Tipos.corpoForte, modifier = Modifier.weight(1f))
        Text("EDITAR", style = Tipos.rotulo, color = Cores.destaque)
    }
}
