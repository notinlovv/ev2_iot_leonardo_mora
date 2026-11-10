package cl.inacap.iot.safeguard

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.inacap.iot.safeguard.databinding.ActivityDashboardBinding
import cl.inacap.iot.safeguard.model.TelemetryState
import cl.inacap.iot.safeguard.network.SocketClientManager
import cl.inacap.iot.safeguard.network.SocketServerManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val clientManager = SocketClientManager()

    private var isUserSwitchAction = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnConnect.setOnClickListener {
            if (clientManager.isConnected.value) {
                clientManager.disconnect()
            } else {
                val ip = binding.etServerIp.text?.toString()?.trim().orEmpty()
                if (ip.isBlank()) {
                    Toast.makeText(this, "Por favor ingrese la IP del servidor", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                clientManager.connect(ip, SocketServerManager.DEFAULT_PORT, lifecycleScope)
            }
        }

        binding.switchMitigationFan.setOnClickListener {
            lifecycleScope.launch {
                val success = clientManager.toggleFan()
                if (!success) {
                    Toast.makeText(this@DashboardActivity, "No se pudo enviar el comando (Verifique conexión)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeState() {
        // Observar estado de conexión
        lifecycleScope.launch {
            clientManager.isConnected.collect { isConnected ->
                if (isConnected) {
                    binding.tvConnectionStatus.text = "Estado: Conectado al Nodo OT"
                    binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.semaforo_verde))
                    binding.btnConnect.text = "Desconectar"
                    binding.etServerIp.isEnabled = false
                } else {
                    binding.tvConnectionStatus.text = "Estado: Desconectado"
                    binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.secondary))
                    binding.btnConnect.text = "Conectar"
                    binding.etServerIp.isEnabled = true
                }
            }
        }

        // Observar errores
        lifecycleScope.launch {
            clientManager.connectionError.collect { error ->
                error?.let {
                    Toast.makeText(this@DashboardActivity, "Error: $it", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observar telemetría en tiempo real
        lifecycleScope.launch {
            clientManager.telemetryState.collect { telemetry ->
                renderTelemetry(telemetry)
            }
        }
    }

    private fun renderTelemetry(state: TelemetryState) {
        // Gas MQ-2
        binding.tvLiveGas.text = "Concentración de Gas (MQ-2): ${state.mq2GasPpm} PPM"
        binding.pbGas.progress = state.mq2GasPpm

        // Humo MQ-2
        binding.tvLiveSmoke.text = "Concentración de Humo (MQ-2): ${state.mq2SmokePpm} PPM"
        binding.pbSmoke.progress = state.mq2SmokePpm

        // Flama KY-026
        if (state.ky026FlameDetected) {
            binding.tvLiveFlame.text = "Sensor de Flama KY-026: ¡FUEGO DETECTADO EN PLANTA!"
            binding.tvLiveFlame.setTextColor(ContextCompat.getColor(this, R.color.semaforo_rojo))
        } else {
            binding.tvLiveFlame.text = "Sensor de Flama KY-026: Sin Fuego Detectado"
            binding.tvLiveFlame.setTextColor(ContextCompat.getColor(this, R.color.black))
        }

        // Semáforo Virtual
        val (colorRes, text) = when (state.semaforoColor) {
            TelemetryState.SEMAFORO_ROJO -> Pair(R.color.semaforo_rojo, "ESTADO: ROJO (PELIGRO CRÍTICO)")
            TelemetryState.SEMAFORO_AMARILLO -> Pair(R.color.semaforo_amarillo, "ESTADO: AMARILLO (PRECAUCIÓN)")
            TelemetryState.SEMAFORO_VERDE -> Pair(R.color.semaforo_verde, "ESTADO: VERDE (PLANTA SEGURA)")
            else -> Pair(R.color.semaforo_normal, "ESTADO: NORMAL")
        }

        val colorInt = ContextCompat.getColor(this, colorRes)
        binding.viewVirtualSemaforo.backgroundTintList = ColorStateList.valueOf(colorInt)
        binding.tvVirtualSemaforo.text = text
        binding.tvVirtualSemaforo.setTextColor(colorInt)

        // Buzzer
        binding.tvVirtualBuzzer.text = "Buzzer: ${state.buzzerBeeps} Beeps"

        // Ventilador (Switch)
        isUserSwitchAction = false
        binding.switchMitigationFan.isChecked = state.fanActive
        binding.tvFanStatus.text = "Estado actual: ${if (state.fanActive) "ACTIVADO (Extrayendo)" else "Apagado"}"
        isUserSwitchAction = true
    }

    override fun onDestroy() {
        super.onDestroy()
        clientManager.disconnect()
    }
}
