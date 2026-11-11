package app.noplano

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.noplano.notif.Canais
import app.noplano.ui.Cores
import app.noplano.ui.TemaNoPlano
import app.noplano.ui.Tipos
import app.noplano.ui.ajustes.AjustesTela
import app.noplano.ui.hoje.HojeTela
import app.noplano.ui.plano.EditorRefeicaoTela
import app.noplano.ui.plano.PlanoTela
import app.noplano.ui.progresso.ProgressoTela
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { TemaNoPlano { Raiz() } }
    }

    override fun onResume() {
        super.onResume()
        // Ao voltar para o app, confere widget e lembretes (a data pode ter mudado).
        app.escopo.launch { NoPlanoApp.sincronizar(this@MainActivity) }
    }
}

private val abas = listOf("hoje" to "Hoje", "plano" to "Plano", "progresso" to "Progresso")

@Composable
private fun Raiz() {
    val nav = rememberNavController()
    val rota = nav.currentBackStackEntryAsState().value?.destination?.route
    PedirNotificacaoUmaVez()

    fun irAba(destino: String) = nav.navigate(destino) {
        popUpTo("hoje") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Column(Modifier.fillMaxSize().background(Cores.fundo).systemBarsPadding()) {
        NavHost(nav, startDestination = "hoje", modifier = Modifier.weight(1f)) {
            composable("hoje") { HojeTela({ nav.navigate("ajustes") }, { irAba("progresso") }, { irAba("plano") }) }
            composable("plano") { PlanoTela({ nav.navigate("refeicao/$it") }) }
            composable("progresso") { ProgressoTela() }
            composable("ajustes") { AjustesTela { nav.popBackStack() } }
            composable("refeicao/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
                EditorRefeicaoTela(
                    entrada.arguments!!.getLong("id"), { nav.popBackStack() },
                    { nav.navigate("refeicao/$it") { popUpTo("plano") } },
                    irParaPlano = { nav.popBackStack("plano", inclusive = false) },
                )
            }
        }
        if (rota in abas.map { it.first }) BarraAbas(rota, ::irAba)
    }
}

@Composable
private fun BarraAbas(rota: String?, ir: (String) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Cores.trilho))
        Row(Modifier.fillMaxWidth().background(Cores.fundo)) {
            abas.forEach { (destino, nome) ->
                val sel = rota == destino
                Column(
                    Modifier.weight(1f).heightIn(min = 60.dp).clickable(role = Role.Tab) { ir(destino) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (sel) Cores.destaque else Cores.fundo))
                    Box(Modifier.heightIn(min = 57.dp), contentAlignment = Alignment.Center) {
                        Text(nome.uppercase(), style = Tipos.botao.copy(fontSize = Tipos.numeroPequeno.fontSize), color = if (sel) Cores.destaque else Cores.textoSuave)
                    }
                }
            }
        }
    }
}

/** Pede a permissão de notificações uma única vez, no primeiro uso (Android 13+). */
@Composable
private fun PedirNotificacaoUmaVez() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        context.app.escopo.launch { NoPlanoApp.sincronizar(context) }
    }
    LaunchedEffect(Unit) {
        val prefs = context.app.preferencias
        if (Build.VERSION.SDK_INT >= 33 && !Canais.permitido(context) && !prefs.atual().pediuNotificacao) {
            prefs.alterar { it.copy(pediuNotificacao = true) }
            pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
