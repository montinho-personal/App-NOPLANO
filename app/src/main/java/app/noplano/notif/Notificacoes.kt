package app.noplano.notif

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.noplano.MainActivity
import app.noplano.NoPlanoApp
import app.noplano.R
import app.noplano.app
import app.noplano.domain.Lembrete
import app.noplano.domain.Lembretes
import app.noplano.domain.TipoLembrete
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object Canais {
    const val REFEICOES = "refeicoes"
    const val AGUA = "agua"
    const val RESUMO = "resumo"

    fun criar(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(REFEICOES, "Refeições", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Lembretes antes da refeição e de refeição ainda sem registro"
                },
                NotificationChannel(AGUA, "Hidratação", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Lembretes de água quando o consumo está abaixo do ritmo"
                },
                NotificationChannel(RESUMO, "Resumo do dia", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Um resumo à noite"
                },
            )
        )
    }

    fun permitido(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
}

private const val EXTRA_TIPO = "tipo"
private const val EXTRA_QUANDO = "quando"
private const val EXTRA_REFEICAO = "refeicao"
private const val EXTRA_TITULO = "titulo"
private const val EXTRA_TEXTO = "texto"
private const val EXTRA_DATA = "data"
private const val EXTRA_NOTIF = "notif"
private const val ACAO_CONCLUIR = "app.noplano.CONCLUIR"
private const val ACAO_AGUA = "app.noplano.AGUA"
private const val ID_AGUA = 2
private const val ID_RESUMO = 3

private fun idRefeicao(refeicaoId: Long): Int = 1000 + (refeicaoId % 100_000).toInt()
private const val IMUTAVEL = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/**
 * Agenda sempre um único alarme: o do próximo lembrete do dia (ou a virada do dia).
 * Quando ele dispara, a regra é reavaliada com os dados atuais e o seguinte é agendado.
 */
