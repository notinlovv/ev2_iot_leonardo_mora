package cl.inacap.iot.safeguard.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Gestor de seguridad y almacenamiento cifrado local en reposo para IoT / OT.
 * Alineado a recomendaciones de seguridad y privacidad IoT (ISO/IEC 27400).
 *
 * Utiliza Android KeyStore con MasterKeys (AES256_GCM_SPEC) y EncryptedSharedPreferences
 * con esquemas AES-256 SIV (claves) y AES-256 GCM (valores).
 */
class SecurityManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_FILE_NAME = "iot_safeguard_secure_vault"
        private const val KEY_AUTH_TOKEN = "sec_auth_token"
        private const val KEY_USERNAME = "sec_username"
        private const val KEY_USER_ROLE = "sec_user_role"

        @Volatile
        private var INSTANCE: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- Métodos de gestión de sesión y credenciales ---

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUserSession(username: String, role: String) {
        sharedPreferences.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }

    fun isLoggedIn(): Boolean {
        return !getUsername().isNullOrBlank()
    }

    fun putSecureString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getSecureString(key: String, default: String? = null): String? {
        return sharedPreferences.getString(key, default)
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
