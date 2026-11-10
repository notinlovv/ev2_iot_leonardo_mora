package cl.inacap.iot.safeguard

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cl.inacap.iot.safeguard.data.local.AppDatabase
import cl.inacap.iot.safeguard.data.local.entity.TelemetryLogEntity
import cl.inacap.iot.safeguard.databinding.ActivitySimulatorBinding
import cl.inacap.iot.safeguard.model.TelemetryState
import cl.inacap.iot.safeguard.network.NetworkUtils
import cl.inacap.iot.safeguard.network.SocketServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimulatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulatorBinding
    private lateinit var serverManager: SocketServerManager
    private lateinit var database: AppDatabase

    private var currentState = TelemetryState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimulatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        serverManager = SocketServerManager()

        initNetworkInfo()
        setupListeners()
        startServer()
        updateUiState()
    }

    private fun initNetworkInfo() {
        val ip = NetworkUtils.getLocalIpAddress()
        binding.tvServerIp.text = "IP Wi-Fi Local: $ip:${SocketServerManager.DEFAULT_PORT}"
    }

    private fun startServer() {
        serverManager.startServer(lifecycleScope)

        // Escuchar cantidad de clientes conectados
        lifecycleScope.launch {
            serverManager.clientCount.collect { count ->
                binding.tvConnectedClients.text = "Clientes conectados: $count"
            }
        }

        // Manejar comandos entrantes desde el operador cliente
        serverManager.onCommandReceived = { command ->
            when (command) {
                SocketServerManager.CMD_TOGGLE_FAN -> {
                    currentState.fanActive = !currentState.fanActive
                    binding.switchFan.isChecked = currentState.fanActive
                    Toast.makeText(this, "Comando recibido: Ventilador ${if (currentState.fanActive) "ACTIVADO" else "DESACTIVADO"}", Toast.LENGTH_SHORT).show()
                    broadcastCurrentState()
                }
                SocketServerManager.CMD_FAN_ON -> {
                    currentState.fanActive = true
                    binding.switchFan.isChecked = true
                    broadcastCurrentState()
                }
                SocketServerManager.CMD_FAN_OFF -> {
                    currentState.fanActive = false
                    binding.switchFan.isChecked = false
                    broadcastCurrentState()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.sbGas.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentState.mq2GasPpm = progress
                binding.tvGasLabel.text = "MQ-2 Gas: $progress PPM"
                onTelemetryChanged()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                broadcastCurrentState()
            }
        })

        binding.sbSmoke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentState.mq2SmokePpm = progress
                binding.tvSmokeLabel.text = "MQ-2 Humo: $progress PPM"
                onTelemetryChanged()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                broadcastCurrentState()
            }
        })

        binding.switchFlame.setOnCheckedChangeListener { _, isChecked ->
            currentState.ky026FlameDetected = isChecked
            onTelemetryChanged()
            broadcastCurrentState()
        }

        binding.switchFan.setOnCheckedChangeListener { _, isChecked ->
            currentState.fanActive = isChecked
            broadcastCurrentState()
        }

        binding.btnBroadcast.setOnClickListener {
            broadcastCurrentState()
            Toast.makeText(this, "Telemetría emitida por Wi-Fi TCP", Toast.LENGTH_SHORT).show()
        }

        binding.btnAuditLogs.setOnClickListener {
            showAuditLogsDialog()
        }
    }

    private fun onTelemetryChanged() {
        currentState.recalculateStatus()
        currentState.timestamp = System.currentTimeMillis()
        updateUiState()

        // Si es condición crítica, registrar automáticamente en SQLite
        if (currentState.isCritical()) {
            saveAuditLog("CONDICION_CRITICA_${currentState.semaforoColor}")
        }
    }

    private fun updateUiState() {
        val (colorRes, statusText) = when (currentState.semaforoColor) {
            TelemetryState.SEMAFORO_ROJO -> Pair(R.color.semaforo_rojo, "Semáforo: ROJO (Peligro/Alarma)")
            TelemetryState.SEMAFORO_AMARILLO -> Pair(R.color.semaforo_amarillo, "Semáforo: AMARILLO (Advertencia)")
            TelemetryState.SEMAFORO_VERDE -> Pair(R.color.semaforo_verde, "Semáforo: VERDE (Normal)")
            else -> Pair(R.color.semaforo_normal, "Semáforo: NORMAL")
        }

        val colorInt = ContextCompat.getColor(this, colorRes)
        binding.viewSemaforoIndicator.backgroundTintList = ColorStateList.valueOf(colorInt)
        binding.tvSemaforoStatus.text = statusText
        binding.tvSemaforoStatus.setTextColor(colorInt)

        binding.tvBuzzerStatus.text = "Buzzer: ${currentState.buzzerBeeps} Beeps"
    }

    private fun broadcastCurrentState() {
        currentState.timestamp = System.currentTimeMillis()
        lifecycleScope.launch(Dispatchers.IO) {
            serverManager.broadcastTelemetry(currentState)
        }
    }

    private fun saveAuditLog(status: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val log = TelemetryLogEntity(
                gasPpm = currentState.mq2GasPpm,
                smokePpm = currentState.mq2SmokePpm,
                flameDetected = currentState.ky026FlameDetected,
                fanActive = currentState.fanActive,
                alertStatus = status
            )
            database.telemetryDao().insertLog(log)
        }
    }

    private fun showAuditLogsDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val logs = database.telemetryDao().getRecentLogs()
            val text = if (logs.isEmpty()) {
                "No hay registros de auditoría en SQLite todavía."
            } else {
                logs.joinToString("\n\n") { log ->
                    "[ID ${log.id}] Gas: ${log.gasPpm} PPM | Humo: ${log.smokePpm} PPM | Fuego: ${if (log.flameDetected) "SI" else "NO"} | Vent.: ${if (log.fanActive) "ON" else "OFF"}\nEstado: ${log.alertStatus}"
                }
            }

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@SimulatorActivity)
                    .setTitle("Auditoría Local SQLite (Últimos 20)")
                    .setMessage(text)
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverManager.stopServer()
    }
}
