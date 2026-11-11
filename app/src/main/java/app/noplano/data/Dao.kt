package app.noplano.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanoDao {
    @Query("SELECT * FROM Plano ORDER BY ordem, id") fun planos(): Flow<List<Plano>>
    @Query("SELECT * FROM Plano ORDER BY ordem, id") suspend fun planosAgora(): List<Plano>
    @Query("SELECT * FROM Plano WHERE id = :id") suspend fun plano(id: Long): Plano?
    @Insert suspend fun inserir(p: Plano): Long
    @Update suspend fun atualizar(p: Plano)
    @Delete suspend fun excluir(p: Plano)

    @Query("SELECT * FROM DiaSemana ORDER BY dia") fun diasSemana(): Flow<List<DiaSemana>>
    @Query("SELECT * FROM DiaSemana ORDER BY dia") suspend fun diasSemanaAgora(): List<DiaSemana>
    @Query("SELECT planoId FROM DiaSemana WHERE dia = :dia") suspend fun planoDoDia(dia: Int): Long?
    @Query("SELECT planoId FROM DiaSemana WHERE dia = :dia") fun planoDoDiaFluxo(dia: Int): Flow<Long?>
    @Upsert suspend fun definirDia(d: DiaSemana)
    @Query("DELETE FROM DiaSemana WHERE planoId = :planoId") suspend fun limparDiasDoPlano(planoId: Long)

    @Transaction
    @Query("SELECT * FROM Refeicao WHERE planoId = :planoId ORDER BY minutos, ordem, id")
    fun refeicoes(planoId: Long): Flow<List<RefeicaoCompleta>>

    @Transaction
    @Query("SELECT * FROM Refeicao WHERE planoId = :planoId ORDER BY minutos, ordem, id")
    suspend fun refeicoesAgora(planoId: Long): List<RefeicaoCompleta>

    @Transaction
    @Query("SELECT * FROM Refeicao WHERE id = :id")
    fun refeicao(id: Long): Flow<RefeicaoCompleta?>

    @Transaction
    @Query("SELECT * FROM Refeicao WHERE id = :id")
    suspend fun refeicaoAgora(id: Long): RefeicaoCompleta?

    @Insert suspend fun inserir(r: Refeicao): Long
    @Update suspend fun atualizar(r: Refeicao)
    @Query("DELETE FROM Refeicao WHERE id = :id") suspend fun excluirRefeicao(id: Long)

    @Insert suspend fun inserir(o: Opcao): Long
    @Update suspend fun atualizar(o: Opcao)
    @Query("DELETE FROM Opcao WHERE id = :id") suspend fun excluirOpcao(id: Long)

    @Insert suspend fun inserir(i: ItemPlano): Long
    @Update suspend fun atualizar(i: ItemPlano)
    @Query("DELETE FROM ItemPlano WHERE id = :id") suspend fun excluirItem(id: Long)
    @Query("SELECT COUNT(*) FROM ItemPlano WHERE alimentoId = :alimentoId") suspend fun usosDoAlimento(alimentoId: Long): Int
}

@Dao
interface AlimentoDao {
    @Query(
        "SELECT * FROM Alimento WHERE arquivado = 0 AND (nome LIKE '%' || :q || '%' OR marca LIKE '%' || :q || '%') " +
            "ORDER BY (fonte = 'TACO'), nome LIMIT 60"
    )
    fun buscar(q: String): Flow<List<Alimento>>

    @Query("SELECT * FROM Alimento WHERE id = :id") suspend fun porId(id: Long): Alimento?
    @Query("SELECT * FROM Alimento") suspend fun todos(): List<Alimento>
    @Query("SELECT COUNT(*) FROM Alimento WHERE fonte = 'TACO'") suspend fun totalTaco(): Int
    @Query("SELECT * FROM Alimento WHERE tacoId = :tacoId LIMIT 1") suspend fun porTaco(tacoId: Int): Alimento?
    @Insert suspend fun inserir(a: Alimento): Long
    @Insert suspend fun inserirTodos(a: List<Alimento>)
    @Update suspend fun atualizar(a: Alimento)
    @Query("DELETE FROM Alimento WHERE id = :id") suspend fun excluir(id: Long)
}

@Dao
interface RegistroDao {
    @Transaction
    @Query("SELECT * FROM Registro WHERE data = :data ORDER BY registradoEm, id")
    fun doDia(data: Long): Flow<List<RegistroComItens>>

    @Transaction
    @Query("SELECT * FROM Registro WHERE data = :data ORDER BY registradoEm, id")
    suspend fun doDiaAgora(data: Long): List<RegistroComItens>

    @Transaction
    @Query("SELECT * FROM Registro WHERE data BETWEEN :de AND :ate ORDER BY data, registradoEm")
    suspend fun entre(de: Long, ate: Long): List<RegistroComItens>

    @Transaction
    @Query("SELECT * FROM Registro WHERE id = :id")
    suspend fun porId(id: Long): RegistroComItens?

    @Query("SELECT id FROM Registro WHERE data = :data AND refeicaoId = :refeicaoId")
    suspend fun idDoPlano(data: Long, refeicaoId: Long): Long?

