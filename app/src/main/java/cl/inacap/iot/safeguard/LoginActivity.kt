package cl.inacap.iot.safeguard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cl.inacap.iot.safeguard.data.local.AppDatabase
import cl.inacap.iot.safeguard.data.local.entity.UserEntity
import cl.inacap.iot.safeguard.databinding.ActivityLoginBinding
import cl.inacap.iot.safeguard.security.HashUtils
import cl.inacap.iot.safeguard.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: AppDatabase
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        securityManager = SecurityManager.getInstance(this)

        checkExistingSession()
        setupListeners()
    }

    private fun checkExistingSession() {
        if (securityManager.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (username.isBlank() || password.isBlank()) {
                binding.tvStatus.text = "Por favor ingrese usuario y contraseña."
                return@setOnClickListener
            }

            performLogin(username, password)
        }

        binding.btnRegisterInitial.setOnClickListener {
            showRegisterDialog()
        }
    }

    private fun performLogin(username: String, passwordRaw: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val passwordHash = HashUtils.sha256(passwordRaw)
            val user = database.userDao().validateLogin(username, passwordHash)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (user != null) {
                    // Guardar sesión cifrada según ISO 27400
                    securityManager.saveUserSession(user.username, user.role)
                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido ${user.username} (${user.role})",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                } else {
                    binding.tvStatus.text = "Credenciales inválidas. Verifique sus datos."
                }
            }
        }
    }

    private fun showRegisterDialog() {
        val inputView = layoutInflater.inflate(R.layout.dialog_register_user, null)
        val etNewUser = inputView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewUsername)
        val etNewPass = inputView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPassword)

        AlertDialog.Builder(this)
            .setTitle("Registro de Usuario Local (SQLite)")
            .setMessage("Crea un usuario para operar en planta (la clave será cifrada con SHA-256):")
            .setView(inputView)
            .setPositiveButton("Crear") { _, _ ->
                val user = etNewUser.text?.toString()?.trim().orEmpty()
                val pass = etNewPass.text?.toString()?.trim().orEmpty()

                if (user.isNotBlank() && pass.isNotBlank()) {
                    registerUser(user, pass)
                } else {
                    Toast.makeText(this, "Campos no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registerUser(user: String, pass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val existing = database.userDao().getUserByUsername(user)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "El usuario ya existe.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val hash = HashUtils.sha256(pass)
            val count = database.userDao().getUserCount()
            val role = if (count == 0) UserEntity.ROLE_ADMIN else UserEntity.ROLE_OPERATOR

            val newUser = UserEntity(
                username = user,
                passwordHash = hash,
                role = role
            )
            database.userDao().insertUser(newUser)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@LoginActivity,
                    "Usuario creado con rol: $role. Ya puede iniciar sesión.",
                    Toast.LENGTH_LONG
                ).show()
                binding.etUsername.setText(user)
                binding.etPassword.setText("")
                binding.tvStatus.text = "Usuario registrado. Ingrese su contraseña."
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
