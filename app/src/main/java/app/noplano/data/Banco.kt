package app.noplano.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Alimento::class, Plano::class, DiaSemana::class, Refeicao::class, Opcao::class, ItemPlano::class,
        Registro::class, ItemRegistro::class, Agua::class, DiaResumo::class, Conquista::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class Banco : RoomDatabase() {
    abstract fun planoDao(): PlanoDao
    abstract fun alimentoDao(): AlimentoDao
    abstract fun registroDao(): RegistroDao
    abstract fun backupDao(): BackupDao

    companion object {
        fun abrir(context: Context): Banco =
            Room.databaseBuilder(context, Banco::class.java, "noplano.db").build()

        fun emMemoria(context: Context): Banco =
            Room.inMemoryDatabaseBuilder(context, Banco::class.java).allowMainThreadQueries().build()
    }
}
