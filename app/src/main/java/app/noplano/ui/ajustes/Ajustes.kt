package app.noplano.ui.ajustes

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.noplano.NoPlanoApp
import app.noplano.app
import app.noplano.data.BackupInvalido
import app.noplano.data.Taco
import app.noplano.domain.Ajustes
import app.noplano.domain.Fmt
import app.noplano.notif.Agendador
import app.noplano.notif.Canais
import app.noplano.ui.BotaoContorno
import app.noplano.ui.Cores
import app.noplano.ui.DialogoConfirmar
import app.noplano.ui.DialogoHora
import app.noplano.ui.DialogoTexto
import app.noplano.ui.Espaco
import app.noplano.ui.LinhaDivisoria
import app.noplano.ui.Margem
import app.noplano.ui.Rotulo
import app.noplano.ui.Tipos
import app.noplano.ui.TituloSecao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private sealed interface Dlg {
    data class Numero(val titulo: String, val rotulo: String, val valor: Int, val sufixo: String, val faixa: IntRange, val salvar: (Ajustes, Int) -> Ajustes) : Dlg
    data class Hora(val titulo: String, val valor: Int, val salvar: (Ajustes, Int) -> Ajustes) : Dlg
    data class Restaurar(val uri: Uri) : Dlg
}

@Composable
fun AjustesTela(voltar: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val escopo = rememberCoroutineScope()
    val a by app.preferencias.fluxo.collectAsState(Ajustes())
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var notifOk by remember { mutableStateOf(Canais.permitido(context)) }
    var exatoOk by remember { mutableStateOf(Agendador.exatoPermitido(context)) }
    LifecycleResumeEffect(Unit) {
        notifOk = Canais.permitido(context); exatoOk = Agendador.exatoPermitido(context)
        onPauseOrDispose { }
    }

    fun mudar(f: (Ajustes) -> Ajustes) = escopo.launch {
        app.preferencias.alterar(f)
        NoPlanoApp.sincronizar(context)
    }

    val pedirNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOk = Canais.permitido(context)
        escopo.launch { NoPlanoApp.sincronizar(context) }
    }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) escopo.launch {
            mensagem = runCatching {
                val texto = app.backup.exportar(System.currentTimeMillis())
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(texto.toByteArray()) } }
                "Backup salvo."
            }.getOrElse { "Não foi possível salvar o backup." }
        }
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) dlg = Dlg.Restaurar(uri) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Margem).padding(top = 18.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AJUSTES", style = Tipos.titulo, modifier = Modifier.weight(1f).semantics { heading() })
            BotaoContorno("Voltar", voltar)
        }
        mensagem?.let { Espaco(10.dp); Text(it, style = Tipos.corpoForte, color = Cores.destaque) }

        TituloSecao("Dia")
        Valor("Meta de água", "${Fmt.litros(a.metaAguaMl)} L") {
            dlg = Dlg.Numero("Meta de água", "Por dia", a.metaAguaMl, "ml", 250..10000) { x, v -> x.copy(metaAguaMl = v) }
        }
        Chave("Vibrar ao registrar", "Vibração curta ao concluir refeição e ao adicionar água.", a.vibrar) { v -> mudar { it.copy(vibrar = v) } }

        TituloSecao("Notificações")
        if (!notifOk) {
            Text("As notificações estão desligadas para o app. Sem elas, os lembretes não aparecem.", style = Tipos.corpoSuave)
            Espaco(8.dp)
            BotaoContorno("Permitir notificações", {
                if (Build.VERSION.SDK_INT >= 33) pedirNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, Modifier.fillMaxWidth(), cor = Cores.destaque)
            Espaco(8.dp)
        }
        if (!exatoOk && Build.VERSION.SDK_INT >= 31) {
            Text("Para os lembretes chegarem na hora exata, libere \"Alarmes e lembretes\". Sem isso, podem atrasar alguns minutos.", style = Tipos.corpoSuave)
            Espaco(8.dp)
            BotaoContorno("Liberar hora exata", {
                runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
            }, Modifier.fillMaxWidth())
            Espaco(8.dp)
        }
        Chave("Antes da refeição", "\"Seu almoço está chegando. Tudo preparado?\"", a.avisarAntes) { v -> mudar { it.copy(avisarAntes = v) } }
        if (a.avisarAntes) Valor("Antecedência", "${a.minutosAntes} min") {
            dlg = Dlg.Numero("Antecedência", "Minutos antes da refeição", a.minutosAntes, "min", 0..120) { x, v -> x.copy(minutosAntes = v) }
        }
        Chave("Refeição sem registro", "Um único aviso depois do horário. Não aparece se você já registrou.", a.avisarPendente) { v -> mudar { it.copy(avisarPendente = v) } }
        if (a.avisarPendente) Valor("Avisar depois de", "${a.minutosPendente} min") {
            dlg = Dlg.Numero("Refeição sem registro", "Minutos depois do horário", a.minutosPendente, "min", 5..240) { x, v -> x.copy(minutosPendente = v) }
        }
        Chave("Hidratação", "Só aparece se a água estiver abaixo do ritmo do dia.", a.avisarAgua) { v -> mudar { it.copy(avisarAgua = v) } }
        if (a.avisarAgua) Valor("Intervalo", "${a.intervaloAguaMin} min") {
            dlg = Dlg.Numero("Hidratação", "Intervalo entre lembretes", a.intervaloAguaMin, "min", 30..480) { x, v -> x.copy(intervaloAguaMin = v) }
        }
        Chave("Resumo do dia", "Uma notificação à noite com o seu dia.", a.avisarResumo) { v -> mudar { it.copy(avisarResumo = v) } }
        if (a.avisarResumo) Valor("Horário do resumo", Fmt.hora(a.resumoMin)) { dlg = Dlg.Hora("Resumo do dia", a.resumoMin) { x, v -> x.copy(resumoMin = v) } }
        Valor("Silêncio começa", Fmt.hora(a.silencioInicioMin)) { dlg = Dlg.Hora("Silêncio começa", a.silencioInicioMin) { x, v -> x.copy(silencioInicioMin = v) } }
        Valor("Silêncio termina", Fmt.hora(a.silencioFimMin)) { dlg = Dlg.Hora("Silêncio termina", a.silencioFimMin) { x, v -> x.copy(silencioFimMin = v) } }
        Valor("Máximo por dia", "${a.tetoDiario}") {
            dlg = Dlg.Numero("Máximo por dia", "Notificações por dia", a.tetoDiario, "", 1..30) { x, v -> x.copy(tetoDiario = v) }
        }
        Espaco(6.dp)
        Text(
            "Se os lembretes não chegarem, alguns celulares exigem tirar o app da economia de bateria: Configurações > Apps > No Plano > Bateria > Sem restrições.",
            style = Tipos.pequeno,
        )

        TituloSecao("Widget")
        Text("Para adicionar: segure um espaço vazio da tela inicial, toque em Widgets e escolha No Plano. O Android não permite widgets de apps na tela de bloqueio dos celulares; lá aparecem as notificações.", style = Tipos.pequeno)
        Espaco(6.dp)
        Chave("Calorias", null, a.widgetCalorias) { v -> mudar { it.copy(widgetCalorias = v) } }
        Chave("Progresso do dia", null, a.widgetProgresso) { v -> mudar { it.copy(widgetProgresso = v) } }
        Chave("Proteínas", null, a.widgetProteinas) { v -> mudar { it.copy(widgetProteinas = v) } }
        Chave("Água", null, a.widgetAgua) { v -> mudar { it.copy(widgetAgua = v) } }
        Chave("Próxima refeição", null, a.widgetProxima) { v -> mudar { it.copy(widgetProxima = v) } }

        TituloSecao("Backup")
        Text("Seus dados ficam só neste aparelho. Salve um backup antes de trocar de celular ou reinstalar o app.", style = Tipos.pequeno)
        Espaco(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Salvar backup", { exportar.launch("noplano-${LocalDate.now()}.json") }, Modifier.weight(1f), cor = Cores.destaque)
            BotaoContorno("Restaurar", { abrir.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, Modifier.weight(1f))
        }

        TituloSecao("Sobre")
        Text("Dados nutricionais: ${Taco.REFERENCIA}. Valores médios por 100 g. Produtos de marca devem ser cadastrados pelo rótulo.", style = Tipos.pequeno)
        Espaco(8.dp)
        Text("O app acompanha um plano que você cadastra. Ele não prescreve dietas e não substitui nutricionista. Sem internet, sem conta, sem anúncios.", style = Tipos.pequeno)
        Espaco(8.dp)
        Text("Versão ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}", style = Tipos.pequeno)
    }

    when (val d = dlg) {
        is Dlg.Numero -> DialogoTexto(
            d.titulo, d.rotulo, d.valor.toString(), { dlg = null }, numerico = true, sufixo = d.sufixo.ifBlank { null },
            validar = { Fmt.lerNumero(it)?.toInt() in d.faixa },
        ) { texto -> mudar { d.salvar(it, Fmt.lerNumero(texto)!!.toInt()) }; dlg = null }
        is Dlg.Hora -> DialogoHora(d.titulo, d.valor, { dlg = null }) { v -> mudar { d.salvar(it, v) }; dlg = null }
        is Dlg.Restaurar -> DialogoConfirmar(
            "Restaurar backup", "Os dados atuais deste aparelho serão substituídos pelos do arquivo.", "Restaurar", { dlg = null },
        ) {
            dlg = null
            escopo.launch {
                mensagem = try {
                    val texto = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(d.uri)!!.use { it.readBytes().decodeToString() } }
                    app.backup.restaurar(texto)
                    Taco.garantir(context, app.banco)
                    app.repo.garantirInicial()
                    NoPlanoApp.sincronizar(context)
                    "Backup restaurado."
                } catch (e: BackupInvalido) {
                    e.message
                } catch (e: Exception) {
                    "Não foi possível ler o arquivo."
                }
            }
        }
        null -> Unit
    }
}

@Composable
private fun Chave(titulo: String, descricao: String?, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(ligado, role = Role.Switch, onValueChange = aoMudar).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(titulo, style = Tipos.corpoForte)
            if (descricao != null) Text(descricao, style = Tipos.pequeno)
        }
        Box(
            Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 32.dp)
                .background(if (ligado) Cores.destaque else Cores.fundo)
                .border(1.dp, if (ligado) Cores.destaque else Cores.linha),
            contentAlignment = Alignment.Center,
        ) { Text(if (ligado) "SIM" else "NÃO", style = Tipos.rotuloGrande, color = if (ligado) Cores.sobreDestaque else Cores.textoSuave) }
    }
    LinhaDivisoria()
}

@Composable
private fun Valor(titulo: String, valor: String, aoTocar: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = aoTocar), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = Tipos.corpoForte, modifier = Modifier.weight(1f))
        Text(valor, style = Tipos.numeroPequeno, color = Cores.destaque)
    }
    LinhaDivisoria()
}
