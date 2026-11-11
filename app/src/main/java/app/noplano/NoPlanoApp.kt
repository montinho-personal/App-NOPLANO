package app.noplano

import android.app.Application
import android.content.Context
import app.noplano.data.Backup
import app.noplano.data.Banco
import app.noplano.data.Preferencias
import app.noplano.data.Repositorio
import app.noplano.data.Taco
import app.noplano.notif.Agendador
import app.noplano.notif.Canais
import app.noplano.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoPlanoApp : Application() {
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val banco: Banco by lazy { Banco.abrir(this) }
    val preferencias: Preferencias by lazy { Preferencias(this) }
    val repo: Repositorio by lazy {
        Repositorio(banco, preferencias).also { it.aoMudar = { sincronizar(this) } }
    }
    val backup: Backup by lazy { Backup(banco, preferencias) }

    override fun onCreate() {
        super.onCreate()
        Canais.criar(this)
        escopo.launch {
            repo.garantirInicial()
            Taco.garantir(this@NoPlanoApp, banco)
            sincronizar(this@NoPlanoApp)
            runCatching { Agendador.agendarManutencao(this@NoPlanoApp) }
        }
    }

    companion object {
        /** Depois de qualquer mudança nos dados: atualiza os widgets e reagenda os lembretes. */
        suspend fun sincronizar(context: Context) {
            runCatching { Widgets.atualizar(context) }
            runCatching { Agendador.reagendar(context) }
        }
    }
}

val Context.app: NoPlanoApp get() = applicationContext as NoPlanoApp
