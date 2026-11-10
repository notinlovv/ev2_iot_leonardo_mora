package cl.inacap.iot.safeguard.security

import java.security.MessageDigest

/**
 * Utilidades criptográficas para cumplimiento de la norma ISO/IEC 27400.
 * Asegura el almacenamiento y verificación de credenciales sin almacenar texto plano.
 */
object HashUtils {

    /**
     * Genera el hash criptográfico SHA-256 de una cadena de texto.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
