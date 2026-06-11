package com.cashtelo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cashtelo.data.dao.TransactionDao
import com.cashtelo.data.dao.UserDao
import com.cashtelo.data.entity.Category
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.data.entity.User

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Criar tabela users preservando os dados da tabela transactions
        database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `users` ADD COLUMN `password` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `users` ADD COLUMN `useBiometrics` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `users` ADD COLUMN `avatarUri` TEXT DEFAULT NULL")
    }
}

@Database(entities = [Transaction::class, User::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CashteloDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: CashteloDatabase? = null

        fun getDatabase(context: Context): CashteloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashteloDatabase::class.java,
                    "cashtelo_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Adiciona as migrações
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
