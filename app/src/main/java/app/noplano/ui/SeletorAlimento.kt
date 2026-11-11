package app.noplano.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.noplano.app
import app.noplano.data.Alimento
import app.noplano.domain.Fmt
import kotlinx.coroutines.launch

/** Resultado da escolha: alimento, quantidade em g/ml e, se for o caso, em medidas caseiras. */
data class Escolha(val alimento: Alimento, val gramas: Double, val unidades: Double?)

private sealed interface Passo {
    data object Busca : Passo
    data class Quantidade(val alimento: Alimento) : Passo
    data class Cadastro(val base: Alimento?) : Passo
}

/**
 * Tela cheia para escolher um alimento (TACO ou próprio), informar a quantidade
 * ou cadastrar um novo a partir do rótulo.
 */
@Composable
fun SeletorAlimento(aoEscolher: (Escolha) -> Unit, aoFechar: () -> Unit) {
    var passo by remember { mutableStateOf<Passo>(Passo.Busca) }
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Cores.fundo).systemBarsPadding().imePadding().padding(horizontal = Margem)) {
            when (val p = passo) {
                Passo.Busca -> Busca(aoFechar, { passo = Passo.Quantidade(it) }, { passo = Passo.Cadastro(null) }, { passo = Passo.Cadastro(it) })
                is Passo.Quantidade -> Quantidade(p.alimento, { passo = Passo.Busca }, aoEscolher)
                is Passo.Cadastro -> Cadastro(p.base, { passo = Passo.Busca }) { passo = Passo.Quantidade(it) }
            }
        }
    }
}

