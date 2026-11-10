package cl.inacap.iot.safeguard.network

import android.util.Log
import cl.inacap.iot.safeguard.model.TelemetryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Gestor de Cliente TCP sobre Wi-Fi Local.
 * Conecta al operador (Dashboard) con el nodo simulador/servidor IoT:
 * - Recibe telemetría en tiempo real mediante StateFlow.
 * - Envía comandos de control para actuadores (ventilador de mitigación).
 */
class SocketClientManager {

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var listeningJob: Job? = null

    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    companion object {
        const val TAG = "SocketClientManager"
        const val CONNECTION_TIMEOUT_MS = 5000
    }

    /**
     * Conecta al servidor Socket en la dirección IP y puerto especificados.
     */
    fun connect(host: String, port: Int, scope: CoroutineScope) {
        disconnect()

        listeningJob = scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Conectando a $host:$port...")
                _connectionError.value = null

                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS)
                socket = newSocket

                writer = BufferedWriter(OutputStreamWriter(newSocket.getOutputStream()))
                reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))

                _isConnected.value = true
                Log.d(TAG, "Conexión TCP establecida con éxito")

                while (isActive) {
                    val line = reader?.readLine() ?: break
                    try {
                        val state = TelemetryState.fromJson(line)
                        _telemetryState.value = state
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando telemetría JSON: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión de socket: ${e.message}")
                _connectionError.value = e.localizedMessage ?: "Error de conexión"
            } finally {
                disconnect()
            }
        }
    }

    /**
     * Envía un comando de texto al nodo servidor por el socket abierto.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentWriter = writer ?: return@withContext false
            currentWriter.write(command)
            currentWriter.newLine()
            currentWriter.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando comando: ${e.message}")
            false
        }
    }

    /**
     * Envía comando específico para conmutar el ventilador de mitigación.
     */
    suspend fun toggleFan(): Boolean {
        return sendCommand(SocketServerManager.CMD_TOGGLE_FAN)
    }

    /**
     * Cierra el socket y libera recursos de forma segura.
     */
    fun disconnect() {
        _isConnected.value = false
        try {
            listeningJob?.cancel()
            listeningJob = null
            writer?.close()
            reader?.close()
            socket?.close()
            socket = null
        } catch (ignored: Exception) {
        }
    }
}
