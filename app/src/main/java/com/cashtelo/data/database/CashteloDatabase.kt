package com.cashtelo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cashtelo.data.dao.CategoryDao
import com.cashtelo.data.dao.TransactionDao
import com.cashtelo.data.dao.UserDao
import com.cashtelo.data.entity.Category
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionCategory
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.data.entity.User

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): TransactionCategory = TransactionCategory.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`emoji` TEXT NOT NULL DEFAULT '📦', " +
            "`type` TEXT NOT NULL DEFAULT 'AMBOS')"
        )
    }
}

@Database(entities = [Transaction::class, User::class, Category::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CashteloDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