@Composable
private fun Cabecalho(titulo: String, voltar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo.uppercase(), style = Tipos.titulo, modifier = Modifier.weight(1f))
        BotaoContorno("Voltar", voltar)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Busca(fechar: () -> Unit, escolher: (Alimento) -> Unit, novo: () -> Unit, editar: (Alimento) -> Unit) {
    val repo = LocalContext.current.app.repo
    var q by remember { mutableStateOf("") }
    val resultados by remember(q) { repo.buscarAlimentos(q) }.collectAsState(emptyList())
    Cabecalho("Alimento", fechar)
    CampoTexto(q, { q = it }, "Buscar na tabela TACO e nos seus alimentos")
    Espaco(10.dp)
    BotaoContorno("+ Cadastrar alimento pelo rótulo", novo, Modifier.fillMaxWidth(), cor = Cores.destaque)
    Espaco(6.dp)
    LazyColumn(Modifier.weight(1f)) {
        items(resultados, key = { it.id }) { a ->
            Row(
                Modifier.fillMaxWidth().clickable { escolher(a) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.nome + if (a.marca.isNotBlank()) " · ${a.marca}" else "", style = Tipos.corpo)
                    Text(
                        "${a.fonte} · ${Fmt.kcal(a.kcal100)} kcal · P ${Fmt.gramas(a.prot100)} · C ${Fmt.gramas(a.carb100)} · G ${Fmt.gramas(a.gord100)} por 100 ${a.medida}",
                        style = Tipos.pequeno,
                    )
                }
                if (a.fonte != "TACO") BotaoContorno("Editar", { editar(a) }, Modifier.padding(start = 8.dp))
            }
            LinhaDivisoria()
        }
        if (resultados.isEmpty()) item {
            Text("Nada encontrado. Você pode cadastrar o alimento com os dados do rótulo.", style = Tipos.corpoSuave, modifier = Modifier.padding(vertical = 24.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Quantidade(a: Alimento, voltar: () -> Unit, escolher: (Escolha) -> Unit) {
    val temUnidade = a.unidadeNome != null && (a.unidadeGramas ?: 0.0) > 0
    var emUnidades by remember { mutableStateOf(temUnidade) }
    var texto by remember { mutableStateOf(if (temUnidade) "1" else "100") }
    val n = Fmt.lerNumero(texto)
    val gramas = n?.let { if (emUnidades) it * a.unidadeGramas!! else it }
    Cabecalho("Quantidade", voltar)
    Text(a.nome, style = Tipos.corpoForte)
    Text("${a.fonte} · valores por 100 ${a.medida}", style = Tipos.pequeno)
    Espaco(18.dp)
    if (temUnidade) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipEscolha(a.unidadeNome!!, emUnidades, { emUnidades = true; texto = "1" })
            ChipEscolha(a.medida, !emUnidades, { emUnidades = false; texto = Fmt.quantidade(a.unidadeGramas!!) })
        }
        Espaco(12.dp)
    }
    CampoTexto(texto, { texto = it }, "Quantidade", numerico = true, sufixo = if (emUnidades) a.unidadeNome else a.medida)
    Espaco(18.dp)
    if (gramas != null) {
        val m = a.macros(gramas)
        Cartao {
            Column {
                if (emUnidades) Text("${Fmt.quantidade(gramas)} ${a.medida}", style = Tipos.pequeno)
                LinhaValor("Calorias", "${Fmt.kcal(m.kcal)} kcal", destaque = true)
                LinhaValor("Proteínas", "${Fmt.gramas(m.prot)} g")
                LinhaValor("Carboidratos", "${Fmt.gramas(m.carb)} g")
                LinhaValor("Gorduras", "${Fmt.gramas(m.gord)} g")
            }
        }
    }
    Espaco(18.dp)
    BotaoPrincipal("Adicionar", { escolher(Escolha(a, gramas!!, if (emUnidades) n else null)) }, ativo = gramas != null && gramas > 0)
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Cadastro(base: Alimento?, voltar: () -> Unit, salvo: (Alimento) -> Unit) {
    val repo = LocalContext.current.app.repo
    val escopo = rememberCoroutineScope()
    fun t(v: Double?) = v?.let { Fmt.quantidade(it) } ?: ""
    var nome by remember { mutableStateOf(base?.nome ?: "") }
    var marca by remember { mutableStateOf(base?.marca ?: "") }
    var emMl by remember { mutableStateOf(base?.emMl ?: false) }
    var porcao by remember { mutableStateOf("100") }
    var kcal by remember { mutableStateOf(t(base?.kcal100)) }
    var prot by remember { mutableStateOf(t(base?.prot100)) }
    var carb by remember { mutableStateOf(t(base?.carb100)) }
    var gord by remember { mutableStateOf(t(base?.gord100)) }
    var unNome by remember { mutableStateOf(base?.unidadeNome ?: "") }
    var unGramas by remember { mutableStateOf(t(base?.unidadeGramas)) }
    val medida = if (emMl) "ml" else "g"
    val p = Fmt.lerNumero(porcao)?.takeIf { it > 0 }
    val valores = listOf(kcal, prot, carb, gord).map { Fmt.lerNumero(it) }
    val unidadeOk = unNome.isBlank() || (Fmt.lerNumero(unGramas) ?: 0.0) > 0
    val valido = nome.isNotBlank() && p != null && valores.none { it == null } && unidadeOk

    Cabecalho(if (base == null) "Novo alimento" else "Editar alimento", voltar)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Copie os valores da tabela nutricional da embalagem. O app converte para 100 $medida.", style = Tipos.corpoSuave)
        CampoTexto(nome, { nome = it }, "Nome")
        CampoTexto(marca, { marca = it }, "Marca (opcional)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipEscolha("Sólido (g)", !emMl, { emMl = false })
            ChipEscolha("Líquido (ml)", emMl, { emMl = true })
        }
        CampoTexto(porcao, { porcao = it }, "Porção do rótulo", numerico = true, sufixo = medida)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoTexto(kcal, { kcal = it }, "Calorias", Modifier.weight(1f), numerico = true, sufixo = "kcal")
            CampoTexto(prot, { prot = it }, "Proteínas", Modifier.weight(1f), numerico = true, sufixo = "g")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoTexto(carb, { carb = it }, "Carboidratos", Modifier.weight(1f), numerico = true, sufixo = "g")
            CampoTexto(gord, { gord = it }, "Gorduras", Modifier.weight(1f), numerico = true, sufixo = "g")
        }
        Text("Medida caseira (opcional). Ex.: fatia = 25 g.", style = Tipos.corpoSuave)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoTexto(unNome, { unNome = it }, "Nome da medida", Modifier.weight(1f))
            CampoTexto(unGramas, { unGramas = it }, "Equivale a", Modifier.weight(1f), numerico = true, sufixo = medida)
        }
        Espaco(4.dp)
    }
    BotaoPrincipal("Salvar", {
        val f = 100.0 / p!!
        val a = Alimento(
            id = base?.id ?: 0, nome = nome.trim(), marca = marca.trim(), fonte = if (base?.fonte == "Manual") "Manual" else "Rótulo",
            kcal100 = valores[0]!! * f, prot100 = valores[1]!! * f, carb100 = valores[2]!! * f, gord100 = valores[3]!! * f,
            emMl = emMl, unidadeNome = unNome.trim().ifBlank { null }, unidadeGramas = if (unNome.isBlank()) null else Fmt.lerNumero(unGramas),
        )
        escopo.launch { salvo(a.copy(id = repo.salvarAlimento(a))) }
    }, Modifier.padding(vertical = 14.dp), ativo = valido)
}
