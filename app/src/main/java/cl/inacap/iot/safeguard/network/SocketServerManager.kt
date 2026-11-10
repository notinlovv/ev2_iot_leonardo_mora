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
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gestor de Servidor TCP sobre Wi-Fi Local (Puerto 8888).
 * Actúa como nodo IoT/Servidor de telemetría de planta:
 * - Emite lecturas de telemetría a todos los clientes conectados.
 * - Recibe comandos de actuadores (conmutación de ventilador).
 */
class SocketServerManager(private val port: Int = DEFAULT_PORT) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val connectedClients = CopyOnWriteArrayList<ClientHandler>()

    private val _clientCount = MutableStateFlow(0)
    val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    var onCommandReceived: ((command: String) -> Unit)? = null

    companion object {
        const val DEFAULT_PORT = 8888
        const val TAG = "SocketServerManager"

        const val CMD_TOGGLE_FAN = "CMD_TOGGLE_FAN"
        const val CMD_FAN_ON = "CMD_FAN_ON"
        const val CMD_FAN_OFF = "CMD_FAN_OFF"
    }

    /**
     * Inicia el ServerSocket en una corrutina en segundo plano (Dispatchers.IO).
     */
    fun startServer(scope: CoroutineScope) {
        if (_isServerRunning.value) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                _isServerRunning.value = true
                Log.d(TAG, "Servidor TCP iniciado en el puerto $port")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Cliente conectado desde: ${clientSocket.inetAddress.hostAddress}")
                    val handler = ClientHandler(clientSocket)
                    connectedClients.add(handler)
                    _clientCount.value = connectedClients.size
                    handler.start(scope)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Error en el servidor TCP: ${e.message}", e)
                }
            } finally {
                stopServer()
            }
        }
    }

    /**
     * Transmite la telemetría actual a todos los operadores conectados en la red Wi-Fi.
     */
    suspend fun broadcastTelemetry(state: TelemetryState) = withContext(Dispatchers.IO) {
        val payload = state.toJson()
        val iterator = connectedClients.iterator()
        while (iterator.hasNext()) {
            val client = iterator.next()
            val success = client.send(payload)
            if (!success) {
                client.close()
                connectedClients.remove(client)
                _clientCount.value = connectedClients.size
            }
        }
    }

    /**
     * Detiene el servidor y cierra todas las conexiones de forma segura.
     */
    fun stopServer() {
        _isServerRunning.value = false
        try {
            connectedClients.forEach { it.close() }
            connectedClients.clear()
            _clientCount.value = 0
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar servidor: ${e.message}", e)
        }
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Manejador de cada conexión individual de cliente.
     */
    private inner class ClientHandler(private val socket: Socket) {
        private var writer: BufferedWriter? = null
        private var reader: BufferedReader? = null
        private var clientJob: Job? = null

        fun start(scope: CoroutineScope) {
            clientJob = scope.launch(Dispatchers.IO) {
                try {
                    writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    while (isActive) {
                        val line = reader?.readLine() ?: break
                        Log.d(TAG, "Comando recibido: $line")
                        withContext(Dispatchers.Main) {
                            onCommandReceived?.invoke(line.trim())
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Cliente desconectado: ${e.message}")
                } finally {
                    close()
                    connectedClients.remove(this@ClientHandler)
                    _clientCount.value = connectedClients.size
                }
            }
        }

        fun send(message: String): Boolean {
            return try {
                writer?.write(message)
                writer?.newLine()
                writer?.flush()
                true
            } catch (e: Exception) {
                false
            }
        }

        fun close() {
            try {
                clientJob?.cancel()
                writer?.close()
                reader?.close()
                socket.close()
            } catch (ignored: Exception) {
            }
        }
    }
}