object Agendador {
    fun exatoPermitido(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    suspend fun reagendar(context: Context) {
        val repo = context.app.repo
        val relogio = repo.relogio
        val hoje = relogio.hoje()
        val dia = repo.estadoDia(hoje)
        val ajustes = repo.ajustes.atual()

        // Refeição registrada: some a notificação dela, se ainda estiver na tela.
        val nm = NotificationManagerCompat.from(context)
        dia.refeicoes.filter { it.feita }.forEach { nm.cancel(idRefeicao(it.id)) }
        if (dia.aguaMl >= dia.metaAguaMl) nm.cancel(ID_AGUA)

        val proximo = Lembretes.proximo(dia, ajustes, relogio.agoraMin())
        val intent = Intent(context, LembreteReceiver::class.java)
        val quando: Long
        if (proximo != null) {
            intent.putExtra(EXTRA_TIPO, proximo.tipo.name).putExtra(EXTRA_QUANDO, proximo.quandoMin)
                .putExtra(EXTRA_REFEICAO, proximo.refeicaoId ?: -1L)
                .putExtra(EXTRA_TITULO, proximo.titulo).putExtra(EXTRA_TEXTO, proximo.texto)
            quando = relogio.millisDe(hoje, proximo.quandoMin)
        } else {
            quando = relogio.millisDe(hoje + 1, 1) // 00h01: novo dia, novo plano
        }
        val pi = PendingIntent.getBroadcast(context, 1, intent, IMUTAVEL)
        val am = context.getSystemService(AlarmManager::class.java)
        runCatching {
            if (exatoPermitido(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
        }.onFailure { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi) }
    }

    /** Rede de segurança: a cada 6 h o WorkManager confere widget e alarmes. */
    fun agendarManutencao(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "manutencao", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ManutencaoWorker>(6, TimeUnit.HOURS).build(),
        )
    }

    suspend fun exibir(context: Context, l: Lembrete) {
        if (!Canais.permitido(context)) return
        val repo = context.app.repo
        val hoje = repo.relogio.hoje()
        val abrir = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), IMUTAVEL)
        val (canal, id) = when (l.tipo) {
            TipoLembrete.ANTES, TipoLembrete.PENDENTE -> Canais.REFEICOES to idRefeicao(l.refeicaoId ?: 0)
            TipoLembrete.AGUA -> Canais.AGUA to ID_AGUA
            TipoLembrete.RESUMO -> Canais.RESUMO to ID_RESUMO
        }
        val b = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(l.titulo).setContentText(l.texto)
            .setColor(0xFFFFD60A.toInt())
            .setContentIntent(abrir).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
        fun acao(nome: String, acao: String, codigo: Int) = PendingIntent.getBroadcast(
            context, codigo,
            Intent(context, AcaoReceiver::class.java).setAction(acao)
                .putExtra(EXTRA_REFEICAO, l.refeicaoId ?: -1L).putExtra(EXTRA_DATA, hoje).putExtra(EXTRA_NOTIF, id),
            IMUTAVEL,
        ).let { b.addAction(0, nome, it) }
        when (l.tipo) {
            TipoLembrete.ANTES, TipoLembrete.PENDENTE -> acao("Concluí", ACAO_CONCLUIR, id)
            TipoLembrete.AGUA -> acao("+250 ml", ACAO_AGUA, id)
            TipoLembrete.RESUMO -> Unit
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, b.build())
        context.app.preferencias.contarNotificacao(hoje)
    }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendente = goAsync()
        context.app.escopo.launch {
            try {
                val tipo = intent.getStringExtra(EXTRA_TIPO)?.let { runCatching { TipoLembrete.valueOf(it) }.getOrNull() }
                if (tipo != null) {
                    val l = Lembrete(
                        tipo, intent.getIntExtra(EXTRA_QUANDO, 0), intent.getLongExtra(EXTRA_REFEICAO, -1).takeIf { it >= 0 },
                        intent.getStringExtra(EXTRA_TITULO) ?: "", intent.getStringExtra(EXTRA_TEXTO) ?: "",
                    )
                    val repo = context.app.repo
                    val hoje = repo.relogio.hoje()
                    val dia = repo.estadoDia(hoje)
                    val ajustes = repo.ajustes.atual()
                    // Reavalia com os dados de agora: se a refeição já foi registrada, não mostra.
                    val vigente = Lembretes.doDia(dia, ajustes).firstOrNull { it.chave == l.chave }
                    if (vigente != null && Lembretes.deveExibir(vigente, dia, ajustes, context.app.preferencias.notificacoesExibidas(hoje))) {
                        Agendador.exibir(context, vigente)
                    }
                }
                NoPlanoApp.sincronizar(context)
            } finally {
                pendente.finish()
            }
        }
    }
}

/** Botões das notificações: registrar a refeição ou adicionar água sem abrir o app. */
class AcaoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendente = goAsync()
        context.app.escopo.launch {
            try {
                val repo = context.app.repo
                val data = intent.getLongExtra(EXTRA_DATA, repo.relogio.hoje())
                when (intent.action) {
                    ACAO_CONCLUIR -> intent.getLongExtra(EXTRA_REFEICAO, -1).takeIf { it >= 0 }?.let { runCatching { repo.concluir(data, it) } }
                    ACAO_AGUA -> repo.adicionarAgua(repo.relogio.hoje(), 250)
                }
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIF, 0))
            } finally {
                pendente.finish()
            }
        }
    }
}

/** Depois de reiniciar o celular, mudar a hora ou atualizar o app, os alarmes precisam ser recriados. */
class ReinicioReceiver : BroadcastReceiver() {
    private val esperadas = setOf(
        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in esperadas) return
        val pendente = goAsync()
        context.app.escopo.launch {
            try { NoPlanoApp.sincronizar(context) } finally { pendente.finish() }
        }
    }
}

class ManutencaoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NoPlanoApp.sincronizar(applicationContext)
        return Result.success()
    }
}
