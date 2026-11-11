package app.noplano.ui.hoje

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.noplano.data.ItemConsumo
import app.noplano.data.RegistroComItens
import app.noplano.domain.Conquistas
import app.noplano.domain.EstadoRefeicao
import app.noplano.domain.Fmt
import app.noplano.domain.somar
import app.noplano.ui.BotaoContorno
import app.noplano.ui.BotaoPrincipal
import app.noplano.ui.CampoTexto
import app.noplano.ui.ChipEscolha
import app.noplano.ui.Cores
import app.noplano.ui.DialogoBase
import app.noplano.ui.Espaco
import app.noplano.ui.LinhaDivisoria
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.SeletorAlimento
import app.noplano.ui.Tipos
import app.noplano.ui.descreverQuantidade
import app.noplano.ui.vibrarConfirmacao
import kotlinx.coroutines.delay

private sealed interface Folha {
    data class Refeicao(val estado: EstadoRefeicao, val opcaoId: Long?) : Folha
    data class Extra(val registro: RegistroComItens) : Folha
    data object Fora : Folha
    data object Agua : Folha
    data object Dificuldade : Folha
}

@Composable
fun HojeTela(abrirAjustes: () -> Unit, abrirProgresso: () -> Unit, abrirPlano: () -> Unit, vm: HojeViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val view = LocalView.current
    var folha by remember { mutableStateOf<Folha?>(null) }
    var aviso by remember { mutableStateOf<Aviso?>(null) }
    var conquista by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.avisos.collect { aviso = it } }
    LaunchedEffect(Unit) { vm.conquistas.collect { conquista = it } }
    LaunchedEffect(aviso) { if (aviso != null) { delay(6_000); aviso = null } }
    LaunchedEffect(conquista) { if (conquista != null) { delay(5_000); conquista = null } }

    val planoNovo by vm.planoNovo.collectAsStateWithLifecycle()
    val estado = ui ?: return Box(Modifier.fillMaxSize().background(Cores.fundo))
    planoNovo?.takeIf { estado.dia.temPlano }?.let { novo ->
        DialogoBase({}) {
            Rotulo("Chegou com a atualização", cor = Cores.destaque)
            Text("PLANO NOVO", style = Tipos.tituloCartao)
            Espaco(6.dp)
            if (novo.notas.isNotBlank()) { Text(novo.notas, style = Tipos.corpoForte); Espaco(6.dp) }
            Text(
                "Aplicar troca as refeições de " + novo.planos.joinToString { "\"${it.nome}\"" } +
                    " pelas da versão nova. O que você já registrou continua no histórico.",
                style = Tipos.corpoSuave,
            )
            Espaco(14.dp)
            BotaoPrincipal("Aplicar plano novo", { vm.aplicarPlanoNovo(novo) })
            Espaco(8.dp)
            BotaoContorno("Manter o meu", { vm.manterPlano(novo) }, Modifier.fillMaxWidth())
        }
    }
    val acoes = HojeAcoes(
        concluir = { r, opcao -> view.vibrarConfirmacao(estado.vibrar); vm.concluir(r.id, r.nome, opcao) },
        abrirRefeicao = { r, opcao -> folha = Folha.Refeicao(r, opcao) },
        abrirExtra = { folha = Folha.Extra(it) },
        agua = { view.vibrarConfirmacao(estado.vibrar); vm.agua(it) },
        aguaOutro = { folha = Folha.Agua },
        foraDoPlano = { folha = Folha.Fora },
        irPara = vm::irPara,
        abrirAjustes = abrirAjustes, abrirProgresso = abrirProgresso, abrirPlano = abrirPlano,
        dificuldade = { folha = Folha.Dificuldade },
        criarExemplo = { vm.criarExemplo() },
    )

    Box(Modifier.fillMaxSize()) {
        HojeConteudo(estado, acoes, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            conquista?.let { chave -> Conquistas.info(chave)?.let { FaixaAviso("Conquista: ${it.titulo}", it.descricao, null) {} } }
            aviso?.let { a ->
                FaixaAviso(a.texto, null, if (a.desfazer != null) "Desfazer" else null) {
                    a.desfazer?.let { vm.executar(it) }
                    aviso = null
                }
            }
        }
    }

    when (val f = folha) {
        is Folha.Refeicao -> FolhaBase({ folha = null }) {
            FolhaRefeicao(
                f.estado, f.opcaoId,
                registrar = { opcao, fracao, itens ->
                    view.vibrarConfirmacao(estado.vibrar)
                    vm.concluir(f.estado.id, f.estado.nome, opcao, fracao, itens); folha = null
                },
                corrigir = { id, itens -> vm.corrigir(id, itens); folha = null },
                desfazer = { vm.desfazer(it); folha = null },
            )
        }
        is Folha.Extra -> FolhaBase({ folha = null }) {
            FolhaItens(
                f.registro.registro.nome, "Fora do plano", f.registro.itens.map { ItemConsumo.de(it) }, "Salvar correção",
                { vm.corrigir(f.registro.registro.id, it); folha = null },
                "Remover registro" to { vm.desfazer(f.registro.registro.id); folha = null },
            )
        }
        Folha.Fora -> FolhaBase({ folha = null }) { FolhaFora { nome, itens -> vm.registrarFora(nome, itens); folha = null } }
        Folha.Agua -> FolhaBase({ folha = null }) { FolhaAgua { view.vibrarConfirmacao(estado.vibrar); vm.agua(it); folha = null } }
        Folha.Dificuldade -> FolhaBase({ folha = null }) { FolhaDificuldade { vm.dificuldade(it); folha = null } }
        null -> Unit
    }
}

