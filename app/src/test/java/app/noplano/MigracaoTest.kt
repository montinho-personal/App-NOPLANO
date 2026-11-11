package app.noplano

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.noplano.data.Banco
import app.noplano.data.Repositorio
import app.noplano.domain.Alvo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Simula um celular com a versão 1 do banco (antes do alvo de macros), abre com a versão
 * atual e confere que nada se perdeu.
 */
@RunWith(AndroidJUnit4::class)
class MigracaoTest {
    private fun criarVersao1(ctx: Context, nome: String) {
        val esquema = Json.parseToJsonElement(File("schemas/app.noplano.data.Banco/1.json").readText()).jsonObject["database"]!!.jsonObject
        val arquivo = ctx.getDatabasePath(nome).also { it.parentFile?.mkdirs(); it.delete() }
        SQLiteDatabase.openOrCreateDatabase(arquivo, null).use { db ->
            for (e in esquema["entities"]!!.jsonArray) {
                val t = e.jsonObject["tableName"]!!.jsonPrimitive.content
                db.execSQL(e.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", t))
                e.jsonObject["indices"]?.jsonArray?.forEach { i -> db.execSQL(i.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", t)) }
            }
            esquema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL("INSERT INTO Plano (id, nome, ordem) VALUES (1, 'Meu plano', 0)")
            db.execSQL("INSERT INTO DiaSemana (dia, planoId) VALUES (1, 1)")
            db.execSQL("INSERT INTO Refeicao (id, planoId, nome, minutos, obs, ordem) VALUES (5, 1, 'Almoço', 750, '', 0)")
            db.execSQL("INSERT INTO Registro (id, data, refeicaoId, nome, minutosPlanejado, registradoEm, fracao, ajustado) VALUES (9, 20000, 5, 'Almoço', 750, 0, 1.0, 0)")
            db.execSQL("INSERT INTO Agua (id, data, ml, em) VALUES (3, 20000, 750, 0)")
            db.version = 1
        }
    }

    @Test fun `versao 1 para 2 preserva os dados e acrescenta o alvo vazio`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        criarVersao1(ctx, "migracao.db")
        val banco = Room.databaseBuilder(ctx, Banco::class.java, "migracao.db").addMigrations(*Banco.MIGRACOES).allowMainThreadQueries().build()
        val repo = Repositorio(banco, AjustesFalsos())
        val plano = repo.plano(1).first()!!
        assertEquals("Meu plano", plano.nome)
        assertTrue(plano.alvo.vazio)
        assertEquals("Almoço", repo.refeicoes(1).first().single().refeicao.nome)
        assertEquals(1, banco.registroDao().entre(20000, 20000).size)
        assertEquals(750, banco.registroDao().aguaTotal(20000))
        repo.salvarAlvo(1, Alvo(prot = 170.0))
        assertEquals(170.0, repo.plano(1).first()!!.alvo.prot!!, 1e-9)
        banco.close()
    }
}
