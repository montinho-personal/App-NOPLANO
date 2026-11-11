package app.noplano.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.noplano.domain.Ajustes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.armazem: DataStore<Preferences> by preferencesDataStore("ajustes")

/** Onde os ajustes ficam guardados. Interface para os testes poderem usar memória. */
interface FonteAjustes {
    val fluxo: Flow<Ajustes>
    suspend fun atual(): Ajustes = fluxo.first()
    suspend fun alterar(f: (Ajustes) -> Ajustes)
}

class Preferencias(private val context: Context) : FonteAjustes {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val chave = stringPreferencesKey("ajustes")
    private val chaveDiaNotif = longPreferencesKey("notif_dia")
    private val chaveContNotif = intPreferencesKey("notif_contagem")

    private fun ler(p: Preferences): Ajustes =
        p[chave]?.let { runCatching { json.decodeFromString<Ajustes>(it) }.getOrNull() } ?: Ajustes()

    override val fluxo: Flow<Ajustes> = context.armazem.data.map(::ler).distinctUntilChanged()

    override suspend fun alterar(f: (Ajustes) -> Ajustes) {
        context.armazem.edit { it[chave] = json.encodeToString(Ajustes.serializer(), f(ler(it))) }
    }

    /** Quantas notificações já foram exibidas em [dia]. */
    suspend fun notificacoesExibidas(dia: Long): Int {
        val p = context.armazem.data.first()
        return if (p[chaveDiaNotif] == dia) p[chaveContNotif] ?: 0 else 0
    }

    suspend fun contarNotificacao(dia: Long) {
        context.armazem.edit {
            val atual = if (it[chaveDiaNotif] == dia) it[chaveContNotif] ?: 0 else 0
            it[chaveDiaNotif] = dia
            it[chaveContNotif] = atual + 1
        }
    }
}
