package cl.inacap.iot.safeguard.model

import org.json.JSONObject

/**
 * Modelo de telemetría y estado operativo del nodo IoT Industrial (OT).
 * Soporta serialización directa a JSON para transmisión TCP por Wi-Fi local.
 */
data class TelemetryState(
    var mq2GasPpm: Int = 0,
    var mq2SmokePpm: Int = 0,
    var ky026FlameDetected: Boolean = false,
    var fanActive: Boolean = false,
    var semaforoColor: String = SEMAFORO_NORMAL,
    var buzzerBeeps: Int = 0,
    var timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val SEMAFORO_NORMAL = "NORMAL"
        const val SEMAFORO_VERDE = "VERDE"
        const val SEMAFORO_AMARILLO = "AMARILLO"
        const val SEMAFORO_ROJO = "ROJO"

        // Umbrales industriales según norma de seguridad de planta
        const val GAS_PPM_WARNING = 300
        const val GAS_PPM_CRITICAL = 750
        const val SMOKE_PPM_WARNING = 200
        const val SMOKE_PPM_CRITICAL = 600

        fun fromJson(jsonStr: String): TelemetryState {
            val json = JSONObject(jsonStr)
            return TelemetryState(
                mq2GasPpm = json.optInt("mq2GasPpm", 0),
                mq2SmokePpm = json.optInt("mq2SmokePpm", 0),
                ky026FlameDetected = json.optBoolean("ky026FlameDetected", false),
                fanActive = json.optBoolean("fanActive", false),
                semaforoColor = json.optString("semaforoColor", SEMAFORO_NORMAL),
                buzzerBeeps = json.optInt("buzzerBeeps", 0),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("mq2GasPpm", mq2GasPpm)
        json.put("mq2SmokePpm", mq2SmokePpm)
        json.put("ky026FlameDetected", ky026FlameDetected)
        json.put("fanActive", fanActive)
        json.put("semaforoColor", semaforoColor)
        json.put("buzzerBeeps", buzzerBeeps)
        json.put("timestamp", timestamp)
        return json.toString()
    }

    /**
     * Calcula dinámicamente el estado del semáforo y del buzzer en base a las lecturas
     */
    fun recalculateStatus() {
        if (ky026FlameDetected || mq2GasPpm >= GAS_PPM_CRITICAL || mq2SmokePpm >= SMOKE_PPM_CRITICAL) {
            semaforoColor = SEMAFORO_ROJO
            buzzerBeeps = 3
        } else if (mq2GasPpm >= GAS_PPM_WARNING || mq2SmokePpm >= SMOKE_PPM_WARNING) {
            semaforoColor = SEMAFORO_AMARILLO
            buzzerBeeps = 1
        } else {
            semaforoColor = SEMAFORO_VERDE
            buzzerBeeps = 0
        }
    }

    fun isCritical(): Boolean {
        return semaforoColor == SEMAFORO_ROJO || ky026FlameDetected
    }
}