    @Query("SELECT DISTINCT data FROM Registro ORDER BY data") suspend fun diasComRegistro(): List<Long>
    @Query("SELECT DISTINCT data FROM Registro ORDER BY data") fun diasComRegistroFluxo(): Flow<List<Long>>
    @Query("SELECT COUNT(*) FROM Registro WHERE refeicaoId IS NOT NULL") suspend fun totalRefeicoesDoPlano(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun inserir(r: Registro): Long
    @Update suspend fun atualizar(r: Registro)
    @Insert suspend fun inserirItens(i: List<ItemRegistro>)
    @Query("DELETE FROM ItemRegistro WHERE registroId = :registroId") suspend fun limparItens(registroId: Long)
    @Query("DELETE FROM Registro WHERE id = :id") suspend fun excluir(id: Long)

    @Query("SELECT * FROM Agua WHERE data = :data ORDER BY em, id") fun agua(data: Long): Flow<List<Agua>>
    @Query("SELECT COALESCE(SUM(ml), 0) FROM Agua WHERE data = :data") suspend fun aguaTotal(data: Long): Int
    @Query("SELECT data, SUM(ml) AS ml FROM Agua WHERE data BETWEEN :de AND :ate GROUP BY data")
    suspend fun aguaEntre(de: Long, ate: Long): List<AguaDoDia>
    @Query("SELECT data, SUM(ml) AS ml FROM Agua GROUP BY data") suspend fun aguaPorDia(): List<AguaDoDia>
    @Insert suspend fun inserir(a: Agua): Long
    @Query("DELETE FROM Agua WHERE id = :id") suspend fun excluirAgua(id: Long)
    @Query("SELECT * FROM Agua WHERE data = :data ORDER BY em DESC, id DESC LIMIT 1") suspend fun ultimaAgua(data: Long): Agua?

    @Upsert suspend fun salvarResumo(d: DiaResumo)
    @Query("SELECT * FROM DiaResumo WHERE data = :data") suspend fun resumo(data: Long): DiaResumo?
    @Query("SELECT * FROM DiaResumo WHERE data BETWEEN :de AND :ate") suspend fun resumosEntre(de: Long, ate: Long): List<DiaResumo>
    @Query("SELECT * FROM DiaResumo") suspend fun resumos(): List<DiaResumo>

    @Query("SELECT * FROM Conquista ORDER BY em") fun conquistas(): Flow<List<Conquista>>
    @Query("SELECT * FROM Conquista") suspend fun conquistasAgora(): List<Conquista>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun inserir(c: Conquista): Long
}

/** Acesso bruto usado só pelo backup. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM Alimento WHERE fonte != 'TACO' OR id IN (SELECT alimentoId FROM ItemPlano)") suspend fun alimentos(): List<Alimento>
    @Query("SELECT * FROM Plano") suspend fun planos(): List<Plano>
    @Query("SELECT * FROM DiaSemana") suspend fun dias(): List<DiaSemana>
    @Query("SELECT * FROM Refeicao") suspend fun refeicoes(): List<Refeicao>
    @Query("SELECT * FROM Opcao") suspend fun opcoes(): List<Opcao>
    @Query("SELECT * FROM ItemPlano") suspend fun itens(): List<ItemPlano>
    @Query("SELECT * FROM Registro") suspend fun registros(): List<Registro>
    @Query("SELECT * FROM ItemRegistro") suspend fun itensRegistro(): List<ItemRegistro>
    @Query("SELECT * FROM Agua") suspend fun aguas(): List<Agua>
    @Query("SELECT * FROM DiaResumo") suspend fun resumos(): List<DiaResumo>
    @Query("SELECT * FROM Conquista") suspend fun conquistas(): List<Conquista>

    @Query("DELETE FROM ItemRegistro") suspend fun apagarItensRegistro()
    @Query("DELETE FROM Registro") suspend fun apagarRegistros()
    @Query("DELETE FROM Agua") suspend fun apagarAguas()
    @Query("DELETE FROM DiaResumo") suspend fun apagarResumos()
    @Query("DELETE FROM Conquista") suspend fun apagarConquistas()
    @Query("DELETE FROM DiaSemana") suspend fun apagarDias()
    @Query("DELETE FROM ItemPlano") suspend fun apagarItens()
    @Query("DELETE FROM Opcao") suspend fun apagarOpcoes()
    @Query("DELETE FROM Refeicao") suspend fun apagarRefeicoes()
    @Query("DELETE FROM Plano") suspend fun apagarPlanos()
    @Query("DELETE FROM Alimento") suspend fun apagarAlimentos()

    @Insert suspend fun alimentos(l: List<Alimento>)
    @Insert suspend fun planos(l: List<Plano>)
    @Insert suspend fun dias(l: List<DiaSemana>)
    @Insert suspend fun refeicoes(l: List<Refeicao>)
    @Insert suspend fun opcoes(l: List<Opcao>)
    @Insert suspend fun itens(l: List<ItemPlano>)
    @Insert suspend fun registros(l: List<Registro>)
    @Insert suspend fun itensRegistro(l: List<ItemRegistro>)
    @Insert suspend fun aguas(l: List<Agua>)
    @Insert suspend fun resumos(l: List<DiaResumo>)
    @Insert suspend fun conquistas(l: List<Conquista>)
}
