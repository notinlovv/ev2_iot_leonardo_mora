package cl.inacap.iot.safeguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de registro histórico y auditoría local de telemetría IoT / eventos críticos.
 */
@Entity(tableName = "telemetry_logs")
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "gas_ppm")
    val gasPpm: Int,

    @ColumnInfo(name = "smoke_ppm")
    val smokePpm: Int,

    @ColumnInfo(name = "flame_detected")
    val flameDetected: Boolean,

    @ColumnInfo(name = "fan_active")
    val fanActive: Boolean,

    @ColumnInfo(name = "alert_status")
    val alertStatus: String
)