@Composable
private fun FaixaAviso(texto: String, detalhe: String?, acao: String?, aoAgir: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Cores.texto).padding(start = 16.dp, end = 6.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(texto, style = Tipos.corpoForte, color = Cores.sobreDestaque)
            if (detalhe != null) Text(detalhe, style = Tipos.pequeno, color = Cores.sobreDestaque)
        }
        if (acao != null) Box(Modifier.heightIn(min = 48.dp).clickable(onClick = aoAgir).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(acao.uppercase(), style = Tipos.botao.copy(fontSize = Tipos.numeroPequeno.fontSize), color = Cores.sobreDestaque)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaBase(fechar: () -> Unit, conteudo: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = fechar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cores.superficie, shape = RectangleShape, dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Margem).padding(top = 22.dp, bottom = 18.dp)
        ) { conteudo() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolhaRefeicao(
    r: EstadoRefeicao,
    opcaoInicial: Long?,
    registrar: (opcaoId: Long?, fracao: Double, itens: List<ItemConsumo>?) -> Unit,
    corrigir: (Long, List<ItemConsumo>) -> Unit,
    desfazer: (Long) -> Unit,
) {
    val reg = r.registro
    if (reg != null) {
        FolhaItens(
            r.nome, "Registrado · ${Fmt.hora(r.minutos)}", reg.itens.map { ItemConsumo.de(it) }, "Salvar correção",
            { corrigir(reg.registro.id, it) }, "Desfazer registro" to { desfazer(reg.registro.id) },
        )
        return
    }
    val opcoes = r.plano.opcoesOrdenadas
    var opcaoId by remember { mutableStateOf(opcaoInicial ?: opcoes.firstOrNull()?.opcao?.id) }
    val opcao = opcoes.firstOrNull { it.opcao.id == opcaoId } ?: opcoes.firstOrNull()
    val base = opcao?.itens?.map { ItemConsumo.de(it) } ?: emptyList()
    if (opcoes.size > 1) {
        Rotulo("Alternativa")
        Espaco(6.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach { o -> ChipEscolha(o.opcao.nome, o.opcao.id == opcao?.opcao?.id, { opcaoId = o.opcao.id }) }
        }
        Espaco(14.dp)
    }
    androidx.compose.runtime.key(opcao?.opcao?.id) {
        FolhaItens(r.nome, "Planejado · ${Fmt.hora(r.minutos)}", base, "Registrar", { itens ->
            // Se todas as quantidades mudaram na mesma proporção, guarda como fração do plano.
            val fracoes = itens.zip(base).map { (novo, b) -> if (b.gramas > 0) novo.gramas / b.gramas else 1.0 }
            val uniforme = itens.size == base.size && fracoes.isNotEmpty() && fracoes.all { kotlin.math.abs(it - fracoes[0]) < 1e-6 }
            if (uniforme) registrar(opcao?.opcao?.id, fracoes[0], null) else registrar(opcao?.opcao?.id, 1.0, itens)
        }, null, comFracoes = true, permiteAdicionar = true)
    }
}

/** Lista de itens com quantidade editável, total e botões. Base de todas as folhas de registro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolhaItens(
    titulo: String,
    subtitulo: String,
    iniciais: List<ItemConsumo>,
    textoBotao: String,
    confirmar: (List<ItemConsumo>) -> Unit,
    secundario: Pair<String, () -> Unit>?,
    comFracoes: Boolean = false,
    permiteAdicionar: Boolean = true,
    campoNome: (@Composable () -> Unit)? = null,
) {
    val bases = remember { mutableStateListOf<ItemConsumo>().apply { addAll(iniciais) } }
    val textos = remember { mutableStateListOf<String>().apply { addAll(iniciais.map { Fmt.quantidade(it.gramas) }) } }
    var escolhendo by remember { mutableStateOf(false) }
    val atuais = bases.mapIndexed { i, b -> Fmt.lerNumero(textos[i])?.let { b.comGramas(it) } }
    val total = atuais.filterNotNull().map { it.macros }.somar()
    val valido = atuais.none { it == null } && atuais.isNotEmpty()

    Rotulo(subtitulo, cor = Cores.destaque)
    Text(titulo.uppercase(), style = Tipos.tituloCartao)
    campoNome?.let { Espaco(10.dp); it() }
    if (comFracoes && bases.isNotEmpty()) {
        Espaco(12.dp)
        Rotulo("Quanto você comeu")
        Espaco(6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.25 to "¼", 0.5 to "½", 0.75 to "¾", 1.0 to "Tudo").forEach { (f, nome) ->
                val sel = bases.indices.all { i -> Fmt.lerNumero(textos[i])?.let { kotlin.math.abs(it - bases[i].gramas * f) < 0.06 } == true }
                ChipEscolha(nome, sel, { bases.indices.forEach { i -> textos[i] = Fmt.quantidade(bases[i].gramas * f) } }, Modifier.weight(1f))
            }
        }
    }
    Espaco(12.dp)
    bases.forEachIndexed { i, b ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.nome, style = Tipos.corpo)
                val m = atuais[i]?.macros
                Text(
                    if (m == null) "Quantidade inválida"
                    else "${Fmt.kcal(m.kcal)} kcal · P ${Fmt.gramas(m.prot)} · C ${Fmt.gramas(m.carb)} · G ${Fmt.gramas(m.gord)}" +
                        (b.unidades?.let { " · plano: ${descreverQuantidade(b.gramas, b.medida, b.unidades, b.unidadeNome)}" } ?: ""),
                    style = Tipos.pequeno, color = if (m == null) Cores.erro else Cores.textoSuave,
                )
            }
            CampoTexto(textos[i], { textos[i] = it }, "", Modifier.width(104.dp), numerico = true, sufixo = b.medida)
            Box(Modifier.heightIn(min = 48.dp).width(40.dp).clickable { bases.removeAt(i); textos.removeAt(i) }, contentAlignment = Alignment.Center) {
                Text("×", style = Tipos.numeroPequeno, color = Cores.textoSuave)
            }
        }
        LinhaDivisoria()
    }
    if (permiteAdicionar) {
        Espaco(10.dp)
        BotaoContorno("+ Adicionar alimento", { escolhendo = true }, Modifier.fillMaxWidth())
    }
    Espaco(14.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Rotulo("Total")
        Text("${Fmt.kcal(total.kcal)} kcal", style = Tipos.numero, color = Cores.destaque)
    }
    Text("P ${Fmt.gramas(total.prot)} g · C ${Fmt.gramas(total.carb)} g · G ${Fmt.gramas(total.gord)} g", style = Tipos.pequeno)
    Espaco(14.dp)
    BotaoPrincipal(textoBotao, { confirmar(atuais.filterNotNull()) }, ativo = valido)
    secundario?.let { (texto, acao) ->
        Espaco(8.dp)
        BotaoContorno(texto, acao, Modifier.fillMaxWidth(), cor = Cores.erro)
    }
    if (escolhendo) SeletorAlimento({ e ->
        bases.add(ItemConsumo.de(e.alimento, e.gramas, e.unidades)); textos.add(Fmt.quantidade(e.gramas)); escolhendo = false
    }, { escolhendo = false })
}

@Composable
private fun FolhaFora(registrar: (String, List<ItemConsumo>) -> Unit) {
    var nome by remember { mutableStateOf("") }
    FolhaItens(
        "Fora do plano", "Registrar o que comi", emptyList(), "Registrar", { registrar(nome, it) }, null,
        campoNome = {
            Column {
                Text("Sem julgamento: registrar mantém seus números reais. O plano segue na próxima refeição.", style = Tipos.corpoSuave)
                Espaco(10.dp)
                CampoTexto(nome, { nome = it }, "Nome (opcional)")
            }
        },
    )
}

@Composable
private fun FolhaAgua(adicionar: (Int) -> Unit) {
    var texto by remember { mutableStateOf("") }
    val ml = Fmt.lerNumero(texto)?.toInt()?.takeIf { it in 1..5000 }
    Text("ÁGUA", style = Tipos.tituloCartao)
    Espaco(12.dp)
    CampoTexto(texto, { texto = it }, "Quantidade", numerico = true, sufixo = "ml")
    Espaco(14.dp)
    BotaoPrincipal("Adicionar", { adicionar(ml!!) }, ativo = ml != null)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolhaDificuldade(escolher: (String?) -> Unit) {
    Rotulo("Opcional", cor = Cores.destaque)
    Text("O QUE DIFICULTOU HOJE?", style = Tipos.tituloCartao)
    Espaco(6.dp)
    Text("Serve só para você enxergar padrões depois. Não muda nada no seu plano.", style = Tipos.corpoSuave)
    Espaco(14.dp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Rotina corrida", "Comida não estava pronta", "Comi fora de casa", "Sem fome", "Esqueci de registrar", "Outro motivo")
            .forEach { ChipEscolha(it, false, { escolher(it) }) }
    }
    Espaco(14.dp)
    BotaoContorno("Prefiro não dizer", { escolher(null) }, Modifier.fillMaxWidth())
}
