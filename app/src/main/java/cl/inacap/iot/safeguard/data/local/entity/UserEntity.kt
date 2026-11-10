package cl.inacap.iot.safeguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de usuario para autenticación local en planta.
 * Conforme a ISO 27400: almacena la contraseña hasheada (SHA-256) y rol de acceso.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "role")
    val role: String = ROLE_OPERATOR
) {
    companion object {
        const val ROLE_OPERATOR = "OPERATOR"
        const val ROLE_ADMIN = "ADMIN"
    }
}
