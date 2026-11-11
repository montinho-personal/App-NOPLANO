package app.noplano.ui.plano

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
import androidx.compose.ui.unit.dp
import app.noplano.app
import app.noplano.data.ItemComAlimento
import app.noplano.data.RefeicaoCompleta
import app.noplano.domain.Fmt
import app.noplano.ui.BotaoContorno
import app.noplano.ui.Cartao
import app.noplano.ui.ChipEscolha
import app.noplano.ui.Cores
import app.noplano.ui.DialogoConfirmar
import app.noplano.ui.DialogoHora
import app.noplano.ui.DialogoTexto
import app.noplano.ui.Espaco
import app.noplano.ui.LinhaDivisoria
import app.noplano.ui.LinhaValor
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.SeletorAlimento
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import app.noplano.ui.descreverQuantidade
import kotlinx.coroutines.launch

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
fun EditorRefeicaoTela(refeicaoId: Long, voltar: () -> Unit, abrir: (Long) -> Unit) {
    val repo = LocalContext.current.app.repo
    val escopo = rememberCoroutineScope()
    val refeicao by remember(refeicaoId) { repo.refeicao(refeicaoId) }.collectAsState(null)
    var carregou by remember { mutableStateOf(false) }
    val r = refeicao
    if (r == null) {
        // Refeição excluída (ou ainda carregando).
        if (carregou) androidx.compose.runtime.LaunchedEffect(Unit) { voltar() }
        Box(Modifier.fillMaxSize())
        return
    }
    carregou = true
    var opcaoId by remember { mutableStateOf<Long?>(null) }
    var dialogo by remember { mutableStateOf<DialogoEditor?>(null) }
    val opcoes = r.opcoesOrdenadas
    val opcao = opcoes.firstOrNull { it.opcao.id == opcaoId } ?: opcoes.firstOrNull()
    val principal = opcao?.opcao?.id == opcoes.firstOrNull()?.opcao?.id
    fun fechar() { dialogo = null }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 18.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Rotulo("Refeição do plano", cor = Cores.destaque)
                Text(r.refeicao.nome.uppercase(), style = Tipos.titulo)
            }
            BotaoContorno("Voltar", voltar)
        }
        Espaco(14.dp)
        Cartao {
            Column {
                LinhaEditavel("Nome", r.refeicao.nome) { dialogo = DialogoEditor.Nome }
                LinhaDivisoria()
                LinhaEditavel("Horário", Fmt.hora(r.refeicao.minutos)) { dialogo = DialogoEditor.Hora }
                LinhaDivisoria()
                LinhaEditavel("Observação", r.refeicao.obs.ifBlank { "Adicionar" }) { dialogo = DialogoEditor.Obs }
            }
        }

        TituloSecao("Alimentos")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach { o -> ChipEscolha(o.opcao.nome, o.opcao.id == opcao?.opcao?.id, { opcaoId = o.opcao.id }) }
            BotaoContorno("+ Alternativa", { dialogo = DialogoEditor.NovaOpcao }, cor = Cores.destaque, altura = 44.dp)
        }
        Espaco(4.dp)
        Text(
            if (principal) "A opção principal define a meta do dia." else "Alternativa: você escolhe na hora de registrar. A meta do dia não muda.",
            style = Tipos.pequeno,
        )
        Espaco(8.dp)
        opcao?.itens?.forEach { item ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button) { dialogo = DialogoEditor.Quantidade(item) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(item.alimento.nome, style = Tipos.corpo)
                    val m = item.macros
                    Text(
                        "${Fmt.kcal(m.kcal)} kcal · P ${Fmt.gramas(m.prot)} · C ${Fmt.gramas(m.carb)} · G ${Fmt.gramas(m.gord)} · ${item.alimento.fonte}",
                        style = Tipos.pequeno,
                    )
                }
                Text(
                    descreverQuantidade(item.item.gramas, item.alimento.medida, item.item.unidades, item.alimento.unidadeNome),
                    style = Tipos.numeroPequeno, color = Cores.destaque,
                )
                Box(Modifier.heightIn(min = 48.dp).width(44.dp).clickable { escopo.launch { repo.excluirItem(item.item.id) } }, contentAlignment = Alignment.Center) {
                    Text("×", style = Tipos.numeroPequeno, color = Cores.textoSuave)
                }
            }
            LinhaDivisoria()
        }
        if (opcao?.itens.isNullOrEmpty()) Text("Nenhum alimento nesta opção.", style = Tipos.corpoSuave, modifier = Modifier.padding(vertical = 10.dp))
        Espaco(10.dp)
        app.noplano.ui.BotaoPrincipal("+ Adicionar alimento", { dialogo = DialogoEditor.Alimento })

        opcao?.let { o ->
            val m = o.macros
            Espaco(14.dp)
            Cartao {
                Column {
                    Rotulo("Total · ${o.opcao.nome}")
                    LinhaValor("Calorias", "${Fmt.kcal(m.kcal)} kcal", destaque = true)
                    LinhaValor("Proteínas", "${Fmt.gramas(m.prot)} g")
                    LinhaValor("Carboidratos", "${Fmt.gramas(m.carb)} g")
                    LinhaValor("Gorduras", "${Fmt.gramas(m.gord)} g")
                }
            }
            if (!principal) {
                Espaco(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoContorno("Renomear alternativa", { dialogo = DialogoEditor.RenomearOpcao }, Modifier.weight(1f))
                    BotaoContorno("Excluir alternativa", { dialogo = DialogoEditor.ExcluirOpcao }, Modifier.weight(1f), cor = Cores.erro)
                }
            }
        }

        TituloSecao("Esta refeição")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Duplicar", { escopo.launch { abrir(repo.duplicarRefeicao(r.refeicao.id)) } }, Modifier.weight(1f))
            BotaoContorno("Excluir", { dialogo = DialogoEditor.Excluir }, Modifier.weight(1f), cor = Cores.erro)
        }
    }

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
        DialogoEditor.Alimento -> SeletorAlimento({ e ->
            opcao?.let { o -> escopo.launch { repo.adicionarItem(o.opcao.id, e.alimento.id, e.gramas, e.unidades) } }; fechar()
        }, ::fechar)
        is DialogoEditor.Quantidade -> {
            val a = d.item.alimento
            val emUnidades = d.item.item.unidades != null && (a.unidadeGramas ?: 0.0) > 0
            DialogoTexto(
                "Quantidade", a.nome, Fmt.quantidade(if (emUnidades) d.item.item.unidades!! else d.item.item.gramas), ::fechar,
                numerico = true, sufixo = if (emUnidades) a.unidadeNome else a.medida,
                validar = { (Fmt.lerNumero(it) ?: 0.0) > 0 },
            ) { texto ->
                val n = Fmt.lerNumero(texto)!!
                val novo = if (emUnidades) d.item.item.copy(unidades = n, gramas = n * a.unidadeGramas!!) else d.item.item.copy(gramas = n)
                escopo.launch { repo.atualizarItem(novo) }; fechar()
            }
        }
        null -> Unit
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
