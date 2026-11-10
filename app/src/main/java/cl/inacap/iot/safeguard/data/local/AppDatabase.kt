package cl.inacap.iot.safeguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.inacap.iot.safeguard.data.local.dao.TelemetryDao
import cl.inacap.iot.safeguard.data.local.dao.UserDao
import cl.inacap.iot.safeguard.data.local.entity.TelemetryLogEntity
import cl.inacap.iot.safeguard.data.local.entity.UserEntity

/**
 * Base de datos SQLite local mediante AndroidX Room.
 * Gestiona credenciales de acceso y logs de auditoría de telemetría de planta.
 */
@Database(
    entities = [UserEntity::class, TelemetryLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        private const val DATABASE_NAME = "iot_safeguard_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
