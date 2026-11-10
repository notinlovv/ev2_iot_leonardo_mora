package cl.inacap.iot.safeguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.inacap.iot.safeguard.data.local.entity.TelemetryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLogEntity): Long

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentLogs(): List<TelemetryLogEntity>

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentLogs(): Flow<List<TelemetryLogEntity>>

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearLogs()
}
