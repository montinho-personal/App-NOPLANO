package app.noplano.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Alimento::class, Plano::class, DiaSemana::class, Refeicao::class, Opcao::class, ItemPlano::class,
        Registro::class, ItemRegistro::class, Agua::class, DiaResumo::class, Conquista::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class Banco : RoomDatabase() {
    abstract fun planoDao(): PlanoDao
    abstract fun alimentoDao(): AlimentoDao
    abstract fun registroDao(): RegistroDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** Versão 2: alvo de macros opcional em cada plano. */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (coluna in listOf("alvoKcal", "alvoProt", "alvoCarb", "alvoGord")) {
                    db.execSQL("ALTER TABLE Plano ADD COLUMN $coluna REAL")
                }
            }
        }

        val MIGRACOES = arrayOf(MIGRACAO_1_2)

        fun abrir(context: Context): Banco =
            Room.databaseBuilder(context, Banco::class.java, "noplano.db").addMigrations(*MIGRACOES).build()

        fun emMemoria(context: Context): Banco =
            Room.inMemoryDatabaseBuilder(context, Banco::class.java).allowMainThreadQueries().build()
    }
}
